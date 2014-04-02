// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library template_binding.test.template_binding_test;

import 'dart:async';
import 'dart:html';
import 'dart:math' as math;
import 'package:observe/observe.dart';
import 'package:template_binding/template_binding.dart';
import 'package:unittest/html_config.dart';
import 'package:unittest/unittest.dart';

// TODO(jmesserly): merge this file?
import 'binding_syntax.dart' show syntaxTests;
import 'utils.dart';

// Note: this file ported from
// https://github.com/Polymer/TemplateBinding/blob/fcb7a502794f19544f2d4b77c96eebb70830591d/tests/tests.js

// TODO(jmesserly): submit a small cleanup patch to original. I fixed some
// cases where "div" and "t" were unintentionally using the JS global scope;
// look for "assertNodesAre".

main() => dirtyCheckZone().run(() {
  useHtmlConfiguration();

  // Load MutationObserver polyfill in case IE needs it.
  var script = new ScriptElement()
      ..src = '/root_dart/pkg/mutation_observer/lib/mutation_observer.min.js';
  var polyfillLoaded = script.onLoad.first;
  document.head.append(script);

  setUp(() => polyfillLoaded.then((_) {
    document.body.append(testDiv = new DivElement());
  }));

  tearDown(() {
    testDiv.remove();
    testDiv = null;
  });

  test('MutationObserver is supported', () {
    expect(MutationObserver.supported, true, reason: 'polyfill was loaded.');
  });

  group('Template', templateInstantiationTests);

  group('Binding Delegate API', () {
    group('with Observable', () {
      syntaxTests(([f, b]) => new FooBarModel(f, b));
    });

    group('with ChangeNotifier', () {
      syntaxTests(([f, b]) => new FooBarNotifyModel(f, b));
    });
  });

  group('Compat', compatTests);
});

var expando = new Expando('test');
void addExpandos(node) {
  while (node != null) {
    expando[node] = node.text;
    node = node.nextNode;
  }
}

void checkExpandos(node) {
  expect(node, isNotNull);
  while (node != null) {
    expect(expando[node], node.text);
    node = node.nextNode;
  }
}

