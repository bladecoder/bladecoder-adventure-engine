<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

##  6) Constants


### Global Constants

Interactive stories often rely on state machines, tracking what stage some higher level process has reached. There are lots of ways to do this, but the most conveninent is to use constants.

Sometimes, it's convenient to define constants to be strings, so you can print them out, for gameplay or debugging purposes.

    CONST HASTINGS = "Hastings"
    CONST POIROT = "Poirot"
    CONST JAPP = "Japp"

    VAR current_chief_suspect = HASTINGS

    === review_evidence ===
        { found_japps_bloodied_glove:
            ~ current_chief_suspect = POIROT
        }
        Current Suspect: {current_chief_suspect}

Sometimes giving them values is useful:

    CONST PI = 3.14
    CONST VALUE_OF_TEN_POUND_NOTE = 10

And sometimes the numbers are useful in other ways:

    CONST LOBBY = 1
    CONST STAIRCASE = 2
    CONST HALLWAY = 3

    CONST HELD_BY_AGENT = -1

    VAR secret_agent_location = LOBBY
    VAR suitcase_location = HALLWAY

    === report_progress ===
    {
        -  secret_agent_location == suitcase_location:
        The secret agent grabs the suitcase!
        ~ suitcase_location = HELD_BY_AGENT

    -  secret_agent_location < suitcase_location:
        The secret agent moves forward.
        ~ secret_agent_location++
    }

Constants are simply a way to allow you to give story states easy-to-understand names.
