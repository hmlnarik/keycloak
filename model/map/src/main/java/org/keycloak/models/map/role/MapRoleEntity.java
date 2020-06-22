/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.models.map.role;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author hmlnarik
 */
public class MapRoleEntity {

    private final UUID id;
    private String name;
    private String description;
    private final Map<String, List<String>> attributes = new HashMap<>();
    private final Set<UUID> roleComponents = new HashSet<>();
    private boolean clientRole;
    private UUID containerId;

    public MapRoleEntity(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
    }

    public MapRoleEntity() {
        this(null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isComposite() {
        return ! this.roleComponents.isEmpty();
    }

    public void addCompositeRole(UUID id) {
        if (id == null) {
            return;
        }
        this.roleComponents.add(id);
    }

    public void removeCompositeRole(UUID id) {
        this.roleComponents.remove(id);
    }

    public Set<UUID> getComposites() {
        return this.roleComponents;
    }

    public boolean isClientRole() {
        return this.clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.clientRole = clientRole;
    }

    public UUID getContainerId() {
        return containerId;
    }

    public void setContainerId(UUID containerId) {
        this.containerId = containerId;
    }

    public void setAttribute(String name, Collection<String> values) {
        this.attributes.put(name, new LinkedList<>(values));
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public List<String> getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

}
