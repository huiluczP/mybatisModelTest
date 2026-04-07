package com.mybatis.test.core;

import com.mybatis.test.core.XMLMethodParser.MethodInfo;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Uses reflection to instantiate PO entities with test data based on field types,
 * then invokes MyBatis mapper methods and records results.
 */
public class ReflectionTester {

    private static final Logger log = LoggerFactory.getLogger(ReflectionTester.class);
    private static final String DEFAULT_ENTITY_PACKAGE = "com.mybatis.test.entity";

    /**
     * Result of a single method test invocation.
     */
    public static class TestResult {
        public final String mapperName;
        public final MethodInfo methodInfo;
        public final String entityClass;
        public final Object testEntity;
        public final boolean success;
        public final String message;
        public final long durationMs;

        public TestResult(String mapperName, MethodInfo methodInfo, String entityClass,
                          Object testEntity, boolean success, String message, long durationMs) {
            this.mapperName = mapperName;
            this.methodInfo = methodInfo;
            this.entityClass = entityClass;
            this.testEntity = testEntity;
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
        }
    }

    /**
     * Resolve the entity class for a method.
     * Priority: XML parameterType (FQN) > XML parameterType (alias) > POEntityRegistry inference.
     */
    private static String resolveEntityClass(String xmlParameterType, String mapperName) {
        // 1. XML parameterType - fully qualified class name
        if (xmlParameterType != null && xmlParameterType.contains(".")) {
            try {
                Class<?> clazz = Class.forName(xmlParameterType);
                // Skip non-entity types like java.util.List, java.lang.String, etc.
                if (!isSimpleType(clazz) && !List.class.isAssignableFrom(clazz)
                        && !Map.class.isAssignableFrom(clazz)) {
                    return xmlParameterType;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 2. XML parameterType - treat as alias, try default entity package
        if (xmlParameterType != null && !xmlParameterType.isEmpty()) {
            String candidate = DEFAULT_ENTITY_PACKAGE + "." + capitalize(xmlParameterType);
            try {
                Class.forName(candidate);
                return candidate;
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 3. Fallback: name-based inference (ProductInfoMapper -> ProductInfo)
        String inferred = POEntityRegistry.getEntityClass(mapperName);
        if (inferred != null) {
            try {
                Class.forName(inferred);
                return inferred;
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    /**
     * Check if a type is a simple/primitive/wrapper type (not a PO entity).
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == Boolean.class || type == boolean.class
                || type == Character.class || type == char.class
                || type == BigDecimal.class
                || type == Date.class
                || type == LocalDateTime.class;
    }

    /**
     * Generate test argument value for a single method parameter.
     */
    private static Object generateTestArgument(Class<?> paramType, String entityClassName) {
        // List<PO> -> create a list containing one test entity
        if (List.class.isAssignableFrom(paramType)) {
            if (entityClassName != null) {
                try {
                    Object entity = createTestEntity(entityClassName);
                    List<Object> list = new ArrayList<>();
                    list.add(entity);
                    return list;
                } catch (Exception e) {
                    log.warn("[Reflection] Failed to create entity for List param, using empty list: {}", e.getMessage());
                    return new ArrayList<>();
                }
            }
            return new ArrayList<>();
        }

        // Map param (e.g., for dynamic @Param) -> populate with test values
        if (Map.class.isAssignableFrom(paramType)) {
            return new HashMap<String, Object>();
        }

        // If param type matches or is compatible with the entity class -> create filled entity
        if (entityClassName != null) {
            try {
                Class<?> entityClass = Class.forName(entityClassName);
                if (entityClass.isAssignableFrom(paramType) || paramType.isAssignableFrom(entityClass)) {
                    return createTestEntity(entityClassName);
                }
            } catch (Exception e) {
                // Class not found or entity creation failed, fall through to simple type
            }
        }

        // Simple/primitive type -> generate based on type
        return generateTestValue("param", paramType);
    }

    /**
     * Populate a PO entity with test data based on field types.
     */
    public static Object createTestEntity(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Object entity = clazz.getDeclaredConstructor().newInstance();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object testValue = generateTestValue(field.getName(), field.getType());
            field.set(entity, testValue);
        }
        return entity;
    }

    /**
     * Generate a test value for a given field type.
     */
    private static Object generateTestValue(String fieldName, Class<?> type) {
        if (type == String.class) {
            return "[TEST]_" + fieldName;
        } else if (type == Long.class || type == long.class) {
            return System.currentTimeMillis() % 1_000_000L;
        } else if (type == Integer.class || type == int.class) {
            return new Random().nextInt(1000) + 1;
        } else if (type == Double.class || type == double.class) {
            return new Random().nextDouble() * 1000;
        } else if (type == Float.class || type == float.class) {
            return (float) (new Random().nextDouble() * 1000);
        } else if (type == BigDecimal.class) {
            return BigDecimal.valueOf(new Random().nextInt(10000), 2);
        } else if (type == Boolean.class || type == boolean.class) {
            return true;
        } else if (type == LocalDateTime.class) {
            return LocalDateTime.now();
        } else if (type == Date.class) {
            return new Date();
        } else if (type == Short.class || type == short.class) {
            return (short) 1;
        } else if (type == Byte.class || type == byte.class) {
            return (byte) 1;
        } else if (type == Character.class || type == char.class) {
            return 'X';
        }
        return null;
    }

    /**
     * Invoke a single mapper method, building the correct arguments from method signature.
     */
    public static TestResult testMethod(SqlSession session, String mapperName,
                                         MethodInfo methodInfo, String fallbackEntityClass) {
        try {
            // Determine the mapper interface
            String mapperClassName = "com.mybatis.test.mapper." + mapperName;
            Class<?> mapperClass = Class.forName(mapperClassName);
            Object mapper = session.getMapper(mapperClass);

            // Find the method by id
            Method targetMethod = null;
            for (Method m : mapperClass.getDeclaredMethods()) {
                if (m.getName().equals(methodInfo.id)) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod == null) {
                return new TestResult(mapperName, methodInfo, fallbackEntityClass, null,
                        false, "Method '" + methodInfo.id + "' not found in " + mapperClassName, 0);
            }

            // Resolve entity class: XML parameterType > name inference
            String entityClassName = resolveEntityClass(methodInfo.parameterType, mapperName);
            if (entityClassName == null) {
                entityClassName = fallbackEntityClass;
            }

            // Build arguments based on actual method parameter types
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                args[i] = generateTestArgument(paramTypes[i], entityClassName);
                log.debug("[Reflection] Param[{}] type={} -> value={}", i, paramTypes[i].getSimpleName(), args[i]);
            }

            Object firstArg = args.length > 0 ? args[0] : null;
            log.info("[Reflection] Method {}.{} has {} param(s), entity={}",
                    mapperName, methodInfo.id, paramTypes.length, entityClassName);

            // Invoke the method
            long start = System.currentTimeMillis();
            Object result = targetMethod.invoke(mapper, args);
            long duration = System.currentTimeMillis() - start;

            log.info("[Reflection] Invoked {}.{} => result={}", mapperName, methodInfo.id, result);

            return new TestResult(mapperName, methodInfo, entityClassName, firstArg,
                    true, "Result: " + result, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis();
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.error("[Reflection] Failed {}.{}: {}", mapperName, methodInfo.id, errorMsg, e);
            return new TestResult(mapperName, methodInfo, fallbackEntityClass, null,
                    false, "ERROR: " + errorMsg, duration);
        }
    }

    /**
     * Test all methods for a given mapper XML.
     */
    public static List<TestResult> testMapper(SqlSession session, String mapperName,
                                               List<MethodInfo> methods) {
        String entityClassName = POEntityRegistry.getEntityClass(mapperName);
        if (entityClassName == null) {
            log.warn("[Reflection] No entity class found for mapper: {}", mapperName);
            return Collections.emptyList();
        }

        log.info("[Reflection] Testing mapper: {} with entity: {}", mapperName, entityClassName);

        List<TestResult> results = new ArrayList<>();
        for (MethodInfo mi : methods) {
            TestResult tr = testMethod(session, mapperName, mi, entityClassName);
            results.add(tr);
        }
        return results;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
