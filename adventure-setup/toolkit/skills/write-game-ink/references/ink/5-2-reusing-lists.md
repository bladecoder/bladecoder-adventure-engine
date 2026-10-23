<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 2) Reusing Lists

The above example is fine for the kettle, but what if we have a pot on the stove as well? We can then define a list of states, but put them into variables - and as many variables as we want.

    LIST daysOfTheWeek = Monday, Tuesday, Wednesday, Thursday, Friday
    VAR today = Monday
    VAR tomorrow = Tuesday

### States can be used repeatedly

This allows us to use the same state machine in multiple places.

    LIST heatedWaterStates = cold, boiling, recently_boiled
    VAR kettleState = cold
    VAR potState = cold

    *	{kettleState == cold} [Turn on kettle]
        The kettle begins to boil and bubble.
        ~ kettleState = boiling
    *	{potState == cold} [Light stove]
        The water in the pot begins to boil and bubble.
        ~ potState = boiling

But what if we add a microwave as well? We might want start generalising our functionality a bit:

    LIST heatedWaterStates = cold, boiling, recently_boiled
    VAR kettleState = cold
    VAR potState = cold
    VAR microwaveState = cold

    === function boilSomething(ref thingToBoil, nameOfThing)
        The {nameOfThing} begins to heat up.
        ~ thingToBoil = boiling

    === do_cooking
    *	{kettleState == cold} [Turn on kettle]
        {boilSomething(kettleState, "kettle")}
    *	{potState == cold} [Light stove]
        {boilSomething(potState, "pot")}
    *	{microwaveState == cold} [Turn on microwave]
        {boilSomething(microwaveState, "microwave")}

or even...

    LIST heatedWaterStates = cold, boiling, recently_boiled
    VAR kettleState = cold
    VAR potState = cold
    VAR microwaveState = cold

    === cook_with(nameOfThing, ref thingToBoil)
    + 	{thingToBoil == cold} [Turn on {nameOfThing}]
        The {nameOfThing} begins to heat up.
        ~ thingToBoil = boiling
        -> do_cooking.done

    === do_cooking
    <- cook_with("kettle", kettleState)
    <- cook_with("pot", potState)
    <- cook_with("microwave", microwaveState)
    - (done)

Note that the "heatedWaterStates" list is still available as well, and can still be tested, and take a value.

#### List values can share names

Reusing lists brings with it ambiguity. If we have:

    LIST colours = red, green, blue, purple
    LIST moods = mad, happy, blue

    VAR status = blue

... how can the compiler know which blue you meant?

We resolve these using a `.` syntax similar to that used for knots and stitches.

    VAR status = colours.blue

...and the compiler will issue an error until you specify.

Note the "family name" of the state, and the variable containing a state, are totally separate. So

    { statesOfGrace == statesOfGrace.fallen:
        // is the current state "fallen"
    }

... is correct.


#### Advanced: a LIST is actually a variable

One surprising feature is the statement

    LIST statesOfGrace = ambiguous, saintly, fallen

actually does two things simultaneously: it creates three values, `ambiguous`, `saintly` and `fallen`, and gives them the name-parent `statesOfGrace` if needed; and it creates a variable called `statesOfGrace`.

And that variable can be used like a normal variable. So the following is valid, if horribly confusing and a bad idea:

    LIST statesOfGrace = ambiguous, saintly, fallen

    ~ statesOfGrace = 3.1415 // set the variable to a number not a list value

...and it wouldn't preclude the following from being fine:

    ~ temp anotherStateOfGrace = statesOfGrace.saintly
