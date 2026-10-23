<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 2) Threads

So far, everything in ink has been entirely linear, despite all the branching and diverting. But it's actually possible for a writer to 'fork' a story into different sub-sections, to cover more possible player actions.

We call this 'threading', though it's not really threading in the sense that computer scientists mean it: it's more like stitching in new content from various places.

Note that this is definitely an advanced feature: the engineering stories becomes somewhat more complex once threads are involved!

### Threads join multiple sections together

Threads allow you to compose sections of content from multiple sources in one go. For example:

    == thread_example ==
    I had a headache; threading is hard to get your head around.
    <- conversation
    <- walking


    == conversation ==
    It was a tense moment for Monty and me.
     * "What did you have for lunch today?"[] I asked.
        "Spam and eggs," he replied.
     * "Nice weather, we're having,"[] I said.
        "I've seen better," he replied.
     - -> house

    == walking ==
    We continued to walk down the dusty road.
     * [Continue walking]
        -> house

    == house ==
    Before long, we arrived at his house.
    -> END

It allows multiple sections of story to combined together into a single section:

    I had a headache; threading is hard to get your head around.
    It was a tense moment for Monty and me.
    We continued to walk down the dusty road.
    1: "What did you have for lunch today?"
    2: "Nice weather, we're having,"
    3: Continue walking

On encountering a thread statement such as `<- conversation`, the compiler will fork the story flow. The first fork considered will run the content at `conversation`, collecting up any options it finds. Once it has run out of flow here it'll then run the other fork.

All the content is collected and shown to the player. But when a choice is chosen, the engine will move to that fork of the story and collapse and discard the others.

Note that global variables are *not* forked, including the read counts of knots and stitches.

### Uses of threads

In a normal story, threads might never be needed.

But for games with lots of independent moving parts, threads quickly become essential. Imagine a game in which characters move independently around a map: the main story hub for a room might look like the following:

    CONST HALLWAY = 1
    CONST OFFICE = 2

    VAR player_location = HALLWAY
    VAR generals_location = HALLWAY
    VAR doctors_location = OFFICE

    == run_player_location
        {
            - player_location == HALLWAY: -> hallway
        }

    == hallway ==
        <- characters_present(HALLWAY)
        *	[Drawers]	-> examine_drawers
        * 	[Wardrobe] -> examine_wardrobe
        *  [Go to Office] 	-> go_office
        -	-> run_player_location
    = examine_drawers
        // etc...

    // Here's the thread, which mixes in dialogue for characters you share the room with at the moment.

    == characters_present(room)
        { generals_location == room:
            <- general_conversation
        }
        { doctors_location == room:
            <- doctor_conversation
        }
        -> DONE

    == general_conversation
        *	[Ask the General about the bloodied knife]
            "It's a bad business, I can tell you."
        -	-> run_player_location

    == doctor_conversation
        *	[Ask the Doctor about the bloodied knife]
            "There's nothing strange about blood, is there?"
        -	-> run_player_location



Note in particular, that we need an explicit way to return the player who has gone down a side-thread to return to the main flow. In most cases, threads will either need a parameter telling them where to return to, or they'll need to end the current story section.


### When does a side-thread end?

Side-threads end when they run out of flow to process: and note, they collect up options to display later (unlike tunnels, which collect options, display them and follow them until they hit an explicit return, possibly several moves later).

Sometimes a thread has no content to offer - perhaps there is no conversation to have with a character after all, or perhaps we have simply not written it yet. In that case, we must mark the end of the thread explicitly.

If we didn't, the end of content might be a story-bug or a hanging story thread, and we want the compiler to tell us about those.

### Using `-> DONE`

In cases where we want to mark the end of a thread, we use `-> DONE`: meaning "the flow intentionally ends here". If we don't, we might end up with a warning message - we can still play the game, but it's a reminder that we have unfinished business.

The example at the start of this section will generate a warning; it can be fixed as follows:

    == thread_example ==
    I had a headache; threading is hard to get your head around.
    <- conversation
    <- walking
    -> DONE

The extra DONE tells ink that the flow here has ended and it should rely on the threads for the next part of the story.

Note that we don't need a `-> DONE` if the flow ends with options that fail their conditions. The engine treats this as a valid, intentional, end of flow state.

**You do not need a `-> DONE` after an option has been chosen**. Once an option is chosen, a thread is no longer a thread - it is simply the normal story flow once more.

Using `-> END` in this case will not end the thread, but the whole story flow. (And this is the real reason for having two different ways to end flow.)


#### Example: adding the same choice to several places

Threads can be used to add the same choice into lots of different places. When using them this way, it's normal to pass a divert as a parameter, to tell the story where to go after the choice is done.

    === outside_the_house
    The front step. The house smells. Of murder. And lavender.
    - (top)
        <- review_case_notes(-> top)
        *	[Go through the front door]
            I stepped inside the house.
            -> the_hallway
        * 	[Sniff the air]
            I hate lavender. It makes me think of soap, and soap makes me think about my marriage.
            -> top

    === the_hallway
    The hallway. Front door open to the street. Little bureau.
    - (top)
        <- review_case_notes(-> top)
        *	[Go through the front door]
            I stepped out into the cool sunshine.
            -> outside_the_house
        * 	[Open the bureau]
            Keys. More keys. Even more keys. How many locks do these people need?
            -> top

    === review_case_notes(-> go_back_to)
    +	{not done || TURNS_SINCE(-> done) > 10}
        [Review my case notes]
        // the conditional ensures you don't get the option to check repeatedly
        {I|Once again, I} flicked through the notes I'd made so far. Still not obvious suspects.
    - 	(done) -> go_back_to

Note this is different than a tunnel, which runs the same block of content but doesn't give a player a choice. So a layout like:

    <- childhood_memories(-> next)
    *	[Look out of the window]
        I daydreamed as we rolled along...
     - (next) Then the whistle blew...

might do exactly the same thing as:

    *	[Remember my childhood]
        -> think_back ->
    *	[Look out of the window]
        I daydreamed as we rolled along...
    - 	(next) Then the whistle blew...

but as soon as the option being threaded in includes multiple choices, or conditional logic on choices (or any text content, of course!), the thread version becomes more practical.


#### Example: organisation of wide choice points

A game which uses ink as a script rather than a literal output might often generate very large numbers of parallel choices, intended to be filtered by the player via some other in-game interaction - such as walking around an environment. Threads can be useful in these cases simply to divide up choices.

```
=== the_kitchen
- (top)
    <- drawers(-> top)
    <- cupboards(-> top)
    <- room_exits
= drawers (-> goback)
    // choices about the drawers...
    ...
= cupboards(-> goback)
    // choices about cupboards
    ...
= room_exits
    // exits; doesn't need a "return point" as if you leave, you go elsewhere
    ...
```
