package com.mybatis.test.core;

import com.mybatis.test.core.ReflectionTester.TestResult;
import com.mybatis.test.core.XMLMethodParser.MethodInfo;
import com.mybatis.test.report.ReportGenerator;
import org.apache.ibatis.session.SqlSession;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

/**
 * 主入口。提示输入关键词（如 RCS-4286），扫描 Git 提交，
 * 找出被修改的 MyBatis Mapper XML，解析方法，通过反射测试，生成报告。
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入 Git 仓库路径（默认当前目录）: ");
        String repoPath = scanner.nextLine().trim();
        if (repoPath.isEmpty()) {
            repoPath = System.getProperty("user.dir");
        }

        System.out.print("请输入关键词（如 RCS-4286）: ");
        String keyword = scanner.nextLine().trim();
        if (keyword.isEmpty()) {
            System.out.println("关键词不能为空。退出。");
            return;
        }

        System.out.println("\n[主程序] 开始 MyBatis Mapper 测试，关键词: " + keyword);
        System.out.println("[主程序] Git 仓库: " + repoPath);

        try {
            List<TestResult> allResults = run(repoPath, keyword);
            ReportGenerator.printConsoleReport(allResults, keyword);
            ReportGenerator.generateHtmlReport(allResults, keyword);
        } catch (Exception e) {
            log.error("[主程序] 测试执行失败", e);
            System.out.println("\n[主程序] 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 主执行流程。
     */
    public static List<TestResult> run(String repoPath, String keyword) throws Exception {
        List<TestResult> allResults = new ArrayList<>();

        // 步骤1：查找包含关键词的提交
        log.info("=== 步骤1：扫描 Git 提交，查找包含关键词 '{}' 的提交 ===", keyword);
        List<RevCommit> commits = GitCommitDetector.findCommitsByKeyword(repoPath, keyword);
        if (commits.isEmpty()) {
            log.warn("未找到包含关键词 '{}' 的提交，无需测试。", keyword);
            return allResults;
        }

        // 步骤2：收集所有匹配提交中修改过的 Mapper XML
        log.info("=== 步骤2：收集被修改的 Mapper XML 文件 ===");
        Set<String> modifiedMappers = new HashSet<>();
        try (Repository repo = GitCommitDetector.openRepository(repoPath)) {
            for (RevCommit commit : commits) {
                List<String> xmls = GitCommitDetector.getModifiedMapperXmls(repo, commit);
                for (String xml : xmls) {
                    String fileName = new File(xml).getName();
                    String mapperName = fileName.replace(".xml", "");
                    modifiedMappers.add(mapperName);
                    log.info("  被修改: {} (提交: {})", xml, commit.getName().substring(0, 8));
                }
            }
        }

        if (modifiedMappers.isEmpty()) {
            log.warn("在包含关键词的提交中未找到被修改的 Mapper XML 文件。");
            return allResults;
        }
        log.info("共 {} 个不重复的 Mapper 待测试", modifiedMappers.size());

        // 步骤3：对每个 Mapper 解析方法并通过反射测试
        log.info("=== 步骤3：通过反射测试被修改的 Mapper ===");
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            for (String mapperName : modifiedMappers) {
                String resourcePath = "mapper/" + mapperName + ".xml";
                List<MethodInfo> methods = XMLMethodParser.parseFromResource(resourcePath);

                if (methods.isEmpty()) {
                    log.warn("无法从 {} 解析方法", resourcePath);
                    continue;
                }

                log.info("  从 {} 解析出 {} 个方法: {}", mapperName, methods.size(),
                        methods.stream().map(m -> m.id).collect(java.util.stream.Collectors.toList()));

                String entityClass = POEntityRegistry.getEntityClass(mapperName);
                if (entityClass == null) {
                    log.warn("  未找到 {} 的实体映射，跳过", mapperName);
                    continue;
                }
                log.info("  实体类: {}", entityClass);

                List<TestResult> results = ReflectionTester.testMapper(session, mapperName, methods);
                allResults.addAll(results);
            }

            // 提交测试事务
            session.commit();
        }

        log.info("=== 测试完成，共 {} 条结果 ===", allResults.size());
        return allResults;
    }
}
