package cool.AST;

import cool.structures.*;
import java.util.*;

public class DefinitionPassVisitor implements ASTVisitor<Void> {

    private Scope currentScope;
    private String currentFileName;

    @Override
    public Void visit(Program program) {
        
        for (Class cls : program.classList) {
            if (cls.className.equals("SELF_TYPE")) {
                SymbolTable.error(cls, "Class has illegal name SELF_TYPE");
                continue;
            }

            if (SymbolTable.globals.lookup(cls.className) != null) {
                SymbolTable.error(cls, "Class " + cls.className + " is redefined");
                continue;
            }

            ClassSymbol sym = new ClassSymbol(null, cls.className);
            SymbolTable.globals.add(sym);
        }

        
        for (Class cls : program.classList) {
            Symbol sym = SymbolTable.globals.lookup(cls.className);
            if (sym instanceof ClassSymbol) {
                ClassSymbol classSym = (ClassSymbol) sym;

                String parentName = cls.inherits;
                if (parentName == null) {
                    parentName = "Object";
                }

                if (parentName.equals("Int") || parentName.equals("String") ||
                        parentName.equals("Bool") || parentName.equals("SELF_TYPE")) {
                    SymbolTable.error(cls.fileName, cls.parentToken,
                            "Class " + cls.className + " has illegal parent " + parentName);
                    continue;
                }

                Symbol parentSym = SymbolTable.globals.lookup(parentName);

                if (parentSym == null || !(parentSym instanceof ClassSymbol)) {
                    SymbolTable.error(cls.fileName, cls.parentToken,
                            "Class " + cls.className + " has undefined parent " + parentName);
                } else {
                    classSym.parent = (ClassSymbol) parentSym;
                }
            }
        }

        
        for (Class cls : program.classList) {
            Symbol sym = SymbolTable.globals.lookup(cls.className);
            if (sym instanceof ClassSymbol) {
                ClassSymbol current = (ClassSymbol) sym;
                ClassSymbol iterator = current.getParent();

                while (iterator != null) {
                    if (iterator == current) {
                        SymbolTable.error(cls, "Inheritance cycle for class " + current.getName());
                        break;
                    }
                    iterator = iterator.getParent();
                }
            }
        }

        
        
        
        Set<String> processed = new HashSet<>();
        processed.add("Object");
        processed.add("IO");
        processed.add("Int");
        processed.add("String");
        processed.add("Bool");

        boolean progress = true;
        
        while (progress) {
            progress = false;
            for (Class cls : program.classList) {
                if (!processed.contains(cls.className)) {
                    String parentName = cls.inherits;
                    if (parentName == null)
                        parentName = "Object";

                    
                    if (processed.contains(parentName)) {
                        Symbol sym = SymbolTable.globals.lookup(cls.className);
                        if (sym instanceof ClassSymbol) {
                            currentScope = (ClassSymbol) sym;
                            cls.accept(this); 
                        }
                        processed.add(cls.className);
                        progress = true;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Void visit(Class cls) {
        currentFileName = cls.fileName;
        
        for (Feature feature : cls.featureList) {
            feature.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(VarDef varDef) {
        String name = varDef.featureName;
        String typeName = varDef.type.getText();

        if (name.equals("self")) {
            SymbolTable.error(currentFileName, varDef.token,
                    "Class " + ((ClassSymbol) currentScope).getName() + " has attribute with illegal name self");
            return null;
        }

        IdSymbol attrSym = new IdSymbol(name);
        attrSym.setType(typeName);

        
        if (!currentScope.add(attrSym)) {
            SymbolTable.error(currentFileName, varDef.token,
                    "Class " + ((ClassSymbol) currentScope).getName() + " redefines attribute " + name);
            return null;
        }

        
        Scope parent = currentScope.getParent();
        if (parent != null) {
            Symbol inherited = parent.lookup(name);
            if (inherited instanceof IdSymbol) {
                SymbolTable.error(currentFileName, varDef.token,
                        "Class " + ((ClassSymbol) currentScope).getName() + " redefines inherited attribute " + name);
            }
        }

        if (SymbolTable.globals.lookup(typeName) == null && !typeName.equals("SELF_TYPE")) {
            SymbolTable.error(currentFileName, varDef.type,
                    "Class " + ((ClassSymbol) currentScope).getName() + " has attribute " + name
                            + " with undefined type " + typeName);
        }

        
        return null;
    }

    @Override
    public Void visit(FuncDef funcDef) {
        String methodName = funcDef.featureName;
        String returnTypeName = funcDef.type.getText();

        if (!currentScope.add(new MethodSymbol(currentScope, methodName))) {
            SymbolTable.error(currentFileName, funcDef.token,
                    "Class " + ((ClassSymbol) currentScope).getName() + " redefines method " + methodName);
            return null;
        }

        
        MethodSymbol methodSym = ((ClassSymbol) currentScope).lookupMethod(methodName);
        methodSym.setType(returnTypeName);

        if (SymbolTable.globals.lookup(returnTypeName) == null && !returnTypeName.equals("SELF_TYPE")) {
            SymbolTable.error(currentFileName, funcDef.type,
                    "Class " + ((ClassSymbol) currentScope).getName() + " has method " + methodName
                            + " with undefined return type " + returnTypeName);
        }

        Scope parentScope = currentScope.getParent();
        if (parentScope instanceof ClassSymbol) {
            MethodSymbol parentMethod = ((ClassSymbol) parentScope).lookupMethod(methodName);
            if (parentMethod != null) {
                if (parentMethod.getNrSymbols() != funcDef.formalParmList.size()) {
                    SymbolTable.error(currentFileName, funcDef.token,
                            "Class " + ((ClassSymbol) currentScope).getName() + " overrides method " + methodName
                                    + " with different number of formal parameters");
                } else {
                    
                    int idx = 0;
                    for (FormalPrm formal : funcDef.formalParmList) {
                        Symbol parentParam = parentMethod.getSymbol(idx);
                        String parentType = ((IdSymbol) parentParam).getType();
                        String currentType = formal.type.getText();

                        if (!parentType.equals(currentType)) {
                            SymbolTable.error(currentFileName, formal.type,
                                    "Class " + ((ClassSymbol) currentScope).getName() + " overrides method "
                                            + methodName + " but changes type of formal parameter " + formal.prmName
                                            + " from " + parentType + " to " + currentType);
                        }
                        idx++;
                    }
                }

                if (!parentMethod.getType().equals(returnTypeName)) {
                    SymbolTable.error(currentFileName, funcDef.type,
                            "Class " + ((ClassSymbol) currentScope).getName() + " overrides method " + methodName
                                    + " but changes return type from " + parentMethod.getType() + " to "
                                    + returnTypeName);
                }
            }
        }

        
        for (FormalPrm formal : funcDef.formalParmList) {
            String paramName = formal.prmName;
            String paramType = formal.type.getText();

            if (paramName.equals("self")) {
                SymbolTable.error(currentFileName, formal.token,
                        "Method " + methodName + " of class " + ((ClassSymbol) currentScope).getName()
                                + " has formal parameter with illegal name self");
                continue;
            }

            if (paramType.equals("SELF_TYPE")) {
                SymbolTable.error(currentFileName, formal.type,
                        "Method " + methodName + " of class " + ((ClassSymbol) currentScope).getName()
                                + " has formal parameter " + paramName + " with illegal type SELF_TYPE");
                continue;
            }

            if (SymbolTable.globals.lookup(paramType) == null) {
                SymbolTable.error(currentFileName, formal.type,
                        "Method " + methodName + " of class " + ((ClassSymbol) currentScope).getName()
                                + " has formal parameter " + paramName + " with undefined type " + paramType);
            }

            IdSymbol paramSym = new IdSymbol(paramName);
            paramSym.setType(paramType);

            if (!methodSym.add(paramSym)) {
                SymbolTable.error(currentFileName, formal.token,
                        "Method " + methodName + " of class " + ((ClassSymbol) currentScope).getName()
                                + " redefines formal parameter " + paramName);
            }
        }

        
        return null;
    }

    
    @Override
    public Void visit(Feature feature) {
        return null;
    }

    @Override
    public Void visit(FormalPrm formalPrm) {
        return null;
    }

    @Override
    public Void visit(LocalPrm localPrm) {
        return null;
    }

    @Override
    public Void visit(caseBranch casebranch) {
        return null;
    }

    @Override
    public Void visit(New neww) {
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        return null;
    }

    @Override
    public Void visit(Id id) {
        return null;
    }

    @Override
    public Void visit(Int intt) {
        return null;
    }

    @Override
    public Void visit(StringT stringt) {
        return null;
    }

    @Override
    public Void visit(Booll bool) {
        return null;
    }

    @Override
    public Void visit(Case casee) {
        return null;
    }

    @Override
    public Void visit(If iff) {
        return null;
    }

    @Override
    public Void visit(While whilee) {
        return null;
    }

    @Override
    public Void visit(Let lett) {
        return null;
    }

    @Override
    public Void visit(BinaryOp binaryOp) {
        return null;
    }

    @Override
    public Void visit(UnaryOp unaryOp) {
        return null;
    }

    @Override
    public Void visit(Call call) {
        return null;
    }

    @Override
    public Void visit(InitCall initCall) {
        return null;
    }

    @Override
    public Void visit(Block block) {
        return null;
    }
}