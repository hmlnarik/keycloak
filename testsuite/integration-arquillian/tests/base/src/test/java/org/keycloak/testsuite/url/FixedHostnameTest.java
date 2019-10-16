package org.keycloak.testsuite.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FixedHostnameTest extends AbstractHostnameTest {

    private String authServerUrl;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation test = RealmBuilder.create().name("test")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .build();
        testRealms.add(test);

        RealmRepresentation customHostname = RealmBuilder.create().name("hostname")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .attribute("hostname", "custom-domain.127.0.0.1.nip.io")
                .build();
        testRealms.add(customHostname);
    }

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeNotAuthServerRemote();
    }

    @Test
    public void fixedHostname() throws Exception {
        authServerUrl = oauth.AUTH_SERVER_ROOT;
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), AuthServerTestEnricher.getAuthServerContextRoot())) {
            assertWellKnown("test", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT);

            configureFixed("keycloak.127.0.0.1.nip.io", -1, -1, false);

            assertWellKnown("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertWellKnown("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertTokenIssuer("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertTokenIssuer("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
        } finally {
            reset();
        }
    }

    @Test
    public void fixedHttpPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");

            configureFixed("keycloak.127.0.0.1.nip.io", 80, -1, false);

            assertWellKnown("test", "http://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "http://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", "http://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", "http://custom-domain.127.0.0.1.nip.io");
        } finally {
            reset();
        }
    }

    @Test
    public void fixedHostnameAlwaysHttpsHttpsPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");

            configureFixed("keycloak.127.0.0.1.nip.io", -1, 443, true);

            assertWellKnown("test", "https://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "https://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient, "test", "https://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient, "hostname", "https://custom-domain.127.0.0.1.nip.io");
        } finally {
            reset();
        }
    }

    private void assertInitialAccessTokenFromMasterRealm(Keycloak testAdminClient, String realm, String expectedBaseUrl) throws JWSInputException, ClientRegistrationException {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);

        ClientInitialAccessPresentation initialAccess = testAdminClient.realm(realm).clientInitialAccess().create(rep);
        JsonWebToken token = new JWSInput(initialAccess.getToken()).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        ClientRegistration clientReg = ClientRegistration.create().url(authServerUrl, realm).build();
        clientReg.auth(Auth.token(initialAccess.getToken()));

        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        ClientRepresentation response = clientReg.create(client);

        String registrationAccessToken = response.getRegistrationAccessToken();
        JsonWebToken registrationToken = new JWSInput(registrationAccessToken).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, registrationToken.getIssuer());
    }

    private void assertTokenIssuer(String realm, String expectedBaseUrl) throws Exception {
        oauth.realm(realm);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken token = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(AccessToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        String introspection = oauth.introspectAccessTokenWithClientCredential(oauth.getClientId(), "password", tokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode introspectionNode = objectMapper.readTree(introspection);
        assertTrue(introspectionNode.get("active").asBoolean());
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, introspectionNode.get("iss").asText());
    }

    private void assertWellKnown(String realm, String expectedBaseUrl) {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
    }

}
