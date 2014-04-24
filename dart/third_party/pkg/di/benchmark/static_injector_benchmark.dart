import 'package:benchmark_harness/benchmark_harness.dart';
import 'package:di/static_injector.dart';

import 'injector_benchmark_common.dart';

main() {
  var typeFactories = new Map();
  typeFactories[A] = (f) => new A(f(B), f(C));
  typeFactories[B] = (f) => new B(f(D), f(E));
  typeFactories[C] = (f) => new C();
  typeFactories[D] = (f) => new D();
  typeFactories[E] = (f) => new E();

  new InjectorBenchmark('StaticInjectorBenchmark',
      (m) => new StaticInjector(modules: m, typeFactories: typeFactories)
  ).report();
}