// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ParserTest {
  static final String TAIL = '//comment\n\n';

  static main() {
    print('start ParserTest');
    initializeWorld(new FileSystem('.'));
    testStatements();
    testLambdas();
    testExpressionsAsStatements();
    testDeclarationStatements();
    testTypeAmbiguities();

    testDeclarations();

    testLiterals();
    print('finished ParserTest');
  }

  static testStatements() {
    testStatement('a - b - c - d;');

    testStatement('if (true) return 1*2+3*4; else return 2;');
    testStatement('return 42 ? true : false;');
    testStatement('return true += 2+6*3;');

    testStatement('int x;');
    testStatement('x.m y = 42, z;');
    testStatement('{ int x; return 22; }');
    testStatement('int f() { return 22; }');

    testStatement('int f(int y) { return 22 + 3; }');
    testStatement('int f(int y, int x) { return 22 + x; }');

    testStatement('int f(y, x) { return 22 + x; }');
    testStatement('foo() => 42;');

    testStatement('throw;');
    testStatement('throw x(10);');

    testStatement('break;');
    testStatement('continue;');
    testStatement('assert(true);');
    testStatement('assert(x < 10);');
    testStatement('break foo;');
    testStatement('continue bar;');

    testStatement('var z=22, a=new C(1,2);');

    testStatement('for (Map<String, Object> suite in stats) {}');
  }

  static testLambdas() {
    testStatement('x = function() {};');
    testStatement('x = function() => 42;');
    testStatement('x = void f() {};');
    testStatement('x = func() => 42;');
    testStatement('x = void f(int x, int y) {};');
    testStatement('x = func(x) => 42;');
    testStatement('x = func(x,y,z) => 42;');

    testStatement('x = func(x);');
    testStatement('x = func(x,y,z);');
    testStatement('x = func(42);');
    testStatement('x = func(x,y,42);');


    testStatement('f(x = function() {});');
    testStatement('f(x = function() => 42);');
    testStatement('f(x = void f() {});');
    testStatement('f(x = func() => 42);');

    testStatement('f(foo() {});');

    testStatement('f(void func(KV<K,V> e) {});');
  }

  static testExpressionsAsStatements() {
    testStatement('x + 1;');

    testStatement('(2+3);');
    testStatement('x.m(10);');
    testStatement('x(10);');

    // TODO(jimhug): This is illegal - add as negative test...
    //testStatement('var c = z=22, a=new C(1,2);');

    testStatement('-a++;'); // TODO(jimhug): Ensure this parses as -(a++).

    testStatement('[(2+3), x(10), ++x, x++, --y, y--, x[42]];');
    testStatement('var m = {"a":42, "b":f(76),};');
    testStatement('var m = {};');
    testStatement('var m = {"a":"bye"};');
    testStatement('var m = {"a":42, "b":f(76)};');
    testStatement('var c = const [1,2];');
    testStatement('var c = const {"1":2};');

    testStatement('final m = const <int>[1,2,3];');
    testStatement('final m = const <String, int>{"a":1,"b":2,"c":3};');

    testStatement('if (a is double) {}');
    testStatement('if ((a is double) && b(10)) {}');

    testStatement('try { foo(); } catch (e) {} catch (e, t) {} finally {}');
    testStatement('try { foo(); } catch (Ex1 e) {} catch (Ex2 e, St t) {}');

    testStatement('for (i in set) {}');
    testStatement('for (final i in set) {}');
    testStatement('for (var i in set) {}');
    testStatement('for (final int i in set) {}');

    testStatement('''switch (e) {
                       case 1:
                         foo();
                         break;
                       case 2: case 3:
                         bar(); return 10;
                       default: return null;
                     }''');

    // operator fun
    testStatement('var o = x >> 2;');
    testStatement('var o = x >>> 2;');

    testStatement('x >>= 2;');
    testStatement('x >>>= 2;');
    // TODO - need negatives!

    testStatement('var kv = new KeyValuePair<K, V>(key, value);');

    testStatement('array.sort((a, b) => a < b);');

    testStatement(';');
    testStatement('{;;;;;;;;;}');

    testStatement('print("hello");');
    testStatement('print("hello \$foo");');
    testStatement('print("\${2 + x} = \${y(\'bar\')}");');
  }

  static testDeclarationStatements() {
    testStatement('int m() {}');
    testStatement('m() {}');
    testStatement('m(x) {}');

    testStatement('m(A<B> o) {}');

    testStatement('m(A<B<C>> z) {}');

    testStatement('A.B<C> m(int x, y, double z) {}');
    testStatement('A.B<C> m(int x, y, A<B<C>> z) {}');

    testStatement('int x;');
    testStatement('A.B<C> x = 2+5;');
    testStatement('A.B<C> x = 2+5, y, z=42;');

    testStatement('void f(e) { }');

    testStatement('Promise<int> a = new Promise<int>();');
    testStatement('Promise<Promise<int>> b = new Promise<Promise<int>>();');
    testStatement('P<P<P<int>>> c = new P<P<P<int>>>();');
    testStatement('P<P<P<P<int>>>> c = new P<P<P<P<int>>>>();');
  }


  static testDeclarations() {
    testDeclaration('int m();');
    testDeclaration('m();');
    testDeclaration('m(x) {}');
    testDeclaration('A.B<C> m(int x, y, double z);');


    testDeclaration('A.B<C> m(int x, y, A<B<C>> z);');

    testDeclaration('int x;');
    testDeclaration('A.B<C> x = 2+5;');
    testDeclaration('A.B<C> x = 2+5, y, z=42;');

    testDeclaration('operator +(other) {}');
    testDeclaration('operator [](index) {}');
    testDeclaration('operator []=(index, value) {}');

    testDeclaration('foo() => 42;');

    testDeclaration('''class C extends D implements I {
                         void m() { return 43; }
                         int x;
                       }''');

    testDeclaration('interface I { operator +(y) { return y + 10; }}');
    testDeclaration('interface I { int operator +(y) { return y + 10; }}');

    testDeclaration('HashMap<K, DLLE<KeyValuePair<K, V>>> m_;');

    // pseudo keywords?
    testDeclaration('foo(A source) {}');
    testDeclaration('foo(A get, B assert, C static, D extends) {}');

    testDeclaration('C(): b=x+y {}');

    testDeclaration('C(): z=22, a=new C(1,2) {}');
    testDeclaration('C(b): _b = b == null ? 10 : 20 {}');

    testDeclaration('C(this.x, this.y) {}');

    testDeclaration('C(this.x, this.y): z=22, a=new C(1,2), b=x+y {}');

    testDeclaration('void f(e) { }');
  }

  static testTypeAmbiguities() {
    testMaybeType('A.B.C', false, false);
    testMaybeType('A.B.C<10', false, true);
    testMaybeType('A.B.C<D', false, true);
    testMaybeType('A.B.C<D>', true, false);
    testMaybeType('A.B.C<D,E>', true, false);
    testMaybeType('A.B.C<D,E,F>', true, false);
    testMaybeType('A.B.C<D,E,F,G>', true, false);

    testMaybeType('A<B1,B2<C>,D>', true, false);

    testMaybeType('A<B<C>,D>', true, false);
    testMaybeType('A<B1,B2<C>,D>', true, false);
    testMaybeType('A<B<C>>', true, false);
    testMaybeType('A<B<C<D>>>', true, false);

    testMaybeType('KeyValuePair<K, V>', true, false);
    testMaybeType('QueueEntry<KeyValuePair<K, V> >', true, false);
    testMaybeType('QueueEntry<KeyValuePair<K, V>>', true, false);
    testMaybeType('HashMap<K, QueueEntry<KeyValuePair<K, V> > >',
                  true, false);
    testMaybeType('HashMap<K, QueueEntry<KeyValuePair<K, V>>>',
                  true, false);
  }


  static testLiterals() {
    testLiteral('2', 2);
    testLiteral('10e100', 10e100);
    testLiteral('3.14159265', 3.14159265);
    testLiteral('0x0', 0);
    testLiteral('0xa8', 168);
    testLiteral('0xfF', 255);
    testLiteral('0XfF', 255);

    testLiteral('0xabcdef', 11259375);
    testLiteral('0xABCDEF', 11259375);
    // TODO(jimhug): This is too big for JS int
    //testLiteral('0x123456789', 4886718345);
    testLiteral('0x12345678', 305419896);
  }

  static Parser makeParser(String filename, String code) {
    return new Parser(new SourceFile(filename, code));
  }

  static void assertEqual(a, b) {
    Expect.equals(a, b);
  }

  static void validateNode(Node node, String filename, String code) {
    // first, confirm sourcespan exists and is "correct"
    var span = node.span;
    assertEqual(span.file.filename, filename);
    assertEqual(span.start, 0);
    assertEqual(span.end, code.length);

    // TODO(jimhug): Walk and validate children spans and contents.
  }

  static void testLiteral(String expr, var expected) {
    print('>>>${expr}<<<');
    var filename = 'expr.dart';
    final parser = makeParser(filename, expr + TAIL);
    var node = parser.expression();
    validateNode(node, filename, expr);
    parser.checkEndOfFile();

    assertEqual(expected, node.value);
  }

  static void testDeclaration(String code, [String filename='test.dart']) {
    print('>>>${code}<<<');
    final parser = makeParser(filename, code + TAIL);
    var decl = parser.topLevelDefinition();
    validateNode(decl, filename, code);
    parser.checkEndOfFile();
  }

  static void testStatement(String code, [String filename='test.dart']) {
    print('>>>${code}<<<');
    final parser = makeParser(filename, code + TAIL);
    var node = parser.statement();
    validateNode(node, filename, code);
    parser.checkEndOfFile();
  }

  static void testMaybeType(String code, bool mustBeType,
                            bool mustBeExpression) {
    if (!mustBeType) {
      testStatement('x + ' + code + ';');
    }
    if (!mustBeExpression) {
      testStatement(code + ' x;');
      testDeclaration(code + ' x;');
    }
    // TODO(jimhug): add negative test cases.
  }
}
