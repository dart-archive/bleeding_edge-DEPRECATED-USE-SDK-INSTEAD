
Limitations:

1. Right now only syntax translation, no semantics.

2. No support for body in "this" redirecting constructor.

3. Support for enum is not finished - no ordinal, values(), toString(), etc.
   May be generate "static const" and "const" constructor.

4. Need to rename overloaded methods.

5. Need to rename methods and fields with the same name.
   May be eliminate method if it is getter of final field.

6. Need to rename overloaded constructors.

7. Need to move to initializers or formal initializers assignment to final fields in constructors.

