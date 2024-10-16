package gitlet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Commit.*;
import static gitlet.Refs.*;
import static gitlet.Utils.*;
import static gitlet.Blob.*;
import static java.lang.System.exit;


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author ethanyi
 */
public class Repository {
    /**
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     * <p>
     * <p>
     * the path we created as below:
     * <p>
     * .gitlet (folder)
     *      |── objects (folder) // 存储commit对象文件
     *          |-- commits
     *          |-- blobs
     *      |── refs (folder)
     *          |── heads (folder) //指向目前的branch
     *              |-- master (file)
     *              |-- other file      //表示其他分支的路径
     *          |-- HEAD (file)     // 保存HEAD指针的对应hashname
     *      |-- addstage (folder)       // 暂存区文件夹
     *      |-- removestage (folder)
     */


    /**
     * initialize the folder instructure
     */
    public static void setupPersistence() {
        GITLET_DIR.mkdirs();
        COMMIT_FOLDER.mkdirs();
        BLOBS_FOLDER.mkdirs();
        REFS_DIR.mkdirs();
        HEAD_DIR.mkdirs();
        ADD_STAGE_DIR.mkdirs();
        REMOVE_STAGE_DIR.mkdirs();

    }

    /**
     * Check if the ARGS of java gitlet.Main is empty
     *
     * @param args
     */
    public static void checkArgsEmpty(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            exit(0);
        }
    }

    /**
     * To get Date obj a format to transform the object to String.
     *
     * @param date a Date obj
     * @return timestamp in standrad format
     */
    public static String dateToTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }


