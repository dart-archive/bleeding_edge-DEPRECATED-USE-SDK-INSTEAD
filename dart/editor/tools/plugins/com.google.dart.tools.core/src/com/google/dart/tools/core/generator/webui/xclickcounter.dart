import 'package:web_ui/web_ui.dart';

class CounterComponent extends WebComponent {
  int count = 0;

  void increment() {
    count++;
  }
}
