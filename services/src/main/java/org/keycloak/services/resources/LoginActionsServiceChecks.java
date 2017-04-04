/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.DefaultActionToken;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ExplainedTokenVerificationException;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsServiceChecks.AdjustFlowException.NextStep;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
/**
 *
 * @author hmlnarik
 */
public class LoginActionsServiceChecks {

    private static final Logger LOG = Logger.getLogger(LoginActionsServiceChecks.class.getName());

    @FunctionalInterface
    public interface RestartFlow {
        Response restartFlow(AuthenticationSessionModel authenticationSession);
    }

    public static class AdjustFlowException extends VerificationException {
        public static enum NextStep {
            NONE,
            RESTART_FLOW_FROM_COOKIE,
            START_RESET_CREDENTIALS_FLOW_WITH_NEW_SESSION,
            REDIRECT_TO_REQUIRED_ACTIONS,
            REDIRECT_TO_RESET_CREDENTIALS,
        }

        private final NextStep nextStep;

        public AdjustFlowException(NextStep nextStep) {
            this.nextStep = nextStep == null ? NextStep.NONE : nextStep;
        }

        public NextStep getNextStep() {
            return nextStep;
        }
    }

    /**
     * This check verifies that if the token has not authentication session set, a new authentication session is introduced
     * for the given client and reset-credentials flow is started with this new session.
     */
    public static class StartWithFreshAuthenticationSessionIfNotSet implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;
        private final NextStep nextStepOnFailure;

