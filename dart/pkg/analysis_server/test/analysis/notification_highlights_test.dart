// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.analysis.notification.highlights;

import 'dart:async';

import 'package:analysis_server/src/constants.dart';
import 'package:analysis_server/src/protocol.dart';
import 'package:unittest/unittest.dart';

import '../analysis_abstract.dart';
import '../reflective_tests.dart';


main() {
  runReflectiveTests(AnalysisNotificationHighlightsTest);
  runReflectiveTests(HighlightTypeTest);
}


@reflectiveTest
class AnalysisNotificationHighlightsTest extends AbstractAnalysisTest {
  List<HighlightRegion> regions;

  void assertHasRawRegion(HighlightRegionType type, int offset, int length) {
    for (HighlightRegion region in regions) {
      if (region.offset == offset &&
          region.length == length &&
          region.type == type) {
        return;
      }
    }
    fail(
        'Expected to find (offset=$offset; length=$length; type=$type) in\n'
            '${regions.join('\n')}');
  }

  void assertHasRegion(HighlightRegionType type, String search, [int length =
      -1]) {
    int offset = findOffset(search);
    length = findRegionLength(search, length);
    assertHasRawRegion(type, offset, length);
  }

  void assertHasStringRegion(HighlightRegionType type, String str) {
    int offset = findOffset(str);
    int length = str.length;
    assertHasRawRegion(type, offset, length);
  }

  void assertNoRawRegion(HighlightRegionType type, int offset, int length) {
    for (HighlightRegion region in regions) {
      if (region.offset == offset &&
          region.length == length &&
          region.type == type) {
        fail(
            'Not expected to find (offset=$offset; length=$length; type=$type) in\n'
                '${regions.join('\n')}');
      }
    }
  }


  void assertNoRegion(HighlightRegionType type, String search, [int length =
      -1]) {
    int offset = findOffset(search);
    length = findRegionLength(search, length);
    assertNoRawRegion(type, offset, length);
  }

  int findRegionLength(String search, int length) {
    if (length == -1) {
      length = 0;
      while (length < search.length) {
        int c = search.codeUnitAt(length);
        if (length == 0 && c == '@'.codeUnitAt(0)) {
          length++;
          continue;
        }
        if (!(c >= 'a'.codeUnitAt(0) && c <= 'z'.codeUnitAt(0) ||
            c >= 'A'.codeUnitAt(0) && c <= 'Z'.codeUnitAt(0) ||
            c >= '0'.codeUnitAt(0) && c <= '9'.codeUnitAt(0))) {
          break;
        }
        length++;
      }
    }
    return length;
  }

  Future prepareHighlights() {
    addAnalysisSubscription(AnalysisService.HIGHLIGHTS, testFile);
    return waitForTasksFinished();
  }

  void processNotification(Notification notification) {
    if (notification.event == ANALYSIS_HIGHLIGHTS) {
      var params = new AnalysisHighlightsParams.fromNotification(notification);
      if (params.file == testFile) {
        regions = params.regions;
      }
    }
  }

  @override
  void setUp() {
    super.setUp();
    createProject();
  }

