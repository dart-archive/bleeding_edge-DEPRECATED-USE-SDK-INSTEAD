// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.completion.dart.local;

import 'package:analysis_server/src/protocol_server.dart';
import 'package:analysis_server/src/services/completion/dart_completion_manager.dart';
import 'package:analysis_server/src/services/completion/local_computer.dart';
import 'package:unittest/unittest.dart';

import '../../reflective_tests.dart';
import 'completion_test_util.dart';

main() {
  groupSep = ' | ';
  runReflectiveTests(LocalComputerTest);
}

@reflectiveTest
class LocalComputerTest extends AbstractSelectorSuggestionTest {

  @override
  CompletionSuggestion assertSuggestLocalClass(String name, {int relevance:
      DART_RELEVANCE_DEFAULT, bool isDeprecated: false}) {
    return assertSuggestClass(
        name,
        relevance: relevance,
        isDeprecated: isDeprecated);
  }

  @override
  CompletionSuggestion assertSuggestLocalClassTypeAlias(String name,
      {int relevance: DART_RELEVANCE_DEFAULT}) {
    return assertSuggestClassTypeAlias(name, relevance);
  }

  @override
  CompletionSuggestion assertSuggestLocalField(String name, String type,
      {int relevance: DART_RELEVANCE_LOCAL_FIELD, bool deprecated: false}) {
    return assertSuggestField(
        name,
        type,
        relevance: relevance,
        isDeprecated: deprecated);
  }

  @override
  CompletionSuggestion assertSuggestLocalFunction(String name,
      String returnType, {bool deprecated: false, int relevance:
      DART_RELEVANCE_LOCAL_FUNCTION}) {
    return assertSuggestFunction(name, returnType, deprecated, relevance);
  }

  @override
  CompletionSuggestion assertSuggestLocalFunctionTypeAlias(String name,
      String returnType, {bool deprecated: false, int relevance:
      DART_RELEVANCE_DEFAULT}) {
    return assertSuggestFunctionTypeAlias(
        name,
        returnType,
        deprecated,
        relevance);
  }

  @override
  CompletionSuggestion assertSuggestLocalGetter(String name, String returnType,
      {int relevance: DART_RELEVANCE_LOCAL_ACCESSOR, bool deprecated: false}) {
    return assertSuggestGetter(
        name,
        returnType,
        relevance: relevance,
        isDeprecated: deprecated);
  }

  @override
  CompletionSuggestion assertSuggestLocalMethod(String name,
      String declaringType, String returnType, {int relevance:
      DART_RELEVANCE_LOCAL_METHOD, bool deprecated: false}) {
    return assertSuggestMethod(
        name,
        declaringType,
        returnType,
        relevance: relevance,
        isDeprecated: deprecated);
  }

  @override
  CompletionSuggestion assertSuggestLocalSetter(String name, {int relevance:
      DART_RELEVANCE_LOCAL_ACCESSOR}) {
    return assertSuggestSetter(name, relevance);
  }

  @override
  CompletionSuggestion assertSuggestLocalTopLevelVar(String name,
      String returnType, {int relevance: DART_RELEVANCE_LOCAL_TOP_LEVEL_VARIABLE}) {
    return assertSuggestTopLevelVar(name, returnType, relevance);
  }

  @override
  CompletionSuggestion assertSuggestLocalVariable(String name,
      String returnType, {int relevance: DART_RELEVANCE_LOCAL_VARIABLE}) {
    // Local variables should only be suggested by LocalComputer
    CompletionSuggestion cs = assertSuggest(
        name,
        csKind: CompletionSuggestionKind.INVOCATION,
        relevance: relevance);
    expect(cs.returnType, returnType != null ? returnType : 'dynamic');
    Element element = cs.element;
    expect(element, isNotNull);
    expect(element.kind, equals(ElementKind.LOCAL_VARIABLE));
    expect(element.name, equals(name));
    expect(element.parameters, isNull);
    expect(element.returnType, returnType != null ? returnType : 'dynamic');
    assertHasNoParameterInfo(cs);
    return cs;
  }

