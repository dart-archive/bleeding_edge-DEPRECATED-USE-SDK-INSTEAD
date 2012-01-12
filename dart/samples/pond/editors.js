// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(sigmund): move this into its own isolate.
function jsReady() {
  var dartEditor = CodeMirror(document.getElementById("dartEditor"), {
    mode:  "dart",
    matchBrackets: true,
    tabSize: 2,
    lineNumbers: true,
    gutter: false
  });
  var htmlEditor = CodeMirror(document.getElementById("htmlEditor"), {
    mode: "htmlmixed",
    tabSize: 2,
    lineNumbers: true,
    gutter: true
  });
  var jsEditor = CodeMirror(document.getElementById("jsEditor"), {
    mode: "javascript",
    tabSize: 2,
    lineNumbers: true,
    gutter: true
  });
  var warningEditor = CodeMirror(document.getElementById("warningEditor"), {
    mode: "diff",
    tabSize: 2,
    lineNumbers: true,
    gutter: true
  });

  document.getElementById("testButton").addEventListener("click", function() {
  }, false);

  function toDataURL(text) {
    var preamble = "data:text/html;charset=utf-8,";
    var escaped = encodeURIComponent(text);
    return preamble + escaped;
  }

  function getSourceText(id) {
    var s = document.getElementById(id).text;
    s = s.replace(/^\s+|\s+$/g,"");
    return s + "\n";
  }
  window.getEditorText = function(id) {
    if (id == "dartEditor") {
      return dartEditor.getValue();
    }
  }
  window.markText = function(startLine, startCol, endLine, endCol, className) {
    return dartEditor.markText({line:startLine, ch:startCol}, {line:endLine, ch:endCol}, className);
  }

  window.setEditorText = function(id, text) {
    var editor
    if (id == "jsEditor") {
      editor = jsEditor;
    } else if (id == "dartEditor") {
      editor = dartEditor;
    } else if (id == "htmlEditor") {
      editor = htmlEditor;
    } else if (id == "warningEditor") {
      editor = warningEditor;
    } else {
      throw "can't find editor for id " + id;
    }
    editor.setValue(text);
    if (editor === jsEditor) {
      var html = htmlEditor.getValue();
      var start = html.indexOf("{{DART}}");
      html = html.substr(0, start) + text + html.substr(start + "{{DART}}".length);
      html = html.replace("application/dart", "text/javascript");
      document.getElementById("resultFrame").setAttribute("src", toDataURL(html));
    } else if (id == "htmlEditor") {
      htmlEditor.setValue(text);
    }
  }
  pondMain();
  dartEditor.focus();
}

if (document.readyState == "loaded") {
  jsReady();
} else {
  window.addEventListener("load", function(e) { jsReady(); }, false);
}
