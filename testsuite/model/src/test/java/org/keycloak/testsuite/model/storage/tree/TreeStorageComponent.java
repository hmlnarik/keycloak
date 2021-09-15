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
package org.keycloak.testsuite.model.storage.tree;

import org.keycloak.common.util.EnumUtils;
import org.keycloak.component.PrioritizedComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.tree.EdgeProperties;
import org.keycloak.models.map.storage.tree.NodeProperties;
import org.keycloak.models.map.storage.tree.TreeProperties;
import org.keycloak.models.map.storage.tree.TreeStorage;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription;
import org.keycloak.models.map.storage.tree.TreeStorageProviderFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class TreeStorageComponent<Self extends TreeStorageComponent<Self>> extends PrioritizedComponentModel {

    private static final Logger LOG = Logger.getLogger(TreeStorageComponent.class);

    private static final String TREE_PROPERTY_PREFIX = "tp.";
    private static final String NODE_PROPERTY_PREFIX = "np.";
    private static final String EDGE_PROPERTY_PREFIX = "ep.";

    private static final Map<String, Class<?>> MODEL_CLASS_VALUES = new HashMap<>();
    static {
        MODEL_CLASS_VALUES.put("clients", ClientModel.class);
    }

    public TreeStorageComponent() {
        setProviderType(MapStorageProvider.class.getName());
        setProviderId(TreeStorageProviderFactory.PROVIDER_ID);
    }

    public TreeStorageComponent(Self copy) {
        super(copy);
        if (! Objects.equals(copy.getProviderType(), TreeStorage.class.getName())) {
            throw new IllegalArgumentException("Invalid component type: " + copy.getProviderType());
        }
    }

    public <V extends AbstractEntity, M> TreeStorageNodePrescription toTreeStorageNode() {
        final Map<String, Object> treeProperties = getConfig().entrySet().stream()
          .filter(TreeStorageComponent::isTreeProperty)
          .collect(Collectors.toMap(me -> me.getKey().substring(3), TreeStorageComponent::toTreeProperty));

        return toTreeStorageNode(treeProperties, "0.");
        
    }

    private <V extends AbstractEntity, M> TreeStorageNodePrescription toTreeStorageNode(Map<String, Object> treeProperties, String prefix) {
        String npPrefix = prefix + NODE_PROPERTY_PREFIX;
        String epPrefix = prefix + EDGE_PROPERTY_PREFIX;
        Map<String, Object> nodeProperties = getConfig().entrySet().stream()
          .filter(me -> me.getKey().startsWith(npPrefix))
          .collect(Collectors.toMap(me -> me.getKey().substring(npPrefix.length()), me -> toNodeProperty(me.getKey().substring(npPrefix.length()), me.getValue())));
        Map<String, Object> edgeProperties = getConfig().entrySet().stream()
          .filter(me -> me.getKey().startsWith(epPrefix))
          .collect(Collectors.toMap(me -> me.getKey().substring(epPrefix.length()), me -> toEdgeProperty(me.getKey().substring(epPrefix.length()), me.getValue())));
        TreeStorageNodePrescription res = new TreeStorageNodePrescription(treeProperties, nodeProperties, edgeProperties);
        return res;
    }


    private static boolean isTreeProperty(Map.Entry<String, List<String>> me) {
        return me.getKey().startsWith(TREE_PROPERTY_PREFIX);
    }

    private static Object toTreeProperty(Map.Entry<String, List<String>> me) {
        List<String> value = me.getValue();
        switch (me.getKey().substring(TREE_PROPERTY_PREFIX.length())) {
            case TreeProperties.DEFAULT_STORE_CREATE:
                return value == null || value.isEmpty() ? null : value.get(0);
            case TreeProperties.MODEL_CLASS:
                return value == null || value.isEmpty() ? null : MODEL_CLASS_VALUES.get(value.get(0));
        }
        LOG.warnf("Unrecognized parameter: %s", me.getKey());
        return null;
    }

    private static Object toNodeProperty(String key, List<String> value) {
        switch (key) {
            case NodeProperties.READ_ONLY:
                return value == null || value.isEmpty() ? null : Boolean.valueOf(value.get(0));
        }
        LOG.warnf("Unrecognized parameter: %s", key);
        return null;
    }

    private static Object toEdgeProperty(String key, List<String> value) {
        switch (key) {
            case EdgeProperties.VALIDATE:
                return value == null || value.isEmpty()
                  ? EdgeProperties.Validate.NEVER
                  : EnumUtils.safeValueOf(value.get(0), EdgeProperties.Validate.NEVER);
        }
        LOG.warnf("Unrecognized parameter: %s", key);
        return null;
    }

}
