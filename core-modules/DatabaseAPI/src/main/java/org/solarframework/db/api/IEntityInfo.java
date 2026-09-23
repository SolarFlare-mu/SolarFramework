package org.solarframework.db.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.solarframework.db.api.dto.TableStats;
import org.solarframework.db.spring.DatabaseObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.solarframework.db.spring.DatabaseRegistry.SolarDBManager;

/**
 * Not abstract, though {@link org.solarframework.db.spring.EntityInfo} is the only real subclass and its one
 * addition ({@code entityClass}) is {@code @JsonIgnore}: every field a saved data source file needs is already
 * declared here. Gson 2.14 refuses to instantiate an abstract class at all when deserialising {@code List<IEntityInfo>}
 * - unlike {@link FieldInfo}/{@link Relation}, which have no no-arg constructor either but are concrete, so Gson's
 * usual Unsafe-allocate-then-reflect-in-the-fields path still applies to them. Being abstract made every load of
 * {@code database.json} throw, silently reset by {@code JSONItem.ReadOrCreate}'s fallback - the file only ever
 * looked like it was persisting non-default sources.
 */
public class IEntityInfo {
    protected String className;
    protected String simpleClassName;
    protected String tableName;
    protected String tableComment;
    protected final String classLoader;
    protected final Set<FieldInfo> fields = new HashSet<>();
    protected final Set<Relation> relations = new HashSet<>();


    protected IEntityInfo(Class<?> clazz, String classLoaderKey) {
        this.classLoader = classLoaderKey;
        Update(clazz);
    }

    public void Update(Class<?> clazz) {}

    public String getClassName() {
        return className;
    }
    public String getSimpleClassName() {
        return simpleClassName;
    }
    public String getTableName() {
        return tableName;
    }
    public String getTableComment() {
        return tableComment == null ? "" : tableComment;
    }
    public String getClassLoader() {
        return classLoader;
    }
    /** Whether an administrator places this on a data source themselves - nothing adopts it, so a table designed after the platform was installed lands in the database its owner picks rather than the default one. */
    public boolean isPlacedByHand() {
        return SolarDBManager.needsManualAssignment(classLoader);
    }

    public Set<FieldInfo> getFields() {
        return fields;
    }
    public Set<Relation> getRelations() {
        return relations;
    }

    public Set<String> getBinaryColumns() {
        return fields.stream().filter(FieldInfo::isBinary).map(FieldInfo::getColumnName).collect(java.util.stream.Collectors.toSet());
    }
    public Relation getRelationOf(FieldInfo field) {
        return field.getRelationProperty().isBlank() ? null : relations.stream().filter(r -> r.getVariableName().equals(field.getRelationProperty())).findFirst().orElse(null);
    }

    /** What columns a "needs to be updated" verdict actually means, so the log naming it says which columns rather than leaving the reader to work it out by hand. */
    @JsonIgnore
    public String getSchemaMismatch() {
        TableStats schema = getTableStats();
        List<String> existing = schema.getColumnNames();
        Set<String> declared = fields.stream().map(FieldInfo::getColumnName).collect(java.util.stream.Collectors.toSet());
        List<String> missing = declared.stream().filter(c -> !existing.contains(c)).sorted().toList();
        List<String> extra = existing.stream().filter(c -> !declared.contains(c)).sorted().toList();
        StringBuilder summary = new StringBuilder();
        if (!missing.isEmpty()) summary.append("missing column(s) ").append(missing);
        if (!extra.isEmpty()) summary.append(summary.isEmpty() ? "" : ", ").append("column(s) in the table but not the entity ").append(extra);
        if (summary.isEmpty()) {
            List<String> lengthMismatches = fields.stream()
                    .filter(f -> f.getMaxLength() > 0 && schema.getColumn(f.getColumnName()) != null && schema.getColumn(f.getColumnName()).getSize() != f.getMaxLength())
                    .map(f -> f.getColumnName() + " (entity " + f.getMaxLength() + ", table " + schema.getColumn(f.getColumnName()).getSize() + ")")
                    .sorted().toList();
            if (!lengthMismatches.isEmpty()) summary.append("length differs on ").append(lengthMismatches);
        }
        return summary.isEmpty() ? "column set and lengths match; the type itself differs - compare each column's declared length here against its storage type on the Database Manager's Entities tab" : summary.toString();
    }

