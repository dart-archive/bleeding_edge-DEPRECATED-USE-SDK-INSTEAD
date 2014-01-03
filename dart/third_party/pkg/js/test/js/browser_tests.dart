// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library js_tests;

import 'dart:html';

import 'package:js/js.dart' as js;
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';

class Foo implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  Foo(num a) : this._proxy = new js.Proxy(js.context.Foo, a);

  js.Proxy toJs() => _proxy;

  num get a => _proxy.a;
  num bar() => _proxy.bar();
}

class Color implements js.Serializable<String> {
  static final RED = new Color._("red");
  static final BLUE = new Color._("blue");
  String _value;
  Color._(this._value);
  String toJs() => this._value;
}

main() {
  useHtmlConfiguration();

  test('read global field', () {
    expect(js.context.x, equals(42));
    expect(js.context['x'], equals(42));
    expect(() => js.context.y, throwsA(isNoSuchMethodError));
  });

  test('read global field with underscore', () {
    expect(js.context._x, equals(123));
    expect(js.context['_x'], equals(123));
    expect(() => js.context._y, throwsA(isNoSuchMethodError));
    expect(js.context['_y'], equals(null));
  });

  test('js instantiation : new Foo()', () {
    final Foo2 = js.context.container.Foo;
    final foo = new js.Proxy(Foo2, 42);
    expect(foo.a, 42);
    expect(Foo2.b, 38);
  });

  test('js instantiation : new Array()', () {
    final a = new js.Proxy(js.context.Array);
    expect(a, isNotNull);
    expect(a.length, equals(0));

    a.push("value 1");
    expect(a.length, equals(1));
    expect(a[0], equals("value 1"));

    a.pop();
    expect(a.length, equals(0));
  });

  test('js instantiation : new Date()', () {
    final a = new js.Proxy(js.context.Date);
    expect(a.getTime(), isNotNull);
  });

  test('js instantiation : new Date(int)', () {
    final a = new js.Proxy(js.context.Date, 12345678);
    expect(a.getTime(), equals(12345678));
  });

  test('js instantiation : new Date(String)', () {
    final a = new js.Proxy(js.context.Date,
        "December 17, 1995 03:24:00 GMT+01:00");
    expect(a.getTime(), equals(819167040000));
  });

  test('js instantiation : new Date(int, int, int)', () {
    // Note: JS Date counts months from 0 while Dart counts from 1.
    final a = new js.Proxy(js.context.Date, 1995, 11, 17);
    final b = new DateTime(1995, 12, 17);
    expect(a.getTime(), equals(b.millisecondsSinceEpoch));
  });

  test('js instantiation : new Date(1995,11,17,3,24,0)', () {
    // Note: JS Date counts months from 0 while Dart counts from 1.
    final a = new js.Proxy.withArgList(js.context.Date,
        [1995, 11, 17, 3, 24, 0]);
    final b = new DateTime(1995, 12, 17, 3, 24, 0);
    expect(a.getTime(), equals(b.millisecondsSinceEpoch));
  });

  test('js instantiation : new Object()', () {
    final a = new js.Proxy(js.context.Object);
    expect(a, isNotNull);

    a.attr = "value";
    expect(a.attr, equals("value"));
  });

  test(r'js instantiation : new RegExp("^\w+$")', () {
    final a = new js.Proxy(js.context.RegExp, r'^\w+$');
    expect(a, isNotNull);
    expect(a.test('true'), isTrue);
    expect(a.test(' false'), isFalse);
  });

  test('js instantiation via map notation : new Array()', () {
    final a = new js.Proxy(js.context['Array']);
    expect(a, isNotNull);
    expect(a['length'], equals(0));

    a.push("value 1");
    expect(a['length'], equals(1));
    expect(a[0], equals("value 1"));

    a.pop();
    expect(a['length'], equals(0));
  });

  test('js instantiation via map notation : new Date()', () {
    final a = new js.Proxy(js.context['Date']);
    expect(a.getTime(), isNotNull);
  });

  test('js instantiation : typed array', () {
    final codeUnits = "test".codeUnits;
    final buf = new js.Proxy(js.context.ArrayBuffer, codeUnits.length);
    final bufView = new js.Proxy(js.context.Uint8Array, buf);
    for (var i = 0; i < codeUnits.length; i++) {
      bufView[i] = codeUnits[i];
    }
  });

  test('write global field', () {
    js.context.y = 42;
    expect(js.context.y, equals(42));
    expect(js.context['y'], equals(42));
  });

  test('get JS FunctionProxy', () {
    var razzle = js.context.razzle;
    expect(razzle(), equals(42));
  });

  test('call JS function', () {
    expect(js.context.razzle(), equals(42));
    expect(() => js.context.dazzle(), throwsA(isNoSuchMethodError));
  });

  test('call JS function via map notation', () {
    expect(js.context['razzle'](), equals(42));
    expect(() => js.context['dazzle'](), throwsA(isNoSuchMethodError));
  });

  test('call JS function with varargs', () {
    expect(js.context.varArgs(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
      equals(55));
  });

  test('allocate JS object', () {
    var foo = new js.Proxy(js.context.Foo, 42);
    expect(foo.a, equals(42));
    expect(foo.bar(), equals(42));
    expect(() => foo.baz(), throwsA(isNoSuchMethodError));
  });

  test('call toString()', () {
    var foo = new js.Proxy(js.context.Foo, 42);
    expect(foo.toString(), equals("I'm a Foo a=42"));
    var container = js.context.container;
    expect(container.toString(), equals("[object Object]"));
  });

  test('allocate simple JS array', () {
    final list = [1, 2, 3, 4, 5, 6, 7, 8];
    var array = js.array(list);
    expect(js.context.isArray(array), isTrue);
    expect(array.length, equals(list.length));
    for (var i = 0; i < list.length ; i++) {
      expect(array[i], equals(list[i]));
    }
  });

  test('allocate JS array with iterable', () {
    final set = new Set.from([1, 2, 3, 4, 5, 6, 7, 8]);
    var array = js.array(set);
    expect(js.context.isArray(array), isTrue);
    expect(array.length, equals(set.length));
    for (var i = 0; i < array.length ; i++) {
      expect(set.contains(array[i]), isTrue);
    }
  });

  test('allocate simple JS map', () {
    var map = {'a': 1, 'b': 2, 'c': 3};
    var jsMap = js.map(map);
    expect(!js.context.isArray(jsMap), isTrue);
    for (final key in map.keys) {
      expect(js.context.checkMap(jsMap, key, map[key]), isTrue);
    }
  });

  test('allocate complex JS object', () {
    final object =
      {
        'a': [1, [2, 3]],
        'b': {
          'c': 3,
          'd': new js.Proxy(js.context.Foo, 42)
        },
        'e': null
      };
    var jsObject = js.map(object);
    expect(jsObject['a'][0], equals(object['a'][0]));
    expect(jsObject['a'][1][0], equals(object['a'][1][0]));
    expect(jsObject['a'][1][1], equals(object['a'][1][1]));
    expect(jsObject['b']['c'], equals(object['b']['c']));
    expect(jsObject['b']['d'], equals(object['b']['d']));
    expect(jsObject['b']['d'].bar(), equals(42));
    expect(jsObject['e'], isNull);
  });

  test('invoke Dart callback from JS(with js.Callback)', () {
    expect(() => js.context.invokeCallback(), throws);

    js.context.callback = () => 42;
    expect(js.context.invokeCallback(), 42);

    js.deleteProperty(js.context, 'callback');
  });

  test('invoke Dart callback from JS', () {
    expect(() => js.context.invokeCallback(), throws);

    js.context.callback = () => 42;
    expect(js.context.invokeCallback(), equals(42));

    js.deleteProperty(js.context, 'callback');
  });

  test('callback as parameter', () {
    expect(js.context.getTypeOf(js.context.razzle), equals("function"));
  });

  test('invoke Dart callback from JS with this', () {
    final constructor = new js.FunctionProxy.withThis(($this, arg1) {
      $this.a = 42;
      $this.b = js.array(["a", arg1]);
    });
    var o = new js.Proxy(constructor, "b");
    expect(o.a, equals(42));
    expect(o.b[0], equals("a"));
    expect(o.b[1], equals("b"));
  });

  test('invoke Dart callback from JS with 11 parameters', () {
    js.context.callbackWith11params = (p1, p2, p3, p4, p5, p6, p7, p8, p9, p10,
        p11) => '$p1$p2$p3$p4$p5$p6$p7$p8$p9$p10$p11';
    expect(js.context.invokeCallbackWith11params(), equals('1234567891011'));
  });

  test('pass unattached Dom Element', () {
    final div = new DivElement();
    div.classes.add('a');
    expect(js.context.getElementAttribute(div, 'class'), equals('a'));
  });

  test('pass unattached Dom Element two times on same call', () {
    final div = new DivElement();
    div.classes.add('a');
    expect(js.context.addClassAttributes(js.array([div, div])), equals('aa'));
  });

  test('pass Dom Element attached to an unattached element', () {
    final div = new DivElement();
    div.classes.add('a');
    final container = new DivElement();
    container.children.add(div);
    expect(js.context.getElementAttribute(div, 'class'), equals('a'));
  });

  test('pass 2 Dom Elements attached to an unattached element', () {
    final div1 = new DivElement();
    div1.classes.add('a');
    final div2 = new DivElement();
    div2.classes.add('b');
    final container = new DivElement();
    container.children.add(div1);
    container.children.add(div2);
    final f = js.context.addClassAttributes;
    expect(f(js.array([div1, div2])), equals('ab'));
  });

  test('pass multiple Dom Elements unattached to document', () {
    // A is alone
    // 1 and 3 are brother
    // 2 is child of 3
    final divA = new DivElement()..classes.add('A');
    final div1 = new DivElement()..classes.add('1');
    final div2 = new DivElement()..classes.add('2');
    final div3 = new DivElement()..classes.add('3')..children.add(div2);
    final container = new DivElement()..children.addAll([div1, div3]);
    final f = js.context.addClassAttributes;
    expect(f(js.array([divA, div1, div2, div3])), equals('A123'));
    expect(f(js.array([divA, div1, div3, div2])), equals('A132'));
    expect(f(js.array([divA, div1, div1, div3, divA, div2, div3])),
        equals('A113A23'));
    expect(!document.documentElement.contains(divA), isTrue);
    expect(!document.documentElement.contains(div1), isTrue);
    expect(!document.documentElement.contains(div2), isTrue);
    expect(!document.documentElement.contains(div3), isTrue);
    expect(!document.documentElement.contains(container), isTrue);
  });

  test('pass one Dom Elements unattached and another attached', () {
    final div1 = new DivElement()..classes.add('1');
    final div2 = new DivElement()..classes.add('2');
    document.documentElement.children.add(div2);
    final f = js.context.addClassAttributes;
    expect(f(js.array([div1, div2])), equals('12'));
    expect(!document.documentElement.contains(div1), isTrue);
    expect(document.documentElement.contains(div2), isTrue);
  });

  test('pass documentElement', () {
    expect(js.context.returnElement(document.documentElement),
        equals(document.documentElement));
  });

  test('retrieve unattached Dom Element', () {
    var result = js.context.getNewDivElement();
    expect(result is DivElement, isTrue);
    expect(!document.documentElement.contains(result), isTrue);
  });

  test('return a JS proxy to JavaScript', () {
    var result = js.context.testJsMap(() => js.map({ 'value': 42 }));
    expect(result, 42);
  });

  test('test proxy equality', () {
    var foo1 = new js.Proxy(js.context.Foo, 1);
    var foo2 = new js.Proxy(js.context.Foo, 2);
    js.context.foo = foo1;
    js.context.foo = foo2;
    expect(foo1, isNot(equals(js.context.foo)));
    expect(foo2, equals(js.context.foo));
  });

  test('test instanceof', () {
    var foo = new js.Proxy(js.context.Foo, 1);
    expect(js.instanceof(foo, js.context.Foo), isTrue);
    expect(js.instanceof(foo, js.context.Object), isTrue);
    expect(js.instanceof(foo, js.context.String), isFalse);
  });

  test('test hasProperty', () {
    var object = js.map({});
    object.a = 1;
    expect(js.hasProperty(object, "a"), isTrue);
    expect(js.hasProperty(object, "b"), isFalse);
  });

  test('test deleteProperty', () {
    var object = js.map({});
    object.a = 1;
    expect(js.context.Object.keys(object).length, 1);
    expect(js.context.Object.keys(object)[0], "a");
    js.deleteProperty(object, "a");
    expect(js.context.Object.keys(object).length, 0);
  });

  test('test index get and set', () {
    final myArray = js.context.myArray;
    expect(myArray.length, equals(1));
    expect(myArray[0], equals("value1"));
    myArray[0] = "value2";
    expect(myArray.length, equals(1));
    expect(myArray[0], equals("value2"));

    final foo = new js.Proxy(js.context.Foo, 1);
    foo["getAge"] = () => 10;
    expect(foo.getAge(), equals(10));
  });

  test('access a property of a function', () {
    expect(js.context.Bar(), "ret_value");
    expect(js.context.Bar.foo, "property_value");
  });

  test('retrieve same dart Object', () {
    final o = new Object();
    js.context.o = o;
    expect(js.context.o, same(o));
  });

  test('usage of Serializable', () {
    final red = Color.RED;
    js.context.color = red;
    expect(js.context.color, equals(red._value));
  });

}
