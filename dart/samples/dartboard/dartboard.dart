// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:html", prefix:"html");
#import("../../frog/lang.dart", prefix:"lang");
#import("../../frog/file_system.dart", prefix:"file_system");

class Dartboard {
  final List<Object> markers;

  Dartboard() : markers = new List() {}

  void clearOutput() {
    setEditorText("warningEditor", "");
    setEditorText("jsEditor", "");
    html.document.query("#resultFrame").attributes["src"] = "about:blank";
    for (Object marker in markers) {
      clearMarker(marker);
    }
    markers.clear();
  }

  void run() {
    setEditorText("dartEditor", SampleCode.dart);
    setEditorText("htmlEditor", SampleCode.getHtml());

    html.document.query("#clearButton").on.click.add(void _(html.Event e) {
      clearOutput();
    });

    html.document.query("#runButton").on.click.add(void _(html.Event e) {
      clearOutput();
      String warnings = "";
      HtmlFileSystem fs = new HtmlFileSystem();


      List<String> args = ["dummyArg1", "dummyArg2", "user.dart"]; 
      lang.options = new lang.FrogOptions("dartdir/frog", args, fs);
      lang.options.useColors = false;
      lang.options.warningsAsErrors =
        html.document.query("#warningCheckbox").dynamic.checked;

      int millis1 = new Date.now().value;
      if (lang.world === null) {
        lang.initializeWorld(fs);
      } else {
        lang.world.reset();
      }

      lang.world.messageHandler = (String prefix, String message, lang.SourceSpan span) {
        warnings += prefix + message + span.locationText + "\n";
        if (span.file.filename == "user.dart") {
          int startLine = SpanHelper.startLine(span);
          int startCol = SpanHelper.startCol(span);
          int endLine = SpanHelper.endLine(span);
          int endCol = SpanHelper.endCol(span);
          String cssClass = null;
          if (prefix.startsWith("error") || prefix.startsWith("fatal")) {
            cssClass = "compile_error";
          } else if (prefix.startsWith("warning")) {
            cssClass = "compile_warning";
          }
          markers.add(markText(startLine, startCol, endLine, endCol, cssClass));
        }
      };
      bool success = lang.world.compile();
      if (success) {
        String code = lang.world.getGeneratedCode();
        setEditorText("jsEditor", code);
      }
      int millis2 = new Date.now().value;
      warnings += "\ncompile time: ${millis2 - millis1}ms\n";
      setEditorText("warningEditor", warnings);
    });
  }

  // TODO - remove native
  static String markText(int startLine, int startCol,
        int endLine, int endCol, String cssClass) native
     'return window.markText(startLine, startCol, endLine, endCol, cssClass);';

  // TODO - remove native
  static void clearMarker(Object marker) native 'marker.clear();';

  // TODO - remove native
  static String getEditorText(String id) native
    'return window.getEditorText(id)';

  // TODO - remove native
  static String setEditorText(String id, String text) native
    'window.setEditorText(id, text);';
}

class HtmlFileSystem implements file_system.FileSystem {
  final Object frameDocument;

  HtmlFileSystem() : frameDocument = getFrameDocument() {}

  // TODO - remove native
  static Object getFrameDocument() native
    'return document.getElementById("dartlibFrame").contentDocument;';

  // TODO - remove native
  static String getElementText(Object frame, String id) native
    'return frame.getElementById(id).text;';

  String readAll(String filename) {
    filename = resolveDartColon(filename);
    if (filename == "user.dart") {
      return Dartboard.getEditorText("dartEditor");
    }
    String id = filename.replaceAll(".", "_").replaceAll("/", "_");
    return getElementText(frameDocument, id);
  }
  
  static String resolveDartColon(String filename) {
    if (!filename.startsWith("dart:")) {
      return filename;
    }    
    String symbol = filename.substring("dart:".length);
    String relPath = symbolToRelPath[symbol];
    if (relPath === null) {
      throw new Exception("dart:$symbol not recognized"); 
    }
    return "dartdir/" + relPath;
  }

  void writeString(String outfile, String text) {
    throw new UnsupportedOperationException();
  }

  bool fileExists(String filename) {
    return true;
  }

  void createDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }
  void removeDirectory(String path, [bool recursive]) {
    throw new UnsupportedOperationException();
  }
  
  static final Map<String, String> symbolToRelPath = const <String>{
    "core" : "frog/lib/corelib.dart",
    "dom" : "lib/dom/frog/dom_frog.dart",
    "html" : "lib/html/html_frog.dart",
    "json" : "lib/json/json_frog.dart",
    "uri" : "lib/uri/uri.dart",
    "utf" : "lib/utf/utf.dart"
  };    
}

void main() {
  // TODO - temporary, the if(false) here is so that tree-shaking doesn't
  // delete frogPondMain.  (We want to be able to call frogPondMain at
  // the appropriate time after js setup.)
  if (false) {
    dartboardMain();
  }
}

void dartboardMain() {
  new Dartboard().run();
}

/** Slightly friendlier interface to SourceSpan */
class SpanHelper {
  static int startLine(lang.SourceSpan span) {
    return span.file.getLine(span.start);
  }

  static int startCol(lang.SourceSpan span) {
    return span.file.getColumn(span.file.getLine(span.start), span.start);
  }

  static int endLine(lang.SourceSpan span) {
    return span.file.getLine(span.end);
  }

  static int endCol(lang.SourceSpan span) {
    return span.file.getColumn(span.file.getLine(span.end), span.end);
  }
}

class SampleCode {

  static final String dart = '''
#import("dart:html");
void main() {
  window.on.load.add(void handler(Event e) {
    Element element = document.query("#status");
    if (element === null) {
      throw "can't find status element";
    }
    element.innerHTML = "hello, dart";
    element.on.click.add(
      void handler(Event e) {
        if (element.classes.remove("highlight")) {
          return;
        }
        element.classes.add("highlight");
      });
    });
}
''';

  static String getHtml() { 
    return '''
<html>
  <head>
    <style type="text/css">
      .highlight {
        background: #003300;
      }
    </style>
    <scrip_t type="application/dart">
      {{DART}}
    </scrip_t>
  </head>
  <body>
    <h2 id="status">not running</h2>
  </body>
</html>
'''.replaceAll("scrip_t", "script");
  }
}

