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
package org.keycloak.models.map.storage.criteria;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.chm.CriteriaOperator;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode.AtomicFormulaInstantiator;
import org.keycloak.models.map.storage.mapper.ConstantMapper;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.keycloak.storage.SearchableModelField;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.cast;

/**
 *
 * @author hmlnarik
 */
public class SearchableFieldTranslator<R extends AbstractEntity, M> implements AtomicFormulaInstantiator<M> {

    protected final MappersMap<?, R> mapperPerField;

    public SearchableFieldTranslator(MappersMap<?, R> mapperPerField) {
        this.mapperPerField = mapperPerField;
    }

    static <M, V extends AbstractEntity> Optional<ParameterizedEntityField<V>> translateSearchableField(SearchableModelField<M> field, Object... parameters) {
        return ModelEntityUtil.fromSearchableField(field, parameters);
    }

    @Override
    public ModelCriteriaNode<M> instantiateAtomicFormula(ModelCriteriaNode<M> node) {
        return instantiateAtomicFormula(node.field, node.getSimpleOperator(), node.getSimpleOperatorArguments());
    }

    protected ModelCriteriaNode<M> instantiateAtomicFormula(SearchableModelField<? super M> field, Operator op, Object... operatorArguments) {
        Optional<ParameterizedEntityField<AbstractEntity>> pefOpt = translateSearchableField(field, operatorArguments);
        if (! pefOpt.isPresent()) {
            return ModelCriteriaNode.atomicFormula(field, op, operatorArguments);
        }
        ParameterizedEntityField<?> pef = pefOpt.orElse(null);

        Mapper<? super R> mapper = mapperPerField.get(pef);
        if (mapper == null) {
            return ModelCriteriaNode.atomicFormula(field, op, operatorArguments);
        }

        return translateAtomicFormula(field, op, mapper, pef.isParameterized() ? 1 : 0, operatorArguments);
    }

    public static <M, R> ModelCriteriaNode<M> translateAtomicFormula(SearchableModelField<? super M> field, Operator op, Mapper<R> mapper, int parameterStartIndex, Object[] parameters) {
        Object[] param;
        Map<String, Object> paramBackMap;

        switch (op) {
            case EQ:
            case NE:
            case LE:
            case LT:
            case GE:
            case GT:
            case LIKE:
            case ILIKE:
                param = new Object[] { parameters[parameterStartIndex] };
                if (mapper instanceof ConstantMapper) {
                    return evaluateDirectly((ConstantMapper) mapper, op, param);
                }
                paramBackMap = mapper.backToMap(param[0]);
                if (paramBackMap == null || paramBackMap.isEmpty()) {
                    return op == Operator.NE ? ModelCriteriaNode.trueNode() : ModelCriteriaNode.falseNode();
                }
                return new PartialModelCriteriaNode<>(field, op, paramBackMap);

            case EXISTS:
            case NOT_EXISTS:
                if (mapper instanceof ConstantMapper) {
                    return evaluateDirectly((ConstantMapper) mapper, op, null);
                }
                paramBackMap = mapper.backToMap(null);
                if (paramBackMap == null || paramBackMap.isEmpty()) {
                    return op == Operator.NOT_EXISTS ? ModelCriteriaNode.trueNode() : ModelCriteriaNode.falseNode();
                }
                return new PartialModelCriteriaNode<>(field, op, paramBackMap);

            case IN:
                param = new Object[parameters.length - parameterStartIndex];
                if (mapper instanceof ConstantMapper) {
                    System.arraycopy(parameters, parameterStartIndex, param, 0, parameters.length - parameterStartIndex);
                    return evaluateDirectly((ConstantMapper) mapper, op, param);
                }
                for (int i = parameterStartIndex; i < parameters.length; i ++) {
                    param[i - parameterStartIndex] = mapper.backToMap(parameters[i]);
                }
                return new PartialModelCriteriaNode<>(field, op, param);
        }

        throw new IllegalArgumentException("Unknown operator " + op);
    }

    private static <M> ModelCriteriaNode<M> evaluateDirectly(ConstantMapper mapper, Operator op, Object[] param) {
        Object value = mapper.getValue();
        if (value != null && param != null && param.length > 0 && param[0] != null) {
            value = cast(value, param[0].getClass());
        }
        Predicate<Object> predicate = CriteriaOperator.predicateFor(op, param);
        return predicate.test(value) ? ModelCriteriaNode.trueNode() : ModelCriteriaNode.falseNode();
    }

}
