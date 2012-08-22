#library("dom_modify_html");
#import("dart:html");
#import("dart:json");
#import("dart:math", prefix: "Math");
#source("Common.dart");
#source("RunnerSuite.dart");

void main() {
  final int num = 400;

  String str = 'null';
  // Very ugly way to build up the string, but let's mimic JS version as much as possible.
  for (int i = 0; i < 1024; i++) {
    str = "$str${new String.fromCharCodes([((25 * Math.random()) + 97).toInt()])}";
  }

  List<Node> elems = <Node>[];

  // Try to force real results.
  var ret;

  final htmlstr = document.body.innerHTML;

  new Suite(window, 'dom-modify')
    .test('createElement', () {
      for (int i = 0; i < num; i++) {
        ret = new Element.tag('div');
        ret = new Element.tag('span');
        ret = new Element.tag('table');
        ret = new Element.tag('tr');
        ret = new Element.tag('select');
      }
    })
    .test('createTextNode', () {
      for (int i = 0; i < num; i++) {
        ret = new Text(str);
        ret = new Text('${str}2');
        ret = new Text('${str}3');
        ret = new Text('${str}4');
        ret = new Text('${str}5');
      }
    })
    .test('innerHTML', () {
      document.body.innerHTML = htmlstr;
    })
    .prep(() {
      elems = new List<Node>();
      final telems = document.body.nodes;
      for (int i = 0; i < telems.length; i++) {
        elems.add(telems[i]);
      }
    })
    .test('cloneNode', () {
      for (int i = 0; i < elems.length; i++) {
        ret = elems[i].clone(false);
        ret = elems[i].clone(true);
        ret = elems[i].clone(true);
        }
    })
    .test('appendChild', () {
      for (int i = 0; i < elems.length; i++)
        document.body.$dom_appendChild(elems[i]);
    })
    .test('insertBefore', () {
      for (int i = 0; i < elems.length; i++)
        document.body.insertBefore(elems[i], document.body.$dom_firstChild);
    })
    .end();
}
