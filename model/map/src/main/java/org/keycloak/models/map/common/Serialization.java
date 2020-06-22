package org.keycloak.models.map.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;

/**
 *
 * @author hmlnarik
 */
public class Serialization {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }


    public static <T extends HasUpdated> T from(T orig) {
        if (orig == null) {
            return null;
        }
        try {
            // Naive solution but will do.
            final T res = MAPPER.readValue(MAPPER.writeValueAsBytes(orig), (Class<T>) orig.getClass());
            res.setUpdated(false);
            return res;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
