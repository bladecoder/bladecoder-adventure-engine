<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 2) Nested Flow

The weaves shown above are quite simple, "flat" structures. Whatever the player does, they take the same number of turns to get from top to bottom. However, sometimes certain choices warrant a bit more depth or complexity.

For that, we allow weaves to nest.

This section comes with a warning. Nested weaves are very powerful and very compact, but they can take a bit of getting used to!

### Options can be nested

Consider the following scene:

    - 	"Well, Poirot? Murder or suicide?"
    *	"Murder!"
    * 	"Suicide!"
    -	Ms. Christie lowered her manuscript a moment. The rest of the writing group sat, open-mouthed.

The first choice presented is "Murder!" or "Suicide!". If Poirot declares a suicide, there's no more to do, but in the case of murder, there's a follow-up question needed - who does he suspect?

We can add new options via a set of nested sub-choices. We tell the script that these new choices are "part of" another choice by using two asterisks, instead of just one.


    - 	"Well, Poirot? Murder or suicide?"
        *	"Murder!"
            "And who did it?"
            * * 	"Detective-Inspector Japp!"
            * * 	"Captain Hastings!"
            * * 	"Myself!"
        * 	"Suicide!"
        -	Mrs. Christie lowered her manuscript a moment. The rest of the writing group sat, open-mouthed.

(Note that it's good style to also indent the lines to show the nesting, but the compiler doesn't mind.)

And should we want to add new sub-options to the other route, we do that in similar fashion.

    - 	"Well, Poirot? Murder or suicide?"
        *	"Murder!"
            "And who did it?"
            * * 	"Detective-Inspector Japp!"
            * * 	"Captain Hastings!"
            * * 	"Myself!"
        * 	"Suicide!"
            "Really, Poirot? Are you quite sure?"
            * * 	"Quite sure."
            * *		"It is perfectly obvious."
        -	Mrs. Christie lowered her manuscript a moment. The rest of the writing group sat, open-mouthed.

Now, that initial choice of accusation will lead to specific follow-up questions - but either way, the flow will come back together at the gather point, for Mrs. Christie's cameo appearance.

But what if we want a more extended sub-scene?

### Gather points can be nested too

Sometimes, it's not a question of expanding the number of options, but having more than one additional beat of story. We can do this by nesting gather points as well as options.

    - 	"Well, Poirot? Murder or suicide?"
            *	"Murder!"
                "And who did it?"
                * * 	"Detective-Inspector Japp!"
                * * 	"Captain Hastings!"
                * * 	"Myself!"
                - - 	"You must be joking!"
                * * 	"Mon ami, I am deadly serious."
                * *		"If only..."
            * 	"Suicide!"
                "Really, Poirot? Are you quite sure?"
                * * 	"Quite sure."
                * *		"It is perfectly obvious."
            -	Mrs. Christie lowered her manuscript a moment. The rest of the writing group sat, open-mouthed.

If the player chooses the "murder" option, they'll have two choices in a row on their sub-branch - a whole flat weave, just for them.

#### Advanced: What gathers do

Gathers are hopefully intuitive, but their behaviour is a little harder to put into words: in general, after an option has been taken, the story finds the next gather down that isn't on a lower level, and diverts to it.

The basic idea is this: options separate the paths of the story, and gathers bring them back together. (Hence the name, "weave"!)


### You can nest as many levels are you like

Above, we used two levels of nesting; the main flow, and the sub-flow. But there's no limit to how many levels deep you can go.

    -	"Tell us a tale, Captain!"
        *	"Very well, you sea-dogs. Here's a tale..."
            * * 	"It was a dark and stormy night..."
                    * * * 	"...and the crew were restless..."
                            * * * *  "... and they said to their Captain..."
                                    * * * * *		"...Tell us a tale Captain!"
        *	"No, it's past your bed-time."
    -	To a man, the crew began to yawn.

After a while, this sub-nesting gets hard to read and manipulate, so it's good style to divert away to a new stitch if a side-choice goes unwieldy.

But, in theory at least, you could write your entire story as a single weave.

### Example: a conversation with nested nodes

Here's a longer example:

    - I looked at Monsieur Fogg
    *	... and I could contain myself no longer.
        'What is the purpose of our journey, Monsieur?'
        'A wager,' he replied.
        * * 	'A wager!'[] I returned.
                He nodded.
                * * * 	'But surely that is foolishness!'
                * * *  'A most serious matter then!'
                - - - 	He nodded again.
                * * *	'But can we win?'
                        'That is what we will endeavour to find out,' he answered.
                * * *	'A modest wager, I trust?'
                        'Twenty thousand pounds,' he replied, quite flatly.
                * * * 	I asked nothing further of him then[.], and after a final, polite cough, he offered nothing more to me. <>
        * * 	'Ah[.'],' I replied, uncertain what I thought.
        - - 	After that, <>
    *	... but I said nothing[] and <>
    - we passed the day in silence.
    - -> END

with a couple of possible playthroughs. A short one:

    I looked at Monsieur Fogg

    1: ... and I could contain myself no longer.
    2: ... but I said nothing

    > 2
    ... but I said nothing and we passed the day in silence.

and a longer one:

    I looked at Monsieur Fogg

    1: ... and I could contain myself no longer.
    2: ... but I said nothing

    > 1
    ... and I could contain myself no longer.
    'What is the purpose of our journey, Monsieur?'
    'A wager,' he replied.

    1: 'A wager!'
    2: 'Ah.'

    > 1
    'A wager!' I returned.
    He nodded.

    1: 'But surely that is foolishness!'
    2: 'A most serious matter then!'

    > 2
    'A most serious matter then!'
    He nodded again.

    1: 'But can we win?'
    2: 'A modest wager, I trust?'
    3: I asked nothing further of him then.

    > 2
    'A modest wager, I trust?'
    'Twenty thousand pounds,' he replied, quite flatly.
    After that, we passed the day in silence.

Hopefully, this demonstrates the philosophy laid out above: that weaves offer a compact way to offer a lot of branching, a lot of choices, but with the guarantee of getting from beginning to end!
