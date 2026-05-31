package com.edwards.orm.core;


import com.edwards.orm.annotation.Query;
import com.edwards.orm.annotation.Table;
import com.edwards.orm.database.Database;
import com.edwards.orm.repository.Repository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RepositoryProxy implements InvocationHandler {
    private final Database database;
    private final String tableName;

    public RepositoryProxy(Database database, Class<?> repoInterface) {
        this.database = database;
        this.tableName = resolveTableName(repoInterface);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        Query query = method.getAnnotation(Query.class);
        if (query == null) {
            throw new OrmException("@Query is not present on " + method);
        }

        QueryBuilder.ParsedQuery parsed = QueryBuilder.queryFromTemplate(
                query.value(),
                args != null ? args : new Object[0],
                method.getParameters(),
                tableName
        );

        if (isSelect(parsed.sql())) {
            return ResultMapper.map(method, database.executeQuery(parsed.sql(), parsed.params()));
        }
        return ResultMapper.mapUpdate(method, database.executeUpdate(parsed.sql(), parsed.params()));
    }

    private static String resolveTableName(Class<?> repoInterface) {
        Class<?> entity = findEntityType(repoInterface);
        if (entity != null) {
            return EntityMetadata.of(entity).tableName();
        }
        Table fallback = repoInterface.getAnnotation(Table.class);
        if (fallback != null) {
            return fallback.name();
        }
        throw new OrmException(repoInterface.getName()
                + " must extend Repository<EntityType> so the proxy can resolve the table name");
    }

    private static Class<?> findEntityType(Class<?> repoInterface) {
        for (Type t : repoInterface.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt && pt.getRawType() == Repository.class) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> c) return c;
            }
        }
        for (Class<?> parent : repoInterface.getInterfaces()) {
            Class<?> found = findEntityType(parent);
            if (found != null) return found;
        }
        return null;
    }

    private static boolean isSelect(String sql) {
        int i = 0;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) i++;
        return sql.regionMatches(true, i, "SELECT", 0, 6);
    }
}
