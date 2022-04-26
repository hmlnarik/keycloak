package org.keycloak.storage;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author Alexander Schwartz
 */
public interface MigrationManager {

    void migrate();

    void migrate(RealmModel realm, RealmRepresentation rep, boolean skipUserDependent);
}
