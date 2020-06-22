/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.models.map.role;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public class MapRoleAdapter implements RoleModel {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final MapRoleEntity entity;

    public MapRoleAdapter(KeycloakSession session, RealmModel realm, MapRoleEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(realm, "realm");
        this.session = session;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public String getName() {
        return this.entity.getName();
    }

    @Override
    public String getDescription() {
        return this.entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        this.entity.setDescription(description);
    }

    @Override
    public String getId() {
        return this.entity.getId().toString();
    }

    @Override
    public void setName(String name) {
        this.entity.setName(name);
    }

    @Override
    public boolean isComposite() {
        return this.entity.isComposite();
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        if (role == null) {
            return;
        }
        this.entity.addCompositeRole(UUID.fromString(role.getId()));
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        if (role == null) {
            return;
        }
        this.entity.removeCompositeRole(UUID.fromString(role.getId()));
    }

    private RoleModel idToModel(UUID id) {
        final String idString = id.toString();
        // This is indeed completely lame
        RoleModel res = realm.getRoleById(idString);
        if (res == null) {
            res = realm.getClients().stream()
              .map(ClientModel::getRoles)
              .flatMap(Collection::stream)
              .filter(role -> Objects.equals(idString, role.getId()))
              .findAny()
              .orElse(null);
        }
        return res;
    }

    @Override
    public Set<RoleModel> getComposites() {
        return this.entity.getComposites().stream()
          .map(this::idToModel)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    }

    @Override
    public boolean isClientRole() {
        return this.entity.isClientRole();
    }

    public void setClientRole(boolean clientRole) {
        this.entity.setClientRole(clientRole);
    }

    @Override
    public String getContainerId() {
        return this.entity.getContainerId().toString();
    }

    public void setContainerId(String containerId) {
        this.entity.setContainerId(UUID.fromString(containerId));
    }

    @Override
    public RoleContainerModel getContainer() {
        if (entity.isClientRole()) {
            return realm.getClientById(getContainerId());
        } else {
            return realm;
        }
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role) || KeycloakModelUtils.searchFor(role, this, new HashSet<>());
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        setAttribute(name, Arrays.asList(value));
    }

    @Override
    public void setAttribute(String name, Collection<String> values) {
        this.entity.setAttribute(name, new LinkedList<>(values));
    }

    @Override
    public void removeAttribute(String name) {
        this.entity.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> val = getAttribute(name);
        return (val == null || val.isEmpty())
          ? null
          : val.iterator().next();
    }

    @Override
    public List<String> getAttribute(String name) {
        return Collections.unmodifiableList(this.entity.getAttribute(name));
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(entity.getAttributes());
    }

}
