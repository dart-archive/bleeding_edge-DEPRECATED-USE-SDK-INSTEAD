// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('pond_ui');

#import('dart:html', prefix:'html');
#import('dart:isolate', prefix: 'isolate');
#import('editors.dart');

class PondUI {
  Editor dartEditor;
  final List<Marker> markers;

  Editor warningEditor;
  Editor jsEditor;
  Editor htmlEditor;
  OutConsole outConsole;
  isolate.SendPort compilerIsolate;

  bool _compiling = false;
  bool _compileAgain = false;

  TabGroup outTabs;
  Tab errorTab;
  Tab webTab;

  PondUI() : markers = [] {
    compilerIsolate = isolate.spawnUri("compiler.dart.js");
  }

  void setupAndRun(EditorFactory editors) {
    // forward output messages to the out-console.
    outConsole = new OutConsole();

    editors.newEditor("dartEditor", "dart", _scheduleCompilation)
        .then((Editor e) {
            dartEditor = e;
            editors.newEditor("htmlEditor", "htmlmixed").then((Editor e) {
              htmlEditor = e;
              editors.newEditor("jsEditor", "javascript").then((Editor e) {
                jsEditor = e;
                editors.newEditor("warningEditor", "diff").then((Editor e) {
                  warningEditor = e;
                  setup();
                });
              });
          });
        });

  }

  void setup() {
    dartEditor.setText(SampleCode.DART);
    htmlEditor.setText(SampleCode.HTML);

    html.document.query('#clearButton').on.click.add((e) {
      _clearOutput();
    });

    html.document.query('#runButton').on.click.add((e) {
      _scheduleCompilation();
    });

    new TabGroup([
        new Tab('tab-dart', 'dartEditor', dartEditor),
        new Tab('tab-html', 'htmlEditor', htmlEditor)]).addListeners();

    outTabs = new TabGroup([
        new Tab('tab-warnings', 'warningEditor', warningEditor),
        new Tab('tab-console', 'console'),
        new Tab('tab-web', 'results'),
        new Tab('tab-js', 'jsEditor', jsEditor)]);
    outTabs.addListeners();
    errorTab = outTabs.tabs[0];
    webTab = outTabs.tabs[2];
  }

  void _clearOutput() {
    warningEditor.setText('');
    jsEditor.setText('');
    html.document.query('#resultFrame').attributes['src'] = 'about:blank';
    for (Marker marker in markers) {
      marker.clear();
    }
    markers.clear();
  }

  void _scheduleCompilation() {
    if (_compiling) {
      _compileAgain = true;
    } else {
      _compile();
    }
  }

  void _compile() {
    _compiling = true;
    final totalWatch = new Stopwatch.start();
    _clearOutput();
    // TODO(sigmund): cleanup using 'await' as follows:
    // String userCode = await dartEditor.getText();
    dartEditor.getText().then((userCode) {
      final compileWatch = new Stopwatch.start();
      compilerIsolate.call({
          'code': userCode,
          'warningsAsErrors':
               html.document.query('#warningCheckbox').dynamic.checked
        }).then((reply) {
          String warnings = '';
          for (final warning in reply['warnings']) {
            String prefix = warning["prefix"];
            warnings = '${warnings}$prefix ${warning["msg"]}';
            warnings = '${warnings}[${warning["locationText"]}]\n';
            if (warning['filename'] == 'user.dart') {
              final start = new Position(warning['line'], warning['column']);
              final end = new Position(
                  warning['endLine'], warning['endColumn']);
              int kind = Marks.NONE;
              String prefix = warning['prefix'];
              if (prefix.startsWith('error') || prefix.startsWith('fatal')) {
                kind = Marks.ERROR;
              } else if (prefix.startsWith('warning')) {
                kind = Marks.WARNING;
              }
              dartEditor.mark(start, end, kind).then(
                (Marker m) { markers.add(m); });
            }
          }

          if (reply['success']) {
            outConsole.clear();
            String code = reply['code'];
            int lastLine = code.lastIndexOf(";", code.lastIndexOf(";") - 1);
            String injectedCode =
                '${OutConsole.CODE_PATCH}${code.substring(lastLine)}';
            code = "${code.substring(0, lastLine)}$injectedCode";
            jsEditor.setText(code);
            htmlEditor.getText().then((htmlText) {
              var start = htmlText.indexOf("{{DART}}");
              htmlText = htmlText.substring(0, start) + code
                + htmlText.substring(start + "{{DART}}".length);
              htmlText = htmlText.replaceAll(
                "application/dart", "text/javascript");
              html.document.query("#resultFrame").attributes["src"] =
              _toDataURL(htmlText);
            });
          }
          compileWatch.stop();
          totalWatch.stop();
          int time = reply["time"];
          warnings = '$warnings\ncompile time: ${time}ms\n';
          time = compileWatch.elapsedInMs();
          warnings = '$warnings\ncompile + isolate time: ${time}ms\n';
          time = totalWatch.elapsedInMs();
          warnings = '${warnings}total time: ${time}ms\n';
          warningEditor.setText(warnings);
          if (_compileAgain) {
            _compileAgain = false;
            _compile();
          } else {
            _compiling = false;
          }
        });
    });
  }

