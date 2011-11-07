// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(kasperl): This is a very naive non-hashed implementation of a
// value set for global value numbering. Should be optimized.
class ValueSet {
  List<HInstruction> instructions;
  ValueSet() : instructions = new List<HInstruction>();

  void add(HInstruction instruction) {
    assert(lookup(instruction) === null);
    instructions.add(instruction);
  }

  HInstruction lookup(HInstruction instruction) {
    int index = instructions.indexOf(instruction);
    return (index >= 0) ? instructions[index] : null;
  }

  void kill(int flags) {
    int depends = HInstruction.computeDependsOnFlags(flags);
    instructions = instructions.filter((e) => (e.flags & depends) == 0);
  }
}
