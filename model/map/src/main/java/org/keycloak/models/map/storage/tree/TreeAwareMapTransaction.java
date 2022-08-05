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
package org.keycloak.models.map.storage.tree;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.criteria.ModelCriteriaNode;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.storage.StorageId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A {@link MapStorage} that is willing to participate in an inner node of a {@link TreeStorage}.
 * @author hmlnarik
 * @param <V>
 * @param <M>
 */
public interface TreeAwareMapTransaction<V extends AbstractEntity, R extends AbstractEntity, M> {

    /**
     * Called after an entity is instantiated (read or created) by a storage that is
     * in the tree structure a (direct or indirect) child of this node.
     * <p>
     * This method can return the same entity if this storage acts as a pass-through store (e.g. to validate the values)
     * or can provide its own logic and replace the entity with its own implementation that usually keeps reference
     * to the original node and entity. For example, a JPA store above a LDAP store might store the original
     * LDAP node ID and object ID in some attribute in the database.
     * <p>
     * It is the responsibility of the returned entity to handle all deferrals to read / write properties
     * to the {@code entity}.
     *
     * @param pathFromBottomToNode Path from the original creator node (at head of the list) to the node with current storage
     *   (at the end of the list).
     * @param valueFromChild Value representing created entity processed by a direct child
     * @param original Original value that induced creation of the {@code value}. This value could have been
     *   processed first by any transitive child of this node.
     *
     * @see #getOriginalStorageId
     * 
     * @return Value that represents the loaded entity. This value is then similarly passed to the parents of this node
     *   and ultimately to the caller of the operation of this store that instantiated the entity. Both {@code null}
     *   and {@code childNodeWithEntity.getEntity()} result is treated as if this was a pass-through storage.
     */
    V loadedInSubnode(TreeStorageNodeInstance<V> childNode, V entity);

    /**
     * Invalidates a value that became completely stale in this storage. The cached values are required to be
     * refreshed upon next access of this object.
     * <p>
     * This might happen e.g. in case when this store caches values from LDAP, and the object was deleted from LDAP.
     * @param value 
     */
    void invalidate(V value);

    /**
     * Validates {@code entityFromParent} from parent node against the state of the same object in this (child) storage.
     * Returns either {@code entityFromParent} or {@code null}.
     * May update fields of the {@code entityFromParent} to match those that should be updated in the parent.
     * <p>
     * This is useful e.g. to check that a user obtained from JPA intermittent store still exists in LDAP.
     * 
     * @param thisStorageNode Node that contains this storage
     * @param thisStorageId {@link StorageId#getExternalId() External ID} of the entity in this storage as returned
     *                      by parent's {@link #getOriginalStorageId}
     * @param entityFromParent Entity as returned from the parent
     * @return See description
     */
    V validate(TreeStorageNodeInstance<V> thisStorageNode, String thisStorageId, V entityFromParent);

    /**
     * Returns storage ID in the original storage of the {@code entity} known to this storage, including the original ID
     * of the storage node.
     * <p>
     * This is useful for e.g. to determine original LDAP ID from a cached object.
     * @param thisStorageNode Node where this storage is
     * @param thisStorageRawEntityLoader Method that supplies raw (no mappers applied) object from the storage
     * @return
     */
    StorageId getOriginalStorageId(TreeStorageNodeInstance<V> thisStorageNode, StorageId idInThisStorage, Supplier<R> thisStorageRawEntityLoader);

    /**
     * Returns an non-empty {@link Optional} with individual condition nodes with criteria
     * that are not recognized by this storage in bulk operations. If all criteria
     * are recognized and supported by this storage, {@link Optional#empty()} is returned
     * <p>
     * TODO: should this be rather in some TreeAwareMapStorage rather than _transaction_ class?
     *
     *
     * @param criteria
     * @return See description
     */
    Optional<Set<ModelCriteriaNode<M>>> getNotRecognizedCriteria(DefaultModelCriteria<M> criteria);


}
