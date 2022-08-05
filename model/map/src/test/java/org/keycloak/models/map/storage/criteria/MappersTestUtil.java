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

import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode.ExtOperator;
import org.keycloak.models.map.storage.tree.config.MapperTranslator.NativeEntityFieldAccessors;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.TemplateMapper;
import java.util.IdentityHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 * @author hmlnarik
 */
public class MappersTestUtil {

    private static final NativeEntityFieldAccessors<MapClientEntity> NATIVE_ACCESSORS_CLIENT = NativeEntityFieldAccessors.forClass(MapClientEntity.class);

    public static final int INTEGER_CONSTANT = 123456;
    public static final String INTEGER_CONSTANT_ST = String.valueOf(INTEGER_CONSTANT);
    public static final String NAME_NATIVE_STRING_FIELD = MapClientEntityFields.NAME.getNameCamelCase();

    public static final Mapper<MapClientEntity> INTEGER_CONSTANT_MAPPER = TemplateMapper.forTemplate(INTEGER_CONSTANT_ST, NATIVE_ACCESSORS_CLIENT, Integer.class);
    public static final Mapper<MapClientEntity> STRING_CONSTANT_MAPPER = TemplateMapper.forTemplate(INTEGER_CONSTANT_ST, NATIVE_ACCESSORS_CLIENT, String.class);
    public static final Mapper<MapClientEntity> STRING_EXPRESSION_MAPPER = TemplateMapper.forTemplate("client-{" + NAME_NATIVE_STRING_FIELD + "}", NATIVE_ACCESSORS_CLIENT, String.class);
    public static final Mapper<MapClientEntity> DIRECT_MAPPER = TemplateMapper.forTemplate("{" + NAME_NATIVE_STRING_FIELD + "}", NATIVE_ACCESSORS_CLIENT, String.class);


    public static void assertTrueNode(ModelCriteriaNode<?> node) {
        assertThat(node, is(ModelCriteriaNode.trueNode()));
    }

    public static void assertFalseNode(ModelCriteriaNode<?> node) {
        assertThat(node, is(ModelCriteriaNode.falseNode()));
    }

    public static void assertPNode(ModelCriteriaNode<?> node, Operator op, Object... keyValues) {
        assertThat(node, notNullValue());
        assertThat(node.getNodeOperator(), is(ExtOperator.ATOMIC_FORMULA));
        assertThat(node.getSimpleOperator(), is(op));
        assertThat(node, instanceOf(PartialModelCriteriaNode.class));

        PartialModelCriteriaNode<?> pmcn = (PartialModelCriteriaNode<?>) node;

        for (int i = 0; i < keyValues.length - 1; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            assertThat(pmcn.getFirstArgument(), hasEntry(key, value));
        }
        assertThat(pmcn.getFirstArgument().size(), is(keyValues.length / 2));
    }

    public static void assertMCNode(ModelCriteriaNode<?> node, Operator op, Object... values) {
        assertThat(node, notNullValue());
        assertThat(node.getNodeOperator(), is(ExtOperator.ATOMIC_FORMULA));
        assertThat(node.getSimpleOperator(), is(op));
        assertThat(node, not(instanceOf(PartialModelCriteriaNode.class)));

        assertThat(node.simpleOperatorArguments, is(values));
    }

}