templateInstantiationTests() {
  // Dart note: renamed some of these tests to have unique names

  test('Bind (simple)', () {
    var div = createTestHtml('<template bind={{}}>text</template>');
    templateBind(div.firstChild).model = {};
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'text');

      // Dart note: null is used instead of undefined to clear the template.
      templateBind(div.firstChild).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
      templateBind(div.firstChild).model = 123;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'text');
    });
  });

  test('oneTime-Bind', () {
    var div = createTestHtml('<template bind="[[ bound ]]">text</template>');
    var model = toObservable({'bound': 1});
    templateBind(div.firstChild).model = model;
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'text');

      model['bound'] = false;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'text');
    });
  });

  test('Bind - no parent', () {
    var div = createTestHtml('<template bind>text</template>');
    var template = div.firstChild;
    template.remove();

    templateBind(template).model = {};
    return new Future(() {
      expect(template.nodes.length, 0);
      expect(template.nextNode, null);
    });
  });

  test('Bind - no defaultView', () {
    var div = createTestHtml('<template bind>text</template>');
    var template = div.firstChild;
    var doc = document.implementation.createHtmlDocument('');
    doc.adoptNode(div);
    recursivelySetTemplateModel(template, {});
    return new Future(() => expect(div.nodes.length, 1));
  });

  test('Empty Bind', () {
    var div = createTestHtml('<template bind>text</template>');
    var template = div.firstChild;
    templateBind(template).model = {};
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'text');
    });
  });

  test('Bind If', () {
    var div = createTestHtml(
        '<template bind="{{ bound }}" if="{{ predicate }}">'
          'value:{{ value }}'
        '</template>');
    // Dart note: predicate changed from 0->null because 0 isn't falsey in Dart.
    // See https://code.google.com/p/dart/issues/detail?id=11956
    // Changed bound from null->1 since null is equivalent to JS undefined,
    // and would cause the template to not be expanded.
    var m = toObservable({ 'predicate': null, 'bound': 1 });
    var template = div.firstChild;
    bool errorSeen = false;
    runZoned(() {
      templateBind(template).model = m;
    }, onError: (e, s) {
      expect(e, isNoSuchMethodError);
      errorSeen = true;
    });
    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = 1;

      expect(errorSeen, isFalse);
    }).then(nextMicrotask).then((_) {
      expect(errorSeen, isTrue);
      expect(div.nodes.length, 1);

      m['bound'] = toObservable({ 'value': 2 });

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:2');

      m['bound']['value'] = 3;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      templateBind(template).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Bind oneTime-If - predicate false', () {
    var div = createTestHtml(
        '<template bind="{{ bound }}" if="[[ predicate ]]">'
          'value:{{ value }}'
        '</template>');
    // Dart note: predicate changed from 0->null because 0 isn't falsey in Dart.
    // See https://code.google.com/p/dart/issues/detail?id=11956
    // Changed bound from null->1 since null is equivalent to JS undefined,
    // and would cause the template to not be expanded.
    var m = toObservable({ 'predicate': null, 'bound': 1 });
    var template = div.firstChild;
    templateBind(template).model = m;

    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = 1;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);

      m['bound'] = toObservable({ 'value': 2 });

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);

      m['bound']['value'] = 3;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);

      templateBind(template).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Bind oneTime-If - predicate true', () {
    var div = createTestHtml(
        '<template bind="{{ bound }}" if="[[ predicate ]]">'
          'value:{{ value }}'
        '</template>');

    // Dart note: changed bound from null->1 since null is equivalent to JS
    // undefined, and would cause the template to not be expanded.
    var m = toObservable({ 'predicate': 1, 'bound': 1 });
    var template = div.firstChild;
    bool errorSeen = false;
    runZoned(() {
      templateBind(template).model = m;
    }, onError: (e, s) {
      expect(e, isNoSuchMethodError);
      errorSeen = true;
    });

    return new Future(() {
      expect(div.nodes.length, 1);
      m['bound'] = toObservable({ 'value': 2 });
      expect(errorSeen, isTrue);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:2');

      m['bound']['value'] = 3;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      m['predicate'] = null; // will have no effect

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      templateBind(template).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('oneTime-Bind If', () {
    var div = createTestHtml(
        '<template bind="[[ bound ]]" if="{{ predicate }}">'
          'value:{{ value }}'
        '</template>');

    var m = toObservable({'predicate': null, 'bound': {'value': 2}});
    var template = div.firstChild;
    templateBind(template).model = m;

    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = 1;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:2');

      m['bound']['value'] = 3;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      m['bound'] = toObservable({'value': 4 });

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      templateBind(template).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('oneTime-Bind oneTime-If', () {
    var div = createTestHtml(
        '<template bind="[[ bound ]]" if="[[ predicate ]]">'
          'value:{{ value }}'
        '</template>');

    var m = toObservable({'predicate': 1, 'bound': {'value': 2}});
    var template = div.firstChild;
    templateBind(template).model = m;

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:2');

      m['bound']['value'] = 3;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      m['bound'] = toObservable({'value': 4 });

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      m['predicate'] = false;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'value:3');

      templateBind(template).model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Bind If, 2', () {
    var div = createTestHtml(
        '<template bind="{{ foo }}" if="{{ bar }}">{{ bat }}</template>');
    var m = toObservable({ 'bar': null, 'foo': { 'bat': 'baz' } });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 1);

      m['bar'] = 1;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'baz');
    });
  });

  test('If', () {
    var div = createTestHtml('<template if="{{ foo }}">{{ value }}</template>');
    // Dart note: foo changed from 0->null because 0 isn't falsey in Dart.
    // See https://code.google.com/p/dart/issues/detail?id=11956
    var m = toObservable({ 'foo': null, 'value': 'foo' });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 1);

      m['foo'] = 1;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'foo');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Empty-If', () {
    var div = createTestHtml('<template if>{{ value }}</template>');
    var m = toObservable({ 'value': 'foo' });
    recursivelySetTemplateModel(div, null);
    return new Future(() {
      expect(div.nodes.length, 1);

      recursivelySetTemplateModel(div, m);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'foo');
    });
  });

  test('OneTime - simple text', () {
    var div = createTestHtml('<template bind>[[ value ]]</template>');
    var m = toObservable({ 'value': 'foo' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'foo');

      m['value'] = 'bar';

    }).then(endOfMicrotask).then((_) {
      // unchanged.
      expect(div.lastChild.text, 'foo');
    });
  });

  test('OneTime - compound text', () {
    var div = createTestHtml(
        '<template bind>[[ foo ]] bar [[ baz ]]</template>');
    var m = toObservable({ 'foo': 'FOO', 'baz': 'BAZ' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'FOO bar BAZ');

      m['foo'] = 'FI';
      m['baz'] = 'BA';

    }).then(endOfMicrotask).then((_) {
      // unchanged.
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'FOO bar BAZ');
    });
  });

  test('OneTime/Dynamic Mixed - compound text', () {
    var div = createTestHtml(
        '<template bind>[[ foo ]] bar {{ baz }}</template>');
    var m = toObservable({ 'foo': 'FOO', 'baz': 'BAZ' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'FOO bar BAZ');

      m['foo'] = 'FI';
      m['baz'] = 'BA';

    }).then(endOfMicrotask).then((_) {
      // unchanged [[ foo ]].
      expect(div.nodes.length, 2);
      expect(div.lastChild.text, 'FOO bar BA');
    });
  });

  test('OneTime - simple attribute', () {
    var div = createTestHtml(
        '<template bind><div foo="[[ value ]]"></div></template>');
    var m = toObservable({ 'value': 'foo' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.attributes['foo'], 'foo');

      m['value'] = 'bar';

    }).then(endOfMicrotask).then((_) {
      // unchanged.
      expect(div.nodes.length, 2);
      expect(div.lastChild.attributes['foo'], 'foo');
    });
  });

  test('OneTime - compound attribute', () {
    var div = createTestHtml(
        '<template bind>'
          '<div foo="[[ value ]]:[[ otherValue ]]"></div>'
        '</template>');
    var m = toObservable({ 'value': 'foo', 'otherValue': 'bar' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.attributes['foo'], 'foo:bar');

      m['value'] = 'baz';
      m['otherValue'] = 'bot';

    }).then(endOfMicrotask).then((_) {
      // unchanged.
      expect(div.lastChild.attributes['foo'], 'foo:bar');
    });
  });

  test('OneTime/Dynamic mixed - compound attribute', () {
    var div = createTestHtml(
        '<template bind>'
          '<div foo="{{ value }}:[[ otherValue ]]"></div>'
        '</template>');
    var m = toObservable({ 'value': 'foo', 'otherValue': 'bar' });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.lastChild.attributes['foo'], 'foo:bar');

      m['value'] = 'baz';
      m['otherValue'] = 'bot';

    }).then(endOfMicrotask).then((_) {
      // unchanged [[ otherValue ]].
      expect(div.lastChild.attributes['foo'], 'baz:bar');
    });
  });

  test('Repeat If', () {
    var div = createTestHtml(
        '<template repeat="{{ items }}" if="{{ predicate }}">{{}}</template>');
    // Dart note: predicate changed from 0->null because 0 isn't falsey in Dart.
    // See https://code.google.com/p/dart/issues/detail?id=11956
    var m = toObservable({ 'predicate': null, 'items': [1] });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = 1;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items']..add(2)..add(3);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);
      expect(div.nodes[1].text, '1');
      expect(div.nodes[2].text, '2');
      expect(div.nodes[3].text, '3');

      m['items'] = [4];

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '4');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Repeat oneTime-If (predicate false)', () {
    var div = createTestHtml(
        '<template repeat="{{ items }}" if="[[ predicate ]]">{{}}</template>');
    // Dart note: predicate changed from 0->null because 0 isn't falsey in Dart.
    // See https://code.google.com/p/dart/issues/detail?id=11956
    var m = toObservable({ 'predicate': null, 'items': [1] });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = 1;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1, reason: 'unchanged');

      m['items']..add(2)..add(3);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1, reason: 'unchanged');

      m['items'] = [4];

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1, reason: 'unchanged');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Repeat oneTime-If (predicate true)', () {
    var div = createTestHtml(
        '<template repeat="{{ items }}" if="[[ predicate ]]">{{}}</template>');

    var m = toObservable({ 'predicate': true, 'items': [1] });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items']..add(2)..add(3);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);
      expect(div.nodes[1].text, '1');
      expect(div.nodes[2].text, '2');
      expect(div.nodes[3].text, '3');

      m['items'] = [4];

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '4');

      m['predicate'] = false;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2, reason: 'unchanged');
      expect(div.nodes[1].text, '4', reason: 'unchanged');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('oneTime-Repeat If', () {
    var div = createTestHtml(
        '<template repeat="[[ items ]]" if="{{ predicate }}">{{}}</template>');

    var m = toObservable({ 'predicate': false, 'items': [1] });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 1);

      m['predicate'] = true;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items']..add(2)..add(3);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items'] = [4];

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('oneTime-Repeat oneTime-If', () {
    var div = createTestHtml(
        '<template repeat="[[ items ]]" if="[[ predicate ]]">{{}}</template>');

    var m = toObservable({ 'predicate': true, 'items': [1] });
    var template = div.firstChild;
    templateBind(template).model = m;
    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items']..add(2)..add(3);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['items'] = [4];

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      m['predicate'] = false;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes[1].text, '1');

      templateBind(template).model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('TextTemplateWithNullStringBinding', () {
    var div = createTestHtml('<template bind={{}}>a{{b}}c</template>');
    var model = toObservable({'b': 'B'});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'aBc');

      model['b'] = 'b';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'abc');

      model['b'] = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'ac');

      model = null;
    }).then(endOfMicrotask).then((_) {
      // setting model isn't bindable.
      expect(div.nodes.last.text, 'ac');
    });
  });

  test('TextTemplateWithBindingPath', () {
    var div = createTestHtml(
        '<template bind="{{ data }}">a{{b}}c</template>');
    var model = toObservable({ 'data': {'b': 'B'} });
    var template = div.firstChild;
    templateBind(template).model = model;

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'aBc');

      model['data']['b'] = 'b';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'abc');

      model['data'] = toObservable({'b': 'X'});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'aXc');

      // Dart note: changed from `null` since our null means don't render a model.
      model['data'] = toObservable({});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'ac');

      model['data'] = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('TextTemplateWithBindingAndConditional', () {
    var div = createTestHtml(
        '<template bind="{{}}" if="{{ d }}">a{{b}}c</template>');
    var model = toObservable({'b': 'B', 'd': 1});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'aBc');

      model['b'] = 'b';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'abc');

      // TODO(jmesserly): MDV set this to empty string and relies on JS conversion
      // rules. Is that intended?
      // See https://github.com/Polymer/TemplateBinding/issues/59
      model['d'] = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);

      model['d'] = 'here';
      model['b'] = 'd';

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'adc');
    });
  });

  test('TemplateWithTextBinding2', () {
    var div = createTestHtml(
        '<template bind="{{ b }}">a{{value}}c</template>');
    expect(div.nodes.length, 1);
    var model = toObservable({'b': {'value': 'B'}});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.text, 'aBc');

      model['b'] = toObservable({'value': 'b'});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.text, 'abc');
    });
  });

  test('TemplateWithAttributeBinding', () {
    var div = createTestHtml(
        '<template bind="{{}}">'
        '<div foo="a{{b}}c"></div>'
        '</template>');
    var model = toObservable({'b': 'B'});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.attributes['foo'], 'aBc');

      model['b'] = 'b';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.attributes['foo'], 'abc');

      model['b'] = 'X';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.attributes['foo'], 'aXc');
    });
  });

  test('TemplateWithConditionalBinding', () {
    var div = createTestHtml(
        '<template bind="{{}}">'
        '<div foo?="{{b}}"></div>'
        '</template>');
    var model = toObservable({'b': 'b'});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.attributes['foo'], '');
      expect(div.nodes.last.attributes, isNot(contains('foo?')));

      model['b'] = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.attributes, isNot(contains('foo')));
    });
  });

  test('Repeat', () {
    var div = createTestHtml(
        '<template repeat="{{ array }}">{{}},</template>');

    var model = toObservable({'array': [0, 1, 2]});
    var template = templateBind(div.firstChild);
    template.model = model;

    return new Future(() {
      expect(div.nodes.length, 4);
      expect(div.text, '0,1,2,');

      model['array'].length = 1;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);
      expect(div.text, '0,');

      model['array'].addAll([3, 4]);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);
      expect(div.text, '0,3,4,');

      model['array'].removeRange(1, 2);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 3);
      expect(div.text, '0,4,');

      model['array'].addAll([5, 6]);
      model['array'] = toObservable(['x', 'y']);

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 3);
      expect(div.text, 'x,y,');

      template.model = null;

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
      expect(div.text, '');
    });
  });

  test('Repeat - oneTime', () {
    var div = createTestHtml('<template repeat="[[]]">text</template>');

    var model = toObservable([0, 1, 2]);
    var template = templateBind(div.firstChild);
    template.model = model;

    return new Future(() {
      expect(div.nodes.length, 4);

      model.length = 1;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);

      model.addAll([3, 4]);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);

      model.removeRange(1, 2);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);

      template.model = null;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);
    });
  });

  test('Repeat - Reuse Instances', () {
    var div = createTestHtml('<template repeat>{{ val }}</template>');

    var model = toObservable([
      {'val': 10},
      {'val': 5},
      {'val': 2},
      {'val': 8},
      {'val': 1}
    ]);
    recursivelySetTemplateModel(div, model);
    var template = div.firstChild;

    return new Future(() {
      expect(div.nodes.length, 6);

      addExpandos(template.nextNode);
      checkExpandos(template.nextNode);

      model.sort((a, b) => a['val'] - b['val']);
    }).then(endOfMicrotask).then((_) {
      checkExpandos(template.nextNode);

      model = toObservable(model.reversed);
      recursivelySetTemplateModel(div, model);
    }).then(endOfMicrotask).then((_) {
      checkExpandos(template.nextNode);

      for (var item in model) {
        item['val'] += 1;
      }

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[1].text, "11");
      expect(div.nodes[2].text, "9");
      expect(div.nodes[3].text, "6");
      expect(div.nodes[4].text, "3");
      expect(div.nodes[5].text, "2");
    });
  });

  test('Bind - Reuse Instance', () {
    var div = createTestHtml(
        '<template bind="{{ foo }}">{{ bar }}</template>');

    var model = toObservable({ 'foo': { 'bar': 5 }});
    recursivelySetTemplateModel(div, model);
    var template = div.firstChild;

    return new Future(() {
      expect(div.nodes.length, 2);

      addExpandos(template.nextNode);
      checkExpandos(template.nextNode);

      model = toObservable({'foo': model['foo']});
      recursivelySetTemplateModel(div, model);
    }).then(endOfMicrotask).then((_) {
      checkExpandos(template.nextNode);
    });
  });

  test('Repeat-Empty', () {
    var div = createTestHtml(
        '<template repeat>text</template>');

    var model = toObservable([0, 1, 2]);
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 4);

      model.length = 1;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 2);

      model.addAll(toObservable([3, 4]));
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);

      model.removeRange(1, 2);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 3);
    });
  });

  test('Removal from iteration needs to unbind', () {
    var div = createTestHtml(
        '<template repeat="{{}}"><a>{{v}}</a></template>');
    var model = toObservable([{'v': 0}, {'v': 1}, {'v': 2}, {'v': 3},
        {'v': 4}]);
    recursivelySetTemplateModel(div, model);

    var nodes, vs;
    return new Future(() {

      nodes = div.nodes.skip(1).toList();
      vs = model.toList();

      for (var i = 0; i < 5; i++) {
        expect(nodes[i].text, '$i');
      }

      model.length = 3;
    }).then(endOfMicrotask).then((_) {
      for (var i = 0; i < 5; i++) {
        expect(nodes[i].text, '$i');
      }

      vs[3]['v'] = 33;
      vs[4]['v'] = 44;
    }).then(endOfMicrotask).then((_) {
      for (var i = 0; i < 5; i++) {
        expect(nodes[i].text, '$i');
      }
    });
  });

  test('DOM Stability on Iteration', () {
    var div = createTestHtml(
        '<template repeat="{{}}">{{}}</template>');
    var model = toObservable([1, 2, 3, 4, 5]);
    recursivelySetTemplateModel(div, model);

    var nodes;
    return new Future(() {
      // Note: the node at index 0 is the <template>.
      nodes = div.nodes.toList();
      expect(nodes.length, 6, reason: 'list has 5 items');

      model.removeAt(0);
      model.removeLast();

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4, reason: 'list has 3 items');
      expect(identical(div.nodes[1], nodes[2]), true, reason: '2 not removed');
      expect(identical(div.nodes[2], nodes[3]), true, reason: '3 not removed');
      expect(identical(div.nodes[3], nodes[4]), true, reason: '4 not removed');

      model.insert(0, 5);
      model[2] = 6;
      model.add(7);

    }).then(endOfMicrotask).then((_) {

      expect(div.nodes.length, 6, reason: 'list has 5 items');
      expect(nodes.contains(div.nodes[1]), false, reason: '5 is a new node');
      expect(identical(div.nodes[2], nodes[2]), true);
      expect(nodes.contains(div.nodes[3]), false, reason: '6 is a new node');
      expect(identical(div.nodes[4], nodes[4]), true);
      expect(nodes.contains(div.nodes[5]), false, reason: '7 is a new node');

      nodes = div.nodes.toList();

      model.insert(2, 8);

    }).then(endOfMicrotask).then((_) {

      expect(div.nodes.length, 7, reason: 'list has 6 items');
      expect(identical(div.nodes[1], nodes[1]), true);
      expect(identical(div.nodes[2], nodes[2]), true);
      expect(nodes.contains(div.nodes[3]), false, reason: '8 is a new node');
      expect(identical(div.nodes[4], nodes[3]), true);
      expect(identical(div.nodes[5], nodes[4]), true);
      expect(identical(div.nodes[6], nodes[5]), true);
    });
  });

  test('Repeat2', () {
    var div = createTestHtml(
        '<template repeat="{{}}">{{value}}</template>');
    expect(div.nodes.length, 1);

    var model = toObservable([
      {'value': 0},
      {'value': 1},
      {'value': 2}
    ]);
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 4);
      expect(div.nodes[1].text, '0');
      expect(div.nodes[2].text, '1');
      expect(div.nodes[3].text, '2');

      model[1]['value'] = 'One';
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);
      expect(div.nodes[1].text, '0');
      expect(div.nodes[2].text, 'One');
      expect(div.nodes[3].text, '2');

      model.replaceRange(0, 1, toObservable([{'value': 'Zero'}]));
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 4);
      expect(div.nodes[1].text, 'Zero');
      expect(div.nodes[2].text, 'One');
      expect(div.nodes[3].text, '2');
    });
  });

  test('TemplateWithInputValue', () {
    var div = createTestHtml(
        '<template bind="{{}}">'
        '<input value="{{x}}">'
        '</template>');
    var model = toObservable({'x': 'hi'});
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 2);
      expect(div.nodes.last.value, 'hi');

      model['x'] = 'bye';
      expect(div.nodes.last.value, 'hi');
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.value, 'bye');

      div.nodes.last.value = 'hello';
      dispatchEvent('input', div.nodes.last);
      expect(model['x'], 'hello');
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.last.value, 'hello');
    });
  });

