#library("dom_query_html");
#import("dart:html");
#import('../common/common.dart');
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
        ret = document.query('#testA$num').hidden;
        ret = document.query('#testB$num').hidden;
        ret = document.query('#testC$num').hidden;
        ret = document.query('#testD$num').hidden;
        ret = document.query('#testE$num').hidden;
        ret = document.query('#testF$num').hidden;
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
        var elems = document.queryAll('div');
        ret = elems.last().hidden;
      }
    })
    .test('getElementsByTagName(p)', () {
      for (int i = 0; i < num; i++) {
        final elems = document.queryAll('p');
        ret = elems.last().hidden;
      }
    })
    .test('getElementsByTagName(a)', () {
      for (int i = 0; i < num; i++) {
        var elems = document.queryAll('a');
        ret = elems.last().hidden;
      }
    })
    .test('getElementsByTagName(*)', () {
      for (int i = 0; i < num; i++) {
        var elems = document.queryAll('*');
        ret = elems.last().hidden;
      }
    })
    .test('getElementsByTagName (not in document)', () {
      for (int i = 0; i < num; i++) {
        var elems = document.queryAll('strong');
        ret = elems.length == 0;
      }
    })
    .test('getElementsByName', () {
      for (int i = 0; i < num * 20; i++) {
        var elems = document.queryAll('[name="test$num"]');
        ret = elems.last().hidden;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().hidden;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().hidden;
        elems = document.queryAll('[name="test$num"]');
        ret = elems.last().hidden;
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
