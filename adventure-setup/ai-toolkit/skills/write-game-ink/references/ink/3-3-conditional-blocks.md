<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 3) Conditional blocks (if/else)

We've seen conditionals used to control options and story content; **ink** also provides an equivalent of the normal if/else-if/else structure.

### A simple 'if'

The if syntax takes its cue from the other conditionals used so far, with the `{`...`}` syntax indicating that something is being tested.

    { x > 0:
        ~ y = x - 1
    }

Else conditions can be provided:

    { x > 0:
        ~ y = x - 1
    - else:
        ~ y = x + 1
    }

### Extended if/else if/else blocks

The above syntax is actually a specific case of a more general structure, something like a "switch" statement of another language:

    {
        - x > 0:
            ~ y = x - 1
        - else:
            ~ y = x + 1
    }

And using this form we can include 'else-if' conditions:

    {
        - x == 0:
            ~ y = 0
        - x > 0:
            ~ y = x - 1
        - else:
            ~ y = x + 1
    }

(Note, as with everything else, the white-space is purely for readability and has no syntactic meaning.)

### Switch blocks

And there's also an actual switch statement:

    { x:
    - 0: 	zero
    - 1: 	one
    - 2: 	two
    - else: lots
    }

#### Example: context-relevant content

Note these tests don't have to be variable-based and can use read-counts, just as other conditionals can, and the following construction is quite frequent, as a way of saying "do some content which is relevant to the current game state":

    === dream ===
        {
            - visited_snakes && not dream_about_snakes:
                ~ fear++
                -> dream_about_snakes

            - visited_poland && not dream_about_polish_beer:
                ~ fear--
                -> dream_about_polish_beer

            - else:
                // breakfast-based dreams have no effect
                -> dream_about_marmalade
        }

The syntax has the advantage of being easy to extend, and prioritise.



### Conditional blocks are not limited to logic

Conditional blocks can be used to control story content as well as logic:

    I stared at Monsieur Fogg.
    { know_about_wager:
        <> "But surely you are not serious?" I demanded.
    - else:
        <> "But there must be a reason for this trip," I observed.
    }
    He said nothing in reply, merely considering his newspaper with as much thoroughness as entomologist considering his latest pinned addition.

You can even put options inside conditional blocks:

    { door_open:
        * 	I strode out of the compartment[] and I fancied I heard my master quietly tutting to himself. 			-> go_outside
    - else:
        *	I asked permission to leave[] and Monsieur Fogg looked surprised. 	-> open_door
        * 	I stood and went to open the door[]. Monsieur Fogg seemed untroubled by this small rebellion. -> open_door
    }

...but note that the lack of weave-syntax and nesting in the above example isn't accidental: to avoid confusing the various kinds of nesting at work, you aren't allowed to include gather points inside conditional blocks.

### Multiline blocks

There's one other class of multiline block, which expands on the alternatives system from above. The following are all valid and do what you might expect:

    // Sequence: go through the alternatives, and stick on last
    { stopping:
        -	I entered the casino.
        -  I entered the casino again.
        -  Once more, I went inside.
    }

    // Shuffle: show one at random
    At the table, I drew a card. <>
    { shuffle:
        - 	Ace of Hearts.
        - 	King of Spades.
        - 	2 of Diamonds.
            'You lose this time!' crowed the croupier.
    }

    // Cycle: show each in turn, and then cycle
    { cycle:
        - I held my breath.
        - I waited impatiently.
        - I paused.
    }

    // Once: show each, once, in turn, until all have been shown
    { once:
        - Would my luck hold?
        - Could I win the hand?
    }

#### Advanced: modified shuffles

The shuffle block above is really a "shuffled cycle"; in that it'll shuffle the content, play through it, then reshuffle and go again.

There are two other versions of shuffle:

`shuffle once` which will shuffle the content, play through it, and then do nothing.

    { shuffle once:
    -	The sun was hot.
    - 	It was a hot day.
    }

`shuffle stopping` will shuffle all the content (except the last entry), and once its been played, it'll stick on the last entry.

    { shuffle stopping:
    - 	A silver BMW roars past.
    -	A bright yellow Mustang takes the turn.
    - 	There are like, cars, here.
    }
