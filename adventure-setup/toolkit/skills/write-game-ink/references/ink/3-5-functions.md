<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 5) Functions

The use of parameters on knots means they are almost functions in the usual sense, but they lack one key concept - that of the call stack, and the use of return values.

**ink** includes functions: they are knots, with the following limitations and features:

A function:
- cannot contain stitches
- cannot use diverts or offer choices
- can call other functions
- can include printed content
- can return a value of any type
- can recurse safely

(Some of these may seem quite limiting, but for more story-oriented call-stack-style features, see the section on [Tunnels](4-1-tunnels.md#1-tunnels).)

Return values are provided via the `~ return` statement.

### Defining and calling functions

To define a function, simply declare a knot to be one:

    === function say_yes_to_everything ===
        ~ return true

    === function lerp(a, b, k) ===
        ~ return ((b - a) * k) + a

Functions are called by name, and with brackets, even if they have no parameters:

    ~ x = lerp(2, 8, 0.3)

    *	{say_yes_to_everything()} 'Yes.'

As in any other language, a function, once done, returns the flow to wherever it was called from - and despite not being allowed to divert the flow, functions can still call other functions.

    === function say_no_to_nothing ===
        ~ return say_yes_to_everything()

### Functions don't have to return anything

A function does not need to have a return value, and can simply do something that is worth packaging up:

    === function harm(x) ===
        { stamina < x:
            ~ stamina = 0
        - else:
            ~ stamina = stamina - x
        }

...though remember a function cannot divert, so while the above prevents a negative Stamina value, it won't kill a player who hits zero.

### Functions can be called inline

Functions can be called on `~` content lines, but can also be called during a piece of content. In this context, the return value, if there is one, is printed (as well as anything else the function wants to print.) If there is no return value, nothing is printed.

Content is, by default, 'glued in', so the following:

    Monsieur Fogg was looking {describe_health(health)}.

    === function describe_health(x) ===
    {
    - x == 100:
        ~ return "spritely"
    - x > 75:
        ~ return "chipper"
    - x > 45:
        ~ return "somewhat flagging"
    - else:
        ~ return "despondent"
    }

produces:

    Monsieur Fogg was looking despondent.

#### Examples

For instance, you might include:

    === function max(a,b) ===
        { a < b:
            ~ return b
        - else:
            ~ return a
        }

    === function exp(x, e) ===
        // returns x to the power e where e is an integer
        { e <= 0:
            ~ return 1
        - else:
            ~ return x * exp(x, e - 1)
        }

Then:

    The maximum of 2^5 and 3^3 is {max(exp(2,5), exp(3,3))}.

produces:

    The maximum of 2^5 and 3^3 is 32.


#### Example: turning numbers into words

The following example is long, but appears in pretty much every inkle game to date. (Recall that a hyphenated line inside multiline curly braces indicates either "a condition to test" or, if the curly brace began with a variable, "a value to compare against".)

    === function print_num(x) ===
    {
        - x >= 1000:
            {print_num(x / 1000)} thousand { x mod 1000 > 0:{print_num(x mod 1000)}}
        - x >= 100:
            {print_num(x / 100)} hundred { x mod 100 > 0:and {print_num(x mod 100)}}
        - x == 0:
            zero
        - else:
            { x >= 20:
                { x / 10:
                    - 2: twenty
                    - 3: thirty
                    - 4: forty
                    - 5: fifty
                    - 6: sixty
                    - 7: seventy
                    - 8: eighty
                    - 9: ninety
                }
                { x mod 10 > 0:<>-<>}
            }
            { x < 10 || x > 20:
                { x mod 10:
                    - 1: one
                    - 2: two
                    - 3: three
                    - 4: four
                    - 5: five
                    - 6: six
                    - 7: seven
                    - 8: eight
                    - 9: nine
                }
            - else:
                { x:
                    - 10: ten
                    - 11: eleven
                    - 12: twelve
                    - 13: thirteen
                    - 14: fourteen
                    - 15: fifteen
                    - 16: sixteen
                    - 17: seventeen
                    - 18: eighteen
                    - 19: nineteen
                }
            }
    }

which enables us to write things like:

    ~ price = 15

    I pulled out {print_num(price)} coins from my pocket and slowly counted them.
    "Oh, never mind," the trader replied. "I'll take half." And she took {print_num(price / 2)}, and pushed the rest back over to me.



### Parameters can be passed by reference

Function parameters can also be passed 'by reference', meaning that the function can actually alter the the variable being passed in, instead of creating a temporary variable with that value.

For instance, most **inkle** stories include the following:

    === function alter(ref x, k) ===
        ~ x = x + k

Lines such as:

    ~ gold = gold + 7
    ~ health = health - 4

then become:

    ~ alter(gold, 7)
    ~ alter(health, -4)

which are slightly easier to read, and (more usefully) can be done inline for maximum compactness.

    *	I ate a biscuit[] and felt refreshed. {alter(health, 2)}
    * 	I gave a biscuit to Monsieur Fogg[] and he wolfed it down most undecorously. {alter(foggs_health, 1)}
    -	<> Then we continued on our way.

Wrapping up simple operations in function can also provide a simple place to put debugging information, if required.
