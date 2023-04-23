package com.canvas.lox;

public class Interpreter implements Expr.Visitor<Object> {
    public void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // Comparison
            case GREATER -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left <= (double) right;
            }

            // Equality
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }

            // Arithmetic
            case MINUS -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                // else
                throw new RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings"
                );
            }

            case SLASH -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, right, left);
                return (double) left * (double) right;
            }
        }

        // Unreachable
        Helper.unreachable();
        return null;
    }


    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }
            case BANG -> {
                return isTruthy(right);
            }
        }

        // Unreachable
        Helper.unreachable();
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object right, Object left) {
        if (right instanceof Double && left instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be a numbers");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;

        return true;
    }

    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }
}
