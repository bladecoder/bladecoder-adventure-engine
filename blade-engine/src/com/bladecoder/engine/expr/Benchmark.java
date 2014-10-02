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
// Run benchmarks from the command line.

package com.bladecoder.engine.expr;

/**
 * Time evaluating many expressions over many values.
 */
public class Benchmark {
    public static void main(String[] args) {
        double parse_product = 1.0;
        double run_product = 1.0;
        for (int i = 0; i < args.length; ++i) {
            long parsetime = timeParse(args[i]);
            long runtime = timeRun(args[i]);
            System.out.println("" + msec(parsetime) + " ms(parse) "
                               + msec(runtime) + " ms(run): "
                               + args[i]);
            parse_product *= parsetime;
            run_product *= runtime;
        }
        if (0 < args.length) {
            double run_geomean = Math.pow(run_product, 1.0 / args.length);
            double parse_geomean = Math.pow(parse_product, 1.0 / args.length);
            System.out.println("" + msec(parse_geomean) + " ms(parse) "
                               + msec(run_geomean) + " ms(run): (geometric mean)");
        }
    }

    static long msec(double nsec) {
        return (long) Math.rint(nsec * 1e-6);
    }

    static final int nruns = 1000000;

    static long timeRun(String expression) {
	Variable x = Variable.make("x");
        Expr expr = parse(expression);

	double low  = 0.0;
	double high = 4.0;
	double step = (high - low) / nruns;

        long start = System.nanoTime();
	for (double xval = low; xval <= high; xval += step) {
	    x.setValue(xval);
	    expr.value();
	}
        return System.nanoTime() - start;
    }

    static final int nparses = 1000;

    static long timeParse(String expression) {
        long start = System.nanoTime();
        for (int i = 0; i < nparses; ++i)
            parse(expression); 
        return System.nanoTime() - start;
    }

    static Expr parse(String expression) {
        try {
            return Parser.parse(expression); 
        } catch (SyntaxException e) {
            System.err.println(e.explain());
            throw new Error(e);
        }
    }
}
