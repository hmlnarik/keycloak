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
package org.keycloak.models.map.storage.ldap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;

// todo: might extend LDAPTransaction in the future
public abstract class LdapMapKeycloakTransaction<RE, E extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<E, M> {

    private final MapKeycloakTransaction<E, M> delegate;
    private final Config.Scope config;
    private final KeycloakSession session;

    public LdapMapKeycloakTransaction(KeycloakSession session, Config.Scope config, MapKeycloakTransaction<E, M> delegate) {
        this.session = session;
        this.config = config;
        this.delegate = delegate;
    }

    protected abstract static class MapTaskWithValue {
        public abstract void execute();
    }

    protected abstract class DeleteOperation extends MapTaskWithValue {
        final protected RE entity;
        public DeleteOperation(RE entity) {
            this.entity = entity;
        }
    }

    protected final LinkedList<MapTaskWithValue> tasksOnRollback = new LinkedList<>();

    protected final LinkedList<MapTaskWithValue> tasksOnCommit = new LinkedList<>();

    protected final Map<EntityKey, RE> entities = new HashMap<>();

    @Override
    public E create(E mapEntity) {
        return delegate.create(mapEntity);
    }

    @Override
    public E read(String key) {
        return delegate.read(key);
    }

    protected abstract LdapModelCriteriaBuilder createLdapModelCriteriaBuilder();

    protected abstract LdapModelCriteriaBuilderForRealm createLdapModelCriteriaBuilderForRealm();

    protected abstract LdapModelCriteriaBuilderForClientId createLdapModelCriteriaBuilderForClientId();

    @Override
    public Stream<E> read(QueryParameters<M> queryParameters) {
        throw new NotImplementedException("will have a common method here, soon");
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return delegate.getCount(queryParameters);
    }

    @Override
    public boolean delete(String key) {
        return delegate.delete(key);
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        return delegate.delete(queryParameters);
    }

    protected static class EntityKey {
        private final String key;

        public String getRealmId() {
            return realmId;
        }

        private final String realmId;
        public EntityKey(String realmId, String key) {
            this.realmId = realmId;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityKey entityKey = (EntityKey) o;
            return Objects.equals(key, entityKey.key) && Objects.equals(realmId, entityKey.realmId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, realmId);
        }
    }
}
