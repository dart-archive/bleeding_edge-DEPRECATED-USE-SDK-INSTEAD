// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("pond_ui");

#import("dart:html", prefix:'html');
#import("editors.dart");
#import("../../frog/lang.dart");
#import("html_file_system.dart");

class PondUI {
  Editor dartEditor;
  final List<Marker> markers;

  Editor warningEditor;
  Editor jsEditor;
  Editor htmlEditor;

  PondUI() : markers = [] {}

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
      int millis0 = new Date.now().value;
      clearOutput();
      String warnings = '';
      HtmlFileSystem fs = new HtmlFileSystem();
      parseOptions('../../frog', ['dummyArg1', 'dummyArg2', 'user.dart'], fs);
      // TODO(sigmund): cleanup using 'await' as follows:
      // String userCode = await dartEditor.getText();
      dartEditor.getText().then((userCode) {
        fs.writeString("user.dart", userCode);
        options.useColors = false;
        options.warningsAsErrors =
          html.document.query('#warningCheckbox').dynamic.checked;

        int millis1 = new Date.now().value;
        initializeWorld(fs);
        world.messageHandler = (String prefix, String msg, SourceSpan span) {
          warnings += prefix + msg;
          if (span != null) {
            warnings += ' [' + span.locationText + '] \n';
            if (span.file.filename == 'user.dart') {
              final start = new Position(span.line, span.column);
              final end = new Position(span.endLine, span.endColumn);
              int kind = Marks.NONE;
              if (prefix.startsWith('error') || prefix.startsWith('fatal')) {
                kind = Marks.ERROR;
              } else if (prefix.startsWith('warning')) {
                kind = Marks.WARNING;
              }
              // TODO(sigmund): cleanup using 'await' as follows:
              // markers.add(await dartEditor.mark(start, end, kind));
              dartEditor.mark(start, end, kind).then(
                  (Marker m) { markers.add(m); });
            }
          } else {
            warnings += '\n';
          }
        };
        bool success = world.compile();
        if (success) {
          String code = world.getGeneratedCode();
          jsEditor.setText(code);
          // TODO(sigmund): cleanup using 'await' as follows:
          // String htmlTxt = await htmlEditor.getText();
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
        int millis2 = new Date.now().value;
        warnings += '\ncompile time: ${millis2 - millis1}ms\n';
        warnings += '\ntotal time: ${millis2 - millis0}ms\n';
        warningEditor.setText(warnings);
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
