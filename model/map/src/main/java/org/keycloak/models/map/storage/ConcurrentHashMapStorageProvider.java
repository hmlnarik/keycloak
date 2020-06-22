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
package org.keycloak.models.map.storage;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.Serialization;
import com.fasterxml.jackson.databind.JavaType;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProvider implements MapStorageProvider {

    private static class ConcurrentHashMapStorage<K, V> extends ConcurrentHashMap<K, V> implements MapStorage<K, V> {
    }

    private static final String PROVIDER_ID = "concurrenthashmap";

    private static final Logger LOG = Logger.getLogger(ConcurrentHashMapStorageProvider.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<?,?>> storages = new ConcurrentHashMap<>();

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        storages.forEach(ConcurrentHashMapStorageProvider::storeMap);
    }

    private static void storeMap(String fileName, ConcurrentHashMap<?, ?> store) {
        if (fileName != null) {
            final File f = new File("target/map-" + fileName + ".json");
            try {
                LOG.debugf("Storing contents to %s", f.getCanonicalPath());
                Serialization.MAPPER.writeValue(f, store.values());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static <K, V extends AbstractEntity<K>> ConcurrentHashMapStorage<?, V> loadMap(String fileName, Class<V> valueType, EnumSet<Flag> flags) {
        ConcurrentHashMapStorage<K, V> store = new ConcurrentHashMapStorage<>();

        if (! flags.contains(Flag.INITIALIZE_EMPTY)) {
            final File f = new File("target/map-" + fileName + ".json");
            if (f.exists()) {
                try {
                    LOG.debugf("Restoring contents from %s", f.getCanonicalPath());
                    JavaType type = Serialization.MAPPER.getTypeFactory().constructCollectionType(List.class, valueType);

                    List<V> values = Serialization.MAPPER.readValue(f, type);
                    values.forEach((V mce) -> store.put(mce.getId(), mce));
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

    @Override
    @SuppressWarnings("unchecked")
    public <K, V extends AbstractEntity<K>> MapStorage<K, V> getStorage(String name, Class<K> keyType, Class<V> valueType, Flag... flags) {
        EnumSet<Flag> f = flags == null || flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
        return (MapStorage<K, V>) storages.computeIfAbsent(name, n -> loadMap(name, valueType, f));
    }

}
