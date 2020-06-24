/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.models.map.client;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import com.google.common.base.Functions;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public abstract class MapClientAdapter implements ClientModel {

    private final KeycloakSession session;
    private final RealmModel realm;
    protected final MapClientEntity entity;

    public MapClientAdapter(KeycloakSession session, RealmModel realm, MapClientEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(realm, "realm");
        this.session = session;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        final String id = clientScope == null ? null : clientScope.getId();
        if (id != null) {
            entity.addClientScope(UUID.fromString(id), defaultScope);
        }
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        for (ClientScopeModel cs : clientScopes) {
            addClientScope(cs, defaultScope);
        }
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        final String id = clientScope == null ? null : clientScope.getId();
        if (id != null) {
            entity.removeClientScope(UUID.fromString(id));
        }
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope, boolean filterByProtocol) {
        Stream<ClientScopeModel> res = this.entity.getClientScopes(defaultScope)
          .map(Object::toString)
          .map(realm::getClientScopeById)
          .filter(Objects::nonNull);

        if (filterByProtocol) {
            String clientProtocol = getProtocol() == null ? OIDCLoginProtocol.LOGIN_PROTOCOL : getProtocol();
            res = res.filter(cs -> Objects.equals(cs.getProtocol(), clientProtocol));
        }

        return res.collect(Collectors.toMap(ClientScopeModel::getName, Functions.identity()));
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return getScopeMappingsStream().collect(Collectors.toSet());
    }

    public Stream<RoleModel> getScopeMappingsStream() {
        return this.entity.getScopeMappings().stream()
                .map(UUID::toString)
                .map(realm::getRoleById)
                .filter(Objects::nonNull);
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.addScopeMapping(UUID.fromString(id));
        }
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.deleteScopeMapping(UUID.fromString(id));
        }
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        String realmId = realm.getId();
        return getScopeMappingsStream()
          .filter(rm -> Objects.equals(rm.getContainerId(), realmId))
          .collect(Collectors.toSet());
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;

        final String id = role == null ? null : role.getId();
        if (id != null && this.entity.getScopeMappings().contains(UUID.fromString(id))) {
            return true;
        }

        if (getScopeMappingsStream().anyMatch(r -> r.hasRole(role))) {
            return true;
        }

        Set<RoleModel> roles = getRoles();
        if (roles.contains(role)) return true;

        return roles.stream().anyMatch(r -> r.hasRole(role));
    }

    @Override
    public RoleModel getRole(String name) {
        return session.realms().getClientRole(realm, this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.realms().addClientRole(realm, this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.realms().addClientRole(realm, this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.realms().removeRole(realm, role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        return session.realms().getClientRoles(realm, this);
    }

    @Override
    public Set<RoleModel> getRoles(Integer firstResult, Integer maxResults) {
        return session.realms().getClientRoles(realm, this, firstResult, maxResults);
    }

    @Override
    public Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return session.realms().searchForClientRoles(realm, this, search, first, max);
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }
        this.entity.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String... defaultRoles) {
        List<String> defaultRolesArray = Arrays.asList(defaultRoles);
        Collection<String> entities = entity.getDefaultRoles();
        Set<String> already = new HashSet<String>();
        ArrayList<String> remove = new ArrayList<>();
        for (String rel : entities) {
            if (! defaultRolesArray.contains(rel)) {
                remove.add(rel);
            } else {
                already.add(rel);
            }
        }
        entity.removeDefaultRoles(remove.toArray(new String[] {}));

        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(roleName);
            }
        }
    }

    @Override
    public void removeDefaultRoles(String... defaultRoles) {
        this.entity.removeDefaultRoles(defaultRoles);
    }

    @Override
    public String getId() {
        return entity.getId().toString();
    }

    @Override
    public String getClientId() {
        return entity.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        entity.setClientId(clientId);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return entity.isAlwaysDisplayInConsole();
    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        entity.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return entity.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        entity.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public Set<String> getWebOrigins() {
        return entity.getWebOrigins();
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        entity.addWebOrigin(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        entity.removeWebOrigin(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        return entity.getRedirectUris();
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        entity.addRedirectUri(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        entity.removeRedirectUri(redirectUri);
    }

    @Override
    public String getManagementUrl() {
        return entity.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        entity.setManagementUrl(url);
    }

    @Override
    public String getRootUrl() {
        return entity.getRootUrl();
    }

    @Override
    public void setRootUrl(String url) {
        entity.setRootUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return entity.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        entity.setBaseUrl(url);
    }

    @Override
    public boolean isBearerOnly() {
        return entity.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        entity.setBearerOnly(only);
    }

    @Override
    public String getClientAuthenticatorType() {
        return entity.getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        entity.setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public boolean validateSecret(String secret) {
        return MessageDigest.isEqual(secret.getBytes(), entity.getSecret().getBytes());
    }

    @Override
    public String getSecret() {
        return entity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        entity.setSecret(secret);
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return entity.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        entity.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public String getRegistrationToken() {
        return entity.getRegistrationToken();
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        entity.setRegistrationToken(registrationToken);
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        entity.setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return entity.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return entity.getAttributes();
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return entity.getAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return entity.getAuthenticationFlowBindingOverrides();
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        entity.removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        entity.setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public boolean isFrontchannelLogout() {
        return entity.isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        entity.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        return entity.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        entity.setFullScopeAllowed(value);
    }

    @Override
    public boolean isPublicClient() {
        return entity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        entity.setPublicClient(flag);
    }

    @Override
    public boolean isConsentRequired() {
        return entity.isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        entity.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return entity.isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        entity.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return entity.isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        entity.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return entity.isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        entity.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return entity.isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        entity.setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return entity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return entity.getProtocolMappers();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        ProtocolMapperModel pm = new ProtocolMapperModel();
        pm.setId(KeycloakModelUtils.generateId());
        pm.setName(model.getName());
        pm.setProtocol(model.getProtocol());
        pm.setProtocolMapper(model.getProtocolMapper());
        if (model.getConfig() != null) {
            pm.setConfig(new HashMap<>(model.getConfig()));
        } else {
            pm.setConfig(new HashMap<>());
        }
        return entity.addProtocolMapper(pm);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        entity.removeProtocolMapper(mapping);
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        entity.updateProtocolMapper(mapping);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return entity.getProtocolMapperById(id);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return entity.getProtocolMapperByName(protocol, name);
    }

    @Override
    public List<String> getDefaultRoles() {
        return entity.getDefaultRoles();
    }

    /* ******** equals and hashMap could be extracted to common ancestor or at least some auxiliary class */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
