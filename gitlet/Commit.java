package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Refs.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.lang.System.exit;

/**
 * Represents a gitlet commit object.
 *  does at a high level.
 *
 * @author ethanyi
 */
public class Commit implements Serializable {
    /**
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /* The message of this Commit. */
    private String message;
    /* The parent of commit, null if it's the first commit */
    private String directParent;
    private String otherParent;
    /* the timestamp of commit*/
    private Date timestamp;
    /* the contents of commit files*/
    private HashMap<String, String> blobMap = new HashMap<>();


    public Commit(String message, Date timestamp, String directparent,
                  String blobFileName, String blobHashName) {
        this.message = message;
        this.timestamp = timestamp;
        this.directParent = directparent;
        if (blobFileName == null || blobFileName.isEmpty()) {
            this.blobMap = new HashMap<>();
        } else {
            this.blobMap.put(blobFileName, blobHashName);
        }
    }

    public Commit(Commit directparent) {
        this.message = directparent.message;
        this.timestamp = directparent.timestamp;
        this.directParent = directparent.directParent;
        this.blobMap = directparent.blobMap;
    }




    /**
     * To save commit into files in COMMIT_FOLDER, persists the status of object.
     */
    public void saveCommit() {
        // get the uid of this
        String hashname = this.getHashName();

        // write obj to files
        File commitFile = new File(COMMIT_FOLDER, hashname);
        writeObject(commitFile, this);
    }


    /**
     * @param blobName blob的hashname
     */
    public void addBlob(String fileName, String blobName) {
        this.blobMap.put(fileName, blobName);
    }

    public void removeBlob(String fileName) {
        this.blobMap.remove(fileName);
    }


    public String getHashName() {
        return sha1(this.message, dateToTimeStamp(this.timestamp), this.directParent);
    }

    public void setDirectParent(String directParent) {
        this.directParent = directParent;
    }

    public String getDirectParent() {
        return directParent;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public HashMap<String, String> getBlobMap() {
        return blobMap;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String getOtherParent() {
        return otherParent;
    }

    public void setOtherParent(String otherParent) {
        this.otherParent = otherParent;
    }
    /* ======================== 以上为getter和setter ======================*/

    /**
     * 用于获取HEAD指针指向的Commit对象
     *
     * @return
     */
    public static Commit getHeadCommit() {
        /* 获取HEAD指针,这个指针指向目前最新的commit */
        String headContent = readContentsAsString(HEAD_POINT);
        String headHashName = headContent.split(":")[1];
        File commitFile = join(COMMIT_FOLDER, headHashName);
        /* 获取commit文件 */
        Commit commit = readObject(commitFile, Commit.class);

        return commit;

    }

    /**
     * 用于获取branches文件夹中分支文件指向的Commit对象
     *
     * @return
     */
    public static Commit getBranchHeadCommit(String branchName, String errorMsg) {


        File brancheFile = join(HEAD_DIR, branchName);
        if (!brancheFile.exists()) {
            System.out.println(errorMsg);
            exit(0);
        }

        /* 获取HEAD指针,这个指针指向目前最新的commit */
        String headHashName = readContentsAsString(brancheFile);


        File commitFile = join(COMMIT_FOLDER, headHashName);
        /* 获取commit文件 */
        Commit commit = readObject(commitFile, Commit.class);

        return commit;

    }

    /**
     * 通过hashname来获取Commit对象
     *
     * @param hashName commit自己的hashName
     * @return
     */
    public static Commit getCommit(String hashName) {
        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        /* 如果在commit文件夹中不存在此文件 */
        if (!commitFiles.contains(hashName)) {
            return null;
        }
        File commitFile = join(COMMIT_FOLDER, hashName);
        Commit commit = readObject(commitFile, Commit.class);
        return commit;
    }


    /**
     * 给定一个commitId，返回一个相对应的commit对象，若是没有这个commit对象，则返回null
     *
     * @param commitId
     * @return commit或者null
     */
    public static Commit getCommitFromId(String commitId) {
        Commit commit = null;
        /* 查找对应的commit */

        /*  直接从commit文件夹中依次寻找 */
        String resCommitId = null;
        List<String> commitFileNames = plainFilenamesIn(COMMIT_FOLDER);
        /* 用于应对前缀的情况 */
        for (String commitFileName : commitFileNames) {
            if (commitFileName.startsWith(commitId)) {
                resCommitId = commitFileName;
                break;
            }
        }

        if (resCommitId == null) {
            return null;
        } else {
            File commitFile = join(COMMIT_FOLDER, resCommitId);
            commit = readObject(commitFile, Commit.class);
        }

        return commit;
    }

    /**
     * 获取两个分支的共同节点，仅从directParents搜索
     *
     * @param commitA
     * @param commitB
     * @return
     */
    public static Commit getSplitCommit(Commit commitA, Commit commitB) {

        Commit p1 = commitA, p2 = commitB;
        /* 用于遍历提交链 */
        Deque<Commit> dequecommitA = new ArrayDeque<>();
        Deque<Commit> dequecommitB = new ArrayDeque<>();
        /* 用于保存访问过的节点 */
        HashSet<String> visitedInCommitA = new HashSet<>();
        HashSet<String> visitedInCommitB = new HashSet<>();

        dequecommitA.add(p1);
        dequecommitB.add(p2);

        while (!dequecommitA.isEmpty() || !dequecommitB.isEmpty()) {
            if (!dequecommitA.isEmpty()) {
                /* commitA 的队列中存在可遍历对象 */
                Commit currA = dequecommitA.poll();
                if (visitedInCommitB.contains(currA.getHashName())) {
                    return currA;
                }
                visitedInCommitA.add(currA.getHashName());
                addParentsToDeque(currA, dequecommitA);
            }

            if (!dequecommitB.isEmpty()) {
                Commit currB = dequecommitB.poll();
                if (visitedInCommitA.contains(currB.getHashName())) {
                    return currB;
                }
                visitedInCommitB.add(currB.getHashName());
                addParentsToDeque(currB, dequecommitB);
            }
        }


        // 如果没有找到，就是null
        return null;

    }

    /**
     * 将此节点的父节点（或者是两个父节点）放入队列中
     *
     * @param commit
     * @param dequeCommit
     */
    private static void addParentsToDeque(Commit commit, Queue<Commit> dequeCommit) {
        if (!commit.getDirectParent().isEmpty()) {
            dequeCommit.add(getCommitFromId(commit.getDirectParent()));
        }

        if (commit.getOtherParent() != null) {
            dequeCommit.add(getCommitFromId(commit.getOtherParent()));
        }
    }

}
