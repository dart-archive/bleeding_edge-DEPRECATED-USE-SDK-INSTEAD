import 'dart:html';

Element sampleText;

void main() {
  sampleText = querySelector("#sample_text_id");
  sampleText.onClick.listen(reverseText);
}

void reverseText(MouseEvent event) {
  var text = sampleText.text;
  sampleText.text = '${text.substring(1)}${text.substring(0, 1)}';
}