  CompletionSuggestion assertSuggestParameter(String name, String returnType,
      {int relevance: DART_RELEVANCE_PARAMETER}) {
    CompletionSuggestion cs = assertSuggest(
        name,
        csKind: CompletionSuggestionKind.INVOCATION,
        relevance: relevance);
    expect(cs.returnType, returnType != null ? returnType : 'dynamic');
    Element element = cs.element;
    expect(element, isNotNull);
    expect(element.kind, equals(ElementKind.PARAMETER));
    expect(element.name, equals(name));
    expect(element.parameters, isNull);
    expect(
        element.returnType,
        equals(returnType != null ? returnType : 'dynamic'));
    return cs;
  }

  fail_mixin_ordering() {
    // TODO(paulberry): Duplicates aren't being removed, so we see both M1.m()
    // and M2.m().
    addTestSource('''
class B {}
class M1 {
  void m() {}
}
class M2 {
  void m() {}
}
class C extends B with M1, M2 {
  void f() {
    ^
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestMethod('m', 'M2', 'void');
  }

  @override
  void setUpComputer() {
    computer = new LocalComputer();
  }

  test_break_ignores_outer_functions_using_closure() {
    addTestSource('''
void main() {
  foo: while (true) {
    var f = () {
      bar: while (true) { break ^ }
    };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_break_ignores_outer_functions_using_local_function() {
    addTestSource('''
void main() {
  foo: while (true) {
    void f() {
      bar: while (true) { break ^ }
    };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_break_ignores_toplevel_variables() {
    addTestSource('''
int x;
void main() {
  while (true) {
    break ^
  }
}
''');
    expect(computeFast(), isTrue);
    assertNotSuggested('x');
  }

  test_break_ignores_unrelated_statements() {
    addTestSource('''
void main() {
  foo: while (true) {}
  while (true) { break ^ }
  bar: while (true) {}
}
''');
    expect(computeFast(), isTrue);
    // The scope of the label defined by a labeled statement is just the
    // statement itself, so neither "foo" nor "bar" are in scope at the caret
    // position.
    assertNotSuggested('foo');
    assertNotSuggested('bar');
  }

  test_break_to_enclosing_loop() {
    addTestSource('''
void main() {
  foo: while (true) {
    bar: while (true) {
      break ^
    }
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
    assertSuggestLabel('bar');
  }

  test_continue_from_loop_to_switch() {
    addTestSource('''
void main() {
  switch (x) {
    foo: case 1:
      break;
    bar: case 2:
      while (true) {
        continue ^;
      }
      break;
    baz: case 3:
      break;
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
    assertSuggestLabel('bar');
    assertSuggestLabel('baz');
  }

  test_continue_from_switch_to_loop() {
    addTestSource('''
void main() {
  foo: while (true) {
    switch (x) {
      case 1:
        continue ^;
    }
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
  }

  test_continue_ignores_outer_functions_using_closure_with_loop() {
    addTestSource('''
void main() {
  foo: while (true) {
    var f = () {
      bar: while (true) { continue ^ }
    };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_continue_ignores_outer_functions_using_closure_with_switch() {
    addTestSource('''
void main() {
  switch (x) {
    foo: case 1:
      var f = () {
        bar: while (true) { continue ^ }
      };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_continue_ignores_outer_functions_using_local_function_with_loop() {
    addTestSource('''
void main() {
  foo: while (true) {
    void f() {
      bar: while (true) { continue ^ }
    };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_continue_ignores_outer_functions_using_local_function_with_switch() {
    addTestSource('''
void main() {
  switch (x) {
    foo: case 1:
      void f() {
        bar: while (true) { continue ^ }
      };
  }
}
''');
    expect(computeFast(), isTrue);
    // Labels in outer functions are never accessible.
    assertSuggestLabel('bar');
    assertNotSuggested('foo');
  }

  test_continue_ignores_unrelated_statements() {
    addTestSource('''
void main() {
  foo: while (true) {}
  while (true) { continue ^ }
  bar: while (true) {}
}
''');
    expect(computeFast(), isTrue);
    // The scope of the label defined by a labeled statement is just the
    // statement itself, so neither "foo" nor "bar" are in scope at the caret
    // position.
    assertNotSuggested('foo');
    assertNotSuggested('bar');
  }

  test_continue_to_earlier_case() {
    addTestSource('''
void main() {
  switch (x) {
    foo: case 1:
      break;
    case 2:
      continue ^;
    case 3:
      break;
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
  }

  test_continue_to_enclosing_loop() {
    addTestSource('''
void main() {
  foo: while (true) {
    bar: while (true) {
      continue ^
    }
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
    assertSuggestLabel('bar');
  }

  test_continue_to_enclosing_switch() {
    addTestSource('''
void main() {
  switch (x) {
    foo: case 1:
      break;
    bar: case 2:
      switch (y) {
        case 1:
          continue ^;
      }
      break;
    baz: case 3:
      break;
  }
}
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
    assertSuggestLabel('bar');
    assertSuggestLabel('baz');
  }

  test_continue_to_later_case() {
    addTestSource('''
void main() {
  switch (x) {
    case 1:
      break;
    case 2:
      continue ^;
    foo: case 3:
      break;
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
  }

  test_continue_to_same_case() {
    addTestSource('''
void main() {
  switch (x) {
    case 1:
      break;
    foo: case 2:
      continue ^;
    case 3:
      break;
''');
    expect(computeFast(), isTrue);
    assertSuggestLabel('foo');
  }

  test_function_parameters_mixed_required_and_named() {
    addTestSource('''
void m(x, {int y}) {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 1);
    expect(suggestion.hasNamedParameters, true);
  }

  test_function_parameters_mixed_required_and_positional() {
    addTestSource('''
void m(x, [int y]) {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 1);
    expect(suggestion.hasNamedParameters, false);
  }

  test_function_parameters_named() {
    addTestSource('''
void m({x, int y}) {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, true);
  }

  test_function_parameters_none() {
    addTestSource('''
void m() {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, isEmpty);
    expect(suggestion.parameterTypes, isEmpty);
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, false);
  }

  test_function_parameters_positional() {
    addTestSource('''
void m([x, int y]) {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, false);
  }

  test_function_parameters_required() {
    addTestSource('''
void m(x, int y) {}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion = assertSuggestLocalFunction('m', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 2);
    expect(suggestion.hasNamedParameters, false);
  }

  test_method_parameters_mixed_required_and_named() {
    addTestSource('''
class A {
  void m(x, {int y}) {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 1);
    expect(suggestion.hasNamedParameters, true);
  }

  test_method_parameters_mixed_required_and_positional() {
    addTestSource('''
class A {
  void m(x, [int y]) {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 1);
    expect(suggestion.hasNamedParameters, false);
  }

  test_method_parameters_named() {
    addTestSource('''
class A {
  void m({x, int y}) {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, true);
  }

  test_method_parameters_none() {
    addTestSource('''
class A {
  void m() {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, isEmpty);
    expect(suggestion.parameterTypes, isEmpty);
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, false);
  }

  test_method_parameters_positional() {
    addTestSource('''
class A {
  void m([x, int y]) {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 0);
    expect(suggestion.hasNamedParameters, false);
  }

  test_method_parameters_required() {
    addTestSource('''
class A {
  void m(x, int y) {}
}
class B extends A {
  main() {^}
}
''');
    expect(computeFast(), isTrue);
    CompletionSuggestion suggestion =
        assertSuggestLocalMethod('m', 'A', 'void');
    expect(suggestion.parameterNames, hasLength(2));
    expect(suggestion.parameterNames[0], 'x');
    expect(suggestion.parameterTypes[0], 'dynamic');
    expect(suggestion.parameterNames[1], 'y');
    expect(suggestion.parameterTypes[1], 'int');
    expect(suggestion.requiredParameterCount, 2);
    expect(suggestion.hasNamedParameters, false);
  }
}
