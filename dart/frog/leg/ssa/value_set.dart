// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(kasperl): This is a very naive non-hashed implementation of a
// value set for global value numbering. Should be optimized.
class ValueSet {
  int size = 0;
  List<ValueSetNode> table;
  ValueSet() : table = new List<ValueSetNode>(1);

  bool isEmpty() => size == 0;

  void add(HInstruction instruction) {
    assert(lookup(instruction) === null);
    int index = tableIndexForInstruction(instruction);
    table[index] = new ValueSetNode(instruction, table[index]);
    size++;
  }

  HInstruction lookup(HInstruction instruction) {
    int index = tableIndexForInstruction(instruction);
    for (ValueSetNode node = table[index]; node !== null; node = node.next) {
      HInstruction cached = node.value;
      if (cached.equals(instruction)) return cached;
    }
    return null;
  }

  void kill(int flags) {
    int depends = HInstruction.computeDependsOnFlags(flags);
    for (int i = 0, length = table.length; i < length; i++) {
      ValueSetNode previous = null;
      ValueSetNode current = table[i];
      while (current !== null) {
        ValueSetNode next = current.next;
        HInstruction cached = current.value;
        if ((cached.flags & depends) != 0) {
          if (previous === null) {
            table[i] = next;
          } else {
            previous.next = next;
          }
          size--;
        } else {
          previous = current;
        }
        current = next;
      }
    }
  }

  ValueSet copy() {
    ValueSet result = new ValueSet();
    for (int i = 0, length = table.length; i < length; i++) {
      ValueSetNode current = table[i];
      while (current !== null) {
        result.add(current.value);
        current = current.next;
      }
    }
    return result;
  }

  // TODO(kasperl): Replace this with a proper hash based version.
  int tableIndexForInstruction(HInstruction instruction) => 0;
}

class ValueSetNode {
  final HInstruction value;
  ValueSetNode next;
  ValueSetNode(this.value, this.next);
}
