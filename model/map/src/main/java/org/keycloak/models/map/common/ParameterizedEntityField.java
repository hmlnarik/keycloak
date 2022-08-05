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
package org.keycloak.models.map.common;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author hmlnarik
 */
public class ParameterizedEntityField<E> implements EntityField<E> {

    private final EntityField<E> entityField;

    private final Object parameter;

    private static final Map<EntityField, ParameterizedEntityField> directField = new IdentityHashMap<>();
    private static final Map<EntityField, Map<Object, ParameterizedEntityField>> paramField = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public static <E> ParameterizedEntityField<E> from(EntityField<E> entityField) {
        return entityField instanceof ParameterizedEntityField
          ? (ParameterizedEntityField<E>) entityField
          : directField.computeIfAbsent(entityField, ParameterizedEntityField::new);
    }

    @SuppressWarnings("unchecked")
    public static <E> ParameterizedEntityField<E> from(EntityField<E> entityField, Object parameter) {
        return (parameter == null || Objects.equals("", parameter))
          ? from(entityField)
          : paramField
              .computeIfAbsent(from(entityField), ef -> new HashMap<>())
              .computeIfAbsent(parameter, p -> new ParameterizedEntityField(entityField, parameter));
    }

    private ParameterizedEntityField(EntityField<E> entityField) {
        this(entityField, null);
    }

    private ParameterizedEntityField(EntityField<E> entityField, Object parameter) {
        this.entityField = entityField;
        this.parameter = parameter;
    }

    @Override
    public String getName() {
        return this.entityField.getName() + (isParameterized() ? "." + this.parameter : "");
    }

    @Override
    public String getNameCamelCase() {
        return this.entityField.getNameCamelCase() + (isParameterized() ? "." + this.parameter : "");
    }

    @Override
    public Object get(E entity) {
        return this.parameter == null
          ? this.entityField.get(entity)
          : this.entityField.mapGet(entity, this.parameter);
    }

    @Override
    public <T> void set(E entity, T value) {
        if (this.parameter == null) {
            this.entityField.set(entity, value);
        } else {
            this.entityField.mapPut(entity, this.parameter, value);
        }
    }

    @Override
    public Class<?> getFieldClass() {
        if (this.parameter == null) {
            return this.entityField.getFieldClass();
        } else {
            return this.entityField.getMapValueClass();
        }
    }

    public ParameterizedEntityField<E> withoutParameter() {
        return from(this.entityField);
    }

    public boolean isParameterized() {
        return this.parameter != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityField, this.parameter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ParameterizedEntityField<?> other = (ParameterizedEntityField<?>) obj;
        return Objects.equals(this.entityField, other.entityField) && Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public String toString() {
        return entityField + (parameter == null ? "" : "[" + parameter + "]");
    }

    public EntityField<E> getEntityField() {
        return entityField;
    }

    public Object getParameter() {
        return parameter;
    }

}
