// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("client_dartc_test_config");

#import("dart:io");
#import("../../../tools/testing/dart/test_suite.dart");

class SamplesDartcTestSuite extends DartcCompilationTestSuite {
  SamplesDartcTestSuite(Map configuration)
      : super(configuration,
              'dartc',
              'samples',
              [ 'actors',
                'belay',
                'calculator',
                'chat',
                'clock',
                'dartcombat',
                'hi',
                'isolate',
                'isolate_html',
                'leap',
                'logo',
                'markdown',
                'matrix',
                'pond',
                'sample_extension',
                'slider',
                'spirodraw',
                'sunflower',
                'swarm',
                'tests',
                'third_party',
                'time',
                'ui_lib',
              ],
              ['samples/tests/dartc/dartc.status']);

  bool isTestFile(String filename) {
    if (!filename.endsWith(".dart")) return false;
    // Using readOptionsFromFile here causes the file to be read twice,
    // because readOptionsFromFile is called again in the superclass.
    // Avoid this in new code.
    return StandardTestSuite.readOptionsFromFile(new Path.fromNative(filename))
        ["containsLeadingHash"];
  }

  bool listRecursively() => true;
}
