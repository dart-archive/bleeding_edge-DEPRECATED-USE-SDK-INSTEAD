// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// File imported by browser_perf_testing.dart controller page to determine when
/// a performance test is done. This is specific to each performance test.

// The function that does the test to determine if this performance test is
// completed, given a String containing the event queue (most recent event is
// last).
function testIsComplete(message) {
  return message.indexOf('alldone') != -1;
}
