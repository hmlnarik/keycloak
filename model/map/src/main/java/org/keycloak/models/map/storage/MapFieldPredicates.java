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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.storage.SearchableModelField;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.models.map.storage.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.UserConsentEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.storage.StorageId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author hmlnarik
 */
public class MapFieldPredicates {

    public static final Map<SearchableModelField<ClientModel>, UpdatePredicatesFunc<Object, MapClientEntity<Object>, ClientModel>> CLIENT_PREDICATES = basePredicates(ClientModel.SearchableFields.ID);
    public static final Map<SearchableModelField<ClientScopeModel>, UpdatePredicatesFunc<Object, MapClientScopeEntity<Object>, ClientScopeModel>> CLIENT_SCOPE_PREDICATES = basePredicates(ClientScopeModel.SearchableFields.ID);
    public static final Map<SearchableModelField<GroupModel>, UpdatePredicatesFunc<Object, MapGroupEntity<Object>, GroupModel>> GROUP_PREDICATES = basePredicates(GroupModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RoleModel>, UpdatePredicatesFunc<Object, MapRoleEntity<Object>, RoleModel>> ROLE_PREDICATES = basePredicates(RoleModel.SearchableFields.ID);
    public static final Map<SearchableModelField<UserModel>, UpdatePredicatesFunc<Object, MapUserEntity<Object>, UserModel>> USER_PREDICATES = basePredicates(UserModel.SearchableFields.ID);
    public static final Map<SearchableModelField<RootAuthenticationSessionModel>, UpdatePredicatesFunc<Object, MapRootAuthenticationSessionEntity<Object>, RootAuthenticationSessionModel>> AUTHENTICATION_SESSION_PREDICATES = basePredicates(RootAuthenticationSessionModel.SearchableFields.ID);

    @SuppressWarnings("unchecked")
    private static final Map<Class<?>, Map> PREDICATES = new HashMap<>();

    static {
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.REALM_ID,             MapClientEntity::getRealmId);
        put(CLIENT_PREDICATES, ClientModel.SearchableFields.CLIENT_ID,            MapClientEntity::getClientId);

        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.REALM_ID,  MapClientScopeEntity::getRealmId);
        put(CLIENT_SCOPE_PREDICATES, ClientScopeModel.SearchableFields.NAME,      MapClientScopeEntity::getName);

