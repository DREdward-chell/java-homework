package com.edwards.orm.repository;

import com.edwards.orm.annotation.validation.Validator;
import com.edwards.orm.annotation.validation.Violation;
import com.edwards.orm.core.EntityMetadata;
import com.edwards.orm.core.EntityMetadata.FieldMeta;
import com.edwards.orm.core.OrmException;
import com.edwards.orm.core.ValidationException;
import com.edwards.orm.database.Database;
import com.edwards.orm.di.DatabaseRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class EntityManager {
    private final Database database;

    public EntityManager() {
        this(DatabaseRegistry.shadowInject());
    }

    public EntityManager(Database database) {
        this.database = database;
    }

    public void createTable(Class<?> clazz) {
        EntityMetadata m = EntityMetadata.of(clazz);
        StringJoiner cols = new StringJoiner(", ");
        cols.add(m.id().columnName() + " BIGINT AUTO_INCREMENT PRIMARY KEY");
        for (FieldMeta c : m.columns()) {
            cols.add(c.columnName() + " " + c.sqlType() + (c.nullable() ? "" : " NOT NULL"));
        }
        database.executeDDL("CREATE TABLE IF NOT EXISTS " + m.tableName() + " (" + cols + ")");
    }

    public Long save(Object entity) {
        EntityMetadata m = EntityMetadata.of(entity.getClass());
        validate(entity);
        m.checkNotNullable(entity);
        Long id = database.executeInsert(insertSql(m), m.columnValues(entity));
        m.writeId(entity, id);
        return id;
    }

    public void saveAll(List<?> entities) {
        if (entities == null || entities.isEmpty()) return;
        Class<?> type = entities.get(0).getClass();
        for (Object e : entities) {
            if (e == null) throw new OrmException("saveAll does not accept null elements");
            if (e.getClass() != type) throw new OrmException("saveAll requires elements of the same class");
        }
        EntityMetadata m = EntityMetadata.of(type);

        List<Violation> all = new ArrayList<>();
        for (Object e : entities) all.addAll(validateCollect(e));
        if (!all.isEmpty()) throw new ValidationException(all);

        List<Object[]> batch = new ArrayList<>(entities.size());
        for (Object e : entities) {
            m.checkNotNullable(e);
            batch.add(m.columnValues(e));
        }
        List<Long> ids = database.executeBatchInsert(insertSql(m), batch);
        for (int i = 0; i < ids.size() && i < entities.size(); i++) m.writeId(entities.get(i), ids.get(i));
    }

    public <T> Optional<T> findById(Class<T> clazz, Long id) {
        EntityMetadata m = EntityMetadata.of(clazz);
        String sql = "SELECT " + selectList(m) + " FROM " + m.tableName()
                + " WHERE " + m.id().columnName() + " = ?";
        List<Map<String, Object>> rows = database.executeQuery(sql, new Object[]{id});
        return rows.isEmpty() ? Optional.empty() : Optional.of(m.fromRow(rows.get(0)));
    }

    public <T> List<T> findAll(Class<T> clazz) {
        EntityMetadata m = EntityMetadata.of(clazz);
        String sql = "SELECT " + selectList(m) + " FROM " + m.tableName();
        return mapRows(database.executeQuery(sql, new Object[0]), m);
    }

    public <T> List<T> findAllWhere(Class<T> clazz, String fieldName, Object value) {
        EntityMetadata m = EntityMetadata.of(clazz);
        FieldMeta f = m.byFieldName(fieldName);
        if (!m.isCompatibleValue(fieldName, value)) {
            throw new OrmException("Value of type " + value.getClass().getName()
                    + " is not compatible with field " + fieldName);
        }
        String sql = "SELECT " + selectList(m) + " FROM " + m.tableName()
                + " WHERE " + f.columnName() + " = ?";
        return mapRows(database.executeQuery(sql, new Object[]{EntityMetadata.toSql(value)}), m);
    }

    public <T> Optional<T> findOneWhere(Class<T> clazz, String fieldName, Object value) {
        List<T> rows = findAllWhere(clazz, fieldName, value);
        if (rows.size() > 1) throw new OrmException("findOneWhere returned " + rows.size() + " rows");
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long count(Class<?> clazz) {
        EntityMetadata m = EntityMetadata.of(clazz);
        List<Map<String, Object>> rows = database.executeQuery(
                "SELECT COUNT(*) AS c FROM " + m.tableName(), new Object[0]);
        return ((Number) rows.getFirst().values().iterator().next()).longValue();
    }

    public boolean existsById(Class<?> clazz, Long id) {
        EntityMetadata m = EntityMetadata.of(clazz);
        String sql = "SELECT 1 FROM " + m.tableName()
                + " WHERE " + m.id().columnName() + " = ? LIMIT 1";
        return !database.executeQuery(sql, new Object[]{id}).isEmpty();
    }

    public int update(Object entity) {
        EntityMetadata m = EntityMetadata.of(entity.getClass());
        validate(entity);
        m.checkNotNullable(entity);
        StringJoiner sets = new StringJoiner(", ");
        for (FieldMeta c : m.columns()) sets.add(c.columnName() + " = ?");
        String sql = "UPDATE " + m.tableName() + " SET " + sets
                + " WHERE " + m.id().columnName() + " = ?";
        Object[] cols = m.columnValues(entity);
        Object[] params = new Object[cols.length + 1];
        System.arraycopy(cols, 0, params, 0, cols.length);
        params[cols.length] = m.readId(entity);
        return database.executeUpdate(sql, params);
    }

    public int delete(Object entity) {
        EntityMetadata m = EntityMetadata.of(entity.getClass());
        return deleteById(entity.getClass(), (Long) m.readId(entity));
    }

    public int deleteById(Class<?> clazz, Long id) {
        EntityMetadata m = EntityMetadata.of(clazz);
        String sql = "DELETE FROM " + m.tableName() + " WHERE " + m.id().columnName() + " = ?";
        return database.executeUpdate(sql, new Object[]{id});
    }

    private static String insertSql(EntityMetadata m) {
        StringJoiner cols = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        for (FieldMeta c : m.columns()) { cols.add(c.columnName()); placeholders.add("?"); }
        return "INSERT INTO " + m.tableName() + " (" + cols + ") VALUES (" + placeholders + ")";
    }

    private static String selectList(EntityMetadata m) {
        StringJoiner sj = new StringJoiner(", ");
        for (FieldMeta f : m.allFields()) sj.add(f.columnName());
        return sj.toString();
    }

    private static <T> List<T> mapRows(List<Map<String, Object>> rows, EntityMetadata m) {
        List<T> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) out.add(m.fromRow(r));
        return out;
    }

    private static void validate(Object entity) {
        List<Violation> v = validateCollect(entity);
        if (!v.isEmpty()) throw new ValidationException(v);
    }

    private static List<Violation> validateCollect(Object entity) {
        try {
            return Validator.validateObject(entity);
        } catch (ReflectiveOperationException e) {
            throw new OrmException("Validation failed reflectively", e);
        }
    }
}
