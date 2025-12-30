package cool.AST;
import cool.parser.CoolParser;
import org.antlr.v4.runtime.Token;

import java.util.List;


public abstract class ASTNode {
    public Token token;

    ASTNode(Token token) {
        this.token = token;
    }
    public <T> T accept(ASTVisitor<T> visitor) {
        throw new UnsupportedOperationException("accept not implemented in " + this.getClass().getName());
    }
}

abstract class Feature extends ASTNode {
    String featureName;
    Token type;

    Feature (String name, Token type, Token token) {
        super (token);
        this.featureName = name;
        this.type = type;
    }
}

class FormalPrm extends ASTNode {
    String prmName;
    Token type;

    FormalPrm(String name, Token type, Token token) {
        super(token);
        this.prmName = name;
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class LocalPrm extends ASTNode {
    String name;
    Token type;
    Expression val;

    LocalPrm(String name, Token type, Expression val, Token token) {
        super(token);
        this.name = name;
        this.type = type;
        this.val = val;
    }
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class caseBranch extends ASTNode {
    String name;
    Token type;
    Expression expr;

    caseBranch(String name, Token type, Expression expr, Token token) {
        super(token);
        this.name = name;
        this.type = type;
        this.expr = expr;
    }
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}



class VarDef extends Feature {
    Expression initExpr;

    VarDef (String name, Token type, Expression initExpr, Token token) {
        super (name, type, token);
        this.initExpr = initExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class FuncDef extends Feature {
    List<FormalPrm> formalParmList;
    Expression funcBody;
    FuncDef (String name, Token type, List<FormalPrm> formalParmList, Expression funcBody, Token token) {
        super (name, type, token);
        this.formalParmList = formalParmList;
        this.funcBody = funcBody;
    }
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Class extends ASTNode {
    String className;
    String inherits;
    List<Feature> featureList;

    Class (String name, String inherits, List<Feature> featureList, Token token) {
        super (token);
        this.className = name;
        this.inherits = inherits;
        this.featureList = featureList;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Program extends ASTNode {
    List<Class> classList;

    Program(List<Class> classList, Token token) {
        super(token);
        this.classList = classList;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

abstract class Expression extends ASTNode {
    Expression(Token token) {
        super(token);
    }
}

class Case extends Expression {
    Expression expr;
    List<caseBranch> branchList;

    Case(Expression expr, List<caseBranch> branchList, Token token) {
        super(token);
        this.expr = expr;
        this.branchList = branchList;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class New extends Expression {
    Token type;

    New(Token type, Token token) {
        super(token);
        this.type = type;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Assign extends Expression {
    String varName;
    Expression expr;

    Assign(String varName, Expression expr, Token token) {
        super(token);
        this.varName = varName;
        this.expr = expr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Id extends Expression {
    Id(Token token) {
        super(token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Int extends Expression {
    Int(Token token) {
        super(token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Booll extends Expression {
    Booll(Token token) {
        super(token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class StringT extends Expression {
    StringT(Token token) {
        super(token);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class If extends Expression {
    Expression condition;
    Expression thenBranch;
    Expression elseBranch;

    If(Expression condition, Expression thenBranch, Expression elseBranch, Token token) {
        super(token);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class While extends Expression {
    Expression condition;
    Expression body;

    While(Expression condition, Expression body, Token token) {
        super(token);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Let extends Expression {
    List<LocalPrm> localPrmList;
    Expression body;

    Let(List<LocalPrm> localPrmList, Expression body, Token token) {
        super(token);
        this.localPrmList = localPrmList;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class BinaryOp extends Expression {
    Expression left;
    Expression right;
    Token operator;

    BinaryOp(Expression left, Expression right, Token operator, Token token) {
        super(token);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class UnaryOp extends Expression {
    Expression expr;
    Token operator;

    UnaryOp(Expression expr, Token operator, Token token) {
        super(token);
        this.expr = expr;
        this.operator = operator;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Call extends Expression {
    Expression prefix;
    Token type;
    Token name;
    List<Expression> args;

    Call(Expression prefix, Token type, Token name, List<Expression> args, Token token) {
        super(token);
        this.prefix = prefix;
        this.type = type;
        this.name = name;
        this.args = args;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class InitCall extends Expression {
    Token name;
    List<Expression> args;

    InitCall(Token name, List<Expression> args, Token token) {
        super(token);
        this.name = name;
        this.args = args;
    }
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

class Block extends Expression {
    List<Expression> exprList;

    Block(List<Expression> exprList, Token token) {
        super(token);
        this.exprList = exprList;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}


