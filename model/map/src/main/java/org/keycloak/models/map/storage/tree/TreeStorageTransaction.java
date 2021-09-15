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

import org.keycloak.models.map.storage.ReadOnlyMapTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.delegate.PerFieldDelegateProvider;
import org.keycloak.models.map.storage.CriterionNotSupportedException;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.tree.AuthoritativeDecider.AuthoritativeStatus;
import org.keycloak.models.map.storage.tree.TreeNode.PathOrientation;
import org.keycloak.models.map.storage.tree.TreeStorageNodeInstance.WithEntity;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.storage.StorageId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import static org.keycloak.models.map.storage.tree.NodeProperties.AUTHORITATIVE_NODES;
import static org.keycloak.models.map.storage.tree.NodeProperties.READ_ONLY;

/**
 *
 * @author hmlnarik
 */
public class TreeStorageTransaction<V extends AbstractEntity, M> implements MapKeycloakTransaction<V, M> {

    private static final Logger LOG = Logger.getLogger(TreeStorageTransaction.class);

    private final KeycloakSession session;
    private final TreeStorageNodeInstance<V> root;
    private final SearchableModelField<M> idField;
    private final ConcurrentHashMap<TreeStorageNodeInstance<V>, MapKeycloakTransaction<V, ?>> transactions = new ConcurrentHashMap<>();
    private boolean active;
    private boolean rollback;

    public TreeStorageTransaction(KeycloakSession session, TreeStorageNodeInstance<V> root, SearchableModelField<M> idField) {
        this.session = session;
        this.root = root;
        this.idField = idField;
    }

    @Override
    public void begin() {
        this.active = true;
        transactions.values().forEach(MapKeycloakTransaction::begin);
    }

    @Override
    public void commit() {
        transactions.values().forEach(MapKeycloakTransaction::commit);
    }

    @Override
    public void rollback() {
        transactions.values().forEach(MapKeycloakTransaction::rollback);
    }

    @Override
    public void setRollbackOnly() {
        this.rollback = true;
        transactions.values().forEach(MapKeycloakTransaction::setRollbackOnly);
    }