    /** Every live entity is an {@link org.solarframework.db.spring.EntityInfo} overriding both of these; a bare {@code IEntityInfo} is what deserialising a data source file produces, kept only long enough to be matched by {@link #getClassName()} against a loaded class - asking it for anything more is a caller mistake. */
    public Class<?> getEntityClass() { throw new UnsupportedOperationException("'" + className + "' has no loaded class behind it: read from a file, never adopted by a live entity."); }
    public TableStats getTableStats() { throw new UnsupportedOperationException("'" + className + "' has no loaded class behind it: read from a file, never adopted by a live entity."); }


    /* ===== What the schema says about this entity's table, asked of the entity itself =====
     * Every one of these is @JsonIgnore: an IEntityInfo is serialised straight into a page's JS state
     * (the Database Manager inlines the built-in entities that way), and a derived getter would put a
     * second copy of the whole TableStats into that payload and query the database to build it. What
     * the page wants is getTableStats(), which it already had. */

    /** The schema the table really has, or null when it was never created and when its source cannot be reached - {@link #getTableStats} answers by throwing in the second case, and a reader wanting what the class declares should not have to catch that. */
    @JsonIgnore
    public TableStats getSchema() {
        try {
            return getTableStats();
        } catch (Exception ignored) {
            return null;
        }
    }
    /** The database holding this entity's table, or null where no table has been created for it. */
    @JsonIgnore
    public String getSourceName() {
        TableStats schema = getSchema();
        return schema == null ? null : schema.getSourceName();
    }
    @JsonIgnore
    public String getSchemaName() {
        TableStats schema = getSchema();
        return schema == null ? null : schema.getSchemaName();
    }
    /** Rows currently stored, and zero where there is no table to count. */
    @JsonIgnore
    public long getTotalRows() {
        TableStats schema = getSchema();
        return schema == null ? 0 : schema.getTotalRows();
    }

    /** Every column as both sides describe it, the declaration paired with the schema row. Asks the schema once rather than once per column. */
    @JsonIgnore
    public List<ColumnInfo> getColumns() {
        TableStats schema = getSchema();
        return fields.stream().map(field -> new ColumnInfo(field, schema == null ? null : schema.getColumn(field.getColumnName()), getRelationOf(field))).toList();
    }
    /** One column as both sides describe it. */
    public ColumnInfo getColumn(FieldInfo field) {
        TableStats schema = getSchema();
        return new ColumnInfo(field, schema == null ? null : schema.getColumn(field.getColumnName()), getRelationOf(field));
    }
    /** One column by the name it is stored under, or null when this entity declares no such column. */
    @JsonIgnore
    public ColumnInfo getColumn(String columnName) {
        return fields.stream().filter(f -> f.getColumnName().equalsIgnoreCase(columnName)).findFirst().map(this::getColumn).orElse(null);
    }


    /**
     * One column as both sides describe it: what the entity declares, and what the schema really has.
     * <p>The schema answers wherever it has an answer, because it is the truth of what is stored; the declaration
     * fills in what only the code knows, such as an enum's constants. Where the schema has no such column at all
     * {@link #isInDatabase} says so rather than hiding it - an entity declaring a column the database has not got
     * is worth seeing, not worth papering over.
     */
    public static class ColumnInfo {
        private final FieldInfo field;
        private final TableStats.Column schema;
        private final Relation relation;

        public ColumnInfo(FieldInfo field, TableStats.Column schema, Relation relation) {
            this.field = field; this.schema = schema; this.relation = relation;
        }

