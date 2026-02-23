package cool.AST;

import java.util.List;

public class ASTPrintTree implements ASTVisitor<Void> {

    private int indent = 0;

    private void printIndent(String str) {
        if (str == null || str.isEmpty()) {
            return;
        }
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.println(str);
    }

    private void visit(List<? extends ASTNode> nodes) {
        if (nodes == null) {
            return;
        }
        for (ASTNode node : nodes) {
            node.accept(this);
        }
    }

    @Override
    public Void visit(Program program) {
        printIndent("program");
        indent++;
        visit(program.classList);
        indent--;
        return null;
    }

    @Override
    public Void visit(Class classs) {
        printIndent("class");
        indent++;
        printIndent(classs.className);
        if (classs.inherits != null) {
            printIndent(classs.inherits);
        }
        visit(classs.featureList);
        indent--;
        return null;
    }

    @Override
    public Void visit(FuncDef funcDef) {
        printIndent("method");
        indent++;
        printIndent(funcDef.featureName);
        visit(funcDef.formalParmList);
        printIndent(funcDef.type.getText());
        funcDef.funcBody.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(VarDef varDef) {
        printIndent("attribute");
        indent++;
        printIndent(varDef.featureName);
        printIndent(varDef.type.getText());
        if (varDef.initExpr != null) {
            varDef.initExpr.accept(this);
        }
        indent--;
        return null;
    }

    @Override
    public Void visit(FormalPrm formalPrm) {
        printIndent("formal");
        indent++;
        printIndent(formalPrm.prmName);
        printIndent(formalPrm.type.getText());
        indent--;
        return null;
    }

    @Override
    public Void visit(LocalPrm localPrm) {
        printIndent("local");
        indent++;
        printIndent(localPrm.name);
        printIndent(localPrm.type.getText());
        if (localPrm.val != null) {
            localPrm.val.accept(this);
        }
        indent--;
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        printIndent("<-");
        indent++;
        printIndent(assign.varName);
        assign.expr.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(BinaryOp binaryOp) {
        printIndent(binaryOp.operator.getText());
        indent++;
        binaryOp.left.accept(this);
        binaryOp.right.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(UnaryOp unaryOp) {
        printIndent(unaryOp.operator.getText());
        indent++;
        unaryOp.expr.accept(this);
        indent--;

        return null;
    }

    @Override
    public Void visit(Call call) {
        printIndent(".");
        indent++;
        call.prefix.accept(this);
        if (call.type != null) {
            printIndent(call.type.getText());
        }
        printIndent(call.name.getText());
        visit(call.args);
        indent--;
        return null;
    }

    @Override
    public Void visit(InitCall initCall) {
        printIndent("implicit dispatch");
        indent++;
        printIndent(initCall.name.getText());
        visit(initCall.args);
        indent--;
        return null;
    }

    @Override
    public Void visit(If iff) {
        printIndent("if");
        indent++;
        iff.condition.accept(this);
        iff.thenBranch.accept(this);
        iff.elseBranch.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(While whilee) {
        printIndent("while");
        indent++;
        whilee.condition.accept(this);
        whilee.body.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(Let lett) {
        printIndent("let");
        indent++;
        visit(lett.localPrmList);
        lett.body.accept(this);
        indent--;
        return null;
    }

    @Override
    public Void visit(Block block) {
        printIndent("block");
        indent++;
        visit(block.exprList);
        indent--;
        return null;
    }

    @Override
    public Void visit(New neww) {
        printIndent("new");
        indent++;
        printIndent(neww.type.getText());
        indent--;
        return null;
    }
    @Override
    public Void visit(Id id) {
        printIndent(id.token.getText());
        return null;
    }

    @Override
    public Void visit(Int intt) {
        printIndent(intt.token.getText());
        return null;
    }

    @Override
    public Void visit(StringT stringt) {
        printIndent(stringt.token.getText());
        return null;
    }

    @Override
    public Void visit(Booll bool) {
        printIndent(bool.token.getText());
        return null;
    }

    @Override
    public Void visit(Case casee) {
        printIndent("case");
        indent++;
        casee.expr.accept(this);
        visit(casee.branchList);
        indent--;
        return null;
    }

    @Override
    public Void visit(caseBranch casebranch) {
        printIndent("case branch");
        indent++;
        printIndent(casebranch.name);
        printIndent(casebranch.type.getText());
        casebranch.expr.accept(this);
        indent--;
        return null;
    }
}