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

/**
 *
 * @author hmlnarik
 */
class EmptyMapper<T> implements Mapper<T> {

    private static final EmptyMapper<?> INSTANCE = new EmptyMapper<>();

    @SuppressWarnings(value = "unchecked")
    static <T> EmptyMapper<T> instance() {
        return (EmptyMapper<T>) INSTANCE;
    }

    @Override
    public Object there(T object) {
        return null;
    }

    @Override
    public Class<?> getThereClass() {
        return Void.class;
    }

    @Override
    public void back(T object, Object value) {
    }

    @Override
    public Map<String, Object> backToMap(Object value) {
        return Collections.emptyMap();
    }
}
