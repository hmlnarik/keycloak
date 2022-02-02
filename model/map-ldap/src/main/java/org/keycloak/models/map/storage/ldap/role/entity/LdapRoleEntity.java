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
import java.util.Set;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.role.MapRoleEntity.AbstractRoleEntity;

public class LdapRoleEntity extends AbstractRoleEntity  {

    private final MapRoleEntity delegate;

    // TODO: would I need one with a cloner?
    public LdapRoleEntity(MapRoleEntity delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void setId(String id) {
        delegate.setId(id);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        delegate.setAttributes(attributes);
    }

    @Override
    public List<String> getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, List<String> value) {
        delegate.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public boolean isUpdated() {
        return delegate.isUpdated();
    }

    @Override
    public void clearUpdatedFlag() {
        delegate.clearUpdatedFlag();
    }

    @Override
    public Boolean isClientRole() {
        return delegate.isClientRole();
    }

    @Override
    public String getRealmId() {
        return delegate.getRealmId();
    }

    @Override
    public String getClientId() {
        return delegate.getClientId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void setClientRole(Boolean clientRole) {
        delegate.setClientRole(clientRole);
    }

    @Override
    public void setRealmId(String realmId) {
        delegate.setRealmId(realmId);
    }

    @Override
    public void setClientId(String clientId) {
        delegate.setClientId(clientId);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public Set<String> getCompositeRoles() {
        return delegate.getCompositeRoles();
    }

    @Override
    public void setCompositeRoles(Set<String> compositeRoles) {
        delegate.setCompositeRoles(compositeRoles);
    }

    @Override
    public void addCompositeRole(String roleId) {
        delegate.addCompositeRole(roleId);
    }

    @Override
    public void removeCompositeRole(String roleId) {
        delegate.removeCompositeRole(roleId);
    }

}
