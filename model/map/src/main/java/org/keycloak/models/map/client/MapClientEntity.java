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
package org.keycloak.models.map.client;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.UpdatableEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public interface MapClientEntity extends AbstractEntity, UpdatableEntity {

    void addClientScope(String id, Boolean defaultScope);
    Stream<String> getClientScopes(boolean defaultScope);
    void removeClientScope(String id);

    ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model);
    ProtocolMapperModel getProtocolMapperById(String id);
    Collection<ProtocolMapperModel> getProtocolMappers();
    void removeProtocolMapper(String id);
    void updateProtocolMapper(String id, ProtocolMapperModel mapping);

    void addRedirectUri(String redirectUri);
    Set<String> getRedirectUris();
    void removeRedirectUri(String redirectUri);
    void setRedirectUris(Set<String> redirectUris);

    void addScopeMapping(String id);
    void deleteScopeMapping(String id);
    Collection<String> getScopeMappings();

    void addWebOrigin(String webOrigin);
    Set<String> getWebOrigins();
    void removeWebOrigin(String webOrigin);
    void setWebOrigins(Set<String> webOrigins);

    List<String> getAttribute(String name);
    Map<String, List<String>> getAttributes();
    void removeAttribute(String name);
    void setAttribute(String name, List<String> values);

    Map<String, String> getAuthFlowBindings();
    void setAuthFlowBindings(Map<String, String> authFlowBindings);

    String getAuthenticationFlowBindingOverride(String binding);
    Map<String, String> getAuthenticationFlowBindingOverrides();
    void removeAuthenticationFlowBindingOverride(String binding);
    void setAuthenticationFlowBindingOverride(String binding, String flowId);

    String getBaseUrl();

    String getClientAuthenticatorType();

    String getClientId();

    String getDescription();

    String getManagementUrl();

    String getName();

    Integer getNodeReRegistrationTimeout();

    Integer getNotBefore();

    String getProtocol();

    String getRealmId();

    String getRegistrationToken();

    String getRootUrl();

    Set<String> getScope();

    String getSecret();

    Boolean isAlwaysDisplayInConsole();

    Boolean isBearerOnly();

    Boolean isConsentRequired();

    Boolean isDirectAccessGrantsEnabled();

    Boolean isEnabled();

    Boolean isFrontchannelLogout();

    Boolean isFullScopeAllowed();

    Boolean isImplicitFlowEnabled();

    Boolean isPublicClient();

    Boolean isServiceAccountsEnabled();

    Boolean isStandardFlowEnabled();

    Boolean isSurrogateAuthRequired();

    void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole);

    void setBaseUrl(String baseUrl);

    void setBearerOnly(Boolean bearerOnly);

    void setClientAuthenticatorType(String clientAuthenticatorType);

    void setClientId(String clientId);

    void setConsentRequired(Boolean consentRequired);

    void setDescription(String description);

    void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled);

    void setEnabled(Boolean enabled);

    void setFrontchannelLogout(Boolean frontchannelLogout);

    void setFullScopeAllowed(Boolean fullScopeAllowed);

    void setImplicitFlowEnabled(Boolean implicitFlowEnabled);

    void setManagementUrl(String managementUrl);

    void setName(String name);

    void setNodeReRegistrationTimeout(Integer nodeReRegistrationTimeout);

    void setNotBefore(Integer notBefore);

    void setProtocol(String protocol);

    void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers);

    void setPublicClient(Boolean publicClient);

    void setRegistrationToken(String registrationToken);

    void setRootUrl(String rootUrl);

    void setScope(Set<String> scope);

    void setSecret(String secret);

    void setServiceAccountsEnabled(Boolean serviceAccountsEnabled);

    void setStandardFlowEnabled(Boolean standardFlowEnabled);

    void setSurrogateAuthRequired(Boolean surrogateAuthRequired);

}
