// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.analysis.notification.navigation;

import 'dart:async';

import 'package:analysis_server/src/constants.dart';
import 'package:analysis_server/src/protocol.dart';
import 'package:unittest/unittest.dart';

import '../analysis_abstract.dart';
import '../reflective_tests.dart';


main() {
  groupSep = ' | ';
  runReflectiveTests(AnalysisNotificationNavigationTest);
}


@reflectiveTest
class AnalysisNotificationNavigationTest extends AbstractAnalysisTest {
  List<NavigationRegion> regions;
  List<NavigationTarget> targets;
  List<String> targetFiles;

  NavigationRegion testRegion;
  List<int> testTargetIndexes;
  NavigationTarget testTarget;

  /**
   * Validates that there is a target in [testTargetIndexes] with [file],
   * at [offset] and with the given [length].
   */
  void assertHasFileTarget(String file, int offset, int length) {
    List<NavigationTarget> testTargets =
        testTargetIndexes.map((int index) => targets[index]).toList();
    for (NavigationTarget target in testTargets) {
      if (targetFiles[target.fileIndex] == file &&
          target.offset == offset &&
          target.length == length) {
        testTarget = target;
        return;
      }
    }
    fail(
        'Expected to find target (file=$file; offset=$offset; length=$length) in\n'
            '${testRegion} in\n' '${testTargets.join('\n')}');
  }

  void assertHasOperatorRegion(String regionSearch, int regionLength,
      String targetSearch, int targetLength) {
    assertHasRegion(regionSearch, regionLength);
    assertHasTarget(targetSearch, targetLength);
  }

  /**
   * Validates that there is a region at the offset of [search] in [testFile].
   * If [length] is not specified explicitly, then length of an identifier
   * from [search] is used.
   */
  void assertHasRegion(String search, [int length = -1]) {
    int offset = findOffset(search);
    if (length == -1) {
      length = findIdentifierLength(search);
    }
    findRegion(offset, length, true);
  }

  /**
   * Validates that there is a region at the offset of [search] in [testFile]
   * with the given [length] or the length of [search].
   */
  void assertHasRegionString(String search, [int length = -1]) {
    int offset = findOffset(search);
    if (length == -1) {
      length = search.length;
    }
    findRegion(offset, length, true);
  }

  /**
   * Validates that there is an identifier region at [regionSearch] with target
   * at [targetSearch].
   */
  void assertHasRegionTarget(String regionSearch, String targetSearch) {
    assertHasRegion(regionSearch);
    assertHasTarget(targetSearch);
  }

  /**
   * Validates that there is a target in [testTargets]  with [testFile], at the
   * offset of [search] in [testFile], and with the given [length] or the length
   * of an leading identifier in [search].
   */
  void assertHasTarget(String search, [int length = -1]) {
    int offset = findOffset(search);
    if (length == -1) {
      length = findIdentifierLength(search);
    }
    assertHasFileTarget(testFile, offset, length);
  }

  /**
   * Validates that there is no a region at [search] and with the given
   * [length].
   */
  void assertNoRegion(String search, int length) {
    int offset = findOffset(search);
    findRegion(offset, length, false);
  }

  /**
   * Validates that there is no a region at [search] with any length.
   */
  void assertNoRegionAt(String search) {
    int offset = findOffset(search);
    findRegion(offset, -1, false);
  }

  /**
   * Validates that there is no a region for [search] string.
   */
  void assertNoRegionString(String search) {
    int offset = findOffset(search);
    int length = search.length;
    findRegion(offset, length, false);
  }

  void assertRegionsSorted() {
    int lastEnd = -1;
    for (NavigationRegion region in regions) {
      int offset = region.offset;
      if (offset < lastEnd) {
        fail('$lastEnd was expected to be > $offset in\n' + regions.join('\n'));
      }
      lastEnd = offset + region.length;
    }
  }

  /**
   * Finds the navigation region with the given [offset] and [length].
   * If [length] is `-1`, then it is ignored.
   *
   * If [exists] is `true`, then fails if such region does not exist.
   * Otherwise remembers this it into [testRegion].
   * Also fills [testTargets] with its targets.
   *
   * If [exists] is `false`, then fails if such region exists.
   */
  void findRegion(int offset, int length, bool exists) {
    for (NavigationRegion region in regions) {
      if (region.offset == offset &&
          (length == -1 || region.length == length)) {
        if (exists == false) {
          fail(
              'Not expected to find (offset=$offset; length=$length) in\n'
                  '${regions.join('\n')}');
        }
        testRegion = region;
        testTargetIndexes = region.targets;
        return;
      }
    }
    if (exists == true) {
      fail(
          'Expected to find (offset=$offset; length=$length) in\n'
              '${regions.join('\n')}');
    }
  }

