
/**
 * @author Jun
 * CompilationEngine: recursive top-down parser.
 * The Compilation Engine performs syntax analysis of a Jack file.
 * It parses tokens from the JackTokenizer and writes the corresponding XML tags to the output file.
 * 
 * Based on the grammar delineated in section 10.3.3, each method
 * will output XML and make appropriate calls to other methods. This recursive
 * process means that the caller only needs to call compileClass for each file
 * and all other compilations will occur as needed.
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
     */
    public CompilationEngine(File inputFile, File outputFile) {
        try {
            tokenizer = new JackTokenizer(inputFile);
            writer = new PrintWriter(outputFile);
            compileClass();
            close();
        } catch (IOException e) {
            System.out.println("error compilation engine constructor: " + e);
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Writes a given String to the file.
     * 
     * @param s The String/line(s) to be printed WITHOUT the last newline character.
     */
    private void w(String s) {
        writer.println(s);
    }

    /**
     * Closes the output writer. Should be called after compilation is complete.
     */
    private void close() {
        writer.close();
    }

    /**
     * Given a comparison token (&, <, >) returns its XML counterpart (&amp, &lt,
     * &gt)
     */
    private String comparisonSwitch(char token) {
        return switch (token) {
            case '&' -> "&amp;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            default -> "" + token;
        };
    }

    // ========== JACK GRAMMAR COMPILATION METHODS ==========

    /**
     * Compiles a complete class.
     * Grammar: 'class' className '{' classVarDec* subroutineDec* '}'
     * 
     * This is the entry point for parsing a Jack class. It handles the class
     * keyword,
     * class name, opening brace, any number of class variable declarations,
     * any number of subroutine declarations, and the closing brace.
     */
    public void compileClass() {

        // class: 'class' className '{' classVarDec* subroutine* '}'

        w("<class>");

        // 'class'
        tokenizer.advance();
        w("<keyword> class </keyword>");

        // className
        tokenizer.advance();
        w("<identifier> " + tokenizer.identifier() + " </identifier>");

        // '{'
        tokenizer.advance();
        w("<symbol> { </symbol>");

        tokenizer.advance();
        OUTER: while (true) {
            String keyword = tokenizer.keyword();
            switch (keyword) {
                case "static", "field" -> compileClassVarDec();
                case "constructor", "function", "method" -> compileSubroutine();
                default -> {
                    // end of file
                    break OUTER;
                }
            }
        }

        w("<symbol> } </symbol>");
        w("</class>");

        /*
         * The general procedure for any "compile" method is to handle each terminal
         * in order according to the Jack grammar. When a non-terminal is encountered,
         * a method is to be called to handle that.
         * 
         * ie: In this method, we should encounter 3 terminals (the keyword "class",
         * an identifier and the symbol '{'). We will next see a keyword related
         * to classVarDec ("static" or "field") or a keyword related to subroutines
         * ("constructor", "function" or "method"). We loop calling compileClassVarDec()
         * until we have subroutine keywords and loop calling compileSubroutine().
         * We look for the closing '}', close the output file and return.
         */
    }

    /**
     * Compiles a static declaration or a field declaration.
     * Grammar: ('static' | 'field') type varName (',' varName)* ';'
     * 
     * Handles class-level variable declarations. Can be either static variables
     * (class variables) or field variables (instance variables).
     */
    private void compileClassVarDec() {
        // classVarDec: ('static' | 'field') type varName (',' vaName)* ';'
        w("<classVarDec>");
        w("<keyword> " + tokenizer.keyword() + " </keyword>"); // either static or field
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) { // could be a primitive data type or an object
            w("<keyword> " + tokenizer.keyword() + " </keyword>");
        } else {
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
        }
        tokenizer.advance();

        w("<identifier> " + tokenizer.identifier() + " </identifier>"); // regardless of previous decision, the
                                                                        // variable's name comes next
        tokenizer.advance();

        while (tokenizer.symbol() == ',') { // handle multiple variables
            w("<symbol> , </symbol>");
            tokenizer.advance();

            w("<identifier> " + tokenizer.identifier() + " </identifier>");
            tokenizer.advance();
        }

        w("<symbol> ; </symbol>");

        tokenizer.advance();

        w("</classVarDec>");

    }

    /**
     * Compiles a complete method, function, or constructor.
     * Grammar: ('constructor' | 'function' | 'method') ('void' | type)
     * subroutineName
     * '(' parameterList ')' subroutineBody
     * 
     * Handles all three types of subroutines in Jack: constructors (create
     * objects),
     * functions (static methods), and methods (instance methods).
     * 
     * Section 10.3.3 suggests making compileSubroutine. Figure 10.5 does not
     * explicitly define subroutine but we can infer it as being a subroutine
     * declaration (subroutineDec) followed by a subroutine body
     * (subroutineBody), both of which are defined.
     */
    private void compileSubroutine() {
        // subroutine: subroutineDec subroutineBody
        // subroutineDec: ('constructor' | 'function' | 'method')
        // ('void' | type) subroutineName '(' parameterList ')'
        // subroutineBody
        // subroutineBody: '{' varDec* statements '}'

        w("<subroutineDec>");

        w("<keyword> " + tokenizer.keyword() + " </keyword>"); // constructor, function, or method
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) { // keyword or
            w("<keyword> " + tokenizer.keyword() + " </keyword>");
        } else {
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
        }
        tokenizer.advance();

        w("<identifier> " + tokenizer.identifier() + " </identifier>"); // subroutineName
        tokenizer.advance();

        w("<symbol> ( </symbol>"); // '('
        tokenizer.advance();

        compileParameterList(); // parameters

        w("<symbol> ) </symbol>"); // ')'
        tokenizer.advance();

        compileSubroutineBody(); // body

        w("</subroutineDec>");
    }

    /**
     * Compiles a subroutine's body.
     * Grammar: '{' varDec* statements '}'
     * 
     * The body of a subroutine contains local variable declarations followed
     * by the executable statements.
     */
    private void compileSubroutineBody() {
        w("<subroutineBody>");

        w("<symbol> { </symbol>");
        tokenizer.advance();

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("var")) {
            compileVarDec();
        }

        compileStatements();

        w("<symbol> } </symbol>");
        tokenizer.advance();

        w("</subroutineBody>");
    }

    /**
     * Compiles a (possibly empty) parameter list, not including the enclosing "()".
     * Grammar: ((type varName) (',' type varName)*)?
     * 
     * Handles the formal parameters of a subroutine. The parameter list can be
     * empty
     * or contain one or more parameters separated by commas.
     */
    private void compileParameterList() {
        // parameterList: ((type varName) (',' type varName)*)?
        w("<parameterList>");

        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD
                || tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {

            // first type
            if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                w("<keyword> " + tokenizer.keyword() + " </keyword>");
            } else {
                w("<identifier> " + tokenizer.identifier() + " </identifier>");
            }
            tokenizer.advance();

            // first variable
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
            tokenizer.advance();

            // additional variables ", (type) (name)"s
            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                w("<symbol> , </symbol>");
                tokenizer.advance();

                if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
                    w("<keyword> " + tokenizer.keyword() + " </keyword>");
                } else {
                    w("<identifier> " + tokenizer.identifier() + " </identifier>");
                }
                tokenizer.advance();

                w("<identifier> " + tokenizer.identifier() + " </identifier>");
                tokenizer.advance();
            }
        }

        w("</parameterList>");
    }

    /**
     * Compiles a var declaration.
     * Grammar: 'var' type varName (',' varName)* ';'
     * 
     * Handles local variable declarations within subroutines. Multiple variables
     * of the same type can be declared in a single statement.
     */
    private void compileVarDec() {
        // varDec: 'var' type varName (',' type varName)*)?
        w("<varDec>");
        w("<keyword> var </keyword>");
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) { // could be a primitive data type or an object
            w("<keyword> " + tokenizer.keyword() + " </keyword>");
        } else {
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
        }

        tokenizer.advance();

        w("<identifier> " + tokenizer.identifier() + " </identifier>"); // regardless of previous decision, the
                                                                        // variable's name comes next
        tokenizer.advance();

        while (tokenizer.symbol() == ',') { // handle multiple variables
            w("<symbol> , </symbol>");
            tokenizer.advance();

            w("<identifier> " + tokenizer.identifier() + " </identifier>");
            tokenizer.advance();
        }

        w("<symbol> ; </symbol>");
        tokenizer.advance();

        w("</varDec>");
    }

    /**
     * Compiles a sequence of statements, not including the enclosing "{}".
     * Grammar: statement*
     * 
     * A sequence of zero or more statements. This method handles the dispatch
     * to specific statement compilation methods based on the statement type.
     */
    private void compileStatements() {
        // statements: statement*
        w("<statements>");

        while (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            String keyword = tokenizer.keyword();
            switch (keyword) {
                case "let" -> compileLet();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                default -> {
                    return;
                }
            }
        }

        w("</statements>");

    }

    /**
     * Compiles a do statement.
     * Grammar: 'do' subroutineCall ';'
     * 
     * Do statements are used to call subroutines whose return value is discarded.
     * The subroutineCall can be a method call or a function call.
     */
    private void compileDo() {
        // doStatement: 'do' subroutineCall ';'
        w("<doStatement>");

        // do
        w("<keyword> do </keyword>");
        tokenizer.advance();

        // write identifier (subroutineName, className or varName)
        w("<identifier> " + tokenizer.identifier() + " </identifier>");
        tokenizer.advance();

        if (tokenizer.symbol() == '.') {
            w("<symbol> . </symbol>");
            tokenizer.advance();

            // subroutineName
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
            tokenizer.advance();
        }

        w("<symbol> ( </symbol>");
        tokenizer.advance();

        compileExpressionList();

        w("<symbol> ) </symbol>");
        tokenizer.advance();

        // ;
        w("<symbol> ; </symbol>");
        tokenizer.advance();

        w("</doStatement>");
    }

    /**
     * Compiles a let statement.
     * Grammar: 'let' varName ('[' expression ']')? '=' expression ';'
     * 
     * Let statements handle variable assignment. Can assign to simple variables
     * or array elements (indicated by the optional array indexing).
     */
    private void compileLet() {
        // letStatement: 'let' varName ('[' expression ']')? '=' expression ';'

        w("<letStatement>");

        // let
        w("<keyword> let </keyword>");
        tokenizer.advance();

        // varName
        w("<identifier> " + tokenizer.identifier() + " </identifier>");
        tokenizer.advance();

        // [expression]?
        if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == '[') {
            w("<symbol> [ </symbol>");
            tokenizer.advance();

            compileExpression();

            w("<symbol> ] </symbol>");
            tokenizer.advance();
        }

        // =
        w("<symbol> = </symbol>");
        tokenizer.advance();

        // expression
        compileExpression();

        // ;
        w("<symbol> ; </symbol>");
        tokenizer.advance();

        w("</letStatement>");
    }

    /**
     * Compiles a while statement.
     * Grammar: 'while' '(' expression ')' '{' statements '}'
     * 
     * While loops continue executing the statements in the body as long as
     * the condition expression evaluates to true (non-zero).
     */
    private void compileWhile() {
        // whileStatement: 'while' '(' expression ')' '{' statements '}'

        w("<whileStatement>");

        // while
        w("<keyword> while </keyword>");
        tokenizer.advance();

        // (
        w("<symbol> ( </symbol>");
        tokenizer.advance();

        // expression
        compileExpression();

        // )
        w("<symbol> ) </symbol>");
        tokenizer.advance();

        // {
        w("<symbol> { </symbol>");
        tokenizer.advance();

        // statements
        compileStatements();

        // }
        w("<symbol> } </symbol>");
        tokenizer.advance();

        w("</whileStatement>");
    }

    /**
     * Compiles a return statement.
     * Grammar: 'return' expression? ';'
     * 
     * Return statements end subroutine execution. For void subroutines,
     * no expression is provided. For non-void subroutines, an expression
     * specifies the return value.
     */
    private void compileReturn() {
        // returnStatement: 'return' expression? ';'
        w("<returnStatement>");

        // return
        w("<keyword> return </keyword>");
        tokenizer.advance();

        // expression?
        if (tokenizer.tokenType() != JackTokenizer.TokenType.SYMBOL) { // if not ;
            compileExpression();
        }

        // ;
        w("<symbol> ; </symbol>");
        tokenizer.advance();

        w("</returnStatement>");

    }

    /**
     * Compiles an if statement, possibly with a trailing else clause.
     * Grammar: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements
     * '}')?
     * 
     * If statements provide conditional execution. The optional else clause
     * executes when the condition is false (zero).
     */
    private void compileIf() {
        // ifStatement: 'if' '(' expression ')' '{' statements '}'
        // ('else' '{' statements '}')?

        w("<ifStatement>");

        // if
        w("<keyword> if </keyword>");
        tokenizer.advance();

        // (
        w("<symbol> ( </symbol>");
        tokenizer.advance();

        // expression
        compileExpression();

        // )
        w("<symbol> ) </symbol>");
        tokenizer.advance();

        // {
        w("<symbol> { </symbol>");
        tokenizer.advance();

        // statements
        compileStatements();

        // }
        w("<symbol> } </symbol>");
        tokenizer.advance();

        // else?
        if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD && tokenizer.keyword().equals("else")) {
            w("<keyword> else </keyword>");
            tokenizer.advance();

            w("<symbol> { </symbol>");
            tokenizer.advance();

            compileStatements();

            w("<symbol> } </symbol>");
            tokenizer.advance();
        }

        w("</ifStatement>");
    }

    /**
     * Compiles an expression.
     * Grammar: term (op term)*
     * 
     * Expressions are built from terms connected by binary operators.
     * This implements left-associative parsing of binary expressions.
     * Operators: +, -, *, /, &, |, <, >, =
     */
    private void compileExpression() {
        // expression: term (op term)*
        w("<expression>");
        compileTerm();

        while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && isValidSymbol(tokenizer.symbol())) {
            w("<symbol> " + comparisonSwitch(tokenizer.symbol()) + " </symbol>");
            tokenizer.advance();
            compileTerm();
        }

        w("</expression>");

    }

    /**
     * Compiles a term. This routine is faced with a slight difficulty when trying
     * to decide between
     * some of the alternative parsing rules. Specifically, if the current token is
     * an identifier,
     * the routine must distinguish between a variable, an array entry, and a
     * subroutine call.
     * A single look-ahead token, which may be one of "[", "(", or "." suffices to
     * distinguish
     * between the three possibilities. Any other token is not part of this term and
     * should not be
     * advanced over.
     * Grammar: integerConstant | stringConstant | keywordConstant | varName |
     * varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp
     * term
     * 
     * Terms are the basic building blocks of expressions. This method handles
     * all possible term types including constants, variables, array access,
     * subroutine calls, parenthesized expressions, and unary operations.
     */
    private void compileTerm() {
        // term: integerConstant | stringConstant | keywordConstant |
        // varName | varName '[' expression ']' | subroutineCall |
        // '(' expression ')' | unaryOp term

        w("<term>");

        if (tokenizer.tokenType() == JackTokenizer.TokenType.INT_CONST) {
            w("<integerConstant> " + tokenizer.intVal() + " </integerConstant>");
            tokenizer.advance();

        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.STRING_CONST) {
            w("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
            tokenizer.advance();

        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.KEYWORD) {
            w("<keyword> " + tokenizer.keyword() + " </keyword>");
            tokenizer.advance();

        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL
                && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
            w("<symbol> " + tokenizer.symbol() + " </symbol>");
            tokenizer.advance();
            compileTerm();

        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL
                && tokenizer.symbol() == '(') {
            w("<symbol> ( </symbol>");
            tokenizer.advance();
            compileExpression();
            w("<symbol> ) </symbol>");
            tokenizer.advance();

        } else if (tokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
            w("<identifier> " + tokenizer.identifier() + " </identifier>");
            tokenizer.advance();

            if (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL) {
                switch (tokenizer.symbol()) {
                    case '[' -> {
                        w("<symbol> [ </symbol>");
                        tokenizer.advance();
                        compileExpression();
                        w("<symbol> ] </symbol>");
                        tokenizer.advance();
                    }
                    case '(' -> {
                        w("<symbol> ( </symbol>");
                        tokenizer.advance();
                        compileExpressionList();
                        w("<symbol> ) </symbol>");
                        tokenizer.advance();
                    }
                    case '.' -> {
                        w("<symbol> . </symbol>");
                        tokenizer.advance();
                        w("<identifier> " + tokenizer.identifier() + " </identifier>");
                        tokenizer.advance();
                        w("<symbol> ( </symbol>");
                        tokenizer.advance();
                        compileExpressionList();
                        w("<symbol> ) </symbol>");
                        tokenizer.advance();
                    }
                    default -> {
                    }
                }
            }
        }

        w("</term>");
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Grammar: (expression (',' expression)*)?
     * 
     * Used for subroutine call arguments. The list can be empty or contain
     * one or more expressions separated by commas.
     */
    private void compileExpressionList() {
        // expressionList: ( expression (',' expression)* )?
        w("<expressionList>");

        if (!(tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ')')) {
            compileExpression();

            while (tokenizer.tokenType() == JackTokenizer.TokenType.SYMBOL && tokenizer.symbol() == ',') {
                w("<symbol> , </symbol>");
                tokenizer.advance();
                compileExpression();
            }
        }

        w("</expressionList>");
    }

    // ========== HELPER METHODS FOR SPECIFIC CONSTRUCTS ==========

    private boolean isValidSymbol(char c) {
        return "+-*/&|<>=".indexOf(c) != -1;
    }
}