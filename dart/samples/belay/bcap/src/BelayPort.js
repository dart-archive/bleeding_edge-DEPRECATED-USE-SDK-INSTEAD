// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// JavaScript implementation of BelayPort.

function native_BelayPort__initialize(ready) {
  var BELAY_PORT_URL = 'http://localhost:9000/belay-port.js';

  window.belay = {
    portReady: function() { $dartcall(ready, []); },
    DEBUG: false
  };
   
  window.addEventListener('load', function() {
    var script = window.document.createElement('script');
    script.setAttribute('src', BELAY_PORT_URL);
    window.document.body.appendChild(script);
  });
}

function native_BelayPort__postMessage(str) {
  var json = JSON.parse(str);
  window.belay.port.postMessage(json);
}

function native_BelayPort__setOnMessage(callback) {
  window.belay.port.onmessage = function(evt) {
    var str = JSON.stringify(evt.data);
    $dartcall(callback, [str]);
  }
}

///////////////////////////////////////////////////////////////////////////////
// Workaround to support HTML5 drag and drop

function native_BelayUtil_belayEncode(data) {
  return window.btoa(window.unescape(window.encodeURIComponent(data)))
}

function native_BelayUtil__getDataTransfer(raw) {
  try {
    return __dom_wrap(raw.$dom.dataTransfer);
  } catch (e) {
    throw __dom_wrap_exception(e);
  }
}

