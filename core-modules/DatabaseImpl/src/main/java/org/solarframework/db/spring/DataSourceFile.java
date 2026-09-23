package org.solarframework.db.spring;

import org.solarframework.db.api.IDatabaseManager;
import org.solarframework.db.api.IDatabaseService;
import org.solarframework.json.JSONItem;

import java.util.ArrayList;
import java.util.List;

import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/** Database sources as stored in `config/data/database.json`. */
class DataSourceFile extends JSONItem<DataSourceFile> {
    private final List<DatabaseService> sources = new ArrayList<>();

    public DataSourceFile(IDatabaseManager manager) {
        for (IDatabaseService ds : manager.getSources()) sources.add((DatabaseService) ds);
    }

    @Override public String file() { return DatabaseConfig.FILE; }
    public List<DatabaseService> getSources() { return sources; }

    public static DataSourceFile read(String path) { return ReadOrCreate(path, DataSourceFile.class, new DataSourceFile(SolarDBManager)); }
}
