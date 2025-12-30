package cool.AST;

public interface ASTVisitor<T> {
    T visit (FormalPrm formalPrm);
    T visit (LocalPrm localPrm);

    T visit (VarDef varDef);
    T visit (Class classs);
    T visit (Program program);
    T visit (caseBranch casebranch);
    T visit (FuncDef funcDef);

    T visit (New neww);
    T visit (Assign assign);
    T visit (Id id);
    T visit (Int intt);
    T visit (StringT stringt);
    T visit (Booll bool);
    T visit (Case casee);
    T visit (If iff);
    T visit (While whilee);

    T visit (Let lett);
    T visit (BinaryOp binaryOp);
    T visit (UnaryOp unaryOp);

    T visit (Call call);
    T visit (InitCall initCall);

    T visit (Block block);
}
