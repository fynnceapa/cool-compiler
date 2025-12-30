parser grammar CoolParser;

options {
    tokenVocab = CoolLexer;
}

@header{
    package cool.parser;
}

program
    :   (classes+=definition SEMICOLON)+ EOF
    ;

definition
    :   CLASS name=TYPE (INHERITS parent=TYPE)? LBRACE (features+=feature SEMICOLON)* RBRACE # classDef
    ;
feature
    :   id=OBJECT LPAREN (formals+=formal (COMMA formals+=formal)*)? RPAREN COLON type=TYPE LBRACE e=expr RBRACE # method
    |   id=OBJECT COLON type=TYPE (ASSIGN e=expr)?                                        # attr
    ;

formal
    :   id=OBJECT COLON type=TYPE
    ;

expr
    :   left=expr op=(MULT | DIV) right=expr                                              # multDiv
    |   left=expr op=(PLUS | MINUS) right=expr                                            # plusMinus
    |   left=expr op=(LESS_THAN | LESS_EQ | EQUAL) right=expr                             # relational
    |   NOT e=expr                                                                        # not
    |   ISVOID e=expr                                                                     # isvoid
    |   COMPLEMENT e=expr                                                                 # complement
    |   caller=expr (AT type=TYPE)? DOT methodName=OBJECT LPAREN (args+=expr (COMMA args+=expr)*)? RPAREN # dispatch
    |   methodName=OBJECT LPAREN (args+=expr (COMMA args+=expr)*)? RPAREN                 # implicitDispatch
    |   IF predicate=expr THEN thenBody=expr ELSE elseBody=expr FI                        # if
    |   WHILE predicate=expr LOOP loopBody=expr POOL                                      # while
    |   LET localParams+=local (COMMA localParams+=local)* IN letBody=expr                # let
    |   CASE predicate=expr OF (branches+=branch)+ ESAC                                   # case
    |   <assoc=right> id=OBJECT ASSIGN e=expr                                             # assign
    |   LBRACE (exprs+=expr SEMICOLON)+ RBRACE                                            # block
    |   NEW type=TYPE                                                                     # new
    |   LPAREN e=expr RPAREN                                                              # paren
    |   OBJECT                                                                            # object
    |   INT                                                                               # int
    |   STRING                                                                            # string
    |   TRUE                                                                              # true
    |   FALSE                                                                             # false
    ;

local
    :   id=OBJECT COLON type=TYPE (ASSIGN e=expr)? # localVarDef
    ;

branch
    :   id=OBJECT COLON type=TYPE CASE_BRANCH e=expr SEMICOLON # caseBranch
    ;