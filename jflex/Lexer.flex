/* JFlex example: part of Java language lexer specification */

/**
 * This class is a simple example lexer.
 */

package llama;

%%

%class Lexer
%public
%unicode
%line
%column
%type String

%{
  int tokenLength;
  int tokenStart;
  StringBuffer string = new StringBuffer();

  private String token(String type) {
	return { type, yychar, this.yylength() }
  } 
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]+

/* comments */
Comment = ";" {InputCharacter}* {LineTerminator}?

Identifier = [:jletter:] [:jletterdigit:]*

/* integer literals */
DecIntegerLiteral = 0 | [1-9][0-9]*
DecLongLiteral    = {DecIntegerLiteral} [lL]

HexIntegerLiteral = 0 [xX] 0* {HexDigit} {1,8}
HexLongLiteral    = 0 [xX] 0* {HexDigit} {1,16} [lL]
HexDigit          = [0-9a-fA-F]

OctIntegerLiteral = 0+ [1-3]? {OctDigit} {1,15}
OctLongLiteral    = 0+ 1? {OctDigit} {1,21} [lL]
OctDigit          = [0-7]
    
/* floating point literals */        
FloatLiteral  = ({FLit1}|{FLit2}|{FLit3}) {Exponent}? [fF]
DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?

FLit1    = [0-9]+ \. [0-9]* 
FLit2    = \. [0-9]+ 
FLit3    = [0-9]+ 
Exponent = [eE] [+-]? [0-9]+

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]

%state STRING, CHARLITERAL

%%

