import 'package:benchmark_harness/benchmark_harness.dart';
import 'package:di/di.dart';

import 'injector_benchmark_common.dart';

class ModuleBenchmark extends BenchmarkBase {
  var injectorFactory;

  ModuleBenchmark() : super('ModuleBenchmark');

  void run() {
    var m = new Module()
      ..type(A)
      ..type(B)
      ..type(C)
      ..type(D)
      ..type(E);
  }
}

main() {
  new ModuleBenchmark().report();
}