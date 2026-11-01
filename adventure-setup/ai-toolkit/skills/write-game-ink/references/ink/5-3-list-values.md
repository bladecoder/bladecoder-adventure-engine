<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

## 3) List Values

When a list is defined, the values are listed in an order, and that order is considered to be significant. In fact, we can treat these values as if they *were* numbers. (That is to say, they are enums.)

    LIST volumeLevel = off, quiet, medium, loud, deafening
    VAR lecturersVolume = quiet
    VAR murmurersVolume = quiet

    { lecturersVolume < deafening:
        ~ lecturersVolume++

        { lecturersVolume > murmurersVolume:
            ~ murmurersVolume++
            The murmuring gets louder.
        }
    }

The values themselves can be printed using the usual `{...}` syntax, but this will print their name.

    The lecturer's voice becomes {lecturersVolume}.

### Converting values to numbers

The numerical value, if needed, can be got explicitly using the LIST_VALUE function. Note the first value in a list has the value 1, and not the value 0.

    The lecturer has {LIST_VALUE(deafening) - LIST_VALUE(lecturersVolume)} notches still available to him.

### Converting numbers to values

You can go the other way by using the list's name as a function:

    LIST Numbers = one, two, three
    VAR score = one
    ~ score = Numbers(2) // score will be "two"

### Advanced: defining your own numerical values

By default, the values in a list start at 1 and go up by one each time, but you can specify your own values if you need to.

    LIST primeNumbers = two = 2, three = 3, five = 5

If you specify a value, but not the next value, ink will assume an increment of 1. So the following is the same:

    LIST primeNumbers = two = 2, three, five = 5
