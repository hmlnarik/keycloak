package org.keycloak.storage;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * @author Alexander Schwartz
 */
public interface ExportImportManager {
    void importRealm(RealmRepresentation rep, RealmModel newRealm);
}
