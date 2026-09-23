package org.solarframework.db.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.solarframework.db.api.DatabaseType;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableStats {
    @JsonIgnore
    private transient Set<String> timeColumns;

    public String sourceName;
    public DatabaseType sourceType;
    public String schemaName;
    public String tableName;
    public long totalRows = 0;
    public List<Column> columns = new ArrayList<>();
    public List<Reference> references = new ArrayList<>();

    public String getSourceName() {
        return sourceName;
    }
    public DatabaseType getSourceType() {
        return sourceType;
    }
    public String getSchemaName() {
        return schemaName;
    }
    public String getTableName() {
        return tableName;
    }
    public long getTotalRows() {
        return totalRows;
    }
    public List<Column> getColumns() {
        return columns;
    }
    public List<String> getColumnNames() {
        return columns.stream().map(Column::getName).toList();
    }
    /** One column by name, or null when the schema has no such column. JDBC hands names back in the database's own casing, which is rarely the casing the entity spells them in. */
    public Column getColumn(String name) {
        return columns.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    /** The foreign keys this table declares. A schema that declares none - SQLite cannot add one to a table that already exists - reports an empty list rather than an absent one. */
    public List<Reference> getReferences() {
        return references;
    }

    /** One foreign key: the column here that holds it, and the table and column it points at. */
    public static class Reference {
        public String name;
        public String column;
        public String table;
        public String referencedColumn;

        public String getName() {
            return name;
        }
        public String getColumn() {
            return column;
        }
        public String getTable() {
            return table;
        }
        public String getReferencedColumn() {
            return referencedColumn;
        }
    }

    /** The columns a migration has to put through a normaliser rather than copy across as they stand. */
    public Set<String> getTimeColumns() {
        return timeColumns == null ? timeColumns = getColumns().stream().filter(Column::isTemporal).map(c -> c.getName().toLowerCase()).collect(Collectors.toSet()) : timeColumns;
    }
    public static class Column {
        /** Not the serialised {@code storageType} property, which the getter below produces - this only keeps the answer once it has been worked out. */
        @JsonIgnore
        private transient String cachedStorageType;

        public String name;
        public String type;
        public int dataType;
        public int ordinalPosition;
        public int size;
        public int decimalDigits;
        public boolean nullable;
        public boolean isAutoIncrement;
        public boolean isGenerated;
        public boolean isPrimaryKey;
        public boolean isUnique;
        public String defaultValue;
        public String remarks;
        public String referencesTable;
        public String referencesColumn;

        public boolean isAutoIncrement() {
            return isAutoIncrement;
        }
        /** A column the database computes for itself, so nothing may be written to it. */
        public boolean isGenerated() {
            return isGenerated;
        }
        /** The {@link java.sql.Types} code, which says what a column holds without anyone matching on the driver's spelling of the type. */
        public int getDataType() {
            return dataType;
        }
        /** Where the column sits in the table as the schema holds it, counting from 1. */
        public int getOrdinalPosition() {
            return ordinalPosition;
        }
        /** The table this column's foreign key points at, or null when it holds none. */
        public String getReferencesTable() {
            return referencesTable;
        }
        public String getReferencesColumn() {
            return referencesColumn;
        }
        public boolean isForeignKey() {
            return referencesTable != null;
        }
        public boolean isNullable() {
            return nullable;
        }
        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }
        public boolean isUnique() {
            return isUnique;
        }
        public int getDecimalDigits() {
            return decimalDigits;
        }
        public int getSize() {
            return size;
        }
        public String getType() {
            return type;
        }
        /** The type as a reader shows it, length and all - worked out once and kept, the way {@link #getTimeColumns} keeps its answer. SQLite hands back the whole declaration the column was created with, MariaDB only the bare type, so a length is added only when one is not already there. */
        public String getStorageType() {
            if (cachedStorageType != null) return cachedStorageType;
            String declared = type.toUpperCase();
            if (declared.contains("(") || size <= 0) return cachedStorageType = declared;
            if (decimalDigits > 0) return cachedStorageType = declared + "(" + size + "," + decimalDigits + ")";
            return cachedStorageType = declared.contains("CHAR") ? declared + "(" + size + ")" : declared;
        }
        /** A date or time column, which a migration has to normalise rather than copy as it stands. The {@link java.sql.Types} code is exact where the driver reports one; the name is matched as well, since a hand-built detail carries no code - and a name alone misses a declaration carrying a precision, such as {@code DATETIME(3)}. */
        public boolean isTemporal() {
            return dataType == Types.TIMESTAMP || dataType == Types.DATE || dataType == Types.TIME || dataType == Types.TIMESTAMP_WITH_TIMEZONE || dataType == Types.TIME_WITH_TIMEZONE || TEMPORAL_TYPES.contains(type.toUpperCase());
        }
        private static final Set<String> TEMPORAL_TYPES = Set.of("DATETIME", "TIMESTAMP", "DATE", "TIME");
        public String getName() {
            return name;
        }
        public String getDefaultValue() {
            return defaultValue;
        }
        public String getRemarks() {
            return remarks;
        }
    }
}
