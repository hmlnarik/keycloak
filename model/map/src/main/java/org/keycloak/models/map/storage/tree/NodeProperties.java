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

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.mapper.DirectMapper;
import org.keycloak.models.map.storage.mapper.Mapper;
import org.keycloak.models.map.storage.mapper.MappersMap;
import java.util.Collection;

/**
 *
 * @author hmlnarik
 */
public final class NodeProperties {

    /**
     * Defines the filter that must be satisfied for every entity within this store.
     * Type: {@link DefaultModelCriteria}
     */
    public static final String ENTITY_RESTRICTION = "entity-restriction";
    public static final String AUTHORITATIVE_DECIDER = "authoritative-decider";
    public static final String READ_ONLY = "read-only";

    public static final String AUTHORITATIVE_NODES = "___authoritative-nodes___";
    public static final String STORAGE_PROVIDER = "___storage-provider___";
    public static final String STORAGE_SUPPLIER = "___storage-supplier___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is primary source for the values of all attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for the values of attributes "address" and "logo".
     * </ul>
     */
    public static final String PRIMARY_SOURCE_FOR = "___primary-source-for___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is not primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is not primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is not primary source for the values of any attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for attributes apart from "address" and "logo" attributes.
     * </ul>
     */
    public static final String PRIMARY_SOURCE_FOR_EXCLUDED = "___primary-source-for-excluded___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is primary source for the values of all attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for the values of attributes "address" and "logo".
     * </ul>
     */
    public static final String CACHE_FOR = "___cache-for___";

    /**
     * Map of pairs ({@code k}: {@link EntityField}, {@code v}: {@link Collection}) of fields that the node is not primary source for.
     * <p>
     * For example, the following statements are expressed:
     * <ul>
     *  <li>{@code (name -> null)}: This node is not primary source for the value of the field {@code name}.
     *  <li>{@code (attributes -> null)}: This node is not primary source for the values of any attributes.
     *  <li>{@code (attributes -> {"address", "logo"})}: This node is primary source only for attributes apart from "address" and "logo" attributes.
     * </ul>
     */
    public static final String CACHE_FOR_EXCLUDED = "___cache-for-excluded___";


    /**
     * Defines a {@link MappersMap} which maps an entity field to an entity mapper. The mappers
     * are applied <i>after</i> the entity is loaded from the store, and <i>before</i> it is stored in the store.
     * <p>
     * The direction <i>there</i> (realized by {@link Mapper#there} method)
     * is to obtain a value seen in the upper node
     * from values stored in the lower node. For example, if a mapping exists
     * for a field {@code id} to a mapper {@code mapper}, then:
     * <ul>
     * <li>Applying {@link Mapper#there mapper.there(entity)}
     *     would compute the value of {@code id} field as viewed in the upper
     *     entity from the values of the lower entity {@code entity}.</li>
     * <li>Applying {@link Mapper#back mapper.back(entity, value)}
     *     would set the value of all fields in the lower entity that
     *     the {@code id} field in the upper entity is composed of.</li>
     * </ul>
     * <p>
     *
     * @see Mapper
     * @see DirectMapper
     */
    public static final String STORE_MAPPERS = "partial-store-mappers";

    /**
     * Defines a {@code Map<ParameterizedEntityField, Set<ParameterizedEntityField>>}. This map maintains
     * a mapping of a <code>(field -> { depField1, depField2, ...})</code>
     * where {@code depFieldN} is seen from the upper node
     * and {@code field} is stored in the lower node; meaning of the map is that value
     * of each {@code depFieldN} field depends on the {@code field} field.
     * In other words, a change in {@code field} in the lower node changes value
     * of each {@code depFieldN} field in the upper node.
     * <p>
     * For example, if the following templates were active for fields: <code>attributes.original=original{id}</code>
     * and <code>clientId=client{id}</code>, this map would contain an entry <code>id={attributes.original,clientId}</code>.
     */
    public static final String FIELD_COMPONENTS_OF_MAPPERS = "mapper-field-components";

    /**
     * Defines a {@link Completeness} of the storage.
     */
    public static final String STORAGE_COMPLETENESS = "storage-completeness";
}
