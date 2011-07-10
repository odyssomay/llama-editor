
package llama.lexer;
import llama.lexer.Token;

%%

%public
%class ClojureLexer
%unicode
%line
%column
%char
%type Token

%{
public ClojureLexer() {}

private Token token(String type) {
	return new Token(type, yyline, yycolumn, yychar, yylength());
}

private Token token(String type, Integer line, Integer column, Integer offset, Integer length) {
	return new Token(type, line, column, offset, length);
}
%}

LineTerminator = (\r|\n|\r\n)
InputCharacter = [^\r\n]

StandardComment = ;{InputCharacter}*{LineTerminator}?
FormComment = \(comment [^\)]*\)

Number = -?[0-9\\\.]M?
ArbitraryBaseNumber = [0-6]?[0-9]{1}r[0-9a-zA-Z]
OctalNumber = 0x[0-9a-fA-F]+

String = \"([^\"]|\\\")*\"

%%

<YYINITIAL> {
	
==INSERT_KEYWORDS_HERE== { return token("KEYWORD", yyline, yycolumn + 1, yychar + 1, yylength() - 1); }
{StandardComment}|{FormComment} { return token("COMMENT"); }
{Number}|{ArbitraryBaseNumber}|{OctalNumber} { return token("NUMBER"); }
{String} { return token("STRING"); }

}

.|\n {}
