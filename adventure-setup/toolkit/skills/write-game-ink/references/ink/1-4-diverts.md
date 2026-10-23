<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 4) Diverts

### Knots divert to knots

You can tell the story to move from one knot to another using `->`, a "divert arrow". Diverts happen immediately without any user input.

    === back_in_london ===

    We arrived into London at 9.45pm exactly.
    -> hurry_home

    === hurry_home ===
    We hurried home to Savile Row as fast as we could.

#### Diverts are invisible

Diverts are intended to be seamless and can even happen mid-sentence:

    === hurry_home ===
    We hurried home to Savile Row -> as_fast_as_we_could

    === as_fast_as_we_could ===
    as fast as we could.

produces the same line as above:

    We hurried home to Savile Row as fast as we could.

#### Glue

The default behaviour inserts line-breaks before every new line of content. In some cases, however, content must insist on not having a line-break, and it can do so using `<>`, or "glue".

    === hurry_home ===
    We hurried home <>
    -> to_savile_row

    === to_savile_row ===
    to Savile Row
    -> as_fast_as_we_could

    === as_fast_as_we_could ===
    <> as fast as we could.

also produces:

    We hurried home to Savile Row as fast as we could.

You can't use too much glue: multiple glues next to each other have no additional effect. (And there's no way to "negate" a glue; once a line is sticky, it'll stick.)
