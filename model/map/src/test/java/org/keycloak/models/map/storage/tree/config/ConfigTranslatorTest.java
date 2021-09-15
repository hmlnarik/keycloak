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
package org.keycloak.models.map.storage.tree.config;

import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.models.map.storage.tree.TreeStorageProviderFactory.DEBUG_STORAGE_CONFIG;

/**
 *
 * @author hmlnarik
 */
public class ConfigTranslatorTest {

    @Test
    public void testSimpleConfig() throws IOException {
        Map<String, Object> file = new Yaml().load(DEBUG_STORAGE_CONFIG);
        final List<Object> stores = ConfigTranslator.getList(file, "stores");
        assertThat(stores, hasSize(1));
        Map<String, Object> firstItem = ConfigTranslator.streamOfMaps(stores.stream()).findFirst().orElse(null);
        assertThat(firstItem, hasKey(ConcurrentHashMapStorageProviderFactory.PROVIDER_ID));
    }

}
