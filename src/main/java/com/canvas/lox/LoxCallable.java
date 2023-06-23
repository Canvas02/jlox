package com.canvas.lox;

import java.util.List;

/**
 * An Object that is made to be called
 *
 * @author Canvas20
 */
public interface LoxCallable {
    /**
     * The number of arguments the callable object is supported to have
     *
     * @return The function arity
     */
    int arity();

    /**
     * Calls the callable object with the passed <code>Interpreter</code> and arguments
     *
     * @param interpreter The interpreter used to call the callable
     * @param arguments   The arguments passed to the callable
     * @return The return value of the callable
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}
