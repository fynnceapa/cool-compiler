package cool.AST;

import cool.structures.*;

public class TypeCheckVisitor implements ASTVisitor<Symbol> {
    private Scope currentScope;
    private String currentFileName;

    @Override
    public Symbol visit(Program program) {
        for (Class cls : program.classList) {
            cls.accept(this);
        }
        return null;
    }

    @Override
    public Symbol visit(Class cls) {
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
    public Symbol visit(FuncDef funcDef) {
        String methodName = funcDef.featureName;
        MethodSymbol methodSym = ((ClassSymbol) currentScope).lookupMethod(methodName);

        Scope oldScope = currentScope;
        currentScope = methodSym;

        Symbol bodyType = funcDef.funcBody.accept(this);

        Symbol declaredType = null;
        if (methodSym != null) {
            String declaredName = methodSym.getType();
            if (declaredName != null) {
                
                
                
                if (declaredName.equals("SELF_TYPE") && oldScope instanceof ClassSymbol) {
                    declaredType = (ClassSymbol) oldScope;
                } else {
                    declaredType = SymbolTable.globals.lookup(declaredName);
                }
            }
        }

        if (bodyType != null && declaredType != null) {
            if (SymbolTable.conforms(bodyType.getName(), declaredType.getName())) {
                SymbolTable.error(currentFileName, funcDef.funcBody.token,
                        "Type " + bodyType.getName() + " of the body of method " + methodName +
                                " is incompatible with declared return type " + declaredType.getName());
            }
        }

        currentScope = oldScope;

        return declaredType;
    }

    @Override
    public Symbol visit(VarDef varDef) {
        if (varDef.initExpr != null) {
            Symbol initType = varDef.initExpr.accept(this);

            String typeName = varDef.type.getText();
            Symbol declaredType = SymbolTable.globals.lookup(typeName);

            if (initType != null && declaredType != null) {
                if (SymbolTable.conforms(initType.getName(), declaredType.getName())) {
                    SymbolTable.error(currentFileName, varDef.initExpr.token,
                            "Type " + initType.getName() +
                                    " of initialization expression of attribute " + varDef.featureName +
                                    " is incompatible with declared type " + declaredType.getName());
                }
            }
        }
        return null;
    }

    @Override
    public Symbol visit(Id id) {
        String name = id.token.getText();

        Symbol sym = currentScope.lookup(name);
        if (sym instanceof IdSymbol) {
            String typeName = ((IdSymbol) sym).getType();
            return SymbolTable.globals.lookup(typeName);
        }
        return null;
    }

    @Override
    public Symbol visit(Int intt) {
        return SymbolTable.globals.lookup("Int");
    }

    @Override
    public Symbol visit(Booll bool) {
        return SymbolTable.globals.lookup("Bool");
    }

    @Override
    public Symbol visit(StringT string) {
        return SymbolTable.globals.lookup("String");
    }

    @Override
    public Symbol visit(BinaryOp binaryOp) {
        Symbol leftType = binaryOp.left.accept(this);
        Symbol rightType = binaryOp.right.accept(this);
        String op = binaryOp.operator.getText();

        
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (leftType != null && !leftType.getName().equals("Int")) {
                SymbolTable.error(currentFileName, binaryOp.left.token,
                        "Operand of " + op + " has type " + leftType.getName() + " instead of Int");
                return null;
            }
            if (rightType != null && !rightType.getName().equals("Int")) {
                SymbolTable.error(currentFileName, binaryOp.right.token,
                        "Operand of " + op + " has type " + rightType.getName() + " instead of Int");
                return null;
            }
            return SymbolTable.globals.lookup("Int");
        }

        
        if (op.equals("<") || op.equals("<=")) {
            if (leftType != null && !leftType.getName().equals("Int")) {
                SymbolTable.error(currentFileName, binaryOp.left.token,
                        "Operand of " + op + " has type " + leftType.getName() + " instead of Int");
                return null;
            }
            if (rightType != null && !rightType.getName().equals("Int")) {
                SymbolTable.error(currentFileName, binaryOp.right.token,
                        "Operand of " + op + " has type " + rightType.getName() + " instead of Int");
                return null;
            }
            return SymbolTable.globals.lookup("Bool");
        }

        
        if (op.equals("=")) {
            if (leftType != null && rightType != null) {
                String lName = leftType.getName();
                String rName = rightType.getName();

                
                boolean lBasic = lName.equals("Int") || lName.equals("String") || lName.equals("Bool");
                boolean rBasic = rName.equals("Int") || rName.equals("String") || rName.equals("Bool");

                if ((lBasic || rBasic) && !lName.equals(rName)) {
                    SymbolTable.error(currentFileName, binaryOp.operator,
                            "Cannot compare " + lName + " with " + rName);
                    return null;
                }
            }
            return SymbolTable.globals.lookup("Bool");
        }

        return null;
    }

