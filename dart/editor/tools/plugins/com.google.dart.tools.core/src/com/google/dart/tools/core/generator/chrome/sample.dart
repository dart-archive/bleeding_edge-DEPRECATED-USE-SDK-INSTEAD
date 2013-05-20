import 'dart:html';

import 'package:js/js.dart' as js;

int boundsChange = 100;

/**
 * For non-trivial uses of the Chrome apps API, please see the 'chrome'
 * package.
 * 
 * http://pub.dartlang.org/packages/chrome
 * http://developer.chrome.com/apps/api_index.html
 */
void main() {
  query("#sample_text_id")
    ..text = "Click me!"
    ..onClick.listen(resizeWindow);
}

void resizeWindow(MouseEvent event) {
  var appWindow = js.context.chrome.app.window.current();
  var bounds = appWindow.getBounds();
  
  bounds.width += boundsChange;
  bounds.left -= boundsChange ~/ 2;
  
  appWindow.setBounds(bounds);
  
  boundsChange *= -1;
}