  Future prepareNavigation() {
    addAnalysisSubscription(AnalysisService.NAVIGATION, testFile);
    return waitForTasksFinished().then((_) {
      assertRegionsSorted();
    });
  }

  void processNotification(Notification notification) {
    if (notification.event == ANALYSIS_NAVIGATION) {
      var params = new AnalysisNavigationParams.fromNotification(notification);
      if (params.file == testFile) {
        regions = params.regions;
        targets = params.targets;
        targetFiles = params.files;
      }
    }
  }

  @override
  void setUp() {
    super.setUp();
    createProject();
  }

  test_afterAnalysis() {
    addTestFile('''
class AAA {}
AAA aaa;
''');
    return waitForTasksFinished().then((_) {
      return prepareNavigation().then((_) {
        assertHasRegionTarget('AAA aaa;', 'AAA {}');
      });
    });
  }

  test_class_fromSDK() {
    addTestFile('''
int V = 42;
''');
    return prepareNavigation().then((_) {
      assertHasRegion('int V');
      int targetIndex = testTargetIndexes[0];
      NavigationTarget target = targets[targetIndex];
      expect(target.startLine, greaterThan(0));
      expect(target.startColumn, greaterThan(0));
    });
  }

  test_constructor_named() {
    addTestFile('''
class A {
  A.named(BBB p) {}
}
class BBB {}
''');
    return prepareNavigation().then((_) {
      // has region for complete "A.named"
      assertHasRegionString('A.named');
      assertHasTarget('named(BBB');
      // no separate regions for "A" and "named"
      assertNoRegion('A.named(', 'A'.length);
      assertNoRegion('named(', 'named'.length);
      // validate that we don't forget to resolve parameters
      assertHasRegionTarget('BBB p', 'BBB {}');
    });
  }

  test_constructor_unnamed() {
    addTestFile('''
class A {
  A(BBB p) {}
}
class BBB {}
''');
    return prepareNavigation().then((_) {
      // has region for complete "A.named"
      assertHasRegion("A(BBB");
      assertHasTarget("A(BBB", 0);
      // validate that we don't forget to resolve parameters
      assertHasRegionTarget('BBB p', 'BBB {}');
    });
  }

  test_factoryRedirectingConstructor_implicit() {
    addTestFile('''
class A {
  factory A() = B;
}
class B {
}
''');
    return prepareNavigation().then((_) {
      assertHasRegion('B;');
      assertHasTarget('B {');
    });
  }

  test_factoryRedirectingConstructor_implicit_withTypeArgument() {
    addTestFile('''
class A {}
class B {
  factory B() = C<A>;
}
class C<T> {}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegion('C<A>');
        assertHasTarget('C<T> {');
      }
      {
        assertHasRegion('A>;');
        assertHasTarget('A {');
      }
    });
  }

  test_factoryRedirectingConstructor_named() {
    addTestFile('''
class A {
  factory A() = B.named;
}
class B {
  B.named();
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionString('B.named');
      assertHasTarget('named();');
    });
  }

