package org.bladecoder.engine.expr;

/**
 * Test for bugs in the whole package.
 */
public class RegressionTest {

	public static void main(String[] args) {
		Variable.make("pi").setValue(Math.PI);

		expect(9, "3^2");
		expect(256, "2^2^3");
		expect(6, "3*2");
		expect(1.5, "3/2");
		expect(5, "3+2");
		expect(1, "3-2");
		expect(-3, "-3");
		expect(1, "2<3");
		expect(0, "2<2");
		expect(0, "3<2");
		expect(1, "2<=3");
		expect(1, "2<=2");
		expect(0, "3<=2");
		expect(0, "2=3");
		expect(1, "2=2");
		expect(1, "2<>3");
		expect(0, "2<>2");
		expect(0, "2>=3");
		expect(1, "2>=2");
		expect(1, "3>=2");
		expect(0, "2>3");
		expect(0, "2>2");
		expect(1, "3>2");
		expect(1, "(1 and 1)");
		expect(0, "(1 and 0)");
		expect(0, "(0 and 1)");
		expect(0, "(0 and 0)");
		expect(1, "(1 or 1)");
		expect(1, "(1 or 0)");
		expect(1, "(0 or 1)");
		expect(0, "(0 or 0)");
		expect(2, "abs(-2)");
		expect(2, "abs(2)");
		expect(0, "acos(1)");
		expect(Math.PI / 2, "asin(1)");
		expect(Math.PI / 4, "atan(1)");
		expect(-3 * Math.PI / 4, "atan2(-1, -1)");
		expect(4, "ceil(3.5)");
		expect(-3, "ceil(-3.5)");
		expect(1, "cos(0)");
		expect(Math.exp(1), "exp(1)");
		expect(3, "floor(3.5)");
		expect(-4, "floor(-3.5)");
		expect(1, "log(2.7182818284590451)");
		expect(4, "round(3.5)");
		expect(-4, "round(-3.5)");
		expect(1, "sin(pi/2)");
		expect(3, "sqrt(9)");
		expect(0.99999999999999989, "tan(pi/4)");
		expect(3, "max(2, 3)");
		expect(2, "min(2, 3)");
		expect(137, "if(0, 42, 137)");
		expect(42, "if(1, 42, 137)");
		expect(1, "'abc' = 'abc'");
		expect(0, "'ac' = 'abc'");
		expect(20, "if('a1134c' = 'abc',10,20)");

		expect(-3.0 * Math.pow(1.01, 100.1), "  -3 * 1.01^100.1  ");

		Variable x = Variable.make("x");
		x.setValue(-40.0);
		expect(-171.375208, "-0.00504238 * x^2 + 2.34528 * x - 69.4962");

		{
			boolean caught = false;
			Parser p = new Parser();
			p.allow(x); // or p.allow(null);
			try {
				p.parseString("whoo");
			} catch (SyntaxException se) {
				caught = true;
			}
			if (!caught)
				throw new Error("Test failed: unknown variable allowed");
		}

		x.setValue(1.1);

		expect(137, "137");
		expect(Math.PI, "pi");
		expect(1.1, "x");
		expect(3.8013239000000003, "3.14159 * x^2");
		expect(-1.457526100326025, "sin(10*x) + sin(9*x)");
		expect(0.8907649332805846, "sin(x) + sin(100*x)/100");
		expect(-0.16000473871962462, "sin(0.1*x) * (sin(9*x) + sin(10*x))");
		expect(0.29819727942988733, "exp(-x^2)");
		expect(0.43226861565393254, "2^(-x^2)");
		expect(0.7075295010833899, "(x^3)^(-x^2)");
		expect(0.8678400091286832, "x*sin(1/x)");
		expect(-5.89, "x^2-x-6");
		expect(3.1953090617340916, "sqrt(3^2 + x^2)");
		expect(1.3542460218188073, "atan(5/x)");
		expect(1.5761904761904764, "(x^2 + x + 1)/(x + 1)");
		expect(2.6451713395638627, "(x^3 - (4*x^2) + 12)/(x^2 + 2)");
		expect(-2.2199999999999998, "-2*(x-3)^2+5");
		expect(1.2000000000000002, "2*abs(x+1)-3");
		expect(2.7910571473905725, "sqrt(9-x^2)");

		System.out.println("All tests passed.");
	}

	private static void expect(double expected, String input) {
		Expr expr;
		try {
			expr = Parser.parse(input);
		} catch (SyntaxException e) {
			throw new Error(e.explain());
		}

		double result = expr.value();
		if (result != expected) {
			throw new Error("Bad result: " + result
					+ " instead of the expected " + expected + " in \"" + input
					+ "\"");
		}
	}

}
