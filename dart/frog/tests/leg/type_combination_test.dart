// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#source("../../../lib/compiler/implementation/ssa/types.dart");

class Type {
  const Type(this.str);
  final String str;
}

final CONFLICTING = HType.CONFLICTING;
final UNKNOWN = HType.UNKNOWN;
final BOOLEAN = HType.BOOLEAN;
final NUMBER = HType.NUMBER;
final INTEGER = HType.INTEGER;
final DOUBLE = HType.DOUBLE;
final INDEXABLE_PRIMITIVE = HType.INDEXABLE_PRIMITIVE;
final STRING = HType.STRING;
final READABLE_ARRAY = HType.READABLE_ARRAY;
final MUTABLE_ARRAY = HType.MUTABLE_ARRAY;
final EXTENDABLE_ARRAY = HType.EXTENDABLE_ARRAY;
final NON_PRIMITIVE1 = const HBoundedType(const Type("type1"));
final NON_PRIMITIVE2 = const HBoundedType(const Type("type2"));

void testUnion() {
  Expect.equals(CONFLICTING, CONFLICTING.union(CONFLICTING));
  Expect.equals(CONFLICTING, CONFLICTING.union(UNKNOWN));
  Expect.equals(CONFLICTING, CONFLICTING.union(BOOLEAN));
  Expect.equals(CONFLICTING, CONFLICTING.union(NUMBER));
  Expect.equals(CONFLICTING, CONFLICTING.union(INTEGER));
  Expect.equals(CONFLICTING, CONFLICTING.union(DOUBLE));
  Expect.equals(CONFLICTING, CONFLICTING.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, CONFLICTING.union(STRING));
  Expect.equals(CONFLICTING, CONFLICTING.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, CONFLICTING.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, UNKNOWN.union(CONFLICTING));
  Expect.equals(UNKNOWN, UNKNOWN.union(UNKNOWN));
  Expect.equals(BOOLEAN, UNKNOWN.union(BOOLEAN));
  Expect.equals(NUMBER, UNKNOWN.union(NUMBER));
  Expect.equals(INTEGER, UNKNOWN.union(INTEGER));
  Expect.equals(DOUBLE, UNKNOWN.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, UNKNOWN.union(INDEXABLE_PRIMITIVE));
  Expect.equals(STRING, UNKNOWN.union(STRING));
  Expect.equals(READABLE_ARRAY, UNKNOWN.union(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, UNKNOWN.union(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, UNKNOWN.union(EXTENDABLE_ARRAY));
  Expect.equals(NON_PRIMITIVE1, UNKNOWN.union(NON_PRIMITIVE1));
  Expect.equals(NON_PRIMITIVE2, UNKNOWN.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, BOOLEAN.union(CONFLICTING));
  Expect.equals(BOOLEAN, BOOLEAN.union(UNKNOWN));
  Expect.equals(BOOLEAN, BOOLEAN.union(BOOLEAN));
  Expect.equals(CONFLICTING, BOOLEAN.union(NUMBER));
  Expect.equals(CONFLICTING, BOOLEAN.union(INTEGER));
  Expect.equals(CONFLICTING, BOOLEAN.union(DOUBLE));
  Expect.equals(CONFLICTING, BOOLEAN.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, BOOLEAN.union(STRING));
  Expect.equals(CONFLICTING, BOOLEAN.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, BOOLEAN.union(NON_PRIMITIVE2));
  
  Expect.equals(CONFLICTING, NUMBER.union(CONFLICTING));
  Expect.equals(NUMBER, NUMBER.union(UNKNOWN));
  Expect.equals(CONFLICTING, NUMBER.union(BOOLEAN));
  Expect.equals(NUMBER, NUMBER.union(NUMBER));
  Expect.equals(NUMBER, NUMBER.union(INTEGER));
  Expect.equals(NUMBER, NUMBER.union(DOUBLE));
  Expect.equals(CONFLICTING, NUMBER.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NUMBER.union(STRING));
  Expect.equals(CONFLICTING, NUMBER.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, NUMBER.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, INTEGER.union(CONFLICTING));
  Expect.equals(INTEGER, INTEGER.union(UNKNOWN));
  Expect.equals(CONFLICTING, INTEGER.union(BOOLEAN));
  Expect.equals(NUMBER, INTEGER.union(NUMBER));
  Expect.equals(INTEGER, INTEGER.union(INTEGER));
  Expect.equals(NUMBER, INTEGER.union(DOUBLE));
  Expect.equals(CONFLICTING, INTEGER.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, INTEGER.union(STRING));
  Expect.equals(CONFLICTING, INTEGER.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, INTEGER.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, DOUBLE.union(CONFLICTING));
  Expect.equals(DOUBLE, DOUBLE.union(UNKNOWN));
  Expect.equals(CONFLICTING, DOUBLE.union(BOOLEAN));
  Expect.equals(NUMBER, DOUBLE.union(NUMBER));
  Expect.equals(NUMBER, DOUBLE.union(INTEGER));
  Expect.equals(DOUBLE, DOUBLE.union(DOUBLE));
  Expect.equals(CONFLICTING, DOUBLE.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, DOUBLE.union(STRING));
  Expect.equals(CONFLICTING, DOUBLE.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, DOUBLE.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(CONFLICTING));
  Expect.equals(INDEXABLE_PRIMITIVE, INDEXABLE_PRIMITIVE.union(UNKNOWN));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(BOOLEAN));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(NUMBER));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(INTEGER));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE,
                INDEXABLE_PRIMITIVE.union(INDEXABLE_PRIMITIVE));
  Expect.equals(INDEXABLE_PRIMITIVE, INDEXABLE_PRIMITIVE.union(STRING));
  Expect.equals(INDEXABLE_PRIMITIVE, INDEXABLE_PRIMITIVE.union(READABLE_ARRAY));
  Expect.equals(INDEXABLE_PRIMITIVE, INDEXABLE_PRIMITIVE.union(MUTABLE_ARRAY));
  Expect.equals(INDEXABLE_PRIMITIVE,
                INDEXABLE_PRIMITIVE.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, STRING.union(CONFLICTING));
  Expect.equals(STRING, STRING.union(UNKNOWN));
  Expect.equals(CONFLICTING, STRING.union(BOOLEAN));
  Expect.equals(CONFLICTING, STRING.union(NUMBER));
  Expect.equals(CONFLICTING, STRING.union(INTEGER));
  Expect.equals(CONFLICTING, STRING.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, STRING.union(INDEXABLE_PRIMITIVE));
  Expect.equals(STRING, STRING.union(STRING));
  Expect.equals(INDEXABLE_PRIMITIVE, STRING.union(READABLE_ARRAY));
  Expect.equals(INDEXABLE_PRIMITIVE, STRING.union(MUTABLE_ARRAY));
  Expect.equals(INDEXABLE_PRIMITIVE, STRING.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, STRING.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, STRING.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, READABLE_ARRAY.union(CONFLICTING));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.union(UNKNOWN));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(BOOLEAN));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(NUMBER));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(INTEGER));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, READABLE_ARRAY.union(INDEXABLE_PRIMITIVE));
  Expect.equals(INDEXABLE_PRIMITIVE, READABLE_ARRAY.union(STRING));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.union(READABLE_ARRAY));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.union(MUTABLE_ARRAY));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, READABLE_ARRAY.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(CONFLICTING));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.union(UNKNOWN));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(BOOLEAN));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(NUMBER));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(INTEGER));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, MUTABLE_ARRAY.union(INDEXABLE_PRIMITIVE));
  Expect.equals(INDEXABLE_PRIMITIVE, MUTABLE_ARRAY.union(STRING));
  Expect.equals(READABLE_ARRAY, MUTABLE_ARRAY.union(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.union(MUTABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(CONFLICTING));
  Expect.equals(EXTENDABLE_ARRAY, EXTENDABLE_ARRAY.union(UNKNOWN));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(BOOLEAN));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(NUMBER));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(INTEGER));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, EXTENDABLE_ARRAY.union(INDEXABLE_PRIMITIVE));
  Expect.equals(INDEXABLE_PRIMITIVE, EXTENDABLE_ARRAY.union(STRING));
  Expect.equals(READABLE_ARRAY, EXTENDABLE_ARRAY.union(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, EXTENDABLE_ARRAY.union(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, EXTENDABLE_ARRAY.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(CONFLICTING));
  Expect.equals(NON_PRIMITIVE1, NON_PRIMITIVE1.union(UNKNOWN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(BOOLEAN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(NUMBER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(INTEGER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(DOUBLE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(STRING));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(EXTENDABLE_ARRAY));
  Expect.equals(NON_PRIMITIVE1, NON_PRIMITIVE1.union(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.union(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(CONFLICTING));
  Expect.equals(NON_PRIMITIVE2, NON_PRIMITIVE2.union(UNKNOWN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(BOOLEAN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(NUMBER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(INTEGER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(DOUBLE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(STRING));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.union(NON_PRIMITIVE1));
  Expect.equals(NON_PRIMITIVE2, NON_PRIMITIVE2.union(NON_PRIMITIVE2));
}

void testIntersection() {
  Expect.equals(CONFLICTING, CONFLICTING.intersection(CONFLICTING));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(NUMBER));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(INTEGER));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(DOUBLE));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(STRING));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, CONFLICTING.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, UNKNOWN.intersection(CONFLICTING));
  Expect.equals(UNKNOWN, UNKNOWN.intersection(UNKNOWN));
  Expect.equals(BOOLEAN, UNKNOWN.intersection(BOOLEAN));
  Expect.equals(NUMBER, UNKNOWN.intersection(NUMBER));
  Expect.equals(INTEGER, UNKNOWN.intersection(INTEGER));
  Expect.equals(DOUBLE, UNKNOWN.intersection(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE, UNKNOWN.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(STRING, UNKNOWN.intersection(STRING));
  Expect.equals(READABLE_ARRAY, UNKNOWN.intersection(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, UNKNOWN.intersection(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, UNKNOWN.intersection(EXTENDABLE_ARRAY));
  Expect.equals(NON_PRIMITIVE1, UNKNOWN.intersection(NON_PRIMITIVE1));
  Expect.equals(NON_PRIMITIVE2, UNKNOWN.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, BOOLEAN.intersection(CONFLICTING));
  Expect.equals(BOOLEAN, BOOLEAN.intersection(UNKNOWN));
  Expect.equals(BOOLEAN, BOOLEAN.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(NUMBER));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(INTEGER));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(DOUBLE));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(STRING));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, BOOLEAN.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, NUMBER.intersection(CONFLICTING));
  Expect.equals(NUMBER, NUMBER.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, NUMBER.intersection(BOOLEAN));
  Expect.equals(NUMBER, NUMBER.intersection(NUMBER));
  Expect.equals(INTEGER, NUMBER.intersection(INTEGER));
  Expect.equals(DOUBLE, NUMBER.intersection(DOUBLE));
  Expect.equals(CONFLICTING, NUMBER.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NUMBER.intersection(STRING));
  Expect.equals(CONFLICTING, NUMBER.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, NUMBER.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, NUMBER.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, INTEGER.intersection(CONFLICTING));
  Expect.equals(INTEGER, INTEGER.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, INTEGER.intersection(BOOLEAN));
  Expect.equals(INTEGER, INTEGER.intersection(NUMBER));
  Expect.equals(INTEGER, INTEGER.intersection(INTEGER));
  Expect.equals(CONFLICTING, INTEGER.intersection(DOUBLE));
  Expect.equals(CONFLICTING, INTEGER.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, INTEGER.intersection(STRING));
  Expect.equals(CONFLICTING, INTEGER.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, INTEGER.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, INTEGER.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, DOUBLE.intersection(CONFLICTING));
  Expect.equals(DOUBLE, DOUBLE.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, DOUBLE.intersection(BOOLEAN));
  Expect.equals(DOUBLE, DOUBLE.intersection(NUMBER));
  Expect.equals(CONFLICTING, DOUBLE.intersection(INTEGER));
  Expect.equals(DOUBLE, DOUBLE.intersection(DOUBLE));
  Expect.equals(CONFLICTING, DOUBLE.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, DOUBLE.intersection(STRING));
  Expect.equals(CONFLICTING, DOUBLE.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, DOUBLE.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, DOUBLE.intersection(NON_PRIMITIVE2));
  
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.intersection(CONFLICTING));
  Expect.equals(INDEXABLE_PRIMITIVE, INDEXABLE_PRIMITIVE.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.intersection(NUMBER));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.intersection(INTEGER));
  Expect.equals(CONFLICTING, INDEXABLE_PRIMITIVE.intersection(DOUBLE));
  Expect.equals(INDEXABLE_PRIMITIVE,
                INDEXABLE_PRIMITIVE.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(STRING, INDEXABLE_PRIMITIVE.intersection(STRING));
  Expect.equals(READABLE_ARRAY,
                INDEXABLE_PRIMITIVE.intersection(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY,
                INDEXABLE_PRIMITIVE.intersection(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY,
                INDEXABLE_PRIMITIVE.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, STRING.intersection(CONFLICTING));
  Expect.equals(STRING, STRING.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, STRING.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, STRING.intersection(NUMBER));
  Expect.equals(CONFLICTING, STRING.intersection(INTEGER));
  Expect.equals(CONFLICTING, STRING.intersection(DOUBLE));
  Expect.equals(STRING, STRING.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(STRING, STRING.intersection(STRING));
  Expect.equals(CONFLICTING, STRING.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, STRING.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, STRING.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, STRING.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, STRING.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(CONFLICTING));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(NUMBER));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(INTEGER));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(DOUBLE));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(STRING));
  Expect.equals(READABLE_ARRAY, READABLE_ARRAY.intersection(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, READABLE_ARRAY.intersection(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, READABLE_ARRAY.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, READABLE_ARRAY.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(CONFLICTING));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(NUMBER));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(INTEGER));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(DOUBLE));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(STRING));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.intersection(READABLE_ARRAY));
  Expect.equals(MUTABLE_ARRAY, MUTABLE_ARRAY.intersection(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, MUTABLE_ARRAY.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, MUTABLE_ARRAY.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(CONFLICTING));
  Expect.equals(EXTENDABLE_ARRAY, EXTENDABLE_ARRAY.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(NUMBER));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(INTEGER));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(DOUBLE));
  Expect.equals(EXTENDABLE_ARRAY,
                EXTENDABLE_ARRAY.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(STRING));
  Expect.equals(EXTENDABLE_ARRAY,
                EXTENDABLE_ARRAY.intersection(READABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY, EXTENDABLE_ARRAY.intersection(MUTABLE_ARRAY));
  Expect.equals(EXTENDABLE_ARRAY,
                EXTENDABLE_ARRAY.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, EXTENDABLE_ARRAY.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(CONFLICTING));
  Expect.equals(NON_PRIMITIVE1, NON_PRIMITIVE1.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(NUMBER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(INTEGER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(DOUBLE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(STRING));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(EXTENDABLE_ARRAY));
  Expect.equals(NON_PRIMITIVE1, NON_PRIMITIVE1.intersection(NON_PRIMITIVE1));
  Expect.equals(CONFLICTING, NON_PRIMITIVE1.intersection(NON_PRIMITIVE2));

  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(CONFLICTING));
  Expect.equals(NON_PRIMITIVE2, NON_PRIMITIVE2.intersection(UNKNOWN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(BOOLEAN));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(NUMBER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(INTEGER));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(DOUBLE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(INDEXABLE_PRIMITIVE));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(STRING));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(READABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(MUTABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(EXTENDABLE_ARRAY));
  Expect.equals(CONFLICTING, NON_PRIMITIVE2.intersection(NON_PRIMITIVE1));
  Expect.equals(NON_PRIMITIVE2, NON_PRIMITIVE2.intersection(NON_PRIMITIVE2));
}

void main() {
  testUnion();
  testIntersection();
}
