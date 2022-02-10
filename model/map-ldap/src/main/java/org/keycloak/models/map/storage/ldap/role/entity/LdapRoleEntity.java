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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.role.MapRoleEntity.AbstractRoleEntity;
import org.keycloak.models.map.storage.ldap.LdapRoleMapperConfig;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

public class LdapRoleEntity extends AbstractRoleEntity  {

    private final LDAPObject ldapObject;
    private final LdapRoleMapperConfig roleMapperConfig;

    public LdapRoleEntity(DeepCloner cloner, LdapRoleMapperConfig roleMapperConfig) {
        ldapObject = new LDAPObject();
        ldapObject.setObjectClasses(Arrays.asList("top", "groupOfNames"));
        ldapObject.setRdnAttributeName("cn");
        this.roleMapperConfig = roleMapperConfig;
    }

    // TODO: would I need one with a cloner -> might/will need it once I create new entities
    // to transform a MapRoleEntity to a LdapRoleEntity
    public LdapRoleEntity(LDAPObject ldapObject, LdapRoleMapperConfig roleMapperConfig) {
        this.ldapObject = ldapObject;
        this.roleMapperConfig = roleMapperConfig;
    }

    @Override
    public String getId() {
        // for now, only support one realm, don't make realm part of the key
        // https://github.com/keycloak/keycloak/discussions/10045
        // return roleMapperConfig.getRealm() + "." + ldapObject.getUuid();
        return ldapObject.getUuid();
    }

    @Override
    public void setId(String id) {
        this.updated |= !Objects.equals(getId(), id);
        ldapObject.setUuid(id);
    }


    @Override
    public Map<String, List<String>> getAttributes() {
        return null;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        if (attributes != null && attributes.size() > 0) {
            // maybe delegate this, or have some custom mapper to ldap attributes
            throw new NotImplementedException();
        }
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
        return roleMapperConfig.getClientId();
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
        if (!Objects.equals(this.isClientRole(), clientRole)) {
            throw new NotImplementedException();
        }
    }

    @Override
    public void setRealmId(String realmId) {
        if (!Objects.equals(this.getRealmId(), realmId)) {
            throw new NotImplementedException();
        }
    }

    @Override
    public void setClientId(String clientId) {
        if (!Objects.equals(this.getClientId(), clientId)) {
            throw new NotImplementedException();
        }
    }

    @Override
    public void setName(String name) {
        this.updated |= !Objects.equals(getName(), name);
        ldapObject.setSingleAttribute(roleMapperConfig.getRoleNameLdapAttribute(), name);
        ldapObject.setDn(LDAPDn.fromString(roleMapperConfig.getRoleNameLdapAttribute() + "=" + name + "," + roleMapperConfig.getRolesDn()));
    }

    @Override
    public void setDescription(String description) {
        this.updated |= !Objects.equals(getDescription(), description);
        if (description != null) {
            ldapObject.setSingleAttribute("description", description);
        } else {
            ldapObject.setSingleAttribute("description", "");
        }
    }

    @Override
    public Set<String> getCompositeRoles() {
        return null;
    }

    @Override
    public void setCompositeRoles(Set<String> compositeRoles) {
        if (compositeRoles != null && compositeRoles.size() > 0) {
            throw new NotImplementedException();
        }
    }

    @Override
    public void addCompositeRole(String roleId) {
        throw new NotImplementedException();
    }

    @Override
    public void removeCompositeRole(String roleId) {
        throw new NotImplementedException();
    }

    public LDAPObject getLdapObject() {
        return ldapObject;
    }
}
