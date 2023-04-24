package com.canvas.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.canvas.lox.TokenType.*;

/**
 * A recursive descent parsing for the lox language
 */
public class Parser {
    // Expressions to parse:
    // expression     → literal
    //               | unary
    //               | binary
    //               | grouping ;
    //
    //literal        → NUMBER | STRING | "true" | "false" | "nil" ;
    //grouping       → "(" expression ")" ;
    //unary          → ( "-" | "!" ) expression ;
    //binary         → expression operator expression ;
    //operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
    //               | "+"  | "-"  | "*" | "/" ;

    // Split and ordered by precedence:
    // expression     → equality
    // equality       → comparison ( ( "!=" | "==" ) comparison )*
    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    // term           → factor ( ( "-" | "+" ) factor )*
    // factor         → unary ( ( "/" | "*" ) unary )*
    // unary          → ( "!" | "-" ) unary
    //               | primary
    // primary        → NUMBER | STRING | "true" | "false" | "nil"
    //               | "(" expression ")"
    //               | IDENTIFIERS

    private final List<Token> tokens;
    /**
     * The next token that's going to be parsed
     */
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parses the source code and return an AST
     *
     * @return The AST, the function returns null in case of an error
     */
    // program        → statement* EOF ;
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLIN, "Expected a ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    // statement      → exprStmt
    //               | printStmt ;
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect } after block");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLIN, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLIN, "Expect ';' after value.");
        return new Stmt.Expression(value);
    }

    // expression    → equality
    private Expr expression() {
        return assignment();

    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target");
        }

        return expr;
    }

    /**
     * Handles equality expressions
     *
     * @return The parsed expression
     */
    // equality     → comparison ( ( "!=" | "==" ) comparison )*
    // equality has the lowest precedence, that's why it's first
    private Expr equality() {
        /*
        Expr expr = comparison();

        // While we have '==' or '!=' operators we keep parsing
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
        */

        return binaryExpr(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    /**
     * Handles comparison expressions
     *
     * @return The parsed expression
     */
    // comparison    → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    // Same as `equality` but with different types and functions
    private Expr comparison() {
        /*
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
         */

        return binaryExpr(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    /**
     * Handles term (addition, subtraction) expressions
     *
     * @return The parsed expression
     */
    // term     → factor ( ( "-" | "+" ) factor )*
    private Expr term() {
        return binaryExpr(this::factor, MINUS, PLUS);   // Order matters!!
    }

    /**
     * Handles factor (division, multiplication) expressions <br />
     * SLASH (division) and STAR (multiplication) are lower in the tree, so they have higher precedence
     *
     * @return The parsed expression
     */
    // factor    → unary ( ( "/" | "*" ) unary )*
    private Expr factor() {
        return binaryExpr(this::unary, SLASH, STAR);    // Again: Order matters!!
    }

    /**
     * Handles unary (negation, boolean reverse?) expressions
     *
     * @return The parsed expression
     */
    // unary          → ( "!" | "-" ) unary
    //               | primary
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // primary        → NUMBER | STRING | "true" | "false" | "nil"
    //               | "(" expression ")"
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PEREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    /**
     * Consumes the expected token, if it's not the expected token an error is thrown with a message
     *
     * @param type    The expected type
     * @param message The error message to be given
     * @return The consumed token
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * A function that creates a ParseError
     *
     * @param token   The token at which the error was found
     * @param message The message that is to be given to the user
     * @return A ParseError (to be caught in synchronisation)
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronise() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLIN) return;

            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
            }

            advance();
        }
    }

    /**
     * Helper method for parsing left-associative series of binary operators
     *
     * @param parserFunction The function to use for parsing the operands
     * @param matchedTypes   The operator types to match for
     * @return The resulting expression
     */
    private Expr binaryExpr(Supplier<Expr> parserFunction, TokenType... matchedTypes) {
        Expr expr = parserFunction.get();

        while (match(matchedTypes)) {
            Token operator = previous();
            Expr right = parserFunction.get();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Checks if the current token has any of the given types
     * if it matches it consumes the Token and advanced to the next
     *
     * @param types The types to match for
     * @return Does one of the type match
     */
    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the current token is of the given type without consuming it
     *
     * @param type the type to match for
     * @return Does the type match
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Advances ths index to the next token in the source
     *
     * @return The consumed token
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Checks if EOF was reached
     *
     * @return The value
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Returns the token at the current index without consuming it
     *
     * @return The token at the current index
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the token at the previous index without consuming it
     *
     * @return The token at the previous index
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private static class ParseError extends RuntimeException {
    }
}
