<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 4) Temporary Variables

### Temporary variables are for scratch calculations

Sometimes, a global variable is unwieldy. **ink** provides temporary variables for quick calculations of things.

    === near_north_pole ===
        ~ temp number_of_warm_things = 0
        { blanket:
            ~ number_of_warm_things++
        }
        { ear_muffs:
            ~ number_of_warm_things++
        }
        { gloves:
            ~ number_of_warm_things++
        }
        { number_of_warm_things > 2:
            Despite the snow, I felt incorrigibly snug.
        - else:
            That night I was colder than I have ever been.
        }

The value in a temporary variable is thrown away after the story leaves the stitch in which it was defined.

### Knots and stitches can take parameters

A particularly useful form of temporary variable is a parameter. Any knot or stitch can be given a value as a parameter.

    *	[Accuse Hasting]
            -> accuse("Hastings")
    *	[Accuse Mrs Black]
            -> accuse("Claudia")
    *	[Accuse myself]
            -> accuse("myself")

    === accuse(who) ===
        "I accuse {who}!" Poirot declared.
        "Really?" Japp replied. "{who == "myself":You did it?|{who}?}"
        "And why not?" Poirot shot back.


... and you'll need to use parameters if you want to pass a temporary value from one stitch to another!

#### Example: a recursive knot definition

Temporary variables are safe to use in recursion (unlike globals), so the following will work.

    -> add_one_to_one_hundred(0, 1)

    === add_one_to_one_hundred(total, x) ===
        ~ total = total + x
        { x == 100:
            -> finished(total)
        - else:
            -> add_one_to_one_hundred(total, x + 1)
        }

    === finished(total) ===
        "The result is {total}!" you announce.
        Gauss stares at you in horror.
        -> END


(In fact, this kind of definition is useful enough that **ink** provides a special kind of knot, called, imaginatively enough, a `function`, which comes with certain restrictions and can return a value. See the section below.)


#### Advanced: sending divert targets as parameters

Knot/stitch addresses are a type of value, indicated by a `->` character, and can be stored and passed around. The following is therefore legal, and often useful:

    === sleeping_in_hut ===
        You lie down and close your eyes.
        -> generic_sleep (-> waking_in_the_hut)

    ===	 generic_sleep (-> waking)
        You sleep perchance to dream etc. etc.
        -> waking

    === waking_in_the_hut
        You get back to your feet, ready to continue your journey.

...but note the `->` in the `generic_sleep` definition: that's the one case in **ink** where a parameter needs to be typed: because it's too easy to otherwise accidentally do the following:

    === sleeping_in_hut ===
        You lie down and close your eyes.
        -> generic_sleep (waking_in_the_hut)

... which sends the read count of `waking_in_the_hut` into the sleeping knot, and then attempts to divert to it.
