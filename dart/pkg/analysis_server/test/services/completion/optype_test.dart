// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library test.services.completion.computer.dart.optype;

import 'package:analysis_server/src/services/completion/completion_target.dart';
import 'package:analysis_server/src/services/completion/optype.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/source.dart';
import 'package:unittest/unittest.dart';

import '../../abstract_context.dart';
import '../../reflective_tests.dart';

main() {
  groupSep = ' | ';
  runReflectiveTests(OpTypeTest);
}

@reflectiveTest
class OpTypeTest {

  OpType visitor;

  void addTestSource(String content, {bool resolved: false}) {
    int offset = content.indexOf('^');
    expect(offset, isNot(equals(-1)), reason: 'missing ^');
    int nextOffset = content.indexOf('^', offset + 1);
    expect(nextOffset, equals(-1), reason: 'too many ^');
    content = content.substring(0, offset) + content.substring(offset + 1);
    Source source = new _TestSource('/completionTest.dart');
    AnalysisContext context = AnalysisEngine.instance.createAnalysisContext();
    context.sourceFactory =
        new SourceFactory([AbstractContextTest.SDK_RESOLVER]);
    context.setContents(source, content);
    CompilationUnit unit = resolved ?
        context.resolveCompilationUnit2(source, source) :
        context.parseCompilationUnit(source);
    CompletionTarget completionTarget =
        new CompletionTarget.forOffset(unit, offset);
    visitor = new OpType.forCompletion(completionTarget, offset);
  }

  void assertOpType({bool invocation: false, bool returnValue: false,
      bool typeNames: false, bool voidReturn: false, bool statementLabel: false,
      bool caseLabel: false}) {
    expect(
        visitor.includeInvocationSuggestions,
        equals(invocation),
        reason: 'invocation');
    expect(
        visitor.includeReturnValueSuggestions,
        equals(returnValue),
        reason: 'returnValue');
    expect(
        visitor.includeTypeNameSuggestions,
        equals(typeNames),
        reason: 'typeNames');
    expect(
        visitor.includeVoidReturnSuggestions,
        equals(voidReturn),
        reason: 'voidReturn');
    expect(
        visitor.includeStatementLabelSuggestions,
        equals(statementLabel),
        reason: 'statementLabel');
    expect(
        visitor.includeCaseLabelSuggestions,
        equals(caseLabel),
        reason: 'caseLabel');
  }

