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
package org.keycloak.testsuite.model.storage.tree.sample;


import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.client.MapClientEntityImpl;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.tree.TreeAwareMapTransaction;
import org.keycloak.models.map.storage.tree.TreeStorageNodeInstance;
import org.keycloak.storage.StorageId;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class ChangingIdStorageProviderFactory implements AmphibianProviderFactory<MapStorageProvider>, MapStorageProviderFactory.Native {

    private static final Logger LOG = Logger.getLogger(ChangingIdStorageProviderFactory.class);
    public static final String PROVIDER_ID = "changing-id";
    public static final String MAPPERS = "mappers";

    private Map<EntityField, Mapper> mappers = new IdentityHashMap<>();

    public class Provider implements MapStorageProvider {

        @Override
        @SuppressWarnings("unchecked")
        public <V extends AbstractEntity, M> Storage<V, M> getStorage(Class<M> modelType, Flag... flags) {
            if (modelType != ClientModel.class) {
                throw new UnsupportedOperationException("Unsupported model type.");
            }
            return getStorageFor(modelType);
        }

        @Override
        public boolean supports(Class<?> modelType) {
            return modelType == ClientModel.class;
        }

        @Override
        public void close() {
        }
    }

    public class Storage<V extends AbstractEntity, M> implements MapStorage<V, M> {

        @Override
        @SuppressWarnings("unchecked")
        public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
            return transactionInstance;
        }
    }

    private class Transaction<V extends AbstractEntity, M> implements MapKeycloakTransaction<V, M>, TreeAwareMapTransaction<V, V, M> {
        @Override
        public V create(V value) {
            return value;
        }

        @Override
        public V read(String key) {
            return null;
        }

        @Override
        public Stream<V> read(QueryParameters<M> queryParameters) {
            return Stream.empty();
        }

        @Override
        public long getCount(QueryParameters<M> queryParameters) {
            return 0L;
        }

        @Override
        public boolean delete(String key) {
            return false;
        }

        @Override
        public long delete(QueryParameters<M> queryParameters) {
            return 0L;
        }

        @Override
        public void begin() {
        }

        @Override
        public void commit() {
        }

        @Override
        public void rollback() {
        }

        @Override
        public void setRollbackOnly() {
        }

        @Override
        public boolean getRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public V loadedInSubnode(TreeStorageNodeInstance<V> childNode, V entity) {
            return DeepCloner.DUMB_CLONER.entityFieldDelegate(entity, new EntityFieldDelegate<V>() {
                @Override
                public Object get(EntityField<V> field) {
                    @SuppressWarnings("unchecked")
                    Mapper<V> mapper = mappers.get(field);
                    return mapper == null ? field.get(entity) : mapper.there(entity);
                }

                @Override
                public <T> void set(EntityField<V> field, T value) {
                    if (MapClientEntityFields.ID == field) {
                        @SuppressWarnings("unchecked")
                        Mapper<V> mapper = mappers.get(field);
                        final String sValue = (String) value;
                        mapper.back(entity, sValue);
                    } else {
                        field.set(entity, value);
                    }
                }

                @Override
                public boolean isUpdated() {
                    return (entity instanceof UpdatableEntity) ? ((UpdatableEntity) entity).isUpdated() : false;
                }
            });
        }

        @Override
        public void invalidate(V value) {
        }

        @Override
        public V validate(TreeStorageNodeInstance<V> thisStorageNode, String thisStorageId, V entityFromParent) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public StorageId getOriginalStorageId(TreeStorageNodeInstance<V> thisStorageNode, StorageId idInThisStorage, Supplier<V> thisStorageEntityLoader) {
            @SuppressWarnings("unchecked")
            Mapper<Object> idMapper = mappers.get(MapClientEntityFields.ID);
            if (idMapper != null) {
                MapClientEntityImpl e = new MapClientEntityImpl(DeepCloner.DUMB_CLONER);
                idMapper.back(e, idInThisStorage.getExternalId());
                if (e.getId() != null) {
                    return new StorageId(e.getId());
                }
            }
            return idInThisStorage;
        }

        @Override
        public Optional<Set<ModelCriteriaNode<M>>> getNotRecognizedCriteria(DefaultModelCriteria<M> criteria) {
            throw new UnsupportedOperationException("Not supported yet.");
        }


    }

    private final Storage<?, ?> store = new Storage<>();
    private final Transaction transactionInstance = new Transaction();
    private final MapStorageProvider providerInstance = new Provider();

    @SuppressWarnings("unchecked")
    private <V extends AbstractEntity, M> Storage<V, M> getStorageFor(Class<M> modelType) {
        return (Storage<V, M>) store;
    }

    @Override
    public MapStorageProvider create(KeycloakSession session) {
        return providerInstance;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "TESTS: Mapping virtual storage";
    }

}
