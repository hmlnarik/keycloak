/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap.role;

import org.keycloak.models.RoleModel;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.LdapMapQuery;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.SearchableModelField;

public class LdapRoleModelCriteriaBuilder<Self extends ModelCriteriaBuilder<RoleModel, Self>> extends LdapModelCriteriaBuilder<LdapRoleEntity, RoleModel, Self> {

    private LdapMapQuery query = new LdapMapQuery();

    public LdapRoleModelCriteriaBuilder(Object o) {
        super();
    }

    @SafeVarargs
    @Override
    public final Self and(Self... builders) {
        return (Self) this;
    }

    @SafeVarargs
    @Override
    public final Self or(Self... builders) {
        return (Self) this;
    }

    @Override
    public Self not(Self builder) {
        return (Self) this;
    }

    @Override
    public Self compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        return (Self) this;
    }
}
