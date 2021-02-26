/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.Serialization;
import com.fasterxml.jackson.databind.JavaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>,MapStorageProviderFactory {

    public static final String PROVIDER_ID = "concurrenthashmap";

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageProviderFactory.class);

    private final ConcurrentHashMap<String, ConcurrentHashMapStorage<?,?,?>> storages = new ConcurrentHashMap<>();

    private File storageDirectory;

    private String suffix;

    public static final Map<Class<?>, String> MODEL_TO_NAME = new HashMap<>();
    static {
        MODEL_TO_NAME.put(AuthenticatedClientSessionModel.class, "client-sessions");
        MODEL_TO_NAME.put(ClientScopeModel.class, "client-scopes");
        MODEL_TO_NAME.put(ClientModel.class, "clients");
        MODEL_TO_NAME.put(GroupModel.class, "groups");
        MODEL_TO_NAME.put(RealmModel.class, "realms");
        MODEL_TO_NAME.put(RoleModel.class, "roles");
        MODEL_TO_NAME.put(RootAuthenticationSessionModel.class, "sessions");
        MODEL_TO_NAME.put(UserModel.class, "users");
        MODEL_TO_NAME.put(UserSessionModel.class, "user-sessions");
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return new ConcurrentHashMapStorageProvider(this);
    }


    @Override
    public void init(Scope config) {
        if (config instanceof ComponentModelScope) {
            this.suffix = "-" + ((ComponentModelScope) config).getComponentId();
        } else {
            this.suffix = "";
        }

        final String dir = config.get("dir");
        try {
            if (dir == null || dir.trim().isEmpty()) {
                this.storageDirectory = Files.createTempDirectory("storage-map-chm-").toFile();
            } else {
                File f = new File(dir);
                Files.createDirectories(f.toPath());
                this.storageDirectory = f.exists()
                  ? f
                  : Files.createTempDirectory("storage-map-chm-").toFile();
            }
        } catch (IOException ex) {
            this.storageDirectory = null;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        storages.forEach(this::storeMap);
    }

    private void storeMap(String fileName, ConcurrentHashMapStorage<?, ?, ?> store) {
        if (fileName != null) {
            File f = getFile(fileName);
            try {
                if (storageDirectory != null) {
                    LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                    @SuppressWarnings("unchecked")
                    final ModelCriteriaBuilder readAllCriteria = store.createCriteriaBuilder();
                    Serialization.MAPPER.writeValue(f, store.read(readAllCriteria));
                } else {
                    LOG.debugf("Not storing contents of %s because directory not set", fileName);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> loadMap(String fileName,
      Class<V> valueType, Class<M> modelType, EnumSet<Flag> flags) {
        ConcurrentHashMapStorage<K, V, M> store = new ConcurrentHashMapStorage<>(modelType);

        LOG.debugf("Initializing new map storage: %s", fileName);

        if (! flags.contains(Flag.INITIALIZE_EMPTY)) {
            final File f = getFile(fileName);
            if (f != null && f.exists()) {
                try {
                    LOG.debugf("Restoring contents from %s", f.getCanonicalPath());
                    JavaType type = Serialization.MAPPER.getTypeFactory().constructCollectionType(List.class, valueType);

                    List<V> values = Serialization.MAPPER.readValue(f, type);
                    values.forEach((V mce) -> store.create(mce.getId(), mce));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return store;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @SuppressWarnings("unchecked")
    public <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> getStorage(
      Class<K> keyType, Class<V> valueType, Class<M> modelType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        String name = MODEL_TO_NAME.getOrDefault(modelType, modelType.getSimpleName());
        return (ConcurrentHashMapStorage<K, V, M>) storages.computeIfAbsent(name, n -> loadMap(name, valueType, modelType, f));
    }

    private File getFile(String fileName) {
        return storageDirectory == null
          ? null
          : new File(storageDirectory, "map-" + fileName + suffix + ".json");
    }

    @Override
    public String getHelpText() {
        return "In-memory ConcurrentHashMap storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

}
