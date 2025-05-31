package gitlet;

import static gitlet.Repository.GITLET_DIR;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        if (!GITLET_DIR.exists() && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Repository repo = new Repository();
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                validateNumArgs(args, 1);
                repo.init();
                break;
            case "add":
                validateNumArgs(args, 2);
                repo.add(args[1]);
                break;
            case "commit":
                validateNumArgs(args, 2);
                repo.commit(args[1]);
                break;
            case "log":
                validateNumArgs(args, 1);
                repo.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                repo.globalLog();
                break;
            case "find":
                validateNumArgs(args, 2);
                repo.find(args[1]);
                break;
            case "status":
                validateNumArgs(args, 1);
                repo.status();
                break;
            case "checkout":
                handleCheckout(repo, args);
                break;
            case "branch":
                validateNumArgs(args, 2);
                repo.createBranch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs(args, 2);
                repo.removeBranch(args[1]);
                break;
            case "rm":
                validateNumArgs(args, 2);
                repo.remove(args[1]);
                break;
            case "reset":
                validateNumArgs(args, 2);
                repo.reset(args[1]);
                break;
            case "merge":
                validateNumArgs(args, 2);
                repo.merge(args[1]);
                break;
            case "add-remote":
                validateNumArgs(args, 3);
                repo.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validateNumArgs(args, 2);
                repo.removeRemote(args[1]);
                break;
            case "push":
                validateNumArgs(args, 3);
                repo.push(args[1], args[2]);
                break;
            case "init-remote":
                validateNumArgs(args, 2);
                repo.initRemote(args[1]);
                break;
            case "fetch":
                validateNumArgs(args, 3);
                repo.fetch(args[1], args[2]);
                break;
            case "pull":
                validateNumArgs(args, 3);
                repo.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    private static void validateNumArgs(String[] args, int expected) {
        if (args.length != expected) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static void handleCheckout(Repository repo, String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            repo.checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            repo.checkoutFileFromCommit(args[1], args[3]);
        } else if (args.length == 2) {
            repo.checkoutBranch(args[1]);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
