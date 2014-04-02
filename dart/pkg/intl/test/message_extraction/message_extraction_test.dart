// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library message_extraction_test;

import 'package:unittest/unittest.dart';
import 'dart:io';
import 'dart:async';
import 'dart:convert';
import 'package:path/path.dart' as path;
import '../data_directory.dart';

final dart = Platform.executable;

/** The VM arguments we were given, most important package-root. */
final vmArgs = Platform.executableArguments;

var tempDir = Directory.systemTemp.createTempSync('message_extraction_test'
    ).path;

/**
 * Translate a relative file path into this test directory. This is
 * applied to all the arguments of [run]. It will ignore a string that
 * is an absolute path or begins with "--", because some of the arguments
 * might be command-line options.
 */
String asTestDirPath([String s]) {
  if (s == null || s.startsWith("--") || path.isAbsolute(s)) return s;
  return path.join(intlDirectory, 'test', 'message_extraction', s);
}

/**
 * Translate a relative file path into our temp directory. This is
 * applied to all the arguments of [run]. It will ignore a string that
 * is an absolute path or begins with "--", because some of the arguments
 * might be command-line options.
 */
String asTempDirPath([String s]) {
  if (s == null || s.startsWith("--") || path.isAbsolute(s)) return s;
  return path.join(tempDir, s);
}

main() {
  test("Test round trip message extraction, translation, code generation, "
      "and printing", () {
    copyFilesToTempDirectory();
    return extractMessages(null).then((result) {
      return generateTranslationFiles(result);
    }).then((result) {
      return generateCodeFromTranslation(result);
    }).then((result) => runAndVerify(result));
  });
}

void copyFilesToTempDirectory() {
  var files = [asTestDirPath('sample_with_messages.dart'), asTestDirPath(
      'part_of_sample_with_messages.dart'), asTestDirPath('verify_messages.dart'),
      asTestDirPath('run_and_verify.dart')];
  for (var filename in files) {
    var file = new File(filename);
    file.copySync(path.join(tempDir, path.basename(filename)));
  }
}

void deleteGeneratedFiles() {
  try {
    var dir = new Directory(tempDir);
    dir.listSync().forEach((x) => x.deleteSync());
    dir.deleteSync();
  } on Error catch (e) {
    print("Failed to delete $tempDir");
    print("Exception:\n$e");
  }
}

/**
 * Run the process with the given list of filenames, which we assume
 * are in dir() and need to be qualified in case that's not our working
 * directory.
 */
Future<ProcessResult> run(ProcessResult previousResult, List<String> filenames)
    {
  // If there's a failure in one of the sub-programs, print its output.
  if (previousResult != null) {
    if (previousResult.exitCode != 0) {
      print("Error running sub-program:");
    }
    print(previousResult.stdout);
    print(previousResult.stderr);
    print("exitCode=${previousResult.exitCode}");
  }
  var filesInTheRightDirectory = filenames.map((x) => asTempDirPath(x)).toList(
      );
  // Inject the script argument --output-dir in between the script and its
  // arguments.
  var args = []
      ..addAll(vmArgs)
      ..add(filesInTheRightDirectory.first)
      ..addAll(["--output-dir=$tempDir"])
      ..addAll(filesInTheRightDirectory.skip(1));
  var result = Process.run(dart, args, stdoutEncoding: UTF8, stderrEncoding:
      UTF8);
  return result;
}

Future<ProcessResult> extractMessages(ProcessResult previousResult) => run(
    previousResult, [asTestDirPath('extract_to_json.dart'),
    '--suppress-warnings', 'sample_with_messages.dart',
    'part_of_sample_with_messages.dart']);

Future<ProcessResult> generateTranslationFiles(ProcessResult previousResult) =>
    run(previousResult,
        [asTestDirPath('make_hardcoded_translation.dart'),
        'intl_messages.json']);

Future<ProcessResult> generateCodeFromTranslation(ProcessResult previousResult)
    => run(previousResult, [asTestDirPath('generate_from_json.dart'),
    '--generated-file-prefix=foo_', 'sample_with_messages.dart',
    'part_of_sample_with_messages.dart', 'translation_fr.json',
    'translation_de_DE.json']);

Future<ProcessResult> runAndVerify(ProcessResult previousResult) => run(
    previousResult, [asTempDirPath('run_and_verify.dart')]);
