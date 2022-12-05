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
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContextAwareParser;
import org.keycloak.models.map.storage.file.yaml.parser.map.MapEntityYamlContext;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jboss.logging.Logger;

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

    private final Class<V> entityClass;
    private final Crud crud = new Crud();
    private final Function<String, Path> dataDirectoryFunc;

    // TODO: Add auxiliary directory for indices etc.
    // private final String auxiliaryFilesDirectory;

    public FileMapStorage(Class<V> entityClass, Function<String, Path> dataDirectoryFunc) {
        this.entityClass = entityClass;
        this.dataDirectoryFunc = dataDirectoryFunc;
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

    private class Crud implements ConcurrentHashMapCrudOperations<V, M>, HasRealmId {

        private String defaultRealmId;

        protected Optional<Path> getSanitizedPathForId(String id) {
            final Path dataDirectory = getDataDirectory();
            final Path pId = Path.of(id + FILE_SUFFIX);
            final Path pIdNormalized = pId.normalize();
            if (pId.isAbsolute() || ! pId.equals(pIdNormalized)) {
                LOG.warnf("%s object requested with illegal ID: %s", entityClass.getSimpleName(), id);
                return Optional.empty();
            }
            return Optional.of(dataDirectory.resolve(pId));
        }

        protected V parse(Path fileName) {
            try {
                final V parsedObject = YamlContextAwareParser.parse(Files.newInputStream(fileName), new MapEntityYamlContext<>(entityClass));
                final String fileNameStr = fileName.getFileName().toString();

                parsedObject.setId(fileNameStr.substring(0, fileNameStr.length() - FILE_SUFFIX.length()));
                parsedObject.clearUpdatedFlag();

                return parsedObject;
            } catch (IOException ex) {
                LOG.warnf(ex, "Error reading %s", fileName);
                return null;
            }
        }

        @Override
        public V create(V value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public V read(String key) {
            Optional<Path> fileName = getSanitizedPathForId(key);
            return fileName
              .filter(Files::exists)
              .map(this::parse)
              .orElse(null);
        }

        @Override
        public Stream<V> read(QueryParameters<M> queryParameters) {
            final List<Path> paths;
            FileCriteriaBuilder cb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(FileCriteriaBuilder.criteria());
            String realmId = (String) cb.getSingleRestrictionArgument(SEARCHABLE_FIELD_REALM_ID_FIELD_NAME);

            setRealmId(realmId);

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(getDataDirectory())) {
                // The paths list has to be materialized first, otherwise "dirStream" would be closed
                // before the resulting stream would be read and would return empty result
                paths = StreamSupport.stream(Spliterators.spliteratorUnknownSize(dirStream.iterator(), Spliterator.IMMUTABLE), false)
                  .filter(Files::isReadable)
                  .filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX))
                  .collect(Collectors.toList());
            } catch (IOException ex) {
                LOG.warnf(ex, "Error listing %s", getDataDirectory());
                return Stream.empty();
            }

            return paths.stream().map(this::parse);
        }

        @Override
        public V update(V value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean delete(String key) {
            Optional<Path> fileName = getSanitizedPathForId(key);
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
            return dataDirectoryFunc.apply(defaultRealmId);
        }

    }

}
