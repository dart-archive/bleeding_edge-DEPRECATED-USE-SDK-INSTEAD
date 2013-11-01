csslib [![Build Status](https://drone.io/github.com/dart-lang/csslib/status.png)](https://drone.io/github.com/dart-lang/csslib/latest)
======

This is a CSS Parser/Pre-processor written entirely in [Dart][dart].  
Because of that you can use it safely from a script or server side app.



Installation
------------

Add this to your `pubspec.yaml` (or create it):
```yaml
dependencies:
  csslib: any
```
Then run the [Pub Package Manager][pub] (comes with the Dart SDK):

    pub get

Usage
-----

Parsing CSS is easy!
```dart
import 'package:csslib/parser.dart' show compile;
import 'package:csslib/parser.dart';
import 'package:csslib/visitor.dart';

main() {
  var srcContents = new File('scss/myStyles.scss').readAsStringSync();
    
  var ast = compile(srcContents);
	
  var printer = new CssPrinter();
  printer.visitTree(ast, pretty: true);
	
  var outputFile = new File('css/myStyles.css').openSync(mode: FileMode.WRITE);
  outputFile
	..writeStringSync(printer.toString())
	..closeSync();
}
```

You can pass a String or list of bytes to `parse`.


Updating
--------

You can upgrade the library with:

    pub upgrade

Disclaimer: the APIs are not finished. Updating may break your code. If that
happens, you can check the
[commit log](https://github.com/dart-lang/csslib/commits/master), to figure
out what the change was.

If you want to avoid breakage, you can also put the version constraint in your
`pubspec.yaml` in place of the word `any`.

Running Tests
-------------

All tests (both canary and suite) should be passing.  Canary are quick test
verifies that basic CSS is working.  The suite tests are a comprehensive set of
~11,000 tests.

```bash
export DART_SDK=path/to/dart/sdk

# Make sure dependencies are installed
pub get

# Run command both canary and the suite tests
test/run.sh
```

  Run only the canary test:

```bash
 test/run.sh canary
```

  Run only the suite tests:

```bash
 test/run.sh suite
```

[dart]: http://www.dartlang.org/
[pub]: http://www.dartlang.org/docs/pub-package-manager/
