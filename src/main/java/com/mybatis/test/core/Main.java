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
 * Main entry point. Prompts for a keyword (e.g., RCS-4286), scans git commits,
 * finds modified MyBatis mapper XMLs, parses methods, tests via reflection, generates report.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter git repo path (default: current directory): ");
        String repoPath = scanner.nextLine().trim();
        if (repoPath.isEmpty()) {
            repoPath = System.getProperty("user.dir");
        }

        System.out.print("Enter keyword (e.g., RCS-4286): ");
        String keyword = scanner.nextLine().trim();
        if (keyword.isEmpty()) {
            System.out.println("Keyword cannot be empty. Exiting.");
            return;
        }

        System.out.println("\n[Main] Starting MyBatis Mapper Test for keyword: " + keyword);
        System.out.println("[Main] Git repo: " + repoPath);

        try {
            List<TestResult> allResults = run(repoPath, keyword);
            ReportGenerator.printConsoleReport(allResults, keyword);
            ReportGenerator.generateHtmlReport(allResults, keyword);
        } catch (Exception e) {
            log.error("[Main] Test execution failed", e);
            System.out.println("\n[Main] FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main execution pipeline.
     */
    public static List<TestResult> run(String repoPath, String keyword) throws Exception {
        List<TestResult> allResults = new ArrayList<>();

        // Step 1: Find commits containing the keyword
        log.info("=== Step 1: Scanning git commits for keyword '{}' ===", keyword);
        List<RevCommit> commits = GitCommitDetector.findCommitsByKeyword(repoPath, keyword);
        if (commits.isEmpty()) {
            log.warn("No commits found containing '{}'. Nothing to test.", keyword);
            return allResults;
        }

        // Step 2: Collect modified mapper XML files across all matching commits
        log.info("=== Step 2: Collecting modified Mapper XML files ===");
        Set<String> modifiedMappers = new HashSet<>();
        try (Repository repo = GitCommitDetector.openRepository(repoPath)) {
            for (RevCommit commit : commits) {
                List<String> xmls = GitCommitDetector.getModifiedMapperXmls(repo, commit);
                for (String xml : xmls) {
                    // Extract mapper name: e.g., "src/main/resources/mapper/ProductInfoMapper.xml" -> "ProductInfoMapper"
                    String fileName = new File(xml).getName();
                    String mapperName = fileName.replace(".xml", "");
                    modifiedMappers.add(mapperName);
                    log.info("  Modified: {} (commit: {})", xml, commit.getName().substring(0, 8));
                }
            }
        }

        if (modifiedMappers.isEmpty()) {
            log.warn("No modified MyBatis Mapper XMLs found in commits containing '{}'.", keyword);
            return allResults;
        }
        log.info("Total unique mappers to test: {}", modifiedMappers);

        // Step 3: For each modified mapper, parse methods and test via reflection
        log.info("=== Step 3: Testing modified mappers via reflection ===");
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            for (String mapperName : modifiedMappers) {
                // Find the XML file on classpath
                String resourcePath = "mapper/" + mapperName + ".xml";
                List<MethodInfo> methods = XMLMethodParser.parseFromResource(resourcePath);

                if (methods.isEmpty()) {
                    log.warn("No methods parsed from {}", resourcePath);
                    continue;
                }

                log.info("  Parsed {} methods from {}: {}", methods.size(), mapperName,
                        methods.stream().map(m -> m.id).collect(java.util.stream.Collectors.toList()));

                // Check if entity class is registered
                String entityClass = POEntityRegistry.getEntityClass(mapperName);
                if (entityClass == null) {
                    log.warn("  No entity class mapping for {}. Skipping.", mapperName);
                    continue;
                }
                log.info("  Entity class: {}", entityClass);

                // Test each method
                List<TestResult> results = ReflectionTester.testMapper(session, mapperName, methods);
                allResults.addAll(results);
            }

            // Commit the test transaction (we opened with autoCommit=false for safety)
            session.commit();
        }

        log.info("=== Test complete. Total results: {} ===", allResults.size());
        return allResults;
    }
}
