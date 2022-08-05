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
package org.keycloak.testsuite.model.storage.tree.sample;

import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author hmlnarik
 */
public class DictMapperFieldDescriptor implements MapperFieldDescriptor<Dict> {

    private final String fieldName;

    public DictMapperFieldDescriptor(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public Function<Dict, Object> fieldGetter() {
        return d -> d.get(fieldName);
    }

    @Override
    public BiConsumer<Dict, Object> fieldSetter() {
        return (d, value) -> d.put(fieldName, value);
    }

    @Override
    public Class<?> getFieldClass() {
        return Object.class;
    }

}
