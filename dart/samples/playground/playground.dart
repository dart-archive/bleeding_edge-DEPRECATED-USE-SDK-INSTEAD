// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:html';

void run() {
  // Get user script.
  final textarea = document.query('#code');
  final text = textarea.value;

  // Clear previous output.
  final output = document.query('#output');
  output.innerHtml = '';

  // Run user script in new iframe.
  final iframe = new IFrameElement();
  iframe.height = '200';
  iframe.width = '100%';
  iframe.src = '''data:text/html,
<html>
  <body>
    <script type="application/dart">
${text.replaceAll('\n', '%0A')}
    </script>
    <script>window.navigator.webkitStartDart();</script>
  </body>
</html>
''';
  output.nodes.add(iframe);
}

void main() {
  final button = document.query('#button');
  button.on.click.add((e) => run(), false);
}
