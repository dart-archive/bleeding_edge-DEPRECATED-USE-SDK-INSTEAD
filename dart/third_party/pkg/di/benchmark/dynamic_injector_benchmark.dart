import 'package:benchmark_harness/benchmark_harness.dart';
import 'package:di/dynamic_injector.dart';

import 'injector_benchmark_common.dart';


main() {
  new InjectorBenchmark('DynamicInjectorBenchmark',
      (m) => new DynamicInjector(modules: m)).report();
}