/**
 * @author Jun
 * Chapter 10 nand2tetris
 * JackAnalyzer: top-level driver that sets up and invokes the other modules;
 * 
 *  The analyzer program operates on a given source, where source is either a file name of the form Xxx.jack
 * or a directory name containing one or more such files. For each source Xxx.jack file, the analyzer goes
 * through the following logic:
 *  1. Create a JackTokenizer from the Xxx.jack input file.
 *  2. Create an output file called Xxx.xml and prepare it for writing.
 *  3. Use the CompilationEngine to compile the input JackTokenizer into the output file
 */
import java.io.*;

public class JackAnalyzer {

    /**
     * Main method to run the tokenizer.
     * @param args The command line argument: inputFile.jack
     * @throws IOException if input/output operations fail
     */
    public static void main(String[] args) throws IOException {
        // Exit if no arguments
        if (args.length == 0) {
            System.out.println("Error: Filepath required.\n");
            return;
        }

        // Argument variables
        String filePath = args[0];

        // Prepare Input & Output files
        File input = new File(filePath);
        File[] inputFiles;
        
        // Convert input path into an array of files
        if (input.isFile()) {
            inputFiles = new File[1];
            inputFiles[0] = input;
        } else {
            inputFiles = input.listFiles();
        }

        // Process the input files in a loop!
        for (File fileI : inputFiles) {
            // Only process Jack files
            if (!fileI.getName().contains(".jack")) {
                continue;
            }
            // Ch10 Stage 1:
            JackTokenizer tokenizer = new JackTokenizer(fileI); // Step 1
            PrintWriter writer = new PrintWriter(new File(fileI.getPath().replace(".jack", "T.xml"))); // Step 2 

            // Tokenize the input file and write each token to the XML
            writer.println("<tokens>");
            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                switch (tokenizer.tokenType()) {
                    case KEYWORD -> writer.printf("<keyword> %s </keyword>%n", tokenizer.keyword());
                    case SYMBOL -> {
                        // Handle special xml characters
                        char symbol = tokenizer.symbol();
                        String s = switch (symbol) {
                            case '<' -> "&lt;";
                            case '>' -> "&gt;";
                            case '&' -> "&amp;";
                            default -> String.valueOf(symbol);
                        };
                        writer.printf("<symbol> %s </symbol>%n", s);
                    }
                    // print the formatted tokens in xml format
                    case IDENTIFIER -> writer.printf("<identifier> %s </identifier>%n", tokenizer.identifier());
                    case INT_CONST -> writer.printf("<integerConstant> %d </integerConstant>%n", tokenizer.intVal());
                    case STRING_CONST -> writer.printf("<stringConstant> %s </stringConstant>%n", tokenizer.stringVal());
                }
            }
            writer.println("</tokens>");
            writer.close();
            System.out.println("Token XML generated");

            // Ch10 Stage 2: CompilationEngine
            CompilationEngine compiler = new CompilationEngine(fileI, new File(fileI.getPath().replace(".jack", ".xml")));
            compiler.compileClass();
            System.out.println("Compiled XML generated");
        }
    }
}