package cool.AST;

import cool.parser.CoolParser;
import cool.parser.CoolParserBaseVisitor;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ASTBuilderVisitor extends CoolParserBaseVisitor<ASTNode> {

    private List<Expression> visitExprList(List<CoolParser.ExprContext> exprs) {
        if (exprs == null) {
            return new ArrayList<>();
        }
        return exprs.stream()
                .map(exprCtx -> (Expression) visit(exprCtx))
                .collect(Collectors.toList());
    }
    @Override
    public ASTNode visitProgram(CoolParser.ProgramContext ctx) {
        List<Class> classes = new ArrayList<>();
        for (var classCtx : ctx.classes) {
            classes.add((Class) visit(classCtx));
        }
        return new Program(classes, ctx.getStart());
    }

    @Override
    public ASTNode visitClassDef(CoolParser.ClassDefContext ctx) {
        String className = ctx.name.getText();
        String inherits = (ctx.parent != null) ? ctx.parent.getText() : null;

        List<Feature> features = new ArrayList<>();
        for (var featureCtx : ctx.features) {
            features.add((Feature) visit(featureCtx));
        }

        return new Class(className, inherits, features, ctx.name);
    }

    @Override
    public ASTNode visitMethod(CoolParser.MethodContext ctx) {
        String name = ctx.id.getText();
        Token type = ctx.type; // Constructorul tău așteaptă Token
        Expression body = (Expression) visit(ctx.e);

        List<FormalPrm> formals = new ArrayList<>();
        if (ctx.formals != null) {
            for (var formalCtx : ctx.formals) {
                formals.add((FormalPrm) visit(formalCtx));
            }
        }
        return new FuncDef(name, type, formals, body, ctx.id);
    }

    @Override
    public ASTNode visitAttr(CoolParser.AttrContext ctx) {
        String name = ctx.id.getText();
        Token type = ctx.type;
        Expression init = null;
        if (ctx.e != null) {
            init = (Expression) visit(ctx.e);
        }
        return new VarDef(name, type, init, ctx.id);
    }

    @Override
    public ASTNode visitFormal(CoolParser.FormalContext ctx) {
        String name = ctx.id.getText();
        Token type = ctx.type;
        return new FormalPrm(name, type, ctx.id);
    }

    public ASTNode visitCaseBranch(CoolParser.CaseBranchContext ctx) {
        String name = ctx.id.getText();
        Token type = ctx.type;
        Expression expr = (Expression) visit(ctx.e);
        return new caseBranch(name, type, expr, ctx.id);
    }

    public ASTNode visitCase(CoolParser.CaseContext ctx) {
        Expression expr = (Expression) visit(ctx.predicate);
        List<caseBranch> branches = new ArrayList<>();
        for (var branchCtx : ctx.branches) {
            branches.add((caseBranch) visit(branchCtx));
        }
        return new Case(expr, branches, ctx.CASE().getSymbol());
    }


    @Override
    public ASTNode visitAssign(CoolParser.AssignContext ctx) {
        String varName = ctx.id.getText();
        Expression expr = (Expression) visit(ctx.e);
        return new Assign(varName, expr, ctx.id);
    }

    @Override
    public ASTNode visitDispatch(CoolParser.DispatchContext ctx) {
        Expression prefix = (Expression) visit(ctx.caller);
        Token type = (ctx.type != null) ? ctx.type : null;
        Token name = ctx.methodName;
        List<Expression> args = visitExprList(ctx.args);
        return new Call(prefix, type, name, args, ctx.getStart());
    }

    @Override
    public ASTNode visitImplicitDispatch(CoolParser.ImplicitDispatchContext ctx) {
        Token name = ctx.methodName;
        List<Expression> args = visitExprList(ctx.args);
        return new InitCall(name, args, ctx.getStart());
    }

    @Override
    public ASTNode visitIf(CoolParser.IfContext ctx) {
        Expression cond = (Expression) visit(ctx.predicate);
        Expression then = (Expression) visit(ctx.thenBody);
        Expression elseB = (Expression) visit(ctx.elseBody);
        return new If(cond, then, elseB, ctx.IF().getSymbol());
    }

    @Override
    public ASTNode visitWhile(CoolParser.WhileContext ctx) {
        Expression cond = (Expression) visit(ctx.predicate);
        Expression body = (Expression) visit(ctx.loopBody);
        return new While(cond, body, ctx.WHILE().getSymbol());
    }

    @Override
    public ASTNode visitBlock(CoolParser.BlockContext ctx) {
        List<Expression> exprs = visitExprList(ctx.exprs);
        return new Block(exprs, ctx.LBRACE().getSymbol());
    }

    @Override
    public ASTNode visitLet(CoolParser.LetContext ctx) {
        List<LocalPrm> locals = new ArrayList<>();
        for (var localCtx : ctx.localParams) {
            locals.add((LocalPrm) visit(localCtx));
        }
        Expression body = (Expression) visit(ctx.letBody);
        return new Let(locals, body, ctx.LET().getSymbol());
    }

    @Override
    public ASTNode visitLocalVarDef(CoolParser.LocalVarDefContext ctx) {
        String name = ctx.id.getText();
        Token type = ctx.type;
        Expression init = null;
        if (ctx.e != null) {
            init = (Expression) visit(ctx.e);
        }
        return new LocalPrm(name, type, init, ctx.id);
    }

    @Override
    public ASTNode visitIsvoid(CoolParser.IsvoidContext ctx) {
        Expression expr = (Expression) visit(ctx.e);
        return new UnaryOp(expr, ctx.ISVOID().getSymbol(), ctx.getStart());
    }

    @Override
    public ASTNode visitComplement(CoolParser.ComplementContext ctx) {
        Expression expr = (Expression) visit(ctx.e);
        return new UnaryOp(expr, ctx.COMPLEMENT().getSymbol(), ctx.getStart());
    }

    @Override
    public ASTNode visitNot(CoolParser.NotContext ctx) {
        Expression expr = (Expression) visit(ctx.e);
        return new UnaryOp(expr, ctx.NOT().getSymbol(), ctx.getStart());
    }

    @Override
    public ASTNode visitRelational(CoolParser.RelationalContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOp(left, right, ctx.op, ctx.op);
    }

    @Override
    public ASTNode visitPlusMinus(CoolParser.PlusMinusContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOp(left, right, ctx.op, ctx.op);
    }

    @Override
    public ASTNode visitMultDiv(CoolParser.MultDivContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new BinaryOp(left, right, ctx.op, ctx.op);
    }

    @Override
    public ASTNode visitNew(CoolParser.NewContext ctx) {
        return new New(ctx.type, ctx.NEW().getSymbol());
    }

    @Override
    public ASTNode visitParen(CoolParser.ParenContext ctx) {
        return visit(ctx.e);
    }

    @Override
    public ASTNode visitObject(CoolParser.ObjectContext ctx) {
        return new Id(ctx.OBJECT().getSymbol());
    }

    @Override
    public ASTNode visitInt(CoolParser.IntContext ctx) {
        return new Int(ctx.INT().getSymbol());
    }

    @Override
    public ASTNode visitString(CoolParser.StringContext ctx) {
        return new StringT(ctx.STRING().getSymbol());
    }

    @Override
    public ASTNode visitTrue(CoolParser.TrueContext ctx) {
        return new Booll(ctx.TRUE().getSymbol());
    }

    @Override
    public ASTNode visitFalse(CoolParser.FalseContext ctx) {
        return new Booll(ctx.FALSE().getSymbol());
    }

}