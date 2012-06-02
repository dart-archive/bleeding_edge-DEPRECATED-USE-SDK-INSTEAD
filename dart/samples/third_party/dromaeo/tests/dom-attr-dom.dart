#library("dom_attr");
#import("dart:dom_deprecated");
#import("dart:json");
#source("Common.dart");
#source("RunnerSuite.dart");

void main() {
  final int num = 10240;

  // Try to force real results.
  var ret;

  HTMLElement elem = document.getElementById('test1');
  HTMLElement a = document.getElementsByTagName('a')[0];

  new Suite(window, 'dom-attr')
    .test('getAttribute', () {
      for (int i = 0; i < num; i++)
        ret = elem.getAttribute('id');
    })
    .test('element.property', () {
      for (int i = 0; i < num * 2; i++)
        ret = elem.id;
    })
    .test('setAttribute', () {
        for (int i = 0; i < num; i++)
        a.setAttribute('id', 'foo');
    })
    .test('element.property = value', () {
      for (int i = 0; i < num; i++)
        a.id = 'foo';
    })
    .end();
}
