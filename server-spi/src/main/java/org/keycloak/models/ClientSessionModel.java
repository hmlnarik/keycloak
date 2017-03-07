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

package org.keycloak.models;

import java.util.Map;
import java.util.Set;

import org.keycloak.sessions.CommonLoginSessionModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ClientSessionModel extends CommonLoginSessionModel {

    public UserSessionModel getUserSession();
    public void setUserSession(UserSessionModel userSession);

    public String getRedirectUri();
    public void setRedirectUri(String uri);

    public Map<String, ExecutionStatus> getExecutionStatus();
    public void setExecutionStatus(String authenticator, ExecutionStatus status);
    public void clearExecutionStatus();
    public UserModel getAuthenticatedUser();
    public void setAuthenticatedUser(UserModel user);



    /**
     * Authentication request type, i.e. OAUTH, SAML 2.0, SAML 1.1, etc.
     *
     * @return
     */
    public String getAuthMethod();
    public void setAuthMethod(String method);

    /**
     * Required actions that are attached to this client session.
     *
     * @return
     */
    Set<String> getRequiredActions();

    void addRequiredAction(String action);

    void removeRequiredAction(String action);

    void addRequiredAction(UserModel.RequiredAction action);

    void removeRequiredAction(UserModel.RequiredAction action);


    /**
     * These are notes you want applied to the UserSessionModel when the client session is attached to it.
     *
     * @param name
     * @param value
     */
    public void setUserSessionNote(String name, String value);

    /**
     * These are notes you want applied to the UserSessionModel when the client session is attached to it.
     *
     * @return
     */
    public Map<String, String> getUserSessionNotes();

    public void clearUserSessionNotes();

    public static enum Action {
        OAUTH_GRANT,
        CODE_TO_TOKEN,
        VERIFY_EMAIL,
        UPDATE_PROFILE,
        CONFIGURE_TOTP,
        UPDATE_PASSWORD,
        RECOVER_PASSWORD, // deprecated
        AUTHENTICATE,
        SOCIAL_CALLBACK,
        LOGGED_OUT,
        RESET_CREDENTIALS,
        EXECUTE_ACTIONS,
        REQUIRED_ACTIONS
    }

    public enum ExecutionStatus {
        FAILED,
        SUCCESS,
        SETUP_REQUIRED,
        ATTEMPTED,
        SKIPPED,
        CHALLENGED
    }
}
