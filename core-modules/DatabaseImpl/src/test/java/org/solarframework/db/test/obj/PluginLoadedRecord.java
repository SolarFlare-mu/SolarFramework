package org.solarframework.db.test.obj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.solarframework.db.spring.DatabaseObject;

@Entity
@Table(name = "plugin_loaded_record", comment = "Entity loaded through a plugin-style classloader for transaction tests.")
public class PluginLoadedRecord extends DatabaseObject.ID_RECORD_OBJ<Long, PluginLoadedRecord> {
    @Column(name = "Name", nullable = false)
    private String name;

    protected PluginLoadedRecord() {}
    public PluginLoadedRecord(Long id, String name) {
        this.ID = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