//    /**
//     * To save some Commit objects into files .
//     *
//     * @apiNote 暂时没有用到
//     */
//    public void saveObject2File(File path, Commit obj) {
//        // get the uid of this
//        String hashname = obj.getHashName();
//
//        // write obj to files
//        File commitFile = new File(COMMIT_FOLDER, hashname);
//        writeObject(commitFile, obj);
//    }


    public static void printCommitLog(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getHashName());
        System.out.println("Date: " + dateToTimeStamp(commit.getTimestamp()));
        System.out.println(commit.getMessage());
        System.out.print("\n");
    }

    /**
     * @param field      打印的标题区域
     * @param files      文件夹中的所有文件
     * @param branchName 指定的branchName
     */
    public static void printStatusPerField(String field, Collection<String> files,
                                           String branchName) {
        System.out.println("=== " + field + " ===");
        if (field.equals("Branches")) {
            for (var file : files) {
                // 如果是head文件
                if (file.equals(branchName)) {
                    System.out.println("*" + file);
                } else {
                    System.out.println(file);
                }
            }
        } else {
            for (var file : files) {
                System.out.println(file);
            }
        }

        System.out.print("\n");
    }


    /**
     * 对Modifications Not Staged For Commit这个领域的输出
     *
     * @param field         打印的标题区域
     * @param modifiedFiles 标记为modified的文件
     * @param deletedFiles  标记为deleted的文件
     */
    public static void printStatusWithStatus(String field, Collection<String> modifiedFiles,
                                             Collection<String> deletedFiles) {
        System.out.println("=== " + field + " ===");

        for (var file : modifiedFiles) {
            System.out.println(file + " " + "(modified)");
        }
        for (var file : deletedFiles) {
            System.out.println(file + " " + "(deleted)");
        }

        System.out.print("\n");
    }


    public static boolean untrackFileExists(Commit commit) {
        List<String> workFileNames = plainFilenamesIn(CWD);
        Set<String> currTrackSet = commit.getBlobMap().keySet();
        /* 先检测CWD中是否存在未被current branch跟踪的文件 */

        for (String workFile : workFileNames) {
            if (!currTrackSet.contains(workFile)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 用于处理两边文件都被修改的情况
     *
     * @param headCommit
     * @param otherHeadCommit
     */
    public static void processConflict(Commit headCommit, Commit otherHeadCommit,
                                       String splitTrackName) {
        String otherBlobFile = "";
        String otherBlobContent = "";

        String headBlobFile = "";
        String headBlobContent = "";

        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();

        /* 打印冲突 */
        message("Encountered a merge conflict.");
        /* 获取*/
        if (otherHeadCommitBolbMap.containsKey(splitTrackName)) {
            otherBlobFile = otherHeadCommitBolbMap.get(splitTrackName);
            otherBlobContent = getBlobContentFromName(otherBlobFile);
        }

        if (headCommitBolbMap.containsKey(splitTrackName)) {
            headBlobFile = headCommitBolbMap.get(splitTrackName);
            headBlobContent = getBlobContentFromName(headBlobFile);
        }

        /* 修改workFile中的内容*/
        StringBuilder resContent = new StringBuilder();
        resContent.append("<<<<<<< HEAD\n");
        resContent.append(headBlobContent);
        resContent.append("=======" + "\n");
        resContent.append(otherBlobContent);
        resContent.append(">>>>>>>" + "\n");

        String resContentString = resContent.toString();
        writeContents(join(CWD, splitTrackName), resContentString);
        addStage(splitTrackName);
    }

    /* ---------------------- 功能函数实现 --------------------- */

    /**
     * java gitlet.Main init
     */
    public static void initPersistence() {
        // if .gitlet dir existed
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            exit(0);
        }
        // create the folders in need
        setupPersistence();
        // create timestamp,Commit and save commit into files
        Date timestampInit = new Date(0);
        Commit initialCommit = new Commit("initial commit", timestampInit,
                "", null, null);
        initialCommit.saveCommit();

        // save the hashname to heads dir
        String commitHashName = initialCommit.getHashName();
        String branchName = "master";
        saveBranch(branchName, commitHashName);

        // 将此时的HEAD指针指向commit中的代表head的文件
        saveHEAD("master", commitHashName);

    }

    /**
     * java gitlet.Main add [file name]
     *
     * @param addFileName
     * @apiNote 这个函数用于实现git add
     */
    public static void addStage(String addFileName) {
        /* 如果文件名是空 */
        if (addFileName == null || addFileName.isEmpty()) {
            System.out.println("Please enter a file name.");
            exit(0);
        }

        File fileAdded = join(CWD, addFileName);
        /* 如果在工作目录中不存在此文件 */

        if (!fileAdded.exists()) {
            System.out.println("File does not exist.");
            exit(0);
        }
        String fileAddedContent = readContentsAsString(fileAdded);

        Commit headCommit = getHeadCommit();
        HashMap<String, String> headCommitBlobMap = headCommit.getBlobMap();

        /* 如果这个文件已经被track */
        if (headCommitBlobMap.containsKey(addFileName)) {
            String fileAddedInHash = headCommit.getBlobMap().get(addFileName);
            String commitContent = getBlobContentFromName(fileAddedInHash);

            /* 如果暂存内容和想要添加内容一致，则不将其纳入暂存区，
            同时将其从暂存区删除（如果存在）,同时将其从removal区移除 */
            if (commitContent.equals(fileAddedContent)) {
                List<String> filesAdd = plainFilenamesIn(ADD_STAGE_DIR);
                List<String> filesRm = plainFilenamesIn(REMOVE_STAGE_DIR);
                /* 如果在暂存区存在,从暂存区删除 */
                if (filesAdd.contains(addFileName)) {
                    join(ADD_STAGE_DIR, addFileName).delete();
                }
                /* 如果在removal area存在,从中删除 */
                if (filesRm.contains(addFileName)) {
                    join(REMOVE_STAGE_DIR, addFileName).delete();
                }

                return; //直接退出
            }

        }

        /* 将文件放入暂存区，blob文件名是内容的hash值，内容是源文件内容 */
        String fileContent = readContentsAsString(fileAdded);
        String blobName = sha1(fileContent);

        Blob blobAdd = new Blob(fileContent, blobName); // 使用blob进行对象化管理
        blobAdd.saveBlob();

        /* 不管原先是否存在，都会执行写逻辑*/
        /* addStage中写入指针,文件名是addFileName, 内容是暂存区保存的路径 */
        File blobPoint = join(ADD_STAGE_DIR, addFileName);
        writeContents(blobPoint, blobAdd.getFilePath().getName());
    }

    /**
     * java gitlet.Main commit [message]
     */
    public static void commitFile(String commitMsg) {
        /* 获取addstage中的filename和hashname */
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        /* 错误的情况，直接返回 */
        if (addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        if (commitMsg == null || commitMsg.isEmpty()) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }


        /* 获取最新的commit*/
        Commit oldCommit = getHeadCommit();

        /* 创建新的commit，newCommit根据oldCommit进行调整*/
        Commit newCommit = new Commit(oldCommit);
        newCommit.setDirectParent(oldCommit.getHashName());  // 指定父节点
        newCommit.setTimestamp(new Date(System.currentTimeMillis())); // 修改新一次的commit的时间戳为目前时间
        newCommit.setMessage(commitMsg); // 修改新一次的commit的时间戳为目前时间
//        newCommit.setBranchName(oldCommit.getBranchName()); // 在log或者status中需要展示本次commit的分支


        /* 对每一个addstage中的fileName进行其路径的读取，保存进commit的blobMap */
        for (String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            newCommit.addBlob(stageFileName, hashName);     // 在newCommit中更新blob
            join(ADD_STAGE_DIR, stageFileName).delete();
        }

        HashMap<String, String> blobMap = newCommit.getBlobMap();

        /* 对每一个rmstage中的fileName进行其路径的读取，删除commit的blobMap中对应的值 */
        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                newCommit.removeBlob(stageFileName);   // 在newCommit中删除removeStage中的blob
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        newCommit.saveCommit();

        /* 更新HEAD指针和当前branch head指针 */
        saveHEAD(getHeadBranchName(), newCommit.getHashName());
        saveBranch(getHeadBranchName(), newCommit.getHashName());

    }

    /**
     * 根据commit重载的方法，作用是为了进行merge时候的自动commit
     *
     * @param commitMsg
     * @param branchName
     */
    public static void commitFileForMerge(String commitMsg, String branchName) {


        /* 获取addstage中的filename和hashname */
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> removeStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        /* 错误的情况，直接返回 */
        if (addStageFiles.isEmpty() && removeStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }

        if (commitMsg == null) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }

        /* 获取最新的commit*/
        Commit oldCommit = getHeadCommit();
        Commit branchHeadCommit = getBranchHeadCommit(branchName, null);

        /* 创建新的commit，newCommit根据oldCommit进行调整*/
        Commit newCommit = new Commit(oldCommit);
        newCommit.setDirectParent(oldCommit.getHashName());  // 指定父节点
        newCommit.setTimestamp(new Date(System.currentTimeMillis())); // 修改新一次的commit的时间戳为目前时间
        newCommit.setMessage(commitMsg); // 修改新一次的commit的时间戳为目前时间
        newCommit.setOtherParent(branchHeadCommit.getHashName());   // 指定另一个父节点

        /* 对每一个addstage中的fileName进行其路径的读取，保存进commit的blobMap */
        for (String stageFileName : addStageFiles) {
            String hashName = readContentsAsString(join(ADD_STAGE_DIR, stageFileName));
            newCommit.addBlob(stageFileName, hashName);     // 在newCommit中更新blob
            join(ADD_STAGE_DIR, stageFileName).delete();
        }

        HashMap<String, String> blobMap = newCommit.getBlobMap();

        /* 对每一个rmstage中的fileName进行其路径的读取，删除commit的blobMap中对应的值 */
        for (String stageFileName : removeStageFiles) {
            if (blobMap.containsKey(stageFileName)) {
                join(BLOBS_FOLDER, blobMap.get(stageFileName)).delete(); // 删除blobs中的文件
                newCommit.removeBlob(stageFileName);   // 在newCommit中删除removeStage中的blob
            }
            join(REMOVE_STAGE_DIR, stageFileName).delete();
        }

        newCommit.saveCommit();

        /* 更新HEAD指针和master指针 */
        saveHEAD(getHeadBranchName(), newCommit.getHashName());
        saveBranch(getHeadBranchName(), newCommit.getHashName());
    }

    /**
     * java gitlet.Main rm [file name]
     *
     * @param removeFileName 指定删除的文件名
     */
    public static void removeStage(String removeFileName) {
        /* 如果文件名是空或者如果工作区没有这个文件 */
        if (removeFileName == null || removeFileName.isEmpty()) {
            System.out.println("Please enter a file name.");
            exit(0);
        }

        /* 如果在暂存目录中不存在此文件,同时在在commit中不存在此文件 */
        Commit headCommit = getHeadCommit();
        HashMap<String, String> blobMap = headCommit.getBlobMap();
        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);

        if (!blobMap.containsKey(removeFileName)) {
            if (!addStageFiles.contains(removeFileName)) {
                System.out.println("No reason to remove the file.");
                exit(0);
            }

        }

        /* 如果addStage中存在，则删除 */
        File addStageFile = join(ADD_STAGE_DIR, removeFileName);
        if (addStageFile.exists()) {
            addStageFile.delete();
        }


        /* 当此文件正被track中 */
        if (blobMap.containsKey(removeFileName)) {
            /* 添加进removeStage */
            File remoteFilePoint = new File(REMOVE_STAGE_DIR, removeFileName);
            writeContents(remoteFilePoint, "");

            /* 删除工作目录下文件,注意仅在这个文件被track的时候进行删除 */
            File fileDeleted = new File(CWD, removeFileName);
            restrictedDelete(fileDeleted);
        }

    }

    /**
     * java gitlet.Main log
     */
    public static void printLog() {

        Commit headCommit = getHeadCommit();
        Commit commit = headCommit;

        while (!commit.getDirectParent().equals("")) {
            printCommitLog(commit);
            commit = getCommit(commit.getDirectParent());
        }
        /* 打印最开始的一项*/
        printCommitLog(commit);
    }


    /**
     * java gitlet.Main global-log
     *
     * @apiNote 这是不关注分支，只是把文件夹中的内容都打印出来了
     */
    public static void printGlobalLog() {

        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFileName : commitFiles) {
            Commit commit = getCommit(commitFileName);
            printCommitLog(commit);
        }
    }

    /**
     * java gitlet.Main find [commit message]
     */
    public static void findCommit(String commitMsg) {
        Commit headCommit = getHeadCommit();
        Commit commit = headCommit;
        boolean found = false;
        /* 如果msg相等就break，或者是到达初始提交就退出 */

//        note: 这个方法有bug，无法找出不在branch中的commit
//        while (!commit.getDirectParent().isEmpty()) {
//
//            if (commit.getMessage().equals(commitMsg)) {
//                found = true;
//                System.out.println(commit.getHashName());
//            }
//            commit = getCommit(commit.getDirectParent());
//        }
//        /* 检查最后一个提交 */
//        if (commit.getMessage().equals(commitMsg)) {
//            found = true;
//            System.out.println(commit.getHashName());
//        }

        /*  直接从commit文件夹中依次寻找 */
        List<String> commitFiles = plainFilenamesIn(COMMIT_FOLDER);
        for (String commitFile : commitFiles) {
            Commit commit1 = getCommit(commitFile);
            if (commit1.getMessage().equals(commitMsg)) {
                message(commit1.getHashName());
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }

    }


    /**
     * java gitlet.Main status
     */
    public static void showStatus() {
        File gitletFile = join(CWD, ".gitlet");
        if (!gitletFile.exists()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }
        /* 获取当前分支名 */
        Commit headCommit = getHeadCommit();
        String branchName = getHeadBranchName();

        List<String> filesInHead = plainFilenamesIn(HEAD_DIR);
        List<String> filesInAdd = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> filesInRm = plainFilenamesIn(REMOVE_STAGE_DIR);
        HashMap<String, String> blobMap = headCommit.getBlobMap();
        Set<String> trackFileSet = blobMap.keySet();  // commit中跟踪着的文件名
        LinkedList<String> modifiedFilesList = new LinkedList<>();
        LinkedList<String> deletedFilesList = new LinkedList<>();
        LinkedList<String> untrackFilesList = new LinkedList<>();

        printStatusPerField("Branches", filesInHead, branchName);
        printStatusPerField("Staged Files", filesInAdd, branchName);
        printStatusPerField("Removed Files", filesInRm, branchName);

        /* 开始进行：Modifications Not Staged For Commit */
        /* 暂存已经添加，但内容与工作目录中的内容不同 */
        for (String fileAdd : filesInAdd) {
            /* 如果文件在暂存区存在，但是在工作区不存在，则直接加入modifiedFilesList */
            if (!join(CWD, fileAdd).exists()) {
                deletedFilesList.add(fileAdd);
                continue;
            }
            String workFileContent = readContentsAsString(join(CWD, fileAdd));
            String addStageBlobName = readContentsAsString(join(ADD_STAGE_DIR, fileAdd));
            String addStageFileContent = readContentsAsString(join(BLOBS_FOLDER, addStageBlobName));
            if (!workFileContent.equals(addStageFileContent)) {
                // 当工作区和addStage中文件内容不一致，则进入modifiedFilesList
                modifiedFilesList.add(fileAdd);
            }
        }

        /* 在当前commit中跟踪，在工作目录中更改，但未暂存 */
        for (String trackFile : trackFileSet) {
            if (trackFile.isEmpty() || trackFile == null) {
                continue;
            }
            File workFile = join(CWD, trackFile);
            File fileInRmStage = join(REMOVE_STAGE_DIR, trackFile);
            if (!workFile.exists()) {      // 当工作区文件直接不存在的情况
                if (!fileInRmStage.exists()) {
                    deletedFilesList.add(trackFile);       // 在rmStage中无此文件，同时工作区也没有这个文件
                }
                continue;
            }
            if (!filesInAdd.contains(trackFile)) { // 当addStage中没有此文件
                String workFileContent = readContentsAsString(workFile);
                String blobFileContent = readContentsAsString(join(BLOBS_FOLDER,
                        blobMap.get(trackFile)));
                if (!workFileContent.equals(blobFileContent)) {
                    // 当正在track的文件被修改，但addStage中无此文件，则进入modifiedFilesList
                    modifiedFilesList.add(trackFile);
                }
            }
        }
        printStatusWithStatus("Modifications Not Staged For Commit",
                modifiedFilesList, deletedFilesList);
        /* 开始进行：Untracked Files */
        List<String> workFiles = plainFilenamesIn(CWD);
        for (String workFile : workFiles) {
            if (!filesInAdd.contains(workFile)
                    && !filesInRm.contains(workFile)
                    && !trackFileSet.contains(workFile)) {
                untrackFilesList.add(workFile);
                continue;
            }
            if (filesInRm.contains(workFile)) {
                untrackFilesList.add(workFile);
            }
        }
        printStatusPerField("Untracked Files", untrackFilesList, branchName);
    }


    /**
     * java gitlet.Main checkout -- [file name]
     * java gitlet.Main checkout [commit id] -- [file name]
     * java gitlet.Main checkout [branch name]
     *
     * @param args
     */
    public static void checkOut(String[] args) {
        String fileName;
        if (args.length == 2) {
            //  git checkout branchName
            checkoutBranch(args[1]);
        } else if (args.length == 4) {
            //  git checkout [commit id] -- [file name]
            if (!args[2].equals("--")) {
                message("Incorrect operands.");
                exit(0);
            }
            /* 获取到Blob对象 */
            fileName = args[3];
            String commitId = args[1];
            Commit commit = getHeadCommit();

            /* 是否可以进行对objects文件夹的重构，实现hashMap结构
                使得时间效率上不是线性, 而不是依靠链表查找？ */
            if (getCommitFromId(commitId) == null) {
                System.out.println("No commit with that id exists.");
                exit(0);
            } else {
                commit = getCommitFromId(commitId);
            }

            if (!commit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            String blobName = commit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            /* 将Blob对象中的内容覆盖working directory中的内容 */
            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);

        } else if (args.length == 3) {
            //  git checkout -- [file name]
            /* 获取到Blob对象中的内容 */
            fileName = args[2];
            Commit headCommit = getHeadCommit();
            if (!headCommit.getBlobMap().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                exit(0);
            }
            String blobName = headCommit.getBlobMap().get(fileName);
            String targetBlobContent = getBlobContentFromName(blobName);

            /* 将Blob对象中的内容覆盖working directory中的内容 */
            File fileInWorkDir = join(CWD, fileName);
            overWriteFileWithBlob(fileInWorkDir, targetBlobContent);

        }
    }

    /**
     * 仅针对checkout的
     * java gitlet.Main checkout [branch name]情况
     *
     * @param branchName
     */
    public static void checkoutBranch(String branchName) {
        Commit headCommit = getHeadCommit();

        if (branchName.equals(getHeadBranchName())) {
            System.out.println("No need to checkout the current branch.");
            exit(0);
        }
        // 获取branchName的head对应的commit
        Commit branchHeadCommit = getBranchHeadCommit(branchName, "No such branch exists");
        HashMap<String, String> branchHeadBlobMap = branchHeadCommit.getBlobMap();
        Set<String> fileNameSet = branchHeadBlobMap.keySet();

        List<String> workFileNames = plainFilenamesIn(CWD);

        if (untrackFileExists(headCommit)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }

        /* 检测完后清空CWD文件夹 */
        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        /* 将fileNameSet中每一个跟踪的文件重写入工作文件夹中 */
        for (var trackedfileName : fileNameSet) {
            // 每一个trackedfileName是一个commit中跟踪的fileName
            File workFile = join(CWD, trackedfileName);
            String blobHash = branchHeadBlobMap.get(trackedfileName);   // 文件对应的blobName
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        /* 将目前给定的分支视作当前分支 */
        saveHEAD(branchName, branchHeadCommit.getHashName());
    }


    /**
     * java gitlet.Main branch [branch name]
     */
    public static void createBranch(String branchName) {
        Commit headCommit = getHeadCommit();
        List<String> fileNameinHeadDir = plainFilenamesIn(HEAD_DIR);
        if (fileNameinHeadDir.contains(branchName)) {
            message("A branch with that name already exists.");
            exit(0);
        }

        saveBranch(branchName, headCommit.getHashName());

    }


    /**
     * java gitlet.Main rm-branch [branch name]
     */
    public static void removeBranch(String branchName) {
        /* 检测是否有相关Branch */
        File brancheFile = join(HEAD_DIR, branchName);
        if (!brancheFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }
        /* 检测Branch是否为curr branch */
        Commit headCommit = getHeadCommit();
        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            exit(0);
        }
        /* 删除这个branch的指针文件 */
        File branchHeadPoint = join(HEAD_DIR, branchName);
        branchHeadPoint.delete();
    }

    /**
     * java gitlet.Main reset [commit id]
     *
     * @apiNote java gitlet.Main reset [commit id]      将文件内容全部转化为[commit id]中的文件
     */
    public static void reset(String commitId) {

        if (getCommitFromId(commitId) == null) {
            System.out.println("No commit with that id exists.");
            exit(0);
        }
        Commit headCommit = getHeadCommit();
        Commit commit = getCommitFromId(commitId);  // 将要reset的commit
        HashMap<String, String> commitBlobMap = commit.getBlobMap();

        /* 先检测CWD中是否存在未被current branch跟踪的文件 */
        List<String> workFileNames = plainFilenamesIn(CWD);
        /* 先检测CWD中是否存在未被current branch跟踪的文件 */
        if (untrackFileExists(headCommit)) {
            Set<String> currTrackSet = headCommit.getBlobMap().keySet();
            Set<String> resetTrackSet = commit.getBlobMap().keySet();
            boolean isUntrackInBoth = false;

            /* workfile没有在headCommit中也没有在commit中，将其从addstage中剔除，但在CWD中保存 */
            for (String workFile : workFileNames) {
                if (!currTrackSet.contains(workFile) && !resetTrackSet.contains(workFile)) {
                    removeStage(workFile);
                    isUntrackInBoth = true;
                    break;
                }
            }
            if (!isUntrackInBoth) {
                message("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                exit(0);
            }

        }

        /* 检测完后清空CWD文件夹 */
        for (String workFile : workFileNames) {
            restrictedDelete(join(CWD, workFile));
        }

        /* 将fileNameSet中每一个跟踪的文件重写入工作文件夹中 */
        for (var trackedfileName : commit.getBlobMap().keySet()) {
            // 每一个trackedfileName是一个commit中跟踪的fileName
            File workFile = join(CWD, trackedfileName);
            String blobHash = commitBlobMap.get(trackedfileName);   // 文件对应的blobName
            String blobFromNameContent = getBlobContentFromName(blobHash);
            writeContents(workFile, blobFromNameContent);
        }

        /* 同时将其branchHEAD指向commit*/
        saveBranch(getHeadBranchName(), commitId);
        /* 将目前给定的HEAD指针指向这个commit */
        saveHEAD(getHeadBranchName(), commitId);
    }


    /**
     * Merges files from the given branch into the current branch.
     * If the split point is the same commit as the given branch,
     * then we do nothing; the merge is complete, and the operation ends with the message:
     * Given branch is an ancestor of the current branch.
     * <p>
     * If the split point is the current branch, then the effect is to check out the given branch,
     * and the operation ends after printing the message: Current branch fast-forwarded.
     * Otherwise, we continue with the steps below.
     *
     * @apiNote :
     * 1. other：被修改      HEAD：未被修改 --->  working DIR: other, 并且需要被add
     * 2. other：未被修改    HEAD：被修改   --->  working DIR: HEAD
     * 3. other：被修改      HEAD：被修改   --->  （一致的修改）  working DIR: HEAD, 相当于什么都不做
     * |->  （不一致的修改）  working DIR: Conflict
     * 4. split：不存在      other：不存在    HEAD：被添加   --->  working DIR: HEAD
     * 5. split：不存在      other：被添加    HEAD：不存在   --->  working DIR: other, 并且需要被add
     * 6. other：被删除      HEAD：未被修改   --->  working DIR: 被删除，同时被暂存于removal
     * 7. other：未被修改     HEAD：被删除   --->  working DIR: 被删除
     */
    public static void mergeBranch(String branchName) {
        checkSafetyInMerge(branchName);
        Commit headCommit = getHeadCommit();
        Commit otherHeadCommit = getBranchHeadCommit(branchName,
                "A branch with that name does not exist."); // 如果不存在这个branch，则报错
        /* 获取当前splitCommit对象 */
        Commit splitCommit = getSplitCommit(headCommit, otherHeadCommit);
        if (splitCommit.getHashName().equals(otherHeadCommit.getHashName())) {
            message("Given branch is an ancestor of the current branch.");
            exit(0);
        }

        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitKeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        processSplitCommit(splitCommit, headCommit, otherHeadCommit);
        /* 为解决被删除操作 */
        for (var headTrackName : headKeySet) {
            if (!otherHeadCommitBolbMap.containsKey(headTrackName)) {
                if (!splitCommitBolbMap.containsKey(headTrackName)) {
                    /* 情况4：如果在other和split中都没有这个文件 */
                    continue;
                } else {
                    /* split：存在  other：被删除 */
                    if (!headCommitBolbMap.get(headTrackName)
                            .equals(splitCommitBolbMap.get(headTrackName))) {
                        /* HEAD：被修改 */
                        /* 存在 conflict */
                        processConflict(headCommit, otherHeadCommit, headTrackName);
                    }
                    /* 其他情况是情况6 已处理过 */
                }
            } else if (otherHeadCommitBolbMap.containsKey(headTrackName)
                    && !splitCommitBolbMap.containsKey(headTrackName)) {
                /* 情况3b other中存在文件, split中不存在文件，即这是不一致的修改*/
                if (!otherHeadCommitBolbMap.get(headTrackName)
                        .equals(headCommitBolbMap.get(headTrackName))) {
                    /*如果是不一致的修改，进行conflict处理，如果是一致的就跳过*/
                    processConflict(headCommit, otherHeadCommit, headTrackName);
                }
            }
        }
        for (var otherTrackName : otherKeySet) {
            if (!headCommitBolbMap.containsKey(otherTrackName)
                    && !splitCommitBolbMap.containsKey(otherTrackName)) {
                /* 情况5：如果在head和split中都没有这个文件 */
                String[] checkOutArgs = {"checkout",
                                         otherHeadCommit.getHashName(),
                                         "--",
                                         otherTrackName};
                checkOut(checkOutArgs);
                addStage(otherTrackName);
            }
        }
        if (splitCommit.getHashName().equals(headCommit.getHashName())) {
            message("Current branch fast-forwarded.");
        }
        /* 进行一次自动的commit */
        String commitMsg = String.format("Merged %s into %s.", branchName, getHeadBranchName());
        commitFileForMerge(commitMsg, branchName);
    }


    public static void processSplitCommit(Commit splitCommit, Commit headCommit,
                                          Commit otherHeadCommit) {
        HashMap<String, String> splitCommitBolbMap = splitCommit.getBlobMap();
        Set<String> splitKeySet = splitCommitBolbMap.keySet();
        HashMap<String, String> headCommitBolbMap = headCommit.getBlobMap();
        Set<String> headKeySet = headCommitBolbMap.keySet();
        HashMap<String, String> otherHeadCommitBolbMap = otherHeadCommit.getBlobMap();
        Set<String> otherKeySet = otherHeadCommitBolbMap.keySet();

        /* 从split中的文件开始 */
        for (var splitTrackName : splitKeySet) {
            // 如果在HEAD中未被修改(包括未被删除）
            if (headCommitBolbMap.containsKey(splitTrackName)
                    && headCommitBolbMap.get(splitTrackName)
                            .equals(splitCommitBolbMap.get(splitTrackName))) {
                // 如果other中存在此文件
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)) {
                    /* 情况1 HEAD中未被修改，other中被修改*/
                    if (!otherHeadCommitBolbMap.get(splitTrackName)
                            .equals(splitCommitBolbMap.get(splitTrackName))) {
                        // 使用checkout将other的文件覆盖进工作区，同时将其add进暂存区
                        String[] checkOutArgs = {"checkout",
                                                 otherHeadCommit.getHashName(),
                                                 "--",
                                                 splitTrackName};
                        checkOut(checkOutArgs);
                        addStage(splitTrackName);
                    }
                } else {
                    /* 情况6: 当HEAD未修改，other中被删除 */
                    removeStage(splitTrackName);
                }
            } else {
                // 在HEAD中被修改（包括被删除）
                if (otherHeadCommitBolbMap.containsKey(splitTrackName)
                        && otherHeadCommitBolbMap.get(splitTrackName)
                                .equals(splitCommitBolbMap.get(splitTrackName))) {
                    /* 情况2 other中未被修改，HEAD中被修改，则不修改任何事情
                       情况7 other中未被修改，HEAD中被删除，则不修改任何事情 */
                    continue;
                } else {
                    /* other中被修改 或者被删除 */
                    if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                            && !headCommitBolbMap.containsKey(splitTrackName)) {
                        /* 情况3a 一致的删除 */
                        continue;
                    } else if (!otherHeadCommitBolbMap.containsKey(splitTrackName)
                            || !headCommitBolbMap.containsKey(splitTrackName)) {
                        /* 只存在一方被删除，跳过，从后面单独对HEAD和other指针进行操作 */
                        continue;
                    } else {
                        if (otherHeadCommitBolbMap.get(splitTrackName)
                                .equals(headCommitBolbMap.get(splitTrackName))) {
                            /* 情况3a 一致的修改 */
                            continue;
                        } else {
                            /* 情况3b 不一致的修改，不包括删除操作 */
                            processConflict(headCommit, otherHeadCommit, splitTrackName);
                        }
                    }
                }
            }
        }
    }


    /**
     * 检测merge指令的错误情况
     *
     * @param branchName
     */
    public static void checkSafetyInMerge(String branchName) {

        List<String> addStageFiles = plainFilenamesIn(ADD_STAGE_DIR);
        List<String> rmStageFiles = plainFilenamesIn(REMOVE_STAGE_DIR);
        /* 如果存在暂存，直接退出 */
        if (!addStageFiles.isEmpty() || !rmStageFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }
        Commit headCommit = getHeadCommit();
        // 如果不存在这个branch，则报错
        String errMsg = "A branch with that name does not exist.";
        Commit otherHeadCommit = getBranchHeadCommit(branchName, errMsg);

        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            exit(0);
        }
        /* 查看是否存在未被跟踪的文件 */
        if (untrackFileExists(headCommit)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            exit(0);
        }
    }
}
