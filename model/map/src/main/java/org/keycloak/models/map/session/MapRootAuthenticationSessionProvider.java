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
package org.keycloak.models.map.session;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.Serialization;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.utils.RealmInfoUtil;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionProvider implements AuthenticationSessionProvider {

    private static final Logger LOG = Logger.getLogger(MapRootAuthenticationSessionProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<UUID, MapRootAuthenticationSessionEntity> tx;
    private final MapStorage<UUID, MapRootAuthenticationSessionEntity> sessionStore;

    private static final Predicate<MapRootAuthenticationSessionEntity> ALWAYS_FALSE = role -> false;
    private static final String AUTHENTICATION_SESSION_EVENTS = "AUTHENTICATION_SESSION_EVENTS";

    public MapRootAuthenticationSessionProvider(KeycloakSession session, MapStorage<UUID, MapRootAuthenticationSessionEntity> sessionStore) {
        this.session = session;
        this.sessionStore = sessionStore;
        this.tx = new MapKeycloakTransaction<>(sessionStore);

        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    private Function<MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapRootAuthenticationSessionAdapter(session, realm, registerEntityForChanges(origEntity));
    }

    private MapRootAuthenticationSessionEntity registerEntityForChanges(MapRootAuthenticationSessionEntity origEntity) {
        MapRootAuthenticationSessionEntity res = tx.get(origEntity.getId(), id -> Serialization.from(origEntity));
        tx.putIfChanged(origEntity.getId(), res, MapRootAuthenticationSessionEntity::isUpdated);
        return res;
    }

    private Predicate<MapRootAuthenticationSessionEntity> entityRealmFilter(String realmId) {
        if (realmId == null) {
            return MapRootAuthenticationSessionProvider.ALWAYS_FALSE;
        }
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm) {
        return createRootAuthenticationSession(null, realm);
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(String id, RealmModel realm) {
        final UUID entityId = id == null ? UUID.randomUUID() : UUID.fromString(id);

        LOG.tracef("createRootAuthenticationSession(%s)%s", realm.getName(), getShortStackTrace());

        // create map authentication session entity
        MapRootAuthenticationSessionEntity entity = new MapRootAuthenticationSessionEntity(entityId, realm.getId());
        entity.setRealmId(realm.getId());
        entity.setTimestamp(Time.currentTime());

        if (tx.get(entity.getId(), sessionStore::get) != null) {
            throw new ModelDuplicateException("Root authentication session exists: " + entity.getId());
        }

        tx.putIfAbsent(entity.getId(), entity);

        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String authenticationSessionId) {
        if (authenticationSessionId == null) {
            return null;
        }

        LOG.tracef("getRootAuthenticationSession(%s, %s)%s", realm.getName(), authenticationSessionId, getShortStackTrace());

        MapRootAuthenticationSessionEntity entity = tx.get(UUID.fromString(authenticationSessionId), sessionStore::get);
        return (entity == null || !entityRealmFilter(realm.getId()).test(entity))
                ? null
                : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession) {
        tx.remove(UUID.fromString(authenticationSession.getId()));
    }

    @Override
    public void removeRootAuthenticationSessionById(String id) {
        tx.remove(UUID.fromString(id));
    }

    @Override
    public void removeExpired(RealmModel realm) {
        LOG.debugf("Removing expired sessions");

        int expired = Time.currentTime() - RealmInfoUtil.getDettachedClientSessionLifespan(realm);

        List<String> sessionIds = sessionStore.entrySet().stream()
                .filter(entity -> entityRealmFilter(realm.getId()).test(entity.getValue()))
                .filter(entity -> entity.getValue().getTimestamp() < expired)
                .map(Map.Entry::getKey)
                .map(UUID::toString)
                .collect(Collectors.toList());

        LOG.debugf("Removed %d expired authentication sessions for realm '%s'", sessionIds.size(), realm.getName());

        sessionIds.forEach(this::removeRootAuthenticationSessionById);
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        sessionStore.entrySet().stream()
                .filter(entity -> entityRealmFilter(realm.getId()).test(entity.getValue()))
                .map(Map.Entry::getKey)
                .map(UUID::toString)
                .collect(Collectors.toList())
                .forEach(this::removeRootAuthenticationSessionById);
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {

    }

    @Override
    public void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment) {
        if (compoundId == null) {
            return;
        }

        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(
                AUTHENTICATION_SESSION_EVENTS,
                MapAuthenticationSessionAuthNoteUpdateEvent.create(compoundId.getRootSessionId(), compoundId.getTabId(),
                        compoundId.getClientUUID(), authNotesFragment),
                true,
                ClusterProvider.DCNotify.ALL_BUT_LOCAL_DC
        );
    }

    @Override
    public void close() {

    }
}
