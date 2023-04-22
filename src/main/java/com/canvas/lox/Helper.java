package com.canvas.lox;

/**
 * General helper functions
 */
public class Helper {
    /**
     * Get the current line number.
     *
     * @return int - Current line number.
     */
    // Taken from: https://stackoverflow.com/a/115027/16854783
    public static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }

    /**
     * Used to mark unreachable code, if ever reached it prints filename + line and exist with -1
     */
    public static void unreachable() {
        // Can't use getLineNumber here, because then that would add to the stack
        System.err.printf(
                "fatal: unreachable code reached at %s\n",
                Thread.currentThread().getStackTrace()[2].toString()
        );
        System.exit(-1);
    }

    public static void unimplemented() {
        // Can't use getLineNumber here, because then that would add to the stack
        System.err.printf(
                "fatal: unimplemented code reached at %s\n",
                Thread.currentThread().getStackTrace()[2].toString()
        );
        System.exit(-2);
    }
}
