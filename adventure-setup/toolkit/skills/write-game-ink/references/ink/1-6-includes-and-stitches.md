<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 6) Includes and Stitches

### Knots can be subdivided

As stories get longer, they become more confusing to keep organised without some additional structure.

Knots can include sub-sections called "stitches". These are marked using a single equals sign.

    === the_orient_express ===
    = in_first_class
        ...
    = in_third_class
        ...
    = in_the_guards_van
        ...
    = missed_the_train
        ...

One could use a knot for a scene, for instance, and stitches for the events within the scene.

### Stitches have unique names

A stitch can be diverted to using its "address".

    *	[Travel in third class]
        -> the_orient_express.in_third_class

    *	[Travel in the guard's van]
        -> the_orient_express.in_the_guards_van

### The first stitch is the default

Diverting to a knot which contains stitches will divert to the first stitch in the knot. So:

    *	[Travel in first class]
        "First class, Monsieur. Where else?"
        -> the_orient_express

is the same as:

    *	[Travel in first class]
        "First class, Monsieur. Where else?"
        -> the_orient_express.in_first_class

(...unless we move the order of the stitches around inside the knot!)

You can also include content at the top of a knot outside of any stitch. However, you need to remember to divert out of it - the engine *won't* automatically enter the first stitch once it's worked its way through the header content.

    === the_orient_express ===

    We boarded the train, but where?
    *	[First class] -> in_first_class
    *	[Second class] -> in_second_class

    = in_first_class
        ...
    = in_second_class
        ...


### Local diverts

From inside a knot, you don't need to use the full address for a stitch.

    -> the_orient_express

    === the_orient_express ===
    = in_first_class
        I settled my master.
        *	[Move to third class]
            -> in_third_class

    = in_third_class
        I put myself in third.

This means stitches and knots can't share names, but several knots can contain the same stitch name. (So both the Orient Express and the SS Mongolia can have first class.)

The compiler will warn you if ambiguous names are used.

### Script files can be combined

You can also split your content across multiple files, using an include statement.

    INCLUDE newspaper.ink
    INCLUDE cities/vienna.ink
    INCLUDE journeys/orient_express.ink

Include statements should always go at the top of a file, and not inside knots.

There are no rules about what file a knot must be in to be diverted to. (In other words, separating files has no effect on the game's namespacing).
