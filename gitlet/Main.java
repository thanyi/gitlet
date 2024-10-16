package gitlet;

import static gitlet.Repository.*;
import static gitlet.Utils.message;
import static java.lang.System.exit;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author TODO
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

        switch (firstArg) {
            case "init":
                //
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                initPersistence();
                break;
            case "add":
                // TODO: handle the `add [filename]` command\
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
                    message("Incorrect operands.");
                    exit(0);
                }
                printLog();
                break;
            case "global-log":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                printGlobalLog();
                break;
            case "find":
                String findMsg = args[1];
                findCommit(findMsg);
                break;
            case "status":
                if (args.length != 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
//                findCommit(findMsg);
                showStatus();
                break;

            case "checkout":
                if (args.length == 1) {
                    message("Incorrect operands.");
                    exit(0);
                }
                checkOut(args);
                break;
            case "branch":
                if (args.length != 2) {
                    message("Incorrect operands.");
                    exit(0);
                }
                createBranch(args[1]);
                break;
            case "rm-branch":
                //  java gitlet.Main rm-branch [branch name]
                if (args.length != 2) {
                    message("Incorrect operands.");
                    exit(0);
                }
                removeBranch(args[1]);
                break;
            case "reset":
                //  java gitlet.Main reset [commit id]
                if (args.length != 2) {
                    message("Incorrect operands.");
                    exit(0);
                }
                reset(args[1]);
                break;

            case "merge":
                if (args.length != 2) {
                    message("Incorrect operands.");
                    exit(0);
                }
                mergeBranch(args[1]);
                break;
            default:
                message("No command with that name exists.");
                exit(0);
                // TODO: FILL THE REST IN
        }
    }
}
