package com.edwards.orm.core;

import com.edwards.orm.database.Database;
import com.edwards.orm.di.DatabaseRegistry;
import com.edwards.orm.repository.Repository;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RepositoryFactory {
    private static volatile RepositoryFactory instance;

    private final Map<Class<?>, Object> repositoryMap;
    private final Database database;

    private RepositoryFactory() {
        repositoryMap = new ConcurrentHashMap<>();
        database = DatabaseRegistry.shadowInject();
    }

    private static RepositoryFactory getInstance() {
        if (instance == null) {
            synchronized (RepositoryFactory.class) {
                if (instance == null) {
                    instance = new RepositoryFactory();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Repository<?>> T getRepository(Class<T> clazz) {
        RepositoryFactory factory = getInstance();
        return (T) factory.repositoryMap.computeIfAbsent(clazz, key ->
                Proxy.newProxyInstance(
                        key.getClassLoader(),
                        new Class[]{key},
                        new RepositoryProxy(factory.database, key)
                )
        );
    }
}
