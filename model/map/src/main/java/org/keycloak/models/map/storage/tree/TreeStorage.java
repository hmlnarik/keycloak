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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.tree.TreeNode.PathOrientation;
import org.keycloak.storage.SearchableModelField;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jboss.logging.Logger;
import static org.keycloak.models.map.storage.tree.TreeProperties.DEFAULT_STORE_CREATE;

public class TreeStorage<V extends AbstractEntity, M> implements MapStorage<V, M> {

    private static final Logger LOG = Logger.getLogger(TreeStorage.class);

    protected final TreeStorageNodePrescription root;
    protected final SearchableModelField<M> idField;

    public TreeStorage(TreeStorageNodePrescription root, SearchableModelField<M> idField) {
        this.root = root;
        this.idField = idField;
    }

    public V read(String key) {

//        TreeStorageNodeInstance<V,M> t = getAuthoritativeStoreSubtree(root, createCriteriaBuilder().compare(modelField, Operator.EQ, key));

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        Optional<List<TreeStorageNodeInstance<V, M>>> pathOp = determineAuthoritativeStorage(
//          key, getTreePropertyString(DEFAULT_STORE_READ), PathOrientation.TOP_FIRST, t -> t.isAuthoritativeForRead(key)
//        ) .map(Optional::of)
//          .orElseGet(() -> determineLeftmostBottommostAuthoritativeStorageByFunc(TreeStorage::hasNoChildren, PathOrientation.TOP_FIRST));
//        List<TreeStorageNodeInstance<V, M>> path = pathOp.orElseThrow(() -> new IllegalStateException("Cannot find storage to read value from: " + key));
//
//        // Now the path contains at least one node, starts with the root node
//        // and finishes with the bottommost authoritative node.
//        // There may be several authoritative nodes for reading on that path
//        // (e.g. JPA and LDAP for LDAP in sync mode)!
//
//        //    _______            _______   Example  _____            ______
//        //   /   0   \          /   1   \          / 2 * \          / 3 *  \        Path index (* indicates authoritative)
//        //  | default | ------ |  cache  | ------ |  JPA  | ------ |  LDAP  |
//        //   \_______/          \_______/          \_____/          \______/
//        //     top                                                   bottom         Original tree depth
//
//        // First try to read from caches which are towards the top, and get to
//        // more in-depth (and usually also more costly) storages later
//        ListIterator<TreeStorageNodeInstance<V, M>> it = path.listIterator();
//        TreeStorageNodeInstance<V, M> currentStorageNode;
//        V res;
//        do {
//            currentStorageNode = it.next();
//            res = currentStorageNode.getStorage().read(key);
//        } while (res == null && it.hasNext());
//
//        if (res == null) {
//            // None of the authoritative storages found the entry.
//            return null;
//        }
//
//        List<TreeStorageNodeInstance<V, M>> nextPath = path.subList(it.nextIndex(), path.size());
//        List<TreeStorageNodeInstance<V, M>> previousPath = path.subList(0, it.previousIndex());
//
//        //
//        //    _______            _______            _____            ______
//        //   /   0   \          /   1   \          / 2 * \          / 3 *  \        Path index (* indicates authoritative)
//        //  | default | ------ |  cache  | ------ |  JPA  | ------ |  LDAP  |
//        //   \_______/          \_______/          \_____/          \______/
//        //     ^^^^^            ^      ^                \____________/
//        //  previousPath      res, currentStorageNode      nextPath
//
//        int previousStorageIndex = it.previousIndex();
//        // Now validate all storages below the current storage node
//
//        if (currentStorageNode.getNodeProperty(NodeProperties.REVALIDATE, Boolean.class).orElse(Boolean.TRUE)) {
////            final String originalId = currentStorageNode.getTreeStorageComponent().getOriginalId(res);
//
//            final int lowestAuthoritativeStorageIndex = it.nextIndex();
//            for (ListIterator<TreeStorageNodeInstance<V, M>> authIt = path.listIterator(previousStorageIndex); authIt.hasPrevious() && authIt.previousIndex() >= lowestAuthoritativeStorageIndex;) {
//                TreeStorageNodeInstance<V, M> n = authIt.previous();
//                if (n.isAuthoritativeForRead(key) || hasNoChildren(n)) {
//                    V r = res;
////                    final Function<MapStorageInTree<V, M>, V> validateFunc = m -> m.validate(r);
////                    res = n.getTreeStorageComponent().map(validateFunc).orElseGet(() -> n.getStorage().read(key) != null ? r : null);
//                    if (currentStorageNode.getNodeProperty(NodeProperties.REVALIDATE, Boolean.class).orElse(Boolean.FALSE)) {
//
//                    }
//                }
//            }
//        }
////        res = validate(res);
//
//
//        return res;
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        return new TreeStorageTransaction<>(session, root.instantiate(session), idField);
    }
}
