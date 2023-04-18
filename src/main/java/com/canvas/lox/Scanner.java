package com.canvas.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.canvas.lox.TokenType.*;

/**
 * A Tokenizer for the Lox language <br/>
 * Usage:
 * <pre>
 * {@code
 *     var scanner = new Scanner(sourceCode);
 *     List<Token> tokens = scanner.scanTokens();
 *
 *      for (var token : tokens) {
 *      System.out.println(token);
 * }
 * </pre>
 */
public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    /**
     * Constructs a Scanner (Tokenizer)
     * @param source The source code for the wanted program
     */
    Scanner(String source) {
        this.source = source;
    }

    /**
     * Scans the source to tokens
     * @return The list of token scanned from the source
     */
    List<Token> scanTokens() {
        // Iterate through the whole file
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line)); // Add an End-of-file token
        return tokens;
    }

    /**
     * Checks if EOF was reached
     * @return The value
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Scans the current token, advances the current value (index), and adds it to a list
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single character lexemes
            case '(': addToken(LEFT_PEREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLIN); break;
            case '*': addToken(STAR); break;

            // Single or double character lexemes
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // Longer lexemes
            case '/':
                if (match('/')) {   // a line comment (spans until the end of the line)
                    // Skip until end of line
                    // TODO: Support other line endings (ie. CRLF, ..)
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Meaningless characters
            // Ignore whitespace
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // Strings
            case '"': string(); break;

            // Reserved words
            case 'o': // OR
                if (match('r')) {
                    addToken(OR);
                }
                break;

            default: // Contains number handling + invalid input handling
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) { // checks for an identifier
                    identifier();
                } else {
                Lox.error(line, "Unexpected character: " + c);
                }
                break;
        }
    }

    /**
     * Handles identifier
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;

        addToken(type);
    }

    /**
     * Handles number literals
     */
    private void number() {
        while (isDigit(peek())) advance();

        // look for a DOT in the middle of a number
        if (peek() == '.' && isDigit(peekNext())) {
            advance();  // consume the '.'

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Handles string literals
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        advance();  // The closing "

        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Advances ths index to the next character in the source
     * @return The consumed character
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Adds a token to the list of tokens stored, with a literal of null
     * @param type The token type
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Adds a token to the list of tokens stored
     * @param type The token type
     * @param literal Only used by literals for the value of a literal
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /**
     * Checks if the provided character match the one in the source,
     * if it matches it consumes the character and advanced to the next
     * @param expected The expected character
     * @return The result
    */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Returns the character at the current index without consuming it
     * @return The character at the current index
    */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Returns the next character from the current index without consuming it
     * @return The next character from the current index
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0'; // EOF
        return source.charAt(current + 1);
    }

    /**
     * Checks if a character is an alphabet character and not a number or symbol
     * @param c The checked character
     * @return  The result
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    /**
     * Checks if a character is a number or an alphabet character and not a symbol
     * @param c The checked character
     * @return  The result
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Checks if a character is a number and not a alphabet character or symbol
     * @param c The checked character
     * @return  The result
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
