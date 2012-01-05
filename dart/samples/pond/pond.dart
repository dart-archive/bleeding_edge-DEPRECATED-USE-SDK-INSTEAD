// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:html");
#import("../../frog/lang.dart");
#import("../../frog/file_system.dart");

// TODO - remove once corelib has Exception base class
class Exception_ implements Exception {
  final String message;
  Exception_(String message) : message = message {}
  String toString() { return message; }
}

class Pond {
  final List<Object> markers;
  
  Pond() : markers = new List() {}
    
  void clearOutput() {
    setEditorText("warningEditor", "");      
    setEditorText("jsEditor", "");
    document.query("#resultFrame").attributes["src"] = "about:blank";
    for (Object marker in markers) {
      clearMarker(marker);
    }
    markers.clear();
  }

  void run() {
    setEditorText("dartEditor", PondSampleCode.dart);
    setEditorText("htmlEditor", PondSampleCode.html);
    
    document.query("#clearButton").on.click.add((Event e) {
      clearOutput();
    });

    document.query("#runButton").on.click.add((Event e) {
      clearOutput();
      String warnings = "";      
      HtmlFileSystem fs = new HtmlFileSystem();
      parseOptions("frogroot", ["dummyArg1", "dummyArg2", "user.dart"], fs);
      options.useColors = false;
      options.warningsAsErrors = 
        document.query("#warningCheckbox").dynamic.checked;

      int millis1 = new Date.now().value;
      initializeWorld(fs);
      world.messageHandler = (String prefix, String message, SourceSpan span) {
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
      bool success = world.compile();
      if (success) {
        String code = world.getGeneratedCode();
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
  
class HtmlFileSystem implements FileSystem {
  final Object frameDocument;

  HtmlFileSystem() : frameDocument = getFrameDocument() {}

  // TODO - remove native
  static Object getFrameDocument() native
    'return document.getElementById("dartlibFrame").contentDocument;';

  // TODO - remove native
  static String getElementText(Object frame, String id) native
    'return frame.getElementById(id).text;';

  String readAll(String filename) {
    if (filename == "user.dart") {
      return Pond.getEditorText("dartEditor");
    }
    int slash1 = filename.lastIndexOf("/", filename.length);
    if (slash1 < 0) {
      throw new Exception_("can't find slash1");
    }
    int slash2 = filename.lastIndexOf("/", slash1 - 1);
    String name = filename.substring(slash2 + 1);
    String id = name.replaceAll(".", "_").replaceAll("/", "_");
    return getElementText(frameDocument, id);
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
}

void main() {
  // TODO - temporary, the if(false) here is so that tree-shaking doesn't 
  // delete frogPondMain.  (We want to be able to call frogPondMain at
  // the appropriate time after js setup.)
  if (false) {
    pondMain();
  }
}

void pondMain() {
  new Pond().run();
}

/** Slightly friendlier interface to SourceSpan */
class SpanHelper {
  static int startLine(SourceSpan span) {
    return span.file.getLine(span.start);
  }
  
  static int startCol(SourceSpan span) {
    return span.file.getColumn(span.file.getLine(span.start), span.start);
  }
  
  static int endLine(SourceSpan span) {
    return span.file.getLine(span.end);
  }
  
  static int endCol(SourceSpan span) {    
    return span.file.getColumn(span.file.getLine(span.end), span.end);
  }    
}

class PondSampleCode {

  static final String dart = '''
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

  static final String html = '''
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

