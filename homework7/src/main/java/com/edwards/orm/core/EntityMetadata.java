package com.edwards.orm.core;

import com.edwards.orm.annotation.Column;
import com.edwards.orm.annotation.Id;
import com.edwards.orm.annotation.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityMetadata {
    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    private final Class<?> entityClass;
    private final String tableName;
    private final FieldMeta id;
    private final List<FieldMeta> columns;
    private final Map<String, FieldMeta> byFieldName;
    private final Constructor<?> noArgCtor;

    public record FieldMeta(Field field, String columnName, String sqlType, boolean nullable, boolean isId) {}

    private EntityMetadata(Class<?> entityClass, String tableName, FieldMeta id,
                           List<FieldMeta> columns, Map<String, FieldMeta> byFieldName,
                           Constructor<?> noArgCtor) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.id = id;
        this.columns = columns;
        this.byFieldName = byFieldName;
        this.noArgCtor = noArgCtor;
    }

    public static EntityMetadata of(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, EntityMetadata::parse);
    }

    public Class<?> entityClass() { return entityClass; }
    public String tableName() { return tableName; }
    public FieldMeta id() { return id; }
    public List<FieldMeta> columns() { return columns; }

    public List<FieldMeta> allFields() {
        List<FieldMeta> all = new ArrayList<>(columns.size() + 1);
        all.add(id);
        all.addAll(columns);
        return all;
    }

    public FieldMeta byFieldName(String fieldName) {
        FieldMeta m = byFieldName.get(fieldName);
        if (m == null) throw new OrmException("No field '" + fieldName + "' on " + entityClass.getName());
        return m;
    }

    public Object[] columnValues(Object entity) {
        Object[] values = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) values[i] = toSql(read(columns.get(i).field(), entity));
        return values;
    }

    public Object readId(Object entity) {
        return read(id.field(), entity);
    }

    public void writeId(Object entity, Long value) {
        write(id.field(), entity, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromRow(Map<String, Object> row) {
        Object instance = newInstance();
        for (FieldMeta f : allFields()) write(f.field(), instance, fromSql(row.get(f.columnName()), f.field().getType()));
        return (T) instance;
    }

    public void checkNotNullable(Object entity) {
        for (FieldMeta c : columns) {
            if (!c.nullable() && read(c.field(), entity) == null) {
                throw new OrmException("Field " + c.field().getName() + " (column " + c.columnName() + ") must not be null");
            }
        }
    }

    public boolean isCompatibleValue(String fieldName, Object value) {
        if (value == null) return true;
        return boxed(byFieldName(fieldName).field().getType()).isInstance(value);
    }

    public static Object toSql(Object value) {
        return value instanceof LocalDate d ? Date.valueOf(d) : value;
    }

    private static Object fromSql(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == LocalDate.class && value instanceof Date d) return d.toLocalDate();
        return value;
    }

    private Object newInstance() {
        try {
            return noArgCtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new OrmException("Cannot instantiate " + entityClass.getName() + " (need no-arg constructor)", e);
        }
    }

    private static Object read(Field f, Object owner) {
        try {
            return f.get(owner);
        } catch (IllegalAccessException e) {
            throw new OrmException("Cannot read field " + f.getName(), e);
        }
    }

    private static void write(Field f, Object owner, Object value) {
        try {
            f.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new OrmException("Cannot write field " + f.getName(), e);
        }
    }

    private static Class<?> boxed(Class<?> t) {
        if (t == int.class)     return Integer.class;
        if (t == long.class)    return Long.class;
        if (t == double.class)  return Double.class;
        if (t == boolean.class) return Boolean.class;
        return t;
    }

    private static EntityMetadata parse(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) throw new OrmException("@Table is missing on " + clazz.getName());
        String tableName = table.name().isEmpty() ? clazz.getSimpleName().toLowerCase() : table.name();

        FieldMeta id = null;
        List<FieldMeta> columns = new ArrayList<>();
        Map<String, FieldMeta> byFieldName = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            Id idAnn = field.getAnnotation(Id.class);
            Column colAnn = field.getAnnotation(Column.class);
            if (idAnn == null && colAnn == null) continue;
            field.setAccessible(true);

            if (idAnn != null) {
                if (field.getType() != Long.class && field.getType() != long.class) {
                    throw new OrmException("@Id field must be Long/long: " + field);
                }
                String name = (colAnn != null && !colAnn.name().isEmpty()) ? colAnn.name() : field.getName();
                FieldMeta meta = new FieldMeta(field, name, sqlTypeFor(field.getType()), true, true);
                if (id != null) throw new OrmException("Multiple @Id fields on " + clazz.getName());
                id = meta;
                byFieldName.put(field.getName(), meta);
            } else {
                String name = colAnn.name().isEmpty() ? field.getName() : colAnn.name();
                FieldMeta meta = new FieldMeta(field, name, sqlTypeFor(field.getType()), colAnn.nullable(), false);
                columns.add(meta);
                byFieldName.put(field.getName(), meta);
            }
        }

        if (id == null) throw new OrmException("No @Id field on " + clazz.getName());

        Constructor<?> ctor;
        try {
            ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new OrmException("Entity " + clazz.getName() + " needs a no-arg constructor", e);
        }

        return new EntityMetadata(clazz, tableName, id, List.copyOf(columns), Map.copyOf(byFieldName), ctor);
    }

    private static String sqlTypeFor(Class<?> type) {
        if (type == Long.class    || type == long.class)    return "BIGINT";
        if (type == Integer.class || type == int.class)     return "INT";
        if (type == Double.class  || type == double.class)  return "DOUBLE";
        if (type == Boolean.class || type == boolean.class) return "BOOLEAN";
        if (type == String.class)                            return "VARCHAR(255)";
        if (type == LocalDate.class)                         return "DATE";
        throw new OrmException("Unsupported field type: " + type.getName());
    }
}
