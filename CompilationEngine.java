/**
 * @author Jun
 * CompilationEngine: recursive top-down parser.
 * The Compilation Engine performs syntax analysis of a Jack file.
 * It parses tokens from the JackTokenizer and writes the corresponding XML tags to the output file.
 */
import java.io.*;

public class CompilationEngine {
    private JackTokenizer tokenizer;
    private PrintWriter writer;

    /**
     * Creates a new compilation engine with the given input and output files.
     * Opens the input file and prepares for parsing.
     * 
     * @param inputFile  the .jack source file to compile
     * @param outputFile the output file for the generated XML
     * @throws IOException if file operations fail
     */
    public CompilationEngine(File inputFile, File outputFile) throws IOException {
        tokenizer = new JackTokenizer(inputFile);
        writer = new PrintWriter(outputFile);
    }

    // ========== UTILITY METHODS ==========

    /**
     * Writes an XML tag with content in the format: <tag> content </tag>
     * 
     * @param tag the XML tag name
     * @param content the content between the tags
     */
    private void writeTag(String tag, String content) {
        writer.printf("<%s> %s </%s>%n", tag, content, tag);
    }

    /**
     * Writes an opening XML tag: <tag>
     * 
     * @param tag the XML tag name
     */
    private void writeOpenTag(String tag) {
        writer.printf("<%s>%n", tag);
    }

    /**
     * Writes a closing XML tag: </tag>
     * 
     * @param tag the XML tag name
     */
    private void writeCloseTag(String tag) {
        writer.printf("</%s>%n", tag);
    }

    /**
     * Writes XML symbol tags, handling special characters that need escaping.
     * Converts <, >, & to their XML entity equivalents.
     * 
     * @param symbol the symbol character to write
     */
    private void writeSymbol(char symbol) {
        String content = switch (symbol) {
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            case '&' -> "&amp;";
            default -> String.valueOf(symbol);
        };
        writeTag("symbol", content);
    }

    /**
     * Writes the current token to XML based on its type.
     * Handles all token types: keyword, symbol, identifier, integer constant, string constant.
     */
    private void writeCurrentToken() {
        switch (tokenizer.tokenType()) {
            case KEYWORD -> writeTag("keyword", tokenizer.keyword());
            case SYMBOL -> writeSymbol(tokenizer.symbol());
            case IDENTIFIER -> writeTag("identifier", tokenizer.identifier());
            case INT_CONST -> writeTag("integerConstant", String.valueOf(tokenizer.intVal()));
            case STRING_CONST -> writeTag("stringConstant", tokenizer.stringVal());
        }
    }

    /**
     * Advances to the next token and writes it to the output.
     * Convenience method for the common pattern of advance + write.
     */
    private void advanceAndWrite() {
        tokenizer.advance();
        writeCurrentToken();
    }

    /**
     * Closes the output writer. Should be called after compilation is complete.
     */
    private void close() {
        writer.close();
    }

    // ========== JACK GRAMMAR COMPILATION METHODS ==========

