package cool.compiler;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import cool.lexer.*;
import cool.parser.*;
import cool.structures.SymbolTable;
import cool.AST.*;

import java.io.*;

public class Compiler {
    
    public static ParseTreeProperty<String> fileNames = new ParseTreeProperty<>();

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("No file(s) given");
            return;
        }

        CoolLexer lexer = null;
        CommonTokenStream tokenStream = null;
        CoolParser parser = null;
        ParserRuleContext globalTree = null;

        
        boolean lexicalSyntaxErrors = false;

        
        
        for (var fileName : args) {
            var input = CharStreams.fromFileName(fileName);

            
            if (lexer == null)
                lexer = new CoolLexer(input);
            else
                lexer.setInputStream(input);

            
            if (tokenStream == null)
                tokenStream = new CommonTokenStream(lexer);
            else
                tokenStream.setTokenSource(lexer);

             

            
            if (parser == null)
                parser = new CoolParser(tokenStream);
            else
                parser.setTokenStream(tokenStream);

            
            
            var errorListener = new BaseErrorListener() {
                public boolean errors = false;

                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                        Object offendingSymbol,
                        int line, int charPositionInLine,
                        String msg,
                        RecognitionException e) {
                    String newMsg = "\"" + new File(fileName).getName() + "\", line " +
                            line + ":" + (charPositionInLine + 1) + ", ";

                    Token token = (Token) offendingSymbol;
                    if (token.getType() == CoolLexer.ERROR)
                        newMsg += "Lexical error: " + token.getText();
                    else
                        newMsg += "Syntax error: " + msg;

                    System.err.println(newMsg);
                    errors = true;
                }
            };

            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            
            var tree = parser.program();
            if (globalTree == null)
                globalTree = tree;
            else
                
                for (int i = 0; i < tree.getChildCount(); i++)
                    globalTree.addAnyChild(tree.getChild(i));

            
            
            for (int i = 0; i < tree.getChildCount(); i++) {
                var child = tree.getChild(i);
                
                
                if (child instanceof ParserRuleContext)
                    fileNames.put(child, fileName);
            }

            
            lexicalSyntaxErrors |= errorListener.errors;
        }

        
        if (lexicalSyntaxErrors) {
            System.err.println("Compilation halted");
            return;
        }

        
        SymbolTable.defineBasicClasses();

        

        
        ASTBuilderVisitor astBuilder = new ASTBuilderVisitor();

        var programAST = astBuilder.visit(globalTree);

        
        
        if (programAST != null) {
            DefinitionPassVisitor definitionPass = new DefinitionPassVisitor();
            programAST.accept(definitionPass);
        }

        
        if (SymbolTable.hasSemanticErrors()) {
            System.err.println("Compilation halted");
            return;
        }

        ResolutionPassVisitor resolutionPass = new ResolutionPassVisitor();
        programAST.accept(resolutionPass);

        if (SymbolTable.hasSemanticErrors()) {
            System.err.println("Compilation halted");
            return;
        }
        

        TypeCheckVisitor typeCheck = new TypeCheckVisitor();
        programAST.accept(typeCheck);

        if (SymbolTable.hasSemanticErrors()) {
            System.err.println("Compilation halted");
            return;
        }

        
        CodeGenVisitor codeGen = new CodeGenVisitor();
        org.stringtemplate.v4.ST result = (org.stringtemplate.v4.ST) programAST.accept(codeGen);
        System.out.println(result.render());
    }
}
