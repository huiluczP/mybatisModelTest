package com.mybatis.test.report;

import com.mybatis.test.core.ReflectionTester.TestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 生成控制台和 HTML 格式的测试报告。
 */
public class ReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 在控制台打印汇总报告。
     */
    public static void printConsoleReport(List<TestResult> results, String keyword) {
        String separator = repeatStr("=", 80);
        String dash = repeatStr("-", 80);
        System.out.println("\n" + separator);
        System.out.println("  MyBatis Mapper 测试报告");
        System.out.println("  Keyword: " + keyword);
        System.out.println("  生成时间: " + LocalDateTime.now().format(FMT));
        System.out.println(separator);

        int total = results.size();
        long passed = results.stream().filter(r -> r.success).count();
        long failed = total - passed;

        System.out.println("\n  Total: " + total + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println(dash);

        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            String status = r.success ? "PASS" : "FAIL";
            System.out.printf("  [%d] [%s] %s.%s%n",
                    i + 1, status, r.mapperName, r.methodInfo.id);
            System.out.printf("      实体类: %s%n", r.entityClass);
            System.out.printf("      耗时: %d ms%n", r.durationMs);
            if (r.testEntity != null) {
                System.out.printf("      测试数据: %s%n", r.testEntity);
            }
            System.out.printf("      信息: %s%n", r.message);
            System.out.println();
        }

        System.out.println(dash);
        System.out.println("  汇总: " + (failed == 0 ? "全部通过" : failed + " 个测试失败"));
        System.out.println(separator + "\n");
    }

    /**
     * 生成 HTML 报告文件。
     */
    public static String generateHtmlReport(List<TestResult> results, String keyword) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "test-report-" + timestamp + ".html";
        Path reportPath = Paths.get("reports", fileName);
        Files.createDirectories(reportPath.getParent());

        int total = results.size();
        long passed = results.stream().filter(r -> r.success).count();
        long failed = total - passed;

        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            String statusColor = r.success ? "#28a745" : "#dc3545";
            String statusText = r.success ? "PASS" : "FAIL";
            String testData = r.testEntity != null
                    ? escapeHtml(r.testEntity.toString())
                    : "N/A";
            rows.append("<tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td>").append(escapeHtml(r.mapperName)).append("</td>")
                    .append("<td>").append(escapeHtml(r.methodInfo.id)).append("</td>")
                    .append("<td>").append(escapeHtml(r.entityClass)).append("</td>")
                    .append("<td>").append(escapeHtml(r.methodInfo.type)).append("</td>")
                    .append("<td>").append(testData).append("</td>")
                    .append("<td style='color:").append(statusColor).append("'><b>")
                    .append(statusText).append("</b></td>")
                    .append("<td>").append(r.durationMs).append(" ms</td>")
                    .append("<td>").append(escapeHtml(r.message)).append("</td>")
                    .append("</tr>\n");
        }

        String html = buildHtml(keyword, total, passed, failed, rows.toString());
        Files.writeString(reportPath, html);
        String absPath = reportPath.toAbsolutePath().toString();
        System.out.println("[报告] HTML 报告已生成: " + absPath);
        return absPath;
    }

    private static String buildHtml(String keyword, int total, long passed, long failed, String rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<title>MyBatis Mapper 测试报告</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;margin:20px;background:#f5f5f5}");
        sb.append(".container{max-width:1400px;margin:0 auto;background:#fff;padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,.1)}");
        sb.append("h1{color:#333;border-bottom:2px solid #007bff;padding-bottom:10px}");
        sb.append(".summary{display:flex;gap:20px;margin:20px 0}");
        sb.append(".summary-card{flex:1;padding:15px;border-radius:6px;text-align:center;color:#fff}");
        sb.append(".total{background:#6c757d}.passed{background:#28a745}.failed{background:#dc3545}");
        sb.append(".summary-card h3{margin:0;font-size:32px}.summary-card p{margin:5px 0 0;opacity:.9}");
        sb.append("table{width:100%;border-collapse:collapse;margin-top:20px}");
        sb.append("th,td{padding:10px 12px;text-align:left;border-bottom:1px solid #dee2e6;font-size:13px}");
        sb.append("th{background:#007bff;color:#fff;position:sticky;top:0}");
        sb.append("tr:hover{background:#f8f9fa}");
        sb.append(".meta{color:#666;font-size:14px;margin-bottom:10px}");
        sb.append("</style></head><body><div class=\"container\">");
        sb.append("<h1>MyBatis Mapper 测试报告</h1>");
        sb.append("<p class=\"meta\">Keyword: <b>").append(escapeHtml(keyword)).append("</b> | 生成时间: ");
        sb.append(LocalDateTime.now().format(FMT)).append("</p>");
        sb.append("<div class=\"summary\">");
        sb.append("<div class=\"summary-card total\"><h3>").append(total).append("</h3><p>Total</p></div>");
        sb.append("<div class=\"summary-card passed\"><h3>").append(passed).append("</h3><p>Passed</p></div>");
        sb.append("<div class=\"summary-card failed\"><h3>").append(failed).append("</h3><p>Failed</p></div>");
        sb.append("</div>");
        sb.append("<table><tr>");
        sb.append("<th>#</th><th>Mapper</th><th>方法</th><th>实体</th>");
        sb.append("<th>类型</th><th>测试数据</th><th>状态</th><th>耗时</th><th>信息</th>");
        sb.append("</tr>");
        sb.append(rows);
        sb.append("</table></div></body></html>");
        return sb.toString();
    }

    private static String repeatStr(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