    @Override
    public Symbol visit(UnaryOp unaryOp) {
        Symbol exprType = unaryOp.expr.accept(this);
        String op = unaryOp.operator.getText();

        if (op.equals("~")) {
            if (exprType != null && !exprType.getName().equals("Int")) {
                SymbolTable.error(currentFileName, unaryOp.expr.token,
                        "Operand of ~ has type " + exprType.getName() + " instead of Int");
            }
            return SymbolTable.globals.lookup("Int");
        }

        if (op.toLowerCase().equals("not")) {
            if (exprType != null && !exprType.getName().equals("Bool")) {
                SymbolTable.error(currentFileName, unaryOp.expr.token,
                        "Operand of not has type " + exprType.getName() + " instead of Bool");
            }
            return SymbolTable.globals.lookup("Bool");
        }

        if (op.toLowerCase().equals("isvoid")) {
            return SymbolTable.globals.lookup("Bool");
        }

        return null;
    }

    @Override
    public Symbol visit(Assign assign) {
        Symbol exprType = assign.expr.accept(this);
        String name = assign.token.getText();
        Symbol sym = currentScope.lookup(name);
        if (name.equals("self")) {
            SymbolTable.error(currentFileName, assign.token, "Cannot assign to self");
            assign.expr.accept(this);
            return SymbolTable.globals.lookup("Object");
        }
        if (sym instanceof IdSymbol) {
            String varTypeName = ((IdSymbol) sym).getType();
            Symbol varType = SymbolTable.globals.lookup(varTypeName);

            if (exprType != null && varType != null) {
                if (SymbolTable.conforms(exprType.getName(), varType.getName())) {
                    SymbolTable.error(currentFileName, assign.expr.token,
                            "Type " + exprType.getName() + " of assigned expression is incompatible with declared type "
                                    + varType.getName() + " of identifier " + name);
                }
            }

            return exprType;
        }
        return null;
    }

    @Override
    public Symbol visit(New neww) {
        String typeName = neww.type.getText();
        Symbol type = SymbolTable.globals.lookup(typeName);

        if (type == null) {
            SymbolTable.error(currentFileName, neww.type, "new is used with undefined type " + typeName);
            return null;
        }

        return type;
    }

    @Override
    public Symbol visit(While whilee) {
        Symbol condType = whilee.condition.accept(this);
        if (condType != null && !condType.getName().equals("Bool")) {
            SymbolTable.error(currentFileName, whilee.condition.token,
                    "While condition has type " + condType.getName() + " instead of Bool");
        }
        whilee.body.accept(this);
        return SymbolTable.globals.lookup("Object");
    }

    @Override
    public Symbol visit(If iff) {
        Symbol condType = iff.condition.accept(this);
        if (condType != null && !condType.getName().equals("Bool")) {
            SymbolTable.error(currentFileName, iff.condition.token,
                    "If condition has type " + condType.getName() + " instead of Bool");
        }
        Symbol thenType = iff.thenBranch.accept(this);
        Symbol elseType = iff.elseBranch.accept(this);

        if (thenType != null && elseType != null) {
            return SymbolTable.leastCommonAncestor(thenType, elseType);
        }
        return null;
    }

