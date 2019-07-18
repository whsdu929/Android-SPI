package com.xudadong.spi.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceProvider {

    private static boolean isInitialized = false;

    public static synchronized void init() {
        if (!isInitialized) {
            isInitialized = true;
            register();
        }
    }

    private static synchronized void register() {
    }

    public static <T> List<T> getProviders(Class<T> clazz) {
        if (!isInitialized) {
            init();
        }

        List<T> providers = new ArrayList<>();
        String classCanonicalName = clazz.getCanonicalName();
        if (classCanonicalName == null) {
            return providers;
        }

        Set<Object> providersSet = ProvidersPool.mProvidersCache.get(classCanonicalName);
        if (providersSet == null) {
            providersSet = new HashSet<>();
            Set<String> classNameSet = ProvidersPool.mProviders.get(classCanonicalName);
            if (classNameSet == null) {
                classNameSet = new HashSet<>();
            }

            if (classNameSet.size() > 0) {
                for (String className : classNameSet) {
                    try {
                        Class<?> cls = Class.forName(className);
                        providersSet.add(cls.getConstructor().newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            ProvidersPool.mProvidersCache.put(classCanonicalName, providersSet);
        }

        if (providersSet.size() > 0) {
            for (Object object : providersSet) {
                providers.add((T) object);
            }
        }
        return providers;
    }

    public static synchronized void destroy() {
        if (isInitialized) {
            isInitialized = false;
            ProvidersPool.mProviders.clear();
            ProvidersPool.mProvidersCache.clear();
        }
    }
}
