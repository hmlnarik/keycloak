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
package org.keycloak.models.map.storage;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;
import org.keycloak.provider.Provider;
import static org.keycloak.models.map.storage.ModelEntityUtil.getModelName;

/**
 *
 * @author hmlnarik
 */
public interface MapStorageProvider extends Provider {
    
    public enum DefaultFlag implements Flag {
        /**
         * If the requested storage does not exist yet or is not supported by the map storage factory, do not create it.
         * If a storage is requested with this flag, the method may return {@code null}.
         * <p>
         * Examples:
         * <ul>
         * <li>If client storage is requested from JPA implementation and the database of clients has not yet been created.</li>
         * <li>If realm storage is requested from LDAP and the LDAP does not implement realm storage.</li>
         * </ul>
         */
        DONT_INITIALIZE,
        INITIALIZE_EMPTY
    }

    /**
     * Returns a key-value storage implementation for the given types.
     * @param <V> type of the value
     * @param <M> type of the corresponding model (e.g. {@code UserModel})
     * @param modelType Model type
     * @param flags Flags of the returned storage. Best effort, flags may be not honored by underlying implementation
     * @return Storage instance for the given model type. {@code null} would only possible to be returned
     *         if one of the {@code flags} explicitly treats it as a valid result, e.g. {@link DefaultFlag#DONT_INITIALIZE}.
     * @throws IllegalArgumentException If the {@code modelType} is not supported by the underlying implementation
     *              and no flag indicates it is expected to return {@code null}
     */
    <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, Flag... flags);

    /**
     * Returns {@code true} if this map storage supports storing objects of the given model class.
     * @param modelClass
     * @return
     */
    default boolean supports(Class<?> modelType) {
        return getModelName(modelType) != null && getStorage(modelType, DefaultFlag.DONT_INITIALIZE) != null;
    }
}
