#library("dom_query_html");
#import("dart:html");
#import('../common/common.dart');
#import("dart:math", prefix: "Math");
#source("Common.dart");
#source("RunnerSuite.dart");

void main() {
  final int num = 40;

  // Try to force real results.
  var ret;

  String html = document.body.innerHTML;

  new Suite(window, 'dom-query')
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
      document.body.nodes.add(div);
    })
    .test('getElementById', () {
      for (int i = 0; i < num * 30; i++) {
        ret = document.query('#testA$num').$dom_nodeType;
        ret = document.query('#testB$num').$dom_nodeType;
        ret = document.query('#testC$num').$dom_nodeType;
        ret = document.query('#testD$num').$dom_nodeType;
        ret = document.query('#testE$num').$dom_nodeType;
        ret = document.query('#testF$num').$dom_nodeType;
      }
    })
    .test('getElementById (not in document)', () {
      for (int i = 0; i < num * 30; i++) {
        ret = document.query('#testA');
        ret = document.query('#testB');
        ret = document.query('#testC');
        ret = document.query('#testD');
        ret = document.query('#testE');
        ret = document.query('#testF');
      }
    })
    .test('getElementsByTagName(div)', () {
      for (int i = 0; i < num; i++) {
        List<Element> elems = document.queryAll('div');
        ret = elems.last().$dom_nodeType;
      }
    })
    .test('getElementsByTagName(p)', () {
      for (int i = 0; i < num; i++) {
        List<Element> elems = document.queryAll('p');
        ret = elems.last().$dom_nodeType;
      }
    })
    .test('getElementsByTagName(a)', () {
      for (int i = 0; i < num; i++) {
        List<Element> elems = document.queryAll('a');
        ret = elems.last().$dom_nodeType;
      }
    })
    .test('getElementsByTagName(*)', () {
      for (int i = 0; i < num; i++) {
        List<Element> elems = document.queryAll('*');
        ret = elems.last().$dom_nodeType;
      }
    })
    .test('getElementsByTagName (not in document)', () {
      for (int i = 0; i < num; i++) {
        List<Element> elems = document.queryAll('strong');
        ret = elems.isEmpty();
      }
    })
    .test('getElementsByName', () {
      for (int i = 0; i < num * 20; i++) {
        List<Element> elems = document.queryAll('[name="test$num"]');
        ret = elems.last().$dom_nodeType;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().$dom_nodeType;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().$dom_nodeType;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().$dom_nodeType;
      }
    })
    .test('getElementsByName (not in document)', () {
      for (int i = 0; i < num * 20; i++) {
        ret = document.queryAll('[name="test"]').length == 0;
        ret = document.queryAll('[name="test"]').length == 0;
        ret = document.queryAll('[name="test"]').length == 0;
        ret = document.queryAll('[name="test"]').length == 0;
        ret = document.queryAll('[name="test"]').length == 0;
      }
    })
    .end();
}