        public FieldInfo getField() {
            return field;
        }
        public TableStats.Column getSchema() {
            return schema;
        }
        public Relation getRelation() {
            return relation;
        }
        /** False when the entity declares a column the database has not got, in which case only the declaration can answer for it. */
        public boolean isInDatabase() {
            return schema != null;
        }

        public String getName() {
            return field.getColumnName();
        }
        public String getVariableName() {
            return field.getVariableName();
        }
        /** SQLite and older schemas may have no stored description, so the declaration fills that gap. */
        public String getComment() {
            return schema != null && schema.getRemarks() != null && !schema.getRemarks().isBlank() ? schema.getRemarks() : field.getComment();
        }
        public String getJavaTypeName() {
            return field.getJavaTypeName();
        }
        public String getSimpleTypeName() {
            return field.getSimpleTypeName();
        }
        public List<String> getEnumConstants() {
            return field.getEnumConstants();
        }
        public boolean isBinary() {
            return field.isBinary();
        }

        /** The type the column was really created as, or null where the database has not got it. */
        public String getStorageType() {
            return schema == null ? null : schema.getStorageType();
        }
        /** How much the column will actually hold: the width the schema was created with, and the declared one only where there is no schema to ask. A form sized from the annotation lets a user type more than the database will take. */
        public int getMaxLength() {
            return schema != null && schema.getSize() > 0 ? schema.getSize() : field.getMaxLength();
        }
        public boolean isPrimaryKey() {
            return schema == null ? field.isPrimaryKey() : schema.isPrimaryKey();
        }
        public boolean isNullable() {
            return schema == null ? field.isNullable() : schema.isNullable();
        }
        public boolean isUnique() {
            return schema == null ? field.isUnique() : schema.isUnique();
        }
        /** The table this key points at: the schema's own constraint where one is declared, and the relation mapped beside the column where none is - SQLite cannot add a constraint to a table that already exists, so its schemas declare none. Blank when the column is not a key. */
        public String getReferencesTable() {
            if (schema != null && schema.isForeignKey()) return schema.getReferencesTable();
            return relation == null ? "" : relation.getForeignTableName();
        }
    }


    public static class FieldInfo {
        private final String variableName;
        private final String columnName;
        private final String comment;
        private final String javaTypeName;
        private final String simpleTypeName;
        private final List<String> enumConstants;
        private final String relationProperty;
        private final int maxLength;
        private final boolean primaryKey;
        private final boolean unique;
        private final boolean nullable;

        public FieldInfo(Field f) {
            variableName = f.getName();
            javaTypeName = f.getType().getName();
            primaryKey = f.getAnnotation(Id.class) != null;
            unique = f.getAnnotation(Column.class) != null && f.getAnnotation(Column.class).unique();
            nullable = f.getAnnotation(Column.class) != null && f.getAnnotation(Column.class).nullable();
            columnName = DatabaseObject.getColumnName(f);
            comment = f.getAnnotation(Column.class) == null ? "" : f.getAnnotation(Column.class).comment();
            maxLength = f.getAnnotation(Column.class) == null ? 0 : f.getAnnotation(Column.class).length();
            simpleTypeName = f.getType().getSimpleName();
            enumConstants = f.getType().isEnum() ? Arrays.stream(f.getType().getEnumConstants()).map(String::valueOf).toList() : List.of();
            relationProperty = columnName.length() > 2 && columnName.endsWith("ID") ? Character.toLowerCase(columnName.charAt(0)) + columnName.substring(1, columnName.length() - 2) : "";
        }

