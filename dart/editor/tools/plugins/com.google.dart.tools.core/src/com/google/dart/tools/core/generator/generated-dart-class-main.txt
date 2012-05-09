#import('dart:html');

void main() {
  show('Hello, World!');
}

void show(String message) {
  document.query('#status').innerHTML = message;
}
