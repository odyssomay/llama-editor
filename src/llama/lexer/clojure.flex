
package llama.lexer;
import llama.lexer.Token;

%%

%public
%final
%class ClojureLexer
%unicode
%line
%column
%char
%type Token[]

%{
/*
int parensCount;
int line;
int column;
int offset;
int length;
*/

public ClojureLexer() {}

private Token singleToken(String type) {
	return new Token(type, yyline, yycolumn, yychar, yylength());
}

private Token singleToken(String type, Integer line, Integer column, Integer offset, Integer length) {
	return new Token(type, line, column, offset, length);
}

private Token[] token(String type) {
	Token[] ret = { new Token(type, yyline, yycolumn, yychar, yylength())};
	return ret;
}

private Token[] token(String type, Integer line, Integer column, Integer offset, Integer length) {
	Token[] ret = { new Token(type, line, column, offset, length) };
	return ret;
}

private int currentParensCount(Integer start, Integer end) {
int pCount = 0;

for (int i = start; i < end; i++) {
	if (yycharat(i) == '(') {
		pCount += 1;
	} else if (yycharat(i) == ')') {
		pCount -= 1;
	}
}

return pCount;

}
%}

LineTerminator = (\r|\n|\r\n)
InputCharacter = [^\r\n]
Identifier = [^\r\n \(\)\[\]\{\}]
Separator = [\r\n \(\)\[\]\{\}]

StandardComment = ;{InputCharacter}*{LineTerminator}?
FormComment = \(comment [^\)]*\)

Number = -?[0-9\.]+M?
Ratio = -?[0-9]+\/[0-9]+
ArbitraryBaseNumber = -?[0-3]?[0-9]{1}r[0-9a-zA-Z]+
OctalNumber = 0x[0-9a-fA-F]+

String = \"([^\"]|\\\")*\"

NewClass = {Identifier}+\.{Separator}

%state COMMENT

%%

<YYINITIAL> {

/*	
"(comment " {
parensCount = 1;
line = yyline;
column = yycolumn;
offset = yychar;
}
*/	

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
"(unchecked-divide " { 
Token[] ret = { 
	singleToken("SEPARATOR", yyline, yycolumn, yychar, 1) ,
	singleToken("CORE-SYMBOL", yyline, yycolumn + 1, yychar + 1, yylength() - 1)
};
return ret;
}

{StandardComment} { return token("COMMENT"); }

/*
{FormComment} 
{ 
for (int i = 0; i < yylength(); i++) {
	if (currentParensCount(0, i) == 0) {
//		yypushback(yylength() - i);	
//		return token("COMMENT", yyline, yycolumn, yychar, i + 1);
	}
//yypushback(yylength() - 1); // fallback
return token("COMMENT");

}
}
*/
	

{Number} |
{Ratio} |
{ArbitraryBaseNumber} |
{OctalNumber} { return token("NUMBER"); }

[ \(\[\{]:{1,2}[^\r\n \(\)\[\]\{\}:]{Identifier}+ 
{ 
Token[] ret = {
	singleToken("SEPARATOR", yyline, yycolumn, yychar, 1) ,
	singleToken("KEYWORD", yyline, yycolumn + 1, yychar + 1, yylength())
};
return ret;
}

{String} { return token("STRING"); }

{NewClass} { return token("NEW-CLASS"); }

{Separator} { return token("SEPARATOR"); }

}

/*
<COMMENT> {

"(" { 
length += 1;
parensCount += 1; 
}

")" {
length += 1; 
parensCount -= 1;
if (parensCount == 0) {
	return token("COMMENT", line, column, offset length);
}
}

. { length += 1; }

}
*/

.|\n { return token("DEFAULT"); }
