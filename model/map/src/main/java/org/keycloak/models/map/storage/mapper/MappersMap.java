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
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode;
import org.keycloak.storage.SearchableModelField;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public class MappersMap<V extends AbstractEntity, R> extends IdentityHashMap<ParameterizedEntityField<?>, Mapper<R>> {

    public MappersMap() {
    }

    private MappersMap(MappersMap<V, R> other) {
        super(other);
    }

    public MappersMap(EntityField<? extends V> field, Mapper<R> mapper) {
        add(field, mapper);
    }

    public MappersMap(EntityField<? extends V> field, String fieldParameter, Mapper<R> mapper) {
        add(field, fieldParameter, mapper);
    }

    public final MappersMap<V, R> add(EntityField<? extends V> field, Mapper<R> mapper) {
        put(ParameterizedEntityField.from(field), mapper);
        return this;
    }

    public final MappersMap<V, R> add(EntityField<? extends V> field, String fieldParameter, Mapper<R> mapper) {
        put(ParameterizedEntityField.from(field, fieldParameter), mapper);
        return this;
    }

    public <M> ModelCriteriaNode<M> evaluate(ModelCriteriaNode<M> node) {
        if (node.getSimpleOperator() != Operator.EQ) {
            return node.cloneNode();
        }
        Object parameter = node.getSimpleOperatorArguments()[0];

        SearchableModelField<? super M> searchableField = node.getField();
        Optional<Mapper<R>> om = ModelEntityUtil.fromSearchableField(searchableField, node.getSimpleOperatorArguments()).map(this::get);
        if (om.isPresent()) {
            Mapper<R> m = om.orElse(null);
            if (m instanceof ConstantMapper) {
                return Objects.equals(((ConstantMapper) m).getValue(), parameter) ? ModelCriteriaNode.trueNode() : node.cloneNode();
            }
        }
        return node.cloneNode();
    }

    public Set<ParameterizedEntityField<?>> getPrimarySourceFields() {
        return keySet();
    }

    public MappersMap<V, R> overriddenWith(MappersMap<V, R> other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        MappersMap<V, R> res = new MappersMap<>(this);
        res.putAll(other);
        return res;
    }
}
