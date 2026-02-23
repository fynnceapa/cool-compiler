# Compilator COOL (Classroom Object Oriented Language)

Acest proiect reprezintă implementarea unui compilator complet pentru limbajul COOL, dezvoltat în Java utilizând instrumentul ANTLR4 pentru generarea parser-ului. Compilatorul transformă codul sursă COOL în cod de asamblare MIPS.

## Structura Proiectului

Arhitectura compilatorului este organizată pe etape, utilizând pattern-ul **Visitor** pentru parcurgerea Arborelui Sintactic Abstract (AST):

* **Analiză Lexicală și Sintactică**: Realizată cu ANTLR4, folosind definițiile din `CoolLexer.g4` și `CoolParser.g4`.
* **Construirea AST**: Clasa `ASTBuilderVisitor` convertește arborele de derivare ANTLR în noduri de tip `ASTNode`.
* **Analiză Semantică**: Se desfășoară în trei etape principale:
    * `DefinitionPassVisitor`: Identifică clasele și ierarhia de moștenire.
    * `ResolutionPassVisitor`: Verifică domeniile de vizibilitate și rezolvă simbolurile.
    * `TypeCheckVisitor`: Realizează verificarea tipurilor (type checking).
* **Generarea Codului**: `CodeGenVisitor` utilizează template-uri StringTemplate (`cgen.stg`) pentru a genera codul final.