<YYINITIAL> {

  /* keywords */
  "fn"             |
  "fn*"            |
  "if"             |
  "def"            |
  "let"            |
  "let*"           |
  "loop*"          |
  "new"            |
  "nil"            |
  "recur"          |
  "loop"           |
  "do"             |
  "quote"          |
  "the-var"        |
  "identical?"     |
  "throw"          |
  "set!"           |
  "monitor-enter"  |
  "monitor-exit"   |
  "try"            |
  "catch"          |
  "finally"        |
  "in-ns"          { return "keyword"; }

  /* Built-ins */
  "*agent*"                   |
  "*command-line-args*"       |
  "*in*"                      |
  "*macro-meta*"              |
  "*ns*"                      |
  "*out*"                     |
  "*print-meta*"              |
  "*print-readably*"          |
  "*proxy-classes*"           |
  "*warn-on-reflection*"      |
  "+"                         |
  "-"                         |
  "->"                        |
  ".."                        |
  "/"                         |
  "<"                         |
  "<="                        |
  "="                         |
  "=="                        |
  ">"                         |
  ">="                        |
  "accessor"                  |
  "agent"                     |
  "agent-errors"              |
  "aget"                      |
  "alength"                   |
  "all-ns"                    |
  "alter"                     |
  "and"                       |
  "apply"                     |
  "array-map"                 |
  "aset"                      |
  "aset-boolean"              |
  "aset-byte"                 |
  "aset-char"                 |
  "aset-double"               |
  "aset-float"                |
  "aset-int"                  |
  "aset-long"                 |
  "aset-short"                |
  "assert"                    |
  "assoc"                     |
  "await"                     |
  "await-for"                 |
  "bean"                      |
  "binding"                   |
  "bit-and"                   |
  "bit-not"                   |
  "bit-or"                    |
  "bit-shift-left"            |
  "bit-shift-right"           |
  "bit-xor"                   |
  "boolean"                   |
  "butlast"                   |
  "byte"                      |
  "cast"                      |
  "char"                      |
  "class"                     |
  "clear-agent-errors"        |
  "comment"                   |
  "commute"                   |
  "comp"                      |
  "comparator"                |
  "complement"                |
  "concat"                    |
  "cond"                      |
  "conj"                      |
  "cons"                      |
  "constantly"                |
  "construct-proxy"           |
  "contains?"                 |
  "count"                     |
  "create-ns"                 |
  "create-struct"             |
  "cycle"                     |
  "dec"                       |
  "defmacro"                  |
  "defmethod"                 |
  "defmulti"                  |
  "defn"                      |
  "defn-"                     |
  "defstruct"                 |
  "deref"                     |
  "destructure"               |
  "disj"                      |
  "dissoc"                    |
  "distinct"                  |
  "doall"                     |
  "doc"                       |
  "dorun"                     |
  "doseq"                     |
  "dosync"                    |
  "dotimes"                   |
  "doto"                      |
  "double"                    |
  "drop"                      |
  "drop-while"                |
  "ensure"                    |
  "eval"                      |
  "every?"                    |
  "false?"                    |
  "ffirst"                    |
  "file-seq"                  |
  "filter"                    |
  "find"                      |
  "find-doc"                  |
  "find-ns"                   |
  "find-var"                  |
  "first"                     |
  "float"                     |
  "flush"                     |
  "fnseq"                     |
  "for"                       |
  "frest"                     |
  "gensym"                    |
  "gen-class"                 |
  "gen-interface"             |
  "get"                       |
  "get-proxy-class"           |
  "hash-map"                  |
  "hash-set"                  |
  "identity"                  |
  "if-let"                    |
  "import"                    |
  "inc"                       |
  "instance?"                 |
  "int"                       |
  "interleave"                |
  "into"                      |
  "into-array"                |
  "iterate"                   |
  "key"                       |
  "keys"                      |
  "keyword"                   |
  "keyword?"                  |
  "last"                      |
  "lazy-cat"                  |
  "lazy-cons"                 |
  "line-seq"                  |
  "list"                      |
  "list*"                     |
  "load"                      |
  "load-file"                 |
  "locking"                   |
  "long"                      |
  "macroexpand"               |
  "macroexpand-1"             |
  "make-array"                |
  "map"                       |
  "map?"                      |
  "mapcat"                    |
  "max"                       |
  "max-key"                   |
  "memfn"                     |
  "merge"                     |
  "merge-with"                |
  "meta"                      |
  "min"                       |
  "min-key"                   |
  "name"                      |
  "namespace"                 |
  "neg?"                      |
  "newline"                   |
  "nil?"                      |
  "not"                       |
  "not-any?"                  |
  "not-every?"                |
  "not="                      |
  "ns-imports"                |
  "ns-interns"                |
  "ns-map"                    |
  "ns-name"                   |
  "ns-publics"                |
  "ns-refers"                 |
  "ns-resolve"                |
  "ns-unmap"                  |
  "nth"                       |
  "nthrest"                   |
  "or"                        |
  "partial"                   |
  "peek"                      |
  "pmap"                      |
  "pop"                       |
  "pos?"                      |
  "pr"                        |
  "pr-str"                    |
  "print"                     |
  "print-doc"                 |
  "print-str"                 |
  "println"                   |
  "println-str"               |
  "prn"                       |
  "prn-str"                   |
  "proxy"                     |
  "proxy-mappings"            |
  "quot"                      |
  "rand"                      |
  "rand-int"                  |
  "range"                     |
  "re-find"                   |
  "re-groups"                 |
  "re-matcher"                |
  "re-matches"                |
  "re-pattern"                |
  "re-seq"                    |
  "read"                      |
  "read-line"                 |
  "reduce"                    |
  "ref"                       |
  "ref-set"                   |
  "refer"                     |
  "rem"                       |
  "remove-method"             |
  "remove-ns"                 |
  "repeat"                    |
  "replace"                   |
  "replicate"                 |
  "require"                   |
  "resolve"                   |
  "rest"                      |
  "resultset-seq"             |
  "reverse"                   |
  "rfirst"                    |
  "rrest"                     |
  "rseq"                      |
  "scan"                      |
  "second"                    |
  "select-keys"               |
  "send"                      |
  "send-off"                  |
  "seq"                       |
  "seq?"                      |
  "set"                       |
  "short"                     |
  "slurp"                     |
  "some"                      |
  "sort"                      |
  "sort-by"                   |
  "sorted-map"                |
  "sorted-map-by"             |
  "sorted-set"                |
  "special-symbol?"           |
  "split-at"                  |
  "split-with"                |
  "str"                       |
  "string?"                   |
  "struct"                    |
  "struct-map"                |
  "subs"                      |
  "subvec"                    |
  "symbol"                    |
  "symbol?"                   |
  "sync"                      |
  "take"                      |
  "take-nth"                  |
  "take-while"                |
  "test"                      |
  "time"                      |
  "to-array"                  |
  "to-array-2d"               |
  "touch"                     |
  "tree-seq"                  |
  "true?"                     |
  "update-proxy"              |
  "val"                       |
  "vals"                      |
  "var-get"                   |
  "var-set"                   |
  "var?"                      |
  "vector"                    |
  "vector?"                   |
  "when"                      |
  "when-first"                |
  "when-let"                  |
  "when-not"                  |
  "while"                     |
  "with-local-vars"           |
  "with-meta"                 |
  "with-open"                 |
  "with-out-str"              |
  "xml-seq"                   |
  "zero?"                     |
  "zipmap"                    |
  "repeatedly"                |
  "add-classpath"             |
  "vec"                       |
  "hash"                      { return "keyword2"; }


  /* operators */

  "("                            { return "lparan"; }
  ")"                            { return "rparan"; }
  "{"                            { return "lcbracket"; }
  "}"                            { return "rcbracket"; }
  "["                            { return "lbracket"; }
  "]"                            { return "rbracket"; }
  
  /* string literal */
  \"                             {  
                                    yybegin(STRING); 
                                    tokenStart = yychar; 
                                    tokenLength = 1; 
                                 }

  /* character literal */
  \'                             {  
                                    yybegin(CHARLITERAL); 
                                    tokenStart = yychar; 
                                    tokenLength = 1; 
                                 }

  /* numeric literals */

  {DecIntegerLiteral}            |
  {DecLongLiteral}               |
  
  {HexIntegerLiteral}            |
  {HexLongLiteral}               |
 
  {OctIntegerLiteral}            |
  {OctLongLiteral}               |
  
  {FloatLiteral}                 |
  {DoubleLiteral}                |
  {DoubleLiteral}[dD]            { return "number"; }
  
  /* comments */
  {Comment}                      { return "comment"; }

  /* whitespace */
  {WhiteSpace}                   { }

  /* identifiers */ 
  {Identifier}                   { return "identifier"; }
}


<STRING> {
  \"                             { 
                                     yybegin(YYINITIAL); 
                                     // length also includes the trailing quote
                                     return new String(Integer.toString(tokenStart) + Integer.toString(tokenLength));
                                 }
  
  {StringCharacter}+             { tokenLength += yylength(); }

  \\[0-3]?{OctDigit}?{OctDigit}  { tokenLength += yylength(); }
  
  /* escape sequences */

  \\.                            { tokenLength += 2; }
  {LineTerminator}               { yybegin(YYINITIAL);  }
}

<CHARLITERAL> {
  \'                             { 
                                     yybegin(YYINITIAL); 
                                     // length also includes the trailing quote
                                     return new String(Integer.toString(tokenStart) + Integer.toString(tokenLength));
                                 }
  
  {SingleCharacter}+             { tokenLength += yylength(); }
  
  /* escape sequences */

  \\.                            { tokenLength += 2; }
  {LineTerminator}               { yybegin(YYINITIAL);  }
}

/* error fallback */
.|\n                             {  }
<<EOF>>                          { return null; }
