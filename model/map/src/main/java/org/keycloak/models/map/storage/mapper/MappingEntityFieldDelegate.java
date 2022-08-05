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
package org.keycloak.models.map.storage.mapper;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.common.delegate.HasRawEntity;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class MappingEntityFieldDelegate<V extends AbstractEntity, R> implements EntityFieldDelegate<V>, HasRawEntity<R> {

    private final MappersMap<V, R> mappers;
    private final R entity;
    private final V originalNativeEntity;

    public static <V extends AbstractEntity, R> V delegate(MappersMap<V, R> mappers, R entity, Class<V> entityClass, Completeness mapperComplete) {
        return DeepCloner.DUMB_CLONER.entityFieldDelegate(entityClass, new MappingEntityFieldDelegate<>(mappers, entity, entityClass, mapperComplete));
    }

    @Override
    public R getRawEntity() {
        return entity;
    }

    @SuppressWarnings(value = "unchecked")
    public MappingEntityFieldDelegate(MappersMap<V, R> mappers, R entity, Class<V> entityClass, Completeness mapperComplete) {
        this.mappers = mappers;
        this.entity = entity;
        this.originalNativeEntity = entity != null && mapperComplete == Completeness.NATIVE ? (V) entity : DeepCloner.DUMB_CLONER.emptyInstance(entityClass);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public Object get(EntityField<V> field) {
        if (field.getFieldClass() == Map.class) {
            // This is naive, but will do for the moment as it handles attributes. It does not handle map of maps.
            final Map origMapContents = (Map) field.get(originalNativeEntity);
            Map<Object, Object> res = origMapContents == null ? new HashMap<>() : new HashMap<>(origMapContents);
            mappers.entrySet().stream().filter(me -> me.getKey().isParameterized() && me.getKey().getEntityField() == field).forEach(me -> {
                final Object v = me.getValue().there(entity);
                if (v == null) {
                    res.remove(me.getKey().getParameter());
                } else {
                    res.put(me.getKey().getParameter(), v);
                }
            });
            return res;
        }
        Mapper<R> mapper = mappers.get(ParameterizedEntityField.from(field));
        return mapper == null ? field.get(originalNativeEntity) : mapper.there(entity);
    }

    @Override
    public <T> void set(EntityField<V> field, T value) {
        Mapper<R> mapper = mappers.get(ParameterizedEntityField.from(field));
        if (mapper != null) {
            mapper.back(entity, value);
        } else {
            field.set(originalNativeEntity, value);
        }
    }

    @Override
    public <K> Object mapGet(EntityField<V> field, K key) {
        Mapper<R> mapper = mappers.get(ParameterizedEntityField.from(field, key));
        return mapper == null ? field.mapGet(originalNativeEntity, key) : mapper.there(entity);
    }

    @Override
    public <K, T> void mapPut(EntityField<V> field, K key, T value) {
        Mapper<R> mapper = mappers.get(ParameterizedEntityField.from(field, key));
        if (mapper != null) {
            mapper.back(entity, value);
        } else {
            field.mapPut(originalNativeEntity, key, value);
        }
    }

    @Override
    public <K> Object mapRemove(EntityField<V> field, K key) {
        Mapper<R> mapper = mappers.get(ParameterizedEntityField.from(field, key));
        if (mapper == null) {
            return field.mapRemove(originalNativeEntity, key);
        }
        final Object res = mapper.there(entity);
        mapper.back(entity, null);
        return res;
    }

    @Override
    public boolean isUpdated() {
        return (entity instanceof UpdatableEntity) ? ((UpdatableEntity) entity).isUpdated() : false;
    }

}
