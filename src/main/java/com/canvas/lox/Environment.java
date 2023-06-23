package com.canvas.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * An object that keeps track of variables, functions and classes
 *
 * @author Canvas02
 */
public class Environment {
    /**
     * The outer environment (scope)
     */
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Defines an object (variable, function, class, etc) in this environment for later use
     *
     * @param name  The name of the defined object
     * @param value The value of the defined object
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Retrieves an object (variable, function, class) from this environment,
     * else it searches the outer (enclosing) environment (recursively).
     * If a variable was not found a <code>RuntimeError</code> is thrown
     *
     * @param name The name of the requested object
     * @return The object (if it exists)
     */
    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, String.format("Undefined variable name '%s'.", name.lexeme));
    }

    /**
     * Assigns an object (variable, function, class) from this environment,
     * else it searches the outer (enclosing) environment (recursively).
     * If a variable was not found a <code>RuntimeError</code> is thrown
     *
     * @param name  The name of the assigned object
     * @param value The value to assign the object to
     */
    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name,
                String.format("Undefined variable '%s'.", name.lexeme)
        );
    }
}
