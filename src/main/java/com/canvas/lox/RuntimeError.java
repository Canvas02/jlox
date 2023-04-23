package com.canvas.lox;

/**
 * A Lox runtime error
 */
public class RuntimeError extends RuntimeException{
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
