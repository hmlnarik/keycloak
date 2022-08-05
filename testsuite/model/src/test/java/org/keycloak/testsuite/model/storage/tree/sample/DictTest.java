package org.keycloak.testsuite.model.storage.tree.sample;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;

import org.junit.Test;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.common.ParameterizedEntityField;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Completeness;
import org.keycloak.models.map.storage.mapper.MappingEntityFieldDelegate;
import org.keycloak.models.map.storage.tree.config.MapperTranslator.AttributeValueConversionMapper;
import org.keycloak.models.map.storage.mapper.MappersMap;
import org.keycloak.models.map.storage.mapper.TemplateMapper;
import org.keycloak.testsuite.model.storage.tree.sample.PartialStorageProviderFactory.DictFieldDescriptorGetter;
import java.util.Optional;

public class DictTest {

    @SuppressWarnings("unchecked")
    <V extends MapClientEntity> V getMappedEntity(Class<V> entityClass, Dict entity) {
        MappersMap<V, Dict> clientMappers = new MappersMap<>();
        clientMappers.add((ParameterizedEntityField<V>) ParameterizedEntityField.from(MapClientEntityFields.ID),
          TemplateMapper.forTemplate("{NAME}", DictFieldDescriptorGetter.INSTANCE, String.class));
        clientMappers.put((ParameterizedEntityField<V>) ParameterizedEntityField.from(MapClientEntityFields.CLIENT_ID),
          TemplateMapper.forTemplate("{NAME}", DictFieldDescriptorGetter.INSTANCE, String.class));
        clientMappers.put((ParameterizedEntityField<V>) ParameterizedEntityField.from(MapClientEntityFields.ENABLED),
          TemplateMapper.forTemplate("{ENABLED}", DictFieldDescriptorGetter.INSTANCE, Boolean.class));
        clientMappers.put((ParameterizedEntityField<V>) ParameterizedEntityField.from(MapClientEntityFields.ATTRIBUTES, "logo"),
          new AttributeValueConversionMapper(TemplateMapper.forTemplate("{LOGO}", DictFieldDescriptorGetter.INSTANCE, String.class)));

        return Optional.of(clientMappers)
          .map(mappers -> MappingEntityFieldDelegate.delegate(mappers, entity, entityClass, Completeness.PARTIAL))
          .orElse(null);
    }

    @Test
    public void testDictClientFromMap() {
        MapClientEntity mce = getMappedEntity(MapClientEntity.class, Dict.clientDelegate());
        assertThat(mce.getClientId(), nullValue());
        assertThat(mce.isEnabled(), nullValue());
        assertThat(mce.getAttribute("logo"), nullValue());
        assertThat(mce.getAttributes().keySet(), is(empty()));

        Dict.asDict(mce).put(Dict.CLIENT_FIELD_NAME, "name");
        Dict.asDict(mce).put(Dict.CLIENT_FIELD_ENABLED, false);
        Dict.asDict(mce).put(Dict.CLIENT_FIELD_LOGO, "thisShouldBeBase64Logo");
        Dict.asDict(mce).put("nonexistent", "nonexistent");

        assertThat(mce.getId(), is("name"));
        assertThat(mce.getClientId(), is("name"));
        assertThat(mce.isEnabled(), is(false));
        assertThat(mce.getAttribute("logo"), hasItems("thisShouldBeBase64Logo"));
        assertThat(mce.getAttributes().keySet(), hasItems("logo"));
    }

    @Test
    public void testDictClientFromEntity() {
        MapClientEntity mce = getMappedEntity(MapClientEntity.class, Dict.clientDelegate());
        
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_NAME), nullValue());
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_ENABLED), nullValue());
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), nullValue());

        mce.setClientId("name");
        mce.setEnabled(false);
        mce.setAttribute("logo", Arrays.asList("thisShouldBeBase64Logo"));
        mce.setAttribute("blah", Arrays.asList("thisShouldBeBase64Logofdas"));
        
        assertThat(mce.getAttributes().keySet(), hasItems("logo"));
        
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_NAME), is("name"));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_ENABLED), is(false));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), is("thisShouldBeBase64Logo"));
        
        mce.setAttribute("logo", Arrays.asList("thisShouldBeAnotherBase64Logo"));
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), is("thisShouldBeAnotherBase64Logo"));

        mce.removeAttribute("logo");
        assertThat(Dict.asDict(mce).get(Dict.CLIENT_FIELD_LOGO), nullValue());
    }
}
