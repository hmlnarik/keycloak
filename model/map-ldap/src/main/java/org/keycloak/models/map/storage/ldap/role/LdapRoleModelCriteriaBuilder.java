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
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.EqualCondition;
import org.keycloak.storage.ldap.idm.query.internal.NoopCondition;
import org.keycloak.storage.ldap.idm.query.internal.NotCondition;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

import java.util.function.Function;

public class LdapRoleModelCriteriaBuilder extends LdapModelCriteriaBuilder<LdapRoleEntity, RoleModel, LdapRoleModelCriteriaBuilder> {

    public LdapRoleModelCriteriaBuilder() {
        super(LdapRoleModelCriteriaBuilder::new);
    }

    private LdapRoleModelCriteriaBuilder(Function<RoleMapperConfig, Condition> predicateFunc) {
        super(LdapRoleModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public LdapRoleModelCriteriaBuilder compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField.equals(RoleModel.SearchableFields.REALM_ID) ||
                        modelField.equals(RoleModel.SearchableFields.IS_CLIENT_ROLE) ||
                        modelField.equals(RoleModel.SearchableFields.CLIENT_ID)) {
                    // don't filter by realm, as the LDAP directory is specific to the realm already
                    // TODO: find out how to handle clientroles
                    return new LdapRoleModelCriteriaBuilder(config -> new NoopCondition());
                } else if (modelField.equals(RoleModel.SearchableFields.NAME)) {
                    // validateValue(value, modelField, op, String.class);
                    // handle client IDs ...
                    return new LdapRoleModelCriteriaBuilder((config) -> {
                        String field = modelFieldNameToLdap(config, modelField);
                        return new EqualCondition(field, value[0], EscapeStrategy.DEFAULT);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NE:
                if (modelField.equals(RoleModel.SearchableFields.REALM_ID) ||
                        modelField.equals(RoleModel.SearchableFields.IS_CLIENT_ROLE) ||
                        modelField.equals(RoleModel.SearchableFields.CLIENT_ID)) {
                    // don't filter by realm, as the LDAP directory is specific to the realm already
                    return new LdapRoleModelCriteriaBuilder(config -> new NoopCondition());
                } else if (modelField.equals(RoleModel.SearchableFields.NAME)) {
                    // validateValue(value, modelField, op, String.class);
                    return new LdapRoleModelCriteriaBuilder((roleMapperConfig) -> {
                        String field = modelFieldNameToLdap(roleMapperConfig, modelField);
                        return new NotCondition(new EqualCondition(field, value[0], EscapeStrategy.DEFAULT));
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case ILIKE:
            case LIKE:
                if (modelField.equals(RoleModel.SearchableFields.NAME) ||
                     modelField.equals(RoleModel.SearchableFields.DESCRIPTION)) {
                    // validateValue(value, modelField, op, String.class);
                    // first escape all elements of the string (which would not escape the percent sign)
                    // then replace percent sign with the wildcard character asterisk
                    // the result should then be used unescaped in the condition.
                    String v = EscapeStrategy.DEFAULT.escape(String.valueOf(value[0])).replaceAll("%", "*");
                    return new LdapRoleModelCriteriaBuilder((config) -> {
                        String field = modelFieldNameToLdap(config, modelField);
                        return new EqualCondition(field, v, EscapeStrategy.NON_ASCII_CHARS_ONLY);
                    });
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }

    private String modelFieldNameToLdap(RoleMapperConfig roleMapperConfig, SearchableModelField<? super RoleModel> modelField) {
        if (modelField.equals(RoleModel.SearchableFields.NAME)) {
            return roleMapperConfig.getRoleNameLdapAttribute();
        } else if (modelField.equals(RoleModel.SearchableFields.DESCRIPTION)) {
            return "description";
        } else if (modelField.equals(RoleModel.SearchableFields.CLIENT_ID)) {
            // TODO: find proper field for a description
            return roleMapperConfig.getRoleNameLdapAttribute();
        } else {
            throw new CriterionNotSupportedException(modelField, null);
        }
    }
}
