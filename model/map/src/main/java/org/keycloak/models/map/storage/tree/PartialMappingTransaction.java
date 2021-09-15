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
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.SearchableFieldTranslator;
import org.keycloak.models.map.storage.mapper.MappingEntityFieldDelegate;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.MappersMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public abstract class PartialMappingTransaction<V extends AbstractEntity, M, R extends AbstractEntity> implements MapKeycloakTransaction<V, M> {

    protected final MappersMap<V, R> mapperPerField;

    protected final Class<V> entityClass;

    protected final Function<String, Map<String, Object>> idFieldMapper;

    @SuppressWarnings("unchecked")
    public PartialMappingTransaction(Class<V> entityClass, MappersMap<V, R> mapperPerField) {
        Objects.requireNonNull(entityClass, "No entity type set");
        Objects.requireNonNull(mapperPerField, "No mappers set");

        this.entityClass = entityClass;
        ParameterizedEntityField<V> idField = ParameterizedEntityField.from(ModelEntityUtil.getIdField(entityClass));
        Objects.requireNonNull(entityClass, "Unknown entity type");
        this.mapperPerField = mapperPerField;

        Mapper<R> idMapper = mapperPerField.get(idField);
        this.idFieldMapper = idMapper == null
          ? id -> Collections.singletonMap(idField.getNameCamelCase(), id)
          : id -> idMapper.backToMap(id);
    }

//    private static Object getId(String id, Map<String, Object> key2Values) {
//        return (key2Values == null || key2Values.size() != 1)
//          ? null
//          : key2Values.values().iterator().next();
//    }
//
    @Override
    public V create(V value) {
        return createMappingDelegate(createRaw(value));
    }

    /**
     * Creates a new instance of the raw class {@code R}.
     * <p>
     * This method may benefit from the available conversion method {@link #toRaw} to
     * obtain values from the {@code nativeValue} parameter.
     * @param nativeValue
     * @return
     */
    protected abstract R createRaw(V nativeValue);

    /**
     * Maps values from {@code sourceNativeEntity} to {@code targetRawEntity} using {@link #mapperPerField}.
     * @param value
     * @param target
     * @return {@code targetRawEntity}
     */
    public R toRaw(V sourceNativeEntity, R targetRawEntity) {
        if (targetRawEntity != null) {
            mapperPerField.forEach((field, mapper) -> mapper.back(targetRawEntity, ((ParameterizedEntityField<V>) field).get(sourceNativeEntity)));
        }
        return targetRawEntity;
    }

    @Override
    public V read(String key) {
        final Map<String, Object> idMap = idFieldMapper.apply(key);
        if (idMap == null || idMap.isEmpty()) {
            return null;
        }
        return createMappingDelegate(readRaw(idMap));
    }

    public abstract R readRaw(Map<String, Object> key);

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        SearchableFieldTranslator<?, M> sft = new SearchableFieldTranslator<>(mapperPerField);
        final QueryParameters<M> transformedParameters = queryParameters.transform(sft);

        // TODO: check sanity of the AST (cannot mix partial and proper nodes inside an OR node)

        final QueryParameters<M> partialParameters = transformedParameters.restrictToPartial();
        if (partialParameters.getModelCriteriaBuilder().isAlwaysFalse()) {
            return Stream.empty();
        }

        final QueryParameters<M> remainderParameters = transformedParameters.restrictToRemainder();
        if (remainderParameters.getModelCriteriaBuilder().isAlwaysFalse()) {
            return Stream.empty();
        }

        final Stream<R> rawStream = readRaw(partialParameters);
        final Stream<V> mappedStream = rawStream.map(this::createMappingDelegate);

        if (remainderParameters.getModelCriteriaBuilder().isAlwaysTrue()) {
            return mappedStream;
        }

        return StreamWithPostFiltering.with(mappedStream, remainderParameters, entityClass);
    }

    public abstract Stream<R> readRaw(QueryParameters<M> queryParametersForRawRead);

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
//        return tx.getCount(queryParameters);
        throw new IllegalStateException("Method not implemented");
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    protected V createMappingDelegate(R entity) {
        if (entity == null) {
            return null;
        }
        return MappingEntityFieldDelegate.<V, R>delegate(mapperPerField, entity, entityClass, Completeness.PARTIAL);
    }

}
