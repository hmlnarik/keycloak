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

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author hmlnarik
 */
public class MapperCastUtils {

    /**
     * Returns a function
     * @param <T>
     * @param ter
     * @param fieldClass
     * @param thereClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T, Object> thereCast(Function<T, Object> ter, Class<?> fieldClass, Class<?> thereClass) {
        if (Objects.equals(fieldClass, thereClass) || thereClass == Object.class) {
            return ter;
        }
        return ter.andThen((Function<? super Object, ? extends T>) getCastFunc(fieldClass, thereClass));
    }

    public static <T> BiConsumer<T, Object> backCast(BiConsumer<T, Object> bkConsumer, Class<?> thereClass, Class<?> fieldClass) {
        if (Objects.equals(fieldClass, thereClass) || fieldClass == Object.class) {
            return bkConsumer;
        }
        @SuppressWarnings("unchecked")
        final Function<? super Object, Object> castFunc = (Function<? super Object, Object>) getCastFunc(thereClass, fieldClass);
        return (t, o) -> bkConsumer.accept(t, castFunc.apply(o));
    }

    public static Object cast(Object value, Class<?> toClass) {
        return value == null ? null : ((Function) getCastFunc(value.getClass(), toClass)).apply(value);
    }

    public static Function<?, ?> getCastFunc(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == toClass) {
            return Function.identity();
        }
        if (toClass == String.class) {
            return Objects::toString;
        }
        if (fromClass == String.class) {
            if (toClass == Integer.class) {
                return (String s) -> Integer.valueOf(s);
            } else if (toClass == Long.class) {
                return (String s) -> Long.valueOf(s);
            } else if (toClass == Boolean.class) {
                return (String s) -> Boolean.valueOf(s);
            }
        }
        if (fromClass == Long.class) {
            if (toClass == Integer.class) {
                return (Long l) -> l.intValue();
            }
        }
        if (fromClass == Integer.class) {
            if (toClass == Long.class) {
                return (Integer l) -> l.longValue();
            }
        }

        throw new IllegalStateException("Unknown cast: " + fromClass + " -> " + toClass);
    }

}
