import 'dart:chrome';
import 'dart:html';

int boundsChange = 100;

void main() {
  query("#sample_text_id")
    ..text = "Click me!"
    ..onClick.listen(resizeWindow);
}

void resizeWindow(MouseEvent event) {
  AppWindowAppWindow appWindow = chrome.app.window.current();

  AppWindowBounds bounds = appWindow.getBounds();
  bounds.width += boundsChange;
  bounds.left -= boundsChange ~/ 2;
  appWindow.setBounds(bounds);

  boundsChange *= -1;
}
