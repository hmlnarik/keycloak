/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.CommonClientSessionModel;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientSessionCode<CLIENT_SESSION extends CommonClientSessionModel> {

    private static final String ACTIVE_CODE = "active_code";

    private static final Logger logger = Logger.getLogger(ClientSessionCode.class);

    private static final String NEXT_CODE = ClientSessionCode.class.getName() + ".nextCode";

    private KeycloakSession session;
    private final RealmModel realm;
    private final CLIENT_SESSION commonLoginSession;

    public enum ActionType {
        CLIENT,
        LOGIN,
        USER
    }

    public ClientSessionCode(KeycloakSession session, RealmModel realm, CLIENT_SESSION commonLoginSession) {
        this.session = session;
        this.realm = realm;
        this.commonLoginSession = commonLoginSession;
    }

    public static class ParseResult<CLIENT_SESSION extends CommonClientSessionModel> {
        ClientSessionCode<CLIENT_SESSION> code;
        boolean authSessionNotFound;
        boolean illegalHash;
        CLIENT_SESSION clientSession;

        public ClientSessionCode<CLIENT_SESSION> getCode() {
            return code;
        }

        public boolean isAuthSessionNotFound() {
            return authSessionNotFound;
        }

        public boolean isIllegalHash() {
            return illegalHash;
        }

        public CLIENT_SESSION getClientSession() {
            return clientSession;
        }
    }

    public static <CLIENT_SESSION extends CommonClientSessionModel> ParseResult<CLIENT_SESSION> parseResult(String code, KeycloakSession session, RealmModel realm, Class<CLIENT_SESSION> sessionClass) {
        ParseResult<CLIENT_SESSION> result = new ParseResult<>();
        if (code == null) {
            result.illegalHash = true;
            return result;
        }
        try {
            result.clientSession = getClientSession(code, session, realm, sessionClass);
            if (result.clientSession == null) {
                result.authSessionNotFound = true;
                return result;
            }

            if (!verifyCode(code, result.clientSession)) {
                result.illegalHash = true;
                return result;
            }

            result.code = new ClientSessionCode<CLIENT_SESSION>(session, realm, result.clientSession);
            return result;
        } catch (RuntimeException e) {
            result.illegalHash = true;
            return result;
        }
    }

    public static <CLIENT_SESSION extends CommonClientSessionModel> CLIENT_SESSION getClientSession(String code, KeycloakSession session, RealmModel realm, Class<CLIENT_SESSION> sessionClass) {
        CLIENT_SESSION clientSession = CodeGenerateUtil.parseSession(code, session, realm, sessionClass);

        // TODO:mposolda Move this to somewhere else? Maybe LoginActionsService.sessionCodeChecks should be somehow even for non-action URLs...
        if (clientSession != null) {
            session.getContext().setClient(clientSession.getClient());
        }

        return clientSession;
    }

    public CLIENT_SESSION getClientSession() {
        return commonLoginSession;
    }

    public boolean isValid(String requestedAction, ActionType actionType) {
        if (!isValidAction(requestedAction)) return false;
        return isActionActive(actionType);
    }

    public boolean isActionActive(ActionType actionType) {
        int timestamp = commonLoginSession.getTimestamp();

        int lifespan;
        switch (actionType) {
            case CLIENT:
                lifespan = realm.getAccessCodeLifespan();
                break;
            case LOGIN:
                lifespan = realm.getAccessCodeLifespanLogin() > 0 ? realm.getAccessCodeLifespanLogin() : realm.getAccessCodeLifespanUserAction();
                break;
            case USER:
                lifespan = realm.getAccessCodeLifespanUserAction();
                break;
            default:
                throw new IllegalArgumentException();
        }

        return timestamp + lifespan > Time.currentTime();
    }

    public boolean isValidAction(String requestedAction) {
        String action = commonLoginSession.getAction();
        if (action == null) {
            return false;
        }
        if (!action.equals(requestedAction)) {
            return false;
        }
        return true;
    }

    public void removeExpiredClientSession() {
        CodeGenerateUtil.removeExpiredSession(session, commonLoginSession);
    }


    public Set<RoleModel> getRequestedRoles() {
        Set<RoleModel> requestedRoles = new HashSet<>();
        for (String roleId : commonLoginSession.getRoles()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) {
                requestedRoles.add(role);
            }
        }
        return requestedRoles;
    }

    public Set<ProtocolMapperModel> getRequestedProtocolMappers() {
        return getRequestedProtocolMappers(commonLoginSession.getProtocolMappers(), commonLoginSession.getClient());
    }

    public static Set<ProtocolMapperModel> getRequestedProtocolMappers(Set<String> protocolMappers, ClientModel client) {
        Set<ProtocolMapperModel> requestedProtocolMappers = new HashSet<>();
        ClientTemplateModel template = client.getClientTemplate();
        if (protocolMappers != null) {
            for (String protocolMapperId : protocolMappers) {
                ProtocolMapperModel protocolMapper = client.getProtocolMapperById(protocolMapperId);
                if (protocolMapper == null && template != null) {
                    protocolMapper = template.getProtocolMapperById(protocolMapperId);
                }
                if (protocolMapper != null) {
                    requestedProtocolMappers.add(protocolMapper);
                }
            }
        }
        return requestedProtocolMappers;
    }

    public void setAction(String action) {
        commonLoginSession.setAction(action);
        commonLoginSession.setTimestamp(Time.currentTime());
    }

    public String getCode() {
        String nextCode = (String) session.getAttribute(NEXT_CODE + "." + commonLoginSession.getId());
        if (nextCode == null) {
            nextCode = generateCode(commonLoginSession);
            session.setAttribute(NEXT_CODE + "." + commonLoginSession.getId(), nextCode);
        } else {
            logger.debug("Code already generated for session, using code from session attributes");
        }
        return nextCode;
    }

    private static String generateCode(CommonClientSessionModel authSession) {
        try {
            String actionId = KeycloakModelUtils.generateSecret();

            String code = CodeGenerateUtil.generateCode(authSession, actionId);

            authSession.setNote(ACTIVE_CODE, code);

            return code;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyCode(String code, CommonClientSessionModel authSession) {
        try {
            String activeCode = authSession.getNote(ACTIVE_CODE);
            if (activeCode == null) {
                logger.debug("Active code not found in client session");
                return false;
            }

            authSession.removeNote(ACTIVE_CODE);

            return MessageDigest.isEqual(code.getBytes(), activeCode.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
