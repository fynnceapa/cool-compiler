package cool.AST;

import cool.structures.*;
import java.util.*;

public class ResolutionPassVisitor implements ASTVisitor<Void> {

    private Scope currentScope;
    private String currentFileName;

    @Override
    public Void visit(Program program) {
        
        
        for (Class cls : program.classList) {
            cls.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Class cls) {
        currentFileName = cls.fileName;

        
        Symbol sym = SymbolTable.globals.lookup(cls.className);
        if (sym instanceof ClassSymbol) {
            currentScope = (ClassSymbol) sym;

            
            for (Feature feature : cls.featureList) {
                feature.accept(this);
            }
        }
        return null;
    }

    @Override
    public Void visit(VarDef varDef) {
        
        
        if (varDef.initExpr != null) {
            varDef.initExpr.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FuncDef funcDef) {
        String methodName = funcDef.featureName;

        MethodSymbol methodSym = ((ClassSymbol)currentScope).lookupMethod(methodName);

        if (methodSym != null) {

            Scope oldScope = currentScope;
            currentScope = methodSym;

            funcDef.funcBody.accept(this);

            currentScope = oldScope;
        }

        return null;
    }

    @Override
    public Void visit(Let lett) {
        
        Scope originalScope = currentScope;

        for (LocalPrm varDef : lett.localPrmList) {
            String name = varDef.name;
            String typeName = varDef.type.getText();

            if (varDef.val != null) {
                varDef.val.accept(this);
            }

            if (name.equals("self")) {
                SymbolTable.error(currentFileName, varDef.token, "Let variable has illegal name self");
                continue;
            }

            
            if (SymbolTable.globals.lookup(typeName) == null) {
                SymbolTable.error(currentFileName, varDef.type, "Let variable " + name + " has undefined type " + typeName);
            }

            
            DefaultScope letScope = new DefaultScope(currentScope);
            letScope.add(new IdSymbol(name));
            currentScope = letScope;
        }

        lett.body.accept(this);

        currentScope = originalScope;
        return null;
    }

    @Override
    public Void visit(Case casee) {
        casee.expr.accept(this);

        for (caseBranch b : casee.branchList) {
            String varName = b.name;
            String typeName = b.type.getText();

            if (varName.equals("self")) {
                SymbolTable.error(currentFileName, b.token, "Case variable has illegal name self");
            } else if (typeName.equals("SELF_TYPE")) {
                SymbolTable.error(currentFileName, b.type, "Case variable " + varName + " has illegal type SELF_TYPE");
            } else if (SymbolTable.globals.lookup(typeName) == null) {
                SymbolTable.error(currentFileName, b.type, "Case variable " + varName + " has undefined type " + typeName);
            }

            Scope oldScope = currentScope;
            currentScope = new DefaultScope(oldScope);
            currentScope.add(new IdSymbol(varName));

            b.expr.accept(this);

            currentScope = oldScope;
        }
        return null;
    }

    @Override
    public Void visit(Id id) {
        String name = id.token.getText();
        if (name.equals("self")) return null;

        if (currentScope.lookup(name) == null) {
            SymbolTable.error(currentFileName, id.token, "Undefined identifier " + name);
            return null;
        }
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        assign.expr.accept(this);
        return null;
    }

    
    @Override
    public Void visit(If iff) {
        iff.condition.accept(this);
        iff.thenBranch.accept(this);
        iff.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visit(While whilee) {
        whilee.condition.accept(this);
        whilee.body.accept(this);
        return null;
    }

    @Override
    public Void visit(BinaryOp binaryOp) {
        binaryOp.left.accept(this);
        binaryOp.right.accept(this);
        return null;
    }

    @Override
    public Void visit(UnaryOp unaryOp) {
        unaryOp.expr.accept(this);
        return null;
    }

    @Override
    public Void visit(Call call) {
        if(call.prefix != null) call.prefix.accept(this);
        for(var arg : call.args) arg.accept(this);
        return null;
    }

    @Override
    public Void visit(InitCall initCall) {
        for(var arg : initCall.args) arg.accept(this);
        return null;
    }

    @Override
    public Void visit(Block block) {
        for(var e : block.exprList) e.accept(this);
        return null;
    }

    
    @Override public Void visit(Feature feature) { return null; }
    @Override public Void visit(FormalPrm formalPrm) { return null; }
    @Override public Void visit(LocalPrm localPrm) { return null; }
    @Override public Void visit(caseBranch casebranch) { return null; }
    @Override public Void visit(New neww) { return null; }
    @Override public Void visit(Int intt) { return null; }
    @Override public Void visit(StringT stringt) { return null; }
    @Override public Void visit(Booll bool) { return null; }
}