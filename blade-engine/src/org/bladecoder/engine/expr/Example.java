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
// Put the expression evaluator through its paces.

// Sample usage:

// $ java expr.Example '3.14159 * x^2' 0 4 1
// 0
// 3.14159
// 12.5664
// 28.2743
// 50.2654
//
// $ java expr.Example 'sin (pi/4 * x)' 0 4 1
// 0
// 0.707107
// 1
// 0.707107
// 1.22461e-16
//
// $ java expr.Example 'sin (pi/4 x)' 0 4 1
// I don't understand your formula "sin (pi/4 x)".
// 
// I got as far as "sin (pi/4" and then saw "x".
// I expected ")" at that point, instead.
// An example of a formula I can parse is "sin (pi/4 + x)".

package org.bladecoder.engine.expr;

/**
 * A simple example of parsing and evaluating an expression.
 */
public class Example {
    public static void main(String[] args) {

	Expr expr;
	try {
	    expr = Parser.parse(args[0]); 
	} catch (SyntaxException e) {
	    System.err.println(e.explain());
	    return;
	}

	double low  = Double.valueOf(args[1]).doubleValue();
	double high = Double.valueOf(args[2]).doubleValue();
	double step = Double.valueOf(args[3]).doubleValue();

	Variable x = Variable.make("x");
	for (double xval = low; xval <= high; xval += step) {
	    x.setValue(xval);
	    System.out.println(expr.value());
	}
    }
}
