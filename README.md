# Tema 3 - Generare de cod

## Obiective

Obiectivele temei sunt următoarele:

- **Generarea de cod** MIPS pornind de la programe Cool.

- Utilizarea opțională a unui **instrument** dedicat pentru generarea de cod, `StringTemplate`.

## Descriere

Tema abordează ultima etapă a construcției compilatorului pentru limbajul Cool, generarea de cod.
Aceasta pornește de la reprezentarea intermediară elaborată în etapa anterioară, de analiză semantică, în forma unui arbore de sintaxă abstractă, adnotat cu simboluri și tipuri.
Rezultatul acestei etape îl constituie un **program MIPS** echivalent cu programului Cool inițial.
Acesta va putea fi rulat în simulatoarele `QtSpim` sau `spim`.

Tema de față va primi ca parametri în linia de comandă numele unuia sau mai multor fișiere conținând programe Cool, și va tipări la *standard output* **programul MIPS** dorit.
Programele de test vor fi **corecte lexical, sintactic și semantic**!

Punctul de plecare îl constituie **sistemul de execuție** Cool, sub forma unui fișier *exception handler*, `trap.handler.nogc`, încărcat în simulator.
Printre altele, acesta conține mecanisme de **alocare dinamică** a memoriei, precum și implementarea **metodelor predefinite**, din clasele `Object`, `IO` și `String`, pe care le veți invoca din codul generat de voi.

Mai jos, funcționalitatea este exemplificată pornind de la cel mai simplu program Cool:

```js
class Main {
    main() : Object { 0 };
};
```

