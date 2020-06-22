/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.client;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;

import org.keycloak.models.RealmModel.ClientUpdatedEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.transaction.MapKeycloakTransaction;
import org.keycloak.models.utils.KeycloakModelUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MapClientProvider implements ClientProvider {

    protected static final Logger logger = Logger.getLogger(MapClientProvider.class);
    private static final Predicate<MapClientEntity> ALWAYS_FALSE = c -> { return false; };
    private final KeycloakSession session;
    private final MapKeycloakTransaction<UUID, MapClientEntity> tx;
    private final ConcurrentMap<UUID, MapClientEntity> clientStore;
    private final ConcurrentMap<UUID, ConcurrentMap<String, Integer>> clientRegisteredNodesStore;

    private static final Comparator<MapClientEntity> COMPARE_BY_CLIENT_ID = new Comparator<MapClientEntity>() {
        @Override
        public int compare(MapClientEntity o1, MapClientEntity o2) {
            String c1 = o1 == null ? null : o1.getClientId();
            String c2 = o2 == null ? null : o2.getClientId();
            return c1 == c2 ? 0
              : c1 == null ? -1
              : c2 == null ? 1
              : c1.compareTo(c2);

        }
    };

    public MapClientProvider(KeycloakSession session, ConcurrentMap<UUID, MapClientEntity> clientStore, ConcurrentMap<UUID, ConcurrentMap<String, Integer>> clientRegisteredNodesStore) {
        this.session = session;
        this.clientStore = clientStore;
        this.clientRegisteredNodesStore = clientRegisteredNodesStore;
        this.tx = new MapKeycloakTransaction<>(clientStore);
        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    private ClientUpdatedEvent clientUpdatedEvent(ClientModel c) {
        return new RealmModel.ClientUpdatedEvent() {
            @Override
            public ClientModel getUpdatedClient() {
                return c;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    private MapClientEntity registerEntityForChanges(MapClientEntity origEntity) {
        final MapClientEntity res = MapClientEntity.from(origEntity);
        tx.putIfChanged(origEntity.getId(), res, MapClientEntity::isUpdated);
        return res;
    }

    private Function<MapClientEntity, ClientModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientAdapter(session, realm, registerEntityForChanges(origEntity)) {
            @Override
            public void updateClient() {
                // commit
                MapClientProvider.this.tx.replace(entity.getId(), this.entity);
                session.getKeycloakSessionFactory().publish(clientUpdatedEvent(this));
            }

            /** This is runtime information and should have never been part of the adapter */
            @Override
            public Map<String, Integer> getRegisteredNodes() {
                return clientRegisteredNodesStore.computeIfAbsent(entity.getId(), k -> new ConcurrentHashMap<>());
            }

            @Override
            public void registerNode(String nodeHost, int registrationTime) {
                Map<String, Integer> value = getRegisteredNodes();
                value.put(nodeHost, registrationTime);
            }

            @Override
            public void unregisterNode(String nodeHost) {
                getRegisteredNodes().remove(nodeHost);
            }

        };
    }

    private Predicate<MapClientEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return MapClientProvider.ALWAYS_FALSE;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm, Integer firstResult, Integer maxResults) {
        Stream<ClientModel> s = getClientsStream(realm);
        if (firstResult >= 0) {
            s = s.skip(firstResult);
        }
        if (maxResults >= 0) {
            s = s.limit(maxResults);
        }
        return s.collect(Collectors.toList());
    }

//    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return clientStore.values().stream()
          .filter(entityRealmFilter(realm))
          .sorted(COMPARE_BY_CLIENT_ID)
          .map(entityToAdapterFunc(realm))
        ;
    }

    @Override
    public List<ClientModel> getClients(RealmModel realm) {
        return getClientsStream(realm).collect(Collectors.toList());
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);
        MapClientEntity entity = new MapClientEntity(entityId, realm.getId());
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        if (tx.get(entity.getId()) != null) {
            throw new ModelDuplicateException("Client exists: " + id);
        }
        tx.putIfAbsent(entity.getId(), entity);
        final ClientModel resource = entityToAdapterFunc(realm).apply(entity);

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish((RealmModel.ClientCreationEvent) () -> resource);
        resource.updateClient();        // This is actualy strange contract - it should be the store code to call updateClient

        return resource;
    }

    @Override
    public List<ClientModel> getAlwaysDisplayInConsoleClients(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeClient(String id, RealmModel realm) {
        if (id == null) {
            return false;
        }

        // TODO: Sending an event (and client role removal) should be extracted to store layer
        final ClientModel client = getClientById(id, realm);
        if (client == null) return false;
        session.users().preRemove(realm, client);
        for (RoleModel role : client.getRoles()) {
            // No need to go through cache. Roles were already invalidated
            removeRole(realm, role);
        }

        session.getKeycloakSessionFactory().publish(new RealmModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
        // TODO: ^^^^^^^ Up to here

        tx.remove(UUID.fromString(id));

        return true;
    }

    @Override
    public ClientModel getClientById(String id, RealmModel realm) {
        if (id == null) {
            return null;
        }
        MapClientEntity entity = tx.get(UUID.fromString(id));
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public ClientModel getClientByClientId(String clientId, RealmModel realm) {
        if (clientId == null) {
            return null;
        }
        String clientIdLower = clientId.toLowerCase();

        return Stream.concat(tx.valuesStream(), clientStore.values().stream())
          .filter(entityRealmFilter(realm))
          .filter(entity -> entity.getClientId() != null && Objects.equals(entity.getClientId().toLowerCase(), clientIdLower))
          .map(entityToAdapterFunc(realm))
          .findFirst()
          .orElse(null)
        ;
    }

    @Override
    public List<ClientModel> searchClientsByClientId(String clientId, Integer firstResult, Integer maxResults, RealmModel realm) {
        if (clientId == null) {
            return Collections.EMPTY_LIST;
        }
        String clientIdLower = clientId.toLowerCase();

        return Stream.concat(tx.valuesStream(), clientStore.values().stream())
          .filter(entityRealmFilter(realm))
          .filter(entity -> entity.getClientId() != null && entity.getClientId().toLowerCase().contains(clientIdLower))
          .sorted(COMPARE_BY_CLIENT_ID)
          .skip(firstResult)
          .limit(maxResults)
          .map(entityToAdapterFunc(realm))
          .collect(Collectors.toList())
        ;
    }

    /********** ROLES - should be eventually stored and maintained separately from ClientProvider ***********/

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String name) {
        return addClientRole(realm, client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(RealmModel realm, ClientModel client, String id, String name) {
        // TODO: extract to role store
//        MapRoleEntity entity = id == null ? null : roleStore.get(UUID.fromString(id));
//        UUID uuid = UUID.fromString(id);
//        if (entity == null) {
//            MapRoleEntity roleEntity = new MapRoleEntity(uuid);
//            roleEntity.setName(name);
//            roleEntity.setContainerId(UUID.fromString(client.getId()));
//            roleEntity.setClientRole(true);
//            roleStore.put(uuid, roleEntity);
//        }
//        return new MapRoleAdapter(session, realm, roleStore.get(uuid));
        return session.realms().addClientRole(realm, client, id, name);
    }

    @Override
    public RoleModel getClientRole(RealmModel realm, ClientModel client, String name) {
        // TODO: extract to role store
//        String id = client == null ? null : client.getId();
//        final UUID clientUuid = id == null ? null : UUID.fromString(id);
//        return (clientUuid == null ? Stream.<MapRoleEntity>empty() : roleStore.values().stream())
//          .filter(role -> Objects.equals(name, role.getName()))
//          .filter(role -> Objects.equals(clientUuid, role.getContainerId()))
//          .findFirst()
//          .map(role -> new MapRoleAdapter(session, realm, role))
//          .orElse(null);
        return session.realms().getClientRole(realm, client, name);
    }

    @Override
    public Set<RoleModel> getClientRoles(RealmModel realm, ClientModel client) {
        // TODO: extract to role store
//        String id = client == null ? null : client.getId();
//        final UUID clientUuid = id == null ? null : UUID.fromString(id);
//        return (clientUuid == null ? Stream.<MapRoleEntity>empty() : roleStore.values().stream())
//          .filter(role -> Objects.equals(clientUuid, role.getContainerId()))
//          .map(role -> new MapRoleAdapter(session, realm, role))
//          .collect(Collectors.toSet());
        return session.realms().getClientRoles(realm, client);
    }


    @Override
    public boolean removeRole(RealmModel realm, RoleModel role) {
//        String id = role == null ? null: role.getId();
        // TODO - check realm
//        return roleStore.remove(UUID.fromString(id)) != null;
        return session.realms().removeRole(realm, role);
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
//        MapRoleEntity entity = id == null ? null : roleStore.get(UUID.fromString(id));
//        // TODO - check realm
//        return entity == null ? null : new MapRoleAdapter(session, realm, entity);
        return session.realms().getRoleById(id, realm);
    }

    @Override
    public void close() {
        
    }

}
