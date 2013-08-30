import 'package:polymer/polymer.dart';

@CustomTag('click-counter')
class ClickCounter extends PolymerElement with ObservableMixin {
  @observable int count = 0;

  void increment() {
    count++;
  }
}

