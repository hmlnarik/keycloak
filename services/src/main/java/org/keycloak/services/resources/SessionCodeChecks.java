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

package org.keycloak.services.resources;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.BrowserHistoryHelper;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.sessions.AuthenticationSessionModel;


public class SessionCodeChecks {

    private static final Logger logger = Logger.getLogger(SessionCodeChecks.class);

    private AuthenticationSessionModel authSession;
    private ClientSessionCode<AuthenticationSessionModel> clientCode;
    private Response response;
    private boolean actionRequest;

    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final ClientConnection clientConnection;
    private final KeycloakSession session;
    private final EventBuilder event;

    private final String code;
    private final String execution;
    private final String flowPath;


    public SessionCodeChecks(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, KeycloakSession session, EventBuilder event, String code, String execution, String flowPath) {
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.clientConnection = clientConnection;
        this.session = session;
        this.event = event;

        this.code = code;
        this.execution = execution;
        this.flowPath = flowPath;
    }


    public AuthenticationSessionModel getAuthenticationSession() {
        return authSession;
    }


    private boolean failed() {
        return response != null;
    }


    public Response getResponse() {
        return response;
    }


    public ClientSessionCode<AuthenticationSessionModel> getClientCode() {
        return clientCode;
    }

    public boolean isActionRequest() {
        return actionRequest;
    }


    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }


    public AuthenticationSessionModel initialVerifyAuthSession() {
        // Basic realm checks
        if (!checkSsl()) {
            event.error(Errors.SSL_REQUIRED);
            response = ErrorPage.error(session, Messages.HTTPS_REQUIRED);
            return null;
        }
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            response = ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
            return null;
        }

        // object retrieve
        AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(code, session, realm, AuthenticationSessionModel.class);
        if (authSession != null) {
            return authSession;
        }

        // See if we are already authenticated and userSession with same ID exists.
        String sessionId = new AuthenticationSessionManager(session).getCurrentAuthenticationSessionId(realm);
        if (sessionId != null) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            if (userSession != null) {

                LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.ALREADY_LOGGED_IN);

                ClientModel client = null;
                String lastClientUuid = userSession.getNote(AuthenticationManager.LAST_AUTHENTICATED_CLIENT);
                if (lastClientUuid != null) {
                    client = realm.getClientById(lastClientUuid);
                }

                if (client != null) {
                    session.getContext().setClient(client);
                } else {
                    loginForm.setAttribute("skipLink", true);
                }

                response = loginForm.createInfoPage();
                return null;
            }
        }

        // Otherwise just try to restart from the cookie
        response = restartAuthenticationSessionFromCookie();
        return null;
    }


    public boolean initialVerify() {
        // Basic realm checks and authenticationSession retrieve
        authSession = initialVerifyAuthSession();
        if (authSession == null) {
            return false;
        }

        // Check cached response from previous action request
        response = BrowserHistoryHelper.getInstance().loadSavedResponse(session, authSession);
        if (response != null) {
            return false;
        }

        // Client checks
        event.detail(Details.CODE_ID, authSession.getId());
        ClientModel client = authSession.getClient();
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            response = ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER);
            clientCode.removeExpiredClientSession();
            return false;
        }

        event.client(client);
        session.getContext().setClient(client);

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            response = ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED);
            clientCode.removeExpiredClientSession();
            return false;
        }


        // Check if it's action or not
        if (code == null) {
            String lastExecFromSession = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            String lastFlow = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);

            // Check if we transitted between flows (eg. clicking "register" on login screen)
            if (execution==null && !flowPath.equals(lastFlow)) {
                logger.debugf("Transition between flows! Current flow: %s, Previous flow: %s", flowPath, lastFlow);

                if (lastFlow == null || LoginActionsService.isFlowTransitionAllowed(flowPath, lastFlow)) {
                    authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, flowPath);
                    authSession.removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                    lastExecFromSession = null;
                }
            }

            if (ObjectUtil.isEqualOrBothNull(execution, lastExecFromSession)) {
                // Allow refresh of previous page
                clientCode = new ClientSessionCode<>(session, realm, authSession);
                actionRequest = false;
                return true;
            } else {
                response = showPageExpired(authSession);
                return false;
            }
        } else {
            ClientSessionCode.ParseResult<AuthenticationSessionModel> result = ClientSessionCode.parseResult(code, session, realm, AuthenticationSessionModel.class);
            clientCode = result.getCode();
            if (clientCode == null) {

                // In case that is replayed action, but sent to the same FORM like actual FORM, we just re-render the page
                if (ObjectUtil.isEqualOrBothNull(execution, authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION))) {
                    String latestFlowPath = authSession.getAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH);
                    URI redirectUri = getLastExecutionUrl(latestFlowPath, execution);

                    logger.debugf("Invalid action code, but execution matches. So just redirecting to %s", redirectUri);
                    authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, Messages.EXPIRED_ACTION);
                    response = Response.status(Response.Status.FOUND).location(redirectUri).build();
                } else {
                    response = showPageExpired(authSession);
                }
                return false;
            }


            actionRequest = true;
            if (execution != null) {
                authSession.setAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION, execution);
            }
            return true;
        }
    }


    public boolean verifyActiveAndValidAction(String expectedAction, ClientSessionCode.ActionType actionType) {
        if (failed()) {
            return false;
        }

        if (!isActionActive(actionType)) {
            return false;
        }

        if (!clientCode.isValidAction(expectedAction)) {
            AuthenticationSessionModel authSession = getAuthenticationSession();
            if (ClientSessionModel.Action.REQUIRED_ACTIONS.name().equals(authSession.getAction())) {
                logger.debug("Incorrect flow '%s' . User authenticated already.");
                response = showPageExpired(authSession);
                return false;
            } else {
                logger.errorf("Bad action. Expected action '%s', current action '%s'", expectedAction, authSession.getAction());
                response = ErrorPage.error(session, Messages.EXPIRED_CODE);
                return false;
            }
        }

        return true;
    }


    private boolean isActionActive(ClientSessionCode.ActionType actionType) {
        if (!clientCode.isActionActive(actionType)) {
            event.clone().error(Errors.EXPIRED_CODE);

            AuthenticationProcessor.resetFlow(authSession, LoginActionsService.AUTHENTICATE_PATH);

            authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, Messages.LOGIN_TIMEOUT);

            URI redirectUri = getLastExecutionUrl(LoginActionsService.AUTHENTICATE_PATH, null);
            logger.debugf("Flow restart after timeout. Redirecting to %s", redirectUri);
            response = Response.status(Response.Status.FOUND).location(redirectUri).build();
            return false;
        }
        return true;
    }


    public boolean verifyRequiredAction(String executedAction) {
        if (failed()) {
            return false;
        }

        if (!clientCode.isValidAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name())) {
            logger.debugf("Expected required action, but session action is '%s' . Showing expired page now.", authSession.getAction());
            event.error(Errors.INVALID_CODE);

            response = showPageExpired(authSession);

            return false;
        }

        if (!isActionActive(ClientSessionCode.ActionType.USER)) {
            return false;
        }

        if (actionRequest) {
            String currentRequiredAction = authSession.getAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
            if (executedAction == null || !executedAction.equals(currentRequiredAction)) {
                logger.debug("required action doesn't match current required action");
                response = redirectToRequiredActions(currentRequiredAction);
                return false;
            }
        }
        return true;
    }


    private Response restartAuthenticationSessionFromCookie() {
        logger.debug("Authentication session not found. Trying to restart from cookie.");
        AuthenticationSessionModel authSession = null;
        try {
            authSession = RestartLoginCookie.restartSession(session, realm);
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToParseRestartLoginCookie(e);
        }

        if (authSession != null) {

            event.clone();
            event.detail(Details.RESTART_AFTER_TIMEOUT, "true");
            event.error(Errors.EXPIRED_CODE);

            String warningMessage = Messages.LOGIN_TIMEOUT;
            authSession.setAuthNote(LoginActionsService.FORWARDED_ERROR_MESSAGE_NOTE, warningMessage);

            String flowPath = authSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
            if (flowPath == null) {
                flowPath = LoginActionsService.AUTHENTICATE_PATH;
            }

            URI redirectUri = getLastExecutionUrl(flowPath, null);
            logger.debugf("Authentication session restart from cookie succeeded. Redirecting to %s", redirectUri);
            return Response.status(Response.Status.FOUND).location(redirectUri).build();
        } else {
            // Finally need to show error as all the fallbacks failed
            event.error(Errors.INVALID_CODE);
            return ErrorPage.error(session, Messages.INVALID_CODE);
        }
    }


    private Response redirectToRequiredActions(String action) {
        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (action != null) {
            uriBuilder.queryParam("execution", action);
        }
        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();
    }


    private URI getLastExecutionUrl(String flowPath, String executionId) {
        return new AuthenticationFlowURLHelper(session, realm, uriInfo)
                .getLastExecutionUrl(flowPath, executionId);
    }


    private Response showPageExpired(AuthenticationSessionModel authSession) {
        return new AuthenticationFlowURLHelper(session, realm, uriInfo)
                .showPageExpired(authSession);
    }

}
