<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 2) Logic

Obviously, our global variables are not intended to be constants, so we need a syntax for altering them.

Since by default, any text in an **ink** script is printed out directly to the screen, we use a markup symbol to indicate that a line of content is intended meant to be doing some numerical work, we use the `~` mark.

The following statements all assign values to variables:


    === set_some_variables ===
        ~ knows_about_wager = true
        ~ x = (x * x) - (y * y) + c
        ~ y = 2 * x * y

and the following will test conditions:

    { x == 1.2 }
    { x / 2 > 4 }
    { y - 1 <= x * x }

### Mathematics

**ink** supports the four basic mathematical operations (`+`, `-`, `*` and `/`), as well as `%` (or `mod`), which returns the remainder after integer division. There's also POW for to-the-power-of:

    {POW(3, 2)} is 9.
    {POW(16, 0.5)} is 4.


If more complex operations are required, one can write functions (using recursion if necessary), or call out to external, game-code functions (for anything more advanced).


#### RANDOM(min, max)

Ink can generate random integers if required using the RANDOM function. RANDOM is authored to be like a dice (yes, pendants, we said *a dice*), so the min and max values are both inclusive.

    ~ temp dice_roll = RANDOM(1, 6)

    ~ temp lazy_grading_for_test_paper = RANDOM(30, 75)

    ~ temp number_of_heads_the_serpent_has = RANDOM(3, 8)

The random number generator can be seeded for testing purposes, see the section of Game Queries and Functions section above.

#### Advanced: numerical types are implicit

Results of operations - in particular, for division - are typed based on the type of the input. So integer division returns integer, but floating point division returns floating point results.

    ~ x = 2 / 3
    ~ y = 7 / 3
    ~ z = 1.2 / 0.5

assigns `x` to be 0, `y` to be 2 and `z` to be 2.4.

#### Advanced: INT(), FLOOR() and FLOAT()

In cases where you don't want implicit types, or you want to round off a variable, you can cast it directly.

    {INT(3.2)} is 3.
    {FLOOR(4.8)} is 4.
    {INT(-4.8)} is -4.
    {FLOOR(-4.8)} is -5.

    {FLOAT(4)} is, um, still 4.



### String queries

Oddly for a text-engine, **ink** doesn't have much in the way of string-handling: it's assumed that any string conversion you need to do will be handled by the game code (and perhaps by external functions.) But we support three basic queries - equality, inequality, and substring (which we call ? for reasons that will become clear in a later chapter).

The following all return true:

    { "Yes, please." == "Yes, please." }
    { "No, thank you." != "Yes, please." }
    { "Yes, please" ? "ease" }
