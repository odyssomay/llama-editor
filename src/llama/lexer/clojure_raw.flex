
package llama.lexer;
import llama.lexer.Token;

%%

%public
%class ClojureLexer
%unicode
%line
%column
%type Token

%{
public ClojureLexer() {}

private Token token(String type) {
	return new Token(type, yyline, yycolumn, yylength());
}
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

StandardComment = ;{InputCharacter}*{LineTerminator}
FormComment = \(comment [^\)]*\)

%%

<YYINITIAL> {
	
{StandardComment}|{FormComment} { return token("comment"); }

}

. {}
