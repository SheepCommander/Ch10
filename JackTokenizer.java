
/**
 * @author Jun
 * JackTokenizer: tokenizer;
 * The JackTokenizer breaks a .jack source file into a stream of tokens.
 * It removes comments and whitespace, and provides token type information
 * and access methods for each token.
 */
import java.io.*;
import java.util.*;

public class JackTokenizer {
    private List<String> tokens = new ArrayList<>();
    private int currentIndex = -1;
    private String currentToken = "";
    private TokenType currentType;

    /**
     * Represents all the possible token types in Jack.
     */
    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    } // T_KEYWORD T_INT K_CHAR K_BOOLEAN

    // Set of all Jack language keywords
    public static final Set<String> KEYWORDS = Set.of(
            "class", "constructor", "function", "method", "field", "static", "var",
            "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return");

    // Set of all symbols used in Jack
    private static final Set<Character> SYMBOLS = Set.of(
            '{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/',
            '&', '|', '<', '>', '=', '~');

    /**
     * Constructor that reads and tokenizes the Jack source file thats given.
     * 
     * @param inputFile the .jack file to tokenize
     * @throws IOException if file read fails
     */
    public JackTokenizer(File inputFile) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        boolean inBlockComment = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Handle block comments
            if (inBlockComment) {
                if (line.contains("*/")) {
                    inBlockComment = false;
                    line = line.substring(line.indexOf("*/") + 2);
                } else {
                    continue;
                }
            }

            if (line.startsWith("/*")) {
                inBlockComment = true;
                if (line.contains("*/")) {
                    inBlockComment = false;
                    line = line.substring(line.indexOf("*/") + 2);
                } else {
                    continue;
                }
            }

            // Remove inline comments
            line = line.replaceAll("//.*", "");
            content.append(line).append(" ");
        }
        reader.close();
        tokenize(content.toString());
    }

    /**
     * Splits the input string into Jack language tokens using regex
     * 
     * @param input the cleaned source code as a single string
     */
    private void tokenize(String input) {
        String regex = "\".*?\"|\\d+|\\w+|\\S"; // match strings, numbers, words, or symbols
        Scanner scanner = new Scanner(input);
        while (scanner.findWithinHorizon(regex, 0) != null) {
            tokens.add(scanner.match().group());
        }
        scanner.close();
    }

    /**
     * Checks if there are more tokens to process
     * 
     * @return true if more tokens exist, false otherwise
     */
    public boolean hasMoreTokens() {
        return currentIndex + 1 < tokens.size();
    }

    /**
     * Advances to the next token and updates its type
     */
    public void advance() {
        if (hasMoreTokens()) {
            currentToken = tokens.get(++currentIndex);

            if (KEYWORDS.contains(currentToken))
                currentType = TokenType.KEYWORD;
            else if (currentToken.length() == 1 && SYMBOLS.contains(currentToken.charAt(0)))
                currentType = TokenType.SYMBOL;
            else if (currentToken.matches("\\d+"))
                currentType = TokenType.INT_CONST;
            else if (currentToken.startsWith("\""))
                currentType = TokenType.STRING_CONST;
            else
                currentType = TokenType.IDENTIFIER;
        }
    }

    /**
     * Retreats the tokenizer by one token
     */
    public void retreat() {
        if (currentIndex > 0) {
            currentIndex--;
            advance(); // Reevaluate the type
            currentIndex--;
        }
    }

    /**
     * Returns the type of the current token
     */
    public TokenType tokenType() {
        return currentType;
    }

    /**
     * Returns the keyword of the current token
     */
    public String keyword() {
        return currentToken;
    }

    /**
     * Returns the symbol character of the current token
     */
    public char symbol() {
        return currentToken.charAt(0);
    }

    /**
     * Returns the identifier of the current token
     */
    public String identifier() {
        return currentToken;
    }

    /**
     * Returns the integer value of the current token
     */
    public int intVal() {
        return Integer.parseInt(currentToken);
    }

    /**
     * Returns the string constant value of the current token (without quotes)
     */
    public String stringVal() {
        return currentToken.replaceAll("\"", "");
    }

    /**
     * Writes all tokens in XML format to a file using a PrintWriter
     * 
     * @param writer the output writer for the token XML
     */
    public void writeTokens(PrintWriter writer) {
        writer.println("<tokens>"); // start of file
        while (hasMoreTokens()) {
            advance();
            switch (tokenType()) {
                case KEYWORD -> writer.printf("<keyword> %s </keyword>%n", keyword());
                case SYMBOL -> {
                    String s = switch (symbol()) {
                        case '<' -> "&lt;";
                        case '>' -> "&gt;";
                        case '&' -> "&amp;";
                        default -> String.valueOf(symbol());
                    };
                    writer.printf("<symbol> %s </symbol>%n", s);
                }
                case IDENTIFIER -> writer.printf("<identifier> %s </identifier>%n", identifier());
                case INT_CONST -> writer.printf("<integerConstant> %d </integerConstant>%n", intVal());
                case STRING_CONST -> writer.printf("<stringConstant> %s </stringConstant>%n", stringVal());
            }
        }
        writer.println("</tokens>"); // end of file.
    }
}