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
package org.keycloak.authentication.actiontoken;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsServiceChecks.AdjustFlowException.NextStep;
import org.keycloak.sessions.AuthenticationSessionModel;
import javax.ws.rs.core.Response;

/**
 *  Handler of the action token.
 *
 *  @author hmlnarik
 */
public interface ActionTokenHandler<T extends JsonWebToken> extends Provider {

    @FunctionalInterface
    public interface ProcessFlow {
        Response processFlow(boolean action, String execution, AuthenticationSessionModel authSession, String flowPath, AuthenticationFlowModel flow, String errorMessage, AuthenticationProcessor processor);
    };

    Class<T> getTokenClass();

    Predicate<? super T>[] getVerifiers(ActionTokenContext tokenContext);

    /**
     * Performs the action as per the token details. This method is only called if all verifiers
     * returned in {@link #handleToken} succeed.
     *
     * @param token
     * @param tokenContext
     * @return
     * @throws VerificationException
     */
    Response handleToken(T token, ActionTokenContext tokenContext, ProcessFlow processFlow) throws VerificationException;

    EventType eventType();

    NextStep getNextStepWhenAuthenticationSessionUnset();

    default AuthenticationSessionModel startFreshAuthenticationSession(T token, ActionTokenContext<T> tokenContext) {
        AuthenticationSessionModel authSession = tokenContext.createAuthenticationSessionForClient(token.getIssuedFor());
        authSession.setAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        return authSession;
    }

    default String getAuthenticationSessionIdFromToken(T token) {
        return null;
    }

    default String getDefaultEventError() {
        return Errors.INVALID_CODE;
    }

    default String getDefaultErrorMessage() {
        return Messages.INVALID_CODE;
    }

}