//////////////////////////////////////////////////////////////////////////////

  test('Decorated', () {
    var div = createTestHtml(
        '<template bind="{{ XX }}" id="t1">'
          '<p>Crew member: {{name}}, Job title: {{title}}</p>'
        '</template>'
        '<template bind="{{ XY }}" id="t2" ref="t1"></template>');

    var model = toObservable({
      'XX': {'name': 'Leela', 'title': 'Captain'},
      'XY': {'name': 'Fry', 'title': 'Delivery boy'},
      'XZ': {'name': 'Zoidberg', 'title': 'Doctor'}
    });
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      var t1 = document.getElementById('t1');
      var instance = t1.nextElementSibling;
      expect(instance.text, 'Crew member: Leela, Job title: Captain');

      var t2 = document.getElementById('t2');
      instance = t2.nextElementSibling;
      expect(instance.text, 'Crew member: Fry, Job title: Delivery boy');

      expect(div.children.length, 4);
      expect(div.nodes.length, 4);

      expect(div.nodes[1].tagName, 'P');
      expect(div.nodes[3].tagName, 'P');
    });
  });

  test('DefaultStyles', () {
    var t = new Element.tag('template');
    TemplateBindExtension.decorate(t);

    document.body.append(t);
    expect(t.getComputedStyle().display, 'none');

    t.remove();
  });


  test('Bind', () {
    var div = createTestHtml('<template bind="{{}}">Hi {{ name }}</template>');
    var model = toObservable({'name': 'Leela'});
    recursivelySetTemplateModel(div, model);

    return new Future(() => expect(div.nodes[1].text, 'Hi Leela'));
  });

  test('BindPlaceHolderHasNewLine', () {
    var div = createTestHtml(
        '<template bind="{{}}">Hi {{\nname\n}}</template>');
    var model = toObservable({'name': 'Leela'});
    recursivelySetTemplateModel(div, model);

    return new Future(() => expect(div.nodes[1].text, 'Hi Leela'));
  });

  test('BindWithRef', () {
    var id = 't${new math.Random().nextInt(100)}';
    var div = createTestHtml(
        '<template id="$id">'
          'Hi {{ name }}'
        '</template>'
        '<template ref="$id" bind="{{}}"></template>');

    var t1 = div.nodes.first;
    var t2 = div.nodes[1];

    expect(templateBind(t2).ref, t1);

    var model = toObservable({'name': 'Fry'});
    recursivelySetTemplateModel(div, model);

    return new Future(() => expect(t2.nextNode.text, 'Hi Fry'));
  });


  test('Update Ref', () {
    var div = createTestHtml(
        '<template id=A>Hi, {{}}</template>'
        '<template id=B>Hola, {{}}</template>'
        '<template ref=A repeat></template>');

    var model = new ObservableList.from(['Fry']);
    recursivelySetTemplateModel(div, model);

    return new Future(() {
      expect(div.nodes.length, 4);
      expect('Hi, Fry', div.nodes[3].text);

      div.nodes[2].attributes['ref'] = 'B';
      model.add('Leela');

    }).then(endOfMicrotask).then((x) {
      expect(div.nodes.length, 5);

      expect('Hi, Fry', div.nodes[3].text);
      expect('Hola, Leela', div.nodes[4].text);
    });
  });

  test('BindWithDynamicRef', () {
    var id = 't${new math.Random().nextInt(100)}';
    var div = createTestHtml(
        '<template id="$id">'
          'Hi {{ name }}'
        '</template>'
        '<template ref="{{ id }}" bind="{{}}"></template>');

    var t1 = div.firstChild;
    var t2 = div.nodes[1];
    var model = toObservable({'name': 'Fry', 'id': id });
    recursivelySetTemplateModel(div, model);

    return new Future(() => expect(t2.nextNode.text, 'Hi Fry'));
  });

  assertNodesAre(div, [arguments]) {
    var expectedLength = arguments.length;
    expect(div.nodes.length, expectedLength + 1);

    for (var i = 0; i < arguments.length; i++) {
      var targetNode = div.nodes[i + 1];
      expect(targetNode.text, arguments[i]);
    }
  }

  test('Repeat3', () {
    var div = createTestHtml(
        '<template repeat="{{ contacts }}">Hi {{ name }}</template>');
    var t = div.nodes.first;

    var m = toObservable({
      'contacts': [
        {'name': 'Raf'},
        {'name': 'Arv'},
        {'name': 'Neal'}
      ]
    });

    recursivelySetTemplateModel(div, m);
    return new Future(() {

      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal']);

      m['contacts'].add(toObservable({'name': 'Alex'}));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal', 'Hi Alex']);

      m['contacts'].replaceRange(0, 2,
          toObservable([{'name': 'Rafael'}, {'name': 'Erik'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Erik', 'Hi Neal', 'Hi Alex']);

      m['contacts'].removeRange(1, 3);
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Alex']);

      m['contacts'].insertAll(1,
          toObservable([{'name': 'Erik'}, {'name': 'Dimitri'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Erik', 'Hi Dimitri', 'Hi Alex']);

      m['contacts'].replaceRange(0, 1,
          toObservable([{'name': 'Tab'}, {'name': 'Neal'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Tab', 'Hi Neal', 'Hi Erik', 'Hi Dimitri',
          'Hi Alex']);

      m['contacts'] = toObservable([{'name': 'Alex'}]);
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Alex']);

      m['contacts'].length = 0;
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, []);
    });
  });

  test('RepeatModelSet', () {
    var div = createTestHtml(
        '<template repeat="{{ contacts }}">'
          'Hi {{ name }}'
        '</template>');
    var m = toObservable({
      'contacts': [
        {'name': 'Raf'},
        {'name': 'Arv'},
        {'name': 'Neal'}
      ]
    });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      var t = div.nodes.first;
      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal']);
    });
  });

  test('RepeatEmptyPath', () {
    var div = createTestHtml(
        '<template repeat="{{}}">Hi {{ name }}</template>');
    var t = div.nodes.first;

    var m = toObservable([
      {'name': 'Raf'},
      {'name': 'Arv'},
      {'name': 'Neal'}
    ]);
    recursivelySetTemplateModel(div, m);
    return new Future(() {

      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal']);

      m.add(toObservable({'name': 'Alex'}));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal', 'Hi Alex']);

      m.replaceRange(0, 2, toObservable([{'name': 'Rafael'}, {'name': 'Erik'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Erik', 'Hi Neal', 'Hi Alex']);

      m.removeRange(1, 3);
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Alex']);

      m.insertAll(1, toObservable([{'name': 'Erik'}, {'name': 'Dimitri'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Rafael', 'Hi Erik', 'Hi Dimitri', 'Hi Alex']);

      m.replaceRange(0, 1, toObservable([{'name': 'Tab'}, {'name': 'Neal'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Tab', 'Hi Neal', 'Hi Erik', 'Hi Dimitri',
          'Hi Alex']);

      m.length = 0;
      m.add(toObservable({'name': 'Alex'}));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Alex']);
    });
  });

  test('RepeatNullModel', () {
    var div = createTestHtml(
        '<template repeat="{{}}">Hi {{ name }}</template>');
    var t = div.nodes.first;

    var m = null;
    recursivelySetTemplateModel(div, m);

    expect(div.nodes.length, 1);

    t.attributes['iterate'] = '';
    m = toObservable({});
    recursivelySetTemplateModel(div, m);
    return new Future(() => expect(div.nodes.length, 1));
  });

  test('RepeatReuse', () {
    var div = createTestHtml(
        '<template repeat="{{}}">Hi {{ name }}</template>');
    var t = div.nodes.first;

    var m = toObservable([
      {'name': 'Raf'},
      {'name': 'Arv'},
      {'name': 'Neal'}
    ]);
    recursivelySetTemplateModel(div, m);

    var node1, node2, node3;
    return new Future(() {
      assertNodesAre(div, ['Hi Raf', 'Hi Arv', 'Hi Neal']);
      node1 = div.nodes[1];
      node2 = div.nodes[2];
      node3 = div.nodes[3];

      m.replaceRange(1, 2, toObservable([{'name': 'Erik'}]));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Raf', 'Hi Erik', 'Hi Neal']);
      expect(div.nodes[1], node1,
          reason: 'model[0] did not change so the node should not have changed');
      expect(div.nodes[2], isNot(equals(node2)),
          reason: 'Should not reuse when replacing');
      expect(div.nodes[3], node3,
          reason: 'model[2] did not change so the node should not have changed');

      node2 = div.nodes[2];
      m.insert(0, toObservable({'name': 'Alex'}));
    }).then(endOfMicrotask).then((_) {
      assertNodesAre(div, ['Hi Alex', 'Hi Raf', 'Hi Erik', 'Hi Neal']);
    });
  });

  test('TwoLevelsDeepBug', () {
    var div = createTestHtml(
      '<template bind="{{}}"><span><span>{{ foo }}</span></span></template>');

    var model = toObservable({'foo': 'bar'});
    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes[1].nodes[0].nodes[0].text, 'bar');
    });
  });

  test('Checked', () {
    var div = createTestHtml(
        '<template bind>'
          '<input type="checkbox" checked="{{a}}">'
        '</template>');
    var t = div.nodes.first;
    templateBind(t).model = toObservable({'a': true });

    return new Future(() {

      var instanceInput = t.nextNode;
      expect(instanceInput.checked, true);

      instanceInput.click();
      expect(instanceInput.checked, false);

      instanceInput.click();
      expect(instanceInput.checked, true);
    });
  });

  nestedHelper(s, start) {
    var div = createTestHtml(s);

    var m = toObservable({
      'a': {
        'b': 1,
        'c': {'d': 2}
      },
    });

    recursivelySetTemplateModel(div, m);
    return new Future(() {

      var i = start;
      expect(div.nodes[i++].text, '1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, '2');

      m['a']['b'] = 11;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[start].text, '11');

      m['a']['c'] = toObservable({'d': 22});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[start + 2].text, '22');
    });
  }

  test('Nested', () => nestedHelper(
      '<template bind="{{a}}">'
        '{{b}}'
        '<template bind="{{c}}">'
          '{{d}}'
        '</template>'
      '</template>', 1));

  test('NestedWithRef', () => nestedHelper(
        '<template id="inner">{{d}}</template>'
        '<template id="outer" bind="{{a}}">'
          '{{b}}'
          '<template ref="inner" bind="{{c}}"></template>'
        '</template>', 2));

  nestedIterateInstantiateHelper(s, start) {
    var div = createTestHtml(s);

    var m = toObservable({
      'a': [
        {
          'b': 1,
          'c': {'d': 11}
        },
        {
          'b': 2,
          'c': {'d': 22}
        }
      ]
    });

    recursivelySetTemplateModel(div, m);
    return new Future(() {

      var i = start;
      expect(div.nodes[i++].text, '1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, '11');
      expect(div.nodes[i++].text, '2');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, '22');

      m['a'][1] = toObservable({
        'b': 3,
        'c': {'d': 33}
      });

    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[start + 3].text, '3');
      expect(div.nodes[start + 5].text, '33');
    });
  }

  test('NestedRepeatBind', () => nestedIterateInstantiateHelper(
      '<template repeat="{{a}}">'
        '{{b}}'
        '<template bind="{{c}}">'
          '{{d}}'
        '</template>'
      '</template>', 1));

  test('NestedRepeatBindWithRef', () => nestedIterateInstantiateHelper(
      '<template id="inner">'
        '{{d}}'
      '</template>'
      '<template repeat="{{a}}">'
        '{{b}}'
        '<template ref="inner" bind="{{c}}"></template>'
      '</template>', 2));

  nestedIterateIterateHelper(s, start) {
    var div = createTestHtml(s);

    var m = toObservable({
      'a': [
        {
          'b': 1,
          'c': [{'d': 11}, {'d': 12}]
        },
        {
          'b': 2,
          'c': [{'d': 21}, {'d': 22}]
        }
      ]
    });

    recursivelySetTemplateModel(div, m);
    return new Future(() {

      var i = start;
      expect(div.nodes[i++].text, '1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, '11');
      expect(div.nodes[i++].text, '12');
      expect(div.nodes[i++].text, '2');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, '21');
      expect(div.nodes[i++].text, '22');

      m['a'][1] = toObservable({
        'b': 3,
        'c': [{'d': 31}, {'d': 32}, {'d': 33}]
      });

      i = start + 4;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[start + 4].text, '3');
      expect(div.nodes[start + 6].text, '31');
      expect(div.nodes[start + 7].text, '32');
      expect(div.nodes[start + 8].text, '33');
    });
  }

  test('NestedRepeatBind', () => nestedIterateIterateHelper(
      '<template repeat="{{a}}">'
        '{{b}}'
        '<template repeat="{{c}}">'
          '{{d}}'
        '</template>'
      '</template>', 1));

  test('NestedRepeatRepeatWithRef', () => nestedIterateIterateHelper(
      '<template id="inner">'
        '{{d}}'
      '</template>'
      '<template repeat="{{a}}">'
        '{{b}}'
        '<template ref="inner" repeat="{{c}}"></template>'
      '</template>', 2));

  test('NestedRepeatSelfRef', () {
    var div = createTestHtml(
        '<template id="t" repeat="{{}}">'
          '{{name}}'
          '<template ref="t" repeat="{{items}}"></template>'
        '</template>');

    var m = toObservable([
      {
        'name': 'Item 1',
        'items': [
          {
            'name': 'Item 1.1',
            'items': [
              {
                 'name': 'Item 1.1.1',
                 'items': []
              }
            ]
          },
          {
            'name': 'Item 1.2'
          }
        ]
      },
      {
        'name': 'Item 2',
        'items': []
      },
    ]);

    recursivelySetTemplateModel(div, m);

    int i = 1;
    return new Future(() {
      expect(div.nodes[i++].text, 'Item 1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, 'Item 1.1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, 'Item 1.1.1');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, 'Item 1.2');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, 'Item 2');

      m[0] = toObservable({'name': 'Item 1 changed'});

      i = 1;
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[i++].text, 'Item 1 changed');
      expect(div.nodes[i++].tagName, 'TEMPLATE');
      expect(div.nodes[i++].text, 'Item 2');
    });
  });

  // Note: we don't need a zone for this test, and we don't want to alter timing
  // since we're testing a rather subtle relationship between select and option.
  test('Attribute Template Option/Optgroup', () {
    var div = createTestHtml(
        '<template bind>'
          '<select selectedIndex="{{ selected }}">'
            '<optgroup template repeat="{{ groups }}" label="{{ name }}">'
              '<option template repeat="{{ items }}">{{ val }}</option>'
            '</optgroup>'
          '</select>'
        '</template>');

    var m = toObservable({
      'selected': 1,
      'groups': [{
        'name': 'one', 'items': [{ 'val': 0 }, { 'val': 1 }]
      }],
    });

    recursivelySetTemplateModel(div, m);

    var completer = new Completer();

    new MutationObserver((records, observer) {
      var select = div.nodes[0].nextNode;
      if (select == null || select.querySelector('option') == null) return;

      observer.disconnect();
      new Future(() {
        expect(select.nodes.length, 2);

        expect(select.selectedIndex, 1, reason: 'selected index should update '
            'after template expands.');

        expect(select.nodes[0].tagName, 'TEMPLATE');
        expect((templateBind(templateBind(select.nodes[0]).ref)
            .content.nodes[0] as Element).tagName, 'OPTGROUP');

        var optgroup = select.nodes[1];
        expect(optgroup.nodes[0].tagName, 'TEMPLATE');
        expect(optgroup.nodes[1].tagName, 'OPTION');
        expect(optgroup.nodes[1].text, '0');
        expect(optgroup.nodes[2].tagName, 'OPTION');
        expect(optgroup.nodes[2].text, '1');

        completer.complete();
      });
    })..observe(div, childList: true, subtree: true);

    Observable.dirtyCheck();

    return completer.future;
  });

  test('NestedIterateTableMixedSemanticNative', () {
    if (!parserHasNativeTemplate) return null;

    var div = createTestHtml(
        '<table><tbody>'
          '<template repeat="{{}}">'
            '<tr>'
              '<td template repeat="{{}}" class="{{ val }}">{{ val }}</td>'
            '</tr>'
          '</template>'
        '</tbody></table>');

    var m = toObservable([
      [{ 'val': 0 }, { 'val': 1 }],
      [{ 'val': 2 }, { 'val': 3 }]
    ]);

    recursivelySetTemplateModel(div, m);
    return new Future(() {
      var tbody = div.nodes[0].nodes[0];

      // 1 for the <tr template>, 2 * (1 tr)
      expect(tbody.nodes.length, 3);

      // 1 for the <td template>, 2 * (1 td)
      expect(tbody.nodes[1].nodes.length, 3);

      expect(tbody.nodes[1].nodes[1].text, '0');
      expect(tbody.nodes[1].nodes[2].text, '1');

      // 1 for the <td template>, 2 * (1 td)
      expect(tbody.nodes[2].nodes.length, 3);
      expect(tbody.nodes[2].nodes[1].text, '2');
      expect(tbody.nodes[2].nodes[2].text, '3');

      // Asset the 'class' binding is retained on the semantic template (just
      // check the last one).
      expect(tbody.nodes[2].nodes[2].attributes["class"], '3');
    });
  });

  test('NestedIterateTable', () {
    var div = createTestHtml(
        '<table><tbody>'
          '<tr template repeat="{{}}">'
            '<td template repeat="{{}}" class="{{ val }}">{{ val }}</td>'
          '</tr>'
        '</tbody></table>');

    var m = toObservable([
      [{ 'val': 0 }, { 'val': 1 }],
      [{ 'val': 2 }, { 'val': 3 }]
    ]);

    recursivelySetTemplateModel(div, m);
    return new Future(() {

      var i = 1;
      var tbody = div.nodes[0].nodes[0];

      // 1 for the <tr template>, 2 * (1 tr)
      expect(tbody.nodes.length, 3);

      // 1 for the <td template>, 2 * (1 td)
      expect(tbody.nodes[1].nodes.length, 3);
      expect(tbody.nodes[1].nodes[1].text, '0');
      expect(tbody.nodes[1].nodes[2].text, '1');

      // 1 for the <td template>, 2 * (1 td)
      expect(tbody.nodes[2].nodes.length, 3);
      expect(tbody.nodes[2].nodes[1].text, '2');
      expect(tbody.nodes[2].nodes[2].text, '3');

      // Asset the 'class' binding is retained on the semantic template (just
      // check the last one).
      expect(tbody.nodes[2].nodes[2].attributes['class'], '3');
    });
  });

  test('NestedRepeatDeletionOfMultipleSubTemplates', () {
    var div = createTestHtml(
        '<ul>'
          '<template repeat="{{}}" id=t1>'
            '<li>{{name}}'
              '<ul>'
                '<template ref=t1 repaet="{{items}}"></template>'
              '</ul>'
            '</li>'
          '</template>'
        '</ul>');

    var m = toObservable([
      {
        'name': 'Item 1',
        'items': [
          {
            'name': 'Item 1.1'
          }
        ]
      }
    ]);

    recursivelySetTemplateModel(div, m);
    return new Future(() => m.removeAt(0));
  });

  test('DeepNested', () {
    var div = createTestHtml(
      '<template bind="{{a}}">'
        '<p>'
          '<template bind="{{b}}">'
            '{{ c }}'
          '</template>'
        '</p>'
      '</template>');

    var m = toObservable({
      'a': {
        'b': {
          'c': 42
        }
      }
    });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes[1].tagName, 'P');
      expect(div.nodes[1].nodes.first.tagName, 'TEMPLATE');
      expect(div.nodes[1].nodes[1].text, '42');
    });
  });

  test('TemplateContentRemoved', () {
    var div = createTestHtml('<template bind="{{}}">{{ }}</template>');
    var model = 42;

    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes[1].text, '42');
      expect(div.nodes[0].text, '');
    });
  });

  test('TemplateContentRemovedEmptyArray', () {
    var div = createTestHtml('<template iterate>Remove me</template>');
    var model = toObservable([]);

    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes.length, 1);
      expect(div.nodes[0].text, '');
    });
  });

  test('TemplateContentRemovedNested', () {
    var div = createTestHtml(
        '<template bind="{{}}">'
          '{{ a }}'
          '<template bind="{{}}">'
            '{{ b }}'
          '</template>'
        '</template>');

    var model = toObservable({
      'a': 1,
      'b': 2
    });
    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes[0].text, '');
      expect(div.nodes[1].text, '1');
      expect(div.nodes[2].text, '');
      expect(div.nodes[3].text, '2');
    });
  });

  test('BindWithUndefinedModel', () {
    var div = createTestHtml(
        '<template bind="{{}}" if="{{}}">{{ a }}</template>');

    var model = toObservable({'a': 42});
    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes[1].text, '42');

      model = null;
      recursivelySetTemplateModel(div, model);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 1);

      model = toObservable({'a': 42});
      recursivelySetTemplateModel(div, model);
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes[1].text, '42');
    });
  });

  test('BindNested', () {
    var div = createTestHtml(
        '<template bind="{{}}">'
          'Name: {{ name }}'
          '<template bind="{{wife}}" if="{{wife}}">'
            'Wife: {{ name }}'
          '</template>'
          '<template bind="{{child}}" if="{{child}}">'
            'Child: {{ name }}'
          '</template>'
        '</template>');

    var m = toObservable({
      'name': 'Hermes',
      'wife': {
        'name': 'LaBarbara'
      }
    });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 5);
      expect(div.nodes[1].text, 'Name: Hermes');
      expect(div.nodes[3].text, 'Wife: LaBarbara');

      m['child'] = toObservable({'name': 'Dwight'});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 6);
      expect(div.nodes[5].text, 'Child: Dwight');

      m.remove('wife');
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 5);
      expect(div.nodes[4].text, 'Child: Dwight');
    });
  });

  test('BindRecursive', () {
    var div = createTestHtml(
        '<template bind="{{}}" if="{{}}" id="t">'
          'Name: {{ name }}'
          '<template bind="{{friend}}" if="{{friend}}" ref="t"></template>'
        '</template>');

    var m = toObservable({
      'name': 'Fry',
      'friend': {
        'name': 'Bender'
      }
    });
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 5);
      expect(div.nodes[1].text, 'Name: Fry');
      expect(div.nodes[3].text, 'Name: Bender');

      m['friend']['friend'] = toObservable({'name': 'Leela'});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 7);
      expect(div.nodes[5].text, 'Name: Leela');

      m['friend'] = toObservable({'name': 'Leela'});
    }).then(endOfMicrotask).then((_) {
      expect(div.nodes.length, 5);
      expect(div.nodes[3].text, 'Name: Leela');
    });
  });

  test('Template - Self is terminator', () {
    var div = createTestHtml(
        '<template repeat>{{ foo }}'
          '<template bind></template>'
        '</template>');

    var m = toObservable([{ 'foo': 'bar' }]);
    recursivelySetTemplateModel(div, m);
    return new Future(() {

      m.add(toObservable({ 'foo': 'baz' }));
      recursivelySetTemplateModel(div, m);
    }).then(endOfMicrotask).then((_) {

      expect(div.nodes.length, 5);
      expect(div.nodes[1].text, 'bar');
      expect(div.nodes[3].text, 'baz');
    });
  });

  test('Template - Same Contents, Different Array has no effect', () {
    if (!MutationObserver.supported) return null;

    var div = createTestHtml('<template repeat>{{ foo }}</template>');

    var m = toObservable([{ 'foo': 'bar' }, { 'foo': 'bat'}]);
    recursivelySetTemplateModel(div, m);
    var observer = new MutationObserver((x, y) {});
    return new Future(() {
      observer.observe(div, childList: true);

      var template = div.firstChild;
      templateBind(template).model = new ObservableList.from(m);
    }).then(endOfMicrotask).then((_) {
      var records = observer.takeRecords();
      expect(records.length, 0);
    });
  });

  test('RecursiveRef', () {
    var div = createTestHtml(
        '<template bind>'
          '<template id=src>{{ foo }}</template>'
          '<template bind ref=src></template>'
        '</template>');

    var m = toObservable({'foo': 'bar'});
    recursivelySetTemplateModel(div, m);
    return new Future(() {
      expect(div.nodes.length, 4);
      expect(div.nodes[3].text, 'bar');
    });
  });

  test('ChangeRefId', () {
    var div = createTestHtml(
        '<template id="a">a:{{ }}</template>'
        '<template id="b">b:{{ }}</template>'
        '<template repeat="{{}}">'
          '<template ref="a" bind="{{}}"></template>'
        '</template>');
    var model = toObservable([]);
    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.nodes.length, 3);

      document.getElementById('a').id = 'old-a';
      document.getElementById('b').id = 'a';

      model..add(1)..add(2);
    }).then(endOfMicrotask).then((_) {

      expect(div.nodes.length, 7);
      expect(div.nodes[4].text, 'b:1');
      expect(div.nodes[6].text, 'b:2');
    });
  });

  test('Content', () {
    var div = createTestHtml(
        '<template><a></a></template>'
        '<template><b></b></template>');
    var templateA = div.nodes.first;
    var templateB = div.nodes.last;
    var contentA = templateBind(templateA).content;
    var contentB = templateBind(templateB).content;
    expect(contentA, isNotNull);

    expect(templateA.ownerDocument, isNot(equals(contentA.ownerDocument)));
    expect(templateB.ownerDocument, isNot(equals(contentB.ownerDocument)));

    expect(templateB.ownerDocument, templateA.ownerDocument);
    expect(contentB.ownerDocument, contentA.ownerDocument);

    expect(templateA.ownerDocument.window, window);
    expect(templateB.ownerDocument.window, window);

    expect(contentA.ownerDocument.window, null);
    expect(contentB.ownerDocument.window, null);

    expect(contentA.nodes.last, contentA.nodes.first);
    expect(contentA.nodes.first.tagName, 'A');

    expect(contentB.nodes.last, contentB.nodes.first);
    expect(contentB.nodes.first.tagName, 'B');
  });

  test('NestedContent', () {
    var div = createTestHtml(
        '<template>'
        '<template></template>'
        '</template>');
    var templateA = div.nodes.first;
    var templateB = templateBind(templateA).content.nodes.first;

    expect(templateB.ownerDocument, templateBind(templateA)
        .content.ownerDocument);
    expect(templateBind(templateB).content.ownerDocument,
        templateBind(templateA).content.ownerDocument);
  });

  test('BindShadowDOM', () {
    if (!ShadowRoot.supported) return null;

    var root = createShadowTestHtml(
        '<template bind="{{}}">Hi {{ name }}</template>');
    var model = toObservable({'name': 'Leela'});
    recursivelySetTemplateModel(root, model);
    return new Future(() => expect(root.nodes[1].text, 'Hi Leela'));
  });

  // Dart note: this test seems gone from JS. Keeping for posterity sake.
  test('BindShadowDOM createInstance', () {
    if (!ShadowRoot.supported) return null;

    var model = toObservable({'name': 'Leela'});
    var template = new Element.html('<template>Hi {{ name }}</template>');
    var root = createShadowTestHtml('');
    root.nodes.add(templateBind(template).createInstance(model));

    return new Future(() {
      expect(root.text, 'Hi Leela');

      model['name'] = 'Fry';
    }).then(endOfMicrotask).then((_) {
      expect(root.text, 'Hi Fry');
    });
  });

  test('BindShadowDOM Template Ref', () {
    if (!ShadowRoot.supported) return null;
    var root = createShadowTestHtml(
        '<template id=foo>Hi</template><template bind ref=foo></template>');
    recursivelySetTemplateModel(root, toObservable({}));
    return new Future(() => expect(root.nodes.length, 3));
  });

  // https://github.com/Polymer/TemplateBinding/issues/8
  test('UnbindingInNestedBind', () {
    var div = createTestHtml(
      '<template bind="{{outer}}" if="{{outer}}" syntax="testHelper">'
        '<template bind="{{inner}}" if="{{inner}}">'
          '{{ age }}'
        '</template>'
      '</template>');

    var syntax = new UnbindingInNestedBindSyntax();
    var model = toObservable({'outer': {'inner': {'age': 42}}});

    recursivelySetTemplateModel(div, model, syntax);

    return new Future(() {
      expect(syntax.count, 1);

      var inner = model['outer']['inner'];
      model['outer'] = null;

    }).then(endOfMicrotask).then((_) {
      expect(syntax.count, 1);

      model['outer'] = toObservable({'inner': {'age': 2}});
      syntax.expectedAge = 2;

    }).then(endOfMicrotask).then((_) {
      expect(syntax.count, 2);
    });
  });

  // https://github.com/toolkitchen/mdv/issues/8
  test('DontCreateInstancesForAbandonedIterators', () {
    var div = createTestHtml(
      '<template bind="{{}} {{}}">'
        '<template bind="{{}}">Foo</template>'
      '</template>');
    recursivelySetTemplateModel(div, null);
    return nextMicrotask;
  });

  test('CreateInstance', () {
    var div = createTestHtml(
      '<template bind="{{a}}">'
        '<template bind="{{b}}">'
          '{{ foo }}:{{ replaceme }}'
        '</template>'
      '</template>');
    var outer = templateBind(div.nodes.first);
    var model = toObservable({'b': {'foo': 'bar'}});

    var host = new DivElement();
    var instance = outer.createInstance(model, new TestBindingSyntax());
    expect(outer.content.nodes.first,
        templateBind(instance.nodes.first).ref);

    host.append(instance);
    return new Future(() {
      expect(host.firstChild.nextNode.text, 'bar:replaced');
    });
  });

  test('CreateInstance - sync error', () {
    var div = createTestHtml('<template>{{foo}}</template>');
    var outer = templateBind(div.nodes.first);
    var model = 1; // model is missing 'foo' should throw.
    expect(() => outer.createInstance(model, new TestBindingSyntax()),
        throwsA(isNoSuchMethodError));
  });

  test('CreateInstance - async error', () {
    var div = createTestHtml(
      '<template>'
        '<template bind="{{b}}">'
          '{{ foo }}:{{ replaceme }}'
        '</template>'
      '</template>');
    var outer = templateBind(div.nodes.first);
    var model = toObservable({'b': 1}); // missing 'foo' should throw.

    bool seen = false;
    runZoned(() => outer.createInstance(model, new TestBindingSyntax()),
      onError: (e) {
        expect(e, isNoSuchMethodError);
        seen = true;
      });
    return new Future(() { expect(seen, isTrue); });
  });

  test('Repeat - svg', () {
    var div = createTestHtml(
        '<svg width="400" height="110">'
          '<template repeat>'
            '<rect width="{{ width }}" height="{{ height }}" />'
          '</template>'
        '</svg>');

    var model = toObservable([{ 'width': 10, 'height': 11 },
                              { 'width': 20, 'height': 21 }]);
    var svg = div.firstChild;
    var template = svg.firstChild;
    templateBind(template).model = model;

    return new Future(() {
      expect(svg.nodes.length, 3);
      expect(svg.nodes[1].attributes['width'], '10');
      expect(svg.nodes[1].attributes['height'], '11');
      expect(svg.nodes[2].attributes['width'], '20');
      expect(svg.nodes[2].attributes['height'], '21');
    });
  });

  test('Bootstrap', () {
    var div = new DivElement();
    div.innerHtml =
      '<template>'
        '<div></div>'
        '<template>'
          'Hello'
        '</template>'
      '</template>';

    TemplateBindExtension.bootstrap(div);
    var template = templateBind(div.nodes.first);
    expect(template.content.nodes.length, 2);
    var template2 = templateBind(template.content.nodes.first.nextNode);
    expect(template2.content.nodes.length, 1);
    expect(template2.content.nodes.first.text, 'Hello');

    template = new Element.tag('template');
    template.innerHtml =
      '<template>'
        '<div></div>'
        '<template>'
          'Hello'
        '</template>'
      '</template>';

    TemplateBindExtension.bootstrap(template);
    template2 = templateBind(templateBind(template).content.nodes.first);
    expect(template2.content.nodes.length, 2);
    var template3 = templateBind(template2.content.nodes.first.nextNode);
    expect(template3.content.nodes.length, 1);
    expect(template3.content.nodes.first.text, 'Hello');
  });

  test('issue-285', () {
    var div = createTestHtml(
        '<template>'
          '<template bind if="{{show}}">'
            '<template id=del repeat="{{items}}">'
              '{{}}'
            '</template>'
          '</template>'
        '</template>');

    var template = div.firstChild;

    var model = toObservable({
      'show': true,
      'items': [1]
    });

    div.append(templateBind(template).createInstance(model,
        new Issue285Syntax()));

    return new Future(() {
      expect(template.nextNode.nextNode.nextNode.text, '2');
      model['show'] = false;
    }).then(endOfMicrotask).then((_) {
      model['show'] = true;
    }).then(endOfMicrotask).then((_) {
      expect(template.nextNode.nextNode.nextNode.text, '2');
    });
  });

  test('issue-141', () {
    var div = createTestHtml(
        '<template bind>'
          '<div foo="{{foo1}} {{foo2}}" bar="{{bar}}"></div>'
        '</template>');

    var model = toObservable({
      'foo1': 'foo1Value',
      'foo2': 'foo2Value',
      'bar': 'barValue'
    });

    recursivelySetTemplateModel(div, model);
    return new Future(() {
      expect(div.lastChild.attributes['bar'], 'barValue');
    });
  });

  test('issue-18', () {
    var delegate = new Issue18Syntax();

    var div = createTestHtml(
        '<template bind>'
          '<div class="foo: {{ bar }}"></div>'
        '</template>');

    var model = toObservable({'bar': 2});

    recursivelySetTemplateModel(div, model, delegate);

    return new Future(() {
      expect(div.lastChild.attributes['class'], 'foo: 2');
    });
  });

  test('issue-152', () {
    var div = createTestHtml('<template ref=notThere></template>');
    var template = div.firstChild;

    // if a ref cannot be located, a template will continue to use itself
    // as the source of template instances.
    expect(template, templateBind(template).ref);
  });
}

