#library("dom_traverse");
#import("dart:html");
#import('../common/common.dart');
#source("Common.dart");
#source("RunnerSuite.dart");


void main() {
  final int num = 40;

  // Try to force real results.
  var ret;

  String html = document.body.innerHTML;

  new Suite(window, 'dom-traverse')
    .prep(() {
      html = BenchUtil.replaceAll(html, 'id="test(\\w).*?"', (Match match) {
        final group = match.group(1);
        return 'id="test${group}${num}"';
      });
      html = BenchUtil.replaceAll(html, 'name="test.*?"', (Match match) {
        return 'name="test${num}"';
      });
      html = BenchUtil.replaceAll(html, 'class="foo.*?"', (Match match) {
        return 'class="foo test${num} bar"';
      });

      final div = new Element.tag('div');
      div.innerHTML = html;
      document.body.$dom_appendChild(div);
    })
    .test('firstChild', () {
      final nodes = document.body.nodes;
      final nl = nodes.length;

      for (int i = 0; i < num; i++) {
        for (int j = 0; j < nl; j++) {
          Node cur = nodes[j];
          while (cur !== null) {
            cur = cur.$dom_firstChild;
          }
          ret = cur;
        }
      }
    })
    .test('lastChild', () {
      final nodes = document.body.nodes;
      final nl = nodes.length;

      for (int i = 0; i < num; i++) {
        for (int j = 0; j < nl; j++) {
          Node cur = nodes[j];
          while (cur !== null) {
            cur = cur.$dom_lastChild;
          }
          ret = cur;
        }
      }
    })
    .test('nextSibling', () {
      for (int i = 0; i < num * 2; i++) {
        Node cur = document.body.$dom_firstChild;
        while (cur !== null) {
          cur = cur.nextNode;
        }
        ret = cur;
      }
    })
    .test('previousSibling', () {
      for (int i = 0; i < num * 2; i++) {
        Node cur = document.body.$dom_lastChild;
        while (cur !== null) {
          cur = cur.previousNode;
        }
        ret = cur;
      }
    })
    .test('childNodes', () {
      for (int i = 0; i < num; i++) {
        final nodes = document.body.nodes;
        for (int j = 0; j < nodes.length; j++) {
          ret = nodes[j];
        }
      }
    })
    .end();
}
