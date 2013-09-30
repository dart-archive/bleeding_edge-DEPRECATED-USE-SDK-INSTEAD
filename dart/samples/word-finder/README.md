# Word Finder

This sample app demonstrates the the use of HTML5 drag and drop in a Polymer
element.  Users are provided a set of characters that they can drag and drop
to form words.  A point is awarded for each character used in a word.

Objects are made draggable by setting the draggable attribute.

You can monitor the drag and drop process by attaching handlers for serveral
events. Here's a summary:

| Event        | Fired when                                                   |
| -------------|:------------------------------------------------------------:|
| dragstart   | a drag is started                                             |
| dragenter   | the mouse is _first_ moved over an element during a drag      |
| dragover    | mouse is moved over an element during a drag                  |
| dragleave   | the mouse leaves an element while a drag is occurring         |
| drop        | the drag operation ends (fired on element where drop occurred)|
| dragend     | drag operation is complete                                    |

This application uses the [Polymer.dart package][polymer-dart].

To run this code with Dartium, launch run `pub install` from the Tools menu of
Dart Editor. Then run `web/index.html`.

View the [source][source] for this example.

Please report any [bugs or feature requests][bugs].

[polymer-dart]: http://www.dartlang.org/polymer-dart/
[source]: https://code.google.com/p/dart/source/browse/branches/bleeding_edge/dart/samples/word-finder/
[bugs]: http://dartbug.com/new
