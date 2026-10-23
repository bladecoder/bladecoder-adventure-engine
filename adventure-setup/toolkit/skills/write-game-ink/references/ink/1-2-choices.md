<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 2) Choices

Input is offered to the player via text choices. A text choice is indicated by an `*` character.

If no other flow instructions are given, once made, the choice will flow into the next line of text.

    Hello world!
    *	Hello back!
        Nice to hear from you!

This produces the following game:

    Hello world!
    1: Hello back!

    > 1
    Hello back!
    Nice to hear from you!

By default, the text of a choice appears again, in the output.

### Suppressing choice text

Some games separate the text of a choice from its outcome. In **ink**, if the choice text is given in square brackets, the text of the choice will not be printed into response.

    Hello world!
    *	[Hello back!]
        Nice to hear from you!

produces

    Hello world!
    1: Hello back!

    > 1
    Nice to hear from you!

#### Advanced: mixing choice and output text

The square brackets in fact divide up the option content. What's before is printed in both choice and output; what's inside only in choice; and what's after, only in output. Effectively, they provide alternative ways for a line to end.

    Hello world!
    *	Hello [back!] right back to you!
        Nice to hear from you!

produces:

    Hello world!
    1: Hello back!
    > 1
    Hello right back to you!
    Nice to hear from you!

This is most useful when writing dialogue choices:

    "What's that?" my master asked.
    *	"I am somewhat tired[."]," I repeated.
        "Really," he responded. "How deleterious."

produces:

    "What's that?" my master asked.
    1: "I am somewhat tired."
    > 1
    "I am somewhat tired," I repeated.
    "Really," he responded. "How deleterious."

### Multiple Choices

To make choices really choices, we need to provide alternatives. We can do this simply by listing them:

    "What's that?" my master asked.
    *	"I am somewhat tired[."]," I repeated.
        "Really," he responded. "How deleterious."
    *	"Nothing, Monsieur!"[] I replied.
        "Very good, then."
    *  "I said, this journey is appalling[."] and I want no more of it."
        "Ah," he replied, not unkindly. "I see you are feeling frustrated. Tomorrow, things will improve."

This produces the following game:

    "What's that?" my master asked.

    1: "I am somewhat tired."
    2: "Nothing, Monsieur!"
    3: "I said, this journey is appalling."

    > 3
    "I said, this journey is appalling and I want no more of it."
    "Ah," he replied, not unkindly. "I see you are feeling frustrated. Tomorrow, things will improve."

The above syntax is enough to write a single set of choices. In a real game, we'll want to move the flow from one point to another based on what the player chooses. To do that, we need to introduce a bit more structure.