compatTests() {
  test('underbar bindings', () {
    var div = createTestHtml(
        '<template bind>'
          '<div _style="color: {{ color }};"></div>'
          '<img _src="{{ url }}">'
          '<a _href="{{ url2 }}">Link</a>'
          '<input type="number" _value="{{ number }}">'
        '</template>');

    var model = toObservable({
      'color': 'red',
      'url': 'pic.jpg',
      'url2': 'link.html',
      'number': 4
    });

    recursivelySetTemplateModel(div, model);
    return new Future(() {
      var subDiv = div.firstChild.nextNode;
      expect(subDiv.attributes['style'], 'color: red;');

      var img = subDiv.nextNode;
      expect(img.attributes['src'], 'pic.jpg');

      var a = img.nextNode;
      expect(a.attributes['href'], 'link.html');

      var input = a.nextNode;
      expect(input.value, '4');
    });
  });
}

class Issue285Syntax extends BindingDelegate {
  prepareInstanceModel(template) {
    if (template.id == 'del') return (val) => val * 2;
  }
}

class TestBindingSyntax extends BindingDelegate {
  prepareBinding(String path, name, node) {
    if (path.trim() == 'replaceme') {
      return (m, n, oneTime) => new PathObserver('replaced', '');
    }
    return null;
  }
}

class UnbindingInNestedBindSyntax extends BindingDelegate {
  int expectedAge = 42;
  int count = 0;

  prepareBinding(path, name, node) {
    if (name != 'text' || path != 'age') return null;

    return (model, _, oneTime) {
      expect(model['age'], expectedAge);
      count++;
      return new PathObserver(model, path);
    };
  }
}

class Issue18Syntax extends BindingDelegate {
  prepareBinding(path, name, node) {
    if (name != 'class') return null;

    return (model, _, oneTime) => new PathObserver(model, path);
  }
}
