<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 3) Tracking a Weave

Sometimes, the weave structure is sufficient. But when it's not, we need a bit more control.

### Weaves are largely unaddressed

By default, lines of content in a weave don't have an address or label, which means they can't be diverted to, and they can't be tested for. In the most basic weave structure, choices vary the path the player takes through the weave and what they see, but once the weave is finished those choices and that path are forgotten.

But should we want to remember what the player has seen, we can - we add in labels where they're needed using the `(label_name)` syntax.

### Gathers and options can be labelled

Gather points at any nested level can be labelled using brackets.

    -  (top)

Once labelled, gather points can be diverted to, or tested for in conditionals, just like knots and stitches. This means you can use previous decisions to alter later outcomes inside the weave, while still keeping all the advantages of a clear, reliable forward-flow.

Options can also be labelled, just like gather points, using brackets. Label brackets come before conditions in the line.

These addresses can be used in conditional tests, which can be useful for creating options unlocked by other options.

    === meet_guard ===
    The guard frowns at you.

    * 	(greet) [Greet him]
        'Greetings.'
    *	(get_out) 'Get out of my way[.'],' you tell the guard.

    - 	'Hmm,' replies the guard.

    *	{greet} 	'Having a nice day?' // only if you greeted him

    * 	'Hmm?'[] you reply.

    *	{get_out} [Shove him aside] 	 // only if you threatened him
        You shove him sharply. He stares in reply, and draws his sword!
        -> fight_guard 			// this route diverts out of the weave

    -	'Mff,' the guard replies, and then offers you a paper bag. 'Toffee?'


### Scope

Inside the same block of weave, you can simply use the label name; from outside the block you need a path, either to a different stitch within the same knot:

    === knot ===
    = stitch_one
        - (gatherpoint) Some content.
    = stitch_two
        *	{stitch_one.gatherpoint} Option

or pointing into another knot:

    === knot_one ===
    -	(gather_one)
        * {knot_two.stitch_two.gather_two} Option

    === knot_two ===
    = stitch_two
        - (gather_two)
            *	{knot_one.gather_one} Option


#### Advanced: all options can be labelled

In truth, all content in ink is a weave, even if there are no gathers in sight. That means you can label *any* option in the game with a bracket label, and then reference it using the addressing syntax. In particular, this means you can test *which* option a player took to reach a particular outcome.

    === fight_guard ===
    ...
    = throw_something
    *	(rock) [Throw rock at guard] -> throw
    * 	(sand) [Throw sand at guard] -> throw

    = throw
    You hurl {throw_something.rock:a rock|a handful of sand} at the guard.


#### Advanced: Loops in a weave

Labelling allows us to create loops inside weaves. Here's a standard pattern for asking questions of an NPC.

    - (opts)
        *	'Can I get a uniform from somewhere?'[] you ask the cheerful guard.
            'Sure. In the locker.' He grins. 'Don't think it'll fit you, though.'
        *	'Tell me about the security system.'
            'It's ancient,' the guard assures you. 'Old as coal.'
        *	'Are there dogs?'
            'Hundreds,' the guard answers, with a toothy grin. 'Hungry devils, too.'
        // We require the player to ask at least one question
        *	{loop} [Enough talking]
            -> done
    - (loop)
        // loop a few times before the guard gets bored
        { -> opts | -> opts | }
        He scratches his head.
        'Well, can't stand around talking all day,' he declares.
    - (done)
        You thank the guard, and move away.





#### Advanced: diverting to options

Options can also be diverted to: but the divert goes to the output of having chosen that choice, *as though the choice had been chosen*. So the content printed will ignore square bracketed text, and if the option is once-only, it will be marked as used up.

    - (opts)
    *	[Pull a face]
        You pull a face, and the soldier comes at you! -> shove

    *	(shove) [Shove the guard aside] You shove the guard to one side, but he comes back swinging.

    *	{shove} [Grapple and fight] -> fight_the_guard

    - 	-> opts

produces:

    1: Pull a face
    2: Shove the guard aside

    > 1
    You pull a face, and the soldier comes at you! You shove the guard to one side, but he comes back swinging.

    1: Grapple and fight

    >

#### Advanced: Gathers directly after an option

The following is valid, and frequently useful.

    *	"Are you quite well, Monsieur?"[] I asked.
        - - (quitewell) "Quite well," he replied.
    *	"How did you do at the crossword, Monsieur?"[] I asked.
        -> quitewell
    *	I said nothing[] and neither did my Master.
    -	We fell into companionable silence once more.

Note the level 2 gather point directly below the first option: there's nothing to gather here, really, but it gives us a handy place to divert the second option to.
