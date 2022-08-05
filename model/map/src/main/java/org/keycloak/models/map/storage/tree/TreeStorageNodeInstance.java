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

import org.keycloak.models.map.storage.mapper.MappingEntityFieldDelegate;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.tree.AuthoritativeDecider.AuthoritativeStatus;
import org.keycloak.storage.StorageId;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import static org.keycloak.models.map.storage.tree.TreeProperties.MODEL_CLASS;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.common.delegate.HasDelegateProvider;
import org.keycloak.models.map.common.delegate.HasEntityFieldDelegate;
import org.keycloak.models.map.common.delegate.PerFieldDelegateProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription.FieldContainedStatus;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.keycloak.models.map.common.delegate.HasRawEntity;
import org.keycloak.models.map.storage.mapper.MappersMap;

/**
 * Instance of the tree storage that is based on a prescription ({@link TreeStorageNodePrescription}),
 * i.e. it provides a map storage instance that can be used for accessing data.
 *
 * @author hmlnarik
 */
public class TreeStorageNodeInstance<V extends AbstractEntity>
  extends DefaultTreeNode<TreeStorageNodeInstance<V>> {

    private final KeycloakSession session;
    private final TreeStorageNodePrescription prescription;
    private final TreeStorageNodeInstance<V> original;   // If this node is detached, keep reference to the node in the original tree here
    /**
     * Node is authoritative in the tree that currently contains this node.
     */
    private boolean nodeAuthoritative = false;

    public class WithEntity {
        private final V entity;
        private final TreeStorageTransaction<V, ?> tx;

        public WithEntity(V entity, TreeStorageTransaction<V, ?> tx) {
            Objects.requireNonNull(entity);
            this.entity = entity;
            this.tx = tx;
        }

        public V getEntity() {
            return entity;
        }

        public TreeStorageNodeInstance<V> getNode() {
            return TreeStorageNodeInstance.this;
        }

        /**
         * Returns an object that handles setting and getting the attributes at the right place in the tree, potentially
         * lazy-loading original from a child of the node.
         * <p>
         * The logic driving "lower" object lookup is the following:
         * <ol>
         * <li>If the node has children, attempts to determine the original node and ID from the entity and the storage
         *     corresponding to the current node (where it can be stored e.g. in an attribute). If the original storage / ID
         *     cannot be determined, attempts to search the children for the object with the same ID.
         * </li>
         * <li>If the node has no children, and the storage at the node does <i>not</i> store all the attributes,
         *     uses the object only to access the attributes handled there; all the rest are directed to an
         *     {@link DeepCloner#emptyInstance empty instance}.
         * </li>
         * <li>Otherwise, i.e. the node has no children and stores all the attributes, returns that object directly</li>
         * </ol>
         * @param objectWithEntity The entity together with the node the entity has been read from.
         * @return wrapped object
         */
        public V wrapLazyDelegationToLowerEntity() {
            TreeStorageNodeInstance<V> origin = getNode();

            if (origin.hasChildren()) {
                Supplier<V> getLowerEntityFunc = this::getLowerEntity;
                PerFieldDelegateProvider<V> lowerEntityDelegateProvider = new PerFieldDelegateProvider<>(origin, getEntity(), getLowerEntityFunc);
                return (V) DeepCloner.DUMB_CLONER.entityFieldDelegate(getEntity(), lowerEntityDelegateProvider);
            }

            if (origin.mayNotContainAllFields()) {
                @SuppressWarnings("unchecked")
                Supplier<V> getLowerEntityFunc = () -> (V) DeepCloner.DUMB_CLONER.emptyInstance((Class<?>) getEntity().getClass());
                PerFieldDelegateProvider<V> lowerEntityDelegateProvider = new PerFieldDelegateProvider<>(origin, getEntity(), getLowerEntityFunc);
                return (V) DeepCloner.DUMB_CLONER.entityFieldDelegate(getEntity(), lowerEntityDelegateProvider);
            }

            return getEntity();
        }

        /**
         * Returns the original entity, i.e. the entity as stored in the storage in a descendant node of this node,
         * which this entity has been derived from. This entity is validated prior
         * returning (if validation is configured so). The returned entity also propagates setter calls to its
         * original entity if such exists.
         * <p>
         * No mappers defined on the edge from original node to this node are applied to the returned entity.
         *
         * @see TreeStorageTransaction#readFromNodePredicate
         * @see #wrapLazyDelegationToLowerEntity()
         * @return
         */
        public V getLowerEntity() {
            final TreeStorageNodeInstance<V> origin = getNode();
            final StorageId thisEntityId = new StorageId(origin.getId(), entity.getId());
            StorageId originalEntityId = tx.getTreeAwareMapTransaction(origin)
              .map(t -> t.getOriginalStorageId(origin, thisEntityId, this::getRawEntity))
              .orElse(new StorageId(entity.getId()));
            TreeStorageNodeInstance<V>.WithEntity readObject;
            Predicate<TreeStorageNodeInstance<V>> readFromNodePredicate;

            // Usually the storage ID will be known and will be a direct child:
            final Optional<TreeStorageNodeInstance<V>> directChild = origin.getChild(originalEntityId.getProviderId());
            readObject = directChild
              .map(dc -> tx.readRawAndValidateFromNode(dc, originalEntityId.getExternalId()))
              .map(e -> directChild.get().new WithEntity(e, tx))
              .orElse(null);

            // If not a direct child or the child has been removed:
            if (! directChild.isPresent()) {
                if (originalEntityId.isLocal()) {
                    // We need to inspect all children looking for the object with the given ID as the node is not known.
                    AtomicReference<TreeStorageNodeInstance<V>.WithEntity> readObjectRef = new AtomicReference<>();
                    readFromNodePredicate = tx.readFromNodePredicate(readObjectRef, originalEntityId);
                    origin.findFirstBfs(readFromNodePredicate);
                    readObject = readObjectRef.get();
                } else {
                    // Otherwise lookup the right child for the given ID
                    final Optional<TreeStorageNodeInstance<V>> indirectChildById = origin.findFirstBfs(n -> Objects.equals(originalEntityId.getProviderId(), n.getId()));
                    readObject = indirectChildById
                      .map(dc -> tx.readRawAndValidateFromNode(dc, originalEntityId.getExternalId()))
                      .map(e -> indirectChildById.get().new WithEntity(e, tx))
                      .orElse(null);
                }
            }

            if (readObject == null) {
                // TODO: This should lead to invalidation of the entity in the origin node
                tx.getTreeAwareMapTransaction(origin).ifPresent(t -> t.invalidate(entity));
                throw new IllegalStateException("Entity is in illegal state: " + entity);
            }

            return readObject.wrapLazyDelegationToLowerEntity();
        }

        @SuppressWarnings("unchecked")
        private <R> R getRawEntity() {
            Object r = entity;
            while (r != null) {
                if (r instanceof HasEntityFieldDelegate) {
                    EntityFieldDelegate d = ((HasEntityFieldDelegate) r).getEntityFieldDelegate();
                    if (d instanceof HasRawEntity) {
                        r = ((HasRawEntity) d).getRawEntity();
                    } else {
                        return (R) r;
                    }
                } else if (r instanceof HasDelegateProvider) {
                    DelegateProvider d = ((HasDelegateProvider) r).getDelegateProvider();
                    if (d instanceof HasRawEntity) {
                        r = ((HasRawEntity) d).getRawEntity();
                    } else {
                        return (R) r;
                    }
                }
            }
            return (R) r;
        }
    }

    public TreeStorageNodeInstance(TreeStorageNodeInstance<V> original) {
        this(original, original.getTreeProperties(), original.getNodeProperties(), original.getEdgeProperties());
    }

    private TreeStorageNodeInstance(TreeStorageNodeInstance<V> original, Map<String, Object> treeProperties, Map<String, Object> nodeProperties, Map<String, Object> edgeProperties) {
        super(treeProperties, nodeProperties, edgeProperties);
        this.original = original;
        this.prescription = original.prescription;
        this.session = original.session;
    }

    public TreeStorageNodeInstance(KeycloakSession session, TreeStorageNodePrescription prescription) {
        super(prescription.getTreeProperties(),
          prescription.getNodeProperties() == null ? null : new HashMap<>(prescription.getNodeProperties()),
          prescription.getEdgeProperties() == null ? null : new HashMap<>(prescription.getEdgeProperties())
        );
        this.prescription = prescription;
        this.session = session;
        this.original = null;
    }

    /**
     * Returns a detached subtree, i.e. the node and edge properties of each node are disjoint
     * from the node and edge properties of the original node.
     * @return
     */
    public TreeStorageNodeInstance<V> asDetachedSubtree() {
        return cloneTree(TreeStorageNodeInstance::cloneNodeOnly);
    }

    /**
     * Returns a detached subtree, i.e. the node and edge properties of each node are disjoint
     * from the node and edge properties of the original node.
     * @param <R>
     * @param filter
     * @param cloner
     * @return
     */
    public <R extends TreeNode<R>> R asDetachedSubtree(Function<TreeStorageNodeInstance<V>, R> cloner, Predicate<? super TreeStorageNodeInstance<V>> filter) {
        return cloneTree(n -> filter.test(n) ? cloner.apply(n) : null);
    }

    public TreeStorageNodeInstance<V> asDetachedBranchToParent() {
        final TreeStorageNodeInstance<V> res = cloneNodeOnly();
        final Optional<TreeStorageNodeInstance<V>> parent = getParent();
        if (parent.isPresent()) {
            TreeStorageNodeInstance<V> pCloned = parent.get().asDetachedBranchToParent();
            pCloned.addChild(res);
            return pCloned;
        }
        return res;
    }

    public KeycloakSession getKeycloakSession() {
        return session;
    }

    public TreeStorageNodeInstance<V> cloneNodeOnly() {
        return new TreeStorageNodeInstance<>(this.original == null ? this : this.original,
          getTreeProperties(),
          getNodeProperties() == null ? null : new HashMap<>(getNodeProperties()),
          getEdgeProperties() == null ? null : new HashMap<>(getEdgeProperties())
        );
    }

    @Override
    public String getId() {
        return prescription.getId();
    }

    @SuppressWarnings("unchecked")
    public <M> MapStorage<V, M> getStorage(Flag... flags) {
        @SuppressWarnings("unchecked")
        Class<M> modelClass = getTreeProperty(MODEL_CLASS, Class.class).orElseThrow(() -> new IllegalStateException("Undefined model class."));
        @SuppressWarnings("unchecked")
        Optional<StorageSupplier> storageSupplier = getNodeProperty(NodeProperties.STORAGE_SUPPLIER, StorageSupplier.class);

        final MapStorage<V, M> res = storageSupplier
          .map(supp -> (MapStorage<V, M>) supp.getStorage(session, modelClass, flags))
          .orElse(EmptyMapStorage.<V, M>getInstance());

        if (res instanceof MapStorage.Partial) {
            final Optional<MappersMap> mappersMap = getNodeProperty(NodeProperties.STORE_MAPPERS, MappersMap.class);

            mappersMap.ifPresent(((MapStorage.Partial) res)::setMappers);
        }

        return res;
    }

    public AuthoritativeStatus getAuthoritativeStatus(DefaultModelCriteria<?> criteria) {
        return prescription.getAuthoritativeStatus(criteria);
    }

    /**
     * Returns {@code true} if the objects originating in the child node with the {@code providerId} ID
     * and known to this node should always be validated before being returned to the caller.
     * <p>
     * Example:
     * <ul>
     * <li>Setup: JPA node above LDAP node.</li>
     * <li>Scenario: It may be required to always validate a LDAP user, regardless of whether its mirror is found in JPA storage.</li>
     * </ul>
     *
     * @param providerId
     * @return
     */
    public boolean shouldValidate(String providerId) {
        return prescription.shouldValidate(providerId);
    }

    public boolean isReadOnly() {
        return prescription.isReadOnly();
    }

    // TODO: Find out a better name
    public boolean mayNotContainAllFields() {
        return prescription.mayNotContainAllFields();
    }

    public FieldContainedStatus isCacheFor(EntityField<V> field, Object parameter) {
        return prescription.isCacheFor(field, parameter);
    }

    public FieldContainedStatus isPrimarySourceFor(EntityField<V> field, Object parameter) {
        return prescription.isPrimarySourceFor(field, parameter);
    }

    void setNodeAuthoritative(boolean authoritative) {
        nodeAuthoritative = authoritative;
    }

    public boolean isNodeAuthoritative() {
        return nodeAuthoritative;
    }

    public Completeness getStorageCompleteness() {
        return prescription.getStorageCompleteness();
    }

    public Class<V> getEntityClass() {
        return getTreeProperty(TreeProperties.ENTITY_CLASS, Class.class).orElseThrow(() -> new IllegalArgumentException("Unknown entity class"));
    }

    public <M> Class<M> getModelClass() {
        return getTreeProperty(TreeProperties.MODEL_CLASS, Class.class).orElseThrow(() -> new IllegalArgumentException("Unknown model class"));
    }

    /**
     * Applies mappers (if any) to the entity in this node.
     * @return
     */
    @SuppressWarnings("unchecked")
    V getMappedEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        return (V) getNodeProperty(NodeProperties.STORE_MAPPERS, MappersMap.class)
          .map(mappers -> MappingEntityFieldDelegate.delegate(mappers, entity, getEntityClass(), getStorageCompleteness()))
          .orElse((V) entity);
    }


}
