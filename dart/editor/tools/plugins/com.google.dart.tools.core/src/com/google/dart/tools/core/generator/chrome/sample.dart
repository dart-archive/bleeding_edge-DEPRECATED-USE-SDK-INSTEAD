
import 'dart:html';

import 'package:chrome_gen/chrome_app.dart' as chrome;

int boundsChange = 100;

/**
 * For non-trivial uses of the Chrome Apps API, please see the
 * [chrome_gen](http://pub.dartlang.org/packages/chrome_gen).
 * 
 * * http://developer.chrome.com/apps/api_index.html
 */
void main() {
  querySelector("#text_id").onClick.listen(resizeWindow);
}

void resizeWindow(MouseEvent event) {
  chrome.Bounds bounds = chrome.app.window.current().getBounds();

  bounds.width += boundsChange;
  bounds.left -= boundsChange ~/ 2;

  chrome.app.window.current().setBounds(bounds);

  boundsChange *= -1;
}
