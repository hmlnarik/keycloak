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
package org.keycloak.models.map.storage.tree.config;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.realm.MapRealmEntityFields;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.MapperProvider;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.FieldDescriptorGetter;
import org.keycloak.models.map.storage.mapper.TemplateMapperProvider;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;
import org.keycloak.models.map.storage.mapper.MappersMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author hmlnarik
 */
public class MapperTranslator {

    private static final Logger LOG = Logger.getLogger(MapperTranslator.class);

    /**
     *  This map maintains a mapping (field -> Mapper) where a field value is mapped by this mapper
     */
    private final MappersMap mappers = new MappersMap<>();

    private final KeycloakSessionFactory factory;

    public MapperTranslator(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    public <V extends AbstractEntity> void parse(Class<V> entityClass, Map<String, ?> mapperDeclarations, MapStorageProviderFactory storageFactory) {
        for (Entry<String, ?> me : mapperDeclarations.entrySet()) {
            Optional<ParameterizedEntityField<V>> pef = ModelEntityUtil.getParameterizedEntityField(entityClass, me.getKey());
            if (pef.isPresent()) {
                final ParameterizedEntityField<V> field = pef.get();
                try {
                    Mapper<V> mapper = getMapper(entityClass, field, me.getValue(), storageFactory);
                    if (mapper != null) {
                        final Object prevMapper = this.mappers.put(field, mapper);
                        if (prevMapper != null) {
                            LOG.warnf("Warning: two mappers exist for %s field of %s type: %s and %s", field, entityClass, prevMapper, mapper);
                        }
                    }
                } catch (Exception ex) {
                    LOG.errorf(ex, "Error while configuring mapper for %s field", field);
                }
            } else {
                LOG.warnf("Unknown field %s in mapper definition for %s entity", me.getKey(), entityClass.getSimpleName());
            }
        }
    }

    public MappersMap getMappers() {
        return mappers;
    }

    public static class NativeEntityFieldAccessors<V extends AbstractEntity> implements FieldDescriptorGetter<V> {

        private final Class<V> entityClass;

        private static final Map<Class<?>, NativeEntityFieldAccessors<?>> INSTANCES = new IdentityHashMap<>();

        public static <V extends AbstractEntity> NativeEntityFieldAccessors<V> forClass(Class<V> entityClass) {
            return (NativeEntityFieldAccessors<V>) INSTANCES.computeIfAbsent(entityClass, c -> new NativeEntityFieldAccessors<>(entityClass));
        }

        private NativeEntityFieldAccessors(Class<V> entityClass) {
            this.entityClass = entityClass;
        }

        private static class EntityFieldDescriptor<R> implements MapperFieldDescriptor<R> {

            private final EntityField<R> field;

            public EntityFieldDescriptor(EntityField<R> field) {
                this.field = field;
            }

            @Override
            public Function<R, Object> fieldGetter() {
                return field::get;
            }

            @Override
            public BiConsumer<R, Object> fieldSetter() {
                return field::set;
            }

            @Override
            public Class<?> getFieldClass() {
                return field.getFieldClass();
            }
        }

        private static class SingletonFieldDescriptor<R> implements MapperFieldDescriptor<R> {

            private final EntityField<R> field;

            public SingletonFieldDescriptor(EntityField<R> field) {
                // At this moment, only attributes are supported for Set/List settings
                if (! Objects.equals(field.getName(), MapRealmEntityFields.ATTRIBUTES.getNameCamelCase())) {   // Any Map*EnityField.ATTRIBUTES would do for name comparison
                    throw new IllegalArgumentException("Unsupported field, only attributes are currently supported");
                }
                this.field = field;
            }

            @Override
            public Function<R, Object> fieldGetter() {
                return this::fieldGetterSingleton;
            }

            @Override
            public BiConsumer<R, Object> fieldSetter() {
                return this::fieldSetterSingleton;
            }

            @Override
            public Class<?> getFieldClass() {
                // TODO: This needs to be much smarter to determine the type but as long as the field is Attributes, it will do
                return String.class;
            }

            private Object fieldGetterSingleton(R v) {
                Object value = field.get(v);
                return (! (value instanceof Collection) || ((Collection) value).isEmpty())
                  ? null
                  : ((Collection) value).iterator().next();
            }

            private void fieldSetterSingleton(R e, Object value) {
                if (value == null) {
                    field.set(e, null);
                } else {
                    field.set(e, Collections.singletonList(value));
                }
            }
        }

        @Override
        public MapperFieldDescriptor<V> getFieldDescriptor(String fieldName) {
            Optional<ParameterizedEntityField<V>> parameterizedEntityField = ModelEntityUtil.getParameterizedEntityField(entityClass, fieldName);
            if (fieldName.startsWith(MapRealmEntityFields.ATTRIBUTES.getNameCamelCase())) {  // TODO: remove this line when adding support for other fields
                if (! fieldName.endsWith("[]")) {
                    return parameterizedEntityField.map(SingletonFieldDescriptor::new).orElse(null);
                }
            }
            return parameterizedEntityField.map(EntityFieldDescriptor::new).orElse(null);
        }
    }

    @SuppressWarnings("unchecked")
    private <R, V extends AbstractEntity> Mapper<R> getMapper(Class<V> entityClass, ParameterizedEntityField<V> outerNativeField,
      Object config, MapStorageProviderFactory storageProviderFactory) {
        Map<String, Object> configMap;
        if (config instanceof Map) {
            configMap = (Map<String, Object>) config;
        } else if (config instanceof String) {
            configMap = new HashMap<>();
            configMap.put("provider", TemplateMapperProvider.ID);
            configMap.put("template", config);
        } else {
            LOG.warnf("Illegal configuration, ignoring: %s", config);
            return null;
        }

        final String providerName = (String) configMap.getOrDefault("provider", TemplateMapperProvider.ID);
        MapperProviderFactory mapperFactory = (MapperProviderFactory) factory.getProviderFactory(MapperProvider.class, providerName);
        if (mapperFactory == null) {
            LOG.warnf("Mapper not found: %s", providerName);
            return null;
        }
        FieldDescriptorGetter<?> innerFieldDescriptorGetter = storageProviderFactory.getFieldDescriptorGetter(entityClass);
        if (innerFieldDescriptorGetter == null) {
            throw new IllegalArgumentException("Storage " + storageProviderFactory.getId() + " is incapable of leveraging mappers");
        }
        
        Mapper<R> res;

        // Currently, the attributes are treated specially
        // TODO: remove this line when adding support for other fields
        if (outerNativeField.getName().startsWith(MapRealmEntityFields.ATTRIBUTES.getName() + ".")
          && (! outerNativeField.getName().endsWith("[]"))) {
            res = new AttributeValueConversionMapper(mapperFactory.forConfig(entityClass, String.class, configMap, innerFieldDescriptorGetter));
        } else {
            res = mapperFactory.forConfig(entityClass, outerNativeField.getFieldClass(), configMap, innerFieldDescriptorGetter);
        }
        return res;
    }

    public static class AttributeValueConversionMapper<R> implements Mapper<R> {

        private final Mapper<R> res;

        public AttributeValueConversionMapper(Mapper<R> res) {
            this.res = res;
        }

        @Override
        public Object there(R object) {
            final Object value = res.there(object);
            return value == null ? null : Collections.singletonList(value);
        }

        @Override
        public Class<?> getThereClass() {
            return List.class;
        }

        @Override
        public void back(R object, Object value) {
            if (value instanceof Collection && ! ((Collection) value).isEmpty()) {
                res.back(object, ((Collection) value).iterator().next());
            } else {
                res.back(object, null);
            }
        }

        @Override
        public Map<String, Object> backToMap(Object value) {
            Map<String, Object> backToMap = res.backToMap(value);
            if (backToMap == null || backToMap.isEmpty()) {
                return backToMap;
            }
            Entry<String, Object> me = backToMap.entrySet().iterator().next();
            if (me.getValue() instanceof Collection && ! ((Collection) me.getValue()).isEmpty()) {
                return Collections.singletonMap(me.getKey(), ((Collection) me.getValue()).iterator().next());
            } else {
                return backToMap;
            }
        }

    }
}