    @Override
    public Symbol visit(Case casee) {
        casee.expr.accept(this);

        Symbol resultType = null;
        for (caseBranch b : casee.branchList) {
            String varName = b.name;
            String typeName = b.type.getText();

            Scope oldScope = currentScope;
            currentScope = new DefaultScope(oldScope);
            currentScope.add(new IdSymbol(varName) {
                {
                    setType(typeName);
                }
            });

            Symbol branchType = b.expr.accept(this);
            if (resultType == null && branchType != null) {
                resultType = branchType;
            } else if (branchType != null) {
                resultType = SymbolTable.leastCommonAncestor(resultType, branchType);
            }

            currentScope = oldScope;
        }
        return resultType;
    }

    @Override
    public Symbol visit(Block block) {
        Symbol lastType = null;
        for (Expression expr : block.exprList) {
            lastType = expr.accept(this);
        }
        return lastType;
    }

    @Override
    public Symbol visit(Let let) {
        Scope oldScope = currentScope;
        currentScope = new DefaultScope(oldScope);

        for (LocalPrm varDef : let.localPrmList) {
            String varName = varDef.name;
            String typeName = varDef.type.getText();

            IdSymbol varSym = new IdSymbol(varName);
            varSym.setType(typeName);
            currentScope.add(varSym);

            if (varDef.val != null) {
                Symbol initType = varDef.val.accept(this);
                Symbol declaredType = SymbolTable.globals.lookup(typeName);

                if (initType != null && declaredType != null) {
                    if (SymbolTable.conforms(initType.getName(), declaredType.getName())) {
                        SymbolTable.error(currentFileName, varDef.val.token,
                                "Type " + initType.getName() + " of initialization expression of identifier " + varName
                                        +
                                        " is incompatible with declared type " + declaredType.getName());
                    }
                }
            }
        }
        Symbol bodyType = let.body.accept(this);
        currentScope = oldScope;
        return bodyType;
    }

    @Override
    public Void visit(Feature feature) {
        return null;
    }

    @Override
    public Symbol visit(FormalPrm formalPrm) {
        return null;
    }

    @Override
    public Symbol visit(LocalPrm localPrm) {
        return null;
    }

    @Override
    public Symbol visit(caseBranch casebranch) {
        return null;
    }

