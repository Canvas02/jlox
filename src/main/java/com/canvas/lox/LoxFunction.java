package com.canvas.lox;

import java.util.List;

/**
 * A wrapper for <code>Stmt.Function</code> that implements <code>LoxCallable</code>
 *
 * @author Canvas02
 * @see Stmt.Function
 */
public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var environment = new Environment(closure);

        // There's no need to check for function arity, since it's checked while interpreting (in visitCallExpr)
        for (int i = 0; i < this.arity(); i++) {
            // Basically a map with (parameter_name: argument)
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
