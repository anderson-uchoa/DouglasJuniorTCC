package br.edu.utfpr.cm.minerador.services.matrix;

import br.edu.utfpr.cm.JGitMinerWeb.dao.BichoDAO;
import br.edu.utfpr.cm.JGitMinerWeb.dao.BichoFileDAO;
import br.edu.utfpr.cm.JGitMinerWeb.dao.GenericBichoDAO;
import br.edu.utfpr.cm.JGitMinerWeb.model.matrix.EntityMatrix;
import br.edu.utfpr.cm.JGitMinerWeb.model.matrix.EntityMatrixNode;
import br.edu.utfpr.cm.JGitMinerWeb.util.OutLog;
import br.edu.utfpr.cm.JGitMinerWeb.util.Util;
import br.edu.utfpr.cm.minerador.services.matrix.model.FilePair;
import br.edu.utfpr.cm.minerador.services.matrix.model.FilePairAprioriOutput;
import br.edu.utfpr.cm.minerador.services.matrix.model.FilePath;
import br.edu.utfpr.cm.minerador.services.matrix.model.Issue;
import br.edu.utfpr.cm.minerador.services.matrix.model.Project;
import br.edu.utfpr.cm.minerador.services.matrix.model.ProjectVersion;
import br.edu.utfpr.cm.minerador.services.matrix.model.ProjectVersionSummary;
import br.edu.utfpr.cm.minerador.services.matrix.model.Version;
import br.edu.utfpr.cm.minerador.services.metric.model.Commit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Rodrigo Kuroda
 */
public class BichoProjectsSummaryPerVersionServices extends AbstractBichoMatrixServices {

    public BichoProjectsSummaryPerVersionServices() {
        super(null, null);
    }

    public BichoProjectsSummaryPerVersionServices(GenericBichoDAO dao, OutLog out) {
        super(dao, out);
    }

    public BichoProjectsSummaryPerVersionServices(GenericBichoDAO dao, String repository, List<EntityMatrix> matricesToSave, Map<Object, Object> params, OutLog out) {
        super(dao, repository, matricesToSave, params, out);
    }

    private Integer getMaxFilesPerCommit() {
        return Util.stringToInteger(params.get("maxFilesPerCommit") + "");
    }

    private Integer getMinFilesPerCommit() {
        return Util.stringToInteger(params.get("minFilesPerCommit") + "");
    }

    private boolean isOnlyFixed() {
        return "true".equalsIgnoreCase(params.get("mergedOnly") + "");
    }

    public List<String> getFilesToIgnore() {
        return getStringLinesParam("filesToIgnore", true, false);
    }

    public List<String> getFilesToConsiders() {
        return getStringLinesParam("filesToConsiders", true, false);
    }

    public String getVersion() {
        return getStringParam("version");
    }

    public String getFutureVersion() {
        return getStringParam("futureVersion");
    }

