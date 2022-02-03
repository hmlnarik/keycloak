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
import org.keycloak.models.map.role.MapRoleEntity;

import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.LdapMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;
import org.keycloak.models.map.storage.ldap.role.entity.LdapRoleEntity;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPContextManager;

public class LdapRoleMapKeycloakTransaction extends LdapMapKeycloakTransaction<LdapRoleEntity, MapRoleEntity, RoleModel> {

    private LDAPContextManager contextManager;
    private final MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate;

    public LdapRoleMapKeycloakTransaction(LDAPContextManager contextManager, MapKeycloakTransaction<MapRoleEntity, RoleModel> delegate) {
        super(delegate);
        this.contextManager = contextManager;
        this.delegate = delegate;
    }

    @Override
    public void begin() {
        delegate.begin();
    }

    @Override
    public void commit() {
        delegate.commit();
    }

    @Override
    public void rollback() {
        delegate.rollback();
    }

    @Override
    public void setRollbackOnly() {
        delegate.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return delegate.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    protected LdapModelCriteriaBuilder createLdapModelCriteriaBuilder() {
        return null;
    }
}