    @Override
    public boolean getRollbackOnly() {
        return this.rollback || transactions.values().stream().anyMatch(MapKeycloakTransaction::getRollbackOnly);
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<V, M> getTransaction(TreeStorageNodeInstance<V> node) {
        return (MapKeycloakTransaction<V, M>) transactions.computeIfAbsent(node, this::createStorageTransaction);
    }

    private MapKeycloakTransaction<V, M> createStorageTransaction(TreeStorageNodeInstance<V> n) {
        final MapStorage<V, M> storage = n.getStorage();
        MapKeycloakTransaction<V, M> res = storage.createTransaction(session);
        if (n.isReadOnly()) {
            res = new ReadOnlyMapTransaction<>(res);
        }
        return res;
    }

    private V wrapReadObject(TreeStorageNodeInstance<V>.WithEntity readNodeWithEntity) {
        // readNodeWithEntity could be read from any storage higher in the tree than the one that owns the object,
        // e.g. one that provides caching or one that supplements fields that cannot be stored within primary store.
        //
        // Who ultimately owns this entity within readNodeWithEntity, i.e. which store determines the very existence of the readObject?
        //
        // It certainly is one of the leaf storages, but there may be a subtree below the "origin" node,
        // including a nonlinear subtree.
        //
        // We do not care about ownership at this moment but would need to find it out when a setter would be used.
        // In other words, we need lazy loading of the entity from the owning storage in case of using a setter.
        // Therefore the node has to be recorded as well to be able to provide the storages that could own the object.
        // This is done in the wrapLazyDelegationToLowerEntity method.

        V objectToReturn = readNodeWithEntity.wrapLazyDelegationToLowerEntity();

        // The remaining thing is to allow the stores from above to process / encapsulate the returned entity
        // in their logic and process the mappers along the way.
        // TODO:
        // TreeStorageNodeInstance<V> readNode = readNodeWithEntity.getNode();
        // objectToReturn = readNode.applyMappers(objectToReturn);

        for (Iterator<TreeStorageNodeInstance<V>> iterator = readNodeWithEntity.getNode().getParentsStream().iterator(); iterator.hasNext();) {
            TreeStorageNodeInstance<V> nextParent = iterator.next();
            TreeStorageNodeInstance<V> prevNode = readNodeWithEntity.getNode();
            final V otr = objectToReturn;
            readNodeWithEntity = nextParent.getTreeAwareMapTransaction()
              .map(t -> t.loadedInSubnode(prevNode, otr))
              .map(e -> e == otr ? null : nextParent.new WithEntity(e, this))
              .orElse(readNodeWithEntity);

            objectToReturn = readNodeWithEntity.getEntity();
            // TODO:
            // objectToReturn = nextParent.applyMappers(objectToReturn);
        }

        return objectToReturn;
    }

    /**
     * Returns a predicate which, given a node, returns {@code true} if the object with key {@code key} exists in the storage
     * of the given node, or {@code false} otherwise. This predicate checks existence by reading the object from that storage
     * and if the object is read, it sets the reference to the read object into {@code readObjectRef}.
     * <p>
     * If there is a requirement to validate the object by the store it came from, it is validated prior to be returned.
     *
     * @param key
     * @param readObjectRef
     * @return see description.
     */
    Predicate<TreeStorageNodeInstance<V>> readFromNodePredicate(AtomicReference<TreeStorageNodeInstance<V>.WithEntity> readObjectRef, String key) {
        return nodePredicate(readObjectRef, n -> readFromNode(n, key));
    }

    /**
     * Reads and validates (if configured) an object with given ID from storage of the given node
     * @param n
     * @param id
     * @return Returns the read entity or {@code null} if not present / not valid.
     */
    V readFromNode(TreeStorageNodeInstance<V> n, String id) {
        return validateIfNeeded(n, getTransaction(n).read(id));
    }

    Stream<V> readFromNode(TreeStorageNodeInstance<V> n, Function<MapKeycloakTransaction<V, M>, Stream<V>> func) {
        return validateIfNeeded(n, func.apply(getTransaction(n)));
    }

    /**
     * Returns a predicate which, given a node, returns {@code true} if the object returned by {@code objSupplier}
     * is not {@code null}, or {@code false} otherwise. If the object exists, it sets the reference to
     * the object together with the node it has been obtained from into {@code readObjectRef}.
     *
     * @param objSupplier
     * @param readObjectRef
     * @return see description.
     */
    Predicate<TreeStorageNodeInstance<V>> nodePredicate(AtomicReference<TreeStorageNodeInstance<V>.WithEntity> readObjectRef, Function<TreeStorageNodeInstance<V>, V> objSupplier) {
        return n -> {
            final V obj = objSupplier.apply(n);
            if (obj != null) {
                // TODO: Once we migrate to Java 9+, use setPlain rather than set, no synchronization is needed here
                readObjectRef.set(n.new WithEntity(obj, this));
            }
            return obj != null;
        };
    }

    private V validateIfNeeded(TreeStorageNodeInstance<V> origin, V entity) {
        if (origin == null || entity == null || origin.hasNoChildren()) {
            return entity;
        }

        final Optional<TreeAwareMapTransaction<V, Object>> treeStorageComponent = origin.getTreeAwareMapTransaction();
        StorageId storageId = treeStorageComponent
          .map(t -> t.getOriginalStorageId(entity))
          .orElse(new StorageId(entity.getId()));

        if (storageId.isLocal()) {
            LOG.warnf("Cannot validate entity %s", entity);
            return entity;
        }

        if (! origin.shouldValidate(storageId.getProviderId())) {
            return entity;
        }

        return treeStorageComponent.map(t -> t.validate(origin, storageId.getExternalId(), entity)).orElse(null);
    }

    private Stream<V> validateIfNeeded(TreeStorageNodeInstance<V> origin, Stream<V> entityStream) {
        return entityStream
          .map(entity -> validateIfNeeded(origin, entity))
          .filter(Objects::nonNull);
    }

    @Override
    public V create(V value) {
//        Optional<List<TreeStorageNodeInstance<V>>> pathOp = determineAuthoritativeStorage(
//          value.getId(), getTreePropertyString(DEFAULT_STORE_CREATE), PathOrientation.BOTTOM_FIRST, t -> t.isAuthoritativeForCreate(value)
//        ) .map(Optional::of)
//          .orElseGet(() -> determineLeftmostBottommostAuthoritativeStorageByFunc(TreeStorage::hasNoChildrenRw, PathOrientation.BOTTOM_FIRST));
//        List<TreeStorageNodeInstance<V>> path = pathOp.orElseThrow(() -> new IllegalStateException("Cannot find storage to create value in: " + value));

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        final TreeStorageNodeInstance<V> authoritativeNode = path.get(0);
//        V res = authoritativeNode.getStorage().create(value);
//        for (int i = 1; i < path.size(); i ++) {
//            TreeStorageNodeInstance<V> node = path.get(i);
//            V r = res;
//            List<TreeStorageNodeInstance<V>> pathToNode = path.subList(0, i);
//            res = node.getTreeStorageComponent().map(tsc -> tsc.createdInSubnode(pathToNode, r, value)).orElse(res);
//        }
//
//        return res;
    }

    @Override
    public V read(String key) {
        DefaultModelCriteria<M> criteria = DefaultModelCriteria.<M> criteria().compare(idField, Operator.EQ, key);
        final Optional<TreeStorageNodeInstance<V>> authoritativeSubtree = determineAuthoritativeSubtree(criteria);

        if (! authoritativeSubtree.isPresent()) {
            return null;
        }

        AtomicReference<TreeStorageNodeInstance<V>.WithEntity> readObjectRef = new AtomicReference<>();
        Predicate<TreeStorageNodeInstance<V>> readFromNodePredicate = readFromNodePredicate(readObjectRef, key);

        return authoritativeSubtree
          .map(tree -> tree.findFirstBfs(readFromNodePredicate).orElse(null))
          .map(node -> wrapReadObject(readObjectRef.get()))
          .orElse(null);
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = queryParameters.getModelCriteriaBuilder();
        final Optional<TreeStorageNodeInstance<V>> authoritativeSubtree = determineAuthoritativeSubtree(criteria);

        if (! authoritativeSubtree.isPresent()) {
            return null;
        }

        Stream<TreeStorageNodeInstance<V>> authoritativeNodes = authoritativeSubtree
          .map(tree -> tree.getNodeProperty(AUTHORITATIVE_NODES, Collection.class).orElse(null))
          .map(Collection<TreeStorageNodeInstance<V>>::stream) // Optional of Stream of authoritative nodes
          .orElse(Stream.empty()); // Stream of authoritative nodes (may be empty)

        // List of nodes that have successfully returned read(queryParameters) operation. These
        // are required to ensure that no object is returned twice: if a parent node succeeds in
        // read, then there is no need to read from descendant nodes.
        Collection<TreeStorageNodeInstance<V>> successfulParents = new HashSet<>();

        // TODO: The following code does not handle ordering correctly
        Optional<Stream<TreeStorageNodeInstance<V>.WithEntity>> streamOfValuesFromAllAuthoritativeNodes = authoritativeNodes.sequential()
          // Find first node that is able to satisfy the query
          .map(node -> {
              try {
                  // If any of the parents has been read, do not read from this subnode
                  // as it would return the same results as contained in the parent node
                  // We cannot use stream filtering since the order of the .map() / .filter()
                  // operations is not guaranteed
                  if (node.getParentsStream().anyMatch(successfulParents::contains)) {
                      return null;
                  }
                  
                  final Stream<V> values = getTransaction(node).read(queryParameters);
                  successfulParents.add(node);
                  node.forEachParent(successfulParents::add);
                  return values.map(v -> node.new WithEntity(v, this));
              } catch (CriterionNotSupportedException e) {
                  LOG.debugf("The storage node did not recognize criteria. Does it implement TreeAwareMapTransaction.criteriaRecognized method correctly?", node);
                  return null;
              }
          })
          .filter(Objects::nonNull)
          .reduce(Stream::concat);

        return streamOfValuesFromAllAuthoritativeNodes
          .map(s -> s.map(this::wrapReadObject))
          .orElse(Stream.empty());
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = queryParameters.getModelCriteriaBuilder();
        final Optional<TreeStorageNodeInstance<V>> authoritativeSubtree = determineAuthoritativeSubtree(criteria);

        if (! authoritativeSubtree.isPresent()) {
            return 0;
        }

        Stream<TreeStorageNodeInstance<V>> authoritativeNodes = authoritativeSubtree
          .map(tree -> tree.getNodeProperty(AUTHORITATIVE_NODES, Collection.class).orElse(null))
          .map(Collection<TreeStorageNodeInstance<V>>::stream) // Optional of Stream of authoritative nodes
          .orElse(Stream.empty()); // Stream of authoritative nodes (may be empty)

        // List of nodes that have successfully returned getCount(queryParameters) operation. These
        // are required to ensure that no object is returned twice: if a parent node succeeds in
        // read, then there is no need to read from descendant nodes.
        Collection<TreeStorageNodeInstance<V>> successfulParents = new HashSet<>();

        return authoritativeNodes.sequential()
          // Find first node that is able to satisfy the query
          .map(node -> {
              try {
                  // If any of the parents has been read, do not read from this subnode
                  // as it would return the same results as contained in the parent node
                  // We cannot use stream filtering since the order of the .map() / .filter()
                  // operations is not guaranteed
                  if (node.getParentsStream().anyMatch(successfulParents::contains)) {
                      return null;
                  }

                  return getTransaction(node).getCount(queryParameters);
              } catch (CriterionNotSupportedException e) {
                  LOG.debugf("The storage node did not recognize criteria. Does it implement TreeAwareMapTransaction.criteriaRecognized method correctly?", node);
                  return null;
              }
          })
          .filter(Objects::nonNull)
          .reduce(0L, Long::sum);
    }

    @Override
    public boolean delete(String key) {
        // TODO
        return false;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        // TODO
//        return determineAuthoritativeSubtree(queryParameters.getModelCriteriaBuilder())
//          .map(this::getTransaction)
//          .map((MapKeycloakTransaction<V, M> t) -> t.delete(queryParameters))
//          .orElse(0L);
        return 0L;
    }

    private Optional<List<TreeStorageNodeInstance<V>>> determineAuthoritativeStorageById(
      StorageId id, String defaultStorageId, PathOrientation orientation) {
        String storageToSearchFor = id.getProviderId() == null ? defaultStorageId : id.getProviderId();

        if (storageToSearchFor == null) {
            return Optional.empty();
        }

        Predicate<TreeStorageNodeInstance<V>> predicate = n -> Objects.equals(storageToSearchFor, n.getId());

        return determineLeftmostBottommostAuthoritativeStorageByFunc(predicate, orientation);
    }

    private Optional<List<TreeStorageNodeInstance<V>>> determineLeftmostBottommostAuthoritativeStorageByFunc(
      Predicate<TreeStorageNodeInstance<V>> isAuthoritativeFunc, PathOrientation orientation) {
        return root.findFirstBottommostDfs(isAuthoritativeFunc)
          .map(n -> n.getPathToRoot(orientation))
          .map(Optional::of)
          .orElse(Optional.empty());
    }

    protected Optional<List<TreeStorageNodeInstance<V>>> determineAuthoritativeStorage(String id, String defaultStorageId,
      PathOrientation pathOrientation, Predicate<TreeStorageNodeInstance<V>> isAuthoritativeFunc) {
        // Determine authoritative storage
        StorageId storageId = new StorageId(id);

        return determineAuthoritativeStorageById(storageId, defaultStorageId, pathOrientation)
          .map(Optional::of)
          .orElseGet(() -> determineLeftmostBottommostAuthoritativeStorageByFunc(isAuthoritativeFunc, pathOrientation));
    }

    /**
     * Prepares a subtree of the store tree which is authoritative for the given criteria.
     * This subtree is either empty (no such node found) or always contains paths from the
     * authoritative nodes to the root node.
     * <p>
     * If a node claims to be strongly authoritative with respect to the given criteria,
     * the returned subtree is the same as the path from the root node to the authoritative node.
     * <p>
     * There may be several authoritative nodes that are on disjoint paths to the root node. The
     * resulting subtree is then equivalent to the union of these paths.
     * <p>
     * The {@link #AUTHORITATIVE_NODES} property of the root node of the returned subtree
     * contains the collection of all nodes that claim being authoritative for the given criteria.
     * This collection is guaranteed to be in the same order as visited via {@link TreeNode#findFirstBottommostDfs}.
     *
     * @return
     */
    protected Optional<TreeStorageNodeInstance<V>> determineAuthoritativeSubtree(DefaultModelCriteria<M> dmc) {
        // Map of potentially authoritative nodes together with their status
        Map<TreeStorageNodeInstance<V>, AuthoritativeStatus> authoritativeNodes = new LinkedHashMap<>();

        // Find the first strongly authoritative node or at least build the set of authoritative nodes
        Optional<TreeStorageNodeInstance<V>> stronglyAuthoritativeNode = root.findFirstBottommostDfs(n -> {
            AuthoritativeStatus as = n.getAuthoritativeStatus(dmc);

            switch (n.getAuthoritativeStatus(dmc)) {
                case AUTHORITATIVE_STRONGLY:
                    authoritativeNodes.put(n, as);
                    return true;

                case AUTHORITATIVE_MAYBE:
                case AUTHORITATIVE_NO:
                    authoritativeNodes.put(n, as);
                    break;

                default:
                    if (hasNoChildren(n)) {
                        authoritativeNodes.put(n, AuthoritativeStatus.AUTHORITATIVE_MAYBE);
                    }
            }

            return false;
        });

        if (authoritativeNodes.isEmpty()) {
            return Optional.empty();
        }

        // If strongly authoritative node is present, ignore all of the rest
        if (stronglyAuthoritativeNode.isPresent()) {
            authoritativeNodes.clear();
            authoritativeNodes.put(stronglyAuthoritativeNode.get(), AuthoritativeStatus.AUTHORITATIVE_MAYBE);
        }

        final LinkedHashMap<TreeStorageNodeInstance<V>, TreeStorageNodeInstance<V>> maybeAuthoritativeNodes = authoritativeNodes.entrySet().stream()
          .filter(me -> me.getValue() == AuthoritativeStatus.AUTHORITATIVE_MAYBE)
          .map(Map.Entry::getKey)
          .collect(Collectors.toMap(Function.identity(), Function.identity(), (t, u) -> t, LinkedHashMap::new));

        HashSet<TreeStorageNodeInstance<V>> transitiveParents = new HashSet<>();
        maybeAuthoritativeNodes.keySet().forEach(n -> {
            Optional<TreeStorageNodeInstance<V>> p = n.getParent();
            while (p.isPresent() && transitiveParents.add(p.get())) {   // IOW: add node until it is a parent that has already been added to transitiveParents
                p = p.get().getParent();
            }
        });

        TreeStorageNodeInstance<V> subtree = root.asDetachedSubtree(
          n -> { TreeStorageNodeInstance<V> res = n.cloneNodeOnly(); maybeAuthoritativeNodes.replace(n, res); return res; },
          n -> transitiveParents.contains(n) 
               || authoritativeNodes.containsKey(n) && n.<M>getTreeAwareMapTransaction().map(t -> (Boolean) t.criteriaRecognized(dmc)).orElse(false)
        );

        subtree.setNodeProperty(AUTHORITATIVE_NODES, maybeAuthoritativeNodes.values());
        maybeAuthoritativeNodes.values().forEach(n -> n.setNodeAuthoritative(true));
        return Optional.of(subtree);
    }

    private static boolean hasNoChildrenRw(TreeStorageNodeInstance<?> node) {
        return ! node.getNodeProperty(READ_ONLY, Boolean.class).orElse(Boolean.FALSE) && hasNoChildren(node);
    }

    private static boolean hasNoChildren(TreeStorageNodeInstance<?> node) {
        return node.getChildren().isEmpty();
    }

    private String getTreePropertyString(String property) {
        return root.getTreeProperty(property, String.class).orElse(null);
    }


}