  test_ANNOTATION_hasArguments() {
    addTestFile('''
class AAA {
  const AAA(a, b, c);
}
@AAA(1, 2, 3) main() {}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.ANNOTATION, '@AAA(', '@AAA('.length);
      assertHasRegion(HighlightRegionType.ANNOTATION, ') main', ')'.length);
    });
  }

  test_ANNOTATION_noArguments() {
    addTestFile('''
const AAA = 42;
@AAA main() {}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.ANNOTATION, '@AAA');
    });
  }

  test_BUILT_IN_abstract() {
    addTestFile('''
abstract class A {};
abstract class B = Object with A;
main() {
  var abstract = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'abstract class A');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'abstract class B');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'abstract = 42');
    });
  }

  test_BUILT_IN_as() {
    addTestFile('''
import 'dart:math' as math;
main() {
  p as int;
  var as = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'as math');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'as int');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'as = 42');
    });
  }

  test_BUILT_IN_async() {
    addTestFile('''
fa() async {}
fb() async* {}
main() {
  bool async = false;
}
''');
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.BUILT_IN, 'async');
      assertHasStringRegion(HighlightRegionType.BUILT_IN, 'async*');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'async = false');
    });
  }

  test_BUILT_IN_await() {
    addTestFile('''
main() async {
  await 42;
  await for (var item in []) {
    print(item);
  }
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'await 42');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'await for');
    });
  }

  test_BUILT_IN_deferred() {
    addTestFile('''
import 'dart:math' deferred as math;
main() {
  var deferred = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'deferred as math');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'deferred = 42');
    });
  }

  test_BUILT_IN_export() {
    addTestFile('''
export "dart:math";
main() {
  var export = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'export "dart:');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'export = 42');
    });
  }

  test_BUILT_IN_external() {
    addTestFile('''
class A {
  external A();
  external aaa();
}
external main() {
  var external = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'external A()');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'external aaa()');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'external main()');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'external = 42');
    });
  }

  test_BUILT_IN_factory() {
    addTestFile('''
class A {
  factory A() => null;
}
main() {
  var factory = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'factory A()');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'factory = 42');
    });
  }

  test_BUILT_IN_get() {
    addTestFile('''
get aaa => 1;
class A {
  get bbb => 2;
}
main() {
  var get = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'get aaa =>');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'get bbb =>');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'get = 42');
    });
  }

  test_BUILT_IN_hide() {
    addTestFile('''
import 'foo.dart' hide Foo;
main() {
  var hide = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'hide Foo');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'hide = 42');
    });
  }

  test_BUILT_IN_implements() {
    addTestFile('''
class A {}
class B implements A {}
main() {
  var implements = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'implements A {}');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'implements = 42');
    });
  }

  test_BUILT_IN_import() {
    addTestFile('''
import "foo.dart";
main() {
  var import = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'import "');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'import = 42');
    });
  }

  test_BUILT_IN_library() {
    addTestFile('''
library lib;
main() {
  var library = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'library lib;');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'library = 42');
    });
  }

  test_BUILT_IN_native() {
    addTestFile('''
class A native "A_native" {}
class B {
  bbb() native "bbb_native";
}
main() {
  var native = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'native "A_');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'native "bbb_');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'native = 42');
    });
  }

  test_BUILT_IN_on() {
    addTestFile('''
main() {
  try {
  } on int catch (e) {
  }
  var on = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'on int');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'on = 42');
    });
  }

  test_BUILT_IN_operator() {
    addTestFile('''
class A {
  operator +(x) => null;
}
main() {
  var operator = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'operator +(');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'operator = 42');
    });
  }

  test_BUILT_IN_part() {
    addTestFile('''
part "my_part.dart";
main() {
  var part = 42;
}''');
    addFile('/project/bin/my_part.dart', 'part of lib;');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'part "my_');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'part = 42');
    });
  }

  test_BUILT_IN_partOf() {
    addTestFile('''
part of lib;
main() {
  var part = 1;
  var of = 2;
}''');
    _addLibraryForTestPart();
    return prepareHighlights().then((_) {
      assertHasRegion(
          HighlightRegionType.BUILT_IN,
          'part of',
          'part of'.length);
      assertNoRegion(HighlightRegionType.BUILT_IN, 'part = 1');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'of = 2');
    });
  }

  test_BUILT_IN_set() {
    addTestFile('''
set aaa(x) {}
class A
  set bbb(x) {}
}
main() {
  var set = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'set aaa(');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'set bbb(');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'set = 42');
    });
  }

  test_BUILT_IN_show() {
    addTestFile('''
import 'foo.dart' show Foo;
main() {
  var show = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'show Foo');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'show = 42');
    });
  }

  test_BUILT_IN_static() {
    addTestFile('''
class A {
  static aaa;
  static bbb() {}
}
main() {
  var static = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'static aaa;');
      assertHasRegion(HighlightRegionType.BUILT_IN, 'static bbb()');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'static = 42');
    });
  }

  test_BUILT_IN_sync() {
    addTestFile('''
fa() sync {}
fb() sync* {}
main() {
  bool sync = false;
}
''');
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.BUILT_IN, 'sync');
      assertHasStringRegion(HighlightRegionType.BUILT_IN, 'sync*');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'sync = false');
    });
  }

  test_BUILT_IN_typedef() {
    addTestFile('''
typedef A();
main() {
  var typedef = 42;
}''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'typedef A();');
      assertNoRegion(HighlightRegionType.BUILT_IN, 'typedef = 42');
    });
  }

  test_BUILT_IN_yield() {
    addTestFile('''
main() async* {
  yield 42;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.BUILT_IN, 'yield 42');
    });
  }

  test_BUILT_IN_yieldStar() {
    addTestFile('''
main() async* {
  yield* [];
}
''');
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.BUILT_IN, 'yield*');
    });
  }

  test_CLASS() {
    addTestFile('''
class AAA {}
AAA aaa;
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.CLASS, 'AAA {}');
      assertHasRegion(HighlightRegionType.CLASS, 'AAA aaa');
    });
  }

  test_CLASS_notDynamic() {
    addTestFile('''
dynamic f() {}
''');
    return prepareHighlights().then((_) {
      assertNoRegion(HighlightRegionType.CLASS, 'dynamic f()');
    });
  }

  test_CLASS_notVoid() {
    addTestFile('''
void f() {}
''');
    return prepareHighlights().then((_) {
      assertNoRegion(HighlightRegionType.CLASS, 'void f()');
    });
  }

  test_COMMENT() {
    addTestFile('''
/**
 * documentation comment
 */ 
void main() {
  // end-of-line comment
  my_function(1);
}

void my_function(String a) {
 /* block comment */
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.COMMENT_DOCUMENTATION, '/**', 32);
      assertHasRegion(HighlightRegionType.COMMENT_END_OF_LINE, '//', 22);
      assertHasRegion(HighlightRegionType.COMMENT_BLOCK, '/* b', 19);
    });
  }

  test_CONSTRUCTOR() {
    addTestFile('''
class AAA {
  AAA() {}
  AAA.name(p) {}
}
main() {
  new AAA();
  new AAA.name(42);
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.CONSTRUCTOR, 'name(p)');
      assertHasRegion(HighlightRegionType.CONSTRUCTOR, 'name(42)');
      assertNoRegion(HighlightRegionType.CONSTRUCTOR, 'AAA() {}');
      assertNoRegion(HighlightRegionType.CONSTRUCTOR, 'AAA();');
    });
  }

  test_DIRECTIVE() {
    addTestFile('''
library lib;
import 'dart:math';
export 'dart:math';
part 'part.dart';
''');
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.DIRECTIVE, "library lib;");
      assertHasStringRegion(
          HighlightRegionType.DIRECTIVE,
          "import 'dart:math';");
      assertHasStringRegion(
          HighlightRegionType.DIRECTIVE,
          "export 'dart:math';");
      assertHasStringRegion(HighlightRegionType.DIRECTIVE, "part 'part.dart';");
    });
  }

  test_DIRECTIVE_partOf() {
    addTestFile('''
part of lib;
''');
    _addLibraryForTestPart();
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.DIRECTIVE, "part of lib;");
    });
  }

  test_DYNAMIC_TYPE() {
    addTestFile('''
f() {}
main(p) {
  print(p);
  var v1 = f();
  int v2;
  var v3 = v2;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.DYNAMIC_TYPE, 'p)');
      assertHasRegion(HighlightRegionType.DYNAMIC_TYPE, 'v1 =');
      assertNoRegion(HighlightRegionType.DYNAMIC_TYPE, 'v2;');
      assertNoRegion(HighlightRegionType.DYNAMIC_TYPE, 'v3 =');
    });
  }

  test_ENUM() {
    addTestFile('''
enum MyEnum {A, B, C}
MyEnum value;
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.ENUM, 'MyEnum {');
      assertHasRegion(HighlightRegionType.ENUM, 'MyEnum value;');
    });
  }

  test_ENUM_CONSTANT() {
    addTestFile('''
enum MyEnum {AAA, BBB}
main() {
  print(MyEnum.AAA);
  print(MyEnum.BBB);
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.ENUM_CONSTANT, 'AAA, ');
      assertHasRegion(HighlightRegionType.ENUM_CONSTANT, 'BBB}');
      assertHasRegion(HighlightRegionType.ENUM_CONSTANT, 'AAA);');
      assertHasRegion(HighlightRegionType.ENUM_CONSTANT, 'BBB);');
    });
  }

  test_FIELD() {
    addTestFile('''
class A {
  int aaa = 1;
  int bbb = 2;
  A([this.bbb = 3]);
}
main(A a) {
  a.aaa = 4;
  a.bbb = 5;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.FIELD, 'aaa = 1');
      assertHasRegion(HighlightRegionType.FIELD, 'bbb = 2');
      assertHasRegion(HighlightRegionType.FIELD, 'bbb = 3');
      assertHasRegion(HighlightRegionType.FIELD, 'aaa = 4');
      assertHasRegion(HighlightRegionType.FIELD, 'bbb = 5');
    });
  }

  test_FIELD_STATIC() {
    addTestFile('''
class A {
  static aaa = 1;
  static get bbb => null;
  static set ccc(x) {}
}
main() {
  A.aaa = 2;
  A.bbb;
  A.ccc = 3;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.FIELD_STATIC, 'aaa = 1');
      assertHasRegion(HighlightRegionType.FIELD_STATIC, 'aaa = 2');
      assertHasRegion(HighlightRegionType.FIELD_STATIC, 'bbb;');
      assertHasRegion(HighlightRegionType.FIELD_STATIC, 'ccc = 3');
    });
  }

  test_FUNCTION() {
    addTestFile('''
fff(p) {}
main() {
  fff(42);
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.FUNCTION_DECLARATION, 'fff(p) {}');
      assertHasRegion(HighlightRegionType.FUNCTION, 'fff(42)');
    });
  }

  test_FUNCTION_TYPE_ALIAS() {
    addTestFile('''
typedef FFF(p);
main(FFF fff) {
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.FUNCTION_TYPE_ALIAS, 'FFF(p)');
      assertHasRegion(HighlightRegionType.FUNCTION_TYPE_ALIAS, 'FFF fff)');
    });
  }

  test_GETTER_DECLARATION() {
    addTestFile('''
get aaa => null;
class A {
  get bbb => null;
}
main(A a) {
  aaa;
  a.bbb;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.GETTER_DECLARATION, 'aaa => null');
      assertHasRegion(HighlightRegionType.GETTER_DECLARATION, 'bbb => null');
      assertHasRegion(HighlightRegionType.TOP_LEVEL_VARIABLE, 'aaa;');
      assertHasRegion(HighlightRegionType.FIELD, 'bbb;');
    });
  }

  test_IDENTIFIER_DEFAULT() {
    addTestFile('''
main() {
  aaa = 42;
  bbb(84);
  CCC ccc;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.IDENTIFIER_DEFAULT, 'aaa = 42');
      assertHasRegion(HighlightRegionType.IDENTIFIER_DEFAULT, 'bbb(84)');
      assertHasRegion(HighlightRegionType.IDENTIFIER_DEFAULT, 'CCC ccc');
    });
  }

  test_IMPORT_PREFIX() {
    addTestFile('''
import 'dart:math' as ma;
main() {
  ma.max(1, 2);
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.IMPORT_PREFIX, 'ma;');
      assertHasRegion(HighlightRegionType.IMPORT_PREFIX, 'ma.max');
    });
  }

  test_KEYWORD() {
    addTestFile('''
main() {
  assert(true);
  for (;;) break;
  switch (0) {
    case 0: break;
    default: break;
  }
  try {} catch (e) {}
  const v1 = 0;
  for (;;) continue;
  do {} while (true);
  if (true) {} else {}
  var v2 = false;
  final v3 = 1;
  try {} finally {}
  for (var v4 in []) {}
  v3 is int;
  new A();
  try {} catch (e) {rethrow;}
  var v5 = true;
  while (true) {}
}
class A {}
class B extends A {
  B() : super();
  m() {
    return this;
  }
}
class C = Object with A;
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.KEYWORD, 'assert(true)');
      assertHasRegion(HighlightRegionType.KEYWORD, 'for (;;)');
      assertHasRegion(HighlightRegionType.KEYWORD, 'for (var v4 in');
      assertHasRegion(HighlightRegionType.KEYWORD, 'break;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'case 0:');
      assertHasRegion(HighlightRegionType.KEYWORD, 'catch (e) {}');
      assertHasRegion(HighlightRegionType.KEYWORD, 'class A {}');
      assertHasRegion(HighlightRegionType.KEYWORD, 'const v1');
      assertHasRegion(HighlightRegionType.KEYWORD, 'continue;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'default:');
      assertHasRegion(HighlightRegionType.KEYWORD, 'do {} while');
      assertHasRegion(HighlightRegionType.KEYWORD, 'if (true)');
      assertHasRegion(HighlightRegionType.KEYWORD, 'false;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'final v3 =');
      assertHasRegion(HighlightRegionType.KEYWORD, 'finally {}');
      assertHasRegion(HighlightRegionType.KEYWORD, 'in []');
      assertHasRegion(HighlightRegionType.KEYWORD, 'is int');
      assertHasRegion(HighlightRegionType.KEYWORD, 'new A();');
      assertHasRegion(HighlightRegionType.KEYWORD, 'rethrow;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'return this');
      assertHasRegion(HighlightRegionType.KEYWORD, 'super();');
      assertHasRegion(HighlightRegionType.KEYWORD, 'switch (0)');
      assertHasRegion(HighlightRegionType.KEYWORD, 'this;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'true;');
      assertHasRegion(HighlightRegionType.KEYWORD, 'try {');
      assertHasRegion(HighlightRegionType.KEYWORD, 'while (true) {}');
      assertHasRegion(HighlightRegionType.KEYWORD, 'while (true);');
      assertHasRegion(HighlightRegionType.KEYWORD, 'with A;');
    });
  }

  test_KEYWORD_void() {
    addTestFile('''
void main() {
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.KEYWORD, 'void main()');
    });
  }

  test_LABEL() {
    addTestFile('''
main() {
myLabel:
  while (true) {
    break myLabel;
  }
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.LABEL, 'myLabel:');
      assertHasRegion(HighlightRegionType.LABEL, 'myLabel;');
    });
  }

  test_LITERAL_BOOLEAN() {
    addTestFile('var V = true;');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.LITERAL_BOOLEAN, 'true;');
    });
  }

  test_LITERAL_DOUBLE() {
    addTestFile('var V = 4.2;');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.LITERAL_DOUBLE, '4.2;', '4.2'.length);
    });
  }

  test_LITERAL_INTEGER() {
    addTestFile('var V = 42;');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.LITERAL_INTEGER, '42;');
    });
  }

  test_LITERAL_LIST() {
    addTestFile('var V = <int>[1, 2, 3];');
    return prepareHighlights().then((_) {
      assertHasStringRegion(HighlightRegionType.LITERAL_LIST, '<int>[1, 2, 3]');
    });
  }

  test_LITERAL_MAP() {
    addTestFile("var V = const <int, String>{1: 'a', 2: 'b', 3: 'c'};");
    return prepareHighlights().then((_) {
      assertHasStringRegion(
          HighlightRegionType.LITERAL_MAP,
          "const <int, String>{1: 'a', 2: 'b', 3: 'c'}");
    });
  }

  test_LITERAL_STRING() {
    addTestFile('var V = "abc";');
    return prepareHighlights().then((_) {
      assertHasRegion(
          HighlightRegionType.LITERAL_STRING,
          '"abc";',
          '"abc"'.length);
    });
  }

  test_LOCAL_VARIABLE() {
    addTestFile('''
main() {
  int vvv = 0;
  vvv;
  vvv = 1;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(
          HighlightRegionType.LOCAL_VARIABLE_DECLARATION,
          'vvv = 0');
      assertHasRegion(HighlightRegionType.LOCAL_VARIABLE, 'vvv;');
      assertHasRegion(HighlightRegionType.LOCAL_VARIABLE, 'vvv = 1;');
    });
  }

  test_METHOD() {
    addTestFile('''
class A {
  aaa() {}
  static bbb() {}
}
main(A a) {
  a.aaa();
  a.aaa;
  A.bbb();
  A.bbb;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.METHOD_DECLARATION, 'aaa() {}');
      assertHasRegion(
          HighlightRegionType.METHOD_DECLARATION_STATIC,
          'bbb() {}');
      assertHasRegion(HighlightRegionType.METHOD, 'aaa();');
      assertHasRegion(HighlightRegionType.METHOD, 'aaa;');
      assertHasRegion(HighlightRegionType.METHOD_STATIC, 'bbb();');
      assertHasRegion(HighlightRegionType.METHOD_STATIC, 'bbb;');
    });
  }

  test_METHOD_bestType() {
    addTestFile('''
main(p) {
  if (p is List) {
    p.add(null);
  }
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.METHOD, 'add(null)');
    });
  }

  test_PARAMETER() {
    addTestFile('''
main(int p) {
  p;
  p = 42;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.PARAMETER, 'p) {');
      assertHasRegion(HighlightRegionType.PARAMETER, 'p;');
      assertHasRegion(HighlightRegionType.PARAMETER, 'p = 42');
    });
  }

  test_SETTER_DECLARATION() {
    addTestFile('''
set aaa(x) {}
class A {
  set bbb(x) {}
}
main(A a) {
  aaa = 1;
  a.bbb = 2;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.SETTER_DECLARATION, 'aaa(x)');
      assertHasRegion(HighlightRegionType.SETTER_DECLARATION, 'bbb(x)');
      assertHasRegion(HighlightRegionType.TOP_LEVEL_VARIABLE, 'aaa = 1');
      assertHasRegion(HighlightRegionType.FIELD, 'bbb = 2');
    });
  }

  test_TOP_LEVEL_VARIABLE() {
    addTestFile('''
const VVV = 0;
@VVV // annotation
main() {
  print(VVV);
  VVV = 1;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.TOP_LEVEL_VARIABLE, 'VVV = 0');
      assertHasRegion(
          HighlightRegionType.TOP_LEVEL_VARIABLE,
          'VVV // annotation');
      assertHasRegion(HighlightRegionType.TOP_LEVEL_VARIABLE, 'VVV);');
      assertHasRegion(HighlightRegionType.TOP_LEVEL_VARIABLE, 'VVV = 1');
    });
  }

  test_TYPE_NAME_DYNAMIC() {
    addTestFile('''
dynamic main() {
  dynamic = 42;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.TYPE_NAME_DYNAMIC, 'dynamic main()');
      assertNoRegion(HighlightRegionType.IDENTIFIER_DEFAULT, 'dynamic main()');
      assertNoRegion(HighlightRegionType.TYPE_NAME_DYNAMIC, 'dynamic = 42');
    });
  }

  test_TYPE_PARAMETER() {
    addTestFile('''
class A<T> {
  T fff;
  T mmm(T p) => null;
}
''');
    return prepareHighlights().then((_) {
      assertHasRegion(HighlightRegionType.TYPE_PARAMETER, 'T> {');
      assertHasRegion(HighlightRegionType.TYPE_PARAMETER, 'T fff;');
      assertHasRegion(HighlightRegionType.TYPE_PARAMETER, 'T mmm(');
      assertHasRegion(HighlightRegionType.TYPE_PARAMETER, 'T p)');
    });
  }

  void _addLibraryForTestPart() {
    addFile('$testFolder/my_lib.dart', '''
library lib;
part 'test.dart';
    ''');
  }
}


@reflectiveTest
class HighlightTypeTest {
  void test_constructor() {
    expect(
        HighlightRegionType.CLASS,
        new HighlightRegionType(HighlightRegionType.CLASS.name));
  }

  void test_toString() {
    expect(HighlightRegionType.CLASS.toString(), 'HighlightRegionType.CLASS');
  }

  void test_valueOf_unknown() {
    expect(() {
      new HighlightRegionType('no-such-type');
    }, throws);
  }
}
