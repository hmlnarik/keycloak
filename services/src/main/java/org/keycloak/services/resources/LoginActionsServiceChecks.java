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
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
/**
 *
 * @author hmlnarik
 */
public class LoginActionsServiceChecks {

    private final LoginActionsService loginActionsService;
    private final EventBuilder event;
    private final KeycloakSession session;
    private final RealmModel realm;
    private final UriInfo uriInfo;

    @FunctionalInterface
    public interface RestartFlow {
        Response restartFlow(AuthenticationSessionModel authenticationSession);
    }

    /**
     * This check verifies that if the token has not authentication session set, a new authentication session is introduced
     * for the given client and reset-credentials flow is started with this new session.
     */
    public class StartWithFreshAuthenticationSessionIfNotSet implements Predicate<JsonWebToken> {

        private final String defaultClientId;
        private final Supplier<AuthenticationSessionModel> getAuthenticationSession;
        private final RestartFlow restartFlow;

        public StartWithFreshAuthenticationSessionIfNotSet(String defaultClientId, Supplier<AuthenticationSessionModel> getAuthenticationSession, RestartFlow restartFlow) {
            this.defaultClientId = defaultClientId;
            this.getAuthenticationSession = getAuthenticationSession;
            this.restartFlow = restartFlow;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = getAuthenticationSession.get();

            if (authSession == null) {
                authSession = loginActionsService.createAuthenticationSessionForClient(this.defaultClientId);
                throw new LoginActionsServiceException(restartFlow.restartFlow(authSession));
            }

            return true;
        }
    }

    /**
     * This check verifies that user ID (subject) from the token matches the one from the context.
     */
    public class AuthenticationSessionUserIdMatches implements Predicate<JsonWebToken> {

        private final Supplier<AuthenticationSessionModel> getAuthenticationSession;

        public AuthenticationSessionUserIdMatches(Supplier<AuthenticationSessionModel> getAuthenticationSession) {
            this.getAuthenticationSession = getAuthenticationSession;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = getAuthenticationSession.get();

            if (authSession == null || authSession.getAuthenticatedUser() == null
              || ! Objects.equals(t.getSubject(), authSession.getAuthenticatedUser().getId())) {
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.RESET_CREDENTIAL_NOT_ALLOWED));
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
    public class IsActionRequired implements Predicate<JsonWebToken> {

        private final ClientSessionModel.Action expectedAction;

        private final Supplier<AuthenticationSessionModel> getAuthenticationSession;

        public IsActionRequired(Action expectedAction, Supplier<AuthenticationSessionModel> getAuthenticationSession) {
            this.expectedAction = expectedAction;
            this.getAuthenticationSession = getAuthenticationSession;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = getAuthenticationSession.get();

            if (authSession != null && ! Objects.equals(authSession.getAction(), this.expectedAction.name())) {
                if (Objects.equals(ClientSessionModel.Action.REQUIRED_ACTIONS.name(), authSession.getAction())) {
                    throw new LoginActionsServiceException(loginActionsService.redirectToRequiredActions(null, authSession));
                }
            }

            return true;
        }
    }

    /**
     * Verifies that the authentication session has not yet been converted to user session, in other words
     * that the user has not yet completed authentication and logged in.
     */
    public class IsAlreadyLoggedIn<T extends JsonWebToken> implements Predicate<T> {

        private final Function<T, String> getAuthenticationSessionIdFromToken;

        public IsAlreadyLoggedIn(Function<T, String> getAuthenticationSessionIdFromToken) {
            this.getAuthenticationSessionIdFromToken = getAuthenticationSessionIdFromToken;
        }

        @Override
        public boolean test(T t) throws VerificationException {
            String authSessionId = t == null ? null : getAuthenticationSessionIdFromToken.apply(t);
            if (authSessionId == null) {
                return true;
            }

            if (session.sessions().getUserSession(realm, authSessionId) != null) {
                throw new LoginActionsServiceException(
                  session.getProvider(LoginFormsProvider.class)
                        .setSuccess(Messages.ALREADY_LOGGED_IN)
                        .createInfoPage());
            }

            return true;
        }
    }

    /**
     * Verifies whether the user both exists and is enabled. If yes, it also sets the user into session context.
     */
    public class IsUserValid implements Predicate<JsonWebToken> {

        private final Supplier<String> getUserId;

        private final Consumer<UserModel> setUser;

        public IsUserValid(Supplier<String> getUserId, Consumer<UserModel> setUser) {
            this.getUserId = getUserId;
            this.setUser = setUser;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            String userId = getUserId.get();

            UserModel user = userId == null ? null : session.users().getUserById(userId, realm);

            if (user == null) {
                event.error(Errors.USER_NOT_FOUND);
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.INVALID_USER));
            }

            setUser.accept(user);

            return true;
        }
    }

    /**
     * Verifies whether the client both exists and is enabled. If yes, it also sets the client into session context.
     */
    public class IsClientValid implements Predicate<JsonWebToken> {

        private final Supplier<ClientModel> getClient;

        private final Supplier<AuthenticationSessionModel> getAuthenticationSession;

        /**
         *
         * @param getClient
         * @param getAuthenticationSession Used to remove authentication session from context on error. Can be null
         */
        public IsClientValid(Supplier<ClientModel> getClient, Supplier<AuthenticationSessionModel> getAuthenticationSession) {
            this.getClient = getClient;
            this.getAuthenticationSession = getAuthenticationSession == null ? () -> null : getAuthenticationSession;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            ClientModel client = getClient.get();

            if (client == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                AuthenticationSessionModel authenticationSession = getAuthenticationSession.get();
                if (authenticationSession != null) {
                    new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authenticationSession, true);
                }
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER));
            }

            if (! client.isEnabled()) {
                event.error(Errors.CLIENT_NOT_FOUND);
                AuthenticationSessionModel authenticationSession = getAuthenticationSession.get();
                if (authenticationSession != null) {
                    new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authenticationSession, true);
                }
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED));
            }

            session.getContext().setClient(client);

            return true;
        }
    }

    /**
     * Verifies whether the given redirect URL is valid for the given client.
     */
    public class IsRedirectionValid implements Predicate<JsonWebToken> {

        private final Supplier<ClientModel> getClient;

        private final String redirectUri;

        public IsRedirectionValid(Supplier<ClientModel> getClient, String redirectUri) {
            this.getClient = getClient;
            this.redirectUri = redirectUri;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (redirectUri == null) {
                return true;
            }

            ClientModel client = getClient.get();

            if (RedirectUtils.verifyRedirectUri(uriInfo, redirectUri, realm, client) == null) {
                event.error(Errors.CLIENT_NOT_FOUND);
                throw new LoginActionsServiceException(ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED));
            }

            return true;
        }
    }

    public LoginActionsServiceChecks(LoginActionsService loginActionsService, KeycloakSession session, RealmModel realm, UriInfo uriInfo, EventBuilder event) {
        this.loginActionsService = loginActionsService;
        this.uriInfo = uriInfo;
        this.session = session;
        this.realm = realm;
        this.event = event;
    }
}
