#import('dart:html');

void main() {
  showMessage('Welcome to Dart!');
}

void showMessage(String message) {
  var textElement = query('#text');

  textElement.text = message;
}
