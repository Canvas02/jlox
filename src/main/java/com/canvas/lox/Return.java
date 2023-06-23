package com.canvas.lox;

/**
 * A wrapper around the return value of a function/method, used to unwind the stack
 *
 * @author Canvas02
 */
public class Return extends RuntimeException {
    /**
     * The return value
     */
    final Object value;

    Return(Object value) {
        // disables some JVM machinery, since we are using this class for control flow and not error handling
        super(null, null, false, false);

        this.value = value;
    }
}
