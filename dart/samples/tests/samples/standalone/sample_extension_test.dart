// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// Dart test program for testing native extensions.

import "package:expect/expect.dart";
import 'dart:async';
import 'dart:io';
import 'dart:isolate';

Future copyFileToDirectory(Path file, Path directory) {
  String src = file.toNativePath();
  String dst = directory.toNativePath();
  switch (Platform.operatingSystem) {
    case 'linux':
    case 'macos':
      return Process.run('cp', [src, dst]);
    case 'windows':
      return Process.run('cmd.exe', ['/C', 'copy $src $dst']);
    default:
      Expect.fail('Unknown operating system ${Platform.operatingSystem}');
  }
}

Path getNativeLibraryPath(Path buildDirectory) {
  switch (Platform.operatingSystem) {
    case 'linux':
      return buildDirectory.append('lib.target/libsample_extension.so');
    case 'macos':
      return buildDirectory.append('libsample_extension.dylib');
    case 'windows':
      return buildDirectory.append('sample_extension.dll');
    default:
      Expect.fail('Unknown operating system ${Platform.operatingSystem}');
  }
}

void main() {
  Path scriptDirectory = new Path(Platform.script).directoryPath;
  Path buildDirectory = new Path(Platform.executable).directoryPath;
  Directory tempDirectory = new Directory('').createTempSync();
  Path testDirectory = new Path(tempDirectory.path);
  Path sourceDirectory = scriptDirectory.append('../../../sample_extension');

  // Copy sample_extension shared library, sample_extension dart files and
  // sample_extension tests to the temporary test directory.
  copyFileToDirectory(getNativeLibraryPath(buildDirectory), testDirectory)
  .then((_) => Future.forEach(['sample_synchronous_extension.dart',
                               'sample_asynchronous_extension.dart',
                               'test_sample_synchronous_extension.dart',
                               'test_sample_asynchronous_extension.dart'],
    (file) => copyFileToDirectory(sourceDirectory.append(file), testDirectory)
  ))

  .then((_) => Future.forEach(['test_sample_synchronous_extension.dart',
                               'test_sample_asynchronous_extension.dart'],
    (test) => Process.run(Platform.executable,
                          [testDirectory.append(test).toNativePath()])
    .then((ProcessResult result) {
      if (result.exitCode != 0) {
        print('Failing test: ${sourceDirectory.append(test).toNativePath()}');
        print('Failing process stdout: ${result.stdout}');
        print('Failing process stderr: ${result.stderr}');
        print('End failing process stderr');
        Expect.fail('Test failed with exit code ${result.exitCode}');
      }
    })
  ))
  .whenComplete(() => tempDirectory.deleteSync(recursive: true));
}