    /**
     * Compiles a complete class.
     * Grammar: 'class' className '{' classVarDec* subroutineDec* '}'
     * 
     * This is the entry point for parsing a Jack class. It handles the class keyword,
     * class name, opening brace, any number of class variable declarations,
     * any number of subroutine declarations, and the closing brace.
     */
    public void compileClass() {
        writeOpenTag("class");
        
        // 1. Consume 'class' keyword
        advanceAndWrite();
        
        // 2. Consume className (identifier)
        advanceAndWrite();
        
        // 3. Consume '{' symbol
        advanceAndWrite();
        
        // 4. Compile zero or more classVarDec
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                String keyword = tokenizer.keyword();
                if (keyword.equals("static") || keyword.equals("field")) {
                    compileClassVarDec();
                } else if (keyword.equals("constructor") || keyword.equals("function") || keyword.equals("method")) {
                    // 5. Compile zero or more subroutineDec
                    compileSubroutine();
                } else {
                    // 6. Consume '}' symbol (end of class)
                    writeCurrentToken();
                    break;
                }
            }
        }
        
        writeCloseTag("class");
        close();
    }

    /**
     * Compiles a static declaration or a field declaration.
     * Grammar: ('static' | 'field') type varName (',' varName)* ';'
     * 
     * Handles class-level variable declarations. Can be either static variables
     * (class variables) or field variables (instance variables).
     */
    public void compileClassVarDec() {
        writeOpenTag("classVarDec");
        
        // 1. Consume ('static' | 'field') keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume type (keyword or identifier)
        advanceAndWrite();
        
        // 3. Consume varName (identifier)
        advanceAndWrite();
        
        // 4. Handle additional variables separated by commas
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.symbol() == ';') {
                // 5. Consume ';' symbol
                writeCurrentToken();
                break;
            } else if (tokenizer.symbol() == ',') {
                writeCurrentToken(); // write comma
                advanceAndWrite(); // write next variable name
            }
        }
        
        writeCloseTag("classVarDec");
    }

    /**
     * Compiles a complete method, function, or constructor.
     * Grammar: ('constructor' | 'function' | 'method') ('void' | type) subroutineName 
     *          '(' parameterList ')' subroutineBody
     * 
     * Handles all three types of subroutines in Jack: constructors (create objects),
     * functions (static methods), and methods (instance methods).
     */
    public void compileSubroutine() {
        writeOpenTag("subroutineDec");
        
        // 1. Consume ('constructor' | 'function' | 'method') keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume return type ('void' or type)
        advanceAndWrite();
        
        // 3. Consume subroutineName (identifier)
        advanceAndWrite();
        
        // 4. Consume '(' symbol
        advanceAndWrite();
        
        // 5. Compile parameter list
        compileParameterList();
        
        // 6. Consume ')' symbol
        advanceAndWrite();
        
        // 7. Compile subroutine body
        compileSubroutineBody();
        
        writeCloseTag("subroutineDec");
    }

    /**
     * Compiles a (possibly empty) parameter list, not including the enclosing "()".
     * Grammar: ((type varName) (',' type varName)*)?
     * 
     * Handles the formal parameters of a subroutine. The parameter list can be empty
     * or contain one or more parameters separated by commas.
     */
    public void compileParameterList() {
        writeOpenTag("parameterList");
        
        // 1. Check if parameter list is empty (next token is ')')
        tokenizer.advance();
        if (tokenizer.symbol() != ')') {
            // 2. If not empty, compile first parameter (type + varName)
            writeCurrentToken(); // type
            advanceAndWrite(); // varName
            
            // 3. Handle additional parameters separated by commas
            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                if (tokenizer.symbol() == ',') {
                    writeCurrentToken(); // comma
                    advanceAndWrite(); // type
                    advanceAndWrite(); // varName
                } else {
                    // Next token should be ')', don't consume it here
                    tokenizer.retreat();
                    break;
                }
            }
        } else {
            // Parameter list is empty, retreat so ')' can be consumed by caller
            tokenizer.retreat();
        }
        
        writeCloseTag("parameterList");
    }

    /**
     * Compiles a subroutine's body.
     * Grammar: '{' varDec* statements '}'
     * 
     * The body of a subroutine contains local variable declarations followed
     * by the executable statements.
     */
    public void compileSubroutineBody() {
        writeOpenTag("subroutineBody");
        
        // 1. Consume '{' symbol
        advanceAndWrite();
        
        // 2. Compile zero or more local variable declarations
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && 
                tokenizer.keyword().equals("var")) {
                compileVarDec();
            } else {
                // No more var declarations, retreat and compile statements
                tokenizer.retreat();
                break;
            }
        }
        
        // 3. Compile statements
        compileStatements();
        
        // 4. Consume '}' symbol
        advanceAndWrite();
        
        writeCloseTag("subroutineBody");
    }

    /**
     * Compiles a var declaration.
     * Grammar: 'var' type varName (',' varName)* ';'
     * 
     * Handles local variable declarations within subroutines. Multiple variables
     * of the same type can be declared in a single statement.
     */
    public void compileVarDec() {
        writeOpenTag("varDec");
        
        // 1. Consume 'var' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume type (keyword or identifier)
        advanceAndWrite();
        
        // 3. Consume varName (identifier)
        advanceAndWrite();
        
        // 4. Handle additional variables separated by commas
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.symbol() == ';') {
                // 5. Consume ';' symbol
                writeCurrentToken();
                break;
            } else if (tokenizer.symbol() == ',') {
                writeCurrentToken(); // write comma
                advanceAndWrite(); // write next variable name
            }
        }
        
        writeCloseTag("varDec");
    }

    /**
     * Compiles a sequence of statements, not including the enclosing "{}".
     * Grammar: statement*
     * 
     * A sequence of zero or more statements. This method handles the dispatch
     * to specific statement compilation methods based on the statement type.
     */
    public void compileStatements() {
        writeOpenTag("statements");
        
        // Loop through tokens and dispatch to appropriate statement compiler
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                String keyword = tokenizer.keyword();
                switch (keyword) {
                    case "let" -> compileLet();
                    case "if" -> compileIf();
                    case "while" -> compileWhile();
                    case "do" -> compileDo();
                    case "return" -> compileReturn();
                    default -> {
                        // No more statements, retreat
                        tokenizer.retreat();
                        writeCloseTag("statements");
                        return;
                    }
                }
            } else {
                // No more statements, retreat
                tokenizer.retreat();
                break;
            }
        }
        
        writeCloseTag("statements");
    }

    /**
     * Compiles a do statement.
     * Grammar: 'do' subroutineCall ';'
     * 
     * Do statements are used to call subroutines whose return value is discarded.
     * The subroutineCall can be a method call or a function call.
     */
    public void compileDo() {
        writeOpenTag("doStatement");
        
        // 1. Consume 'do' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Compile subroutine call (method or function call)
        compileSubroutineCall();
        
        // 3. Consume ';' symbol
        advanceAndWrite();
        
        writeCloseTag("doStatement");
    }

    /**
     * Compiles a let statement.
     * Grammar: 'let' varName ('[' expression ']')? '=' expression ';'
     * 
     * Let statements handle variable assignment. Can assign to simple variables
     * or array elements (indicated by the optional array indexing).
     */
    public void compileLet() {
        writeOpenTag("letStatement");
        
        // 1. Consume 'let' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume varName (identifier)
        advanceAndWrite();
        
        // 3. Check for optional array indexing: '[' expression ']'
        tokenizer.advance();
        if (tokenizer.symbol() == '[') {
            writeCurrentToken(); // '['
            compileExpression();
            advanceAndWrite(); // ']'
            tokenizer.advance();
        }
        
        // 4. Consume '=' symbol
        writeCurrentToken();
        
        // 5. Compile right-hand side expression
        compileExpression();
        
        // 6. Consume ';' symbol
        advanceAndWrite();
        
        writeCloseTag("letStatement");
    }

    /**
     * Compiles a while statement.
     * Grammar: 'while' '(' expression ')' '{' statements '}'
     * 
     * While loops continue executing the statements in the body as long as
     * the condition expression evaluates to true (non-zero).
     */
    public void compileWhile() {
        writeOpenTag("whileStatement");
        
        // 1. Consume 'while' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume '(' symbol
        advanceAndWrite();
        
        // 3. Compile condition expression
        compileExpression();
        
        // 4. Consume ')' symbol
        advanceAndWrite();
        
        // 5. Consume '{' symbol
        advanceAndWrite();
        
        // 6. Compile statements in loop body
        compileStatements();
        
        // 7. Consume '}' symbol
        advanceAndWrite();
        
        writeCloseTag("whileStatement");
    }

    /**
     * Compiles a return statement.
     * Grammar: 'return' expression? ';'
     * 
     * Return statements end subroutine execution. For void subroutines,
     * no expression is provided. For non-void subroutines, an expression
     * specifies the return value.
     */
    public void compileReturn() {
        writeOpenTag("returnStatement");
        
        // 1. Consume 'return' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Check if there's an expression (next token is not ';')
        tokenizer.advance();
        if (tokenizer.symbol() != ';') {
            // 3. If expression exists, compile it
            tokenizer.retreat();
            compileExpression();
            tokenizer.advance();
        }
        
        // 4. Consume ';' symbol
        writeCurrentToken();
        
        writeCloseTag("returnStatement");
    }

    /**
     * Compiles an if statement, possibly with a trailing else clause.
     * Grammar: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     * 
     * If statements provide conditional execution. The optional else clause
     * executes when the condition is false (zero).
     */
    public void compileIf() {
        writeOpenTag("ifStatement");
        
        // 1. Consume 'if' keyword (already at current token)
        writeCurrentToken();
        
        // 2. Consume '(' symbol
        advanceAndWrite();
        
        // 3. Compile condition expression
        compileExpression();
        
        // 4. Consume ')' symbol
        advanceAndWrite();
        
        // 5. Consume '{' symbol
        advanceAndWrite();
        
        // 6. Compile statements in if body
        compileStatements();
        
        // 7. Consume '}' symbol
        advanceAndWrite();
        
        // 8. Check for optional 'else' clause
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && 
                tokenizer.keyword().equals("else")) {
                // 9. If 'else' exists: consume 'else', '{', compile statements, consume '}'
                writeCurrentToken(); // 'else'
                advanceAndWrite(); // '{'
                compileStatements();
                advanceAndWrite(); // '}'
            } else {
                // No else clause, retreat
                tokenizer.retreat();
            }
        }
        
        writeCloseTag("ifStatement");
    }

    /**
     * Compiles an expression.
     * Grammar: term (op term)*
     * 
     * Expressions are built from terms connected by binary operators.
     * This implements left-associative parsing of binary expressions.
     * Operators: +, -, *, /, &, |, <, >, =
     */
    public void compileExpression() {
        writeOpenTag("expression");
        
        // 1. Compile first term
        compileTerm();
        
        // 2. While next token is a binary operator
        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (isBinaryOperator()) {
                // a. Consume and write the operator
                writeCurrentToken();
                // b. Compile the next term
                compileTerm();
            } else {
                // Not an operator, retreat
                tokenizer.retreat();
                break;
            }
        }
        
        writeCloseTag("expression");
    }

    /**
     * Compiles a term. This routine is faced with a slight difficulty when trying to decide between
     * some of the alternative parsing rules. Specifically, if the current token is an identifier,
     * the routine must distinguish between a variable, an array entry, and a subroutine call.
     * A single look-ahead token, which may be one of "[", "(", or "." suffices to distinguish
     * between the three possibilities. Any other token is not part of this term and should not be
     * advanced over.
     * Grammar: integerConstant | stringConstant | keywordConstant | varName | 
     *          varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term
     * 
     * Terms are the basic building blocks of expressions. This method handles
     * all possible term types including constants, variables, array access,
     * subroutine calls, parenthesized expressions, and unary operations.
     */
    public void compileTerm() {
        writeOpenTag("term");
        
        tokenizer.advance();
        
        switch (tokenizer.tokenType()) {
            case INT_CONST, STRING_CONST -> {
                // Integer or string constant
                writeCurrentToken();
            }
            case KEYWORD -> {
                if (isKeywordConstant()) {
                    // Keyword constant (true, false, null, this)
                    writeCurrentToken();
                }
            }
            case IDENTIFIER -> {
                // Could be variable, array access, or subroutine call
                // Need to look ahead to determine which
                if (tokenizer.hasMoreTokens()) {
                    tokenizer.advance();
                    if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL) {
                        char nextChar = tokenizer.symbol();
                        tokenizer.retreat();
                        
                        if (nextChar == '[') {
                            // Array access: varName '[' expression ']'
                            writeCurrentToken(); // varName
                            advanceAndWrite(); // '['
                            compileExpression();
                            advanceAndWrite(); // ']'
                        } else if (nextChar == '(' || nextChar == '.') {
                            // Subroutine call
                            tokenizer.retreat();
                            compileSubroutineCall();
                        } else {
                            // Simple variable
                            writeCurrentToken();
                        }
                    } else {
                        // Next token is not a symbol, so this is a simple variable
                        tokenizer.retreat();
                        writeCurrentToken();
                    }
                } else {
                    // Simple variable (no lookahead possible)
                    writeCurrentToken();
                }
            }
            case SYMBOL -> {
                char symbol = tokenizer.symbol();
                if (symbol == '(') {
                    // Parenthesized expression: '(' expression ')'
                    writeCurrentToken(); // '('
                    compileExpression();
                    advanceAndWrite(); // ')'
                } else if (isUnaryOperator()) {
                    // Unary operation: unaryOp term
                    writeCurrentToken(); // unary operator
                    compileTerm(); // recursive call for the term
                }
            }
        }
        
        writeCloseTag("term");
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Grammar: (expression (',' expression)*)?
     * 
     * Used for subroutine call arguments. The list can be empty or contain
     * one or more expressions separated by commas.
     */
    public void compileExpressionList() {
        writeOpenTag("expressionList");
        
        // 1. Check if list is empty (next token might be closing delimiter)
        tokenizer.advance();
        if (tokenizer.symbol() != ')') {
            // 2. If not empty, compile first expression
            tokenizer.retreat();
            compileExpression();
            
            // 3. While next token is ','
            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                if (tokenizer.symbol() == ',') {
                    // a. Consume ',' symbol
                    writeCurrentToken();
                    // b. Compile next expression
                    compileExpression();
                } else {
                    // No more expressions, retreat
                    tokenizer.retreat();
                    break;
                }
            }
        } else {
            // Expression list is empty, retreat so ')' can be consumed by caller
            tokenizer.retreat();
        }
        
        writeCloseTag("expressionList");
    }

    // ========== HELPER METHODS FOR SPECIFIC CONSTRUCTS ==========

    /**
     * Compiles a subroutine call.
     * Grammar: subroutineName '(' expressionList ')' | 
     *          (className | varName) '.' subroutineName '(' expressionList ')'
     * 
     * Handles both direct subroutine calls and method calls on objects.
     * This is a helper method used by compileDo and compileTerm.
     */
    private void compileSubroutineCall() {
        // First identifier (subroutineName or className|varName)
        advanceAndWrite();
        
        // Check what follows the identifier
        tokenizer.advance();
        if (tokenizer.symbol() == '.') {
            // Form 2: (className | varName) '.' subroutineName '(' expressionList ')'
            writeCurrentToken(); // '.'
            advanceAndWrite(); // subroutineName
            advanceAndWrite(); // '('
            compileExpressionList();
            advanceAndWrite(); // ')'
        } else if (tokenizer.symbol() == '(') {
            // Form 1: subroutineName '(' expressionList ')'
            writeCurrentToken(); // '('
            compileExpressionList();
            advanceAndWrite(); // ')'
        }
    }

    /**
     * Checks if the current token is a binary operator.
     * Operators: +, -, *, /, &, |, <, >, =
     * 
     * @return true if current token is a binary operator
     */
    private boolean isBinaryOperator() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL) {
            return false;
        }
        char symbol = tokenizer.symbol();
        return symbol == '+' || symbol == '-' || symbol == '*' || symbol == '/' ||
               symbol == '&' || symbol == '|' || symbol == '<' || symbol == '>' ||
               symbol == '=';
    }

    /**
     * Checks if the current token is a unary operator.
     * Operators: -, ~
     * 
     * @return true if current token is a unary operator
     */
    private boolean isUnaryOperator() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL) {
            return false;
        }
        char symbol = tokenizer.symbol();
        return symbol == '-' || symbol == '~';
    }

    /**
     * Checks if the current token is a keyword constant.
     * Constants: true, false, null, this
     * 
     * @return true if current token is a keyword constant
     */
    private boolean isKeywordConstant() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD) {
            return false;
        }
        String keyword = tokenizer.keyword();
        return keyword.equals("true") || keyword.equals("false") || 
               keyword.equals("null") || keyword.equals("this");
    }

    /**
     * Checks if the current token is a statement keyword.
     * Keywords: let, if, while, do, return
     * 
     * @return true if current token starts a statement
     */
    private boolean isStatementKeyword() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD) {
            return false;
        }
        String keyword = tokenizer.keyword();
        return keyword.equals("let") || keyword.equals("if") || keyword.equals("while") ||
               keyword.equals("do") || keyword.equals("return");
    }

    /**
     * Checks if the current token is a type keyword.
     * Keywords: int, char, boolean
     * 
     * @return true if current token is a built-in type
     */
    private boolean isTypeKeyword() {
        if (tokenizer.tokenType() != JackTokenizer.TokenType.KEYWORD) {
            return false;
        }
        String keyword = tokenizer.keyword();
        return keyword.equals("int") || keyword.equals("char") || keyword.equals("boolean");
    }
}