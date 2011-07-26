
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
ANumber = {Separator}({Number}|{Ratio}|{ArbitraryBaseNumber}|{OctalNumber}){Separator}

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

==INSERT_KEYWORDS_HERE== { 
Token[] ret = { 
	singleToken("SEPARATOR", yyline, yycolumn, yychar, 1) ,
	singleToken("CORE-SYMBOL", yyline, yycolumn + 1, yychar + 1, yylength() - 1)
};
return ret;
}

{StandardComment} { return token("COMMENT"); }

{ANumber}
{ 
Token[] ret = {
	singleToken("SEPARATOR", yyline, yycolumn, yychar, 1) ,
	singleToken("NUMBER", yyline, yycolumn + 1, yychar + 1, yylength() - 1)
};
yypushback(1);
return ret;
}

return token("NUMBER"); }

[ \(\[\{]:{1,2}[^\r\n \(\)\[\]\{\}:]{Identifier}+ 
{ 
Token[] ret = {
	singleToken("SEPARATOR", yyline, yycolumn, yychar, 1) ,
	singleToken("KEYWORD", yyline, yycolumn + 1, yychar + 1, yylength())
};
return ret;
}

{String} { return token("STRING"); }

{NewClass} { 
yypushback(1);
return token("NEW-CLASS"); 
}

{Separator} { return token("SEPARATOR"); }

}

.|\n { return token("DEFAULT"); }
