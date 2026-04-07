package com.mybatis.test.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 从 entity-mapping.properties 读取实体类覆盖配置，
 * 兜底自动推断：{MapperName}Mapper.xml → com.mybatis.test.entity.{MapperName}
 */
public class POEntityRegistry {

    private static final String DEFAULT_ENTITY_PACKAGE = "com.mybatis.test.entity";
    private static final Map<String, String> overrides = new HashMap<>();

    static {
        loadOverrides();
    }

    private static void loadOverrides() {
        try {
            var props = new java.util.Properties();
            var is = POEntityRegistry.class.getClassLoader()
                    .getResourceAsStream("entity-mapping.properties");
            if (is != null) {
                props.load(is);
                for (String key : props.stringPropertyNames()) {
                    overrides.put(key.trim(), props.getProperty(key).trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[POEntityRegistry] Failed to load entity-mapping.properties: " + e.getMessage());
        }
    }

    /**
     * 将 Mapper 文件名解析为实体类全限定名。
     *
     * @param mapperName 如 "ProductInfoMapper"
     * @return 类名或 null
     */
    public static String getEntityClass(String mapperName) {
        if (mapperName == null || mapperName.isEmpty()) return null;

        // 1. check override config
        String fromOverride = overrides.get(mapperName);
        if (fromOverride != null) return fromOverride;

        // 2. auto-infer: strip "Mapper" suffix, prepend default package
        if (mapperName.endsWith("Mapper")) {
            String entityName = mapperName.substring(0, mapperName.length() - "Mapper".length());
            if (!entityName.isEmpty()) {
                return DEFAULT_ENTITY_PACKAGE + "." + entityName;
            }
        }
        return null;
    }

    /**
     * 检查 Mapper 是否有可解析的实体。
     */
    public static boolean hasEntity(String mapperName) {
        return getEntityClass(mapperName) != null;
    }

    /**
     * 运行时注册或覆盖映射。
     */
    public static void register(String mapperName, String entityClassName) {
        overrides.put(mapperName, entityClassName);
    }
}
