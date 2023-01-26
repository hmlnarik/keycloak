/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.file;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.HasRealmId;
import org.keycloak.models.map.common.StringKeyConverter.StringKey;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.file.yaml.YamlParser;
import org.keycloak.models.map.storage.file.common.MapEntityContext;
import org.keycloak.models.map.storage.file.yaml.PathWriter;
import org.keycloak.models.map.storage.file.yaml.YamlWritingMechanism;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jboss.logging.Logger;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.emitter.Emitter;

/**
 * A file-based {@link MapStorage}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FileMapStorage<V extends AbstractEntity & UpdatableEntity, M> implements MapStorage<V, M> {

    private static final Logger LOG = Logger.getLogger(FileMapStorage.class);

    // any REALM_ID field would do, they share the same name
    private static final String SEARCHABLE_FIELD_REALM_ID_FIELD_NAME = ClientModel.SearchableFields.REALM_ID.getName();
    private static final String FILE_SUFFIX = ".yaml";

    private final static DumpSettings DUMP_SETTINGS = DumpSettings.builder()
      .setIndent(4)
      .setIndicatorIndent(2)
      .setIndentWithIndicator(false)
      .build();

    private final Class<V> entityClass;
    private final Crud crud = new Crud();
    private final Function<String, Path> dataDirectoryFunc;
    private final Function<V, String[]> suggestedPath;

    // TODO: Add auxiliary directory for indices, locks etc.
    // private final String auxiliaryFilesDirectory;

    public FileMapStorage(Class<V> entityClass, Function<V, String[]> uniqueHumanReadableField, Function<String, Path> dataDirectoryFunc) {
        this.entityClass = entityClass;
        this.dataDirectoryFunc = dataDirectoryFunc;
        this.suggestedPath = uniqueHumanReadableField == null ? v -> v.getId() == null ? null : new String[] { v.getId() } : uniqueHumanReadableField;
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        @SuppressWarnings("unchecked")
        MapKeycloakTransaction<V, M> sessionTransaction = session.getAttribute("file-map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (sessionTransaction == null) {
            sessionTransaction = createTransactionInternal(session);
            session.setAttribute("file-map-transaction-" + hashCode(), sessionTransaction);
        }
        return sessionTransaction;
    }

    public MapKeycloakTransaction<V, M> createTransactionInternal(KeycloakSession session) {
        return new FileMapKeycloakTransaction<>(entityClass, crud);
    }

    private static boolean canParseFile(Path p, BasicFileAttributes attrs) {
        final String fn = p.getFileName().toString();
        return attrs.isRegularFile()
          && attrs.size() > 0L
          && ! fn.startsWith(".")
          && fn.endsWith(FILE_SUFFIX)
          && Files.isReadable(p)
        ;
    }

    private class Crud implements ConcurrentHashMapCrudOperations<V, M>, HasRealmId {

        private String defaultRealmId;

        protected Path getPathForSanitizedId(Path sanitizedIdPath) {
            final Path dataDirectory = getDataDirectory();
            final Path dataDirectoryWithChildren = dataDirectory.resolve(sanitizedIdPath).getParent();

            if (! Files.isDirectory(dataDirectoryWithChildren)) {
                try {
                    Files.createDirectories(dataDirectoryWithChildren);
                } catch (IOException ex) {
                    throw new IllegalStateException("Directory does not exist and cannot be created: " + dataDirectory, ex);
                }
            }
            return dataDirectoryWithChildren.resolve(sanitizedIdPath.getFileName() + FILE_SUFFIX);
        }

        protected Path getPathForSanitizedId(String sanitizedId) {
            if (sanitizedId == null) {
                throw new IllegalStateException("Invalid ID to sanitize");
            }

            return getPathForSanitizedId(Path.of(sanitizedId));
        }

        protected String sanitizeId(String id) {
            Objects.requireNonNull(id, "ID must be non-null");

            // TODO: sanitize
//            id = id
//              .replaceAll("=", "=e")
//              .replaceAll(":", "=c")
//              .replaceAll("/", "=s")
//              .replaceAll("\\\\", "=b")
//            ;
            final Path pId = Path.of(id);

            // Do not allow absolute paths
            if (pId.isAbsolute()) {
                throw new IllegalStateException("Illegal ID requested: " + id);
            }

            return id;
        }

        protected String desanitizeId(String sanitizedId) {
            if (sanitizedId == null) {
                return null;
            }

            return sanitizedId
              .replaceAll("=c", ":")
              .replaceAll("=s", "/")
              .replaceAll("=b", "\\\\")
              .replaceAll("=e", "=")
            ;

        }

        protected V parse(Path fileName) {
            try {
                final V parsedObject = YamlParser.parse(Files.newInputStream(fileName), new MapEntityContext<>(entityClass));
                final String fileNameStr = fileName.getFileName().toString();
                String id = determineKeyFromValue(parsedObject, false);
                final String desanitizedId = desanitizeId(fileNameStr.substring(0, fileNameStr.length() - FILE_SUFFIX.length()));
                if (id == null) {
                    id = desanitizedId;
                } else if (! id.endsWith(desanitizedId)) {
                    LOG.warnf("Filename \"%s\" does not end with expected id \"%s\". Fix the file name.", fileNameStr, id);
                }

                parsedObject.setId(id);
                parsedObject.clearUpdatedFlag();

                return parsedObject;
            } catch (IOException ex) {
                LOG.warnf(ex, "Error reading %s", fileName);
                return null;
            }
        }

        @Override
        public V create(V value) {
            // TODO: Lock realm directory for changes (e.g. on realm deletion)
            // TODO: Sanitize ID
            String sanitizedId = sanitizeId(value.getId());

            writeYamlContents(getPathForSanitizedId(sanitizedId), value);

            return value;
        }

        @Override
        public String determineKeyFromValue(V value, boolean forCreate) {
            final boolean randomId;
            String[] proposedId = suggestedPath.apply(value);

            if (! forCreate) {
                return proposedId == null ? null : String.join("/", proposedId);
            }

            if (proposedId == null || proposedId.length == 0) {
                randomId = value.getId() == null;
                proposedId = new String[] { value.getId() == null ? StringKey.INSTANCE.yieldNewUniqueKey() : value.getId() };
            } else {
                randomId = false;
            }
            String[] baseProposedId = proposedId;

            Path sanitizedId = Path.of(
              sanitizeId(proposedId[0]),
              Stream.of(proposedId).skip(1).map(this::sanitizeId).toArray(String[]::new)
            );

            Path sp = getPathForSanitizedId(sanitizedId);
            for (int counter = 0; counter < 100; counter++) {
                LOG.tracef("Attempting to create file %s", sp);
                try {
                    // touch. TODO: Garbage collection upon rollback
                    Files.createFile(sp);
                    return String.join("/", proposedId);
                } catch (FileAlreadyExistsException ex) {
                    final String lastComponent = randomId ? StringKey.INSTANCE.yieldNewUniqueKey() : baseProposedId[baseProposedId.length - 1] + "-" + counter;
                    proposedId[proposedId.length - 1] = lastComponent;
                    sanitizedId = sanitizedId.resolveSibling(sanitizeId(lastComponent));
                    sp = getPathForSanitizedId(sanitizedId);
                } catch (IOException ex) {
                    throw new IllegalStateException("Could not create file " + sp, ex);
                }
            }

            return null;
        }

        @Override
        public V read(String key) {
            return Optional.ofNullable(sanitizeId(key))
              .map(this::getPathForSanitizedId)
              .filter(Files::isReadable)
              .map(this::parse)
              .orElse(null);
        }

        @Override
        public Stream<V> read(QueryParameters<M> queryParameters) {
            final List<Path> paths;
            FileCriteriaBuilder cb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(FileCriteriaBuilder.criteria());
            String realmId = (String) cb.getSingleRestrictionArgument(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME);

            setRealmId(realmId);

            final Path dataDirectory = getDataDirectory();
            if (! Files.isDirectory(dataDirectory)) {
                return Stream.empty();
            }
            try (Stream<Path> dirStream = Files.find(dataDirectory, entityClass == MapRealmEntity.class ? 1 : 2, FileMapStorage::canParseFile)) {
                // The paths list has to be materialized first, otherwise "dirStream" would be closed
                // before the resulting stream would be read and would return empty result
                paths = dirStream.collect(Collectors.toList());
            } catch (IOException | UncheckedIOException ex) {
                LOG.warnf(ex, "Error listing %s", dataDirectory);
                return Stream.empty();
            }

            return paths.stream().map(this::parse);
        }

        @Override
        public V update(V value) {
            String proposedId = value.getId();
            String sanitizedId = sanitizeId(proposedId);

            Path sp = getPathForSanitizedId(sanitizedId);

            writeYamlContents(sp, value);

            return value;
        }

        @Override
        public boolean delete(String key) {
            Optional<Path> fileName = Optional.ofNullable(sanitizeId(key))
              .map(this::getPathForSanitizedId);
            try {
                return fileName.isPresent() ? Files.deleteIfExists(fileName.get()) : false;
            } catch (IOException ex) {
                LOG.warnf(ex, "Could not delete file: %s", fileName);
                return false;
            }
        }

        @Override
        public long delete(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getCount(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRealmId() {
            return defaultRealmId;
        }

        @Override
        public void setRealmId(String realmId) {
            this.defaultRealmId = realmId;
        }

        private Path getDataDirectory() {
            return dataDirectoryFunc.apply(defaultRealmId == null ? null : sanitizeId(defaultRealmId));
        }

        private void writeYamlContents(Path sp, V value) {
            try (PathWriter w = new PathWriter(sp)) {
                final Emitter emitter = new Emitter(DUMP_SETTINGS, w);
                try (YamlWritingMechanism mech = new YamlWritingMechanism(emitter::emit)) {
                    new MapEntityContext<>(entityClass).writeValue(value, mech);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot write " + sp, ex);
            }
        }

    }

}
