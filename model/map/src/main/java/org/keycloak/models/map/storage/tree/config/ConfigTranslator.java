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
package org.keycloak.models.map.storage.tree.config;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.tree.NodeProperties;
import org.keycloak.models.map.storage.tree.StorageSupplier;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author hmlnarik
 */
public class ConfigTranslator {

    private static final Logger LOG = Logger.getLogger(ConfigTranslator.class);
    private static final ThreadLocal<Yaml> YAML_INSTANCE = new ThreadLocal<Yaml>() {
        @Override
        protected Yaml initialValue() {
            return new Yaml();
        }
    };

    public static final String CHILD_STORES = "child-stores";
    public static final String STORES = "stores";
    public static final String MAPPERS = "mappers";
    public static final String COMPONENT = "component";
    public static final String ONLY = "only";

    private final KeycloakSessionFactory factory;
    private final String realmId;
    private final String uniqueId;

    public ConfigTranslator(KeycloakSessionFactory factory, String realmId, String uniqueId) {
        this.factory = factory;
        this.realmId = realmId;
        this.uniqueId = uniqueId;
    }

    static String getString(Map<String, Object> map, String key) {
        return getAs(map, key, String.class, null);
    }

    static Boolean getBoolean(Map<String, Object> map, String key) {
        return getAs(map, key, Boolean.class, () -> Boolean.valueOf(getString(map, key)));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> getMap(Map<String, Object> map, String key) {
        return getAs(map, key, Map.class, null);
    }

    @SuppressWarnings("unchecked")
    static List<Object> getList(Map<String, Object> map, String key) {
        return getAs(map, key, List.class, null);
    }

    static <T> T getAs(Map<String, Object> map, String key, Class<T> expectedClass) {
        return getAs(map, key, expectedClass, null);
    }

    @SuppressWarnings("unchecked")
    static Stream<Map<String, Object>> streamOfMaps(Stream<?> st) {
        return st == null ? Stream.empty() : st
          .filter(Objects::nonNull)
          .filter(o -> {
              if (o instanceof Map) {
                  return true;
              }
              LOG.errorf("Expected a map, got %s", o == null ? null : o.getClass());
              return false;
          })
          .map(Map.class::cast);
    }

    private static <T> T getAs(Map<String, Object> map, String key, Class<T> expectedClass, Supplier<T> defaultValueFunc) {
        Object o = map == null ? null : map.get(key);
        if (o == null || expectedClass.isInstance(o)) {
            return expectedClass.cast(o);
        }
        LOG.debugf("Expected a %s, got %s for key %s near %s", expectedClass, o.getClass(), key, map == null ? null : map.keySet());
        return defaultValueFunc == null ? null : defaultValueFunc.get();
    }

    @SuppressWarnings("unchecked")
    public TreeStorageNodePrescription parseConfiguration(Reader input) throws IOException {
        // TODO: this is very rough, similar to DOM XML parsing. Improve parsing complexity, perhaps by using Yaml.parse()
        Object loaded = YAML_INSTANCE.get().load(input);
        if (! (loaded instanceof Map)) {
            throw new IllegalStateException("YAML does not contain tree store declaration.");
        }

        return translateRoot((Map<String, Object>) loaded);
    }

    public TreeStorageNodePrescription translateRoot(Map<String, Object> node) {
        Map<String, Object> tp = getMap(node, "tree-properties");
        Map<String, Object> treeProperties = tp == null ? new HashMap<>() : new HashMap<>(tp);

        AtomicInteger i = new AtomicInteger();
        List<TreeStorageNodePrescription> c = streamOfMaps(getList(node, STORES).stream())
          .map(m -> getStoreFromMap(m, treeProperties, "." + i.incrementAndGet()))
          .collect(Collectors.toList());

        if (c.size() == 1) {
            return c.get(0);
        }

        if (c.isEmpty()) {
            LOG.error("Configuration invalid");
            return null;
        }

        TreeStorageNodePrescription res = new TreeStorageNodePrescription(treeProperties);
        res.setId(".");
        c.forEach(res::addChild);
        return res;
    }

    private static final Pattern TREE_STORE_RESERVED_OPTIONS = Pattern.compile(
        Pattern.quote(STORES) + "|"
      + Pattern.quote(CHILD_STORES) + "|"
      + Pattern.quote(COMPONENT) + "|"
      + Pattern.quote(MAPPERS) + "\\[\\S+\\]|"
      + Pattern.quote(ONLY) + "\\[\\S+\\]"
    );

    private TreeStorageNodePrescription getStoreFromMap(Map<String, Object> providerMap, Map<String, Object> treeProperties, String suffix) {
        final HashMap<String, Object> nodeProperties = new HashMap<>();
        final HashMap<String, Object> edgeProperties = new HashMap<>();
        TreeStorageNodePrescription res = new TreeStorageNodePrescription(treeProperties, nodeProperties, edgeProperties);
        
        final String providerId = providerMap.keySet().iterator().next();
        MapStorageProviderFactory mspf = (MapStorageProviderFactory) factory.getProviderFactory(MapStorageProvider.class, providerId);

        if (mspf == null) {
            throw new IllegalArgumentException("Unknown storage type: " + providerId);
        }

        Map<String, Object> providerConfigMap = getMap(providerMap, providerId);
        final StorageSupplier storageSupplier;

        // Step 1: Determine storage supplier
        //    1.1. Either the config is stored in a realm component (storage configuration is in component config),
        //    1.2. Or the storage config is stored directly in the providerMap
        if (providerId.equals(COMPONENT)) {
            Object properties = providerMap.get(COMPONENT);
            String componentId = properties instanceof String ? (String) properties : getString(providerConfigMap, "id");
            if (componentId == null) {
                LOG.warnf("Invalid component definition, component ID expected near %s: %s", providerId, properties);
                return null;
            }
            res.setId(componentId);
            // Load config from the component
            // Component ID; this is not completely finished as of now
            storageSupplier = (session, modelClass, flags) -> {
                final MapStorageProvider provider = session.getComponentProvider(MapStorageProvider.class, componentId);
                if (provider == null) {
                    throw new IllegalArgumentException("Cannot construct a map storage provider for component " + componentId);
                }
                return provider.getStorage(modelClass, flags);
            };
            throw new IllegalArgumentException("Configuration by component is not supported at this moment");
        } else {
            // explicit configuration
            final String virtualComponentId = providerId + "-" + uniqueId + suffix;
            String componentId = getString(providerConfigMap, "id");
            res.setId(componentId == null ? virtualComponentId : componentId);
            storageSupplier = (session, modelClass, flags) -> {
                final MapStorageProvider provider = session.getComponentProvider(MapStorageProvider.class, virtualComponentId, ksf -> getStoreConfig(virtualComponentId, providerId, nodeProperties));
                if (provider == null) {
                    throw new IllegalArgumentException("Cannot construct a map storage provider for component " + virtualComponentId);
                }
                return provider.getStorage(modelClass, flags);
            };
        }

        nodeProperties.put(NodeProperties.STORAGE_SUPPLIER, storageSupplier);
        nodeProperties.put(NodeProperties.STORAGE_PROVIDER, providerId);
        nodeProperties.put(NodeProperties.STORAGE_COMPLETENESS, mspf instanceof MapStorageProviderFactory.Partial ? Completeness.PARTIAL : Completeness.NATIVE);

        if (providerConfigMap == null) {
            return res;
        }

        // Step 2: Convert the map with storage provider configuration into tree node and edge options
        //    2.1. All keys that are not reserved are put directly into node properties
        //    2.2. All keys that are under "only[M]" are put into the node as node properties that are
        //         restricted to model type M
        //    2.3. Parse mappers
        MapperTranslator mapperTranslator = new MapperTranslator(factory);
        for (Entry<String, Object> me : providerConfigMap.entrySet()) {
            String configKey = me.getKey();
            if (! TREE_STORE_RESERVED_OPTIONS.matcher(configKey).matches()) {
                nodeProperties.put(configKey, me.getValue());
            } else if (configKey.startsWith(ONLY + "[")) {
                final String entityRestriction = configKey.substring(ONLY.length() + 1, configKey.length() - 1);
                Class<Object> mc = ModelEntityUtil.getModelClass(entityRestriction);
                if (mc == null || ! (me.getValue() instanceof Map)) {
                    LOG.warnf("Illegal use of \"only\" key, ignoring: %s", configKey);
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> restrictedConfig = (Map<String, ?>) me.getValue();
                    // TODO: Should all props go to nodeProperties?
                    restrictedConfig.forEach((k, v) -> nodeProperties.put(k + "[" + entityRestriction + "]", v));
                }
            } else if (configKey.startsWith(MAPPERS + "[")) {
                final String entityRestriction = configKey.substring(MAPPERS.length() + 1, configKey.length() - 1);
                Class<Object> mc = ModelEntityUtil.getModelClass(entityRestriction);
                if (mc == null || ! (me.getValue() instanceof Map)) {
                    LOG.warnf("Invalid key, unknown model name: %s", configKey);
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, ?> restrictedConfig = (Map<String, ?>) me.getValue();
                    mapperTranslator.parse(ModelEntityUtil.getEntityType(mc), restrictedConfig, mspf);
                }
            }
        }

        if (! mapperTranslator.getMappers().isEmpty()) {
            nodeProperties.put(NodeProperties.STORE_MAPPERS, mapperTranslator.getMappers());
        }

        // Step 3: Parse all the child stores
        final List<Object> childStores = getList(providerConfigMap, CHILD_STORES);
        if (childStores != null) {
            AtomicInteger i = new AtomicInteger();
            streamOfMaps(childStores.stream())
              .map(map -> getStoreFromMap(map, treeProperties, suffix + "." + i.incrementAndGet()))
              .forEach(res::addChild);
        }

        return res;
    }

    private ComponentModel getStoreConfig(String id, String providerId, Map<String, Object> storeConfig) {
        return new MapStoreComponentConfig(realmId, id, providerId, storeConfig);
    }

    public static class MapStoreComponentConfig extends ComponentModel {

        private final String realmId;
        private final String id;
        private final String providerId;
        private final Map<String, ? extends Object> storeConfig;

        public static MapStoreComponentConfig from(ComponentModel model) {
            if (model == null || (model instanceof MapStoreComponentConfig)) {
                return (MapStoreComponentConfig) model;
            }
            if (MapStorageProvider.class.getName().equals(model.getProviderType())) {
                final String config = model.get("configYaml");
                if (config != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = YAML_INSTANCE.get().loadAs(config, Map.class);
                    LOG.debugf("Loading config from configYaml, ignoring any other configuration in component %s", model.getId());
                    return new MapStoreComponentConfig(model.getId(), model.getParentId(), model.getProviderId(), configMap);
                }

                return new MapStoreComponentConfig(model.getId(), model.getParentId(), model.getProviderId(), model.getConfig());
            }
            return null;
        }

        public MapStoreComponentConfig(String id, String realmId, String providerId, Map<String, ? extends Object> storeConfig) {
            this.id = id;
            this.realmId = realmId;
            this.providerId = providerId;
            this.storeConfig = storeConfig == null ? Collections.emptyMap() : storeConfig;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getProviderId() {
            return providerId;
        }

        @Override
        public String getParentId() {
            return realmId;
        }

        @Override
        public String getProviderType() {
            return MapStorageProvider.class.getName();
        }

        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException("Cannot update store config here");
        }

        @Override
        public String get(String key) {
            final Object option = getObject(key);
            return option instanceof String ? (String) option : null;
        }

        public Object getObject(String key) {
            return storeConfig.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MultivaluedHashMap<String, String> getConfig() {
            if (storeConfig instanceof MultivaluedHashMap) {
                return (MultivaluedHashMap<String, String>) storeConfig;
            }

            return new MultivaluedHashMap<>(storeConfig.entrySet().stream()
              .filter(me -> me.getValue() instanceof String)
              .collect(Collectors.toMap(
                Map.Entry::getKey,
                me -> Collections.singletonList(Objects.toString(me.getValue(), null))
              )));
        }
    }

}
