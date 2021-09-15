/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 *
 * @author hmlnarik
 */
public class TreeStorageNodeInstanceTest<V extends AbstractEntity>  {

    private static final String DEFAULT_STORE_READ_VALUE = "dfStoreRead";
    private static final String NODE_PROPERTY = "NODE_PROPERTY";
    private static final String EDGE_PROPERTY = "EDGE_PROPERTY";

    private static final Logger LOG = Logger.getLogger(TreeStorageNodeInstanceTest.class.getName());

    private final KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(
      TreeStorageNodeInstanceTest.class.getClassLoader(), new Class[] { KeycloakSession.class },
      (proxy, method, args) -> { throw new IllegalStateException(); });

    public Map<String, Object> treeProperties = new HashMap<>();
    {
        treeProperties.put(TreeProperties.DEFAULT_STORE_READ, DEFAULT_STORE_READ_VALUE);
    }

    private TreeStorageNodePrescription createPrescription(String id) {
        TreeStorageNodePrescription res = new TreeStorageNodePrescription(treeProperties);
        res.setId(id);
        res.setNodeProperty(NODE_PROPERTY, NODE_PROPERTY + "-" + id);
        res.setEdgeProperty(EDGE_PROPERTY, EDGE_PROPERTY + "-" + id);
        return res;
    }

    @Test
    public void testInstantiate() {
        final TreeStorageNodePrescription tsnp = createPrescription("id1");
        assertThat(tsnp.getTreeProperties(), sameInstance(treeProperties));
        
        TreeStorageNodeInstance<?> tsni = tsnp.instantiate(session);
        assertThat(tsni, notNullValue());
        assertThat(tsni.getId(), is("id1"));
        assertThat(tsni.getTreeProperties(), sameInstance(treeProperties));
        assertThat(tsni.getNodeProperties(), equalTo(tsnp.getNodeProperties()));
        assertThat(tsni.getNodeProperties(), not(sameInstance(tsnp.getNodeProperties())));
        assertThat(tsni.getEdgeProperties(), equalTo(tsnp.getEdgeProperties()));
        assertThat(tsni.getEdgeProperties(), not(sameInstance(tsnp.getEdgeProperties())));
    }

    @Test
    public void testClone() {
        final TreeStorageNodePrescription tsnp = createPrescription("id1");
        assertThat(tsnp.getTreeProperties(), sameInstance(treeProperties));

        TreeStorageNodeInstance<V> tsni = tsnp.instantiate(session);
        TreeStorageNodeInstance<V> tsniCloned = tsni.cloneTree(TreeStorageNodeInstance::new);
        assertThat(tsniCloned, notNullValue());
        assertThat(tsniCloned.getId(), is("id1"));
        assertThat(tsniCloned.getTreeProperties(), sameInstance(treeProperties));
        assertThat(tsniCloned.getNodeProperties(), sameInstance(tsni.getNodeProperties()));
        assertThat(tsniCloned.getEdgeProperties(), sameInstance(tsni.getEdgeProperties()));
    }

    @Test
    public void testDetachedNodeClone() {
        final TreeStorageNodePrescription tsnp = createPrescription("id1");

        TreeStorageNodeInstance<V> tsni = tsnp.instantiate(session);
        TreeStorageNodeInstance<V> tsniCloned = tsni.asDetachedSubtree();
        assertDetached(tsniCloned, tsni, n -> true);
    }

    @Test
    public void testDetachedTreeClone() {
        final TreeStorageNodePrescription tsnp1 = createPrescription("id1");
        final TreeStorageNodePrescription tsnp11 = createPrescription("id1.1");
        final TreeStorageNodePrescription tsnp12 = createPrescription("id1.2");
        final TreeStorageNodePrescription tsnp121 = createPrescription("id1.2.1");
        final TreeStorageNodePrescription tsnp122 = createPrescription("id1.2.2");
        tsnp1.addChild(tsnp11);
        tsnp1.addChild(tsnp12);
        tsnp12.addChild(tsnp121);
        tsnp12.addChild(tsnp122);

        TreeStorageNodeInstance<V> tsni1 = tsnp1.instantiate(session);
        assertDetached(tsni1.asDetachedSubtree(), tsni1, a -> true);
    }

    @Test
    public void testDetachedTreeCloneWithFilter() {
        final TreeStorageNodePrescription tsnp1 = createPrescription("id1");
        final TreeStorageNodePrescription tsnp11 = createPrescription("id1.1");
        final TreeStorageNodePrescription tsnp12 = createPrescription("id1.2");
        final TreeStorageNodePrescription tsnp13 = createPrescription("id1.3");
        final TreeStorageNodePrescription tsnp121 = createPrescription("id1.2.1");
        final TreeStorageNodePrescription tsnp122 = createPrescription("id1.2.2");
        tsnp1.addChild(tsnp11);
        tsnp1.addChild(tsnp12);
        tsnp1.addChild(tsnp13);
        tsnp12.addChild(tsnp121);
        tsnp12.addChild(tsnp122);

        TreeStorageNodeInstance<V> tsni1 = tsnp1.instantiate(session);
        Predicate<TreeStorageNodeInstance<V>> filter = n -> n.getId().matches("id1(\\.2.*)?");
        Function<TreeStorageNodeInstance<V>, TreeStorageNodeInstance<V>> cloner = TreeStorageNodeInstance::cloneNodeOnly;
        TreeStorageNodeInstance<V> v = tsni1.asDetachedSubtree(cloner, filter);
        assertDetached(v, tsni1, filter);
        assertThat(v.getChild("id1.1"), is(Optional.empty()));
        assertThat(v.getChild("id1.2"), not(is(Optional.empty())));

        LOG.log(Level.INFO, "{0}", tsni1);
        LOG.log(Level.INFO, "{0}", v);
    }

    private void assertDetached(TreeStorageNodeInstance<V> actual, TreeStorageNodeInstance<V> expected, Predicate<? super TreeStorageNodeInstance<V>> childFilter) {
        assertThat(actual, notNullValue());
        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getTreeProperties(), sameInstance(treeProperties));
        assertThat(actual.getNodeProperties(), not(sameInstance(expected.getNodeProperties())));
        assertThat(actual.getNodeProperties(), equalTo(expected.getNodeProperties()));
        assertThat(actual.getEdgeProperties(), not(sameInstance(expected.getEdgeProperties())));
        assertThat(actual.getEdgeProperties(), equalTo(expected.getEdgeProperties()));

        List<TreeStorageNodeInstance<V>> expectedChildren = actual.getChildren().stream().filter(childFilter).collect(Collectors.toList());
        assertThat(actual.getChildren().stream().map(n -> n.getId()).collect(Collectors.toList()),
          equalTo(expectedChildren.stream().map(n -> n.getId()).collect(Collectors.toList())));

        for (TreeStorageNodeInstance<V> child : actual.getChildren()) {
            assertDetached(child, expected.getChild(child.getId()).orElseThrow(() -> new AssertionError("Cannot find ")), childFilter);
        }
    }

}
