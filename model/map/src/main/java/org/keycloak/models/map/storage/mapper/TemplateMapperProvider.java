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

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.common.AbstractEntity;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class TemplateMapperProvider implements MapperProvider, MapperProviderFactory {

    public static final String ID = "template";

    private static final String CONFIG_TEMPLATE = "template";

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mapper<R> forConfig(
      Class<? extends AbstractEntity> entityClass, Class<?> thereFieldClass, Map<String, Object> config,
      FieldDescriptorGetter<?> descriptorForFieldName
    ) {
        // TODO: allow for better configurability
        return TemplateMapper.forTemplate((String) config.get(CONFIG_TEMPLATE), (FieldDescriptorGetter<R>) descriptorForFieldName, thereFieldClass);
    }

    @Override
    public void close() {
    }

    @Override
    public MapperProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return ID;
    }

}
