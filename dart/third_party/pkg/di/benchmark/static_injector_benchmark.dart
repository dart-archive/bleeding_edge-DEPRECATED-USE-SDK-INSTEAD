import 'package:di/static_injector.dart';

import 'injector_benchmark_common.dart';

main() {
  var typeFactories = {
    A: (f) => new A(f(B), f(C)),
    B: (f) => new B(f(D), f(E)),
    C: (f) => new C(),
    D: (f) => new D(),
    E: (f) => new E(),
  };

  new InjectorBenchmark('StaticInjectorBenchmark',
      (m) => new StaticInjector(modules: m, typeFactories: typeFactories)
  ).report();
}