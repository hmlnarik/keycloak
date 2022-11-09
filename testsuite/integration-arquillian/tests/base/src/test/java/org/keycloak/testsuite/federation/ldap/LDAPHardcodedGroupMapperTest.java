/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation.ldap;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.LDAPRule;

import java.io.Serializable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * @author sventorben
 */
public class LDAPHardcodedGroupMapperTest extends AbstractLDAPTest implements Serializable {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.testing().ldap(TEST_REALM_NAME).prepareHardcodedGroupsLDAPTest();
    }

    /**
     * KEYCLOAK-18308
     */
    @Test
    public void testCompositeGroups() {
        // check users
        UserRepresentation john = testRealm().users().searchByUsername("johnkeycloak", Boolean.TRUE).stream().findAny().orElse(null);
        assertThat(john, notNullValue());
        final String johnId = john.getId();
        UserResource johnRes = testRealm().users().get(johnId);

        ClientRepresentation adminCli = testRealm().clients().findByClientId("admin-cli").stream().findAny().orElse(null);
        assertThat(adminCli, notNullValue());

        // check roles
        List<RoleRepresentation> roles = testRealm().clients().get(adminCli.getId()).roles().list("client_role", false);
        RoleRepresentation clientRoleGrantedViaHardcodedGroupMembership = roles.stream().findAny().orElse(null);
        assertThat(clientRoleGrantedViaHardcodedGroupMembership, notNullValue());

        // check groups
        final List<GroupRepresentation> groups = testRealm().groups().groups();
        GroupRepresentation hardcodedGroup = findGroupByName(groups, "hardcoded_group");
        assertThat(hardcodedGroup, notNullValue());
        GroupRepresentation parentGroup = findGroupByName(groups, "parent_group");
        assertThat(parentGroup, notNullValue());
        final String parentGroupId = parentGroup.getId();
        final String hardcodedGroupId = hardcodedGroup.getId();
        assertThat(idsOfGroups(parentGroup.getSubGroups()), hasItem(hardcodedGroupId));

        // check group membership
        assertThat(idsOfGroups(johnRes.groups()), containsInAnyOrder(hardcodedGroupId));
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserModel user = session.users().getUserById(appRealm, johnId);

            assertThat(user.isMemberOf(session.groups().getGroupById(appRealm, hardcodedGroupId)), is(true));
            assertThat(user.isMemberOf(session.groups().getGroupById(appRealm, parentGroupId)), is(true));
        });

        // check role membership
        assertThat(idsOfRoles(johnRes.roles().clientLevel(adminCli.getId()).listEffective()), containsInAnyOrder(clientRoleGrantedViaHardcodedGroupMembership.getId()));

        assertThat(idsOfUsers(testRealm().groups().group(hardcodedGroupId).members()), contains(johnId));
    }

    private static Set<String> idsOfGroups(List<GroupRepresentation> groups) {
        return groups.stream().filter(Objects::nonNull).map(GroupRepresentation::getId).collect(Collectors.toSet());
    }

    private static Set<String> idsOfRoles(List<RoleRepresentation> groups) {
        return groups.stream().filter(Objects::nonNull).map(RoleRepresentation::getId).collect(Collectors.toSet());
    }

    private static Set<String> idsOfUsers(List<UserRepresentation> groups) {
        return groups.stream().filter(Objects::nonNull).map(UserRepresentation::getId).collect(Collectors.toSet());
    }

    private static GroupRepresentation findGroupByName(List<GroupRepresentation> groups, String name) {
        if (groups == null) {
            return null;
        }
        return groups.stream()
          .filter(g -> Objects.equals(name, g.getName()))
          .findAny().orElseGet(() -> {
              for (GroupRepresentation group : groups) {
                  GroupRepresentation subgroup = findGroupByName(group.getSubGroups(), name);
                  if (subgroup != null) {
                      return subgroup;
                  }
              }
              return null;
          });
    }

}
