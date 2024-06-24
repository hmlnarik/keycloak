package org.keycloak.testsuite.sessionlimits;

import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcSamlBrokerConfiguration;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@IgnoreBrowserDriver(HtmlUnitDriver.class)
public class KcSamlUserSessionLimitsBrokerTest extends AbstractUserSessionLimitsBrokerTest {
    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }
}
