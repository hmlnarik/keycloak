/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import java.util.Set;
import org.keycloak.provider.Provider;

/**
 * Provider of the role records.
 * @author vramik
 */
public interface RoleProvider extends Provider {

    /**
     * Adds a realm role with given {@code name} to the given realm.
     * The internal ID of the role will be created automatically.
     * @param realm Realm owning this role.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    default public RoleModel addRealmRole(RealmModel realm, String name) {
        return addRealmRole(realm, null, name);
    }

    /**
     * Adds a realm role with given internal ID and {@code name} to the given realm.
     * @param realm Realm owning this role.
     * @param id Internal ID of the role or {@code null} if one is to be created by the underlying store
     * @param name String name of the role.
     * @return Model of the created client.
     * @throws IllegalArgumentException If {@code id} does not conform
     *   the format understood by the underlying store.
     */
    public RoleModel addRealmRole(RealmModel realm, String id, String name);

    /**
     * Returns the role with given name of the given realm.
     * @param realm Realm.
     * @param name String name of the role.
     * @return Model of the role.
     */
    public RoleModel getRealmRole(RealmModel realm, String name);

    /**
     * Returns the role with given internal ID of the given realm.
     * @param id Internal ID of the role.
     * @param realm Realm.
     * @return Model of the role.
     * @deprecated Use {@link #getRoleById(RealmModel, String)} instead.
     */
    default public RoleModel getRoleById(String id, RealmModel realm) {
        return getRoleById(realm, id);
    }

    /**
     * Returns the role with given internal ID of the given realm.
     * @param realm Realm.
     * @param id Internal ID of the role.
     * @return Model of the role.
     */
    public RoleModel getRoleById(RealmModel realm, String id);

    /**
     * Returns all the realm roles of the given realm.
     * Effectively the same as the call {@code getRealmRoles(realm, null, null)}.
     * @param realm Realm.
     * @return List of the roles. Never returns {@code null}.
     */
    default public Set<RoleModel> getRealmRoles(RealmModel realm) {
        return getRealmRoles(realm, null, null);
    }

    /**
     * Returns the realm roles of the given realm.
     * @param realm Realm.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return List of the roles. Never returns {@code null}.
     */
    public Set<RoleModel> getRealmRoles(RealmModel realm, Integer first, Integer max);

    /**
     * Returns the realm roles of the given realm by the given search.
     * @param realm Realm.
     * @param search String to search by role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return List of the realm roles their name or description contains given search string. 
     * Never returns {@code null}.
     */
    public Set<RoleModel> searchForRoles(RealmModel realm, String search, Integer first, Integer max);

    /**
     * Removes given realm role from the given realm.
     * @param realm Realm.
     * @param role Role to be removed.
     * @return {@code true} if the role existed and has been removed, {@code false} otherwise.
     */
    public boolean removeRole(RealmModel realm, RoleModel role);

    /**
     * @deprecated Use {@link #addClientRole(ClientModel, String)} instead.
     */
    default public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(client, name);
    }

    /**
     * Adds a client role with given {@code name} to the given client.
     * The internal ID of the role will be created automatically.
     * @param client Client owning this role.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    public RoleModel addClientRole(ClientModel client, String name);

    /**
     * @deprecated Use {@link #addClientRole(ClientModel, String, String)} instead.
     */
    default public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        return addClientRole(client, id, name);
    }

    /**
     * Adds a client role with given internal ID and {@code name} to the given client.
     * @param client Client owning this role.
     * @param id Internal ID of the client role or {@code null} if one is to be created by the underlying store.
     * @param name String name of the role.
     * @return Model of the created role.
     */
    public RoleModel addClientRole(ClientModel client, String id, String name);

    /**
     * @deprecated Use {@link #getClientRole(ClientModel, String)} instead.
     */
    default public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        return getClientRole(client, name);
    }

    /**
     * Returns the role with given name of the given client.
     * @param client Client.
     * @param name String name of the role.
     * @return Model of the role.
     */
    public RoleModel getClientRole(ClientModel client, String name);

    /**
     * @deprecated Use {@link #getClientRoles(ClientModel)} instead.
     */
    default public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        return getClientRoles(client);
    }

    /**
     * Returns all the client roles of the given client.
     * Effectively the same as the call {@code getClientRoles(client, null, null)}.
     * @param client Client.
     * @return List of the roles. Never returns {@code null}.
     */
    public Set<RoleModel> getClientRoles(ClientModel client);

    /**
     * @deprecated Use {@link #getClientRoles(ClientModel, Integer, Integer)} instead.
     */
    default public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client, Integer first, Integer max) {
        return getClientRoles(client, first, max);
    }

    /**
     * Returns the client roles of the given client.
     * @param client Client.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return List of the roles. Never returns {@code null}.
     */
    public Set<RoleModel> getClientRoles(ClientModel client, Integer first, Integer max);

    /**
     * @deprecated Use {@link #searchForClientRoles(ClientModel, String, Integer, Integer)} instead.
     */
    default public Set<RoleModel> searchForClientRoles(RealmModel realm, ClientModel client, String search, Integer first, Integer max) {
        return searchForClientRoles(client, search, first, max);
    }

    /**
     * Returns the client roles of the given client by the given search.
     * @param client Client.
     * @param search String to search by role's name or description.
     * @param first First result to return. Ignored if negative or {@code null}.
     * @param max Maximum number of results to return. Ignored if negative or {@code null}.
     * @return List of the client roles their name or description contains given search string. 
     * Never returns {@code null}.
     */
    public Set<RoleModel> searchForClientRoles(ClientModel client, String search, Integer first, Integer max);
    
}
