// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('EvaluatorTest');

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../evaluator.dart');
// TODO(nweiz): don't depend on Node for these tests
#import('../../../lib/node/node.dart');
#import('../../../file_system_node.dart');
#import('../../../js_evaluator_node.dart');

main() {
  useNodeConfiguration();
  // TODO(nweiz): This won't work if we aren't running through frogsh.
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));
  Evaluator.initWorld(homedir, [], new NodeFileSystem());

  evaluator() => new Evaluator(new NodeJsEvaluator());
  eval(String dart) => evaluator().eval(dart);

  group('Evaluation works correctly for', () {
    test('simple expressions', () {
      Expect.equals(3, eval('1 + 2'));
      Expect.equals('foobar', eval('"foo" + "bar"'));
    });

    test('built-in methods', () {
      Expect.equals(2, eval('(2.3).round()'));
      Expect.isTrue(eval('"foobar".contains("oba")'));
    });

    test('user-defined top-level functions', () {
      var ev = evaluator();
      ev.eval('foo(a) => a + 2');
      Expect.equals(3, ev.eval('foo(1)'));

      ev.eval('bar(b) => "foo" + foo(b)');
      Expect.equals("foo3", ev.eval('bar(1)'));
    });

    test('user-defined variables', () {
      var ev = evaluator();
      ev.eval('a = 3');
      Expect.equals(5, ev.eval('2 + a'));

      ev.eval('b = a + 4');
      Expect.equals(21, ev.eval('b * 3'));
    });

    test('redefining variables', () {
      var ev = evaluator();
      ev.eval('a = 3');
      Expect.equals(3, ev.eval('a'));
      ev.eval('a = 4');
      Expect.equals(4, ev.eval('a'));
    });

    test('redefining functions', () {
      var ev = evaluator();
      ev.eval('int foo() => 3');
      Expect.equals(3, ev.eval('foo()'));
      ev.eval('String foo(int a) => "foo" + a');
      Expect.equals("foo12", ev.eval('foo(12)'));
    });

    test('user-defined classes', () {
      var ev = evaluator();
      ev.eval('class Foo { int a; Foo(this.a); foo(b) => a + b; }');
      ev.eval('f = new Foo(5)');
      Expect.equals(5, ev.eval('f.a'));
      Expect.equals(9, ev.eval('f.foo(4)'));
    });

    test('list literals', () {
      var ev = evaluator();
      // Coerce to a string because the evaluation context can define methods
      // differently than the primary context.
      Expect.equals('1,2,3', ev.eval('[1, 2, 3].toString()'));
    });

    test('map literals', () {
      var ev = evaluator();
      // Coerce to a string because the evaluation context can define methods
      // differently than the primary context.
      Expect.equals('a,b', ev.eval("({'a': 1, 'b': 2}).getKeys().toString()"));
      Expect.equals('1,2', ev.eval("({'a': 1, 'b': 2}).getValues().toString()"));
    });
  });

  group('The parser is flexible enough to', () {
    test('allow semicolons or not', () {
      Expect.equals(2, eval('1 + 1;'));
      Expect.equals(2, eval('1 + 1'));
    });

    test('allow "var" or not', () {
      var ev = evaluator();
      ev.eval('var a = 1');
      Expect.equals(1, ev.eval('a'));
      ev.eval('b = 2');
      Expect.equals(2, ev.eval('b'));
    });

    // TODO(nweiz): make this work
    // test('parse maps with or without parentheses', () {
    //   var ev = evaluator();
    //   // Coerce to a string because the evaluation context can define methods
    //   // differently than the primary context.
    //   Expect.equals('a,b', ev.eval("{'a': 1, 'b': 2}.getKeys().toString()"));
    //   Expect.equals('a,b', ev.eval("({'a': 1, 'b': 2}).getKeys().toString()"));
    // });
  });
}
