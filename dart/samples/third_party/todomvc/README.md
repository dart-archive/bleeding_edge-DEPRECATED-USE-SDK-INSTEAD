# Polymer TodoMVC Example

> Build structured, encapsulated, client-side web apps with Dart and web components.

> _[Polymer.dart](https://www.dartlang.org/polymer-dart/)_

> Polymer is a new type of library for the web, built on top of Web Components, and designed to leverage the evolving web platform on modern browsers.

> _[Polymer - www.polymer-project.org](http://www.polymer-project.org/)_

## Learning Polymer

The [Polymer.dart website](https://www.dartlang.org/polymer-dart/) is a great resource for getting started.

Get help from Polymer devs and users:

* Join the high-traffic [web-ui](https://groups.google.com/a/dartlang.org/forum/#!forum/web-ui) Google group.
* As questions on [Stack Overflow](http://stackoverflow.com/tags/dart-polymer)

## Implementation

The Polymer implementation of TodoMVC has a few key differences with other implementations:

* Since [Web Components](https://dvcs.w3.org/hg/webcomponents/raw-file/tip/explainer/index.html) allow you to create new types of DOM elements, the DOM tree is very different from other implementations.
* The template, styling, and behavior are fully encapsulated in each custom element. Instead of having an overall stylesheet (`base.css` or `app.css`), each element that needs styling has its own stylesheet.
* Non-visual elements such as the router and the model are also implemented as custom elements and appear in the DOM. Implementing them as custom elements instead of plain objects allows you to take advantage of Polymer data binding and event handling throughout the app.

## Running this sample

To run in Dartium (Chrome with Dart VM):

1. Right click on web/index.html and choose "Run in Dartium"

To run in other browsers (such as Firefox, Internet Explorer, Safari, Chrome):

1. Right click on pubspec.yaml and choose "Pub Build"
2. After build finishes, expand the "build" directory
3. Right click on index.html and choose "Run as JavaScript"
