// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** General options used by the compiler. */
FrogOptions options;

/** Extracts options from command-line arguments. */
void parseOptions(String homedir, List<String> args, FileSystem files) {
  assert(options == null);
  options = new FrogOptions(homedir, args, files);
}

// TODO(sigmund): make into a generic option parser...
class FrogOptions {
  /** Location of corelib and other special dart libraries. */
  String libDir;

  /* The top-level dart script to compile. */
  String dartScript;

  /** Where to place the generated code. */
  String outfile;

  // Options that modify behavior significantly
  bool enableLeg = false;
  bool legOnly = false;
  bool enableAsserts = false;
  bool enableTypeChecks = false;
  bool verifyImplements = false; // TODO(jimhug): Implement
  bool compileAll = false; // TODO(jimhug): Implement
  bool dietParse = false;
  bool compileOnly = false;

  // Message support
  bool throwOnErrors = false;
  bool throwOnWarnings = false;
  bool throwOnFatal = false;
  bool showInfo = false;
  bool showWarnings = true;

  /**
   * Options to be used later for passing to the generated code. These are all
   * the arguments after the first dart script, if any.
   */
  List<String> childArgs;

  FrogOptions(String homedir, List<String> args, FileSystem files) {
    libDir = homedir + '/lib'; // Default value for --libdir.
    bool ignoreUnrecognizedFlags = false;
    bool passedLibDir = false;
    childArgs = [];

    // Start from 2 to skip arguments representing the compiler command
    // (node/python followed by frogsh/frog.py).
    // TODO(jimhug): break on switch cases seems broken?
    loop: for (int i = 2; i < args.length; i++) {
      var arg = args[i];

      switch (arg) {
        case '--enable_leg':
          enableLeg = true;
          continue loop;

        case '--leg_only':
          enableLeg = true;
          legOnly = true;
          continue loop;

        case '--enable_asserts':
          enableAsserts = true;
          continue loop;

        case '--enable_type_checks':
          enableTypeChecks = true;
          // This flag also enables asserts in VM
          enableAsserts = true;
          continue loop;

        case '--verify_implements':
          verifyImplements = true;
          continue loop;

        case '--compile_all':
          compileAll = true;
          continue loop;

        case '--diet-parse':
          dietParse = true;
          continue loop;

        case '--ignore-unrecognized-flags':
          ignoreUnrecognizedFlags = true;
          continue loop;

        case '--verbose':
          showInfo = true;
          continue loop;

        case '--suppress_warnings':
          showWarnings = false;
          continue loop;

        case '--throw_on_errors':
          throwOnErrors = true;
          continue loop;

        case '--throw_on_warnings':
          throwOnWarnings = true;
          continue loop;

        case '--compile-only':
          // As opposed to compiling and running, the default behavior.
          compileOnly = true;
          continue loop;

        default:
          if (arg.endsWith('.dart')) {
            dartScript = arg;
            childArgs = args.getRange(i + 1, args.length - i - 1);
            break loop;
          } else if (arg.startsWith('--out=')) {
            outfile = arg.substring('--out='.length);
          } else if (arg.startsWith('--libdir=')) {
            libDir = arg.substring('--libdir='.length);
            passedLibDir = true;
          } else {
            if (!ignoreUnrecognizedFlags) {
              print('unrecognized flag: "$arg"');
            }
          }
      }
    }

    if (!passedLibDir && !files.fileExists(libDir)) {
      // Try locally
      var temp = 'frog/lib';
      if (files.fileExists(temp)) {
        libDir = temp;
      } else {
        libDir = 'lib';
      }
    }
  }
}
