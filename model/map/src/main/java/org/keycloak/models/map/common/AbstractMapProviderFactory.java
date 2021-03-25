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
package org.keycloak.models.map.common;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.provider.Provider;
import org.jboss.logging.Logger;
import static org.keycloak.models.utils.KeycloakModelUtils.getComponentFactory;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapProviderFactory<T extends Provider, K, V extends AbstractEntity<K>, M> implements AmphibianProviderFactory<T> {

    public static final String PROVIDER_ID = "map";

    protected final Logger LOG = Logger.getLogger(getClass());

    protected final Class<K> keyType;

    protected final Class<M> modelType;

    protected final Class<V> entityType;

    private MapStorageProviderFactory storageProviderFactory;

    private Scope storageConfigScope;


    protected AbstractMapProviderFactory(Class<K> keyType, Class<V> entityType, Class<M> modelType) {
        this.keyType = keyType;
        this.modelType = modelType;
        this.entityType = entityType;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected MapStorage<K, V, M> getStorage(KeycloakSession session) {
        final MapStorageProvider factory = this.storageProviderFactory.create(session);

        return factory.getStorage(keyType, entityType, modelType);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.storageProviderFactory = (MapStorageProviderFactory) getComponentFactory(factory, MapStorageProvider.class, storageConfigScope, MapStorageSpi.NAME);

        // Remove unnecessary reference so that the config can be garbage-collected
        this.storageConfigScope = null;
    }

    @Override
    public void init(Scope config) {
        this.storageConfigScope = config.scope("storage");

    }
}
