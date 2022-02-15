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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.role.MapRoleEntity.AbstractRoleEntity;
import org.keycloak.models.map.storage.ldap.LdapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.role.LdapRoleMapKeycloakTransaction;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;

public class LdapRoleEntity extends AbstractRoleEntity  {

    private final LDAPObject ldapObject;
    private final LdapRoleMapperConfig roleMapperConfig;
    private final LdapRoleMapKeycloakTransaction transaction;

    public LdapRoleEntity(DeepCloner cloner, LdapRoleMapperConfig roleMapperConfig, LdapRoleMapKeycloakTransaction transaction) {
        ldapObject = new LDAPObject();
        ldapObject.setObjectClasses(Arrays.asList("top", "groupOfNames"));
        ldapObject.setRdnAttributeName("cn");
        this.roleMapperConfig = roleMapperConfig;
        this.transaction = transaction;
    }

    // TODO: would I need one with a cloner -> might/will need it once I create new entities
    // to transform a MapRoleEntity to a LdapRoleEntity
    public LdapRoleEntity(LDAPObject ldapObject, LdapRoleMapperConfig roleMapperConfig, LdapRoleMapKeycloakTransaction transaction) {
        this.ldapObject = ldapObject;
        this.roleMapperConfig = roleMapperConfig;
        this.transaction = transaction;
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
        Map<String, List<String>> result = new HashMap<>();
        for (String roleAttribute : roleMapperConfig.getRoleAttributes()) {
            Set<String> attrs = ldapObject.getAttributeAsSet(roleAttribute);
            if (attrs != null) {
                result.put(roleAttribute, new ArrayList<>(attrs));
            }
        }
        return result;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        // store all attributes
        if (attributes != null) {
            attributes.forEach(this::setAttribute);
        }
        // clear attributes not in the list
        for (String roleAttribute : roleMapperConfig.getRoleAttributes()) {
            if (attributes == null || !attributes.containsKey(roleAttribute)) {
                removeAttribute(roleAttribute);
            }
        }
    }

    @Override
    public List<String> getAttribute(String name) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't read attribute '" + name +"' as it is not supported");
        }
        return new ArrayList<>(ldapObject.getAttributeAsSet(name));
    }

    @Override
    public void setAttribute(String name, List<String> value) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't set attribute '" + name +"' as it is not supported");
        }
        if ((ldapObject.getAttributeAsSet(name) == null && (value == null || value.size() == 0)) ||
                Objects.equals(ldapObject.getAttributeAsSet(name), new HashSet<>(value))) {
            return;
        }
        if (ldapObject.getReadOnlyAttributeNames().contains(name)) {
            throw new ModelException("can't write attribute '" + name +"' as it is not writeable");
        }
        ldapObject.setAttribute(name, new HashSet<>(value));
        this.updated = true;
    }

    @Override
    public void removeAttribute(String name) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't write attribute '" + name +"' as it is not supported");
        }
        if (ldapObject.getAttributeAsSet(name) == null || ldapObject.getAttributeAsSet(name).size() == 0) {
            return;
        }
        ldapObject.setAttribute(name, null);
        this.updated = true;
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
        Set<String> members = ldapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        HashSet<String> compositeRoles = new HashSet<>();
        for (String member : members) {
            if (member.equals(ldapObject.getDn().toString())) {
                continue;
            }
            if (!member.startsWith(roleMapperConfig.getRoleNameLdapAttribute())) {
                // this is a real user, not a composite role, ignore
                // TODO: this will not work if users and role use the same!
                continue;
            }
            String roleId = transaction.readIdByDn(member);
            if (roleId == null) {
                throw new NotImplementedException();
            }
            compositeRoles.add(roleId);
        }
        return compositeRoles;
    }

    @Override
    public void setCompositeRoles(Set<String> compositeRoles) {
        HashSet<String> translatedCompositeRoles = new HashSet<>();
        if (compositeRoles != null) {
            for (String compositeRole : compositeRoles) {
                MapRoleEntity role = transaction.read(compositeRole);
                if (!(role instanceof LdapRoleEntity)) {
                    // TODO: encode ID as a dummy DN that signals an external entity
                    throw new NotImplementedException();
                }
                LdapRoleEntity ldapRole = (LdapRoleEntity) role;
                translatedCompositeRoles.add(ldapRole.getLdapObject().getDn().toString());
            }
        }
        Set<String> members = ldapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        for (String member : members) {
            if (!member.startsWith(roleMapperConfig.getRoleNameLdapAttribute())) {
                // this is a real user, not a composite role, ignore
                // TODO: this will not work if users and role use the same!
                translatedCompositeRoles.add(member);
            }
        }
        if (!translatedCompositeRoles.equals(members)) {
            ldapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
            this.updated = true;
        }
    }

    @Override
    public void addCompositeRole(String roleId) {
        MapRoleEntity role = transaction.read(roleId);
        if (!(role instanceof LdapRoleEntity)) {
            // TODO: encode ID as a dummy DN that signals an external entity
            throw new NotImplementedException();
        }
        LdapRoleEntity ldapRole = (LdapRoleEntity) role;
        Set<String> members = ldapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        members.add(ldapRole.getLdapObject().getDn().toString());
        ldapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
        this.updated = true;
    }

    @Override
    public void removeCompositeRole(String roleId) {
        MapRoleEntity role = transaction.read(roleId);
        if (!(role instanceof LdapRoleEntity)) {
            throw new NotImplementedException();
        }
        LdapRoleEntity ldapRole = (LdapRoleEntity) role;
        Set<String> members = ldapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        members.remove(ldapRole.getLdapObject().getDn().toString());
        ldapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
        this.updated = true;
    }

    public LDAPObject getLdapObject() {
        return ldapObject;
    }
}
