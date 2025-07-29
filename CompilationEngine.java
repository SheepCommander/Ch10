
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

    public void debug(String keyword) {
        System.out.println("\tkeyword: " + keyword);
    }


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
            writer.close();
        } catch (IOException e) {
            System.out.println("error compilation engine constructor: " + e);
        }
    }

    /**
     * Writes a given String to the file.
     * 
     * @param s The String/line(s) to be printed WITHOUT the last newline character.
     */
    private void w(String s) {
        writer.println(s);
    }

    private String comparisonSwitch(char token) {
        return switch (token) {
            case '&' -> "&amp;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            default -> "" + token;
        };
    }

    /**
     * compileClass() is the only public method. All other methods are called
     * using recursive descent parsing. *
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
     * Section 10.3.3 suggests making compileSubroutine. Figure 10.5 does not
     * explicitly define subroutine but we can infer it as being a subroutine
     * declaration (subroutineDec) followed by a subroutine body
     * (subroutineBody), both of which are defined. *
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

    private boolean isValidSymbol(char c) {
        return "+-*/&|<>=".indexOf(c) != -1;
    }

    /**
     * Near the end of section 10.1.3, it is mentioned that the Jack grammer is
     * "almost" LL(0). The exception being that lookahead is required for the
     * parsing of expressions. Specifically, a subroutineCall starts with an
     * identifier which makes it impossible to differentiate from varName
     * without more context either in terms of a pre-populated symbol table or a
     * lookahead. Subroutine call identifiers are always followed by an '('.
     * Looking ahead one token resolves the problem. *
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
}