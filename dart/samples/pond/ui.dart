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
  isolate.SendPort compilerIsolate;

  PondUI() : markers = [] {
    compilerIsolate = isolate.spawnUri("compiler.dart.js");
  }

  void clearOutput() {
    warningEditor.setText('');
    jsEditor.setText('');
    html.document.query('#resultFrame').attributes['src'] = 'about:blank';
    for (Marker marker in markers) {
      marker.clear();
    }
    markers.clear();
  }

  void setupAndRun(EditorFactory editors) {
    // TODO(sigmund): cleanup using 'await' as follows:
    //   dartEditor = await editors.newEditor("dartEditor", "dart");
    //   htmlEditor = await editors.newEditor("htmlEditor", "htmlmixed");
    //   jsEditor = await editors.newEditor("jsEditor", "javascript");
    //   warningEditor = await editors.newEditor("warningEditor", "diff");
    //   await run();

    editors.newEditor("dartEditor", "dart").then((Editor e) {
      dartEditor = e;
      editors.newEditor("htmlEditor", "htmlmixed").then((Editor e) {
        htmlEditor = e;
        editors.newEditor("jsEditor", "javascript").then((Editor e) {
          jsEditor = e;
          editors.newEditor("warningEditor", "diff").then((Editor e) {
            warningEditor = e;
            run();
          });
        });
      });
    });
  }

  void run() {
    dartEditor.setText(SampleCode.DART);
    htmlEditor.setText(SampleCode.HTML);

    html.document.query('#clearButton').on.click.add((e) {
      clearOutput();
    });

    html.document.query('#runButton').on.click.add((e) {
      final totalWatch = new Stopwatch.start();
      clearOutput();
      // TODO(sigmund): cleanup using 'await' as follows:
      // String userCode = await dartEditor.getText();
      dartEditor.getText().then((userCode) {
        final compileWatch = new Stopwatch.start();
        compilerIsolate.call({
            'code': userCode,
            'warningsAsErrors':
                 html.document.query('#warningCheckbox').dynamic.checked
          }).receive((reply, _) {
            String warnings = '';
            for (final warning in reply['warnings']) {
              warnings = '${warnings}${warning["prefix"]} ${warning["msg"]}';
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
              String code = reply['code'];
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
          });
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

class SampleCode {
  final static String DART = '''
#import("dart:html");
void main() {
  window.on.load.add((Event e) {
    Element element = document.query("#status");
    if (element == null) {
      throw "can't find status element";
    }
    element.innerHTML = "hello, dart";
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
