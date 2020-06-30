package org.keycloak.models.map.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author hmlnarik
 */
public abstract class HasUpdated<K> {

    // has any setter been used?
    @JsonIgnore
    protected boolean updated;

    public abstract K getId();

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     * @return
     */
    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
