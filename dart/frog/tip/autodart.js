// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The primary goal of this code is to detect pages with dart source tags
 * in them and on the file recompile them into JS like the existing
 * htmlconverter.py script.  This would enable a standard chrome browser
 * to act as if Dart was natively installed.
 *
 * TODO(jimhug): Currently this just executes raw .dart files as proof of
 *   concept - and as a very useful local dev tool.
 */

var name = window.location.pathname;
// TODO(jimhug): Port this code to dart and use fancy things like endsWith.
if (name.length > 5 && name.substring(name.length-5) == '.dart') {
  console.log('trying to load dart');
  // Trying to load a .dart file, so run it!
  chrome.extension.sendRequest({
    code: document.body.innerText
  }, function(response) {
    var script = document.createElement('script');
    script.type= 'text/javascript';
    script.innerHTML = response.js;
    document.body.appendChild(script);
  });
}