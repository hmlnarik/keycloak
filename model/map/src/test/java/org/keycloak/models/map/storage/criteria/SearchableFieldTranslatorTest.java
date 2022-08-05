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

import org.keycloak.models.ClientModel;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.junit.Test;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.DIRECT_MAPPER;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.INTEGER_CONSTANT;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.INTEGER_CONSTANT_MAPPER;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.INTEGER_CONSTANT_ST;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.NAME_NATIVE_STRING_FIELD;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.STRING_CONSTANT_MAPPER;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.STRING_EXPRESSION_MAPPER;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.assertFalseNode;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.assertMCNode;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.assertPNode;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.assertTrueNode;

/**
 *
 * @author hmlnarik
 */
public class SearchableFieldTranslatorTest {

    @Test
    public void testIntegerConstant() {
        final MappersMap<MapClientEntity, MapClientEntity> mappers = new MappersMap<>(MapClientEntityFields.CLIENT_ID, INTEGER_CONSTANT_MAPPER);
        SearchableFieldTranslator<MapClientEntity, ClientModel> t = new SearchableFieldTranslator<>(mappers);

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.IN, INTEGER_CONSTANT_ST, INTEGER_CONSTANT_ST + "aa", "aa" + INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.IN, INTEGER_CONSTANT_ST + "aa", "aa" + INTEGER_CONSTANT_ST));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EXISTS));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NOT_EXISTS));

        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT + 3));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT + 3));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT + 3));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "%56"));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "123%"));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "a%"));
    }

    @Test
    public void testStringConstant() {
        final MappersMap<MapClientEntity, MapClientEntity> mappers = new MappersMap<>(MapClientEntityFields.CLIENT_ID, STRING_CONSTANT_MAPPER);
        SearchableFieldTranslator<MapClientEntity, ClientModel> t = new SearchableFieldTranslator<>(mappers);

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT_ST));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.IN, INTEGER_CONSTANT_ST, INTEGER_CONSTANT_ST + "aa", "aa" + INTEGER_CONSTANT_ST));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.IN, INTEGER_CONSTANT_ST + "aa", "aa" + INTEGER_CONSTANT_ST));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EXISTS));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NOT_EXISTS));

        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LE, INTEGER_CONSTANT + 3));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LT, INTEGER_CONSTANT + 3));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GE, INTEGER_CONSTANT + 3));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.GT, INTEGER_CONSTANT + 3));

        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "%56"));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "123%"));
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.LIKE, "a%"));

    }

    @Test
    public void testStringExpression() {
        final MappersMap<MapClientEntity, MapClientEntity> mappers = new MappersMap<>(MapClientEntityFields.CLIENT_ID, STRING_EXPRESSION_MAPPER);
        SearchableFieldTranslator<MapClientEntity, ClientModel> t = new SearchableFieldTranslator<>(mappers);

        assertPNode    (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, "client-14"), Operator.EQ, NAME_NATIVE_STRING_FIELD, "14");
        assertFalseNode(t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, "a-client-14"));
        assertTrueNode (t.instantiateAtomicFormula(ClientModel.SearchableFields.CLIENT_ID, Operator.NE, "a-client-14"));
    }

    @Test
    public void testNonexistentField() {
        final MappersMap<MapClientEntity, MapClientEntity> mappers = new MappersMap<>(MapClientEntityFields.CLIENT_ID, STRING_EXPRESSION_MAPPER)
          .add(MapClientEntityFields.ATTRIBUTES, "attributeName", DIRECT_MAPPER);

        SearchableFieldTranslator<MapClientEntity, ClientModel> t = new SearchableFieldTranslator<>(mappers);

        assertMCNode   (t.instantiateAtomicFormula(ClientModel.SearchableFields.ATTRIBUTE, Operator.EQ, "nonexistentAttributeName", "client-14"),
          Operator.EQ, "nonexistentAttributeName", "client-14");
        assertPNode    (t.instantiateAtomicFormula(ClientModel.SearchableFields.ATTRIBUTE, Operator.EQ, "attributeName", "client-14"),
          Operator.EQ, NAME_NATIVE_STRING_FIELD, "client-14");
    }

}
