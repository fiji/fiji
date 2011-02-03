/*
 * Generated on 4/20/10 2:34 PM
 */
package fiji.scripting;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * 
 */
%%

%public
%class ImageJMacroTokenMaker
%extends AbstractJFlexCTokenMaker
%unicode
/* Case sensitive */
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public ImageJMacroTokenMaker() {
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
	}


	/**
	 * Returns the text to place at the beginning and end of a
	 * line to "comment" it in a this programming language.
	 *
	 * @return The start and end strings to add to a line to "comment"
	 *         it out.
	 */
	public String[] getLineCommentStartAndEnd() {
		return null;
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
						case Token.COMMENT_MULTILINE:
				state = MLC;
				start = text.offset;
				break;

			/* No documentation comments */
			default:
				state = Token.NULL;
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new DefaultToken();
		}

	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(java.io.Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

Letter							= [A-Za-z]
LetterOrUnderscore				= ({Letter}|"_")
NonzeroDigit						= [1-9]
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
AnyCharacterButApostropheOrBackSlash	= ([^\\'])
AnyCharacterButDoubleQuoteOrBackSlash	= ([^\\\"\n])
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
Escape							= ("\\"(([btnfr\"'\\])|([0123]{OctalDigit}?{OctalDigit}?)|({OctalDigit}{OctalDigit}?)|{EscapedSourceCharacter}))
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#"|"\\")
IdentifierStart					= ({LetterOrUnderscore}|"$")
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))

LineTerminator				= (\n)
WhiteSpace				= ([ \t\f]+)

CharLiteral	= ([\']({AnyCharacterButApostropheOrBackSlash}|{Escape})[\'])
UnclosedCharLiteral			= ([\'][^\'\n]*)
ErrorCharLiteral			= ({UnclosedCharLiteral}[\'])
StringLiteral				= ([\"]({AnyCharacterButDoubleQuoteOrBackSlash}|{Escape})*[\"])
UnclosedStringLiteral		= ([\"]([\\].|[^\\\"])*[^\"]?)
ErrorStringLiteral			= ({UnclosedStringLiteral}[\"])

MLCBegin					= "/*"
MLCEnd					= "*/"

/* No documentation comments */
LineCommentBegin			= "//"

IntegerHelper1				= (({NonzeroDigit}{Digit}*)|"0")
IntegerHelper2				= ("0"(([xX]{HexDigit}+)|({OctalDigit}*)))
IntegerLiteral				= ({IntegerHelper1}[lL]?)
HexLiteral				= ({IntegerHelper2}[lL]?)
FloatHelper1				= ([fFdD]?)
FloatHelper2				= ([eE][+-]?{Digit}+{FloatHelper1})
FloatLiteral1				= ({Digit}+"."({FloatHelper1}|{FloatHelper2}|{Digit}+({FloatHelper1}|{FloatHelper2})))
FloatLiteral2				= ("."{Digit}+({FloatHelper1}|{FloatHelper2}))
FloatLiteral3				= ({Digit}+{FloatHelper2})
FloatLiteral				= ({FloatLiteral1}|{FloatLiteral2}|{FloatLiteral3}|({Digit}+[fFdD]))
ErrorNumberFormat			= (({IntegerLiteral}|{HexLiteral}|{FloatLiteral}){NonSeparator}+)
BooleanLiteral				= ("true"|"false")

Separator					= ([\(\)\{\}\[\]])
Separator2				= ([\;,.])

Identifier				= ({IdentifierStart}{IdentifierPart}*)
ErrorIdentifier			= ({NonSeparator}+)

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


/* No string state */
/* No char state */
%state MLC
/* No documentation comment state */
%state EOL_COMMENT

%%

<YYINITIAL> {

	/* Keywords */
	"NaN" |
"PI" |
"do" |
"else" |
"false" |
"for" |
"function" |
"if" |
"macro" |
"return" |
"true" |
"var" |
"while"		{ addToken(Token.RESERVED_WORD); }

	/* Data types */
	/* No data types */

	/* Functions */
	"Array.copy" |
"Array.fill" |
"Array.getStatistics" |
"Array.invert" |
"Array.sort" |
"Array.trim" |
"Dialog.addCheckbox" |
"Dialog.addCheckboxGroup" |
"Dialog.addChoice" |
"Dialog.addMessage" |
"Dialog.addNumber" |
"Dialog.addString" |
"Dialog.getCheckbox" |
"Dialog.getChoice" |
"Dialog.getNumber" |
"Dialog.getString" |
"Dialog.show" |
"Ext" |
"File.append" |
"File.close" |
"File.dateLastModified" |
"File.delete" |
"File.directory" |
"File.exists" |
"File.getName" |
"File.getParent" |
"File.isDirectory" |
"File.lastModified" |
"File.length" |
"File.makeDirectory" |
"File.name" |
"File.nameWithoutExtension" |
"File.open" |
"File.openAsRawString" |
"File.openAsString" |
"File.openDialog" |
"File.openUrlAsString" |
"File.rename" |
"File.saveString" |
"File.separator" |
"Fit.doFit" |
"Fit.f" |
"Fit.getEquation" |
"Fit.logResults" |
"Fit.nEquations" |
"Fit.nParams" |
"Fit.p" |
"Fit.plot" |
"Fit.rSquared" |
"Fit.showDialog" |
"IJ.currentMemory" |
"IJ.deleteRows" |
"IJ.freeMemory" |
"IJ.getToolName" |
"IJ.maxMemory" |
"IJ.redirectErrorMessages" |
"List.clear" |
"List.get" |
"List.getList" |
"List.getValue" |
"List.set" |
"List.setCommands" |
"List.setList" |
"List.setMeasurements" |
"List.size" |
"Overlay" |
"Plot" |
"Stack.getActiveChannels" |
"Stack.getDimensions" |
"Stack.getDisplayMode" |
"Stack.getFrameRate" |
"Stack.getPosition" |
"Stack.getStatistics" |
"Stack.isHyperstack" |
"Stack.setActiveChannels" |
"Stack.setChannel" |
"Stack.setDimensions" |
"Stack.setDisplayMode" |
"Stack.setFrame" |
"Stack.setFrameRate" |
"Stack.setPosition" |
"Stack.setSlice" |
"Stack.setTUnit" |
"Stack.setZUnit" |
"Stack.swap" |
"String.append" |
"String.buffer" |
"String.copy" |
"String.copyResults" |
"String.paste" |
"String.resetBuffer" |
"abs" |
"acos" |
"asin" |
"atan" |
"atan2" |
"autoUpdate" |
"beep" |
"bitDepth" |
"calibrate" |
"call" |
"changeValues" |
"charCodeAt" |
"close" |
"cos" |
"d2s" |
"debug" |
"doCommand" |
"doWand" |
"drawLine" |
"drawOval" |
"drawRect" |
"drawString" |
"dump" |
"endsWith" |
"eval" |
"exec" |
"exit" |
"exp" |
"fill" |
"fillOval" |
"fillRect" |
"floodFill" |
"floor" |
"fromCharCode" |
"getArgument" |
"getBoolean" |
"getBoundingRect" |
"getCursorLoc" |
"getDateAndTime" |
"getDimensions" |
"getDirectory" |
"getFileList" |
"getFontList" |
"getHeight" |
"getHistogram" |
"getImageID" |
"getImageInfo" |
"getInfo" |
"getLine" |
"getList" |
"getLocationAndSize" |
"getLut" |
"getMetadata" |
"getMinAndMax" |
"getNumber" |
"getPixel" |
"getPixelSize" |
"getProfile" |
"getRawStatistics" |
"getResult" |
"getResultLabel" |
"getResultsCount" |
"getSelectionBounds" |
"getSelectionCoordinates" |
"getSliceNumber" |
"getStatistics" |
"getString" |
"getStringWidth" |
"getThreshold" |
"getTime" |
"getTitle" |
"getValue" |
"getVersion" |
"getVoxelSize" |
"getWidth" |
"getZoom" |
"imageCalculator" |
"indexOf" |
"invert" |
"is" |
"isActive" |
"isKeyDown" |
"isNaN" |
"isOpen" |
"lastIndexOf" |
"lengthOf" |
"lineTo" |
"log" |
"makeLine" |
"makeOval" |
"makePoint" |
"makePolygon" |
"makeRectangle" |
"makeSelection" |
"makeText" |
"matches" |
"maxOf" |
"minOf" |
"moveTo" |
"nImages" |
"nResults" |
"nSlices" |
"newArray" |
"newImage" |
"newMenu" |
"open" |
"parseFloat" |
"parseInt" |
"pow" |
"print" |
"putPixel" |
"random" |
"rename" |
"replace" |
"requires" |
"reset" |
"resetMinAndMax" |
"resetThreshold" |
"restorePreviousTool" |
"restoreSettings" |
"roiManager" |
"round" |
"run" |
"runMacro" |
"save" |
"saveAs" |
"saveSettings" |
"screenHeight" |
"screenWidth" |
"selectImage" |
"selectWindow" |
"selectionName" |
"selectionType" |
"setAutoThreshold" |
"setBackgroundColor" |
"setBatchMode" |
"setColor" |
"setFont" |
"setForegroundColor" |
"setJustification" |
"setKeyDown" |
"setLineWidth" |
"setLocation" |
"setLut" |
"setMetadata" |
"setMinAndMax" |
"setOption" |
"setPasteMode" |
"setPixel" |
"setRGBWeights" |
"setResult" |
"setSelectionLocation" |
"setSelectionName" |
"setSlice" |
"setThreshold" |
"setTool" |
"setVoxelSize" |
"setZCoordinate" |
"setupUndo" |
"showMessage" |
"showMessageWithCancel" |
"showProgress" |
"showStatus" |
"showText" |
"sin" |
"snapshot" |
"split" |
"sqrt" |
"startsWith" |
"substring" |
"tan" |
"toBinary" |
"toHex" |
"toLowerCase" |
"toString" |
"toUpperCase" |
"toolID" |
"updateDisplay" |
"updateResults" |
"wait" |
"waitForUser" |
"write"		{ addToken(Token.FUNCTION); }

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{Identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	{CharLiteral}				{ addToken(Token.LITERAL_CHAR); }
{UnclosedCharLiteral}		{ addToken(Token.ERROR_CHAR); addNullToken(); return firstToken; }
{ErrorCharLiteral}			{ addToken(Token.ERROR_CHAR); }
	{StringLiteral}				{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
{UnclosedStringLiteral}		{ addToken(Token.ERROR_STRING_DOUBLE); addNullToken(); return firstToken; }
{ErrorStringLiteral}			{ addToken(Token.ERROR_STRING_DOUBLE); }

	/* Comment literals. */
	{MLCBegin}	{ start = zzMarkedPos-2; yybegin(MLC); }
	/* No documentation comments */
	{LineCommentBegin}			{ start = zzMarkedPos-2; yybegin(EOL_COMMENT); }

	/* Separators. */
	{Separator}					{ addToken(Token.SEPARATOR); }
	{Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Operators. */
	"!" |
"%" |
"%=" |
"&" |
"&&" |
"*" |
"*=" |
"+" |
"++" |
"+=" |
"," |
"-" |
"--" |
"-=" |
"/" |
"/=" |
":" |
"<" |
"<<" |
"<<=" |
"=" |
"==" |
">" |
">>" |
">>=" |
"?" |
"^" |
"|" |
"||" |
"~"		{ addToken(Token.OPERATOR); }

	/* Numbers */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexLiteral}					{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{FloatLiteral}					{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}				{ addToken(Token.ERROR_NUMBER_FORMAT); }

	{ErrorIdentifier}				{ addToken(Token.ERROR_IDENTIFIER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters. */
	.							{ addToken(Token.IDENTIFIER); }

}


/* No char state */

/* No string state */

<MLC> {

	[^hwf\n*]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_MULTILINE); start = zzMarkedPos; }
	[hwf]					{}

	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }
	{MLCEnd}					{ yybegin(YYINITIAL); addToken(start,zzStartRead+2-1, Token.COMMENT_MULTILINE); }
	"*"						{}
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }

}


/* No documentation comment state */

<EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}

