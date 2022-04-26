package org.keycloak.storage.datastore;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.storage.ExportImportManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Alexander Schwartz
 */
public class LegacyExportImportManager implements ExportImportManager {
    private final KeycloakSession session;

    public LegacyExportImportManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void importRealm(RealmRepresentation rep, RealmModel newRealm) {
        importUserFederationProvidersAndMappers(session, rep, newRealm);
    }

    public static void importUserFederationProvidersAndMappers(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm) {
        // providers to convert to component model
        Set<String> convertSet = new HashSet<>();
        convertSet.add(LDAPConstants.LDAP_PROVIDER);
        convertSet.add("kerberos");
        Map<String, String> mapperConvertSet = new HashMap<>();
        mapperConvertSet.put(LDAPConstants.LDAP_PROVIDER, "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");


        Map<String, ComponentModel> userStorageModels = new HashMap<>();

        if (rep.getUserFederationProviders() != null) {
            for (UserFederationProviderRepresentation fedRep : rep.getUserFederationProviders()) {
                if (convertSet.contains(fedRep.getProviderName())) {
                    ComponentModel component = convertFedProviderToComponent(newRealm.getId(), fedRep);
                    userStorageModels.put(fedRep.getDisplayName(), newRealm.importComponentModel(component));
                }
            }
        }

        // This is for case, when you have hand-written JSON file with LDAP userFederationProvider, but WITHOUT any userFederationMappers configured. Default LDAP mappers need to be created in that case.
        Set<String> storageProvidersWhichShouldImportDefaultMappers = new HashSet<>(userStorageModels.keySet());

        if (rep.getUserFederationMappers() != null) {
            for (UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                if (userStorageModels.containsKey(representation.getFederationProviderDisplayName())) {
                    ComponentModel parent = userStorageModels.get(representation.getFederationProviderDisplayName());
                    String newMapperType = mapperConvertSet.get(parent.getProviderId());
                    ComponentModel mapper = convertFedMapperToComponent(newRealm, parent, representation, newMapperType);
                    newRealm.importComponentModel(mapper);


                    storageProvidersWhichShouldImportDefaultMappers.remove(representation.getFederationProviderDisplayName());

                }
            }
        }

        for (String providerDisplayName : storageProvidersWhichShouldImportDefaultMappers) {
            ComponentUtil.notifyCreated(session, newRealm, userStorageModels.get(providerDisplayName));
        }
    }

    public static ComponentModel convertFedMapperToComponent(RealmModel realm, ComponentModel parent, UserFederationMapperRepresentation rep, String newMapperType) {
        ComponentModel mapper = new ComponentModel();
        mapper.setId(rep.getId());
        mapper.setName(rep.getName());
        mapper.setProviderId(rep.getFederationMapperType());
        mapper.setProviderType(newMapperType);
        mapper.setParentId(parent.getId());
        if (rep.getConfig() != null) {
            for (Map.Entry<String, String> entry : rep.getConfig().entrySet()) {
                mapper.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return mapper;
    }

    public static ComponentModel convertFedProviderToComponent(String realmId, UserFederationProviderRepresentation fedModel) {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setId(fedModel.getId());
        model.setName(fedModel.getDisplayName());
        model.setParentId(realmId);
        model.setProviderId(fedModel.getProviderName());
        model.setProviderType(UserStorageProvider.class.getName());
        model.setFullSyncPeriod(fedModel.getFullSyncPeriod());
        model.setPriority(fedModel.getPriority());
        model.setChangedSyncPeriod(fedModel.getChangedSyncPeriod());
        model.setLastSync(fedModel.getLastSync());
        if (fedModel.getConfig() != null) {
            for (Map.Entry<String, String> entry : fedModel.getConfig().entrySet()) {
                model.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return model;
    }

}
