Dart JavaScript Interop
===================

The js.dart library allows Dart code running in the browser to
manipulate JavaScript running in the same page.  It is intended to
allow Dart code to easily interact with third-party JavaScript libraries.

Documentation
-------------

See [API documentation][docs]. You should also watch this [video tutorial][video].

Samples
-------

See [samples][samples] that demonstrate interaction with JavaScript
code.  These include interoperation with the Google Maps JavaScript
library, the Google Visualization JavaScript library, and Twitter's
query API via JSONP.

Usage
-----

The [Dart Editor][editor] now includes pub support.  To try out this
library in the editor:

1.  [Update to the latest editor][editor].

2.  From the "File" menu, open a "New Application" (and make sure "Add
        Pub support" is checked).

3.  Add the following to your pubspec.yaml:

        dependencies:
          js: any


4.  Under the "Tools" menu, run "Pub Install".

5.  Try the following test Dart file:

        import 'package:js/js.dart' as js;
        
        void main() {
          js.context.alert('Hello from Dart via JS');
        }
        
6.  Add the script to your HTML page:

        <script src="packages/browser/dart.js"></script>
        <script src="packages/browser/interop.js"></script> 

Running Tests
-------------

First, use the [Pub Package Manager][pub] to install dependencies:

    pub install

To run browser tests on [Dartium], simply open **test/browser_tests.html**
in Dartium.

To run browser tests using JavaScript in any modern browser, first use the
following command to compile to JavaScript:

    dart2js -otest/browser_tests.dart.js test/browser_tests.dart

and then open **test/browser_tests.html** in any browser.

[d]: http://www.dartlang.org
[mb]: http://www.dartlang.org/support/faq.html#what-browsers-supported
[pub]: http://www.dartlang.org/docs/pub-package-manager/
[Dartium]: http://www.dartlang.org/dartium/index.html
[docs]: http://dart-lang.github.com/js-interop
[samples]: http://dart-lang.github.com/js-interop/example
[editor]: http://www.dartlang.org/docs/editor/getting-started/
[video]: http://www.youtube.com/watch?v=QFuCFUd2Zsw
