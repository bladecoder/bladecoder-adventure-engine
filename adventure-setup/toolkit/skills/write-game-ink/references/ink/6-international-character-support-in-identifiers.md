<!-- Extracted from inkle/ink Documentation/WritingWithInk.md. Source and licenses: index.md#provenance. -->

# Part 6: International character support in identifiers

By default, ink has no limitations on the use of non-ASCII characters inside the story content. However, a limitation currently exsits
on the characters that can be used for names of constants, variables, stictches, diverts and other named flow elements (a.k.a. *identifiers*).

Sometimes it is inconvenient for a writer using a non-ASCII language to write a story because they have to constantly switch to naming identifiers in ASCII and then switching back to whatever language they are using for the story. In addition, naming identifiers in the author's own language could improve the overal readibility of the raw story format.

In an effort to assist in the above scenario, ink *automatically* supports a list of pre-defined non-ASCII character ranges that can be used as identifiers. In general, those ranges have been selected to include the alpha-numeric subset of the official unicode character range, which would suffice for naming identifiers. The below section gives more detailed information on the non-ASCII characters that ink automatically supports.

### Supported Identifier Characters

The support for the additional character ranges in ink is currently limited to a predefined set of character ranges.

Below is a listing of the currently supported identifier ranges.

 - **Arabic**

   Enables characters for languages of the Arabic family and is a subset of the official *Arabic* unicode range `\u0600`-`\u06FF`.


 - **Armenian**

   Enables characters for the Armenian language and is a subset of the official *Armenian* unicode range `\u0530`-`\u058F`.


 - **Cyrillic**

   Enables characters for languages using the Cyrillic alphabet and is a subset of the official *Cyrillic* unicode range `\u0400`-`\u04FF`.


 - **Greek**

   Enables characters for languages using the Greek alphabet and is a subset of the official *Greek and Coptic* unicode range `\u0370`-`\u03FF`.


 - **Hebrew**

   Enables characters in Hebrew using the Hebrew alphabet and is a subset of the official *Hebrew* unicode range `\u0590`-`\u05FF`.


 - **Latin Extended A**

   Enables an extended character range subset of the Latin alphabet - completely represented by the official *Latin Extended-A* unicode range `\u0100`-`\u017F`.


 - **Latin Extended B**

   Enables an extended character range subset of the Latin alphabet - completely represented by the official *Latin Extended-B* unicode range `\u0180`-`\u024F`.

- **Latin 1 Supplement**

   Enables an extended character range subset of the Latin alphabet - completely represented by the official *Latin 1 Supplement* unicode range `\u0080` - `\u00FF`.


**NOTE!** ink files should be saved in UTF-8 format, which ensures that the above character ranges are supported.

If a particular character range that you would like to use within identifiers isn't supported, feel free to open an [issue](https://github.com/inkle/ink/issues/new) or [pull request](https://github.com/inkle/ink/pulls) on the main ink repo.
