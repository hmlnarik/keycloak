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
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;

// todo: might extend LDAPTransaction in the future
public abstract class LdapMapKeycloakTransaction<RE, E extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<E, M> {

    private final MapKeycloakTransaction<E, M> delegate;

    public LdapMapKeycloakTransaction(MapKeycloakTransaction<E, M> delegate) {
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

    @Override
    public Stream<E> read(QueryParameters<M> queryParameters) {
        return delegate.read(queryParameters);
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
