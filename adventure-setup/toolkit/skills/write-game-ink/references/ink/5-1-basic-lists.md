<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 1) Basic Lists

The basic unit of state-tracking is a list of states, defined using the `LIST` keyword. Note that a list is really nothing like a C# list (which is an array).

For instance, we might have:

    LIST kettleState = cold, boiling, recently_boiled

This line defines two things: firstly three new values - `cold`, `boiling` and `recently_boiled` - and secondly, a variable, called `kettleState`, to hold these states.

We can tell the list what value to take:

    ~ kettleState = cold

We can change the value:

    *	[Turn on kettle]
        The kettle begins to bubble and boil.
        ~ kettleState = boiling

We can query the value:

    *	[Touch the kettle]
        { kettleState == cold:
            The kettle is cool to the touch.
        - else:
            The outside of the kettle is very warm!
        }

For convenience, we can give a list a value when it's defined using a bracket:

    LIST kettleState = cold, (boiling), recently_boiled
    // at the start of the game, this kettle is switched on. Edgy, huh?

...and if the notation for that looks a bit redundant, there's a reason for that coming up in a few subsections time.
