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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.tree.TreeStorageNodeInstance;
import java.util.List;
import org.keycloak.models.map.storage.tree.TreeAwareMapTransaction;
import org.keycloak.storage.StorageId;
import java.util.Arrays;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageTreeConnector {}
//public class ConcurrentHashMapStorageTreeConnector<K, V extends AbstractEntity & UpdatableEntity, M> implements TreeAwareMapTransaction<V, M> {
//
//    private final ConcurrentHashMapStorage<K, V, M> map;
//
//    public ConcurrentHashMapStorageTreeConnector(ConcurrentHashMapStorage<K, V, M> map) {
//        this.map = map;
//    }
//
//    @Override
//    public V createdInSubnode(List<TreeStorageNodeInstance<V>> pathFromCreatorToNode, V valueFromChild, V original) {
//        return valueFromChild;
//    }
//
//    @Override
//    public V loadedInSubnode(TreeStorageNodeInstance<V> childNode, V entity, Function<V, V> filterCachedFieldsOnly) {
//        V res = DeepCloner.DUMB_CLONER.from(filterCachedFieldsOnly.apply(entity));
//
//        ((EntityWithAttributes) res).setAttribute("originNode", Arrays.asList(childNode.getId()));
//        return res;
//    }
//
//    @Override
//    public V validate(List<TreeStorageNodeInstance<V>> pathFromReaderToNode, V valueFromParent) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public StorageId getOriginalStorageId(V entity) {
//        return new StorageId(((EntityWithAttributes) entity).getAttribute("originNode").get(0), entity.getId());
//    }
//
//}
