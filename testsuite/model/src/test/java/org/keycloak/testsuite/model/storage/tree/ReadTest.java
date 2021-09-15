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
package org.keycloak.testsuite.model.storage.tree;

import static org.junit.Assume.assumeThat;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.tree.NodeProperties;
import org.keycloak.models.map.storage.tree.TreeStorage;
import org.keycloak.models.map.storage.tree.TreeStorageNodePrescription;
import org.keycloak.models.map.storage.tree.TreeStorageProvider;
import org.keycloak.models.map.storage.tree.TreeStorageProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import org.keycloak.testsuite.model.storage.tree.sample.PartialStorageProviderFactory;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeTrue;

/**
 *
 * @author hmlnarik
 */
@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(value = MapStorageProvider.class, only = { PartialStorageProviderFactory.PROVIDER_ID })
@RequireProvider(value = MapStorageProvider.class, only = { TreeStorageProviderFactory.PROVIDER_ID })
public class ReadTest extends KeycloakModelTest {

    private String realmId;
    private String treeStorageId1;
    private ComponentModel treeComponent;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        this.realmId = realm.getId();

        getParameters(TreeStorageComponent.class).forEach(fs -> inComittedTransaction(session -> {
            assumeThat("Cannot handle more than 1 tree storage provider", treeStorageId1, Matchers.nullValue());

            fs.setParentId(realmId);

            treeComponent = realm.addComponentModel(fs);
            if (treeStorageId1 == null) {
                treeStorageId1 = treeComponent.getId();
            }

            log.infof("Added %s tree storage provider: %s", fs.getName(), treeComponent.getId());
        }));
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testGetClient() {
        withRealm(realmId, (session, realm) -> {
            MapStorageProvider store = session.getComponentProvider(MapStorageProvider.class, treeStorageId1);
            MapStorage<MapClientEntity, ClientModel> storage = store.getStorage(ClientModel.class);
            MapKeycloakTransaction<MapClientEntity, ClientModel> tr = storage.createTransaction(session);

            MapClientEntity cl;

            cl = tr.read("client1");
            assertThat(cl, notNullValue());
            assertThat(cl.getId(), is("client1"));
            assertThat(cl.getClientId(), is("client1"));
            assertThat(cl.isEnabled(), is(Boolean.FALSE));

            cl = tr.read("client2");
            assertThat(cl, notNullValue());
            assertThat(cl.getId(), is("client2"));
            assertThat(cl.getClientId(), is("client2"));
            assertThat(cl.isEnabled(), is(Boolean.TRUE));
            assertThat(cl.getAttribute("logo"), contains("AQIDBAUGBwgJCgsMDQ4P"));
            return null;
        });
    }
    
    @Test
    public void testComposability() {
        withRealm(realmId, (session, realm) -> {
            TreeStorageProvider store = (TreeStorageProvider) session.getComponentProvider(MapStorageProvider.class, treeStorageId1);
            final TreeStorageNodePrescription cf = store.getConfigurationFor(MapClientEntity.class);
            final TreeStorageNodePrescription firstPartialStorage = cf.findFirstDfs(n -> n.getNodeProperty(NodeProperties.STORAGE_PROVIDER, String.class).map(PartialStorageProviderFactory.PROVIDER_ID::equals).orElse(false)).orElse(null);
            assumeThat("There is no partial storage", firstPartialStorage, notNullValue());
            assumeTrue("Partial storage has no parent in this instance", firstPartialStorage.getParent().isPresent());

            TreeStorage<MapClientEntity, ClientModel> storage = store.getStorage(ClientModel.class);

            MapKeycloakTransaction<MapClientEntity, ClientModel> tr = storage.createTransaction(session);
            MapClientEntity cl = tr.read("client1");
            assertThat(cl, notNullValue());
            assertThat(cl.getId(), is("client1"));
            assertThat(cl.getClientId(), is("client1"));
            assertThat(cl.isEnabled(), is(Boolean.FALSE));
            return null;
        });
    }


}
