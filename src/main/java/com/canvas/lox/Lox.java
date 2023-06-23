package com.canvas.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * The lox command line tool
 *
 * @author Canvas02
 */
public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(64);
        if (hadRuntimeError) System.exit(70);
    }

    // REPL
    private static void runPrompt() throws IOException {
        var input = new InputStreamReader(System.in);
        var reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);

            hadError = false;
        }
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        var parser = new Parser(tokens);
        var statements = parser.parse();    // Can throw a ParseError

        // Stop if there was an error
        if (hadError) return;

        // System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(
                    token.line,
                    String.format(" at '%s'", token.lexeme),
                    message
            );
        }
    }

    /**
     * Used when a runtime error is hit, prints to stderr and enters 'panic mode'
     *
     * @param error The runtime error
     */
    public static void runtimeError(RuntimeError error) {
        System.err.printf("%s\n[line %d]\n", error.getMessage(), error.token.line);
        hadRuntimeError = true;
    }

    /**
     * Used when a error is hit, prints to stderr and enters 'panic mode'
     *
     * @param line  The line where the error occurred
     * @param where Where the error occurred (at the start, at the end, etc..)
     * @param msg   The error message
     */
    private static void report(int line, String where, String msg) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + msg
        );
        hadError = true;
    }

}