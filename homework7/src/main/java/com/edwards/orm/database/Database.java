package com.edwards.orm.database;

import com.edwards.orm.core.OrmException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Database implements AutoCloseable {
    private Connection connection = null;
    DatabaseConfiguration databaseConfiguration;

    public Database(DatabaseConfiguration databaseConfiguration) {
        this.databaseConfiguration = databaseConfiguration;
    }

    public Connection getConnection() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(
                        databaseConfiguration.url(),
                        databaseConfiguration.username(),
                        databaseConfiguration.password()
                );
                connection.setAutoCommit(false);
            }
            return connection;
        } catch (SQLException e) {
            throw new OrmException("Can't open connection", e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public void executeDDL(String sql) {
        Connection conn = getConnection();
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new OrmException("DDL failed: " + sql, e);
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object[] params) {
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return readRows(rs);
            }
        } catch (SQLException e) {
            throw new OrmException("query failed: " + sql, e);
        }
    }

    public int executeUpdate(String sql, Object[] params) {
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            int affected = ps.executeUpdate();
            conn.commit();
            return affected;
        } catch (SQLException e) {
            rollback(conn);
            throw new OrmException("update failed: " + sql, e);
        }
    }

    public Long executeInsert(String sql, Object[] params) {
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new OrmException("No generated key returned");
                long id = keys.getLong(1);
                conn.commit();
                return id;
            }
        } catch (SQLException e) {
            rollback(conn);
            throw new OrmException("insert failed: " + sql, e);
        }
    }

    public List<Long> executeBatchInsert(String sql, List<Object[]> batch) {
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Object[] row : batch) {
                bind(ps, row);
                ps.addBatch();
            }
            ps.executeBatch();
            List<Long> ids = new ArrayList<>(batch.size());
            try (ResultSet keys = ps.getGeneratedKeys()) {
                while (keys.next()) ids.add(keys.getLong(1));
            }
            conn.commit();
            return ids;
        } catch (SQLException e) {
            rollback(conn);
            throw new OrmException("batch insert failed: " + sql, e);
        }
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (int i = 1; i <= cols; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
            rows.add(row);
        }
        return rows;
    }

    private static void rollback(Connection conn) {
        try { conn.rollback(); } catch (SQLException ignored) {}
    }
}
