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
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.LdapMapQuery;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.EqualCondition;
import org.keycloak.storage.ldap.idm.query.internal.NotCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdapRoleModelCriteriaBuilder extends LdapModelCriteriaBuilder<LdapRoleEntity, RoleModel, LdapRoleModelCriteriaBuilder> {

    public LdapRoleModelCriteriaBuilder() {
        super(LdapRoleModelCriteriaBuilder::new);
    }

    private LdapRoleModelCriteriaBuilder(Supplier<Condition> predicateFunc) {
        super(LdapRoleModelCriteriaBuilder::new, predicateFunc);
    }

    @Override
    public LdapRoleModelCriteriaBuilder compare(SearchableModelField<? super RoleModel> modelField, Operator op, Object... value) {
        switch (op) {
            case EQ:
                if (modelField.equals(RoleModel.SearchableFields.REALM_ID) ||
                        modelField.equals(RoleModel.SearchableFields.CLIENT_ID) ||
                        modelField.equals(RoleModel.SearchableFields.IS_CLIENT_ROLE) ||
                        modelField.equals(RoleModel.SearchableFields.NAME)) {
                    // validateValue(value, modelField, op, String.class);

                    return new LdapRoleModelCriteriaBuilder(() ->
                            new EqualCondition(modelField.getName(), value[0], EscapeStrategy.DEFAULT)
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            case NE:
                if (modelField.equals(RoleModel.SearchableFields.REALM_ID) ||
                        modelField.equals(RoleModel.SearchableFields.CLIENT_ID) ||
                        modelField.equals(RoleModel.SearchableFields.IS_CLIENT_ROLE) ||
                        modelField.equals(RoleModel.SearchableFields.NAME)) {
                    // validateValue(value, modelField, op, String.class);

                    return new LdapRoleModelCriteriaBuilder(() ->
                            new NotCondition(new EqualCondition(modelField.getName(), value[0], EscapeStrategy.DEFAULT))
                    );
                } else {
                    throw new CriterionNotSupportedException(modelField, op);
                }

            default:
                throw new CriterionNotSupportedException(modelField, op);
        }
    }
}
