lexer grammar CoolLexer;

@header{
    package cool.lexer;
    import java.util.regex.Pattern;
}

tokens { ERROR }

@members{
    private void raiseError(String msg) {
        setText(msg);
        setType(ERROR);
    }

    private void processString() {
        String rawText = getText();
        if (rawText.contains("\u0000")) {
            raiseError("String contains null character");
            return;
        }

        String noQuotes = rawText.substring(1, rawText.length() - 1);
        StringBuilder processed = new StringBuilder();

        for (int i = 0; i < noQuotes.length(); i++) {
            char c = noQuotes.charAt(i);
            if (c == '\\' && i + 1 < noQuotes.length()) {
                char next = noQuotes.charAt(i + 1);
                switch (next) {
                    case 'n': processed.append('\n'); break;
                    case 't': processed.append('\t'); break;
                    case 'b': processed.append('\b'); break;
                    case 'f': processed.append('\f'); break;
                    default:
                        processed.append(next); break;
                }
                i++;
            } else {
                processed.append(c);
            }
        }
        if (processed.length() > 1024) {
            raiseError("String constant too long");
            return;
        }
        setText(processed.toString());
    }

}

ML_COMMENT
     : '(*' ( ML_COMMENT | . )*? ('*)' {skip();} | EOF { raiseError("EOF in comment"); })
     ;

UNMATCHED_COMMENT_END
    : '*)' { raiseError("Unmatched *)"); }
    ;

STRING
    : '"' ( '\\' . | ~["\\\n\r] )*? '"' { processString(); }
    ;

UNTERMINATED_STRING
    : '"' ( '\\' . | ~["\\\n\r\u0000] )* EOL
      { raiseError("Unterminated string constant"); }
    ;

EOF_IN_STRING
    : '"' ( '\\' . | ~["\\\n\r\u0000] )* EOF
      { raiseError("EOF in string constant"); }
    ;
WS:   [ \n\f\r\t]+ -> skip;

LINE_COMMENT:'--' ~[\n\r]* -> skip;

CLASS: C L A S S;
ELSE: E L S E;
FI: F I;
IF: I F;
IN: I N;
INHERITS: I N H E R I T S;
ISVOID: I S V O I D;
LET: L E T;
LOOP: L O O P;
POOL: P O O L;
THEN: T H E N;
WHILE: W H I L E;
CASE: C A S E;
ESAC: E S A C;
NEW: N E W;
OF: O F;
NOT: N O T;

TRUE: 't' R U E;
FALSE: 'f' A L S E;

TYPE: UPPER (LETTER | DIGIT | '_')*;

OBJECT: LOWER (LETTER | DIGIT | '_')*;

INT: DIGIT+;

COMPLEMENT: '~';
DOT: '.';
AT: '@';
COLON : ':';
SEMICOLON : ';';
COMMA : ',';
ASSIGN : '<-';
LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
PLUS : '+';
MINUS : '-';
MULT : '*';
DIV : '/';
EQUAL : '=';
LESS_THAN : '<';
LESS_EQ : '<=';
CASE_BRANCH: '=>';

fragment EOL: '\r\n' | '\n' | '\r';

fragment DIGIT: [0-9];
fragment UPPER: [A-Z];
fragment LOWER: [a-z];
fragment LETTER: UPPER | LOWER;

fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment K: [kK];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Y: [yY];
fragment Z: [zZ];

INVALID_CHAR : . { raiseError("Invalid character: " + getText()); };