<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 5) Branching The Flow

### Basic branching

Combining knots, options and diverts gives us the basic structure of a choose-your-own game.

    === paragraph_1 ===
    You stand by the wall of Analand, sword in hand.
    * [Open the gate] -> paragraph_2
    * [Smash down the gate] -> paragraph_3
    * [Turn back and go home] -> paragraph_4

    === paragraph_2 ===
    You open the gate, and step out onto the path.

    ...

### Branching and joining

Using diverts, the writer can branch the flow, and join it back up again, without showing the player that the flow has rejoined.

    === back_in_london ===

    We arrived into London at 9.45pm exactly.

    *	"There is not a moment to lose!"[] I declared.
        -> hurry_outside

    *	"Monsieur, let us savour this moment!"[] I declared.
        My master clouted me firmly around the head and dragged me out of the door.
        -> dragged_outside

    *	[We hurried home] -> hurry_outside


    === hurry_outside ===
    We hurried home to Savile Row -> as_fast_as_we_could


    === dragged_outside ===
    He insisted that we hurried home to Savile Row
    -> as_fast_as_we_could


    === as_fast_as_we_could ===
    <> as fast as we could.


### The story flow

Knots and diverts combine to create the basic story flow of the game. This flow is "flat" - there's no call-stack, and diverts aren't "returned" from.

In most ink scripts, the story flow starts at the top, bounces around in a spaghetti-like mess, and eventually, hopefully, reaches a `-> END`.

The very loose structure means writers can get on and write, branching and rejoining without worrying about the structure that they're creating as they go. There's no boiler-plate to creating new branches or diversions, and no need to track any state.

#### Advanced: Loops

You absolutely can use diverts to create looped content, and **ink** has several features to exploit this, including ways to make the content vary itself, and ways to control how often options can be chosen.

See the sections on [Varying Text](1-8-variable-text.md#8-variable-text) and [Conditional Choices](1-7-varying-choices.md#conditional-choices) for more information.

Oh, and the following is legal and not a great idea:

    === round ===
    and
    -> round
