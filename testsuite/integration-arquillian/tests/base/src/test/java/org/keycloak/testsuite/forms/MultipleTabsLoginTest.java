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

package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * Tries to test multiple browser tabs
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MultipleTabsLoginTest extends AbstractTestRealmKeycloakTest {

    private String userId;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setup() {
        UserRepresentation user = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString())
                .requiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString())
                .build();

        userId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
        getCleanup().addUserId(userId);

        oauth.clientId("test-app");
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected LoginExpiredPage loginExpiredPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);


    // Test for scenario when user is logged into JS application in 2 browser tabs. Then click "logout" and he is logged-out from both tabs.
    // Now both browser tabs show the 1st login process and we need to make sure that actionURL (code with execution) is valid on both tabs, so user won't have error page when he tries to login from tab1
    @Test
    public void openMultipleTabs() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String actionUrl1 = getActionUrl(driver.getPageSource());

        oauth.openLoginForm();
        loginPage.assertCurrent();
        String actionUrl2 = getActionUrl(driver.getPageSource());

        Assert.assertEquals(actionUrl1, actionUrl2);

    }


    private String getActionUrl(String pageSource) {
        return pageSource.split("action=\"")[1].split("\"")[0].replaceAll("&amp;", "&");
    }


    @Test
    public void multipleTabsParallelLoginTest() {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        loginPage.login("login-test", "password");
        updatePasswordPage.assertCurrent();

        String tab1Url = driver.getCurrentUrl();

        // Simulate login in different browser tab. I should be directly on 'updatePasswordPage'
        oauth.openLoginForm();
        updatePasswordPage.assertCurrent();

        // Login in tab 2
        updatePasswordPage.changePassword("password", "password");
        updateProfilePage.update("John", "Doe3", "john@doe3.com");
        appPage.assertCurrent();

        // Try to go back to tab 1. We should have ALREADY_LOGGED_IN info page
        driver.navigate().to(tab1Url);
        infoPage.assertCurrent();
        Assert.assertEquals("You are already logged in.", infoPage.getInfo());

        infoPage.clickBackToApplicationLink();
        appPage.assertCurrent();
    }
}
