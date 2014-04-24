library lib_b2;

import 'annotations.dart';
import 'common1.dart';

@InjectableTest()
class ServiceB2 {
  sayHi() {
    print('Hi ServiceB2!');
  }
}