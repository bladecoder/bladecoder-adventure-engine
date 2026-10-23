<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 1) Tunnels

The default structure for **ink** stories is a "flat" tree of choices, branching and joining back together, perhaps looping, but with the story always being "at a certain place".

But this flat structure makes certain things difficult: for example, imagine a game in which the following interaction can happen:

    === crossing_the_date_line ===
    *	"Monsieur!"[] I declared with sudden horror. "I have just realised. We have crossed the international date line!"
    -	Monsieur Fogg barely lifted an eyebrow. "I have adjusted for it."
    *	I mopped the sweat from my brow[]. A relief!
    * 	I nodded, becalmed[]. Of course he had!
    *  I cursed, under my breath[]. Once again, I had been belittled!

...but it can happen at several different places in the story. We don't want to have to write copies of the content for each different place, but when the content is finished it needs to know where to return to. We can do this using parameters:

    === crossing_the_date_line(-> return_to) ===
    ...
    -	-> return_to

    ...

    === outside_honolulu ===
    We arrived at the large island of Honolulu.
    - (postscript)
        -> crossing_the_date_line(-> done)
    - (done)
        -> END

    ...

    === outside_pitcairn_island ===
    The boat sailed along the water towards the tiny island.
    - (postscript)
        -> crossing_the_date_line(-> done)
    - (done)
        -> END

Both of these locations now call and execute the same segment of storyflow, but once finished they return to where they need to go next.

But what if the section of story being called is more complex - what if it spreads across several knots? Using the above, we'd have to keep passing the 'return-to' parameter from knot to knot, to ensure we always knew where to return.

So instead, **ink** integrates this into the language with a new kind of divert, that functions rather like a subroutine, and is called a 'tunnel'.

### Tunnels run sub-stories

The tunnel syntax looks like a divert, with another divert on the end:

    -> crossing_the_date_line ->

This means "do the crossing_the_date_line story, then continue from here".

Inside the tunnel itself, the syntax is simplified from the parameterised example: all we do is end the tunnel using the `->->` statement which means, essentially, "go on".

    === crossing_the_date_line ===
    // this is a tunnel!
    ...
    - 	->->

Note that tunnel knots aren't declared as such, so the compiler won't check that tunnels really do end in `->->` statements, except at run-time. So you will need to write carefully to ensure that all the flows into a tunnel really do come out again.

Tunnels can also be chained together, or finish on a normal divert:

    ...
    // this runs the tunnel, then diverts to 'done'
    -> crossing_the_date_line -> done
    ...

    ...
    //this runs one tunnel, then another, then diverts to 'done'
    -> crossing_the_date_line -> check_foggs_health -> done
    ...

Tunnels can be nested, so the following is valid:

    === plains ===
    = night_time
        The dark grass is soft under your feet.
        +	[Sleep]
            -> sleep_here -> wake_here -> day_time
    = day_time
        It is time to move on.

    === wake_here ===
        You wake as the sun rises.
        +	[Eat something]
            -> eat_something ->
        +	[Make a move]
        -	->->

    === sleep_here ===
        You lie down and try to close your eyes.
        -> monster_attacks ->
        Then it is time to sleep.
        -> dream ->
        ->->

... and so on.


#### Advanced: Tunnels can return elsewhere

Sometimes, in a story, things happen. So sometimes a tunnel can't guarantee that it will always want to go back to where it came from. **ink** supplies a syntax to allow you to "returning from a tunnel but actually go somewhere else" but it should be used with caution as the possibility of getting very confused is very high indeed.

Still, there are cases where it's indispensable:

    === fall_down_cliff
    -> hurt(5) ->
    You're still alive! You pick yourself up and walk on.

    === hurt(x)
        ~ stamina -= x
        { stamina <= 0:
            ->-> youre_dead
        }

    === youre_dead
    Suddenly, there is a white light all around you. Fingers lift an eyepiece from your forehead. 'You lost, buddy. Out of the chair.'

And even in less drastic situations, we might want to break up the structure:

    -> talk_to_jim ->

     === talk_to_jim
     - (opts)
        *	[ Ask about the warp lacelles ]
            -> warp_lacells ->
        *	[ Ask about the shield generators ]
            -> shield_generators ->
        * 	[ Stop talking ]
            ->->
     - -> opts

     = warp_lacells
        { shield_generators : ->-> argue }
        "Don't worry about the warp lacelles. They're fine."
        ->->

     = shield_generators
        { warp_lacells : ->-> argue }
        "Forget about the shield generators. They're good."
        ->->

     = argue
        "What's with all these questions?" Jim demands, suddenly.
        ...
        ->->

#### Advanced: Tunnels use a call-stack

Tunnels are on a call-stack, so can safely recurse.
