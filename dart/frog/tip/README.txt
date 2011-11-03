This is a rough experiment at more deeply integrating dart into a browser
in order to simplify the dev experience.  It currently provides a simple
playground for interactively messing around with dart code and an extension
to chrome to automatically compile and run .dart files.  The goal is to
provide the infrastructure to build browser extensions to chrome and other
easily extensible browsers (such as FireFox) that will make it very easy to
experiment with code and later full pages that incorporate the dart language.

KEY NOTE: To use this code, you have to do a copy of the core libraries into
frog/tip/lib.  This is needed to make the files available on the right path for
both the chrome extension and the standalone html. Use the 'copy_libs.py' script
for this purpose.
TODO(jimhug): This step sucks.

> cd $FROG/tip
> python copy_libs.py

Next, you need to build compiler.js by hand - bootstapping, ya'know.
> ./frogsh --out=tip/compiler.js --compile-only tip/compiler.dart

You can run against your local file copy now, but you'll need to run
chrome with --allow-file-access-from-files.  This is not needed when
serving via http.  (The flag above technically opens a security hole.)

I recommend skipping this step and just using the chrome extension; however,
if you'd like to go this route here's how I start chrome with this flag on
my Mac:
> cd $DART
> Chrome.app/Contents/MacOS/Chromium --allow-file-access-from-files

Running as a chrome extension:

1. In your browser, go to chrome://extensions/
2. Click on <Load Unpacked Extension...> and point it at frog/tip.
3. You're done!  Click on reload to update the extension if you make changes.

With the extension, whenever you open a new tab, you should see the Dart Tip
icon that will take you to the playground.  In addition, if you try to load
a .dart file from the filesystem it will compile and execute it.  Good
candidates to try this on are frog/tests/canvastest.dart, or htmltest.dart.

To package and ship the extension.
1. From chrome://extensions/ click on <Pack extension...> and again point
   it to frog/tip.  You'll now get a nicely self-contained tip.crx.
TODO(jimhug): Signing and updating of this extension needs to be worked out.
