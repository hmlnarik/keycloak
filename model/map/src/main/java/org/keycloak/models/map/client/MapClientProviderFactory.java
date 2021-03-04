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
package org.keycloak.models.map.client;

import org.keycloak.models.ClientModel;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapStorage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author hmlnarik
 */
public class MapClientProviderFactory<K> extends AbstractMapProviderFactory<ClientProvider, K, MapClientEntity<K>, ClientModel> implements ClientProviderFactory {

    private final ConcurrentHashMap<K, ConcurrentMap<String, Integer>> REGISTERED_NODES_STORE = new ConcurrentHashMap<>();

    public MapClientProviderFactory() {
        super(MapClientEntity.class, ClientModel.class);
    }

    @Override
    public ClientProvider create(KeycloakSession session) {
        final MapStorage<K, MapClientEntity<K>, ClientModel> storage = getStorage(session);
        return new MapClientProvider<>(session, storage, REGISTERED_NODES_STORE);
    }

    @Override
    public String getHelpText() {
        return "Client provider";
    }

}
