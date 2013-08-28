// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A JS interop sample showing JSONP access to Twitter from Dart.

import 'dart:html';
import 'package:js/js.dart' as js;

void main() {
  // Create a JavaScript function called display that forwards to the Dart
  // function.
  js.context.display = new js.Callback.once(display);

  // Inject a JSONP request to Twitter invoking the JavaScript display
  // function.
  document.body.nodes.add(new ScriptElement()..src =
    "https://search.twitter.com/search.json?q=dartlang&rpp=20&callback=display");
}

// Convert URLs in the text to links.
String linkify(String text) {
  List words = text.split(' ');
  var buffer = new StringBuffer();
  for (var word in words) {
    if (!buffer.isEmpty) buffer.write(' ');
    if (word.startsWith('http://') || word.startsWith('https://')) {
      buffer.write('<a href="$word">$word</a>');
    } else {
      buffer.write(word);
    }
  }
  return buffer.toString();
}

// Display the JSON data on the web page.
// Note callbacks are automatically executed within a scope.
void display(var data) {
  // The data and results objects are proxies to JavaScript object.
  var results = data.results;
  int length = results.length;

  for (int i = 0; i < length; ++i) {
    var result = results[i];
    String user = result.from_user_name;
    String text = linkify(result.text);

    var div = new DivElement()
      ..innerHtml = '<div>From: $user</div><div>$text</div><p>';
    document.body.nodes.add(div);
  }
}
