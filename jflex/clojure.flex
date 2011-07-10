/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package llama;

import jsyntaxpane.DefaultLexer;
import jsyntaxpane.Token;
import jsyntaxpane.TokenType;
 
%% 

%public
%class ClojureLexer
%extends DefaultLexer
%final
%unicode
%char
%type Token


%{
    /**
     * Create an empty lexer, yyrset will be called later to reset and assign
     * the reader
     */
    public ClojureLexer() {
        super();
    }

    private Token token(TokenType type) {
        return new Token(type, yychar, yylength());
    }

    private Token token(TokenType type, int pairValue) {
        return new Token(type, yychar, yylength(), (byte)pairValue);
    }

    private static final byte PARAN     = 1;
    private static final byte BRACKET   = 2;
    private static final byte CURLY     = 3;

%}

/* main character classes */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = {LineTerminator} | [ \t\f]+

/* comments */
Comment = {EndOfLineComment} 

EndOfLineComment = ";" {InputCharacter}* {LineTerminator}?

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

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

/* keywords */
Keyword = :[:jletter:]+

//String = \"([:jletter:]|[ \r\n])*\"
String = \"([^\"]|\\\")*\"

%state STRING, CHARLITERAL

%%

<YYINITIAL> {

  "(def " |
"(if " |
"(do " |
"(let " |
"(quote " |
"(var " |
"(fn " |
"(loop " |
"(recur " |
"(throw " |
"(try " |
"(monitor-enter " |
"(monitor-exit " |
"(sorted-map " |
"(read-line " |
"(re-pattern " |
"(keyword? " |
"(val " |
"(chunked-seq? " |
"(find-protocol-impl " |
"(vector-of " |
"(object-array " |
"(*compile-path* " |
"(max-key " |
"(list* " |
"(ns-aliases " |
"(booleans " |
"(the-ns " |
"(== " |
"(chunk-buffer " |
"(longs " |
"(special-form-anchor " |
"(shorts " |
"(instance? " |
"(syntax-symbol-anchor " |
"(format " |
"(sequential? " |
"(fn? " |
"(empty " |
"(bound-fn " |
"(dorun " |
"(time " |
"(remove-method " |
"(gensym " |
"(not= " |
"(*3 " |
"(unchecked-multiply " |
"(doseq " |
"(bit-or " |
"(aset-byte " |
"(if-not " |
"(hash-set " |
"(add-watch " |
"(unchecked-dec " |
"(some " |
"(nil? " |
"(string? " |
"(boolean-array " |
"(second " |
"(letfn " |
"(keys " |
"(for " |
"(*2 " |
"(long-array " |
"(pop-thread-bindings " |
"(error-mode " |
"(cond " |
"(bit-set " |
"(spit " |
"(find-protocol-method " |
"(fn " |
"(sorted? " |
"(short-array " |
"(ns-unalias " |
"(ns-publics " |
"(EMPTY-NODE " |
"(char-array " |
"(dosync " |
"(all-ns " |
"(long " |
"(with-open " |
"(init-proxy " |
"(add-classpath " |
"(false? " |
"(await1 " |
"(true? " |
"(gen-interface " |
"(sync " |
"(short " |
"(ns-unmap " |
"(repeat " |
"(zipmap " |
"(distinct " |
"(get-in " |
"(bit-xor " |
"(char-escape-string " |
"(complement " |
"(let " |
"(get-validator " |
"(dotimes " |
"(ref-max-history " |
"(print-namespace-doc " |
"(*ns* " |
"(promise " |
"(defmethod " |
"(pop! " |
"(derive " |
"(aset-float " |
"(extend " |
"(-reset-methods " |
"(lazy-cat " |
"(commute " |
"(defstruct " |
"(with-in-str " |
"(partition-by " |
"(rem " |
"(odd? " |
"(symbol? " |
"(*print-level* " |
"(*allow-unresolved-vars* " |
"(thread-bound? " |
"(proxy-call-with-super " |
"(ns-interns " |
"(re-matches " |
"(split-with " |
"(munge " |
"(find-doc " |
"(loop " |
"(future-done? " |
"(next " |
"(import " |
"(deliver " |
"(symbol " |
"(vals " |
"(print-doc " |
"(select-keys " |
"(re-matcher " |
"(rand " |
"(deref " |
"(unchecked-inc " |
"(*math-context* " |
"(read " |
"(sequence " |
"(make-hierarchy " |
"(+ " |
"(number? " |
"(assoc! " |
"(descendants " |
"(into-array " |
"(last " |
"(unchecked-negate " |
"(integer? " |
"(defrecord " |
"(*read-eval* " |
"(alter " |
"(prn " |
"(with-meta " |
"(with-out-str " |
"(floats " |
"(* " |
"(*compile-files* " |
"(when-not " |
"(butlast " |
"(- " |
"(->> " |
"(reversible? " |
"(rseq " |
"(send-off " |
"(seq? " |
"(refer-clojure " |
"(identical? " |
"(.. " |
"(print " |
"(vary-meta " |
"(with-loading-context " |
"(agent-error " |
"(*command-line-args* " |
"(bit-flip " |
"(zero? " |
"(bit-and " |
"(unquote-splicing " |
"(future " |
"(re-groups " |
"(*warn-on-reflection* " |
"(newline " |
"(replicate " |
"(keep-indexed " |
"(char? " |
"(distinct? " |
"(remove-ns " |
"(ratio? " |
"(xml-seq " |
"(vec " |
"(concat " |
"(update-in " |
"(vector " |
"(with-bindings* " |
"(conj " |
"(bases " |
"(/ " |
"(unchecked-add " |
"(ref-set " |
"(assoc " |
"(seque " |
"(aset-char " |
"(boolean " |
"(read-string " |
"(neg? " |
"(float-array " |
"(doubles " |
"(isa? " |
"(future-call " |
"(doto " |
"(extends? " |
"(remove-watch " |
"(print-str " |
"(*e " |
"(ref-history-count " |
"(rsubseq " |
"(*flush-on-newline* " |
"(*out* " |
"(future? " |
"(vector? " |
"(split-at " |
"(chunk-cons " |
"(ns-refers " |
"(create-struct " |
"(proxy-super " |
"(int-array " |
"(float " |
"(assert " |
"(map " |
"(counted? " |
"(memfn " |
"(double-array " |
"(accessor " |
"(*print-length* " |
"(frequencies " |
"(chars " |
"(class? " |
"(rand-int " |
"(*1 " |
"(aset-short " |
"(prn-str " |
"(iterate " |
"(chunk-append " |
"(when-first " |
"(slurp " |
"(restart-agent " |
"(mapcat " |
"(assoc-in " |
"(get-thread-bindings " |
"(special-symbol? " |
"(ref " |
"(conj! " |
"(find-var " |
"(inc " |
"(future-cancel " |
"(definline " |
"(bound-fn* " |
"(unchecked-subtract " |
"(ns-name " |
"(shuffle " |
"(defn- " |
"(*file* " |
"(re-find " |
"(bit-not " |
"(construct-proxy " |
"(ref-min-history " |
"(destructure " |
"(seq " |
"(intern " |
"(pvalues " |
"(to-array-2d " |
"(sorted-map-by " |
"(filter " |
"(*clojure-version* " |
"(var? " |
"(alter-meta! " |
"(comment " |
"(key " |
"(class " |
"(re-seq " |
"(-cache-protocol-fn " |
"(ns " |
"(empty? " |
"(test " |
"(print-dup " |
"(create-ns " |
"(name " |
"(list? " |
"(aset " |
"(nnext " |
"(doall " |
"(extenders " |
"(macroexpand-1 " |
"(not-any? " |
"(resultset-seq " |
"(reductions " |
"(into " |
"(with-precision " |
"(*use-context-classloader* " |
"(transient " |
"(ffirst " |
"(bit-clear " |
"(proxy-name " |
"(extend-type " |
"(load-reader " |
"(or " |
"(hash " |
"(print-ctor " |
"(associative? " |
"(float? " |
"(drop-last " |
"(replace " |
"(decimal? " |
"(defn " |
"(parents " |
"(map? " |
"(prefers " |
"(numerator " |
"(condp " |
"(quot " |
"(chunk-rest " |
"(file-seq " |
"(send " |
"(with-local-vars " |
"(reverse " |
"(with-bindings " |
"(count " |
"(get-proxy-class " |
"(set " |
"(when-let " |
"(comp " |
"(nth " |
"(byte " |
"(dissoc! " |
"(*err* " |
"(constantly " |
"(load " |
"(namespace " |
"(pr-str " |
"(< " |
"(rationalize " |
"(sort-by " |
"(cycle " |
"(peek " |
"(denominator " |
"(reduce " |
"(interleave " |
"(amap " |
"(-> " |
"(cons " |
"(macroexpand " |
"(var-set " |
"(str " |
"(aset-boolean " |
"(ns-imports " |
"(while " |
"(remove-all-methods " |
"(first " |
"(bean " |
"(= " |
"(memoize " |
"(var-get " |
"(range " |
"(tree-seq " |
"(defmacro " |
"(set-validator! " |
"(aset-double " |
"(case " |
"(enumeration-seq " |
"(prefer-method " |
"(partition-all " |
"(ensure " |
"(find-ns " |
"(not-every? " |
"(struct-map " |
"(> " |
"(max " |
"(proxy-mappings " |
"(identity " |
"(ints " |
"(fnext " |
"(min-key " |
"(reset-meta! " |
"(push-thread-bindings " |
"(subs " |
"(compile " |
"(agent-errors " |
"(clear-agent-errors " |
"(printf " |
"(ns-resolve " |
"(method-sig " |
"(>= " |
"(shutdown-agents " |
"(reset! " |
"(even? " |
"(require " |
"(bit-shift-left " |
"(methods " |
"(future-cancelled? " |
"(compare " |
"(deftype " |
"(sorted-set-by " |
"(cast " |
"(namespace-munge " |
"(supers " |
"(pcalls " |
"(load-string " |
"(group-by " |
"(get " |
"(<= " |
"(await " |
"(resolve " |
"(bytes " |
"(print-method " |
"(bound? " |
"(loaded-libs " |
"(fnil " |
"(force " |
"(partial " |
"(pmap " |
"(if-let " |
"(comparator " |
"(pos? " |
"(char " |
"(take-while " |
"(extend-protocol " |
"(and " |
"(refer " |
"(underive " |
"(in-ns " |
"(iterator-seq " |
"(declare " |
"(ancestors " |
"(hash-combine " |
"(persistent! " |
"(locking " |
"(partition " |
"(map-indexed " |
"(contains? " |
"(update-proxy " |
"(interpose " |
"(chunk " |
"(aset-int " |
"(ifn? " |
"(definterface " |
"(load-file " |
"(delay " |
"(apply " |
"(swap! " |
"(defmulti " |
"(proxy " |
"(reify " |
"(subvec " |
"(byte-array " |
"(rest " |
"(keyword " |
"(ns-map " |
"(set-error-mode! " |
"(unquote " |
"(int " |
"(release-pending-sends " |
"(mod " |
"(bigdec " |
"(nfirst " |
"(nthnext " |
"(*agent* " |
"(aset-long " |
"(struct " |
"(array-map " |
"(bigint " |
"(dec " |
"(println " |
"(aget " |
"(pr " |
"(drop " |
"(clojure-version " |
"(*print-dup* " |
"(gen-class " |
"(eval " |
"(unchecked-remainder " |
"(aclone " |
"(char-name-string " |
"(pop " |
"(primitives-classnames " |
"(atom " |
"(defonce " |
"(bit-shift-right " |
"(delay? " |
"(num " |
"(disj " |
"(io! " |
"(*print-readably* " |
"(rational? " |
"(merge-with " |
"(take-nth " |
"(*print-meta* " |
"(double " |
"(lazy-seq " |
"(*in* " |
"(take-last " |
"(line-seq " |
"(take " |
"(when " |
"(areduce " |
"(set? " |
"(make-array " |
"(rand-nth " |
"(alias " |
"(use " |
"(juxt " |
"(alength " |
"(chunk-first " |
"(*source-path* " |
"(defprotocol " |
"(to-array " |
"(hash-map " |
"(bit-and-not " |
"(compare-and-set! " |
"(*assert* " |
"(type " |
"(repeatedly " |
"(trampoline " |
"(set-error-handler! " |
"(remove " |
"(find " |
"(coll? " |
"(drop-while " |
"(not-empty " |
"(flatten " |
"(print-special-doc " |
"(println-str " |
"(list " |
"(chunk-next " |
"(every? " |
"(satisfies? " |
"(flush " |
"(sort " |
"(dissoc " |
"(not " |
"(binding " |
"(doc " |
"(error-handler " |
"(get-method " |
"(agent " |
"(sorted-set " |
"(alter-var-root " |
"(merge " |
"(subseq " |
"(min " |
"(print-simple " |
"(bit-test " |
"(await-for " |
"(keep " |
"(disj! " |
"(meta " |
"(unchecked-divide " { return new Token(TokenType.KEYWORD, yychar + 1, yylength() - 1); }

  /* keywords */
/*  "(fn "             |
  "(fn* "            |
  "(if "             |
  "(def "            |
  "(let "            |
  "(let* "           |
  "(loop* "          |
  "(new "            |
  "(nil "            |
  "(recur "          |
  "(loop "           |
  "(do "             |
  "(quote "          |
  "(the-var "        |
  "(identical? "     |
  "(throw "          |
  "(set! "           |
  "(monitor-enter "  |
  "(monitor-exit "   |
  "(try "            |
  "(catch "          |
  "(finally "        |
  "(in-ns "          { return new Token(TokenType.KEYWORD, yychar + 1, yylength() - 1); }
*/

/*  \((fn|if) { return token(TokenType.KEYWORD); } */


  /* Built-ins */
/*  "*agent*"                   |
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
  "hash"                      { return token(TokenType.KEYWORD2); }
*/


  /* operators */

  "("                            { return token(TokenType.OPERATOR,  PARAN); }
  ")"                            { return token(TokenType.OPERATOR, -PARAN); }
  "{"                            { return token(TokenType.OPERATOR,  CURLY); }
  "}"                            { return token(TokenType.OPERATOR, -CURLY); }
  "["                            { return token(TokenType.OPERATOR,  BRACKET); }
  "]"                            { return token(TokenType.OPERATOR, -BRACKET); }
  
  /* string literal */
/*  \"                             {  
                                    yybegin(STRING); 
                                    tokenStart = yychar; 
                                    tokenLength = 1; 
                                 }
*/

  {String}       		 { return token(TokenType.STRING); }

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
  {DoubleLiteral}[dD]            { return token(TokenType.NUMBER); }
  
  /* comments */
  {Comment}                      { return token(TokenType.COMMENT); }

  /* whitespace */
  {WhiteSpace}                   { }

  /* keyword */
  {Keyword}			 { return token(TokenType.TYPE); }

  /* identifiers */ 
  {Identifier}                   { return token(TokenType.IDENTIFIER); }
}


<STRING> {
  \"                             { 
                                     yybegin(YYINITIAL); 
                                     // length also includes the trailing quote
                                     return new Token(TokenType.STRING, tokenStart, tokenLength + 1);
                                 }
  
  {StringCharacter}+             { tokenLength += yylength(); }

  \\[0-3]?{OctDigit}?{OctDigit}  { tokenLength += yylength(); }
  
  /* escape sequences */

//  \\.                            { tokenLength += 2; }
//  {LineTerminator}               { yybegin(YYINITIAL);  }
}

<CHARLITERAL> {
  \'                             { 
                                     yybegin(YYINITIAL); 
                                     // length also includes the trailing quote
                                     return new Token(TokenType.STRING, tokenStart, tokenLength + 1);
                                 }
  
  {SingleCharacter}+             { tokenLength += yylength(); }
  
  /* escape sequences */

  \\.                            { tokenLength += 2; }
  {LineTerminator}               { yybegin(YYINITIAL);  }
}

/* error fallback */
.|\n                             {  }
<<EOF>>                          { return null; }

