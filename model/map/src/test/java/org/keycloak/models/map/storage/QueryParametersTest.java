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
package org.keycloak.models.map.storage;

import org.keycloak.models.ClientModel;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.criteria.SearchableFieldTranslator;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.junit.Test;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;
import static org.keycloak.models.map.storage.criteria.MappersTestUtil.DIRECT_MAPPER;

/**
 *
 * @author hmlnarik
 */
public class QueryParametersTest {

    @Test
    public void testSimpleQueryParameters() {
        final MappersMap<MapClientEntity, MapClientEntity> mappers = new MappersMap<>(MapClientEntityFields.CLIENT_ID, DIRECT_MAPPER);
        SearchableFieldTranslator<MapClientEntity, ClientModel> t = new SearchableFieldTranslator<>(mappers);

        QueryParameters<ClientModel> qp = new QueryParameters<>(
          DefaultModelCriteria.<ClientModel>criteria()
            .compare(ClientModel.SearchableFields.CLIENT_ID, Operator.EQ, "client-1")
            .compare(ClientModel.SearchableFields.REALM_ID, Operator.EQ, "1")
        ).transform(t);

        qp.restrictToPartial();
        qp.restrictToRemainder();
    }

}
