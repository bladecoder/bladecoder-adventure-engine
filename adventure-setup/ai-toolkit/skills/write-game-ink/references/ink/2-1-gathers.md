<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 1) Gathers

### Gather points gather the flow back together

Let's go back to the first multi-choice example at the top of this document.

    "What's that?" my master asked.
        *	"I am somewhat tired[."]," I repeated.
            "Really," he responded. "How deleterious."
        *	"Nothing, Monsieur!"[] I replied.
        *  "I said, this journey is appalling[."] and I want no more of it."
            "Ah," he replied, not unkindly. "I see you are feeling frustrated. Tomorrow, things will improve."

In a real game, all three of these options might well lead to the same conclusion - Monsieur Fogg leaves the room. We can do this using a gather, without the need to create any new knots, or add any diverts.

    "What's that?" my master asked.
        *	"I am somewhat tired[."]," I repeated.
            "Really," he responded. "How deleterious."
        *	"Nothing, Monsieur!"[] I replied.
            "Very good, then."
        *  "I said, this journey is appalling[."] and I want no more of it."
        "Ah," he replied, not unkindly. "I see you are feeling frustrated. Tomorrow, things will improve."

    -	With that Monsieur Fogg left the room.

This produces the following playthrough:

    "What's that?" my master asked.

    1: "I am somewhat tired."
    2: "Nothing, Monsieur!"
    3: "I said, this journey is appalling."

    > 1
    "I am somewhat tired," I repeated.
    "Really," he responded. "How deleterious."
    With that Monsieur Fogg left the room.

### Options and gathers form chains of content

We can string these gather-and-branch sections together to make branchy sequences that always run forwards.

    === escape ===
    I ran through the forest, the dogs snapping at my heels.

        * 	I checked the jewels[] were still in my pocket, and the feel of them brought a spring to my step. <>

        *  I did not pause for breath[] but kept on running. <>

        *	I cheered with joy. <>

    - 	The road could not be much further! Mackie would have the engine running, and then I'd be safe.

        *	I reached the road and looked about[]. And would you believe it?
        * 	I should interrupt to say Mackie is normally very reliable[]. He's never once let me down. Or rather, never once, previously to that night.

    -	The road was empty. Mackie was nowhere to be seen.

This is the most basic kind of weave. The rest of this section details  additional features that allow weaves to nest, contain side-tracks and diversions, divert within themselves, and above all, reference earlier choices to influence later ones.

#### The weave philosophy

Weaves are more than just a convenient encapsulation of branching flow; they're also a way to author more robust content. The `escape` example above has already four possible routes through, and a more complex sequence might have lots and lots more. Using normal diverts, one has to check the links by chasing the diverts from point to point and it's easy for errors to creep in.

With a weave, the flow is guaranteed to start at the top and "fall" to the bottom. Flow errors are impossible in a basic weave structure, and the output text can be easily skim read. That means there's no need to actually test all the branches in game to be sure they work as intended.

Weaves also allow for easy redrafting of choice-points; in particular, it's easy to break a sentence up and insert additional choices for variety or pacing reasons, without having to re-engineer any flow.