    @Override
    public Symbol visit(Call call) {
        ClassSymbol currentClass = getCurrentClass();

        Symbol prefixType = null;
        if (call.prefix != null) {
            prefixType = call.prefix.accept(this);
            if (prefixType == null && call.prefix instanceof Id &&
                    ((Id) call.prefix).token.getText().equals("self")) {
                prefixType = currentClass;
            }
        } else {
            prefixType = currentClass;
        }

        ClassSymbol actualClass = null;
        if (prefixType instanceof ClassSymbol) {
            actualClass = (ClassSymbol) prefixType;
        } else if (prefixType != null) {
            Symbol resolved = SymbolTable.globals.lookup(prefixType.getName());
            if (resolved instanceof ClassSymbol) {
                actualClass = (ClassSymbol) resolved;
            }
        }
        if (actualClass == null) {
            return null;
        }

        ClassSymbol dispatchClass = actualClass;
        if (call.type != null) {
            String staticName = call.type.getText();
            if (staticName.equals("SELF_TYPE")) {
                SymbolTable.error(currentFileName, call.type,
                        "Type of static dispatch cannot be SELF_TYPE");
                return null;
            }

            Symbol staticSym = SymbolTable.globals.lookup(staticName);
            if (!(staticSym instanceof ClassSymbol)) {
                SymbolTable.error(currentFileName, call.type,
                        "Type " + staticName + " of static dispatch is undefined");
                return null;
            }

            if (SymbolTable.conforms(actualClass.getName(), staticName)) {
                SymbolTable.error(currentFileName, call.type,
                        "Type " + staticName + " of static dispatch is not a superclass of type "
                                + actualClass.getName());
                return null;
            }

            dispatchClass = (ClassSymbol) staticSym;
        }

        String methodName = call.name.getText();
        MethodSymbol methodSym = dispatchClass.lookupMethod(methodName);
        if (methodSym == null) {
            SymbolTable.error(currentFileName, call.name,
                    "Undefined method " + methodName + " in class " + dispatchClass.getName());
            for (Expression arg : call.args) {
                arg.accept(this);
            }
            return null;
        }

        if (methodSym.getNrSymbols() != call.args.size()) {
            SymbolTable.error(currentFileName, call.name,
                    "Method " + methodName + " of class " + dispatchClass.getName()
                            + " is applied to wrong number of arguments");
        }

        int paramCount = methodSym.getNrSymbols();
        int idx = 0;
        for (Expression arg : call.args) {
            Symbol actualType = arg.accept(this);

            if (idx < paramCount) {
                Symbol paramSym = methodSym.getSymbol(idx);

                if (paramSym instanceof IdSymbol) {
                    String declaredName = ((IdSymbol) paramSym).getType();
                    Symbol declaredType = SymbolTable.globals.lookup(declaredName);

                    if (actualType != null && declaredType != null) {
                        if (SymbolTable.conforms(actualType.getName(), declaredType.getName())) {
                            SymbolTable.error(currentFileName, arg.token,
                                    "In call to method " + methodName + " of class " + dispatchClass.getName() +
                                            ", actual type " + actualType.getName() + " of formal parameter "
                                            + paramSym.getName() +
                                            " is incompatible with declared type " + declaredType.getName());
                        }
                    }
                }
            }

            idx++;
        }

        String returnTypeName = methodSym.getType();
        if (returnTypeName != null && returnTypeName.equals("SELF_TYPE")) {
            return actualClass;
        }
        return SymbolTable.globals.lookup(returnTypeName);
    }

    @Override
    public Symbol visit(InitCall initCall) {
        ClassSymbol currentClass = getCurrentClass();
        if (currentClass == null) {
            return null;
        }

        ClassSymbol dispatchClass = currentClass;
        String methodName = initCall.name.getText();
        MethodSymbol methodSym = dispatchClass.lookupMethod(methodName);

        if (methodSym == null) {
            SymbolTable.error(currentFileName, initCall.name,
                    "Undefined method " + methodName + " in class " + dispatchClass.getName());
            for (Expression arg : initCall.args) {
                arg.accept(this);
            }
            return null;
        }

        if (methodSym.getNrSymbols() != initCall.args.size()) {
            SymbolTable.error(currentFileName, initCall.name,
                    "Method " + methodName + " of class " + dispatchClass.getName()
                            + " is applied to wrong number of arguments");
        }

        int paramCount = methodSym.getNrSymbols();
        int idx = 0;
        for (Expression arg : initCall.args) {
            Symbol actualType = arg.accept(this);

            if (idx < paramCount) {
                Symbol paramSym = methodSym.getSymbol(idx);
                if (paramSym instanceof IdSymbol) {
                    String declaredName = ((IdSymbol) paramSym).getType();
                    Symbol declaredType = SymbolTable.globals.lookup(declaredName);

                    if (actualType != null && declaredType != null) {
                        if (SymbolTable.conforms(actualType.getName(), declaredType.getName())) {
                            SymbolTable.error(currentFileName, arg.token,
                                    "In call to method " + methodName + " of class " + dispatchClass.getName() +
                                            ", actual type " + actualType.getName() + " of formal parameter "
                                            + paramSym.getName() +
                                            " is incompatible with declared type " + declaredType.getName());
                        }
                    }
                }
            }

            idx++;
        }

        String returnTypeName = methodSym.getType();
        if (returnTypeName != null && returnTypeName.equals("SELF_TYPE")) {
            return dispatchClass;
        }
        return SymbolTable.globals.lookup(returnTypeName);
    }

    private ClassSymbol getCurrentClass() {
        Scope scope = currentScope;
        while (scope != null && !(scope instanceof ClassSymbol)) {
            scope = scope.getParent();
        }
        return (ClassSymbol) scope;
    }
}