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
package org.keycloak.models.map.storage.tree;

import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.keycloak.models.map.storage.tree.config.ConfigTranslator;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import static org.keycloak.models.map.storage.tree.config.ConfigTranslator.STORES;

/**
 *
 * @author hmlnarik
 */
public class TreeStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "tree";

    private static final Logger LOG = Logger.getLogger(TreeStorageProviderFactory.class);
    public static final String DEBUG_STORAGE_CONFIG =
        STORES + ":\n"
      + "  - " + ConcurrentHashMapStorageProviderFactory.PROVIDER_ID + ":\n";

    // Class<ModelType> -> TreeStorage<RespectiveEntityType, ModelType>
    private final ConcurrentHashMap<Class<?>, TreeStorage<?,?>> storages = new ConcurrentHashMap<>();
    private TreeStorageNodePrescription prescription;
    private Scope config;

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return new TreeStorageProvider(this);
    }

    @Override
    public void init(Scope config) {
        this.config = config;
    }

    private static final AtomicInteger UNIQUE_GENERATOR = new AtomicInteger(1);

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        final String directConfig = config.get("config");
        Reader input = null;
        try {
            if (directConfig != null && ! directConfig.trim().isEmpty()) {
                input = new StringReader(directConfig);
            } else {
                final String configFile = config.get("configFile");
                if (configFile == null || configFile.trim().isEmpty()) {
                    LOG.warn("No config set, using DEBUG storage");
                    input = new StringReader(DEBUG_STORAGE_CONFIG);
                } else if (Files.isReadable(Paths.get(configFile))) {
                    input = new FileReader(configFile);
                } else {
                    LOG.warnf("Cannot read config, using DEBUG storage: %s", configFile);
                    input = new StringReader(DEBUG_STORAGE_CONFIG);
                }
            }
            final String componentId, realmId, uniqueId;
            if (config instanceof ComponentModelScope) {
                realmId = ((ComponentModelScope) config).getComponentParentId();
                uniqueId = ((ComponentModelScope) config).getComponentId();
                componentId = uniqueId;
            } else {
                realmId = "ROOT";
                uniqueId = String.valueOf(UNIQUE_GENERATOR.incrementAndGet() & 0xFFFFFF);
                componentId = realmId + "-tree-" + uniqueId;
            }
            try {
                this.prescription = new ConfigTranslator(realmId, uniqueId).parseConfiguration(input);
            } finally {
                input.close();
            }
            this.prescription.setId(componentId);
        } catch (IOException ex) {
            LOG.warnf(ex, "Cannot initialize config, using DEBUG storage");
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Tree of map storages";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    public <V extends AbstractEntity> TreeStorageNodePrescription forEntityClass(Class<V> entityClass) {
        return prescription.forEntityClass(entityClass);
    }

    <V extends AbstractEntity, M> TreeStorage getStorage(Class<M> modelType, Flag... flags) {
        return storages.computeIfAbsent(modelType, cl -> {
            Class<V> entityClass = ModelEntityUtil.getEntityType(modelType);
            return new TreeStorage<>(forEntityClass(entityClass), null);
        });
    }
}