  // TODO(sigmund): remove use of 'native'.
  String _toDataURL(text) native '''
    var preamble = "data:text/html;charset=utf-8,";
    var escaped = window.encodeURIComponent(text);
    return preamble + escaped;
  ''';
}

class Tab {
  html.Element tab;
  html.Element contents;
  Editor editor;

  Tab(tabId, contentsId, [this.editor = null]) {
    tab = html.document.query("#$tabId");
    contents = html.document.query("#$contentsId");
  }

  void show() {
    tab.classes.add("tab-selected");
    contents.classes.remove("hidden");
    if (editor != null)  editor.refresh();
  }

  void hide() {
    tab.classes.remove("tab-selected");
    contents.classes.add("hidden");
  }
}

class TabGroup {
  List<Tab> tabs;

  TabGroup(this.tabs);

  void addListeners() {
    tabs.forEach((t) { t.tab.on.click.add((e) => selectTab(t)); });
  }

  void selectTab(Tab tab) {
    tabs.forEach((t) => t == tab ? t.show() : t.hide());
  }
}

/** Controls the console output. */
class OutConsole {
  html.Element _root;

  OutConsole() {
    _root = html.document.query("#console");
    html.window.on.message.add((e) { // output is forwarded using postMessage
      var msg = e.data;
      if (msg is List && msg[0] == 'app-to-pond-print') {
        addLine(msg[1]);
      }
    });
  }

  void addLine(String line) {
    _root.nodes.add(new html.Element.html(
          '<span class="console-line">${_htmlEscape(line)}</pre>'));
  }

  void clear() {
    _root.nodes.clear();
  }

  String _htmlEscape(String s) {
    return s.replaceAll('&', '&amp;')
        .replaceAll('<','&lt;')
        .replaceAll('>','&gt;');
  }

  /**
   * A patch (extra code added) to the code generated by of frog so that pond
   * can display printed messages correctly.
   */
  static String CODE_PATCH = @'''

  /* Code added by pond to forward output messages to the UI out console. */
  var $original_print = print$;
  print$ = function (e) {
    window.parent.postMessage(['app-to-pond-print', e], '*');
    $original_print(e);
  }
  ''';
}

class SampleCode {
  final static String DART = '''
#import("dart:html");
void main() {
  window.on.load.add((Event e) {
    Element element = document.query("#status");
    if (element == null) {
      throw "can't find status element";
    }
    element.innerHTML = "hello, dart, click me";
    print("hello dart!");
    element.on.click.add(
      (Event) {
        if (element.classes.remove("clicked")) {
          return;
        }
        element.classes.add("clicked");
      });
    });
}
''';

  final static String HTML = '''
<html>
  <head>
    <style type="text/css">
      .clicked {
        background: #003300;
      }
    </style>
    <script type="application/dart">
      {{DART}}
    </script>
  </head>
  <body>
    <h2 id="status">not running</h2>
  </body>
</html>
''';
}
