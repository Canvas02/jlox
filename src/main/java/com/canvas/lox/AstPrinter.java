package com.canvas.lox;

/**
 * A visitor class that pretty prints lox ASTs <br/>
 * Note: This may go unmaintained in the future (as it's not maintained in the book) <br/>
 * Usage:
 * <pre>
 * {@code
 *      Expr expression = ...;
 *      System.out.println(new AstPrinter().print(expression));
 * }
 * </pre>
 *
 * @see Expr
 */
public class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        if (expr != null)
            return expr.accept(this);
        else
            return null;
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize(String.format("set %s", expr.name.lexeme), expr.value);

        // Helper.unimplemented();
        // return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);

        // Helper.unimplemented();
        // return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;

        // Helper.unimplemented();
        // return null;
    }

    private String parenthesize(String name, Expr... exprs) {
        var builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    // Temp
    /*
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)
                )
        );

        System.out.println(new AstPrinter().print(expression)); // (* (- 123) (group 45.67))
    }
    */
}
