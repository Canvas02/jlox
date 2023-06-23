package com.canvas.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.canvas.lox.TokenType.*;

/**
 * A recursive descent parsing for the lox language
 *
 * @author Canvas02
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
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, String.format("Expected %s name.", kind));
        consume(LEFT_PEREN, String.format("Expected '(' after %s name.", kind));

        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters");
                }

                parameters.add(consume(IDENTIFIER, "Expected parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expected ')' after parameters.");

        consume(LEFT_BRACE, String.format("Expected '{' before %s body.", kind));
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
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
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLIN)) {
            value = expression();
        }

        consume(SEMICOLIN, "Expect ';' after return value;");
        return new Stmt.Return(keyword, value);
    }

    // Syntactic Sugar
    private Stmt forStatement() {
        consume(LEFT_PEREN, "Except '(' after while.");

        // for (var i = 0; i <= 10; i = i + 1) { ... }

        Stmt initializer;
        if (match(SEMICOLIN)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLIN)) {
            condition = expression();
        }
        consume(SEMICOLIN, "Expect ';' after condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }

        consume(RIGHT_PAREN, "Except ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PEREN, "Except '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Except ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(LEFT_PEREN, "Expect '(' after if.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * Parses a block (not including the first <code>LEFT_BRACE</code>) until
     * <code>RIGHT_BRACE</code> (which it consumes)
     * <br/>
     * <p>
     * Note: The reason for this design is for better error handling and
     * error massages
     * </p>
     *
     * @return A list of statements that are in the block
     */
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

    /**
     * Parses the following expression while consuming it
     *
     * @return The consumed expression
     */
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

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

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PEREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }

                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");

        return new Expr.Call(callee, paren, arguments);
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
