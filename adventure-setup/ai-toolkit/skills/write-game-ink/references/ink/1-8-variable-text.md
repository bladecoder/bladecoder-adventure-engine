<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 8) Variable Text

### Text can vary

So far, all the content we've seen has been static, fixed pieces of text. But content can also vary at the moment of being printed.

### Sequences, cycles and other alternatives

The simplest variations of text are provided by alternatives, which are selected from depending on some kind of rule. **ink** supports several types. Alternatives are written inside `{`...`}` curly brackets, with elements separated by `|` symbols (vertical divider lines).

These are only useful if a piece of content is visited more than once!

#### Types of alternatives

**Sequences** (the default):

A sequence (or a "stopping block") is a set of alternatives that tracks how many times its been seen, and each time, shows the next element along. When it runs out of new content it continues the show the final element.

    The radio hissed into life. {"Three!"|"Two!"|"One!"|There was the white noise racket of an explosion.|But it was just static.}

    {I bought a coffee with my five-pound note.|I bought a second coffee for my friend.|I didn't have enough money to buy any more coffee.}

**Cycles** (marked with a `&`):

Cycles are like sequences, but they loop their content.

    It was {&Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday} today.


**Once-only** (marked with a `!`):

Once-only alternatives are like sequences, but when they run out of new content to display, they display nothing. (You can think of a once-only alternative as a sequence with a blank last entry.)

    He told me a joke. {!I laughed politely.|I smiled.|I grimaced.|I promised myself to not react again.}

**Shuffles** (marked with a `~`):

Shuffles produce randomised output.

    I tossed the coin. {~Heads|Tails}.

#### Features of Alternatives

Alternatives can contain blank elements.

    I took a step forward. {!||||Then the lights went out. -> eek}

Alternatives can be nested.

    The Ratbear {&{wastes no time and |}swipes|scratches} {&at you|into your {&leg|arm|cheek}}.

Alternatives can include divert statements.

    I {waited.|waited some more.|snoozed.|woke up and waited more.|gave up and left. -> leave_post_office}

They can also be used inside choice text:

    + 	"Hello, {&Master|Monsieur Fogg|you|brown-eyes}!"[] I declared.

(...with one caveat; you can't start an option's text with a `{`, as it'll look like a conditional.)

(...but the caveat has a caveat, if you escape a whitespace `\ ` before your `{` ink will recognise it as text.)

    +\	{&They headed towards the Sandlands|They set off for the desert|The party followed the old road South}

#### Examples

Alternatives can be used inside loops to create the appearance of intelligent, state-tracking gameplay without particular effort.

Here's a one-knot version of whack-a-mole. Note we use once-only options, and a fallback, to ensure the mole doesn't move around, and the game will always end.

    === whack_a_mole ===
        {I heft the hammer.|{~Missed!|Nothing!|No good. Where is he?|Ah-ha! Got him! -> END}}
        The {&mole|{&nasty|blasted|foul} {&creature|rodent}} is {in here somewhere|hiding somewhere|still at large|laughing at me|still unwhacked|doomed}. <>
        {!I'll show him!|But this time he won't escape!}
        * 	[{&Hit|Smash|Try} top-left] 	-> whack_a_mole
        *  [{&Whallop|Splat|Whack} top-right] -> whack_a_mole
        *  [{&Blast|Hammer} middle] -> whack_a_mole
        *  [{&Clobber|Bosh} bottom-left] 	-> whack_a_mole
        *  [{&Nail|Thump} bottom-right] 	-> whack_a_mole
        *   ->
                Then you collapse from hunger. The mole has defeated you!
                -> END


produces the following 'game':

    I heft the hammer.
    The mole is in here somewhere. I'll show him!

    1: Hit top-left
    2: Whallop top-right
    3: Blast middle
    4: Clobber bottom-left
    5: Nail bottom-right

    > 1
    Missed!
    The nasty creature is hiding somewhere. But this time he won't escape!

    1: Splat top-right
    2: Hammer middle
    3: Bosh bottom-left
    4: Thump bottom-right

    > 4
    Nothing!
    The mole is still at large.
    1: Whack top-right
    2: Blast middle
    3: Clobber bottom-left

    > 2
    Where is he?
    The blasted rodent is laughing at me.
    1: Whallop top-right
    2: Bosh bottom-left

    > 1
    Ah-ha! Got him!


And here's a bit of lifestyle advice. Note the sticky choice - the lure of the television will never fade:

    === turn_on_television ===
    I turned on the television {for the first time|for the second time|again|once more}, but there was {nothing good on, so I turned it off again|still nothing worth watching|even less to hold my interest than before|nothing but rubbish|a program about sharks and I don't like sharks|nothing on}.
    +	[Try it again]	 		-> turn_on_television
    *	[Go outside instead]	-> go_outside_instead

    === go_outside_instead ===
    -> END



#### Sneak Preview: Multiline alternatives
**ink** has another format for making alternatives of varying content blocks, too. See the section on [multiline blocks](3-3-conditional-blocks.md#multiline-blocks) for details.



### Conditional Text

Text can also vary depending on logical tests, just as options can.

    {met_blofeld: "I saw him. Only for a moment." }

and

    "His real name was {met_blofeld.learned_his_name: Franz|a secret}."

These can appear as separate lines, or within a section of content. They can even be nested, so:

    {met_blofeld: "I saw him. Only for a moment. His real name was {met_blofeld.learned_his_name: Franz|kept a secret}." | "I missed him. Was he particularly evil?" }

can produce either:

    "I saw him. Only for a moment. His real name was Franz."

or:

    "I saw him. Only for a moment. His real name was kept a secret."

or:

    "I missed him. Was he particularly evil?"
