package com.mybatis.test.core;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a MyBatis Mapper XML file and extracts method names and their parameter types.
 */
public class XMLMethodParser {

    private static final Logger log = LoggerFactory.getLogger(XMLMethodParser.class);

    /**
     * Represents a parsed SQL method from a mapper XML.
     */
    public static class MethodInfo {
        public final String id;
        public final String type; // insert/update/select/delete
        public final String parameterType; // fully qualified class name, or null

        public MethodInfo(String id, String type, String parameterType) {
            this.id = id;
            this.type = type;
            this.parameterType = parameterType;
        }

        @Override
        public String toString() {
            return "MethodInfo{id='" + id + "', type='" + type + "', paramType='" + parameterType + "'}";
        }
    }

    /**
     * Parse a mapper XML file and return all SQL methods.
     */
    public static List<MethodInfo> parseMethods(File xmlFile) {
        List<MethodInfo> methods = new ArrayList<>();
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(xmlFile);
            Element root = doc.getRootElement();

            for (String tag : new String[]{"insert", "update", "select", "delete"}) {
                for (Element elem : root.elements(tag)) {
                    String id = elem.attributeValue("id");
                    String paramType = elem.attributeValue("parameterType");
                    if (id != null) {
                        methods.add(new MethodInfo(id, tag, paramType));
                    }
                }
            }
        } catch (DocumentException e) {
            log.error("[XML] Failed to parse: {}", xmlFile.getAbsolutePath(), e);
        }
        return methods;
    }

    /**
     * Parse methods from a classpath resource path.
     */
    public static List<MethodInfo> parseFromResource(String resourcePath) {
        File file = new File(resourcePath);
        if (!file.exists()) {
            // try classpath
            var url = XMLMethodParser.class.getClassLoader().getResource(resourcePath);
            if (url != null) {
                file = new File(url.getFile());
            }
        }
        return file.exists() ? parseMethods(file) : java.util.Collections.emptyList();
    }
}