        public String getVariableName() {
            return variableName;
        }
        public String getColumnName() {
            return columnName;
        }
        public String getComment() {
            return comment == null ? "" : comment;
        }
        /** The name the JVM gives the field's type - {@code java.lang.String}, {@code [B}. Not to be confused with a {@link TableStats.Column#getType} beside it, which is the SQL type the database stores it as. */
        public String getJavaTypeName() {
            return javaTypeName;
        }
        /** The same type as a developer reading the entity sees it - {@code String}, {@code UserStatus}. */
        public String getSimpleTypeName() {
            return simpleTypeName;
        }
        public List<String> getEnumConstants() {
            return enumConstants;
        }
        public String getRelationProperty() {
            return relationProperty;
        }
        /** The width {@code @Column(length = ...)} asks for. What the column really holds is {@link ColumnInfo#getMaxLength}, which prefers the schema. */
        public int getMaxLength() {
            return maxLength;
        }
        public boolean isBinary() {
            return "[B".equals(javaTypeName);
        }
        public boolean isPrimaryKey() {
            return primaryKey;
        }
        public boolean isUnique() {
            return unique;
        }
        public boolean isNullable() {
            return nullable;
        }
    }
    public static class Relation {
        private final String variableName;
        private final String foreignClassName;
        private final String foreignSimpleClassName;
        private final String foreignTableName;
        private final String relationType;

        public Relation(Field f) {
            variableName = f.getName();
            relationType = f.getAnnotation(ManyToOne.class) != null ? "ManyToOne" : f.getAnnotation(OneToMany.class) != null ? "OneToMany" : f.getAnnotation(OneToOne.class) != null ? "OneToOne" : "ManyToMany";
            Class<?> foreign = f.getType();
            if (Collection.class.isAssignableFrom(f.getType()) && f.getGenericType() instanceof ParameterizedType pt) {
                this.foreignClassName = pt.getActualTypeArguments()[0].getTypeName();
                foreign = pt.getActualTypeArguments()[0] instanceof Class<?> element ? element : null;
            } else {
                this.foreignClassName = f.getType().getName();
            }
            // asked here rather than by every reader: the table a class is stored in is @Table's business, and a wildcard element type has no class to ask
            foreignSimpleClassName = foreign == null ? foreignClassName.substring(foreignClassName.lastIndexOf('.') + 1) : foreign.getSimpleName();
            foreignTableName = foreign == null ? "" : DatabaseObject.getTableName(foreign);
        }

        public String getVariableName() {
            return variableName;
        }
        public String getForeignClassName() {
            return foreignClassName;
        }
        /** The class name a reader shows, without its package - the other end's answer to {@link IEntityInfo#getSimpleClassName}. */
        public String getForeignSimpleClassName() {
            return foreignSimpleClassName;
        }
        /** The table the other end is stored in, so a key can be followed without a reader holding the registry to look the class up in. */
        public String getForeignTableName() {
            return foreignTableName;
        }
        public String getRelationType() {
            return relationType;
        }
    }


    public static List<IEntityInfo> sortByDependency(Collection<IEntityInfo> entities) {
        Map<String, IEntityInfo> byName = new HashMap<>();
        entities.forEach(e -> byName.put(e.getClassName(), e));

        Map<String, Set<String>> deps = new HashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();
        for (IEntityInfo e : entities) {
            Set<String> d = new HashSet<>();
            for (IEntityInfo.Relation r : e.getRelations())
                if (("ManyToOne".equals(r.getRelationType()) || "OneToOne".equals(r.getRelationType()))
                        && byName.containsKey(r.getForeignClassName())
                        && !r.getForeignClassName().equals(e.getClassName())) // ignore self-reference
                    d.add(r.getForeignClassName());
            deps.put(e.getClassName(), d);
            d.forEach(t -> dependents.computeIfAbsent(t, k -> new HashSet<>()).add(e.getClassName()));
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        deps.forEach((name, d) -> { if (d.isEmpty()) ready.add(name); });

        List<IEntityInfo> sorted = new ArrayList<>(entities.size());
        while (!ready.isEmpty()) {
            String name = ready.poll();
            sorted.add(byName.get(name));
            for (String dep : dependents.getOrDefault(name, Set.of())) {
                Set<String> remaining = deps.get(dep);
                remaining.remove(name);
                if (remaining.isEmpty()) ready.add(dep);
            }
        }

        if (sorted.size() < entities.size()) entities.stream().filter(e -> !sorted.contains(e)).forEach(sorted::add);

        return sorted;
    }
}
