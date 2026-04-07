package com.mybatis.test.core;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans git commit history for commits whose message contains a keyword,
 * and collects the paths of modified MyBatis XML mapper files.
 */
public class GitCommitDetector {

    private static final Logger log = LoggerFactory.getLogger(GitCommitDetector.class);

    /**
     * Find all commits whose message contains the keyword, returning the list of RevCommit.
     */
    public static List<RevCommit> findCommitsByKeyword(String repoPath, String keyword) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.findGitDir(new File(repoPath)).build();

        List<RevCommit> result = new ArrayList<>();
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().all().call();
            for (RevCommit commit : commits) {
                String message = commit.getFullMessage();
                if (message.contains(keyword)) {
                    result.add(commit);
                    log.info("[Git] Found commit: {} | {}", commit.getName().substring(0, 8), message.trim());
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to scan git log in: " + repoPath, e);
        }

        log.info("[Git] Found {} commits containing keyword '{}'", result.size(), keyword);
        return result;
    }

    /**
     * Get all modified XML file paths from a commit using JGit DiffFormatter.
     */
    public static Set<String> getModifiedFiles(Repository repo, RevCommit commit) throws Exception {
        Set<String> xmlFiles = new HashSet<>();
        if (commit.getParentCount() == 0) {
            try (TreeWalk walk = new TreeWalk(repo)) {
                walk.addTree(commit.getTree());
                walk.setRecursive(true);
                while (walk.next()) {
                    String path = walk.getPathString();
                    if (path.endsWith(".xml")) xmlFiles.add(path);
                }
            }
            return xmlFiles;
        }
        ByteArrayOutputStream nullStream = new ByteArrayOutputStream();
        try (DiffFormatter diffFormatter = new DiffFormatter(nullStream)) {
            diffFormatter.setRepository(repo);
            for (int i = 0; i < commit.getParentCount(); i++) {
                List<DiffEntry> diffs = diffFormatter.scan(commit.getParent(i), commit);
                for (DiffEntry diff : diffs) {
                    String newPath = diff.getNewPath();
                    if (!newPath.equals(DiffEntry.DEV_NULL) && newPath.endsWith(".xml")) {
                        xmlFiles.add(newPath);
                    }
                }
            }
        }
        return xmlFiles;
    }

    /**
     * Get all modified MyBatis XML mapper files from a commit.
     * Filters to files ending with "Mapper.xml".
     */
    public static List<String> getModifiedMapperXmls(Repository repo, RevCommit commit) throws Exception {
        Set<String> modifiedFiles = getModifiedFiles(repo, commit);
        return modifiedFiles.stream()
                .filter(path -> path.endsWith("Mapper.xml"))
                .collect(Collectors.toList());
    }

    /**
     * Open a git repository from path.
     */
    public static Repository openRepository(String repoPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.findGitDir(new File(repoPath)).build();
    }
}
