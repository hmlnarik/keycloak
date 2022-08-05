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

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.storage.SearchableModelField;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author hmlnarik
 */
public class PartialModelCriteriaNode<M> extends ModelCriteriaNode<M> {

    public Map<String,Object> getFirstArgument() {
        return this.simpleOperatorArguments != null && this.simpleOperatorArguments.length > 0
          ? (Map<String,Object>) this.simpleOperatorArguments[0]
          : null;
    }

    public PartialModelCriteriaNode(SearchableModelField<? super M> field, Operator op, Map<String,Object> parameterValues) {
        super(field, op, new Object[] { parameterValues });
    }

    public PartialModelCriteriaNode(SearchableModelField<? super M> field, Operator op, Object[] parameters) {
        super(field, op, makeListFrom(parameters));
    }

    private static Object[] makeListFrom(Object[] parameters) {
        Object[] res = new Object[parameters.length];

        for (int i = 0; i < res.length; i ++) {
            Object parameter = parameters[i];

            if (! (parameter instanceof Map)) {
                throw new IllegalArgumentException("Invalid parameter, must be a Map");
            }
            res[i] = (Map) parameter;
        }

        return res;
    }

    @Override
    public ModelCriteriaNode<M> cloneNode() {
        if (this.nodeOperator == ExtOperator.ATOMIC_FORMULA) {
            return new PartialModelCriteriaNode<>(this.field, this.getSimpleOperator(), this.getSimpleOperatorArguments());
        } else {
            return new ModelCriteriaNode<>(this.nodeOperator);
        }
    }

}