  test_Annotation() {
    // SimpleIdentifier  Annotation  MethodDeclaration  ClassDeclaration
    addTestSource('class C { @A^ }');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ArgumentList() {
    // ArgumentList  MethodInvocation  ExpressionStatement  Block
    addTestSource('void main() {expect(^)}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ArgumentList_namedParam() {
    // SimpleIdentifier  NamedExpression  ArgumentList  MethodInvocation
    // ExpressionStatement
    addTestSource('void main() {expect(foo: ^)}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_AsExpression() {
    // SimpleIdentifier  TypeName  AsExpression
    addTestSource('class A {var b; X _c; foo() {var a; (a as ^).foo();}');
    assertOpType(typeNames: true);
  }

  test_AssignmentExpression_name() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('class A {} main() {int a; int ^b = 1;}');
    assertOpType();
  }

  test_AssignmentExpression_RHS() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('class A {} main() {int a; int b = ^}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_AssignmentExpression_type() {
    // SimpleIdentifier  TypeName  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('''
      main() {
        int a;
        ^ b = 1;}''');
    // TODO (danrubel) When entering 1st of 2 identifiers on assignment LHS
    // the user may be either (1) entering a type for the assignment
    // or (2) starting a new statement.
    // Consider suggesting only types
    // if only spaces separates the 1st and 2nd identifiers.
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_AssignmentExpression_type_newline() {
    // SimpleIdentifier  TypeName  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('''
      main() {
        int a;
        ^
        b = 1;}''');
    // Allow non-types preceding an identifier on LHS of assignment
    // if newline follows first identifier
    // because user is probably starting a new statement
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_AssignmentExpression_type_partial() {
    // SimpleIdentifier  TypeName  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('''
      main() {
        int a;
        int^ b = 1;}''');
    // TODO (danrubel) When entering 1st of 2 identifiers on assignment LHS
    // the user may be either (1) entering a type for the assignment
    // or (2) starting a new statement.
    // Consider suggesting only types
    // if only spaces separates the 1st and 2nd identifiers.
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_AssignmentExpression_type_partial_newline() {
    // SimpleIdentifier  TypeName  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('''
      main() {
        int a;
        i^
        b = 1;}''');
    // Allow non-types preceding an identifier on LHS of assignment
    // if newline follows first identifier
    // because user is probably starting a new statement
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_AwaitExpression() {
    // SimpleIdentifier  AwaitExpression  ExpressionStatement
    addTestSource('main() async {A a; await ^}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_BinaryExpression_LHS() {
    // SimpleIdentifier  BinaryExpression  VariableDeclaration
    // VariableDeclarationList  VariableDeclarationStatement
    addTestSource('main() {int a = 1, b = ^ + 2;}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_BinaryExpression_RHS() {
    // SimpleIdentifier  BinaryExpression  VariableDeclaration
    // VariableDeclarationList  VariableDeclarationStatement
    addTestSource('main() {int a = 1, b = 2 + ^;}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_Block() {
    // Block  BlockFunctionBody  MethodDeclaration
    addTestSource('''
      class X {
        a() {
          var f;
          localF(int arg1) { }
          {var x;}
          ^ var r;
        }
      }''');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_Block_empty() {
    // Block  BlockFunctionBody  MethodDeclaration  ClassDeclaration
    addTestSource('class A extends E implements I with M {a() {^}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_Block_identifier_partial() {
    addTestSource('class X {a() {var f; {var x;} D^ var r;} void b() { }}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_Break_after_label() {
    addTestSource('main() { foo: while (true) { break foo ^ ; } }');
    assertOpType(/* No valid completions */);
  }

  test_Break_before_label() {
    addTestSource('main() { foo: while (true) { break ^ foo; } }');
    assertOpType(statementLabel: true);
  }

  test_Break_no_label() {
    addTestSource('main() { foo: while (true) { break ^; } }');
    assertOpType(statementLabel: true);
  }

  test_CascadeExpression_selector1() {
    // PropertyAccess  CascadeExpression  ExpressionStatement  Block
    addTestSource('''
      // looks like a cascade to the parser
      // but the user is trying to get completions for a non-cascade
      main() {A a; a.^.z}''');
    assertOpType(invocation: true);
  }

  test_CascadeExpression_selector2() {
    // SimpleIdentifier  PropertyAccess  CascadeExpression  ExpressionStatement
    addTestSource('main() {A a; a..^z}');
    assertOpType(invocation: true);
  }

  test_CascadeExpression_selector2_withTrailingReturn() {
    // PropertyAccess  CascadeExpression  ExpressionStatement  Block
    addTestSource('main() {A a; a..^ return}');
    assertOpType(invocation: true);
  }

  test_CascadeExpression_target() {
    // SimpleIdentifier  CascadeExpression  ExpressionStatement
    addTestSource('main() {A a; a^..b}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_CatchClause_typed() {
    // Block  CatchClause  TryStatement
    addTestSource('class A {a() {try{var x;} on E catch (e) {^}}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_CatchClause_untyped() {
    // Block  CatchClause  TryStatement
    addTestSource('class A {a() {try{var x;} catch (e, s) {^}}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ClassDeclaration_body() {
    // ClassDeclaration  CompilationUnit
    addTestSource('@deprecated class A {^}');
    assertOpType(typeNames: true);
  }

  test_ClassDeclaration_body2() {
    // SimpleIdentifier  MethodDeclaration  ClassDeclaration
    addTestSource('@deprecated class A {^mth() {}}');
    assertOpType(typeNames: true);
  }

  test_Combinator_hide() {
    // SimpleIdentifier  HideCombinator  ImportDirective
    addTestSource('''
      import "/testAB.dart" hide ^;
      class X {}''');
    assertOpType();
  }

  test_Combinator_show() {
    // SimpleIdentifier  HideCombinator  ImportDirective
    addTestSource('''
      import "/testAB.dart" show ^;
      import "/testCD.dart";
      class X {}''');
    assertOpType();
  }

  test_CommentReference() {
    // SimpleIdentifier  CommentReference  Comment  MethodDeclaration
    addTestSource('class A {/** [^] */ mth() {}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ConditionalExpression_elseExpression() {
    // SimpleIdentifier  ConditionalExpression  ReturnStatement
    addTestSource('class C {foo(){var f; {var x;} return a ? T1 : T^}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ConditionalExpression_elseExpression_empty() {
    // SimpleIdentifier  ConditionalExpression  ReturnStatement
    addTestSource('class C {foo(){var f; {var x;} return a ? T1 : ^}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ConditionalExpression_partial_thenExpression() {
    // SimpleIdentifier  ConditionalExpression  ReturnStatement
    addTestSource('class C {foo(){var f; {var x;} return a ? T^}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ConditionalExpression_partial_thenExpression_empty() {
    // SimpleIdentifier  ConditionalExpression  ReturnStatement
    addTestSource('class C {foo(){var f; {var x;} return a ? ^}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ConditionalExpression_thenExpression() {
    // SimpleIdentifier  ConditionalExpression  ReturnStatement
    addTestSource('class C {foo(){var f; {var x;} return a ? T^ : c}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ConstructorName() {
    // SimpleIdentifier  PrefixedIdentifier  TypeName  ConstructorName
    // InstanceCreationExpression
    addTestSource('main() {new X.^}');
    assertOpType(invocation: true);
  }

  test_ConstructorName_name_resolved() {
    // SimpleIdentifier  PrefixedIdentifier  TypeName  ConstructorName
    // InstanceCreationExpression
    addTestSource('main() {new Str^ing.fromCharCodes([]);}', resolved: true);
    assertOpType(typeNames: true);
  }

  test_ConstructorName_resolved() {
    // SimpleIdentifier  PrefixedIdentifier  TypeName  ConstructorName
    // InstanceCreationExpression
    addTestSource('main() {new String.fr^omCharCodes([]);}', resolved: true);
    assertOpType(invocation: true);
  }

  test_ConstructorName_unresolved() {
    // SimpleIdentifier  PrefixedIdentifier  TypeName  ConstructorName
    // InstanceCreationExpression
    addTestSource('main() {new String.fr^omCharCodes([]);}');
    assertOpType(invocation: true);
  }

  test_Continue_after_label() {
    addTestSource('main() { foo: while (true) { continue foo ^ ; } }');
    assertOpType(/* No valid completions */);
  }

  test_Continue_before_label() {
    addTestSource('main() { foo: while (true) { continue ^ foo; } }');
    assertOpType(statementLabel: true, caseLabel: true);
  }

  test_Continue_no_label() {
    addTestSource('main() { foo: while (true) { continue ^; } }');
    assertOpType(statementLabel: true, caseLabel: true);
  }

  test_DefaultFormalParameter_named_expression() {
    // DefaultFormalParameter FormalParameterList MethodDeclaration
    addTestSource('class A {a(blat: ^) { }}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_DoStatement() {
    // SimpleIdentifier  DoStatement  Block
    addTestSource('main() {do{} while(^x);}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ExpressionFunctionBody() {
    // SimpleIdentifier  ExpressionFunctionBody  FunctionExpression
    addTestSource('m(){[1].forEach((x)=>^x);}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ExpressionStatement() {
    // ExpressionStatement  Block  BlockFunctionBody
    addTestSource('n(){f(3);^}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ExpressionStatement_name() {
    // ExpressionStatement  Block  BlockFunctionBody  MethodDeclaration
    addTestSource('class C {a() {C ^}}');
    assertOpType();
  }

  test_ExtendsClause() {
    // ExtendsClause  ClassDeclaration
    addTestSource('class x extends ^\n{}');
    assertOpType(typeNames: true);
  }

  test_FieldDeclaration_name_typed() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // FieldDeclaration
    addTestSource('class C {A ^}');
    assertOpType();
  }

  test_FieldDeclaration_name_var() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // FieldDeclaration
    addTestSource('class C {var ^}');
    assertOpType();
  }

  test_ForEachStatement() {
    // SimpleIdentifier  ForEachStatement  Block
    addTestSource('main() {for(z in ^zs) {}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ForEachStatement_body_typed() {
    // Block  ForEachStatement
    addTestSource('main(args) {for (int foo in bar) {^}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ForEachStatement_body_untyped() {
    // Block  ForEachStatement
    addTestSource('main(args) {for (foo in bar) {^}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ForEachStatement_iterable() {
    // SimpleIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (int foo in ^) {}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ForEachStatement_loopVariable() {
    // SimpleIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (^ in args) {}}');
    assertOpType(typeNames: true);
  }

  test_ForEachStatement_loopVariable_name() {
    // DeclaredIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (String ^ in args) {}}');
    assertOpType();
  }

  test_ForEachStatement_loopVariable_name2() {
    // DeclaredIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (String f^ in args) {}}');
    assertOpType();
  }

  test_ForEachStatement_loopVariable_type() {
    // SimpleIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (^ foo in args) {}}');
    assertOpType(typeNames: true);
  }

  test_ForEachStatement_loopVariable_type2() {
    // DeclaredIdentifier  ForEachStatement  Block
    addTestSource('main(args) {for (S^ foo in args) {}}');
    assertOpType(typeNames: true);
  }

  test_FormalParameterList() {
    // FormalParameterList MethodDeclaration
    addTestSource('class A {a(^) { }}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ForStatement_condition() {
    // SimpleIdentifier  ForStatement
    addTestSource('main() {for (int index = 0; i^)}');
    // TODO (danrubel) may want to exclude methods/functions with void return
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ForStatement_initializer() {
    // SimpleIdentifier  ForStatement
    addTestSource('main() {List a; for (^)}');
    // TODO (danrubel) may want to exclude methods/functions with void return
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ForStatement_updaters() {
    // SimpleIdentifier  ForStatement
    addTestSource('main() {for (int index = 0; index < 10; i^)}');
    // TODO (danrubel) may want to exclude methods/functions with void return
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_ForStatement_updaters_prefix_expression() {
    // SimpleIdentifier  PrefixExpression  ForStatement
    addTestSource('main() {for (int index = 0; index < 10; ++i^)}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_FunctionTypeAlias() {
    // SimpleIdentifier  FunctionTypeAlias  CompilationUnit
    addTestSource('typedef n^ ;');
    assertOpType(typeNames: true);
  }

  test_IfStatement() {
    // EmptyStatement  IfStatement  Block  BlockFunctionBody
    addTestSource('main(){var a; if (true) ^}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_IfStatement_condition() {
    // SimpleIdentifier  IfStatement  Block  BlockFunctionBody
    addTestSource('main(){var a; if (^)}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_IfStatement_empty() {
    // SimpleIdentifier  PrefixIdentifier  IfStatement
    addTestSource('class A {foo() {A a; if (^) something}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_IfStatement_invocation() {
    // SimpleIdentifier  PrefixIdentifier  IfStatement
    addTestSource('main() {var a; if (a.^) something}');
    assertOpType(invocation: true);
  }

  test_ImplementsClause() {
    // ImplementsClause  ClassDeclaration
    addTestSource('class x implements ^\n{}');
    assertOpType(typeNames: true);
  }

  test_ImportDirective_dart() {
    // SimpleStringLiteral  ImportDirective
    addTestSource('''
      import "dart^";
      main() {}''');
    assertOpType();
  }

  test_IndexExpression() {
    addTestSource('class C {foo(){var f; {var x;} f[^]}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_IndexExpression2() {
    addTestSource('class C {foo(){var f; {var x;} f[T^]}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_InstanceCreationExpression_imported() {
    // SimpleIdentifier  TypeName  ConstructorName  InstanceCreationExpression
    addTestSource('class C {foo(){var f; {var x;} new ^}}');
    assertOpType(typeNames: true);
  }

  test_InstanceCreationExpression_keyword() {
    // InstanceCreationExpression  ExpressionStatement  Block
    addTestSource('class C {foo(){var f; {var x;} new^ }}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_InstanceCreationExpression_keyword2() {
    // InstanceCreationExpression  ExpressionStatement  Block
    addTestSource('class C {foo(){var f; {var x;} new^ C();}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_InterpolationExpression() {
    // SimpleIdentifier  InterpolationExpression  StringInterpolation
    addTestSource('main() {String name; print("hello \$^");}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_InterpolationExpression_block() {
    // SimpleIdentifier  InterpolationExpression  StringInterpolation
    addTestSource('main() {String name; print("hello \${n^}");}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_InterpolationExpression_prefix_selector() {
    // SimpleIdentifier  PrefixedIdentifier  InterpolationExpression
    addTestSource('main() {String name; print("hello \${name.^}");}');
    assertOpType(invocation: true);
  }

  test_InterpolationExpression_prefix_target() {
    // SimpleIdentifier  PrefixedIdentifier  InterpolationExpression
    addTestSource('main() {String name; print("hello \${nam^e.length}");}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_IsExpression() {
    // SimpleIdentifier  TypeName  IsExpression  IfStatement
    addTestSource('main() {var x; if (x is ^) { }}');
    assertOpType(typeNames: true);
  }

  test_IsExpression_target() {
    // IfStatement  Block  BlockFunctionBody
    addTestSource('main(){var a; if (^ is A)}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_IsExpression_type_partial() {
    // SimpleIdentifier  TypeName  IsExpression  IfStatement
    addTestSource('main(){var a; if (a is Obj^)}');
    assertOpType(typeNames: true);
  }

  test_Literal_list() {
    // ']'  ListLiteral  ArgumentList  MethodInvocation
    addTestSource('main() {var Some; print([^]);}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_Literal_list2() {
    // SimpleIdentifier ListLiteral  ArgumentList  MethodInvocation
    addTestSource('main() {var Some; print([S^]);}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_Literal_string() {
    // SimpleStringLiteral  ExpressionStatement  Block
    addTestSource('class A {a() {"hel^lo"}}');
    assertOpType();
  }

  test_MethodDeclaration1() {
    // SimpleIdentifier  MethodDeclaration  ClassDeclaration
    addTestSource('class Bar {const ^Fara();}');
    assertOpType(typeNames: true);
  }

  test_MethodDeclaration2() {
    // SimpleIdentifier  MethodDeclaration  ClassDeclaration
    addTestSource('class Bar {const F^ara();}');
    assertOpType(typeNames: true);
  }

  test_MethodInvocation_no_semicolon() {
    // MethodInvocation  ExpressionStatement  Block
    addTestSource('''
      class A implements I {
        // no semicolon between completion point and next statement
        set _s2(I x) {x.^ m(null);}
      }''');
    assertOpType(invocation: true);
  }

  test_PrefixedIdentifier_class_const() {
    // SimpleIdentifier PrefixedIdentifier ExpressionStatement Block
    addTestSource('main() {A.^}');
    assertOpType(invocation: true);
  }

  test_PrefixedIdentifier_class_imported() {
    // SimpleIdentifier  PrefixedIdentifier  ExpressionStatement
    addTestSource('main() {A a; a.^}');
    assertOpType(invocation: true);
  }

  test_PrefixedIdentifier_prefix() {
    // SimpleIdentifier  PrefixedIdentifier  ExpressionStatement
    addTestSource('class X {foo(){A^.bar}}');
    assertOpType(typeNames: true, returnValue: true, voidReturn: true);
  }

  test_PropertyAccess_expression() {
    // SimpleIdentifier  MethodInvocation  PropertyAccess  ExpressionStatement
    addTestSource('class A {a() {"hello".to^String().length}}');
    assertOpType(invocation: true);
  }

  test_PropertyAccess_selector() {
    // SimpleIdentifier  PropertyAccess  ExpressionStatement  Block
    addTestSource('class A {a() {"hello".length.^}}');
    assertOpType(invocation: true);
  }

  test_ReturnStatement() {
    // ReturnStatement  Block
    addTestSource('f() { var vvv = 42; return ^ }');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_SimpleFormalParameter() {
    // SimpleIdentifier  SimpleFormalParameter  FormalParameterList
    addTestSource('mth() { PNGS.sort((String a, Str^) => a.compareTo(b)); }');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_SwitchCase() {
    // SimpleIdentifier  SwitchCase  SwitchStatement
    addTestSource('''m() {switch (x) {case ^D: return;}}''');
    // TODO (danrubel) should refine this to return constants
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_SwitchStatement() {
    // SimpleIdentifier  SwitchStatement  Block
    addTestSource('main() {switch(^k) {case 1:{}}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_ThisExpression_block() {
    // MethodInvocation  ExpressionStatement  Block
    addTestSource('''
      class A implements I {
        // no semicolon between completion point and next statement
        set s1(I x) {} set _s2(I x) {this.^ m(null);}
      }''');
    assertOpType(invocation: true);
  }

  test_ThisExpression_constructor() {
    // MethodInvocation  ExpressionStatement  Block
    addTestSource('''
      class A implements I {
        A() {this.^}
      }''');
    assertOpType(invocation: true);
  }

  test_TopLevelVariableDeclaration_typed_name() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // TopLevelVariableDeclaration
    addTestSource('class A {} B ^');
    assertOpType();
  }

  test_TopLevelVariableDeclaration_untyped_name() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // TopLevelVariableDeclaration
    addTestSource('class A {} var ^');
    assertOpType();
  }

  test_TypeParameter() {
    // SimpleIdentifier  TypeParameter  TypeParameterList
    addTestSource('class tezetst <String, ^List> {}');
    assertOpType();
  }

  test_TypeParameterList_empty() {
    // SimpleIdentifier  TypeParameter  TypeParameterList
    addTestSource('class tezetst <^> {}');
    assertOpType();
  }

  test_VariableDeclaration_name() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // VariableDeclarationStatement  Block
    addTestSource('main() {var ^}');
    assertOpType();
  }

  test_VariableDeclarationStatement_afterSemicolon() {
    // VariableDeclarationStatement  Block  BlockFunctionBody
    addTestSource('class A {var a; x() {var b;^}}');
    assertOpType(returnValue: true, typeNames: true, voidReturn: true);
  }

  test_VariableDeclarationStatement_RHS() {
    // SimpleIdentifier  VariableDeclaration  VariableDeclarationList
    // VariableDeclarationStatement
    addTestSource('class C {bar(){var f; {var x;} var e = ^}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_VariableDeclarationStatement_RHS_missing_semicolon() {
    // VariableDeclaration  VariableDeclarationList
    // VariableDeclarationStatement
    addTestSource('class C {bar(){var f; {var x;} var e = ^ var g}}');
    assertOpType(returnValue: true, typeNames: true);
  }

  test_WhileStatement() {
    // SimpleIdentifier  WhileStatement  Block
    addTestSource('mth() { while (b^) {} }}');
    assertOpType(returnValue: true, typeNames: true);
  }
}

class _TestSource implements Source {
  String fullName;
  _TestSource(this.fullName);

  @override
  bool get isInSystemLibrary => false;

  @override
  String get shortName => fullName;

  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
