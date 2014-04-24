library lib_a2;

import 'annotations.dart';
import 'common2.dart';

@InjectableTest()
class ServiceA2 {
  sayHi() {
    print('Hi ServiceA2!');
  }
}