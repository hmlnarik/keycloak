/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class ReadOnlyMapTransaction<V extends AbstractEntity, M> implements MapKeycloakTransaction<V, M> {

    private final MapKeycloakTransaction<V, M> tx;

    public ReadOnlyMapTransaction(MapKeycloakTransaction<V, M> tx) {
        this.tx = tx;
    }

    @Override
    public V create(V value) {
        return value;
    }

    @Override
    public V read(String key) {
        return createReadOnlyDelegate(tx.read(key));
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        return tx.read(queryParameters).map(this::createReadOnlyDelegate);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return tx.getCount(queryParameters);
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        return 0;
    }

    @Override
    public void begin() {
        tx.begin();
    }

    @Override
    public void commit() {
        tx.commit();
    }

    @Override
    public void rollback() {
        tx.rollback();
    }

    @Override
    public void setRollbackOnly() {
        tx.setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return tx.getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return tx.isActive();
    }

    private V createReadOnlyDelegate(V entity) {
        if (entity == null) {
            return null;
        }
        final DelegateProvider<V> readOnlyDelegate = new DelegateProvider<V>() {
            @SuppressWarnings("unchecked")
            @Override
            public V getDelegate(boolean isRead, Enum<? extends EntityField<V>> field, Object... parameters) {
                if (isRead) {
                    return entity;
                } else {
                    return (V) DeepCloner.DUMB_CLONER.emptyInstance(entity.getClass());
                }
            }

            @Override
            public boolean isUpdated() {
                return false;
            }
        };
        return DeepCloner.DUMB_CLONER.delegate(entity, readOnlyDelegate);
    }

}
