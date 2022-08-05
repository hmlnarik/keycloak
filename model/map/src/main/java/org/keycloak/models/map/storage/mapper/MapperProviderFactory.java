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

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.provider.ProviderFactory;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author hmlnarik
 */
public interface MapperProviderFactory extends ProviderFactory<MapperProvider> {

    /**
     * Descriptor of a field usable by a {@link Mapper}, specifically for getting / setting mapped field value and retrieving its class.
     * @param <R> Raw object type
     */
    public interface MapperFieldDescriptor<R> {
        /**
         * Returns a method which, given an object of raw type {@code R}, returns a value of the field described by this descriptor.
         */
        Function<R, Object> fieldGetter();

        /**
         * Returns a method which takes two parameters - an object of raw type {@code R} and a value -
         * and upon invocation sets the value of the field described by this descriptor to the given parameter.
         */
        BiConsumer<R, Object> fieldSetter();

        /**
         * Returns the class of the field described by this descriptor.
         * @return
         */
        Class<?> getFieldClass();
    }

    /**
     * Type of a function which returns a mapper field descriptor for a given field name in the object of type {@code R}.
     * @param <R> Raw entity type
     */
    @FunctionalInterface
    public interface FieldDescriptorGetter<R> {
        /**
         * Returns a mapper field descriptor for a given field name.
         * @param fieldName name of the field in {@code camelCase}.
         */
        MapperFieldDescriptor<R> getFieldDescriptor(String fieldName);
    }

    /**
     * Returns a {@link Mapper} for the area configured according to the given config.
     * @param <R>
     * @param entityClass Area class, one of the {@code Map*Entity} classes.
     * @param thereFieldClass Type of the object which has to be returned by the {@link Mapper#there} method of the resulting mapper
     * @param config Configuration. This might be as complex as needed. If the mapper is configured by a single string,
     *               it is stored in the map under the {code template} key, e.g. <code>{ "template": "{client.id}" }</code>.
     * @param descriptorGetterForFieldName Method for obtaining field descriptors of the object which
     *               is used for computing the final mapper output. This object is the object of type {@code R}
     *               in the diagram in {@link Mapper} interface.
     * @return A mapper instance, or {@code null} if a mapper cannot be instantiated for the given configuration.
     *
     * @see ModelEntityUtil#getEntityType(java.lang.Class) for available {@code entityClass} values
     */
    <R> Mapper<R> forConfig(
      Class<? extends AbstractEntity> entityClass,
      Class<?> thereFieldClass,
      Map<String, Object> config,
      FieldDescriptorGetter<?> descriptorGetterForFieldName
    );

}
