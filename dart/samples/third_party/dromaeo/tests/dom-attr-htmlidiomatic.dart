library dom_attr_html;
import 'dart:html';
import 'dart:json';
import 'dart:math' as Math;
part 'Common.dart';
part 'RunnerSuite.dart';

void main() {
  final int num = 10240;

  // Try to force real results.
  var ret;

  Element elem = document.query('#test1');
  Element a = document.queryAll('a')[0];

  new Suite(window, 'dom-attr')
    .test('getAttribute', () {
      for (int i = 0; i < num; i++)
        ret = elem.attributes['id'];
    })
    .test('element.property', () {
      for (int i = 0; i < num * 2; i++)
        ret = elem.id;
    })
    .test('setAttribute', () {
        for (int i = 0; i < num; i++)
        a.attributes['id'] = 'foo';
    })
    .test('element.property = value', () {
      for (int i = 0; i < num; i++)
        a.id = 'foo';
    })
    .end();
}
