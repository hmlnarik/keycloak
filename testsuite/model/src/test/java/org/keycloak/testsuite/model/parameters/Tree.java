/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.parameters;

import org.keycloak.authorization.store.StoreFactorySpi;
import org.keycloak.models.DeploymentStateSpi;
import org.keycloak.models.UserLoginFailureSpi;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.map.client.MapClientProviderFactory;
import org.keycloak.models.map.realm.MapRealmProviderFactory;
import org.junit.Assert;
import org.keycloak.models.map.storage.MapStorageProviderFactory;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.keycloak.models.map.storage.tree.TreeStorageProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.sessions.AuthenticationSessionSpi;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.testsuite.model.storage.tree.TreeStorageComponent;
import org.keycloak.testsuite.model.storage.tree.sample.PartialStorageProviderFactory;
import com.google.common.collect.ImmutableSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class Tree extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(AuthenticationSessionSpi.class)
      .add(MapStorageSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(TreeStorageProviderFactory.class)

      .add(PartialStorageProviderFactory.class)
      .add(ConcurrentHashMapStorageProviderFactory.class)

      .add(MapStorageProviderFactory.class)
      .build();

    private final AtomicInteger counter = new AtomicInteger();

    public Tree() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public void updateConfig(Config cf) {
        setMapStorageProvider(cf, TreeStorageProviderFactory.PROVIDER_ID, "client");
        setMapStorageProvider(cf, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID,
          "clientScope", DeploymentStateSpi.NAME, "group", "realm", "role", StoreFactorySpi.NAME, "user", UserSessionSpi.NAME, UserLoginFailureSpi.NAME);
    }

    private void setMapStorageProvider(Config cf, String provider, String... spis) {
        for (String spi : spis) {
            cf.spi(spi)
              .defaultProvider("map")
              .provider("map").config("storage.provider", provider);
        }
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (TreeStorageComponent.class == clazz) {
            TreeStorageComponent<?> tree = new TreeStorageComponent<>();
            tree.setName(TreeStorageProviderFactory.PROVIDER_ID + ":" + counter.getAndIncrement());
            tree.setProviderId(TreeStorageProviderFactory.PROVIDER_ID);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(System.getProperty("tree.configResource"))))) {
                tree.getConfig().add("config", r.lines().collect(Collectors.joining(System.lineSeparator())));
            } catch (IOException ex) {
                Assert.fail("Cannot load config: " + ex);
            }
            return Stream.of((T) tree);
        } else {
            return super.getParameters(clazz);
        }
    }
}
