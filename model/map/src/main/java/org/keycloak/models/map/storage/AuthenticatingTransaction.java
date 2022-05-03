package org.keycloak.models.map.storage;

import org.keycloak.credential.CredentialInput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.user.MapCredentialValidationOutput;

/**
 * @author Alexander Schwartz
 */
public interface AuthenticatingTransaction<V extends AbstractEntity, M> extends MapKeycloakTransaction<V, M> {
    MapCredentialValidationOutput authenticate(RealmModel realm, CredentialInput input);
}
