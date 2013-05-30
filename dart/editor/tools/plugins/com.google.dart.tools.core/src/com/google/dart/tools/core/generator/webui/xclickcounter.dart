import 'package:web_ui/web_ui.dart';

class CounterComponent extends WebComponent {
  @observable
  int count = 0;

  void increment() {
    count++;
  }
}
