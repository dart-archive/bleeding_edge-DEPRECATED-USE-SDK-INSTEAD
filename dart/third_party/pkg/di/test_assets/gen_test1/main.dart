import 'dart:async';

import 'annotations.dart';
import 'common1.dart';

@lazyA
import 'a.dart' as a;

@lazyB
import 'b.dart' as b;

@lazyC
import 'c.dart' as c;

const lazyA = const DeferredLibrary('lib_a');
const lazyB = const DeferredLibrary('lib_b');
const lazyC = const DeferredLibrary('lib_c');

void main() {
  lazyA.load().then(onALoaded);
  lazyB.load().then(onBLoaded);
  lazyC.load().then(onCLoaded);
}

void onALoaded(_) {
  var serviceA = new a.ServiceA();
  serviceA.sayHi();
}

void onBLoaded(_) {
  var serviceB = new b.ServiceB();
  serviceB.sayHi();
}

void onCLoaded(_) {
  c.cStuff();
}

@InjectableTest()
class ServiceMain {
  sayHi() {
    print('Hi ServiceMain!');
  }
}
