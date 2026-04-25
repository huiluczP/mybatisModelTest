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
 * 使用反射根据字段类型生成测试数据填充 PO 实体，
 * 然后调用 MyBatis Mapper 方法并记录结果。
 */
public class ReflectionTester {

    private static final Logger log = LoggerFactory.getLogger(ReflectionTester.class);
    private static final String DEFAULT_ENTITY_PACKAGE = "com.mybatis.test.entity";

    /**
     * 单次方法测试调用的结果。
     */
    public static class TestResult {
        public final String mapperName;
        public final MethodInfo methodInfo;
        public final String entityClass;
        public final Object testEntity;
        public final Object[] testArgs;
        public final boolean success;
        public final String message;
        public final long durationMs;

        public TestResult(String mapperName, MethodInfo methodInfo, String entityClass,
                          Object testEntity, boolean success, String message, long durationMs) {
            this.mapperName = mapperName;
            this.methodInfo = methodInfo;
            this.entityClass = entityClass;
            this.testEntity = testEntity;
            this.testArgs = null;
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
        }

        public TestResult(String mapperName, MethodInfo methodInfo, String entityClass,
                          Object testEntity, Object[] testArgs, boolean success, String message, long durationMs) {
            this.mapperName = mapperName;
            this.methodInfo = methodInfo;
            this.entityClass = entityClass;
            this.testEntity = testEntity;
            this.testArgs = testArgs;
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
        }
    }

    /**
     * 为方法解析实体类。
     * 优先级：XML parameterType(全限定名) > XML parameterType(别名) > POEntityRegistry 推断。
     */
    private static String resolveEntityClass(String xmlParameterType, String mapperName) {
        // 1. XML parameterType - 全限定类名
        if (xmlParameterType != null && xmlParameterType.contains(".")) {
            try {
                Class<?> clazz = Class.forName(xmlParameterType);
                // 跳过非实体类型如 List、String 等
                if (!isSimpleType(clazz) && !List.class.isAssignableFrom(clazz)
                        && !Map.class.isAssignableFrom(clazz)) {
                    return xmlParameterType;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 2. XML parameterType - 作为别名处理，尝试默认实体包
        if (xmlParameterType != null && !xmlParameterType.isEmpty()) {
            String candidate = DEFAULT_ENTITY_PACKAGE + "." + capitalize(xmlParameterType);
            try {
                Class.forName(candidate);
                return candidate;
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 3. 兜底：基于名称推断 (ProductInfoMapper -> ProductInfo)
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
     * 判断类型是否为简单/基本类型（非 PO 实体）。
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
     * 为单个方法参数生成测试参数值。
     */
    private static Object generateTestArgument(Class<?> paramType, String entityClassName) {
        // List<PO> -> 创建包含一个测试实体的列表
        if (List.class.isAssignableFrom(paramType)) {
            if (entityClassName != null) {
                try {
                    Object entity = createTestEntity(entityClassName);
                    List<Object> list = new ArrayList<>();
                    list.add(entity);
                    return list;
                } catch (Exception e) {
                    log.warn("[反射] 为 List 参数创建实体失败，使用空列表: {}", e.getMessage());
                    return new ArrayList<>();
                }
            }
            return new ArrayList<>();
        }

        // Map 参数（如 @Param 动态参数）-> 返回空 Map
        if (Map.class.isAssignableFrom(paramType)) {
            return new HashMap<String, Object>();
        }

        // 参数类型与实体类匹配 -> 创建填充的实体
        if (entityClassName != null) {
            try {
                Class<?> entityClass = Class.forName(entityClassName);
                if (entityClass.isAssignableFrom(paramType) || paramType.isAssignableFrom(entityClass)) {
                    return createTestEntity(entityClassName);
                }
            } catch (Exception e) {
                // 类未找到或实体创建失败，回退到简单类型
            }
        }

        // 简单/基本类型 -> 按类型生成
        return generateTestValue("param", paramType);
    }

    /**
     * 根据字段类型生成测试数据填充 PO 实体。
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

    private static final Random RANDOM = new Random();

    /**
     * 根据字段类型生成测试值。
     */
    private static Object generateTestValue(String fieldName, Class<?> type) {
        if (type == String.class) {
            return "[TEST]_" + fieldName + "_" + RANDOM.nextInt(100000);
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
     * 调用单个 Mapper 方法，从方法签名构建正确的参数。
     */
    public static TestResult testMethod(SqlSession session, String mapperName,
                                         MethodInfo methodInfo, String fallbackEntityClass) {
        long start = 0;
        try {
            // 确定 Mapper 接口
            String mapperClassName = "com.mybatis.test.mapper." + mapperName;
            Class<?> mapperClass = Class.forName(mapperClassName);
            Object mapper = session.getMapper(mapperClass);

            // 按方法名查找
            Method targetMethod = null;
            for (Method m : mapperClass.getDeclaredMethods()) {
                if (m.getName().equals(methodInfo.id)) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod == null) {
                return new TestResult(mapperName, methodInfo, fallbackEntityClass, null,
                        false, "方法 '" + methodInfo.id + "' 不存在于 " + mapperClassName, 0);
            }

            // 解析实体类：XML parameterType > 名称推断
            String entityClassName = resolveEntityClass(methodInfo.parameterType, mapperName);
            if (entityClassName == null) {
                entityClassName = fallbackEntityClass;
            }

            // 根据实际方法参数类型构建参数
            Class<?>[] paramTypes = targetMethod.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                args[i] = generateTestArgument(paramTypes[i], entityClassName);
                log.debug("[反射] 参数[{}] 类型={} -> 值={}", i, paramTypes[i].getSimpleName(), args[i]);
            }

            Object firstArg = args.length > 0 ? args[0] : null;
            log.info("[反射] 方法 {}.{} 有 {} 个参数，实体={}",
                    mapperName, methodInfo.id, paramTypes.length, entityClassName);

            // 调用方法
            start = System.currentTimeMillis();
            Object result = targetMethod.invoke(mapper, args);
            long duration = System.currentTimeMillis() - start;

            log.info("[反射] 调用 {}.{} => 结果={}", mapperName, methodInfo.id, result);

            return new TestResult(mapperName, methodInfo, entityClassName, firstArg, args,
                    true, "结果: " + result, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.error("[反射] {}.{} 失败: {}", mapperName, methodInfo.id, errorMsg, e);
            return new TestResult(mapperName, methodInfo, fallbackEntityClass, null,
                    false, "错误: " + errorMsg, duration);
        }
    }

    /**
     * 测试指定 Mapper XML 的所有方法。
     */
    public static List<TestResult> testMapper(SqlSession session, String mapperName,
                                               List<MethodInfo> methods) {
        String entityClassName = POEntityRegistry.getEntityClass(mapperName);
        if (entityClassName == null) {
            log.warn("[反射] 未找到 Mapper {} 对应的实体类", mapperName);
            return Collections.emptyList();
        }

        log.info("[反射] 测试 Mapper: {}，实体: {}", mapperName, entityClassName);

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
