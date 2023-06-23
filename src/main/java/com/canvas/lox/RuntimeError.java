package com.canvas.lox;

/**
 * A Lox runtime error
 */
public class RuntimeError extends RuntimeException {
    /**
     * The Token at which the error happened
     */
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
