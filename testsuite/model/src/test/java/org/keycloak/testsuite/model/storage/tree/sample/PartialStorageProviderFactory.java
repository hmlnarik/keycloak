/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.storage.tree.sample;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.tree.config.ConfigTranslator.MapStoreComponentConfig;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.FieldDescriptorGetter;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author hmlnarik
 */
public class PartialStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory.Partial {

    private static final Logger LOG = Logger.getLogger(PartialStorageProviderFactory.class);
    public static final String PROVIDER_ID = "partial";
    private static final String CONTENTS = "contents";

    public class Provider implements MapStorageProvider {

        @Override
        @SuppressWarnings("unchecked")
        public <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, Flag... flags) {
            if (modelType != ClientModel.class) {
                throw new UnsupportedOperationException("Unsupported model type.");
            }
            return (MapStorage<V, M>) getStorageFor(modelType);
        }

        @Override
        public void close() {
        }
    }

    private final ConcurrentMap<Class<?>, DictStorage<?, ?>> store = new ConcurrentHashMap<>();

    static final Map<Class<?>, Supplier<Dict>> CREATOR = new HashMap<>();
    static {
        CREATOR.put(ClientModel.class, Dict::clientDelegate);
    }

    private final MapStorageProvider providerInstance = new Provider();

    @SuppressWarnings("unchecked")
    private <V extends AbstractEntity, M> DictStorage<V, M> getStorageFor(Class<M> modelType) {
        return (DictStorage<V, M>) store.computeIfAbsent(modelType, c -> new DictStorage<>(ModelEntityUtil.getEntityType(modelType), new LinkedList<>(), CREATOR.get(modelType)));
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return providerInstance;
    }

    @Override
    public void init(Scope config) {
        ComponentModel cm = config instanceof ComponentModelScope ? ((ComponentModelScope) config).getComponentModel() : null;
        if (cm instanceof MapStoreComponentConfig) {
            MapStoreComponentConfig c = (MapStoreComponentConfig) cm;

            for (String name : ModelEntityUtil.getModelNames()) {
                Object o = c.getObject(CONTENTS + "[" + name + "]");
                if (o instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fieldValues = (List<Map<String, Object>>) o;
                    addContents(name, fieldValues);
                }
            }
        }
    }

    private void addContents(String name, List<Map<String, Object>> fieldValues) {
        Class<?> modelClass = ModelEntityUtil.getModelClass(name);
        if (modelClass == null) {
            LOG.warnf("Ignoring unknown entity %s", name);
            return;
        }

        DictStorage<?, ?> entStore = getStorageFor(modelClass);
        List<Dict> internalStore = entStore.getStore();

        for (Map<String, Object> m : fieldValues) {
            Dict entity = (Dict) CREATOR.get(modelClass).get();
            internalStore.add(entity);
            for (Map.Entry<String, Object> e : m.entrySet()) {
                entity.put(e.getKey(), e.getValue());
            }
        }
    }

    @Override
    public FieldDescriptorGetter<?> getFieldDescriptorGetter(Class<? extends AbstractEntity> entityClass) {
        // Dict is used for all entity areas, so entityClass does not matter

        return new DictFieldDescriptorGetter();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "TESTS: Partial storage";
    }

    public static class DictFieldDescriptorGetter implements FieldDescriptorGetter<Dict> {

        public static final DictFieldDescriptorGetter INSTANCE = new DictFieldDescriptorGetter();
        private static final Map<String, Class<?>> FIELD_TYPES = Map.of(
          Dict.CLIENT_FIELD_ENABLED, Boolean.class
//          Dict.CLIENT_FIELD_LOGO, byte[].class
        );

        private final class DictMapperFieldDescriptor implements MapperFieldDescriptor<Dict> {

            private final String fieldName;

            public DictMapperFieldDescriptor(String fieldName) {
                this.fieldName = fieldName;
            }

            @Override
            public Function<Dict, Object> fieldGetter() {
                return dict -> dict.get(fieldName);
            }

            @Override
            public BiConsumer<Dict, Object> fieldSetter() {
                return (dict, o) -> dict.put(fieldName, o);
            }

            @Override
            public Class<?> getFieldClass() {
                return FIELD_TYPES.getOrDefault(fieldName, String.class);
            }
        }

        @Override
        public MapperFieldDescriptor<Dict> getFieldDescriptor(String fieldName) {
            return new DictMapperFieldDescriptor(fieldName);
        }
    }

}
