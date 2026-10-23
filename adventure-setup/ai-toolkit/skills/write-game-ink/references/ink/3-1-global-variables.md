<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 1) Global Variables

The most powerful kind of variable, and arguably the most useful for a story, is a variable to store some unique property about the state of the game - anything from the amount of money in the protagonist's pocket, to a value representing the protagonist's state of mind.

This kind of variable is called "global" because it can be accessed from anywhere in the story - both set, and read from. (Traditionally, programming tries to avoid this kind of thing, as it allows one part of a program to mess with another, unrelated part. But a story is a story, and stories are all about consequences: what happens in Vegas rarely stays there.)

### Defining Global Variables

Global variables can be defined anywhere, via a `VAR` statement. They should be given an initial value, which defines what type of variable they are - integer, floating point (decimal), content, or a story address.

    VAR knowledge_of_the_cure = false
    VAR players_name = "Emilia"
    VAR number_of_infected_people = 521
    VAR current_epilogue = -> they_all_die_of_the_plague

### Using Global Variables

We can test global variables to control options, and provide conditional text, in a similar way to what we have previously seen.

    === the_train ===
        The train jolted and rattled. { mood > 0:I was feeling positive enough, however, and did not mind the odd bump|It was more than I could bear}.
        *	{ not knows_about_wager } 'But, Monsieur, why are we travelling?'[] I asked.
        * 	{ knows_about_wager} I contemplated our strange adventure[]. Would it be possible?

#### Advanced: storing diverts as variables

A "divert" statement is actually a type of value in itself, and can be stored, altered, and diverted to.

    VAR 	current_epilogue = -> everybody_dies

    === continue_or_quit ===
    Give up now, or keep trying to save your Kingdom?
    *  [Keep trying!] 	-> more_hopeless_introspection
    *  [Give up] 		-> current_epilogue


#### Advanced: Global variables are externally visible

Global variables can be accessed, and altered, from the runtime as well from the story, so provide a good way to communicate between the wider game and the story.

The **ink** layer is often be a good place to store gameplay-variables; there's no save/load issues to consider, and the story itself can react to the current values.



### Printing variables

The value of a variable can be printed as content using an inline syntax similar to sequences, and conditional text:

    VAR friendly_name_of_player = "Jackie"
    VAR age = 23

    My name is Jean Passepartout, but my friends call me {friendly_name_of_player}. I'm {age} years old.

This can be useful in debugging. For more complex printing based on logic and variables, see the section on [functions](3-5-functions.md#5-functions).

### Evaluating strings

It might be noticed that above we refered to variables as being able to contain "content", rather than "strings". That was deliberate, because a string defined in ink can contain ink - although it will always evaluate to a string. (Yikes!)

    VAR a_colour = ""

    ~ a_colour = "{~red|blue|green|yellow}"

    {a_colour}

... produces one of red, blue, green or yellow.

Note that once a piece of content like this is evaluated, its value is "sticky". (The quantum state collapses.) So the following:

    The goon hits you, and sparks fly before you eyes, {a_colour} and {a_colour}.

... won't produce a very interesting effect. (If you really want this to work, use a text function to print the colour!)

This is also why

    VAR a_colour = "{~red|blue|green|yellow}"

is explicitly disallowed; it would be evaluated on the construction of the story, which probably isn't what you want.
