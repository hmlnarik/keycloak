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
package org.keycloak.authentication.actiontoken.resetcred;

import org.keycloak.Config.Scope;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsServiceChecks.AdjustFlowException.NextStep;
import org.keycloak.services.resources.LoginActionsServiceChecks.IsActionRequired;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import javax.ws.rs.core.Response;
import static org.keycloak.services.resources.LoginActionsService.RESET_CREDENTIALS_PATH;

/**
 *
 * @author hmlnarik
 */
public class ResetCredentialsActionTokenHandler
  implements ActionTokenHandler<ResetCredentialsActionToken>, ActionTokenHandlerFactory<ResetCredentialsActionToken> {

    @Override
    public ActionTokenHandler<ResetCredentialsActionToken> create(KeycloakSession session) {
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
        return ResetCredentialsActionToken.TOKEN_TYPE;
    }

    @Override
    public void close() {
    }

    @Override
    public String getAuthenticationSessionIdFromToken(ResetCredentialsActionToken token) {
        return token == null ? null : token.getAuthenticationSessionId();
    }

    @Override
    public Predicate<? super ResetCredentialsActionToken>[] getVerifiers(ActionTokenContext tokenContext) {
        return new Predicate[] {
            TokenUtils.checkThat(tokenContext.getRealm()::isResetPasswordAllowed, Errors.NOT_ALLOWED, Messages.RESET_CREDENTIAL_NOT_ALLOWED),

            new IsActionRequired(tokenContext, Action.AUTHENTICATE),

//            singleUseCheck,
        };
    }

    @Override
    public Response handleToken(ResetCredentialsActionToken token, ActionTokenContext tokenContext, ProcessFlow processFlow) throws VerificationException {
        AuthenticationProcessor authProcessor = new AuthenticationProcessor() {

            @Override
            protected Response authenticationComplete() {
                boolean firstBrokerLoginInProgress = (tokenContext.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE) != null);
                if (firstBrokerLoginInProgress) {

                    UserModel linkingUser = AbstractIdpAuthenticator.getExistingUser(session, tokenContext.getRealm(), tokenContext.getAuthenticationSession());
                    if (!linkingUser.getId().equals(tokenContext.getAuthenticationSession().getAuthenticatedUser().getId())) {
                        return ErrorPage.error(session,
                          Messages.IDENTITY_PROVIDER_DIFFERENT_USER_MESSAGE,
                          tokenContext.getAuthenticationSession().getAuthenticatedUser().getUsername(),
                          linkingUser.getUsername()
                        );
                    }

                    logger.debugf("Forget-password flow finished when authenticated user '%s' after first broker login.", linkingUser.getUsername());

                    // TODO:mposolda Isn't this a bug that we redirect to 'afterBrokerLoginEndpoint' without rather continue with firstBrokerLogin and other authenticators like OTP?
                    //return redirectToAfterBrokerLoginEndpoint(authSession, true);
                    return null;
                } else {
                    return super.authenticationComplete();
                }
            }
        };

        return processFlow.processFlow(
          false,
          tokenContext.getExecutionId(),
          tokenContext.getAuthenticationSession(),
          RESET_CREDENTIALS_PATH,
          tokenContext.getRealm().getResetCredentialsFlow(),
          null,
          authProcessor
        );
    }

    @Override
    public Class<ResetCredentialsActionToken> getTokenClass() {
        return ResetCredentialsActionToken.class;
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
