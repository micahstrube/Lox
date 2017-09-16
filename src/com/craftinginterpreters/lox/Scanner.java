package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 0;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",     AND);
        keywords.put("class",   CLASS);
        keywords.put("else",    ELSE);
        keywords.put("false",   FALSE);
        keywords.put("for",     FOR);
        keywords.put("fun",     FUN);
        keywords.put("if",      IF);
        keywords.put("nil",     NIL);
        keywords.put("or",      OR);
        keywords.put("print",   PRINT);
        keywords.put("return",  RETURN);
        keywords.put("super",   SUPER);
        keywords.put("this",    THIS);
        keywords.put("true",    TRUE);
        keywords.put("var",     VAR);
        keywords.put("while",   WHILE);
    }

    /**
     * Initialize Scanner
     *
     * @param source The source code to be scanned
     */
    Scanner(String source) {
        this.source = source;
    }

    /**
     * Loop through every token and scan each.
     *
     * @return List of tokens
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Advanced one token and scans the token to determine what TokenType it is, and perform
     * corresponding action.
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    // A block comment goes until */ is found or EOF
                    while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {
                        // Still increment line count
                        if (peek() == '\n') {
                            line++;
                        }
                        advance();
                    }
                    if (!isAtEnd()) {
                        advance(); // STAR
                        advance(); // SLASH
                    }
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':
                line++;
                break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    /**
     * Add an IDENTIFIER token.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        // See if the identifier is a reserved word.
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);
        if (type == null) {
            type = IDENTIFIER;
        }
        addToken(type);

        addToken(IDENTIFIER);
    }

    /**
     * Add a NUMBER token.
     */
    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Add a STRING token.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        // Unterminated string
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Check if the current character is the expected character. If so, consume it. If not, return false.
     *
     * @param expected The expected character
     * @return true if the character is the expected character
     */
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }

        current++;
        return true;
    }

    /**
     * Lookahead 1 to the current unconsumed character.
     *
     * @return the current unconsumed character.
     */
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    /**
     * Lookahead 2 to the next unconsumed character.
     *
     * @return the next unconsumed character
     */
    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    /**
     * Check if the specified character is a letter.
     *
     * @param c The character to check.
     * @return  Whether the character is a letter.
     */
    private boolean isAlpha(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                 c == '_';
    }

    /**
     * Check whether the specified character is a letter or a number.
     *
     * @param c The letter to check.
     * @return  Whether the character is a letter or a number.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Return true if the specified character is a numerical digit.
     *
     * @param c The character to check
     * @return True if c is a digit
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns true if at end of file, false if not
     *
     * @return true if at end of file, false if not
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Returns the next char in the input
     *
     * @return next char
     */
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    /**
     * Grab text of a lexeme and create a token for it
     *
     * @param type The TokenType of this token
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Grab text of a lexeme and create a token for it
     *
     * @param type    The TokenType of this token
     * @param literal The literal value of this token
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
