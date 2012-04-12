// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeTestHelper');

#import('../../../../lib/unittest/unittest.dart');
#import('../../../lib/node/node.dart');

/**
 * Helper method to be able to run the test from:
 * top-level-directory
 * frog
 * frog/tests/frog/src
 */

String getFilename(String pathName) {
  for (var prefix in ['', 'tests/frog/src/', 'frog/', 'frog/tests/frog/src/']) {
    var testPath = prefix + pathName;
    if (path.existsSync(testPath)) {
      return testPath;
    }
  }
  throw new Exception('file not found: ' + pathName);
}

/**
 * Runs a dart program using the same version of node and frogsh that
 * is running this test, collects the results, and calls a callback function.
 * callback (Error error, String stdout, String stderr)
 */
ChildProcess runDartProgram(String program, List<String> argv, String stdinText,
    Child_processCallback callback) {
  final sb = new StringBuffer();
  sb.add('"${process.execPath}"');
  sb.add(' "${process.argv[1]}"');
  sb.add(' "$program"');
  for (var arg in argv) {
    sb.add(' $arg');
  }
  // print('exec ${sb.toString()}');
  final child = child_process.exec(sb.toString(), callback);
  if (stdinText != null) {
    // Something I don't understand:
    // can't call 'child.stdin.end() directly, have
    // to store child.stdin in a WritableStream variable. 'var' won't do.
    WritableStream stdin = child.stdin;
    stdin.end(stdinText);
  }
  return child;
}

/**
 * Expects that the string [actual] matches the regex [expected].
 */
void regexCompare(String expected, String actual){
  if (expected != null) {
    final exp = new RegExp(expected, true);
    bool hasMatch = exp.hasMatch(actual);
    if (!hasMatch) {
      print('"${expected}" does not match "$actual"');
    }
    Expect.isTrue(hasMatch);
  }
}

/**
 * Expects that either [expected] and [actual] are both null,
 * or the string [actual] matches the regex [expected].
 */
void regexCompareError(String expected, Error error){
  if (expected == null) {
    Expect.isNull(error);
  } else {
    Expect.isNotNull(error);
    regexCompare(expected, error.message);
  }
}

/**
 * [callback] must call callbackDone() when done.
 */
void asyncTestWithHelperProgram(String testName, String helperProgram,
    List<String> argv, String stdin, Child_processCallback callback) {
  asyncTest(testName, 1, () {
      runDartProgram( helperProgram, argv, stdin, callback);
  });
}

String _nodeTestOutputBanner = 'Start of Node test output:';

void printTestOutputBanner() {
  print(_nodeTestOutputBanner);
}

// Trim away compiler warning messages from test output.

String trimJunk(String s) {
  int index = s.indexOf(_nodeTestOutputBanner, 0);
  if (index >= 0) {
    // The "+ 1" below is for the '\n' character that was appended when the
    // banner was printed out.
    return s.substring(index + _nodeTestOutputBanner.length + 1, s.length);
  }
  throw new Exception('Did not find test output banner.');
}

void asyncSimpleTestWithHelperProgram(String testName, String helperProgram,
    List<String> argv, String stdin, String expectedStdout,
    String expectedStderr) {
  asyncTestWithHelperProgram(testName, helperProgram, argv, stdin,
      (Error error, String stdout, String stderr) {
        Expect.isNull(error);
        Expect.equals(expectedStdout, trimJunk(stdout));
        Expect.equals(expectedStderr, stderr);
        callbackDone();
  });
}

void asyncFuzzyTestWithHelperProgram(String testName, String helperProgram,
    List<String> argv, String stdin, String expectedError,
    String expectedStdout, String expectedStderr) {
  asyncTestWithHelperProgram(testName, helperProgram, argv, stdin,
      (Error error, String stdout, String stderr) {
        regexCompareError(expectedError, error);
        regexCompare(expectedStdout, trimJunk(stdout));
        regexCompare(expectedStderr, stderr);
        callbackDone();
  });
}

void asyncTestExitCode(String testName, String helperProgram,
    List<String> argv, int expectedExitCode) {
  asyncTest(testName, 2, () {
      int exitCode = null;
      final child = runDartProgram(helperProgram, argv, null,
          (error, stdout, stderr) {
            Expect.equals(expectedExitCode == 0, error == null);
            callbackDone();
      });
      child.onExit((int exitCode, String signal) {
            Expect.equals(expectedExitCode, exitCode);
            callbackDone();
      });
  });
}

/**
 * List entry is name-of-test, argument-to-child, stdin, expected-stdout,
 * expected-stderr
 */
void asyncSimpleTests(String helperProgram, List< List<String> > tests) {
  for (var test in tests) {
    asyncSimpleTestWithHelperProgram(test[0], helperProgram,
        [test[1]], test[2], test[3], test[4]);
  }
}

/**
 * List entry is name-of-test, argument-to-child, stdin, expected-error,
 * expected-stdout, expected-stderr
 */
void asyncFuzzyTests(String helperProgram, List<List<String>> tests) {
  for (var test in tests) {
    asyncFuzzyTestWithHelperProgram(test[0], helperProgram,
        [test[1]], test[2], test[3], test[4], test[5]);
  }
}
