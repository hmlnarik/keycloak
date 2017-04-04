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
package org.keycloak.authentication.actiontoken.execactions;

import org.keycloak.Config.Scope;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsServiceChecks;
import org.keycloak.services.resources.LoginActionsServiceChecks.AdjustFlowException.NextStep;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import javax.ws.rs.core.Response;

/**
 *
 * @author hmlnarik
 */
public class ExecuteActionsActionTokenHandler
  implements ActionTokenHandler<ExecuteActionsActionToken>, ActionTokenHandlerFactory<ExecuteActionsActionToken> {

    private static class IsRedirectValid implements Predicate<ExecuteActionsActionToken> {

        private final ActionTokenContext tokenContext;

        public IsRedirectValid(ActionTokenContext tokenContext) {
            this.tokenContext = tokenContext;
        }

        @Override
        public boolean test(ExecuteActionsActionToken t) throws VerificationException {
            if (t.getRedirectUri() != null
              && RedirectUtils.verifyRedirectUri(tokenContext.getUriInfo(), t.getRedirectUri(),
                tokenContext.getRealm(), tokenContext.getAuthenticationSession().getClient()) == null) {
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_REDIRECT_URI, Messages.INVALID_REDIRECT_URI);
            }

            return true;
        }
    }

    @Override
    public ActionTokenHandler<ExecuteActionsActionToken> create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return ExecuteActionsActionToken.TOKEN_TYPE;
    }

    @Override
    public void close() {
    }

    @Override
    public String getAuthenticationSessionIdFromToken(ExecuteActionsActionToken token) {
        return token == null ? null : token.getAuthenticationSessionId();
    }

    @Override
    public Predicate<? super ExecuteActionsActionToken>[] getVerifiers(ActionTokenContext tokenContext) {
        return new Predicate[] {
            new LoginActionsServiceChecks.IsActionRequired(tokenContext, Action.AUTHENTICATE),
            new IsRedirectValid(tokenContext)
        };
    }

    @Override
    public Response handleToken(ExecuteActionsActionToken token, ActionTokenContext tokenContext, ProcessFlow processFlow) throws VerificationException {
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();

        String redirectUri = RedirectUtils.verifyRedirectUri(tokenContext.getUriInfo(), token.getRedirectUri(),
          tokenContext.getRealm(), authSession.getClient());

        if (redirectUri != null) {
            authSession.setAuthNote(AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS, "true");

            authSession.setRedirectUri(redirectUri);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        }

        token.getRequiredActions().stream().forEach(authSession::addRequiredAction);

        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);

        String nextAction = AuthenticationManager.nextRequiredAction(tokenContext.getSession(), authSession, tokenContext.getClientConnection(), tokenContext.getRequest(), tokenContext.getUriInfo(), tokenContext.getEvent());
        return AuthenticationManager.redirectToRequiredActions(tokenContext.getSession(), tokenContext.getRealm(), authSession, tokenContext.getUriInfo(), nextAction);
    }

    @Override
    public Class<ExecuteActionsActionToken> getTokenClass() {
        return ExecuteActionsActionToken.class;
    }

    @Override
    public EventType eventType() {
        return EventType.RESET_PASSWORD;
    }

    @Override
    public NextStep getNextStepWhenAuthenticationSessionUnset() {
        return NextStep.START_RESET_CREDENTIALS_FLOW_WITH_NEW_SESSION;
    }

    @Override
    public String getDefaultEventError() {
        return Errors.NOT_ALLOWED;
    }

    @Override
    public String getDefaultErrorMessage() {
        return Messages.RESET_CREDENTIAL_NOT_ALLOWED;
    }
}