Un posibil program MIPS echivalent este ilustrat în fișierul [`main.s`](./main.s).
În continuare, este prezentată **structura de ansamblu** a acestuia, urmând ca detaliile să fie furnizate în secțiunea [Cerințe](#cerințe).

- Ca în orice program MIPS, se observă cele două **zone de memorie**, `data` (linia 1), respectiv `text` (linia 186).

- Zona `data` debutează cu o serie de definiții **globale** (liniile 3 - 11), utilizând directiva `.globl`, care sunt vizibile și în alte programe MIPS încărcate concomitent.
În particular, este vorba despre **sistemul de execuție** menționat mai sus, care utilizează anumite etichete generat de voi.

- Cele trei etichete, `_int_tag`, `_string_tag` și `_bool_tag` (liniile 12 - 17) precizează **etichetele** celor trei clase predefinite, fiind utilizate de sistemul de execuție pentru implementarea testului de **egalitate** între valori predefinite.

- În liniile 18 - 66, sunt definiți toți **literalii șir de caractere** din program, incluzând **numele claselor**, și eventual ale **fișierelor**, în anumite cazuri.
Reprezentarea acestor literali este discutată ulterior.

- În liniile 67 - 91, sunt definiți toți **literalii întregi** din program.
Reprezentarea acestora este discutată ulterior.

- În liniile 92 - 101, sunt definiți cei doi **literalii booleeni**.
Reprezentarea acestora este discutată ulterior.

- Liniile 102 - 108 ilustrează **tabelul numelor de clase** (`class_nameTab`), necesar pentru afișarea anumitor mesaje de eroare de către sistemul de execuție.
**Atenție!** Tabelul este indexat de **eticheta** clasei, reprezentată printr-un număr întreg.

- Liniile 109 - 121 ilustrează **tabelul prototipurilor și al rutinelor de inițializare** ale instanțelor claselor (`class_objTab`), discutate mai jos.
Tabelul este necesar pentru construcția `new SELF_TYPE`, care alocă un nou obiect cu tipul dinamic al lui `self`.
**Atenție!** Tabelul este indexat în funcție de **eticheta** clasei, reprezentată printr-un număr întreg.

- Liniile 122 - 150 conțin **prototipurile instanțelor** fiecărei clase din program (`<class>_protObj`), atât predefinite, cât și definite de programator.
Prototipurile respectă structura discutată la curs.

- Liniile 151 - 182 surprind **tabelele de metode** aferente fiecărei clase din program (_dispatch table_, `<class>_dispTab`), ca secvență de etichete din zona `text`, unde se găsesc implementările acestor metode.

- Liniile 184 - 185 marchează finalul zonei statice și începutul zonei
de ***heap*** (`heap_start`), în conformitate cu cerințele sistemului de execuție.

- Zona `text` debutează în linia 186 cu câteva definiții **globale**, solicitate de sistemul de execuție.

- Începând cu linia 192, sunt definite **rutinele de inițializare** a instanțelor fiecărei clase (`<class>_init`), și **metodele** din fiecare clasă, cu numele `<class>.<method>`.

Sistemul de execuție realizează următorii pași la **lansarea** unui program:

- Este instanțiată **clasa** `Main`.
Acest lucru se realizează copiind pe _heap_ obiectul prototip `Main_protObj`.

- Este apelată **rutina de inițializare** `Main_init()` pentru această nouă instanță.

- Este apelată **metoda** `Main.main()`, adresa instanței (`self`) fiind în registrul `$a0`, iar adresa de revenire, în registrul `$ra`.



## Cerințe

În continuare, sunt prezentate particularitățile de reprezentare și implementare pentru anumite construcții de limbaj.
Utilizați **semantica operațională** a limbajului Cool (secțiunea 13 a manualului) pentru a înțelege ce presupune evaluarea fiecărei expresii.
Pentru cazuri concrete, consultați testele (vezi secțiunea [Testare](#testare)).

**Atenție!** **NU** este necesar să generați cod identic cu cel din teste.
Acesta va fi verificat în raport cu funcționalitatea îndeplinită.

## Definiții de clase

Pentru fiecare definiție declasă, predefinită sau definită de programator, este necesară definirea unui **obiect prototip** (`<class>_protObj`) și a unui **tabel de metode** (`<class>_dispTab`) în zona `data`, precum și a unei **rutine de inițializare** (`<class>_init`) în zona `text`.

Reprezentarea în memorie a **obiectelor prototip** respectă convențiile discutate în curs, pe slide-urile 304 - 307, și este reluată în tabelul de mai jos.

| Câmp                  | _Offset_  |
|-----------------------|-----------|
| Etichetă clasă        | 0         |
| Dimensiune în cuvinte | 4         |
| _Dispatch pointer_    | 8         |
| Atribut 1             | 12        |
| Atribut 2             | 16        |
| $\vdots$              | $\vdots$  |

**Atenție!** Atributele din reprezentarea unui obiect le includ atât pe cele definite în clasa curentă, cât și pe toate cele de pe întregul lanț de **moștenire**!
Atributele vor fi inițializate la **valorile implicite** în funcție de tip (vezi slide-ul 333).

**Tabelele de metode** respectă convențiile de pe slide-urile 308 - 309, și cuprind toate metodele accesibile unui obiect, de pe întregul lanț de **moștenire**.
Spre exemplu, `IO_dispTab` include atât metodele specifice clasei `IO`, cât și pe cele ale clasei `Object`.

**Rutinele de inițializare** vizează **atributele** instanței curente.
De asemenea, este necesară inițializarea atributelor moștenite, printr-un apel la rutina de inițializare a **superclasei**.
Spre exemplu, `IO_init` invocă `Object_init`.
Acestea se comportă ca niște metode, care sunt discutate pe larg în secțiunea [aceasta](#definiții-și-apeluri-de-metode).
Ele primesc adresa obiectului curent în registrul `$a0`, unde trebuie să o și întoarcă.

## Valori predefinite

Instanțele clasei `Int` conțin drept unic atribut valoarea întreagă MIPS.
Spre exemplu, `int_const2` are valoarea întreagă `2`.

Instanțele clasei `String` conțin două atribute: adresa unui obiect `Int` cu lungimea șirului, urmat de șirul propriu-zis.
**Atenție!** **Dimensiunea** în cuvinte a unui obiect `String` va depinde de **lungimea** șirului!
Spre exemplu `str_const2`, redând șirul `"IO"`, are lungimea descrisă de `int_const2`, iar câmpul de dimensiune este `5`: 3 cuvinte pentru antet, 1 cuvânt pentru lungime, și 1 cuvânt pentru șirul `"IO"` plus caracterul terminator de șir ($2 + 1 = 3$ octeți, deci 1 cuvânt).
Având în vedere că un șir se poate încheia în mijlocul unui cuvânt, definițiile următoare trebuie **aliniate**, utilizând instrucțiunea `.align 2`.
De asemenea, este important să procesați șirurile înainte să le emiteți, deoarece pot conține **caractere speciale**, ca `\n`.

Instanțele clasei `Bool`, sunt similare celor ale clasei `Int`, unicul atribut fiind `0` sau `1`, pentru `false`, respectiv `true`.

void` este reprezentat prin întregul MIPS `0`.

## Literali

Pentru fiecare literal întreg, șir de caractere și boolean din program, există o definiție aferentă în zona `data`, respectând convențiile din secțiunea [Valori predefinite](#valori-predefinite).
În acest scop, este util să vă definiți **tabele de literali** indexate prin numele literalilor, pentru a le putea **refolosi** pe cele deja definite.

## Definiții și apeluri de metode

**Protocolul de apel** este similar celui din curs, slide-urile 288 - 295, cu câteva modificări, datorate

1. prezenței suplimentare a obiectului pentru care realizăm apelul, în afara parametrilor

2. faptului că sistemul de execuție Cool consideră că registrul `$fp` (*frame pointer*) este gestionat exclusiv de către **apelat**, nu și de apelant!

Prin urmare, salvarea registrului `$fp` se face tot de către apelat, la fel ca și reîncărcarea lui la final.
Această strategie este **mai eficientă**, întrucât, în codul generat, salvarea este menționată acum o singură dată pentru fiecare definiție de metodă, și nu la fiecare apel al acesteia, ca în curs.

În ceea ce privește obiectul `self`, adresa acestuia este depusă în registrul `$a0`.
Având în vedere că acesta va fi **modificat** în cursul execuției metodei, fie pentru a invoca alte metode, fie pentru a stoca rezultatul evaluării unei expresii, se recomandă **salvarea** acestui registru în `$s0`, pentru acces facil.
În consecință, vechea valoare a acestui registru trebuie ea însăși **salvată** pe stivă, alături de `$fp` și `$ra`.
Noua **înregistrare de activare** este ilustrată în figura de mai jos:

| Conținut      | Adresă     |
|---------------|------------|
| Parametru $n$ |            |
| $\vdots$      | $\vdots$   |
| Parametru 2   | `$fp + 16` |
| Parametru 1   | `$fp + 12` |
| `$fp`         |            |
| `$s0`         |            |
| `$ra`         | `$fp`      |
|               | `$sp`      |

Exemplul de cod din secțiunea [Descriere](#descriere) reflectă acest protocol.

În vederea implementării **apelurilor dinamice** de metodă (_dynamic dispatch_), este necesară accesarea intrării corecte din tabelul de metode aferent tipului dinamic al obiectului.
Odată ce încărcați într-un registru adresa metodei, o puteți invoca utilizând insturcțiunea `jalr` (_jump and link register_), care, similar instrucțiunii `jal`, depune în registrul `$ra` adresa de revenire.

În cazul în care apelul se realizează pentru un obiect `void`, este necesar să invocați rutina predefinită `_dispatch_abort` (vezi secțiunea [Metode și rutine predefinite](#metode-și-rutine-predefinite)).

## Construcția `let`

Variabilele de `let` sunt alocate pe **stivă**.
Puteți utiliza orice schemă de stocare a acestora.
Modelele din teste utilizează convenția din figura de mai jos:

| Conținut            | Adresă     |
|---------------------|------------|
| Parametru $n$       |            |
| $\vdots$            | $\vdots$   |
| Parametru 2         | `$fp + 16` |
| Parametru 1         | `$fp + 12` |
| `$fp`               |            |
| `$s0`               |            |
| `$ra`               | `$fp`      |
| Variabilă `let` 1   | `$fp - 4`  |
| Variabilă `let` 2   | `$fp - 8`  |
| $\vdots$            | $\vdots$   |
| Variabilă `let` $m$ |            |
|                     | `$sp`      |

## Construcția `new`

Instrucțiunea `new` presupune alocarea dinamică a unui nou obiect pe _heap_, pornind de la obiectele prototip din zona statică.
În cazul în care `new` este utilizat cu un **nume de clasă**, e.g. `new C`, se aplică mai întâi metoda predefinită `Object.copy()` asupra obiectului prototip `C_protObj` (vezi secțiunea [Metode și rutine predefinite](#metode-și-rutine-predefinite)), iar apoi, asupra noului obiect, rutina de inițializare `C_init()`.

În cazul expresiei `new SELF_TYPE`, este utilizat tabelul `class_objTab`, indexat pornind de la eticheta clasei reprezentând tipul dinamic al lui `self`.
Astfel, dacă eticheta este `tag`, obiectul prototip se găsește în tabel la _offset-ul_ `8 * tag`$, iar rutina de inițializare, la `8 * tag + 4`.

## Verificarea egalității obiectelor

Pentru verificarea egalității obiectelor, este necesară mai întâi testarea egalității **referințelor**.
Dacă acestea sunt diferite, trebuie invocată rutina predefinită `equality_test()` (vezi secțiunea [Metode și rutine predefinite](#metode-și-rutine-predefinite)), care verifică egalitatea de **conținut** în cazul valorilor predefinite, de tip `Int`, `String` și `Bool`.

## Construcția `case`

Construcția `case` solicită prezența la execuție a anumitor informații despre ierarhia de clase.
O observație utilă este că, asociind etichete claselor în ordinea dată de **parcurgerea în adâncime** a arborelui de moștenire, fiecare clasă și toate subclasele ei vor ocupa un interval de etichete consecutive, e.g.
`5`, `6` și `7`, astfel încât extremele acestui interval pot fi utilizate pentru a surprinde orice tip dinamic aferent unei anumite ramuri a construcției `case`.

În cazul în care nicio ramură **nu se potrivește**, este necesară invocarea rutinei predefinite `_case_abort()`.
În cazul în care obiectul analizat este `void`, este necesară invocarea rutinei predefinite `_case_abort2()` (vezi secțiunea [Metode și rutine predefinite](#metode-și-rutine-predefinite)).

## Metode și rutine predefinite

Secțiunea prezintă metodele și rutinele predefinite, și modalitatea lor de invocare:

- `Object.copy()`: Copiază pe *heap- obiectul cu adresa în registrul `$a0` și depune adresa noului obiect tot în `$a0`.

- `Object.abort()`: Afișează numele tipului dinamic al obiectului cu adresa în `$a0` și încheie execuția programului.
Tabelul `class_nameTab` este accesat în acest scop.

- `Object.type_name()`: Întoarce în `$a0` numele tipului dinamic al obiectului cu adresa în `$a0`.

- `IO.out_string()`: Afișează obiectul șir de caractere primit ca parametru.

- `IO.out_int()`: Afișează obiectul număr întreg primit ca parametru.

- `IO.in_string()`: Întoarce în `$a0` adresa obiectului șir de caractere citit de la tastatură.

- `IO.in_int()`: Întoarce în `$a0` adresa obiectului număr întreg citit de la tastatură.

- `String.length()`: Întoarce în `$a0` adresa obiectului număr întreg care reprezintă lungimea obiectului șir de caractere cu adresa în `$a0`.

- `String.concat()`: Întoarce în `$a0` adresa unui **nou** obiect șir de caractere, rezultat din concatenarea obiectului cu adresa în `$a0` cu obiectul primit ca parametru.

- `String.substr()`: Întoarce în `$a0` adresa unui nou obiect șir de caractere, reprezentând subșirul obiectului cu adresa în `$a0`, determinat de index-ul și lungimea date ca parametri.

- `equality_test()`: Verifică dacă obiectele cu adresele în `$t1` și `$t2` au același tip dinamic predefinit și aceeași valoare.
Dacă da, este întoarsă valoarea din `$a0`; altfel, din `$a1`.

- `_dispatch_abort()`: Este utilizată în cazul unui apel de metodă pe un obiect `void`.
Afișează un mesaj de eroare, ce include numele fișierului, ca obiect șir de caractere, cu adresa în `$a0`, și numărul liniei din fișier, ca întreg MIPS (utilizați instrucțiunea `li`) în `$t1`.
Încheie execuția programului.

- `_case_abort()`: Este utilizată când **nu** se realizează potrivire cu nicio ramură a instrucțiunii `case`.
Afișează un mesaj de eroare ce include numele tipului dinamic al obiectului cu adrsa în `$a0` și încheie execuția programului.

- `_case_abort2()`: Este utilizată când instrucțiunea `case` analizează un obiect `void`.
Similar cu `_dispatch_abort`, afișează un mesaj de eroare cu numele fișierului (`$a0`) și linia (`$t1`) aferente obiectului problematic, și încheie execuția programului.

## Testare

Testele pot fi **rulate** pe Linux, ruland comanda `./checker.sh checker` în acest repository.

Pentru rularea manuală a unui program MIPS în simulatorul **spim**, în linia de comandă, utilizați:

```console
$ spim -exception_file trap.handler.nogc -file <file.s>
```

Pentru rularea în **QtSpim**, selectați `Simulator -> Settings -> MIPS -> Load exception handler` și alegeți fișierul `trap.handler.nogc`.
Apoi, în-cărcați programul dorit utilizând butonul `Reinitialize and Load File`.

**Testele** se află în directorul `checker/tests` din rădăcina acestui repository.
Fișierele `.cl` conțin programe Cool de compilat.
Modele de programe MIPS echivalente sunt furnizate în fișierele `.s-model`.
Fișierele `.ref` conțin ieșirile de referință ale simulatorului.
Pentru fiecare test, sistemul de testare redirectează ieșirea standard a compilatorului către un fișier `.s`, pe care îl execută în simulator, și stochează ieșirea acestuia într-un fișier `.out`, pe care îl compară apoi cu cel de referință.

Având în vedere că testele verifică **incremental** funcționalitatea compilatorului, le puteți folosi pentru a vă ghida **dezvoltarea** temei!

Primele patru teste nu afișează nimic.
Scopul lor este de a vă ajuta să organizați codul generat pentru ierarhia de clase.
Ultimul test, 32, este singurul care citește intrarea de la consolă, folosind metoda `IO.in_int`.
Sistemul de testare fixează intrarea acestuia la `5`, astfel încât să nu fie necesară introducerea manuală a numărului de fiecare dată când rulați testele.

## Precizări

- Clasele, variabilele și metodele trebuie **indexate**, pentru a le putea referi corect în codul generat.
Spre exemplu, clasele necesită etichete, atributele trebuie referite în funcț``ie de _offset-ul_ în cadrul reprezentării obiectului din care fac parte, metodele sunt caracterizate de _offset-ul_ în cadrul tabelelor de metode, parametrii formali, de _offset-ul_ pe stivă etc.
Puteți reține aceste informații în **simbolurile** asociate.

- `StringTemplate` posedă anumite mecanisme mai avansate, pe care le puteți exploata în rezolvarea temei.
Spre exemplu, apelarea metodei `add` pe un *template*, utilizând un obiect `List` ca valoare, este echivalentă cu secvența de apeluri `add` pentru fiecare element al listei.
_Template-urile_ pot fi aplicate asupra fiecărei valori a unui atribut (*mapping*), utilizând sintaxa `<attr:transform()>`, unde `attr` este un atribut cu mai multe valori, iar `transform` este un _template_ cu un singur parametru.
De asemenea, există și _template-ul_ special `<if>`, prin care puteți cosmetiza formatarea.
Găsiți mai multe detalii în documentația de la adresa [aceasta](https://github.com/antlr/stringtemplate4/blob/master/doc/index.md).

## Referințe

1. [Manualul limbajului Cool](https://curs.upb.ro/2024/pluginfile.php/96267/mod_folder/content/0/Cool%20-%20Manual.pdf)

1. [Documentația sistemului de execuție al limbajului Cool](https://curs.upb.ro/2024/pluginfile.php/96267/mod_folder/content/0/Cool%20-%20Sistemul%20de%20execu%C8%9Bie.pdf)

1. [Manualul SPIM](https://curs.upb.ro/2024/pluginfile.php/96267/mod_folder/content/0/SPIM%20-%20Manual.pdf)

1. [Documentația `StringTemplate`](https://github.com/antlr/stringtemplate4/blob/master/doc/index.md)