  test_factoryRedirectingConstructor_named_withTypeArgument() {
    addTestFile('''
class A {}
class B {
  factory B.named() = C<A>.named;
}
class C<T> {
  C.named() {}
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegion('C<A>');
        assertHasTarget('named() {}');
      }
      {
        assertHasRegion('A>.named');
        assertHasTarget('A {');
      }
      {
        assertHasRegion('.named;', '.named'.length);
        assertHasTarget('named() {}');
      }
    });
  }

  test_factoryRedirectingConstructor_unnamed() {
    addTestFile('''
class A {
  factory A() = B;
}
class B {
  B() {}
}
''');
    return prepareNavigation().then((_) {
      assertHasRegion('B;');
      assertHasTarget('B() {}', 0);
    });
  }

  test_factoryRedirectingConstructor_unnamed_withTypeArgument() {
    addTestFile('''
class A {}
class B {
  factory B() = C<A>;
}
class C<T> {
  C() {}
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegion('C<A>');
        assertHasTarget('C() {}', 0);
      }
      {
        assertHasRegion('A>;');
        assertHasTarget('A {');
      }
    });
  }

  test_factoryRedirectingConstructor_unresolved() {
    addTestFile('''
class A {
  factory A() = B;
}
''');
    return prepareNavigation().then((_) {
      // don't check regions, but there should be no exceptions
    });
  }

  test_fieldFormalParameter() {
    addTestFile('''
class AAA {
  int fff = 123;
  AAA(this.fff);
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionTarget('fff);', 'fff = 123');
    });
  }

  test_fieldFormalParameter_unresolved() {
    addTestFile('''
class AAA {
  AAA(this.fff);
}
''');
    return prepareNavigation().then((_) {
      assertNoRegion('fff);', 3);
    });
  }

  test_identifier_resolved() {
    addTestFile('''
class AAA {}
main() {
  AAA aaa = null;
  print(aaa);
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionTarget('AAA aaa', 'AAA {}');
      assertHasRegionTarget('aaa);', 'aaa = null');
      assertHasRegionTarget('main() {', 'main() {');
    });
  }

  test_identifier_unresolved() {
    addTestFile('''
main() {
  print(vvv);
}
''');
    return prepareNavigation().then((_) {
      assertNoRegionString('vvv');
    });
  }

  test_identifier_whenStrayImportDirective() {
    addTestFile('''
main() {
  int aaa = 42;
  print(aaa);
}
import 'dart:math';
''');
    return prepareNavigation().then((_) {
      assertHasRegionTarget('aaa);', 'aaa = 42');
    });
  }

  test_instanceCreation_implicit() {
    addTestFile('''
class A {
}
main() {
  new A();
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionString('new A');
      assertHasTarget('A {');
    });
  }

  test_instanceCreation_implicit_withTypeArgument() {
    addTestFile('''
class A {}
class B<T> {}
main() {
  new B<A>();
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegion('new B<A>', 'new B'.length);
        assertHasTarget('B<T> {');
      }
      {
        assertHasRegion('A>();', 'A'.length);
        assertHasTarget('A {');
      }
    });
  }

  test_instanceCreation_named() {
    addTestFile('''
class A {
  A.named() {}
}
main() {
  new A.named();
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionString('new A.named');
      assertHasTarget('named() {}');
    });
  }

  test_instanceCreation_named_withTypeArgument() {
    addTestFile('''
class A {}
class B<T> {
  B.named() {}
}
main() {
  new B<A>.named();
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegionString('new B');
        assertHasTarget('named() {}');
      }
      {
        assertHasRegion('A>.named');
        assertHasTarget('A {');
      }
      {
        assertHasRegion('.named();', '.named'.length);
        assertHasTarget('named() {}');
      }
    });
  }

  test_instanceCreation_unnamed() {
    addTestFile('''
class A {
  A() {}
}
main() {
  new A();
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionString('new A');
      assertHasTarget('A() {}', 0);
    });
  }

  test_instanceCreation_unnamed_withTypeArgument() {
    addTestFile('''
class A {}
class B<T> {
  B() {}
}
main() {
  new B<A>();
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegionString('new B');
        assertHasTarget('B() {}', 0);
      }
      {
        assertHasRegion('A>();');
        assertHasTarget('A {');
      }
    });
  }

  test_multiplyDefinedElement() {
    addFile('$projectPath/bin/libA.dart', 'library A; int TEST = 1;');
    addFile('$projectPath/bin/libB.dart', 'library B; int TEST = 2;');
    addTestFile('''
import 'libA.dart';
import 'libB.dart';
main() {
  TEST;
}
''');
    return prepareNavigation().then((_) {
      assertNoRegionAt('TEST');
    });
  }

  test_operator_arithmetic() {
    addTestFile('''
class A {
  A operator +(other) => null;
  A operator -() => null;
  A operator -(other) => null;
  A operator *(other) => null;
  A operator /(other) => null;
}
main() {
  var a = new A();
  a - 1;
  a + 2;
  -a; // unary
  --a;
  ++a;
  a--; // mm
  a++; // pp
  a -= 3;
  a += 4;
  a *= 5;
  a /= 6;
}
''');
    return prepareNavigation().then((_) {
      assertHasOperatorRegion('- 1', 1, '-(other) => null', 1);
      assertHasOperatorRegion('+ 2', 1, '+(other) => null', 1);
      assertHasOperatorRegion('-a; // unary', 1, '-() => null', 1);
      assertHasOperatorRegion('--a;', 2, '-(other) => null', 1);
      assertHasOperatorRegion('++a;', 2, '+(other) => null', 1);
      assertHasOperatorRegion('--; // mm', 2, '-(other) => null', 1);
      assertHasOperatorRegion('++; // pp', 2, '+(other) => null', 1);
      assertHasOperatorRegion('-= 3', 2, '-(other) => null', 1);
      assertHasOperatorRegion('+= 4', 2, '+(other) => null', 1);
      assertHasOperatorRegion('*= 5', 2, '*(other) => null', 1);
      assertHasOperatorRegion('/= 6', 2, '/(other) => null', 1);
    });
  }

  test_operator_index() {
    addTestFile('''
class A {
  A operator +(other) => null;
}
class B {
  A operator [](index) => null;
  operator []=(index, A value) {}
}
main() {
  var b = new B();
  b[0] // [];
  b[1] = 1; // []=;
  b[2] += 2;
}
''');
    return prepareNavigation().then((_) {
      assertHasOperatorRegion('] // []', 1, '[](index)', 2);
      assertHasOperatorRegion('] = 1;', 1, '[]=(index,', 3);
      assertHasOperatorRegion('] += 2;', 1, '[]=(index,', 3);
      assertHasOperatorRegion('+= 2;', 2, '+(other)', 1);
    });
  }

  test_partOf() {
    var libCode = 'library lib; part "test.dart";';
    var libFile = addFile('$projectPath/bin/lib.dart', libCode);
    addTestFile('part of lib;');
    return prepareNavigation().then((_) {
      assertHasRegionString('part of lib');
      assertHasFileTarget(libFile, libCode.indexOf('lib;'), 'lib'.length);
    });
  }

  test_string_export() {
    var libCode = 'library lib;';
    var libFile = addFile('$projectPath/bin/lib.dart', libCode);
    addTestFile('export "lib.dart";');
    return prepareNavigation().then((_) {
      assertHasRegionString('export "lib.dart"');
      assertHasFileTarget(libFile, libCode.indexOf('lib;'), 'lib'.length);
    });
  }

  test_string_export_unresolvedUri() {
    addTestFile('export "no.dart";');
    return prepareNavigation().then((_) {
      assertNoRegionString('export "no.dart"');
    });
  }

  test_string_import() {
    var libCode = 'library lib;';
    var libFile = addFile('$projectPath/bin/lib.dart', libCode);
    addTestFile('import "lib.dart";');
    return prepareNavigation().then((_) {
      assertHasRegionString('import "lib.dart"');
      assertHasFileTarget(libFile, libCode.indexOf('lib;'), 'lib'.length);
    });
  }

  test_string_import_noUri() {
    addTestFile('import ;');
    return prepareNavigation().then((_) {
      assertNoRegionAt('import ;');
    });
  }

  test_string_import_unresolvedUri() {
    addTestFile('import "no.dart";');
    return prepareNavigation().then((_) {
      assertNoRegionString('import "no.dart"');
    });
  }

  test_string_part() {
    var unitCode = 'part of lib;  f() {}';
    var unitFile = addFile('$projectPath/bin/test_unit.dart', unitCode);
    addTestFile('''
library lib;
part "test_unit.dart";
''');
    return prepareNavigation().then((_) {
      assertHasRegionString('part "test_unit.dart"');
      assertHasFileTarget(unitFile, 0, 0);
    });
  }

  test_string_part_unresolvedUri() {
    addTestFile('''
library lib;
part "test_unit.dart";
''');
    return prepareNavigation().then((_) {
      assertNoRegionString('part "test_unit.dart"');
    });
  }

  test_superConstructorInvocation() {
    addTestFile('''
class A {
  A() {}
  A.named() {}
}
class B extends A {
  B() : super();
  B.named() : super.named();
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegionString('super');
        assertHasTarget('A() {}', 0);
      }
      {
        assertHasRegionString('super.named');
        assertHasTarget('named() {}');
      }
    });
  }

  test_superConstructorInvocation_synthetic() {
    addTestFile('''
class A {
}
class B extends A {
  B() : super();
}
''');
    return prepareNavigation().then((_) {
      {
        assertHasRegionString('super');
        assertHasTarget('A {');
      }
    });
  }

  test_targetElement() {
    addTestFile('''
class AAA {}
main() {
  AAA aaa = null;
}
''');
    return prepareNavigation().then((_) {
      assertHasRegionTarget('AAA aaa', 'AAA {}');
      expect(testTarget.kind, ElementKind.CLASS);
    });
  }

  test_type_dynamic() {
    addTestFile('''
main() {
  dynamic v = null;
}
''');
    return prepareNavigation().then((_) {
      assertNoRegionAt('dynamic');
    });
  }

  test_type_void() {
    addTestFile('''
void main() {
}
''');
    return prepareNavigation().then((_) {
      assertNoRegionAt('void');
    });
  }
}