        put(GROUP_PREDICATES, GroupModel.SearchableFields.REALM_ID,               MapGroupEntity::getRealmId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.NAME,                   MapGroupEntity::getName);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.PARENT_ID,              MapGroupEntity::getParentId);
        put(GROUP_PREDICATES, GroupModel.SearchableFields.ASSIGNED_ROLE,          MapFieldPredicates::checkGrantedGroupRole);

        put(ROLE_PREDICATES, RoleModel.SearchableFields.REALM_ID,                 MapRoleEntity::getRealmId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.CLIENT_ID,                MapRoleEntity::getClientId);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.DESCRIPTION,              MapRoleEntity::getDescription);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.NAME,                     MapRoleEntity::getName);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.IS_CLIENT_ROLE,           MapRoleEntity::isClientRole);
        put(ROLE_PREDICATES, RoleModel.SearchableFields.IS_COMPOSITE_ROLE,        MapRoleEntity::isComposite);

        put(USER_PREDICATES, UserModel.SearchableFields.REALM_ID,                 MapUserEntity::getRealmId);
        put(USER_PREDICATES, UserModel.SearchableFields.USERNAME,                 MapUserEntity::getUsername);
        put(USER_PREDICATES, UserModel.SearchableFields.FIRST_NAME,               MapUserEntity::getFirstName);
        put(USER_PREDICATES, UserModel.SearchableFields.LAST_NAME,                MapUserEntity::getLastName);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL,                    MapUserEntity::getEmail);
        put(USER_PREDICATES, UserModel.SearchableFields.ENABLED,                  MapUserEntity::isEnabled);
        put(USER_PREDICATES, UserModel.SearchableFields.EMAIL_VERIFIED,           MapUserEntity::isEmailVerified);
        put(USER_PREDICATES, UserModel.SearchableFields.FEDERATION_LINK,          MapUserEntity::getFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.ATTRIBUTE,                MapFieldPredicates::checkUserAttributes);
        put(USER_PREDICATES, UserModel.SearchableFields.IDP_AND_USER,             MapFieldPredicates::getUserIdpAliasAtIdentityProviderPredicate);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_ROLE,            MapFieldPredicates::checkGrantedUserRole);
        put(USER_PREDICATES, UserModel.SearchableFields.ASSIGNED_GROUP,           MapFieldPredicates::checkUserGroup);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_FOR_CLIENT,       MapFieldPredicates::checkUserClientConsent);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_WITH_CLIENT_SCOPE, MapFieldPredicates::checkUserConsentsWithClientScope);
        put(USER_PREDICATES, UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, MapFieldPredicates::getUserConsentClientFederationLink);
        put(USER_PREDICATES, UserModel.SearchableFields.SERVICE_ACCOUNT_CLIENT,   MapUserEntity::getServiceAccountClientLink);

        put(AUTHENTICATION_SESSION_PREDICATES, RootAuthenticationSessionModel.SearchableFields.REALM_ID,    MapRootAuthenticationSessionEntity::getRealmId);
        put(AUTHENTICATION_SESSION_PREDICATES, RootAuthenticationSessionModel.SearchableFields.TIMESTAMP,   MapRootAuthenticationSessionEntity::getTimestamp);
    }

    static {
        PREDICATES.put(ClientModel.class,                       CLIENT_PREDICATES);
        PREDICATES.put(ClientScopeModel.class,                  CLIENT_SCOPE_PREDICATES);
        PREDICATES.put(RoleModel.class,                         ROLE_PREDICATES);
        PREDICATES.put(GroupModel.class,                        GROUP_PREDICATES);
        PREDICATES.put(UserModel.class,                         USER_PREDICATES);
        PREDICATES.put(RootAuthenticationSessionModel.class,    AUTHENTICATION_SESSION_PREDICATES);
    }

    private static <K, V extends AbstractEntity<K>, M> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, Function<V, Object> extractor) {
        map.put(field, (mcb, op, values) -> mcb.fieldCompare(op, extractor, values));
    }

    private static <K, V extends AbstractEntity<K>, M> void put(
      Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> map,
      SearchableModelField<M> field, UpdatePredicatesFunc<K, V, M> function) {
        map.put(field, function);
    }

    private static String ensureEqSingleValue(SearchableModelField<?> field, String parameterName, Operator op, Object[] values) throws CriterionNotSupportedException {
        if (op != Operator.EQ) {
            throw new CriterionNotSupportedException(field, op);
        }
        if (values == null || values.length != 1) {
            throw new CriterionNotSupportedException(field, op, "Invalid arguments, expected (" + parameterName + "), got: " + Arrays.toString(values));
        }

        final Object ob = values[0];
        if (! (ob instanceof String)) {
            throw new CriterionNotSupportedException(field, op, "Invalid arguments, expected (String role_id), got: " + Arrays.toString(values));
        }
        String s = (String) ob;
        return s;
    }

    private static MapModelCriteriaBuilder<Object, MapGroupEntity<Object>, GroupModel> checkGrantedGroupRole(MapModelCriteriaBuilder<Object, MapGroupEntity<Object>, GroupModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(GroupModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<MapGroupEntity<Object>, ?> getter;
        getter = ge -> ge.getGrantedRoles().contains(roleIdS);
        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> getUserConsentClientFederationLink(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String providerId = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_CLIENT_FEDERATION_LINK, "provider_id", op, values);
        String providerIdS = new StorageId((String) providerId, "").getId();
        Function<MapUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsents().map(UserConsentEntity::getClientId).anyMatch(v -> v != null && v.startsWith(providerIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> checkUserAttributes(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        if (values == null || values.length <= 1) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (attribute_name, ...), got: " + Arrays.toString(values));
        }

        final Object attrName = values[0];
        if (! (attrName instanceof String)) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.ATTRIBUTE, op, "Invalid arguments, expected (String attribute_name), got: " + Arrays.toString(values));
        }
        String attrNameS = (String) attrName;
        Function<MapUserEntity<Object>, ?> getter;
        Object[] realValues = new Object[values.length - 1];
        System.arraycopy(values, 1, realValues, 0, values.length - 1);
        Predicate<Object> valueComparator = CriteriaOperator.predicateFor(op, realValues);
        getter = ue -> {
            final List<String> attrs = ue.getAttribute(attrNameS);
            return attrs != null && attrs.stream().anyMatch(valueComparator);
        };

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> checkGrantedUserRole(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String roleIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_ROLE, "role_id", op, values);
        Function<MapUserEntity<Object>, ?> getter;
        getter = ue -> ue.getRolesMembership().contains(roleIdS);

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> checkUserGroup(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        Function<MapUserEntity<Object>, ?> getter;
        if (op == Operator.IN && values != null && values.length == 1 && (values[0] instanceof Collection)) {
            Collection<?> c = (Collection<?>) values[0];
            getter = ue -> ue.getGroupsMembership().stream().anyMatch(c::contains);
        } else {
            String groupIdS = ensureEqSingleValue(UserModel.SearchableFields.ASSIGNED_GROUP, "group_id", op, values);
            getter = ue -> ue.getGroupsMembership().contains(groupIdS);
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> checkUserClientConsent(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String clientIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_id", op, values);
        Function<MapUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsent(clientIdS);

        return mcb.fieldCompare(Operator.EXISTS, getter, null);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> checkUserConsentsWithClientScope(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        String clientScopeIdS = ensureEqSingleValue(UserModel.SearchableFields.CONSENT_FOR_CLIENT, "client_scope_id", op, values);
        Function<MapUserEntity<Object>, ?> getter;
        getter = ue -> ue.getUserConsents().anyMatch(consent -> consent.getGrantedClientScopesIds().contains(clientScopeIdS));

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    private static MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> getUserIdpAliasAtIdentityProviderPredicate(MapModelCriteriaBuilder<Object, MapUserEntity<Object>, UserModel> mcb, Operator op, Object[] values) {
        if (op != Operator.EQ) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op);
        }
        if (values == null || values.length == 0 || values.length > 2) {
            throw new CriterionNotSupportedException(UserModel.SearchableFields.IDP_AND_USER, op, "Invalid arguments, expected (idp_alias) or (idp_alias, idp_user), got: " + Arrays.toString(values));
        }

        final Object idpAlias = values[0];
        Function<MapUserEntity<Object>, ?> getter;
        if (values.length == 1) {
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()));
        } else if (idpAlias == null) {
            final Object idpUserId = values[1];
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpUserId, aue.getUserId()));
        } else {
            final Object idpUserId = values[1];
            getter = ue -> ue.getFederatedIdentities()
              .anyMatch(aue -> Objects.equals(idpAlias, aue.getIdentityProvider()) && Objects.equals(idpUserId, aue.getUserId()));
        }

        return mcb.fieldCompare(Boolean.TRUE::equals, getter);
    }

    protected static <K, V extends AbstractEntity<K>, M> Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> basePredicates(SearchableModelField<M> idField) {
        Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates = new HashMap<>();
        fieldPredicates.put(idField, (o, op, values) -> o.idCompare(op, values));
        return fieldPredicates;
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends AbstractEntity<K>, M> Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> getPredicates(Class<M> clazz) {
        return PREDICATES.get(clazz);
    }
}
