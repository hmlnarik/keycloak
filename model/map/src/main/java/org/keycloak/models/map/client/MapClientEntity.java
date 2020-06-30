package org.keycloak.models.map.client;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.map.common.HasUpdated;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class MapClientEntity extends HasUpdated<UUID> {

    // If clone() was improved, the id and realmId could be final
    private UUID id;
    private String realmId;

    private String clientId;
    private String name;
    private String description;
    private Set<String> redirectUris = new HashSet<>();
    private boolean enabled;
    private boolean alwaysDisplayInConsole;
    private String clientAuthenticatorType;
    private String secret;
    private String registrationToken;
    private String protocol;
    private Map<String, String> attributes = new HashMap<>();
    private Map<String, String> authFlowBindings = new HashMap<>();
    private boolean publicClient;
    private boolean fullScopeAllowed;
    private boolean frontchannelLogout;
    private int notBefore;
    private Set<String> scope = new HashSet<>();
    private Set<String> webOrigins = new HashSet<>();
    private Map<UUID, ProtocolMapperModel> protocolMappers = new HashMap<>();
    private Map<UUID, Boolean> clientScopes = new HashMap<>();
    private Set<UUID> scopeMappings = new LinkedHashSet<>();
    private List<String> defaultRoles = new LinkedList<>();
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String rootUrl;
    private String baseUrl;
    private boolean bearerOnly;
    private boolean consentRequired;
    private boolean standardFlowEnabled;
    private boolean implicitFlowEnabled;
    private boolean directAccessGrantsEnabled;
    private boolean serviceAccountsEnabled;
    private int nodeReRegistrationTimeout;

    public MapClientEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapClientEntity(UUID id, String realmId) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.realmId = realmId;
    }

    public UUID getId() {
        return this.id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.updated |= ! Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated |= ! Objects.equals(this.description, description);
        this.description = description;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.updated |= ! Objects.equals(this.redirectUris, redirectUris);
        this.redirectUris = redirectUris;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.updated |= ! Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        this.updated |= ! Objects.equals(this.alwaysDisplayInConsole, alwaysDisplayInConsole);
        this.alwaysDisplayInConsole = alwaysDisplayInConsole;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.updated |= ! Objects.equals(this.clientAuthenticatorType, clientAuthenticatorType);
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.updated |= ! Objects.equals(this.secret, secret);
        this.secret = secret;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.updated |= ! Objects.equals(this.registrationToken, registrationToken);
        this.registrationToken = registrationToken;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.updated |= ! Objects.equals(this.protocol, protocol);
        this.protocol = protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.updated |= ! Objects.equals(this.attributes, attributes);
        this.attributes = attributes;
    }

    public Map<String, String> getAuthFlowBindings() {
        return authFlowBindings;
    }

    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        this.updated |= ! Objects.equals(this.authFlowBindings, authFlowBindings);
        this.authFlowBindings = authFlowBindings;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.updated |= ! Objects.equals(this.publicClient, publicClient);
        this.publicClient = publicClient;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.updated |= ! Objects.equals(this.fullScopeAllowed, fullScopeAllowed);
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.updated |= ! Objects.equals(this.frontchannelLogout, frontchannelLogout);
        this.frontchannelLogout = frontchannelLogout;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.updated |= ! Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.updated |= ! Objects.equals(this.scope, scope);
        this.scope.clear();
        this.scope.addAll(scope);
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.updated |= ! Objects.equals(this.webOrigins, webOrigins);
        this.webOrigins.clear();
        this.webOrigins.addAll(webOrigins);
    }

    public Set<ProtocolMapperModel> getProtocolMappers() {
        return new HashSet<>(protocolMappers.values());
    }

    public void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers) {
        this.updated |= ! Objects.equals(this.protocolMappers, protocolMappers);
        this.protocolMappers.clear();
        this.protocolMappers.putAll(protocolMappers.stream().collect(Collectors.toMap(m -> UUID.fromString(m.getId()), Function.identity())));
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.updated |= ! Objects.equals(this.surrogateAuthRequired, surrogateAuthRequired);
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.updated |= ! Objects.equals(this.managementUrl, managementUrl);
        this.managementUrl = managementUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.updated |= ! Objects.equals(this.rootUrl, rootUrl);
        this.rootUrl = rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.updated |= ! Objects.equals(this.baseUrl, baseUrl);
        this.baseUrl = baseUrl;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<String> defaultRoles) {
        this.updated |= ! Objects.equals(this.defaultRoles, defaultRoles);
        this.defaultRoles.clear();
        this.defaultRoles.addAll(defaultRoles);
    }

    public void addDefaultRole(String name) {
        updated = true;
        if (name != null) {
            defaultRoles.add(name);
        }
    }

    public void removeDefaultRoles(String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            updated |= this.defaultRoles.remove(defaultRole);
        }
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.updated |= ! Objects.equals(this.bearerOnly, bearerOnly);
        this.bearerOnly = bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.updated |= ! Objects.equals(this.consentRequired, consentRequired);
        this.consentRequired = consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        this.updated |= ! Objects.equals(this.standardFlowEnabled, standardFlowEnabled);
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        this.updated |= ! Objects.equals(this.implicitFlowEnabled, implicitFlowEnabled);
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        this.updated |= ! Objects.equals(this.directAccessGrantsEnabled, directAccessGrantsEnabled);
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.updated |= ! Objects.equals(this.serviceAccountsEnabled, serviceAccountsEnabled);
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.updated |= ! Objects.equals(this.nodeReRegistrationTimeout, nodeReRegistrationTimeout);
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public void addWebOrigin(String webOrigin) {
        updated = true;
        this.webOrigins.add(webOrigin);
    }

    public void removeWebOrigin(String webOrigin) {
        updated |= this.webOrigins.remove(webOrigin);
    }

    public void addRedirectUri(String redirectUri) {
        this.updated |= ! Objects.equals(this.nodeReRegistrationTimeout, nodeReRegistrationTimeout);
        this.redirectUris.add(redirectUri);
    }

    public void removeRedirectUri(String redirectUri) {
        updated |= this.redirectUris.remove(redirectUri);
    }

    public void setAttribute(String name, String value) {
        this.updated = true;
        this.attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        this.updated = true;
        this.attributes.remove(name);
    }

    public String getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String getAuthenticationFlowBindingOverride(String binding) {
        return this.authFlowBindings.get(binding);
    }

    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return this.authFlowBindings;
    }

    public void removeAuthenticationFlowBindingOverride(String binding) {
        updated |= this.authFlowBindings.remove(binding) != null;
    }

    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        this.updated |= ! Objects.equals(this.nodeReRegistrationTimeout, nodeReRegistrationTimeout);
        this.authFlowBindings.put(binding, flowId);
    }

    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (model == null) {
            return null;
        }
        updated = true;
        if (model.getId() == null) {
            model.setId(UUID.randomUUID().toString());
        }
        this.protocolMappers.put(UUID.fromString(model.getId()), model);
        return model;
    }

    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            updated |= protocolMappers.remove(UUID.fromString(id)) != null;
        }
    }

    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            updated = true;
            protocolMappers.put(UUID.fromString(id), mapping);
        }
    }

    public ProtocolMapperModel getProtocolMapperById(String id) {
        return id == null ? null : protocolMappers.get(UUID.fromString(id));
    }

    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return protocolMappers.values().stream()
          .filter(pm -> Objects.equals(pm.getProtocol(), protocol) && Objects.equals(pm.getName(), name))
          .findAny()
          .orElse(null);
    }

    public void addClientScope(UUID id, boolean defaultScope) {
        if (id != null) {
            updated = true;
            this.clientScopes.put(id, defaultScope);
        }
    }

    public void removeClientScope(UUID id) {
        if (id != null) {
            updated |= clientScopes.remove(id);
        }
    }

    public Stream<UUID> getClientScopes(boolean defaultScope) {
        return this.clientScopes.entrySet().stream()
          .filter(me -> Objects.equals(me.getValue(), defaultScope))
          .map(Entry::getKey);
    }

    public Collection<UUID> getScopeMappings() {
        return scopeMappings;
    }

    public void addScopeMapping(UUID id) {
        if (id != null) {
            updated = true;
            scopeMappings.add(id);
        }
    }

    public void deleteScopeMapping(UUID id) {
        if (id != null) {
            updated |= scopeMappings.remove(id);
        }
    }

    public String getRealmId() {
        return this.realmId;
    }
}
