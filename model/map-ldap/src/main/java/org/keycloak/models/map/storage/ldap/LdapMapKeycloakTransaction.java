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

import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;

// todo: might extend LDAPTransaction in the future
public abstract class LdapMapKeycloakTransaction<RE, E extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<E, M> {

    private final RoleMapperConfig roleMapperConfig;
    private final MapKeycloakTransaction<E, M> delegate;
    private final LDAPConfig config;
    private final KeycloakSession session;

    public LdapMapKeycloakTransaction(KeycloakSession session, LDAPConfig config, RoleMapperConfig roleMapperConfig, MapKeycloakTransaction<E, M> delegate) {
        this.session = session;
        this.config = config;
        this.roleMapperConfig = roleMapperConfig;
        this.delegate = delegate;
    }

    @Override
    public E create(E mapEntity) {
        return delegate.create(mapEntity);
    }

    @Override
    public E read(String key) {
        return delegate.read(key);
    }

    protected abstract LdapModelCriteriaBuilder createLdapModelCriteriaBuilder();

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
}
