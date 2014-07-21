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
// A lexical token from an input string.

package org.bladecoder.engine.expr;

class Token {
    public static final int TT_ERROR  = -1;
    public static final int TT_EOF    = -2;
    public static final int TT_NUMBER = -3;
    public static final int TT_WORD   = -4;
    public static final int TT_LE     = -5;
    public static final int TT_NE     = -6;
    public static final int TT_GE     = -7;
    public static final int TT_STRING = -8;

    public Token(int ttype, double nval, String input, int start, int end) {
        this.ttype = ttype;
        this.sval = input.substring(start, end);
	this.nval = nval;
	this.location = start;
	
	int count = 0;
	for (int i = start-1; 0 <= i; --i) {
	    if (!Character.isWhitespace(input.charAt(i)))
		break;
	    ++count;
	}
	this.leadingWhitespace = count;

	count = 0;
	for (int i = end; i < input.length(); ++i) {
	    if (!Character.isWhitespace(input.charAt(i)))
		break;
	    ++count;
	}
	this.trailingWhitespace = count;
    }

    Token(int ttype, double nval, String sval, Token token) {
	this.ttype = ttype;
	this.sval = sval;
	this.nval = nval;
	this.location = token.location;
	this.leadingWhitespace = token.leadingWhitespace;
	this.trailingWhitespace = token.trailingWhitespace;
    }

    public final int ttype;
    public final String sval;
    public final double nval;

    public final int location;

    public final int leadingWhitespace, trailingWhitespace;
}
