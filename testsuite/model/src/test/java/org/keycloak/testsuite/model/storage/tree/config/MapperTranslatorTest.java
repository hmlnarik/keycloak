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
package org.keycloak.testsuite.model.storage.tree.config;

import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.junit.Test;
import org.keycloak.models.map.storage.tree.config.MapperTranslator;
import org.keycloak.testsuite.model.KeycloakModelTest;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class MapperTranslatorTest extends KeycloakModelTest {

    @Test
    public void testParse() {
        Map<String, String> simpleConfig = new HashMap<>();
        simpleConfig.put("attributes.original", "original:{id}");
        simpleConfig.put("clientId", "client:{id}");
        MapperTranslator mt = new MapperTranslator(getFactory());
        mt.parse(MapClientEntity.class, simpleConfig, new ConcurrentHashMapStorageProviderFactory());
    }

}
