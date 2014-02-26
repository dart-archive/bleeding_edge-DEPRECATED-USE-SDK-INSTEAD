import 'dart:async';

import 'annotations.dart';
import 'common1.dart';

@lazyA
import 'a.dart' as a;

@lazyB
import 'b.dart' as b;

const lazyA = const DeferredLibrary('lib_a');
const lazyB = const DeferredLibrary('lib_b');

void main() {
  lazyA.load().then(onALoaded);
  lazyB.load().then(onBLoaded);
}

void onALoaded(_) {
  var serviceA = new a.ServiceA();
  serviceA.sayHi();
}

void onBLoaded(_) {
  var serviceB = new b.ServiceB();
  serviceB.sayHi();
}

@Injectable()
class ServiceMain {
  sayHi() {
    print('Hi ServiceMain!');
  }
}
