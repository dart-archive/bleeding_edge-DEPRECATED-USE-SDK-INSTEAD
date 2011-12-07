// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");
#import("../../../leg/leg.dart", prefix: "leg");
#import("../../../leg/elements/elements.dart", prefix: "lego");

twoClasses() {
  var classA = new leg.ClassElement(const leg.SourceString("A"));
  var classB = new leg.ClassElement(const leg.SourceString("B"));
  String generated = compileClasses([classA, classB]);
  Expect.isTrue(generated.contains("Isolate.prototype.A = function() {};"));
  Expect.isTrue(generated.contains("Isolate.prototype.B = function() {};"));
}

class MockType implements leg.Type {
  var element;
  MockType(this.element);
}

subClass() {
  checkOutput(String generated) {
    Expect.isTrue(generated.contains("Isolate.prototype.A = function() {};"));
    Expect.isTrue(generated.contains("Isolate.prototype.B = function() {};"));
    Expect.isTrue(generated.contains(@"Isolate.$inherits = function"));
    Expect.isTrue(generated.contains(
        "Isolate.\$inherits(Isolate.prototype.A, Isolate.prototype.B);\n"));
  }

  var classA = new leg.ClassElement(const leg.SourceString("A"));
  var classB = new leg.ClassElement(const leg.SourceString("B"));
  classA.supertype = new MockType(classB);
  checkOutput(compileClasses([classA, classB]));
  checkOutput(compileClasses([classB, classA]));
}

main() {
  twoClasses();
  subClass();
}
