library auto_injector_test;

import 'package:di/auto_injector.dart' as auto;
import 'main.dart';


void main() {
  createInjectorSpec('Injector',
      (modules, [name]) => auto.defaultInjector(modules: modules, name: name));
}
