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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.keycloak.models.map.storage.tree.PartialMappingTransaction;
import org.keycloak.storage.SearchableModelField;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class DictStorage<V extends AbstractEntity, M> implements MapStorage.Partial<V, M, Dict> {

    private static final Logger LOG = Logger.getLogger(DictStorage.class);

    private final List<Dict> store;

    private final Supplier<Dict> dictCreator;

    private final Class<V> entityClass;

    private MappersMap<V, Dict> mapperPerField;

    private MappersMap<V, Dict> contextMappers;

    public DictStorage(Class<V> entityClass, List<Dict> store, Supplier<Dict> dictNewInstanceCreator) {
        this.store = store;
        this.dictCreator = dictNewInstanceCreator;
        this.entityClass = entityClass;
    }

    List<Dict> getStore() {
        return store;
    }

    private static final Predicate<Dict> ALWAYS_TRUE = d -> true;

    private static class DictModelCriteriaBuilder<M> implements ModelCriteriaBuilder<M, DictModelCriteriaBuilder<M>> {

        private final Predicate<Dict> dictReadFilter;

        private DictModelCriteriaBuilder() {
            this(ALWAYS_TRUE);
        }

        private DictModelCriteriaBuilder(Predicate<Dict> dictReadFilter) {
            this.dictReadFilter = dictReadFilter;
        }

        private DictModelCriteriaBuilder<M> thisAnd(Predicate<Dict> additionalDictFilter) {
            return new DictModelCriteriaBuilder<>(additionalDictFilter == null ? this.dictReadFilter : this.dictReadFilter.and(additionalDictFilter));
        }

        @Override
        @SuppressWarnings("unchecked")
        public DictModelCriteriaBuilder<M> compare(SearchableModelField<? super M> modelField, Operator op, Object... params) {
            Predicate<Dict> thisOp = null;
            Map<String, Object> nativeFieldValues;
            switch (op) {
                case EQ:
                    nativeFieldValues = (Map<String, Object>) params[0];
                    Objects.requireNonNull(nativeFieldValues, "Could not translate predicate for " + modelField);
                    thisOp = nativeFieldValues.entrySet().stream().map(me -> {
                        String key = me.getKey();
                        Object value = me.getValue();
                        return (Predicate<Dict>) ((Dict d) -> Objects.equals(d.get(key), value));
                    }).reduce(Predicate::and).orElse(null);
                    break;
                case NE:
                    nativeFieldValues = (Map<String, Object>) params[0];
                    Objects.requireNonNull(nativeFieldValues, "Could not translate predicate for " + modelField);
                    thisOp = nativeFieldValues.entrySet().stream().map(me -> {
                        String key = me.getKey();
                        Object value = me.getValue();
                        return (Predicate<Dict>) ((Dict d) -> ! Objects.equals(d.get(key), value));
                    }).reduce(Predicate::or).orElse(null);
                    break;
                default:
                    throw new CriterionNotSupportedException(modelField, op);
            }
            return thisAnd(thisOp);
        }

        public Predicate<Dict> getDictReadFilter() {
            return dictReadFilter;
        }

        @Override
        @SafeVarargs
        public final DictModelCriteriaBuilder<M> and(DictModelCriteriaBuilder... builders) {
            @SuppressWarnings("unchecked")
            Predicate<Dict> filter = Stream.of(builders).map(DictModelCriteriaBuilder.class::cast).map(DictModelCriteriaBuilder::getDictReadFilter).filter(p -> p != ALWAYS_TRUE).reduce(this.dictReadFilter, Predicate::and);
            return new DictModelCriteriaBuilder<>(filter);
        }

        @Override
        @SafeVarargs
        public final DictModelCriteriaBuilder<M> or(DictModelCriteriaBuilder... builders) {
            @SuppressWarnings("unchecked")
            Predicate<Dict> filter = Stream.of(builders).map(DictModelCriteriaBuilder.class::cast).map(DictModelCriteriaBuilder::getDictReadFilter).filter(p -> p != ALWAYS_TRUE).reduce(this.dictReadFilter, Predicate::or);
            return new DictModelCriteriaBuilder<>(filter);
        }

        @Override
        public DictModelCriteriaBuilder<M> not(DictModelCriteriaBuilder builder) {
            return new DictModelCriteriaBuilder<>(this.dictReadFilter.negate());
        }
    }

    private final class Transaction extends PartialMappingTransaction<V, M, Dict> {

        private boolean rollbackOnly = false;
        private boolean active = false;

        public Transaction(MappersMap<V, Dict> mapperPerField) {
            super(DictStorage.this.entityClass, mapperPerField);
        }

        @Override
        public Dict createRaw(V nativeValue) {
            return toRaw(nativeValue, dictCreator.get());
        }

        @Override
        public Dict readRaw(Map<String, Object> key) {
            Predicate<Dict> dataFilter = key.entrySet().stream()
              .map(me -> ((Predicate<Dict>) (Dict e) -> Objects.equals(e.get(me.getKey()), me.getValue())))
              .reduce(a -> true, Predicate::and);

            return store.stream()
              .filter(dataFilter)
              .findFirst()
              .orElse(null);
        }

        @Override
        public Stream<Dict> readRaw(QueryParameters<M> queryParameters) {
            LOG.debugf("read(%s)", queryParameters);
            DefaultModelCriteria<M> criteria = queryParameters.getModelCriteriaBuilder();
            if (criteria == null) {
                return Stream.empty();
            }

            DictModelCriteriaBuilder<M> mcb = criteria.flashToModelCriteriaBuilder(new DictModelCriteriaBuilder<>());
            return store.stream().filter(mcb.getDictReadFilter());
        }

        @Override
        public long getCount(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean delete(String key) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long delete(QueryParameters<M> queryParameters) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void begin() {
            this.active = true;
        }

        @Override
        public void commit() {
        }

        @Override
        public void rollback() {
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public boolean getRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        return new Transaction(this.contextMappers == null ? this.mapperPerField : this.contextMappers.overriddenWith(this.mapperPerField));
    }

    @Override
    public void setMappers(MappersMap<V, Dict> mapperPerField) {
        this.mapperPerField = mapperPerField;
    }

    @Override
    public void setContextMappers(MappersMap<V, Dict> contextMappers) {
        this.contextMappers = contextMappers;
    }
}
