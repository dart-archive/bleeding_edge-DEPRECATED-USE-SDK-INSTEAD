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
  int get length() => size;

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

  // Copy the instructions in this value set in [other] and returns
  // [other]. The copy is done by iterating through this value set
  // and calling [:other.add:].
  copyTo(var other) {
    for (int i = 0, length = table.length; i < length; i++) {
      ValueSetNode current = table[i];
      while (current !== null) {
        other.add(current.value);
        current = current.next;
      }
    }
    return other;
  }

  ValueSet copy() {
    return copyTo(new ValueSet());
  }

  ValueSet intersection(ValueSet other) {
    if (size > other.size) return other.intersection(this);
    ValueSet result = new ValueSet();
    for (int i = 0, length = table.length; i < length; i++) {
      ValueSetNode current = table[i];
      while (current !== null) {
        HInstruction value = current.value;
        if (other.lookup(value) != null) {
          result.add(value);
        }
        current = current.next;
      }
    }
    return result;
  }

  List<HInstruction> toList() {
    return copyTo(<HInstruction>[]);
  }

  // TODO(kasperl): Replace this with a proper hash based version.
  int tableIndexForInstruction(HInstruction instruction) => 0;
}

class ValueSetNode {
  final HInstruction value;
  ValueSetNode next;
  ValueSetNode(this.value, this.next);
}
