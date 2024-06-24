package org.keycloak.testsuite.sessionlimits;

import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@IgnoreBrowserDriver(HtmlUnitDriver.class)
public class KcOidcUserSessionLimitsBrokerTest extends AbstractUserSessionLimitsBrokerTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }
}
