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
package org.keycloak.models.map.storage;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntityFields;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntityFields;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntityFields;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntityFields;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntityFields;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntityFields;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.events.MapAdminEventEntity;
import org.keycloak.models.map.events.MapAuthEventEntity;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntityFields;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.role.MapRoleEntityFields;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserEntityFields;
import org.keycloak.models.map.userSession.MapAuthenticatedClientSessionEntity;
import org.keycloak.models.map.userSession.MapUserSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.storage.SearchableModelField;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class covering various aspects of relationship between model and entity classes.
 * @author hmlnarik
 */
public class ModelEntityUtil {

    private static final Map<Class<?>, String> MODEL_TO_NAME = new IdentityHashMap<>();
    static {
        MODEL_TO_NAME.put(SingleUseObjectValueModel.class, "single-use-objects");
        MODEL_TO_NAME.put(ClientScopeModel.class, "client-scopes");
        MODEL_TO_NAME.put(ClientModel.class, "clients");
        MODEL_TO_NAME.put(GroupModel.class, "groups");
        MODEL_TO_NAME.put(RealmModel.class, "realms");
        MODEL_TO_NAME.put(RoleModel.class, "roles");
        MODEL_TO_NAME.put(RootAuthenticationSessionModel.class, "auth-sessions");
        MODEL_TO_NAME.put(UserLoginFailureModel.class, "user-login-failures");
        MODEL_TO_NAME.put(UserModel.class, "users");
        MODEL_TO_NAME.put(UserSessionModel.class, "user-sessions");

        // authz
        MODEL_TO_NAME.put(PermissionTicket.class, "authz-permission-tickets");
        MODEL_TO_NAME.put(Policy.class, "authz-policies");
        MODEL_TO_NAME.put(ResourceServer.class, "authz-resource-servers");
        MODEL_TO_NAME.put(Resource.class, "authz-resources");
        MODEL_TO_NAME.put(org.keycloak.authorization.model.Scope.class, "authz-scopes");

        // events
        MODEL_TO_NAME.put(AdminEvent.class, "admin-events");
        MODEL_TO_NAME.put(Event.class, "auth-events");
    }
    private static final Map<String, Class<?>> NAME_TO_MODEL = MODEL_TO_NAME.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    private static final Map<Class<?>, Class<? extends AbstractEntity>> MODEL_TO_ENTITY_TYPE = new IdentityHashMap<>();
    static {
        MODEL_TO_ENTITY_TYPE.put(SingleUseObjectValueModel.class, MapSingleUseObjectEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ClientScopeModel.class, MapClientScopeEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ClientModel.class, MapClientEntity.class);
        MODEL_TO_ENTITY_TYPE.put(GroupModel.class, MapGroupEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RealmModel.class, MapRealmEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RoleModel.class, MapRoleEntity.class);
        MODEL_TO_ENTITY_TYPE.put(RootAuthenticationSessionModel.class, MapRootAuthenticationSessionEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserLoginFailureModel.class, MapUserLoginFailureEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserModel.class, MapUserEntity.class);
        MODEL_TO_ENTITY_TYPE.put(UserSessionModel.class, MapUserSessionEntity.class);
        MODEL_TO_ENTITY_TYPE.put(AuthenticatedClientSessionModel.class, MapAuthenticatedClientSessionEntity.class);

        // authz
        MODEL_TO_ENTITY_TYPE.put(PermissionTicket.class, MapPermissionTicketEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Policy.class, MapPolicyEntity.class);
        MODEL_TO_ENTITY_TYPE.put(ResourceServer.class, MapResourceServerEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Resource.class, MapResourceEntity.class);
        MODEL_TO_ENTITY_TYPE.put(org.keycloak.authorization.model.Scope.class, MapScopeEntity.class);

        // events
        MODEL_TO_ENTITY_TYPE.put(AdminEvent.class, MapAdminEventEntity.class);
        MODEL_TO_ENTITY_TYPE.put(Event.class, MapAuthEventEntity.class);
    }
    private static final Map<Class<?>, Class<?>> ENTITY_TO_MODEL_TYPE = MODEL_TO_ENTITY_TYPE.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    private static final Map<Class<? extends AbstractEntity>, Class<? extends Enum<?>>> ENTITY_TO_FIELD_TYPE = new IdentityHashMap<>();
    static {
//        ENTITY_TO_FIELD_TYPE.put(MapAuthenticatedClientSessionEntity.class, MapAuthenticatedClientSessionEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapClientScopeEntity.class, MapClientScopeEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapClientEntity.class, MapClientEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapGroupEntity.class, MapGroupEntityFields.class);
//        ENTITY_TO_FIELD_TYPE.put(MapRealmEntity.class, MapRealmEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapRoleEntity.class, MapRoleEntityFields.class);
//        ENTITY_TO_FIELD_TYPE.put(MapRootAuthenticationSessionEntity.class, MapRootAuthenticationSessionEntityFields.class);
//        ENTITY_TO_FIELD_TYPE.put(MapUserLoginFailureEntity.class, MapUserLoginFailureEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapUserEntity.class, MapUserEntityFields.class);
//        ENTITY_TO_FIELD_TYPE.put(MapUserSessionEntity.class, MapUserSessionEntityFields.class);

        // authz
        ENTITY_TO_FIELD_TYPE.put(MapPermissionTicketEntity.class, MapPermissionTicketEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapPolicyEntity.class, MapPolicyEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapResourceServerEntity.class, MapResourceServerEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapResourceEntity.class, MapResourceEntityFields.class);
        ENTITY_TO_FIELD_TYPE.put(MapScopeEntity.class, MapScopeEntityFields.class);
    }
    private static final Map<Class<?>, EntityField<?>> ENTITY_TO_ID_FIELD = ENTITY_TO_FIELD_TYPE.entrySet().stream().collect(Collectors.toMap(Entry::getKey, me -> (EntityField<?>) me.getValue().getEnumConstants()[0]));

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<V> getEntityType(Class<M> modelClass) {
        return (Class<V>) MODEL_TO_ENTITY_TYPE.get(modelClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<V> getEntityType(Class<M> modelClass, Class<? extends AbstractEntity> defaultClass) {
        return (Class<V>) MODEL_TO_ENTITY_TYPE.getOrDefault(modelClass, defaultClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<M> getModelType(Class<V> entityClass) {
        return (Class<M>) ENTITY_TO_MODEL_TYPE.get(entityClass);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Class<M> getModelType(Class<V> entityClass, Class<M> defaultClass) {
        return (Class<M>) ENTITY_TO_MODEL_TYPE.getOrDefault(entityClass, defaultClass);
    }

    public static String getModelName(Class<?> key, String defaultValue) {
        return MODEL_TO_NAME.getOrDefault(key, defaultValue);
    }

    public static String getModelName(Class<?> key) {
        return MODEL_TO_NAME.get(key);
    }

    public static Set<String> getModelNames() {
        return NAME_TO_MODEL.keySet();
    }

    @SuppressWarnings("unchecked")
    public static <M> Class<M> getModelClass(String key) {
        return (Class<M>) NAME_TO_MODEL.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity> Optional<EntityField<V>> getEntityField(Class<V> entityClass, String fieldNameCamelCase) {
        // TODO: Optimize lookup
        Class<? extends Enum<?>> entityFieldClass = ENTITY_TO_FIELD_TYPE.get(entityClass);
        if (entityFieldClass == null) {
            return Optional.empty();
        }
        Object[] values = entityFieldClass.getEnumConstants();
        if (values == null) {
            return Optional.empty();
        }
        return Arrays.asList(values).stream()
          .filter(EntityField.class::isInstance)
          .map(EntityField.class::cast)
          .filter(ef -> fieldNameCamelCase.equals(ef.getNameCamelCase()))
          .map(ef -> (EntityField<V>) ef)
          .findAny();
    }

    /**
     * Returns a parameterized entity field for a string that is either a camel-case field name, or a
     * camel-case field name followed by a dot followed by a string parameter.
     * @param <V>
     * @param entityClass
     * @param fieldNameCamelCaseWithParameter
     * @return
     */
    public static <V extends AbstractEntity> Optional<ParameterizedEntityField<V>> getParameterizedEntityField(Class<V> entityClass, String fieldNameCamelCaseWithParameter) {
        int i = fieldNameCamelCaseWithParameter.indexOf('.');
        final String fieldNameCamelCase = i == -1 ? fieldNameCamelCaseWithParameter : fieldNameCamelCaseWithParameter.substring(0, i);
        final String parameter = i == -1 ? null : fieldNameCamelCaseWithParameter.substring(i + 1);
        return getEntityField(entityClass, fieldNameCamelCase)
            .map(field -> ParameterizedEntityField.from(field, parameter));
    }

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity> EntityField<V> getIdField(Class<V> targetEntityClass) {
        return (EntityField<V>) ENTITY_TO_ID_FIELD.get(targetEntityClass);
    }

    @SuppressWarnings("unchecked")
    public static <M> SearchableModelField<M> getSearchableIdField(Class<M> targetModelClass) {
        return (SearchableModelField<M>) MapFieldPredicates.SEARCHABLE_FIELD_IDS.get(targetModelClass);
    }

    @SuppressWarnings("unchecked")
    public static <M> SearchableModelField<M> getSearchableRealmIdField(Class<M> targetModelClass) {
        return (SearchableModelField<M>) MapFieldPredicates.SEARCHABLE_FIELD_REALM_IDS.get(targetModelClass);
    }

    private static final String ATTRIBUTE_FIELD_NAME = MapClientEntityFields.ATTRIBUTES.getName();  // Any ATTRIBUTES field will do

    @SuppressWarnings("unchecked")
    public static <V extends AbstractEntity, M> Optional<ParameterizedEntityField<V>> fromSearchableField(SearchableModelField<M> searchableField, Object[] params) {
        return Optional
          .ofNullable((ParameterizedEntityField<V>) MapFieldPredicates.MODEL_TO_ENTITY_FIELD.get(searchableField))
          .map(f -> ATTRIBUTE_FIELD_NAME.equals(f.getName()) && params != null && params.length > 0 ? ParameterizedEntityField.from(f, params[0]) : f);
    }

}
