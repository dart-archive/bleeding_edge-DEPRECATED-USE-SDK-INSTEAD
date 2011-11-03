To use frog:

1) Frog is now included in the checkout of the public repo of the bleeding_edge
   branch. The code will be located under $DART/frog.

   Note that $DART/frog/ is a separate dependency in 'all.deps/DEPS', for this
   reason changes in the frog directory will have to be submitted separately
   from changes to the rest of the dart repo.

   (git users): If you'd prefer to use git, you can do the following to have a
   git setup for the frog subdirectory:
  $ rm -rf $DART/frog/
  $ cd $DART
  $ git svn clone -rHEAD https://dart.googlecode.com/svn/experimental/frog frog

2) Make sure you have 'node' in your path (http://nodejs.org/, and
   https://github.com/joyent/node/wiki/Installation for how to install)


BEFORE YOU SUBMIT


Before you submit, you should:

1) Run the html tests:

  $ ./frog.py --html -- tests/htmltest.dart
  $ ./frog.py --html -- tests/canvastest.dart

   These will open a browser -- verify in the console that no errors
   happened.

2) Run the presubmit script:

  $ ./presubmit.py

3) Make sure to include any changes to frogsh in your commit.



Details:


You can use build.py and test.py (that are run by the presubmit script):

  $ ../tools/build.py --mode=release,debug
  $ ../tools/test.py --report -t 5 -p color --mode=release,debug frog

The 'release' version of frog is the self-hosted frog.
The 'debug' version of frog is frog running on the VM.

To build the self-hosted compiler called frogsh (for frog self-hosted), run:

  $ ./frog.py --js_out=frogsh -- frog.dart

(this will make frogsh the same as
$DART/frog/$OUTDIR_PREFIX/Release_ia32/dart_bin)

To run the self-hosted compiler, you can just type frogsh.

  $ ./frogsh tests/hello.dart
This should print 'hello world'.
