package gitlet;

import static gitlet.Repository.*;
import static gitlet.Utils.message;
import static java.lang.System.exit;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author thanyi
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // check if args is empty.
        checkArgsEmpty(args);
        String firstArg = args[0];
        try {
            switch (firstArg) {
                case "init":
                    if (args.length != 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    initPersistence();
                    break;
                case "add":
                    String addFileName = args[1];
                    addStage(addFileName);
                    break;
                case "commit":
                    String commitMsg = args[1];
                    commitFile(commitMsg);
                    break;
                case "rm":
                    String removeFile = args[1];
                    removeStage(removeFile);
                    break;
                case "log":
                    if (args.length != 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    printLog();
                    break;
                case "global-log":
                    if (args.length != 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    printGlobalLog();
                    break;
                case "find":
                    String findMsg = args[1];
                    findCommit(findMsg);
                    break;
                case "status":
                    if (args.length != 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    showStatus();
                    break;

                case "checkout":
                    if (args.length == 1) {
                        throw new GitletException("Incorrect operands.");
                    }
                    checkOut(args);
                    break;
                case "branch":
                    if (args.length != 2) {
                        throw new GitletException("Incorrect operands.");
                    }
                    createBranch(args[1]);
                    break;
                case "rm-branch":
                    //  java gitlet.Main rm-branch [branch name]
                    if (args.length != 2) {
                        throw new GitletException("Incorrect operands.");
                    }
                    removeBranch(args[1]);
                    break;
                case "reset":
                    //  java gitlet.Main reset [commit id]
                    if (args.length != 2) {
                        throw new GitletException("Incorrect operands.");
                    }
                    reset(args[1]);
                    break;

                case "merge":
                    if (args.length != 2) {
                        throw new GitletException("Incorrect operands.");
                    }
                    mergeBranch(args[1]);
                    break;
                default:
                    throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.err.println(e.getMessage());
        }
    }
}
