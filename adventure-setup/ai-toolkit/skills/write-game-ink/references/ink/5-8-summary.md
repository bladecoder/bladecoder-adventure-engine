<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 8) Summary

To summarise a difficult section, **ink**'s list construction provides:

### Flags
* 	Each list entry is an event
* 	Use `+=` to mark an event as having occurred
*  	Test using `?` and `!?`

Example:

    LIST GameEvents = foundSword, openedCasket, metGorgon
    { GameEvents ? openedCasket }
    { GameEvents ? (foundSword, metGorgon) }
    ~ GameEvents += metGorgon

### State machines
* 	Each list entry is a state
*  Use `=` to set the state; `++` and `--` to step forward or backward
*  Test using `==`, `>` etc

Example:

    LIST PancakeState = ingredients_gathered, batter_mix, pan_hot, pancakes_tossed, ready_to_eat
    { PancakeState == batter_mix }
    { PancakeState < ready_to_eat }
    ~ PancakeState++

### Properties
*	Each list is a different property, with values for the states that property can take (on or off, lit or unlit, etc)
* 	Change state by removing the old state, then adding in the new
*  Test using `?` and `!?`

Example:

    LIST OnOffState = on, off
    LIST ChargeState = uncharged, charging, charged

    VAR PhoneState = (off, uncharged)

    *	{PhoneState !? uncharged } [Plug in phone]
        ~ PhoneState -= LIST_ALL(ChargeState)
        ~ PhoneState += charging
        You plug the phone into charge.
    *	{ PhoneState ? (on, charged) } [ Call my mother ]
