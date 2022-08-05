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

import org.keycloak.models.map.storage.mapper.MapperProviderFactory.MapperFieldDescriptor;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.backCast;
import static org.keycloak.models.map.storage.mapper.MapperCastUtils.thereCast;

/**
 *
 * @author hmlnarik
 */
public class DirectMapper<T> implements Mapper<T> {

    private final String originalExpression;
    private final Function<T, Object> th;
    private final BiConsumer<T, Object> ba;
    private final Function<Object, Map> baMap;
    private final Class<?> thereClass;

    public DirectMapper(String originalExpr, MapperFieldDescriptor<T> originatingFieldDescriptor, Class<?> thereClass) {
        this.originalExpression = originalExpr;
        this.th = Optional.ofNullable(originatingFieldDescriptor.fieldGetter())
          .map(getter -> thereCast(getter, originatingFieldDescriptor.getFieldClass(), thereClass))
          .orElse(object -> null);
        this.ba = Optional.ofNullable(originatingFieldDescriptor.fieldSetter())
          .map(setter -> backCast(setter, thereClass, originatingFieldDescriptor.getFieldClass()))
          .orElse((object, v) -> {});
        
        Function<Object, ?> castFunc = (Function<Object, ?>) MapperCastUtils.getCastFunc(thereClass, originatingFieldDescriptor.getFieldClass());
        this.baMap = Optional.ofNullable(originatingFieldDescriptor.fieldSetter())
          .map(setter -> (Function<Object, Map>) (v -> Collections.singletonMap(originalExpr, castFunc.apply(v))))
          .orElse(object -> Collections.emptyMap());
        this.thereClass = thereClass;
    }

    @Override
    public Object there(T object) {
        return th.apply(object);
    }

    @Override
    public Class<?> getThereClass() {
        return thereClass;
    }

    @Override
    public void back(T object, Object value) {
        ba.accept(object, value);
    }

    @Override
    public Map<String, Object> backToMap(Object value) {
        return baMap.apply(value);
    }

    public String getOriginalExpression() {
        return originalExpression;
    }

    @Override
    public String toString() {
        return "direct mapper[expression=" + originalExpression + "]::" + thereClass;
    }
}
