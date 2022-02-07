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
package org.keycloak.models.map.storage.ldap.role.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.models.map.role.MapRoleEntity.AbstractRoleEntity;
import org.keycloak.models.map.storage.ldap.LdapRoleMapperConfig;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

public class LdapRoleEntity extends AbstractRoleEntity  {

    private final LDAPObject ldapObject;
    private final LdapRoleMapperConfig roleMapperConfig;

    // TODO: would I need one with a cloner -> might/will need it once I create new entities
    // to transform a MapRoleEntity to a LdapRoleEntity
    public LdapRoleEntity(LDAPObject ldapObject, LdapRoleMapperConfig roleMapperConfig) {
        this.ldapObject = ldapObject;
        this.roleMapperConfig = roleMapperConfig;
    }

    @Override
    public String getId() {
        return roleMapperConfig.getRealm() + "." + ldapObject.getUuid();
    }

    @Override
    public void setId(String id) {
        if (!Objects.equals(this.getId(), id)) {
            throw new NotImplementedException();
        }
    }


    @Override
    public Map<String, List<String>> getAttributes() {
        return null;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getAttribute(String name) {
        return null;
    }

    @Override
    public void setAttribute(String name, List<String> value) {
        throw new NotImplementedException();
    }

    @Override
    public void removeAttribute(String name) {
        throw new NotImplementedException();
    }

    @Override
    public String getRealmId() {
        return roleMapperConfig.getRealm();
    }

    @Override
    public String getClientId() {
        return null;
    }

    @Override
    public String getName() {
        return ldapObject.getAttributeAsString(roleMapperConfig.getRoleNameLdapAttribute());
    }

    @Override
    public String getDescription() {
        return ldapObject.getAttributeAsString("description");
    }

    @Override
    public void setClientRole(Boolean clientRole) {
        throw new NotImplementedException();
    }

    @Override
    public void setRealmId(String realmId) {
        throw new NotImplementedException();
    }

    @Override
    public void setClientId(String clientId) {
        throw new NotImplementedException();
    }

    @Override
    public void setName(String name) {
        if (!Objects.equals(this.getName(), name)) {
            throw new NotImplementedException();
        }
    }

    @Override
    public void setDescription(String description) {
        if (description != null && description.length() == 0 && getDescription() == null) {
            return;
        }
        if (!Objects.equals(this.getDescription(), description)) {
            throw new NotImplementedException();
        }
    }

    @Override
    public Set<String> getCompositeRoles() {
        return null;
    }

    @Override
    public void setCompositeRoles(Set<String> compositeRoles) {
        throw new NotImplementedException();
    }

    @Override
    public void addCompositeRole(String roleId) {
        throw new NotImplementedException();
    }

    @Override
    public void removeCompositeRole(String roleId) {
        throw new NotImplementedException();
    }
}
