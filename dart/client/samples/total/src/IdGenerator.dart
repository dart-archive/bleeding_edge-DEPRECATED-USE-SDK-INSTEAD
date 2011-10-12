// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/*
 * Object that returns an incrementing value that may be used as a unique id.
 */
class IdGenerator {
  int _id;

  // Construct a IdGenerator that will start with a value of 0
  IdGenerator() : _id = 0 {
  }

  // Return the next value in sequence
  int next() => _id++;
}
