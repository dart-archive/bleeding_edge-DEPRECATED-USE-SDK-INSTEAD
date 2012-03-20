// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

var ERROR_KIND = 1;
var WARNING_KIND = 2;
var ERROR_CLASSNAME = "compile_error";
var WARNING_CLASSNAME = "compile_warning";

var editors = {};
var markers = [];

function newEditor(id, type, listener) {
  editors[id] = CodeMirror(document.getElementById(id), {
    mode: type,
    tabSize: 2,
    lineNumbers: true,
    gutter: true,
    onChange: listener
  });
}

function changeListener(id) {
  return function(editor, textChanges) {
    window.postMessage(['js-to-dart', 'update', id], '*');
  }
}

function newMark(editorId, startLine, startCol, endLine, endCol, kind) {
  var className = "";
  if (kind == ERROR_KIND) {
    className = ERROR_CLASSNAME;
  } else if (kind == WARNING_KIND) {
    className = WARNING_CLASSNAME;
  }
  var marker = editors[editorId].markText(
      {line: startLine, ch: startCol}, {line: endLine, ch: endCol}, className);
  var markerId = markers.length;
  markers.push(marker);
  return markerId;
}

function messageDispatcher(envelope) {
  if (envelope[0] != 'dart-to-js') return;
  var returnId = envelope[1];
  var message = envelope[2];
  var command = message[0];
  var args = message[1];
  var reply = null;
  switch (command) {
    case "newEditor":
      newEditor(args[0], args[1], (args[2] ? changeListener(args[0]): null));
      break;
    case "getText":
      reply = editors[args[0]].getValue();
      break;
    case "setText":
      editors[args[0]].setValue(args[1]);
      break;
    case "refresh":
      editors[args[0]].showLine(0);
      break;
    case "mark":
      var reply = newMark(
          args[0], // editor
          args[1], args[2], // start line & column
          args[3], args[4], // end line & column
          args[5]); // kind
      break;
    case "clearMark":
      markers[args[0]].clear();
      break;
  }
  window.postMessage(['js-to-dart-reply', returnId, reply], '*');
}

window.addEventListener('message',
    function (e) { messageDispatcher(e.data); }, false);
