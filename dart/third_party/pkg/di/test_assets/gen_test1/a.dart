library lib_a;

import 'annotations.dart';
import 'a2.dart';
import 'common1.dart';

@InjectableTest()
class ServiceA {
  sayHi() {
    print('Hi ServiceA!');
  }
}