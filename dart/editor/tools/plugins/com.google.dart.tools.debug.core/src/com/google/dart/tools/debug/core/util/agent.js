// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// -- start Dart Editor debug agent --

var _d_origin = window.location.protocol + '//' + window.location.host;
var _d_remoteWindow = null;

// create an iframe used to send log messages back to the editor
var _d_remoteFrame = document.createElement('iframe');
_d_remoteFrame.style.display = 'none';
_d_remoteFrame.src = _d_origin + '/agent.html';
document.documentElement.appendChild(_d_remoteFrame);

_d_remoteFrame.onload = function () {
  _d_remoteWindow = _d_remoteFrame.contentWindow;
};

// replace the dartPrint function with our own definition
dartPrint = function(str) {
  var MAX = 200000;
  
  if (typeof window == "object" && typeof console == "object") {
    console.log(str);
  } else if (typeof print == "function") {
    print(str);
  }
  
  if (_d_remoteWindow) {
    if (str.length > MAX) {
      str = str.substring(0, MAX - 20) + '\n\nOUTPUT TRUNCATED';
      
      if (window.console) {
        console.log('Dart print() output has been truncated (' + MAX + ' chars max).');
      }
    }

    _d_remoteWindow.postMessage(JSON.stringify({ message: str }), _d_origin);
  }
};

//-- end Dart Editor debug agent --