    @Override
    public void run() {
        System.out.println(params);

        if (getRepository() == null) {
            throw new IllegalArgumentException("Parameter repository must be informed.");
        }

        log("\n --------------- "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
                + "\n --------------- \n");

        BichoDAO bichoDAO = new BichoDAO(dao, getRepository(), getMaxFilesPerCommit());
        BichoFileDAO bichoFileDAO = new BichoFileDAO(dao, getRepository(), getMaxFilesPerCommit());

        final List<String> fixVersionOrdered = bichoDAO.selectFixVersionOrdered();
        final Set<ProjectVersionSummary> projectSummary = new LinkedHashSet<>();

        for (String version : fixVersionOrdered) {

            ProjectVersion projectVersion = new ProjectVersion(new Project(getRepository()), new Version(version));
            ProjectVersionSummary summaryVersion = new ProjectVersionSummary(projectVersion);

            Map<FilePair, FilePairAprioriOutput> pairFiles = new HashMap<>();

            out.printLog("Maximum files per commit: " + getMaxFilesPerCommit());
            out.printLog("Minimum files per commit: " + getMinFilesPerCommit());

            Set<FilePath> allFiles = new HashSet<>();
            Set<FilePath> allTestJavaFiles = new HashSet<>();
            Set<FilePath> allJavaFiles = new HashSet<>();
            Set<FilePath> allXmlFiles = new HashSet<>();
            Set<FilePath> allFilteredFiles = new HashSet<>();

            Set<Commit> allCommits = new HashSet<>();
            Set<Integer> allConsideredCommits = new HashSet<>();
            Set<Integer> allDefectIssues = new HashSet<>();

            // select a issue/pullrequest commenters
            Map<Issue, List<Commit>> issuesConsideredCommits = bichoDAO.selectIssuesAndType(version);
            Set<Issue> allIssues = issuesConsideredCommits.keySet();

            out.printLog("Issues (filtered): " + issuesConsideredCommits.size());

            int count = 1;

            // combina em pares todos os arquivos commitados em uma issue
            final int totalIssues = issuesConsideredCommits.size();
            int progressFilePairing = 0;
            for (Map.Entry<Issue, List<Commit>> entrySet : issuesConsideredCommits.entrySet()) {
                if (++progressFilePairing % 100 == 0
                        || progressFilePairing == totalIssues) {
                    System.out.println(progressFilePairing + "/" + totalIssues);
                }
                Issue issue = entrySet.getKey();
                List<Commit> commits = entrySet.getValue();

                out.printLog("Issue #" + issue);
                out.printLog(count++ + " of the " + issuesConsideredCommits.size());

                out.printLog(commits.size() + " commits references the issue");
                allCommits.addAll(commits);

                List<FilePath> commitedFiles
                        = filterAndAggregateAllFileOfIssue(commits, bichoFileDAO, allFiles, allTestJavaFiles, allFilteredFiles, allJavaFiles, allXmlFiles);

                // empty
                if (commitedFiles.isEmpty()) {
                    out.printLog("No file commited for issue #" + issue);
                    continue;
                } else if (commitedFiles.size() == 1) {
                    out.printLog("One file only commited for issue #" + issue);
                    continue;

                }
                out.printLog("Number of files commited and related with issue: " + commitedFiles.size());

                pairFiles(commitedFiles, pairFiles, issue, allDefectIssues, allConsideredCommits);

                summaryVersion.addIssue(issue);
                summaryVersion.addCommit(commits);
                summaryVersion.addFilePair(pairFiles.keySet());
            }

            projectSummary.add(summaryVersion);

            out.printLog("\n\n" + getRepository() + " " + version + "\n"
                    + "Number of files (JAVA and XML): " + allFiles.size() + "\n"
                    + "Number of files (JAVA): " + allJavaFiles.size() + "\n"
                    + "Number of files (XML): " + allXmlFiles.size() + "\n"
                    + "Number of ignored files !.java, !.xml, *Test.java: " + allFilteredFiles.size() + "\n"
                    + "Number of ignored files *Test.java: " + allTestJavaFiles.size() + "\n"
                    + "Number of file pairs: " + summaryVersion.filePairsSize() + "\n"
                    + "Number of issues: " + allIssues.size() + "\n"
                    + "Number of considered issues: " + summaryVersion.issuesSize() + "\n"
                    + "Number of commits: " + allCommits.size() + "\n"
                    + "Number of considered commits: " + allConsideredCommits.size() + "\n"
                    + "Number of defect issues: " + allDefectIssues.size() + "\n"
            );

            log("\n\n" + getRepository() + " " + version + "\n"
                    + "Number of files (JAVA and XML): " + allFiles.size() + "\n"
                    + "Number of files (JAVA): " + allJavaFiles.size() + "\n"
                    + "Number of files (XML): " + allXmlFiles.size() + "\n"
                    + "Number of ignored files !.java, !.xml, *Test.java: " + allFilteredFiles.size() + "\n"
                    + "Number of ignored files *Test.java: " + allTestJavaFiles.size() + "\n"
                    + "Number of file pairs: " + summaryVersion.filePairsSize() + "\n"
                    + "Number of issues: " + allIssues.size() + "\n"
                    + "Number of considered issues: " + summaryVersion.issuesSize() + "\n"
                    + "Number of commits: " + allCommits.size() + "\n"
                    + "Number of considered commits: " + allConsideredCommits.size() + "\n"
                    + "Number of defect issues: " + allDefectIssues.size() + "\n"
            );

        }

        EntityMatrix matrix = new EntityMatrix();
//        matrix.setNodes(objectsToNodes(pairFileList, FilePairAprioriOutput.getToStringHeaderAprioriOnly()));
        matrix.setNodes(objectsToNodes(projectSummary, "Project;Version;Issues;Commits;Pairs of File"));
        matricesToSave.add(matrix);
    }

    protected static List<EntityMatrixNode> objectsToNodes(final Map<FilePair, Integer[]> list, final List<String> versions) {
        List<EntityMatrixNode> nodes = new ArrayList<>();
        StringBuilder header = new StringBuilder("file1;file2");
        for (String version : versions) {
            header.append(";").append(version);
        }
        nodes.add(new EntityMatrixNode(header.toString()));

        for (Map.Entry<FilePair, Integer[]> entrySet : list.entrySet()) {
            FilePair filePair = entrySet.getKey();
            Integer[] value = entrySet.getValue();
            StringBuilder row = new StringBuilder(filePair.toString());
            for (Integer ocurrencesQuantity : value) {
                row.append(ocurrencesQuantity == null ? 0 : ocurrencesQuantity).append(";");
            }
            nodes.add(new EntityMatrixNode(row.toString()));
        }
        return nodes;
    }

    private List<FilePath> filterAndAggregateAllFileOfIssue(List<Commit> commits, BichoFileDAO bichoFileDAO, Set<FilePath> allFiles, Set<FilePath> allTestJavaFiles, Set<FilePath> allFilteredFiles, Set<FilePath> allJavaFiles, Set<FilePath> allXmlFiles) {
        // monta os pares com os arquivos de todos os commits da issue
        List<FilePath> commitedFiles = new ArrayList<>();
        for (Commit commit : commits) {

            // select name of commited files
            List<FilePath> files = bichoFileDAO.selectFilesByCommitId(commit.getId());

            allFiles.addAll(files);

            out.printLog(files.size() + " files in commit #" + commit.getId());
            for (FilePath file : files) {
                if (file.getFilePath().endsWith("Test.java")
                        || file.getFilePath().toLowerCase().endsWith("_test.java")) {
                    allTestJavaFiles.add(file);
                    allFilteredFiles.add(file);
                } else if (!file.getFilePath().endsWith(".java")
                        && !file.getFilePath().endsWith(".xml")) {
                    allFilteredFiles.add(file);
                } else {
                    if (file.getFilePath().endsWith(".java")) {
                        allJavaFiles.add(file);
                    } else if (file.getFilePath().endsWith(".xml")) {
                        allXmlFiles.add(file);
                    }
                    commitedFiles.add(file);
                }
            }
        }
        return commitedFiles;
    }
}
