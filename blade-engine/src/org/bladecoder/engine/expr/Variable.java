// Variables associate values with names.
// Copyright 1996 by Darius Bacon; see the file COPYING.

package org.bladecoder.engine.expr;

import java.util.HashMap;

/**
 * A variable is a simple expression with a name (like "x") and a
 * settable value.
 */
public class Variable extends Expr {
    private static HashMap<String,Variable> variables = new HashMap<String,Variable>();
    
    /**
     * Return a unique variable named `name'.  There can be only one
     * variable with the same name returned by this method; that is,
     * make(s1) == make(s2) if and only if s1.equals(s2).
     * @param name the variable's name
     * @return the variable; create it initialized to 0 if it doesn't
     *         yet exist */
    static public synchronized Variable make(String name) {
	Variable result =  variables.get(name);
	if (result == null)
	    variables.put(name, result = new Variable(name));
	return result;
    }

    private String name;
    private double val;

    /**
     * Create a new variable, with initial value 0.
     * @param name the variable's name
     */
    public Variable(String name) { 
	this.name = name; val = 0; 
    }

    /** Return the name. */
    public String toString() { return name; }

    /** Get the value.
     * @return the current value */
    public double value() { 
	return val; 
    }
    /** Set the value.
     * @param value the new value */
    public void setValue(double value) { 
	val = value; 
    }

	@Override
	public String getStringLiteral() {
		// TODO Auto-generated method stub
		return null;
	}
}
