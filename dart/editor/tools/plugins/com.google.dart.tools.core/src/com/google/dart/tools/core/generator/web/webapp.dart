import 'dart:html';

void main() {
  query("#sample_text_id")
    ..text = "Click me!"
    ..on.click.add(reverseText);
}

void reverseText(Event event) {
  var text = query("#sample_text_id").text;
  var buffer = new StringBuffer();
  for (int i = text.length - 1; i >= 0; i--) {
    buffer.add(text[i]);
  }
  query("#sample_text_id").text = buffer.toString();
}
