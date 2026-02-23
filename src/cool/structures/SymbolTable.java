package cool.structures;

import java.io.File;

import org.antlr.v4.runtime.*;

import cool.compiler.Compiler;
import cool.parser.CoolParser;
import cool.AST.ASTNode;

public class SymbolTable {
    public static Scope globals;

    private static boolean semanticErrors;

    public static void defineBasicClasses() {
        globals = new DefaultScope(null);
        semanticErrors = false;

        ClassSymbol objectSym = new ClassSymbol(null, "Object");
        globals.add(objectSym);

        
        MethodSymbol abortMethod = new MethodSymbol(objectSym, "abort");
        abortMethod.setType("Object");
        objectSym.add(abortMethod);

        MethodSymbol typeNameMethod = new MethodSymbol(objectSym, "type_name");
        typeNameMethod.setType("String");
        objectSym.add(typeNameMethod);

        MethodSymbol copyMethod = new MethodSymbol(objectSym, "copy");
        copyMethod.setType("SELF_TYPE");
        objectSym.add(copyMethod);

        ClassSymbol ioSym = new ClassSymbol(objectSym, "IO");
        globals.add(ioSym);

        
        MethodSymbol outStringMethod = new MethodSymbol(ioSym, "out_string");
        outStringMethod.setType("SELF_TYPE");
        IdSymbol outStringParam = new IdSymbol("x");
        outStringParam.setType("String");
        outStringMethod.add(outStringParam);
        ioSym.add(outStringMethod);

        MethodSymbol outIntMethod = new MethodSymbol(ioSym, "out_int");
        outIntMethod.setType("SELF_TYPE");
        IdSymbol outIntParam = new IdSymbol("x");
        outIntParam.setType("Int");
        outIntMethod.add(outIntParam);
        ioSym.add(outIntMethod);

        MethodSymbol inStringMethod = new MethodSymbol(ioSym, "in_string");
        inStringMethod.setType("String");
        ioSym.add(inStringMethod);

        MethodSymbol inIntMethod = new MethodSymbol(ioSym, "in_int");
        inIntMethod.setType("Int");
        ioSym.add(inIntMethod);

        ClassSymbol intSym = new ClassSymbol(objectSym, "Int");
        globals.add(intSym);

        ClassSymbol stringSym = new ClassSymbol(objectSym, "String");
        globals.add(stringSym);

        
        MethodSymbol lengthMethod = new MethodSymbol(stringSym, "length");
        lengthMethod.setType("Int");
        stringSym.add(lengthMethod);

        MethodSymbol concatMethod = new MethodSymbol(stringSym, "concat");
        concatMethod.setType("String");
        IdSymbol concatParam = new IdSymbol("s");
        concatParam.setType("String");
        concatMethod.add(concatParam);
        stringSym.add(concatMethod);

        MethodSymbol substrMethod = new MethodSymbol(stringSym, "substr");
        substrMethod.setType("String");
        IdSymbol substrParam1 = new IdSymbol("i");
        substrParam1.setType("Int");
        substrMethod.add(substrParam1);
        IdSymbol substrParam2 = new IdSymbol("l");
        substrParam2.setType("Int");
        substrMethod.add(substrParam2);
        stringSym.add(substrMethod);

        ClassSymbol boolSym = new ClassSymbol(objectSym, "Bool");
        globals.add(boolSym);

    }

    public static void error(ASTNode node, String message) {
        error(node.fileName, node.token, message);
    }

    public static void error(String fileName, Token token, String message) {
        String name = new File(fileName).getName();
        int line = 0;
        int col = 0;

        if (token != null) {
            line = token.getLine();
            col = token.getCharPositionInLine() + 1;
        }

        String errorMsg = "\"" + name + "\", line " + line + ":" + col + ", Semantic error: " + message;
        System.err.println(errorMsg);
        semanticErrors = true;
    }

     
    public static void error(ParserRuleContext ctx, Token info, String str) {
        while (!(ctx.getParent() instanceof CoolParser.ProgramContext))
            ctx = ctx.getParent();

        String message = "\"" + new File(Compiler.fileNames.get(ctx)).getName()
                + "\", line " + info.getLine()
                + ":" + (info.getCharPositionInLine() + 1)
                + ", Semantic error: " + str;

        System.err.println(message);

        semanticErrors = true;
    }

    public static void error(String str) {
        String message = "Semantic error: " + str;

        System.err.println(message);

        semanticErrors = true;
    }

    public static boolean hasSemanticErrors() {
        return semanticErrors;
    }

    public static boolean conforms(String type1, String type2) {
        if (type1.equals(type2))
            return false;

        ClassSymbol sym1 = (ClassSymbol) globals.lookup(type1);
        while (sym1 != null) {
            if (sym1.parent != null && sym1.parent.getName().equals(type2)) {
                return false;
            }
            sym1 = sym1.parent;
        }

        return true;
    }

    public static Symbol leastCommonAncestor(Symbol thenType, Symbol elseType) {
        if (thenType.getName().equals(elseType.getName())) {
            return thenType;
        }

        ClassSymbol sym1 = (ClassSymbol) globals.lookup(thenType.getName());
        while (sym1 != null) {
            ClassSymbol sym2 = (ClassSymbol) globals.lookup(elseType.getName());
            while (sym2 != null) {
                if (sym1.getName().equals(sym2.getName())) {
                    return sym1;
                }
                sym2 = sym2.parent;
            }
            sym1 = sym1.parent;
        }

        return globals.lookup("Object");
    }
}
