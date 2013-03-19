/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.utfpr.cm.JGitMinerWeb.services.matriz;

import br.edu.utfpr.cm.JGitMinerWeb.dao.GenericDao;
import br.edu.utfpr.cm.JGitMinerWeb.pojo.matriz.EntityMatrizNode;
import br.edu.utfpr.cm.JGitMinerWeb.pojo.miner.EntityRepository;
import br.edu.utfpr.cm.JGitMinerWeb.services.matriz.auxiliary.AuxUserUserFile;
import br.edu.utfpr.cm.JGitMinerWeb.util.JsfUtil;
import br.edu.utfpr.cm.JGitMinerWeb.util.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author douglas
 */
public class UserCommentedSameFileServices extends AbstractMatrizServices {

    public UserCommentedSameFileServices(GenericDao dao) {
        super(dao);
    }

    public UserCommentedSameFileServices(GenericDao dao, EntityRepository repository, Map params) {
        super(dao, repository, params);
    }

    public Long getBeginPullRequestNumber() {
        String idPull = params.get("beginPull") + "";
        return Util.tratarStringParaLong(idPull);
    }

    public Long getEndPullRequestNumber() {
        String idPull = params.get("endPull") + "";
        return Util.tratarStringParaLong(idPull);
    }

    private List<String> getFilesName() {
        List<String> filesName = new ArrayList<>();
        for (String fileName : (params.get("filesName") + "").split("\n")) {
            fileName = fileName.trim();
            if (!fileName.isEmpty()) {
                filesName.add(fileName);
            }
        }
        return filesName;
    }

    private Integer getMilestoneNumber() {
        String mileNumber = params.get("milestoneNumber") + "";
        return Util.tratarStringParaInt(mileNumber);
    }

    private Boolean isIncludeGenericComments() {
        return Boolean.parseBoolean(params.get("includeGenericsComments") + "");
    }

    @Override
    public void run() {
        System.out.println(params);

        if (getRepository() == null) {
            throw new IllegalArgumentException("Parâmetro Repository não pode ser nulo.");
        }

        List<AuxUserUserFile> result;

        if (getMilestoneNumber() > 0) {
            result = getByMilestoneNumber();
        } else {
            result = getByDate();
        }

        List<EntityMatrizNode> nodes = new ArrayList<>();
        for (AuxUserUserFile aux : result) {
            nodes.add(new EntityMatrizNode(aux.getUser() + ";" + aux.getUser2(), aux.getFileName()));
        }

        System.out.println("Nodes: " + nodes.size());

        setNodes(nodes);
    }

    @Override
    public String convertToCSV(Collection<EntityMatrizNode> nodes) {
        StringBuilder sb = new StringBuilder("user;user2;file;width\n");
        for (EntityMatrizNode node : nodes) {
            sb.append(node.getFrom()).append(JsfUtil.TOKEN_SEPARATOR);
            sb.append(node.getTo()).append(JsfUtil.TOKEN_SEPARATOR);
            sb.append(Util.tratarDoubleParaString(node.getWeight(), 0)).append("\n");
        }
        return sb.toString();
    }

    private List<AuxUserUserFile> getByMilestoneNumber() {
        String jpql = "SELECT DISTINCT NEW " + AuxUserUserFile.class.getName() + "(cm.user.login, cm2.user.login, cm.pathCommitComment) "
                + "FROM "
                + "EntityPullRequest p JOIN p.repositoryCommits rc JOIN rc.comments cm, "
                + "EntityPullRequest p2 JOIN p2.repositoryCommits rc2 JOIN rc2.comments cm2 "
                + "WHERE "
                + "p.repository = :repo AND "
                + "p2.repository = :repo AND "
                + "p.issue.milestone.number = :milestoneNumber AND "
                + "p.issue.milestone.number = p2.issue.milestone.number AND "
                + (!getFilesName().isEmpty() ? "cm.pathCommitComment IN :filesName AND " : "")
                + "cm.pathCommitComment = cm2.pathCommitComment AND "
                + "cm.user <> cm2.user ";

        String[] bdParams = new String[]{
            "repo",
            !getFilesName().isEmpty() ? "filesName" : "#none#",
            "milestoneNumber"
        };
        Object[] bdObjects = new Object[]{
            getRepository(),
            getFilesName(),
            getMilestoneNumber()
        };

        System.out.println(jpql);

        List<AuxUserUserFile> result = new ArrayList<>();
        List<AuxUserUserFile> temp;

        temp = dao.selectWithParams(jpql,
                bdParams,
                bdObjects);

        System.out.println("Result temp: " + temp.size());

        addAllTempInResult(temp, result);

        if (isIncludeGenericComments()) {
            jpql = "SELECT DISTINCT NEW " + AuxUserUserFile.class.getName() + "(cm.user.login, cm2.user.login, f.filename) "
                    + "FROM "
                    + "EntityPullRequest p JOIN p.repositoryCommits rc JOIN rc.comments cm JOIN rc.files f, "
                    + "EntityPullRequest p2 JOIN p2.repositoryCommits rc2 JOIN rc2.comments cm2 JOIN rc2.files f2 "
                    + "WHERE "
                    + "p.repository = :repo "
                    + "AND p2.repository = :repo "
                    + "AND p.issue.milestone.number = :milestoneNumber "
                    + "AND p.issue.milestone.number = p2.issue.milestone.number "
                    + "AND cm.pathCommitComment IS NULL "
                    + "AND cm2.pathCommitComment IS NULL "
                    + (!getFilesName().isEmpty() ? "AND f.filename IN :filesName " : "")
                    + "AND f.filename = f2.filename "
                    + "AND cm.user <> cm2.user ";

            System.out.println(jpql);

            temp = dao.selectWithParams(jpql,
                    bdParams,
                    bdObjects);

            System.out.println("Result temp2: " + temp.size());

            addAllTempInResult(temp, result);
        }

        System.out.println("Result: " + result.size());
        return result;
    }

