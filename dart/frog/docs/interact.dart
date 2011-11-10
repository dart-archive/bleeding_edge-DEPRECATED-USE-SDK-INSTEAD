#import('../../client/html/html.dart');

main() {
  window.on.contentLoaded.add((e) {
    for (var elem in document.queryAll('.method, .field')) {
      var showCode = elem.query('.show-code');
      var pre = elem.query('pre.source');
      showCode.on.click.add((e) {
        if (pre.classes.contains('expanded')) {
          pre.classes.remove('expanded');
        } else {
          pre.classes.add('expanded');
        }
      });
    }
  });
}