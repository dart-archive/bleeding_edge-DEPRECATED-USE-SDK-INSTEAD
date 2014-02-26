library di.injector_benchmark_common;

import 'package:benchmark_harness/benchmark_harness.dart';
import 'package:di/di.dart';

class InjectorBenchmark extends BenchmarkBase {
  var injectorFactory;
  var module;

  InjectorBenchmark(name, this.injectorFactory) : super(name);

  void run() {
    Injector injector = injectorFactory([module]);
    injector.get(A);
    injector.get(B);

    var childInjector = injector.createChild([module]);
    childInjector.get(A);
    childInjector.get(B);
  }

  setup() {
    module = new Module()
      ..type(A)
      ..type(B)
      ..type(C)
      ..type(D)
      ..type(E);
  }
}

class A {
  A(B b, C c);
}

class B {
  B(D b, E c);
}

class C {
}

class D {
}

class E {
}
