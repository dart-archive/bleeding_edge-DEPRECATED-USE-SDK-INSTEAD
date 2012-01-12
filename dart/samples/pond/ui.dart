// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("pond_ui");

#import("dart:html");
#import("../../frog/lang.dart");
#import("html_file_system.dart");
#import("util.dart");

class PondUI {
  final List<Object> markers;

  PondUI() : markers = new List() {}

  void clearOutput() {
    setEditorText('warningEditor', '');
    setEditorText('jsEditor', '');
    document.query('#resultFrame').attributes['src'] = 'about:blank';
    for (Object marker in markers) {
      clearMarker(marker);
    }
    markers.clear();
  }

  void run() {
    setEditorText('dartEditor', SampleCode.DART);
    setEditorText('htmlEditor', SampleCode.HTML);

    document.query('#clearButton').on.click.add((Event e) {
      clearOutput();
    });

    document.query('#runButton').on.click.add((Event e) {
      clearOutput();
      String warnings = '';
      HtmlFileSystem fs = new HtmlFileSystem();
      parseOptions('frogroot', ['dummyArg1', 'dummyArg2', 'user.dart'], fs);
      options.useColors = false;
      options.warningsAsErrors =
        document.query('#warningCheckbox').dynamic.checked;

      int millis1 = new Date.now().value;
      initializeWorld(fs);
      world.messageHandler = (String prefix, String message, SourceSpan span) {
        warnings += prefix + message;
        if (span != null) {
          warnings += ' [' + span.locationText + '] \n';
          if (span.file.filename == 'user.dart') {
            int startLine = SpanHelper.startLine(span);
            int startCol = SpanHelper.startCol(span);
            int endLine = SpanHelper.endLine(span);
            int endCol = SpanHelper.endCol(span);
            String cssClass = null;
            if (prefix.startsWith('error') || prefix.startsWith('fatal')) {
              cssClass = 'compile_error';
            } else if (prefix.startsWith('warning')) {
              cssClass = 'compile_warning';
            }
            markers.add(markText(startLine, startCol, endLine, endCol, cssClass));
          }
        } else {
          warnings += '\n';
        }
      };
      bool success = world.compile();
      if (success) {
        String code = world.getGeneratedCode();
        setEditorText('jsEditor', code);
      }
      int millis2 = new Date.now().value;
      warnings += '\ncompile time: ${millis2 - millis1}ms\n';
      setEditorText('warningEditor', warnings);
    });
  }
}

// APIs to access the JS editor component of the UI.
// TODO(sigmund,mattsh): remove 'native' thoughout this file, and replace this
// with an appropriate JS interop layer.

String markText(int startLine, int startCol,
      int endLine, int endCol, String cssClass) native
   'return window.markText(startLine, startCol, endLine, endCol, cssClass);';

void clearMarker(Object marker) native 'marker.clear();';

String getEditorText(String id) native
  'return window.getEditorText(id)';

String setEditorText(String id, String text) native
  'window.setEditorText(id, text);';

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
