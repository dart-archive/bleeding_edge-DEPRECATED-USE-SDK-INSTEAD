Frog is a dart compiler implemented in dart.  It is currently focused on
compiling dart code to efficient and readable javascript code.  However, it
is ultimately intended to be the basis for future code analysis tools and
development environments when these can be built in dart as well.  Two
very early examples of this kind of use are in the samples directory:
  * doc.dart - minimal documentation generator
  * ifrog.dart - minimal command-line REPL for dart



To use frog:

1) Frog is now included in the checkout of the public repo of the bleeding_edge
   branch. The code will be located under dart/frog.

2) From the dart/frog directory, run the presubmit script to check your
   installation is all working.
     frog$ ./presubmit.py


BEFORE YOU SUBMIT

Before you submit, you should:

1) Run the html tests:

  $ ./frog.py --html -- tests/htmltest.dart
  $ ./frog.py --html -- tests/canvastest.dart

   These will open a browser -- verify in the console that no errors
   happened.

2) Run the presubmit script:

  $ ./presubmit.py

Note:

If you make any changes to files under tests, you are expected to ensure that
you don't break any of the other configurations.  If you only change the status of tests for frog, you should be okay.  However, if you
modify any of the tests themselves, please run the tests with both vm and
dartc configurations before checking in.


Details:

You can use build.py and test.py (that are run by the presubmit script):

  $ ../tools/build.py --mode=release,debug
  $ ../tools/test.py --report -t 5 -p color --mode=release --component=frog language corelib leg

The 'frog' component is frog running on the VM.