    private List<AuxUserUserFile> getByDate() {
        String jpql = "SELECT DISTINCT NEW " + AuxUserUserFile.class.getName() + "(cm.user.login, cm2.user.login, cm.pathCommitComment) "
                + "FROM "
                + "EntityCommitComment cm JOIN cm.repositoryCommit rc, "
                + "EntityCommitComment cm2 JOIN cm2.repositoryCommit rc2 "
                + "WHERE "
                + "rc.repository = :repo AND "
                + "rc2.repository = :repo AND "
                + "rc.commit.committer.dateCommitUser BETWEEN :beginDate AND :endDate AND "
                + "rc2.commit.committer.dateCommitUser BETWEEN :beginDate AND :endDate AND "
                + (!getFilesName().isEmpty() ? "cm.pathCommitComment IN :filesName AND " : "")
                + "cm.pathCommitComment = cm2.pathCommitComment AND "
                + "cm.user <> cm2.user ";

        String[] bdParams = new String[]{
            "repo",
            "beginDate",
            "endDate",
            !getFilesName().isEmpty() ? "filesName" : "#none#"
        };
        Object[] bdObjects = new Object[]{
            getRepository(),
            getBeginDate(),
            getEndDate(),
            getFilesName()
        };

        System.out.println(jpql);

        List<AuxUserUserFile> result = new ArrayList<>();
        List<AuxUserUserFile> temp;

        temp = dao.selectWithParams(jpql,
                bdParams,
                bdObjects);

        System.out.println("Result temp: " + temp.size());

        addAllTempInResult(temp, result);

        if (isIncludeGenericComments()) {
            jpql = "SELECT DISTINCT NEW " + AuxUserUserFile.class.getName() + "(cm.user.login, cm2.user.login, f.filename) "
                    + "FROM "
                    + "EntityPullRequest p JOIN p.repositoryCommits rc JOIN rc.comments cm JOIN rc.files f, "
                    + "EntityPullRequest p2 JOIN p2.repositoryCommits rc2 JOIN rc2.comments cm2 JOIN rc2.files f2 "
                    + "WHERE "
                    + "p.repository = :repo "
                    + "AND p2.repository = :repo "
                    + "AND rc.commit.committer.dateCommitUser BETWEEN :beginDate AND :endDate "
                    + "AND rc2.commit.committer.dateCommitUser BETWEEN :beginDate AND :endDate "
                    + "AND cm.pathCommitComment IS NULL "
                    + "AND cm2.pathCommitComment IS NULL "
                    + (!getFilesName().isEmpty() ? "AND f.filename IN :filesName " : "")
                    + "AND f.filename = f2.filename "
                    + "AND cm.user <> cm2.user ";

            System.out.println(jpql);

            temp = dao.selectWithParams(jpql,
                    bdParams,
                    bdObjects);

            System.out.println("Result temp2: " + temp.size());

            addAllTempInResult(temp, result);
        }

        System.out.println("Result: " + result.size());
        return result;
    }

    private void addAllTempInResult(List<AuxUserUserFile> temp, List<AuxUserUserFile> result) {
        for (AuxUserUserFile aux : temp) {
            if (!result.contains(aux)) {
                result.add(aux);
            }
        }
    }
}