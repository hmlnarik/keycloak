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

import java.util.Collections;
import java.util.Map;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.cast;

/**
 *
 * @author hmlnarik
 */
public class ConstantMapper<T> implements Mapper<T> {

    private final Object value;
    private final Class<?> thereClass;

    @SuppressWarnings("unchecked")
    public ConstantMapper(Object value, Class<?> thereClass) {
        this.value = cast(value, thereClass);
        this.thereClass = thereClass;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Object there(T object) {
        return value;
    }

    @Override
    public Class<?> getThereClass() {
        return thereClass;
    }

    @Override
    public void back(T object, Object value) {
    }

    @Override
    public Map<String, Object> backToMap(Object value) {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return "constant[" + value + "]::" + thereClass;
    }
}