        public StartWithFreshAuthenticationSessionIfNotSet(ActionTokenContext<?> context, NextStep nextStepOnFailure) {
            this.context = context;
            this.nextStepOnFailure = nextStepOnFailure;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();

            if (authSession == null) {
                throw new AdjustFlowException(nextStepOnFailure);
            }

            return true;
        }
    }

    /**
     * This check verifies that user ID (subject) from the token matches
     * the one from the authentication session.
     */
    public static class AuthenticationSessionUserIdMatchesOneFromToken implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        public AuthenticationSessionUserIdMatchesOneFromToken(ActionTokenContext<?> context) {
            this.context = context;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();

            if (authSession == null || authSession.getAuthenticatedUser() == null
              || ! Objects.equals(t.getSubject(), authSession.getAuthenticatedUser().getId())) {
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_TOKEN, Messages.INVALID_USER);
            }

            return true;
        }
    }

    /**
     * Verifies that if authentication session exists and any action is required according to it, then it is
     * the expected one.
     *
     * If there is an action required in the session, furthermore it is not the expected one, and the required
     * action is redirection to "required actions", it throws with response performing the redirect to required
     * actions.
     * @param <T>
     */
    public static class IsActionRequired implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        private final ClientSessionModel.Action expectedAction;

        public IsActionRequired(ActionTokenContext<?> context, Action expectedAction) {
            this.context = context;
            this.expectedAction = expectedAction;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();

            if (authSession != null && ! Objects.equals(authSession.getAction(), this.expectedAction.name())) {
                if (Objects.equals(ClientSessionModel.Action.REQUIRED_ACTIONS.name(), authSession.getAction())) {
                    throw new LoginActionsServiceException(
                      AuthenticationManager.nextActionAfterAuthentication(context.getSession(), authSession,
                        context.getClientConnection(), context.getRequest(), context.getUriInfo(), context.getEvent()));
                }
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_TOKEN, Messages.INVALID_CODE);
            }

            return true;
        }
    }

    /**
     * Verifies that the authentication session has not yet been converted to user session, in other words
     * that the user has not yet completed authentication and logged in.
     */
    public static <T extends JsonWebToken> void checkNotLoggedInYet(ActionTokenContext<T> context, String authSessionId) throws VerificationException {
        if (authSessionId == null) {
            return;
        }

        UserSessionModel userSession = context.getSession().sessions().getUserSession(context.getRealm(), authSessionId);
        if (userSession != null) {
            LoginFormsProvider loginForm = context.getSession().getProvider(LoginFormsProvider.class)
              .setSuccess(Messages.ALREADY_LOGGED_IN);

            ClientModel client = null;
            String lastClientUuid = userSession.getNote(AuthenticationManager.LAST_AUTHENTICATED_CLIENT);
            if (lastClientUuid != null) {
                client = context.getRealm().getClientById(lastClientUuid);
            }

            if (client != null) {
                context.getSession().getContext().setClient(client);
            } else {
                loginForm.setAttribute("skipLink", true);
            }

            throw new LoginActionsServiceException(loginForm.createInfoPage());
        }
    }

    /**
     *  Verifies whether the user given by ID both exists in the current realm. If yes,
     *  it optionally also injects the user using the given function (e.g. into session context).
     */
    public static <T extends DefaultActionToken> void checkIsUserValid(ActionTokenContext<T> context, T t) throws VerificationException {
        String userId = t.getUserId();

        UserModel user = userId == null ? null : context.getSession().users().getUserById(userId, context.getRealm());

        if (user == null) {
            throw new ExplainedTokenVerificationException(t, Errors.USER_NOT_FOUND, Messages.INVALID_USER);
        }

        if (! user.isEnabled()) {
            throw new ExplainedTokenVerificationException(t, Errors.USER_DISABLED, Messages.INVALID_USER);
        }

        context.getAuthenticationSession().setAuthenticatedUser(user);
    }

    /**
     * Verifies whether the client denoted by client ID in token's {@code iss} ({@code issuedFor})
     * field both exists and is enabled. If yes,
     * it optionally also injects the client using the given function (e.g. into session context).
     */
    public static <T extends DefaultActionToken> void checkIsClientValid(ActionTokenContext<T> context, T t) throws VerificationException {
        String clientId = t.getIssuedFor();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        ClientModel client = authSession == null ? null : authSession.getClient();

        if (client == null) {
            throw new ExplainedTokenVerificationException(t, Errors.CLIENT_NOT_FOUND, Messages.UNKNOWN_LOGIN_REQUESTER);
        }

        if (clientId != null && ! Objects.equals(client.getClientId(), clientId)) {
            throw new ExplainedTokenVerificationException(t, Errors.CLIENT_NOT_FOUND, Messages.UNKNOWN_LOGIN_REQUESTER);
        }

        if (! client.isEnabled()) {
            throw new ExplainedTokenVerificationException(t, Errors.CLIENT_NOT_FOUND, Messages.LOGIN_REQUESTER_NOT_ENABLED);
        }
    }

    /**
     * Verifies whether the given redirect URL, when set, is valid for the given client.
     */
    public static class IsRedirectValid implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        private final String redirectUri;

        public IsRedirectValid(ActionTokenContext<?> context, String redirectUri) {
            this.context = context;
            this.redirectUri = redirectUri;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (redirectUri == null) {
                return true;
            }

            ClientModel client = context.getAuthenticationSession().getClient();

            if (RedirectUtils.verifyRedirectUri(context.getUriInfo(), redirectUri, context.getRealm(), client) == null) {
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_REDIRECT_URI, Messages.INVALID_REDIRECT_URI);
            }

            return true;
        }
    }

    /**
     *  This check verifies that current authentication session is consistent with the one specified in token.
     *  Examples:
     *  <ul>
     *      <li>1. Email from administrator with reset e-mail - token does not contain auth session ID</li>
     *      <li>2. Email from "verify e-mail" step - token does contain auth session ID.</li>
     *      <li>3. User clicked the link in an e-mail - authentication session cookie is not set</li>
     *      <li>4. User clicked the link in an e-mail while having authentication running - authentication session cookie
     *             is already set in the browser</li>
     *  </ul>
     *
     *  <ul>
     *      <li>For combinations 1 and 3, 1 and 4, and 2 and 3: Requests next step</li>
     *      <li>For combination 2 and 4:
     *          <ul>
     *          <li>If the auth session IDs from token and cookie match, pass</li>
     *          <li>Else if the auth session from cookie was forked and its parent auth session ID
     *              matches that of token, replaces current auth session with that of parent and passes</li>
     *          <li>Else requests restart session from cookie</li>
     *          </ul>
     *      </li>
     *  </ul>
     *
     *  When the check passes, it also sets the authentication session in token context accordingly.
     *
     *  @param <T>
     */
    public static <T extends JsonWebToken> void checkAuthenticationSessionFromCookieMatchesOneFromToken(ActionTokenContext<T> context, String authSessionIdFromToken) throws VerificationException {
        NextStep nextStepOnFailure = context.getHandler().getNextStepWhenAuthenticationSessionUnset();

        if (authSessionIdFromToken == null) {
            throw new AdjustFlowException(nextStepOnFailure);
        }

        AuthenticationSessionManager asm = new AuthenticationSessionManager(context.getSession());
        String authSessionIdFromCookie = asm.getCurrentAuthenticationSessionId(context.getRealm());

        if (authSessionIdFromCookie == null) {
            throw new AdjustFlowException(nextStepOnFailure);
        }

        AuthenticationSessionModel authSessionFromCookie = context.getSession()
          .authenticationSessions().getAuthenticationSession(context.getRealm(), authSessionIdFromCookie);
        if (authSessionFromCookie == null) {    // Cookie contains ID of expired auth session
            throw new AdjustFlowException(nextStepOnFailure);
        }

        if (Objects.equals(authSessionIdFromCookie, authSessionIdFromToken)) {
            context.setAuthenticationSession(authSessionFromCookie);
            return;
        }

        String parentSessionId = authSessionFromCookie.getAuthNote(AuthenticationProcessor.FORKED_FROM);
        if (parentSessionId == null || ! Objects.equals(authSessionIdFromToken, parentSessionId)) {
            throw new AdjustFlowException(nextStepOnFailure);
        }

        AuthenticationSessionModel authSessionFromParent = context.getSession()
          .authenticationSessions().getAuthenticationSession(context.getRealm(), parentSessionId);

        // It's the correct browser. Let's remove forked session as we won't continue
        // from the login form (browser flow) but from the token's flow
        // Don't expire KC_RESTART cookie at this point
        asm.removeAuthenticationSession(context.getRealm(), authSessionFromCookie, false);
        LOG.infof("Removed forked session: %s", authSessionFromCookie.getId());

        // Refresh browser cookie
        asm.setAuthSessionCookie(parentSessionId, context.getRealm());

        context.setAuthenticationSession(authSessionFromParent);
        context.setExecutionId(authSessionFromParent.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION));
    }

    /**
     *  This check verifies that authentication session is set in the token context.
     */
    public static class AuthenticationSessionSet implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        private final NextStep nextStepOnFailure;

        public AuthenticationSessionSet(ActionTokenContext<?> context, NextStep nextStepOnFailure) {
            this.context = context;
            this.nextStepOnFailure = nextStepOnFailure;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (context.getAuthenticationSession() == null) {
                throw new AdjustFlowException(this.nextStepOnFailure);
            }

            return true;
        }
    }

}
