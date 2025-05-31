package gitlet;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.*;

public class Repository {


    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMIT_DIR = join(GITLET_DIR, "commits");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File BRANCHES_DIR = join(REFS_DIR, "branches");
    public static final File STAGE_DIR = join(GITLET_DIR, "stage");
    public static final File HEAD_FILE = join(REFS_DIR, "HEAD");
    public static final File REMOTES_DIR = join(GITLET_DIR, "remotes");
    public static final File GITLET_REMOTE_DIR=join(CWD,".gitlet-remote");

    /* TODO: fill in the rest of this class. */
    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        // Create Initial commit and save it in refs/heads/master
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOBS_DIR.mkdir();
        REFS_DIR.mkdir();
        BRANCHES_DIR.mkdir();
        STAGE_DIR.mkdir();
        REMOTES_DIR.mkdir();
        Commit initCommit = new Commit();
        String commitId = sha1Commit(initCommit);

        File commitFile = join(COMMIT_DIR, commitId);
        writeObject(commitFile, initCommit);
        //Save branch "master" pointing to initial commit
        File masterBranch = join(BRANCHES_DIR, "master");
        writeContents(masterBranch, commitId);
        writeContents(HEAD_FILE, "master");

    }
    public void initRemote(String path) {
        File remoteRoot = new File(path);
        File remoteGitlet = Utils.join(remoteRoot, ".gitlet");
        File commits = Utils.join(remoteGitlet, "commits");
        File blobs = Utils.join(remoteGitlet, "blobs");
        File branches = Utils.join(remoteGitlet, "branches");
        File stage = Utils.join(remoteGitlet, "stage");
        File refs = Utils.join(remoteGitlet, "refs");
        File remotes = Utils.join(remoteGitlet, "remotes");
        File headFile = Utils.join(remoteGitlet, "HEAD");

        if (remoteGitlet.exists()) {
            System.out.println("A Gitlet version-control system already exists in the remote directory.");
            System.exit(0);
        }

        // Ensure .gitlet root exists
        if (!remoteGitlet.mkdirs()) {
            System.out.println("Failed to create remote .gitlet directory.");
            System.exit(0);
        }

        // Now create subdirectories
        commits.mkdir();
        blobs.mkdir();
        branches.mkdir();
        stage.mkdir();
        refs.mkdir();
        remotes.mkdir();

        // Check all critical subdirs
        if (!commits.exists() || !branches.exists() || !blobs.exists()) {
            System.out.println("Remote structure failed to initialize.");
            System.exit(0);
        }

        // Write initial commit and refs
        Commit initCommit = new Commit();
        String id = sha1Commit(initCommit);
        Utils.writeObject(Utils.join(commits, id), initCommit);
        Utils.writeContents(Utils.join(branches, "master"), id);
        Utils.writeContents(headFile, "master");
    }




    public void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.isFile()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Blob blob = new Blob(file);
        String blobId = blob.getId();
        blob.save();

        Commit head = getHeadCommit();
        Map<String, String> tracked = head.getBlobs();
        String trackedBlobId = tracked.get(fileName);

        Stage stage = getStage();

        if (blobId.equals(trackedBlobId)) {
            // already tracked and unchanged, remove from stage
            stage.additionMap.remove(fileName);
            stage.removalSet.remove(fileName);
            saveStage(stage);
            return;
        }

        stage.additionMap.put(fileName, blobId);
        stage.removalSet.remove(fileName);
        saveStage(stage);
    }

    public void commit(String message) {
        if (message == null || message.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        Stage stage = getStage();
        Commit parent = getHeadCommit();
        if (stage.additionMap.isEmpty() && stage.removalSet.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Map<String, String> newBlobs = new HashMap<>(parent.getBlobs());
        for (String removedFile : stage.removalSet) {
            newBlobs.remove(removedFile);
        }
        for (Map.Entry<String, String> addedFile : stage.additionMap.entrySet()) {
            newBlobs.put(addedFile.getKey(), addedFile.getValue());
        }

        String parentId = readContentsAsString(join(BRANCHES_DIR, readContentsAsString(HEAD_FILE)));
        Commit newCommit = new Commit(message, parentId, newBlobs);
        String newCommitId = sha1Commit(newCommit);
        writeObject(join(COMMIT_DIR, newCommitId), newCommit);

        String currentBranch = readContentsAsString(HEAD_FILE);
        writeContents(join(BRANCHES_DIR, currentBranch), newCommitId);

        stage.clear();
        saveStage(stage);
    }

    //print log
    public void log() {
        Commit commit = getHeadCommit();
        while (commit != null) {
            printCommitInfo(commit);
            commit = commit.getParent() == null ? null : readCommit(commit.getParent());
        }
    }

    //print global log
    public void globalLog() {
        for (File file : COMMIT_DIR.listFiles()) {
            Commit commit = readObject(file, Commit.class);
            printCommitInfo(commit);
        }
    }

    public void checkoutFile(String fileName) {
        Commit head = getHeadCommit();
        restoreFileFromCommit(head, fileName);
    }

    public void checkoutFileFromCommit(String commitId, String fileName) {
        String fullCommitId = expandCommitId(commitId);
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = readObject(join(COMMIT_DIR, fullCommitId), Commit.class);
        restoreFileFromCommit(commit, fileName);
    }

    public void checkoutBranch(String branchName) {
        File branchFile = join(BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String currentBranch = readContentsAsString(HEAD_FILE);
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        String commitId = readContentsAsString(branchFile);
        Commit targetCommit = readObject(join(COMMIT_DIR, commitId), Commit.class);
        Commit headCommit = getHeadCommit();

        checkUntrackedFiles(headCommit, targetCommit);

        for (String fileName : CWD.list()) {
            if (!targetCommit.getBlobs().containsKey(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        }

        for (Map.Entry<String, String> entry : targetCommit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);
            writeContents(join(CWD, fileName), blob.getContents());
        }

        writeContents(HEAD_FILE, branchName);
    }

    public void createBranch(String name) {
        File branchFile = join(BRANCHES_DIR, name);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exsits");
            System.exit(0);
        }
        String commitId = readContentsAsString(join(BRANCHES_DIR, readContentsAsString(HEAD_FILE)));
        writeContents(branchFile, commitId);

    }

    public void removeBranch(String branchName) {
        if (readContentsAsString(HEAD_FILE).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File branchFile = join(BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        branchFile.delete();
    }

    public void remove(String fileName) {
        Stage stage = getStage();
        Commit head = getHeadCommit();
        Map<String, String> tracked = head.getBlobs();

        if (!tracked.containsKey(fileName) && !stage.additionMap.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (stage.additionMap.containsKey(fileName)) {
            stage.additionMap.remove(fileName);
        }
        if (tracked.containsKey(fileName)) {
            restrictedDelete(join(CWD, fileName));
            stage.removalSet.add(fileName);
        }
        saveStage(stage);
    }

    public void find(String message) {
        boolean found = false;
        for (File file : COMMIT_DIR.listFiles()) {
            Commit commit = readObject(file, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(file.getName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        String currentBranch = readContentsAsString(HEAD_FILE);
        Stage stage = getStage();

        // === Branches ===
        System.out.println("=== Branches ===");
        List<String> branches = new ArrayList<>();
        for (File file : BRANCHES_DIR.listFiles()) {
            branches.add(file.getName());
        }
        Collections.sort(branches);
        for (String branchName : branches) {
            if (branchName.equals(currentBranch)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();
        // === Staged Files ===
        System.out.println("=== Staged Files ===");
        List<String> stagedFiles = new ArrayList<>(stage.additionMap.keySet());
        Collections.sort(stagedFiles);
        for (String fileName : stagedFiles) {
            System.out.println(fileName);
        }
        System.out.println();

        // === Removed Files ===
        System.out.println("=== Removed Files ===");
        List<String> removedFiles = new ArrayList<>(stage.removalSet);
        Collections.sort(removedFiles);
        for (String fileName : removedFiles) {
            System.out.println(fileName);
        }
        System.out.println();

        // === Modifications Not Staged For Commit ===
        System.out.println("=== Modifications Not Staged For Commit ===");
        printModificationsNotStaged();
        System.out.println();

        // === Untracked Files ===
        System.out.println("=== Untracked Files ===");
        printUntrackedFiles();
    }

    public void reset(String commitId) {
        File commitFile = join(COMMIT_DIR, commitId);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit resetCommit = readCommit(commitId);
        Commit headCommit = getHeadCommit();

        checkUntrackedFiles(headCommit, resetCommit);

        for (String fileName : CWD.list()) {
            if (!resetCommit.getBlobs().containsKey(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        }

        for (Map.Entry<String, String> entry : resetCommit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);
            writeContents(join(CWD, fileName), blob.getContents());
        }

        String branchName = readContentsAsString(HEAD_FILE);
        writeContents(join(BRANCHES_DIR, branchName), commitId);

        Stage stage = getStage();
        stage.clear();
        saveStage(stage);
    }

    public void merge(String branchName) {
        String currentBranch = readContentsAsString(HEAD_FILE);
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        File branchFile = join(BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        Stage stage = getStage();
        if (!stage.additionMap.isEmpty() || !stage.removalSet.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        Commit headCommit = getHeadCommit();
        Commit givenCommit = readCommit(readContentsAsString(branchFile));

        checkUntrackedFiles(headCommit, givenCommit);

        String currentCommitId = readContentsAsString(join(BRANCHES_DIR, currentBranch));
        String givenCommitId = readContentsAsString(join(BRANCHES_DIR, branchName));
        String splitCommitId = findSplitPoint(currentCommitId, givenCommitId);

        if (splitCommitId.equals(givenCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitCommitId.equals(currentCommitId)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        boolean hasConflict = mergeFiles(splitCommitId, currentCommitId, givenCommitId);

        Map<String, String> newBlobs = new HashMap<>();
        for (String fileName : plainFilenamesIn(CWD)) {
            File file = join(CWD, fileName);
            byte[] fileContent = readContents(file);
            String blobId = sha1(fileContent);
            Blob blob = new Blob(file);  // 注意只传 File
            writeObject(join(BLOBS_DIR, blobId), blob);
            newBlobs.put(fileName, blobId);
        }

        Commit mergedCommit = new Commit(
                "Merged " + branchName + " into " + currentBranch + ".",
                currentCommitId,
                newBlobs
        );
        mergedCommit.setSecondParent(givenCommitId);

        String mergedCommitId = sha1Commit(mergedCommit);
        writeObject(join(COMMIT_DIR, mergedCommitId), mergedCommit);
        writeContents(join(BRANCHES_DIR, currentBranch), mergedCommitId);

        stage.clear();
        saveStage(stage);

        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }


    public void addRemote(String name, String path) {
        File remoteFile = join(REMOTES_DIR, name);
        if (remoteFile.exists()) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        writeContents(remoteFile, path);
    }

    public void removeRemote(String name) {
        File remoteFile = join(REMOTES_DIR, name);
        if (!remoteFile.exists()) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        restrictedDelete(remoteFile);
    }

    public void push(String remoteName, String branchName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            System.out.println("Remote with that name does not exist.");
            System.exit(0);
        }
        String remotePath = readContentsAsString(remoteFile);
        File remoteGitlet = new File(remotePath);

        File remoteBranchesDir = join(remoteGitlet, "refs", "branches");
        File remoteCommitsDir = join(remoteGitlet, "commits");
        File remoteBlobsDir = join(remoteGitlet, "blobs");

        if (!remoteBranchesDir.exists() || !remoteCommitsDir.exists() || !remoteBlobsDir.exists()) {
            System.out.println("Remote repository not initialized.");
            System.exit(0);
        }

        File remoteBranchFile = join(remoteBranchesDir, branchName);
        String remoteCommitId = remoteBranchFile.exists() ? readContentsAsString(remoteBranchFile) : null;

        Commit head = getHeadCommit();
        String localCommitId = sha1Commit(head);

        if (remoteCommitId != null && !isAncestor(remoteCommitId, localCommitId)) {
            System.out.println("Please pull down remote changes before pushing.");
            System.exit(0);
        }

        copyCommitsAndBlobs(localCommitId, remoteCommitId, remoteGitlet);
        writeContents(remoteBranchFile, localCommitId);
    }





    public void fetch(String remoteName, String branchName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            System.out.println("Remote with that name does not exist.");
            System.exit(0);
        }
        String remotePath = readContentsAsString(remoteFile);
        File remoteGitlet = new File(remotePath);

        File remoteBranchesDir = join(remoteGitlet, "refs", "branches");
        File remoteCommitsDir = join(remoteGitlet, "commits");
        File remoteBlobsDir = join(remoteGitlet, "blobs");

        if (!remoteBranchesDir.exists() || !remoteCommitsDir.exists() || !remoteBlobsDir.exists()) {
            System.out.println("Remote repository not initialized.");
            System.exit(0);
        }

        File remoteBranchFile = join(remoteBranchesDir, branchName);
        if (!remoteBranchFile.exists()) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        String remoteCommitId = readContentsAsString(remoteBranchFile);

        copyCommitsAndBlobsFromRemote(remoteCommitId, remoteGitlet);

        File localRemoteBranch = join(BRANCHES_DIR, remoteName + "__" + branchName);
        writeContents(localRemoteBranch, remoteCommitId);
    }





    public void pull(String remoteName, String branchName) {
        fetch(remoteName, branchName);
        merge(remoteName + "__" + branchName);
    }
    private boolean isAncestor(String ancestorId, String descendantId) {
        if (ancestorId == null) return true; // 远程分支不存在时允许 push

        while (descendantId != null) {
            if (descendantId.equals(ancestorId)) {
                return true;
            }
            Commit commit = readCommit(descendantId);
            descendantId = commit.getParent();
        }
        return false;
    }

    /** 将 fromId 到 untilId（不含）的所有 commit 和 blob 拷贝到远程仓库 remoteGitlet */
    private void copyCommitsAndBlobs(String fromId, String untilId, File remoteGitlet) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(fromId);

        File remoteCommitsDir = Utils.join(remoteGitlet, "commits");
        File remoteBlobsDir = Utils.join(remoteGitlet, "blobs");

        while (!queue.isEmpty()) {
            String commitId = queue.poll();
            if (commitId == null || commitId.equals(untilId) || visited.contains(commitId)) {
                continue;
            }
            visited.add(commitId);

            // 读取本地 commit
            File localCommitFile = Utils.join(COMMIT_DIR, commitId);
            Commit commit = Utils.readObject(localCommitFile, Commit.class);

            // 写入远程 commit
            File remoteCommitFile = Utils.join(remoteCommitsDir, commitId);
            if (!remoteCommitFile.exists()) {
                Utils.writeObject(remoteCommitFile, commit);
            }

            // 复制 blobs（不去重）
            for (String blobId : commit.getBlobs().values()) {
                File localBlobFile = Utils.join(BLOBS_DIR, blobId);
                File remoteBlobFile = Utils.join(remoteBlobsDir, blobId);
                if (!remoteBlobFile.exists()) {
                    byte[] blobContent = Utils.readContents(localBlobFile);
                    Utils.writeContents(remoteBlobFile, blobContent);
                }
            }

            // 向上遍历 commit 链（包括 merge 的 second parent）
            if (commit.getParent() != null) {
                queue.add(commit.getParent());
            }
            if (commit.getSecondParent() != null) {
                queue.add(commit.getSecondParent());
            }
        }
    }

    private void copyCommitsAndBlobsFromRemote(String remoteCommitId, File remoteGitlet) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(remoteCommitId);

        File remoteCommits = join(remoteGitlet, "commits");
        File remoteBlobs = join(remoteGitlet, "blobs");

        while (!queue.isEmpty()) {
            String commitId = queue.poll();
            if (visited.contains(commitId)) continue;
            visited.add(commitId);

            File remoteCommitFile = join(remoteCommits, commitId);
            if (!remoteCommitFile.exists()) continue;

            Commit commit = readObject(remoteCommitFile, Commit.class);

            File localCommitFile = join(COMMIT_DIR, commitId);
            if (!localCommitFile.exists()) {
                writeObject(localCommitFile, commit);
            }

            for (String blobId : commit.getBlobs().values()) {
                File localBlobFile = join(BLOBS_DIR, blobId);
                if (!localBlobFile.exists()) {
                    File remoteBlobFile = join(remoteBlobs, blobId);
                    if (remoteBlobFile.exists()) {
                        byte[] contents = readContents(remoteBlobFile);
                        writeContents(localBlobFile, contents);
                    }
                }
            }

            if (commit.getParent() != null) queue.add(commit.getParent());
            if (commit.getSecondParent() != null) queue.add(commit.getSecondParent());
        }
    }



    //helper function for getHeadCommit
    public Commit getHeadCommit() {
        String branchName = readContentsAsString(HEAD_FILE);
        File branchFile = join(BRANCHES_DIR, branchName);
        String commitId = readContentsAsString(branchFile);
        File commitFile = join(COMMIT_DIR, commitId);
        return readObject(commitFile, Commit.class);
    }

    //helper function for getStage
    public Stage getStage() {
        File stageFile = join(STAGE_DIR, "stage.ser");
        if (!stageFile.exists()) {
            return new Stage();  // 空的 stage（additionMap、removalSet 初始化为空）
        }
        return Utils.readObject(stageFile, Stage.class);
    }

    public void saveStage(Stage stage) {
        File stageFile = join(STAGE_DIR, "stage.ser");
        Utils.writeObject(stageFile, stage);
    }

    //helper function for sha1Commit
    private String sha1Commit(Commit commit) {
        String parent = (commit.getParent() == null) ? "" : commit.getParent();
        String secondParent = (commit.getSecondParent() == null) ? "" : commit.getSecondParent();
        String blobs = (commit.getBlobs() == null) ? "" : commit.getBlobs().toString();

        return Utils.sha1(commit.getMessage(), commit.getTimestamp(), parent, secondParent, blobs);
    }

    //get Commit from file eg: get parent commit
    private Commit readCommit(String CommitId) {
        File commitFile = join(COMMIT_DIR, CommitId);
        return readObject(commitFile, Commit.class);
    }

    //helper fuction for printing commit info for log fuction
    private void printCommitInfo(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + sha1Commit(commit));

        if (commit.getSecondParent() != null) {
            String parent1 = commit.getParent().substring(0, 7);
            String parent2 = commit.getSecondParent().substring(0, 7);
            System.out.println("Merge: " + parent1 + " " + parent2);
        }

        System.out.println("Date: " + commit.getTimestamp());
        System.out.println(commit.getMessage());
        System.out.println();
    }

    // get full commitID
    private String expandCommitId(String shortId) {
        for (File file : COMMIT_DIR.listFiles()) {
            if (file.getName().startsWith(shortId)) {
                return file.getName();
            }
        }
        return null;
    }

    //helper fuction for restore file from checkout commit
    private void restoreFileFromCommit(Commit commit, String fileName) {
        Map<String, String> tracked = commit.getBlobs();
        if (!tracked.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobId = tracked.get(fileName);
        Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);
        writeContents(join(CWD, fileName), blob.getContents());
    }

    private void printModificationsNotStaged() {
        Commit head = getHeadCommit();
        Stage stage = getStage();
        Map<String, String> tracked = head.getBlobs();
        List<String> cwdFiles = plainFilenamesIn(CWD);

        List<String> result = new ArrayList<>();

        // First, check all files in the working directory
        for (String fileName : cwdFiles) {
            File file = join(CWD, fileName);
            Blob blob = new Blob(file);
            String blobId = blob.getId();

            // Case 1: File is staged for addition but modified afterward
            if (stage.additionMap.containsKey(fileName)) {
                // file is staged
                if (!stage.additionMap.get(fileName).equals(blobId)) {
                    result.add(fileName + " (modified)");
                }
                // Case 2: File is tracked by HEAD commit but modified
            } else if (tracked.containsKey(fileName)) {
                if (!tracked.get(fileName).equals(blobId)) {
                    result.add(fileName + " (modified)");
                }
            }
        }

        // Second, check if any tracked file is missing (deleted) but not staged for removal
        for (String fileName : tracked.keySet()) {
            if (!cwdFiles.contains(fileName) && !stage.removalSet.contains(fileName)) {
                result.add(fileName + " (deleted)");
            }
        }

        // Sort results lexicographically
        Collections.sort(result);
        for (String s : result) {
            System.out.println(s);
        }
    }

    private void printUntrackedFiles() {
        Commit head = getHeadCommit();
        Stage stage = getStage();
        Map<String, String> tracked = head.getBlobs();
        List<String> cwdFiles = plainFilenamesIn(CWD);

        List<String> result = new ArrayList<>();
        for (String fileName : cwdFiles) {
            if (!tracked.containsKey(fileName) && !stage.additionMap.containsKey(fileName)) {
                result.add(fileName);
            }
        }
        Collections.sort(result);
        for (String s : result) {
            System.out.println(s);
        }
    }

    //helper function for get parent Ids
    private List<String> getParentIds(Commit commit) {
        List<String> parentIds = new ArrayList<>();
        if (commit.getParent() != null) {
            parentIds.add(commit.getParent());
        }
        if (commit.getSecondParent() != null) {
            parentIds.add(commit.getSecondParent());
        }
        return parentIds;
    }

    //BFS find split point of two commits
    private String findSplitPoint(String commitId1, String commitId2) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId1);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            visited.add(current);
            for (String parentId : getParentIds(readCommit(current))) {
                if (parentId != null && !visited.contains(parentId)) {
                    queue.add(parentId);
                }
            }
        }

        queue.add(commitId2);
        Set<String> visited2 = new HashSet<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (visited.contains(current)) {
                return current;
            }
            visited2.add(current);
            for (String parentId : getParentIds(readCommit(current))) {
                if (parentId != null && !visited2.contains(parentId)) {
                    queue.add(parentId);
                }
            }
        }
        return null;
    }

    private void checkUntrackedFiles(Commit headCommit, Commit givenCommit) {
        Stage stage = getStage();
        Map<String, String> headBlobs = headCommit.getBlobs();
        Map<String, String> givenBlobs = givenCommit.getBlobs();
        List<String> cwdFiles = plainFilenamesIn(CWD);

        for (String fileName : cwdFiles) {
            if (!headBlobs.containsKey(fileName) && !stage.additionMap.containsKey(fileName)
                    && givenBlobs.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }


    private boolean mergeFiles(String splitCommitId, String headCommitId, String givenCommitId) {
        Commit splitCommit = readCommit(splitCommitId);
        Commit headCommit = readCommit(headCommitId);
        Commit givenCommit = readCommit(givenCommitId);

        Map<String, String> splitBlobs = splitCommit.getBlobs();
        Map<String, String> headBlobs = headCommit.getBlobs();
        Map<String, String> givenBlobs = givenCommit.getBlobs();

        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(splitBlobs.keySet());
        allFiles.addAll(headBlobs.keySet());
        allFiles.addAll(givenBlobs.keySet());

        boolean conflict = false;

        for (String fileName : allFiles) {
            String splitBlobId = splitBlobs.get(fileName);
            String headBlobId = headBlobs.get(fileName);
            String givenBlobId = givenBlobs.get(fileName);

            boolean headSameSplit = (splitBlobId == null && headBlobId == null) ||
                    (splitBlobId != null && splitBlobId.equals(headBlobId));
            boolean givenSameSplit = (splitBlobId == null && givenBlobId == null) ||
                    (splitBlobId != null && splitBlobId.equals(givenBlobId));
            boolean headSameGiven = (headBlobId == null && givenBlobId == null) ||
                    (headBlobId != null && headBlobId.equals(givenBlobId));

            if (headSameSplit && !givenSameSplit) {
                // split==head, given不同 => checkout given
                if (givenBlobId == null) {
                    restrictedDelete(join(CWD, fileName));
                    stageForRemoval(fileName);
                } else {
                    Blob blob = readObject(join(BLOBS_DIR, givenBlobId), Blob.class);
                    writeContents(join(CWD, fileName), blob.getContents());
                    add(fileName);
                }
            } else if (!headSameSplit && givenSameSplit) {
                // split==given, head不同 => 保持head
            } else if (!headSameSplit && !givenSameSplit && !headSameGiven) {
                // 冲突
                conflict = true;

                String headContent = (headBlobId == null) ? "" :
                        new String(readObject(join(BLOBS_DIR, headBlobId), Blob.class).getContents(), StandardCharsets.UTF_8);
                String givenContent = (givenBlobId == null) ? "" :
                        new String(readObject(join(BLOBS_DIR, givenBlobId), Blob.class).getContents(), StandardCharsets.UTF_8);

                String conflictContent = "<<<<<<< HEAD\n" + headContent +
                        "=======\n" + givenContent +
                        ">>>>>>>\n";

                writeContents(join(CWD, fileName), conflictContent.getBytes(StandardCharsets.UTF_8));
                add(fileName);
            }
        }

        return conflict;
    }

    private void stageForRemoval(String fileName) {
        Stage stage = getStage();
        stage.additionMap.remove(fileName); // remove if staged for addition
        stage.removalSet.add(fileName); // add to removal set
        saveStage(stage);
    }

}
