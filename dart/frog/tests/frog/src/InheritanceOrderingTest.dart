// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('InheritanceOrderingTest');

/** 
 * This test ensures the order that the javascript classes that are printed out 
 * when using frog are ordered such that the prototype is always printed first.
 * This is important for Internet Explorer to work since we can't just modify
 * __proto__.
 */

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../lang.dart');
#import('../../../file_system_node.dart');
#import('../../../lib/node/node.dart');

class D extends B {
  String msg() => 'd';
}

class B extends A {
  String msg() => 'b';
}

class A {
  String msg() => 'a';
}

class C extends A {
  String msg() => 'c';
}

class G extends F {
  String msg() => 'g';
}

class E {
  String msg() => 'e';
}

class H extends E {
  String msg() => 'h';
}

class F extends E {
  String msg() => 'f';
}

main() {
  useNodeConfiguration();

  // Get the home directory from our executable.
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));

  var argv = new List.from(process.argv);
  argv.add('--compile-only');
  argv.add('tests/frog/src/InheritanceOrderingTest.dart');

  parseOptions(homedir, argv, new NodeFileSystem());
  initializeWorld(new NodeFileSystem());
 
  world.runCompilationPhases(); 
  var code = world.getGeneratedCode();
  A a = new A();
  B b = new B();
  C c = new C();
  D d = new D();
  String foo = a.msg();
  // These are called simply so that the classes don't get optimized away.
  foo += b.msg();
  foo += c.msg();
  foo += d.msg();


  G g = new G();
  E e = new E();
  H h = new H();
  F f = new F();
  foo += e.msg();
  foo += f.msg();
  foo += g.msg();
  foo += h.msg();

  // Ensure that class prototypes are printed in an appropriate order.
  test('class prototype order', () {
    Expect.equals(true, 
        code.indexOf('function A() {') < code.indexOf('function B() {'));
    Expect.equals(true, 
        code.indexOf('function B() {') < code.indexOf('function D() {'));
    Expect.equals(true, 
        code.indexOf('function A() {') < code.indexOf('function C() {'));
    Expect.equals(true, 
        code.indexOf('function E() {') < code.indexOf('function F() {'));
    Expect.equals(true, 
        code.indexOf('function F() {') < code.indexOf('function G() {'));
    Expect.equals(true, 
        code.indexOf('function E() {') < code.indexOf('function H() {'));
  });
  
  // Ensure that the $iherits function is printed in the correct order related
  // to the class declaration
  test('inherit statement ordering', () {
    Expect.equals(true, 
        code.indexOf('\$inherits(F, E)') < code.indexOf('function F() {'));
    Expect.equals(true, 
        code.indexOf('\$inherits(G, F)') < code.indexOf('function G() {'));
    Expect.equals(true, 
        code.indexOf('\$inherits(B, A)') < code.indexOf('function B() {'));
  });
}

