/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
// Scan lexical tokens in input strings.

package org.bladecoder.engine.expr;

import java.util.ArrayList;

class Scanner {

	private String s;
	private String operatorChars;

	ArrayList<Token> tokens = new ArrayList<Token>();
	int index = -1;

	public Scanner(String string, String operatorChars) {
		this.s = string;
		this.operatorChars = operatorChars;

		int i = 0;
		do {
			i = scanToken(i);
		} while (i < s.length());
	}

	public String getInput() {
		return s;
	}

	// The tokens may have been diddled, so this can be different from
	// getInput().
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int whitespace = 0;
		for (int i = 0; i < tokens.size(); ++i) {
			Token t = tokens.get(i);

			int spaces = (whitespace != 0 ? whitespace : t.leadingWhitespace);
			if (i == 0)
				spaces = 0;
			else if (spaces == 0
					&& !joinable(tokens.get(i - 1), t))
				spaces = 1;
			for (int j = spaces; 0 < j; --j)
				sb.append(" ");

			sb.append(t.sval);
			whitespace = t.trailingWhitespace;
		}
		return sb.toString();
	}

	private boolean joinable(Token s, Token t) {
		return !(isAlphanumeric(s) && isAlphanumeric(t));
	}

	private boolean isAlphanumeric(Token t) {
		return t.ttype == Token.TT_WORD || t.ttype == Token.TT_NUMBER;
	}

	public boolean isEmpty() {
		return tokens.size() == 0;
	}

	public boolean atStart() {
		return index <= 0;
	}

	public boolean atEnd() {
		return tokens.size() <= index;
	}

	public Token nextToken() {
		++index;
		return getCurrentToken();
	}

	public Token getCurrentToken() {
		if (atEnd())
			return new Token(Token.TT_EOF, 0, s, s.length(), s.length());
		return  tokens.get(index);
	}

	private int scanToken(int i) {
		while (i < s.length() && Character.isWhitespace(s.charAt(i)))
			++i;

		if (i == s.length()) {
			return i;
		} else if (0 <= operatorChars.indexOf(s.charAt(i))) {
			if (i + 1 < s.length()) {
				String pair = s.substring(i, i + 2);
				int ttype = 0;
				if (pair.equals("<="))
					ttype = Token.TT_LE;
				else if (pair.equals(">="))
					ttype = Token.TT_GE;
				else if (pair.equals("<>"))
					ttype = Token.TT_NE;
				if (0 != ttype) {
					tokens.add(new Token(ttype, 0, s, i, i + 2));
					return i + 2;
				}
			}
			tokens.add(new Token(s.charAt(i), 0, s, i, i + 1));
			return i + 1;
		} else if (Character.isLetter(s.charAt(i))) {
			return scanSymbol(i);
		} else if (s.charAt(i) == '\'') {
			return scanString(i);			
		} else if (Character.isDigit(s.charAt(i)) || '.' == s.charAt(i)) {
			return scanNumber(i);
		} else {
			tokens.add(makeErrorToken(i, i + 1));
			return i + 1;
		}
	}

	private int scanSymbol(int i) {
		int from = i;
		while (i < s.length()
				&& (Character.isLetter(s.charAt(i)) || Character.isDigit(s
						.charAt(i))))
			++i;
		tokens.add(new Token(Token.TT_WORD, 0, s, from, i));
		return i;
	}

	private int scanString(int i) {
		int from = i;
		
		i++;
		
		while (i < s.length() && (s.charAt(i) != '\''))
			++i;
		
		if(i==s.length()) {
			tokens.add(makeErrorToken(from, i));
			return i;
		}
		
		
		tokens.add(new Token(Token.TT_STRING, 0, s, from + 1, i));
		
		return i + 1;
	}

	private int scanNumber(int i) {
		int from = i;

		// We include letters in our purview because otherwise we'd
		// accept a word following with no intervening space.
		for (; i < s.length(); ++i)
			if ('.' != s.charAt(i) && !Character.isDigit(s.charAt(i))
					&& !Character.isLetter(s.charAt(i)))
				break;

		String text = s.substring(from, i);
		double nval;
		try {
			nval = Double.valueOf(text).doubleValue();
		} catch (NumberFormatException nfe) {
			tokens.add(makeErrorToken(from, i));
			return i;
		}

		tokens.add(new Token(Token.TT_NUMBER, nval, s, from, i));
		return i;
	}

	private Token makeErrorToken(int from, int i) {
		return new Token(Token.TT_ERROR, 0, s, from, i);
	}
}
