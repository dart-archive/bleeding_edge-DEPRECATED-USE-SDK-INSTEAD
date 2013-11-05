library test_console;

import 'package:unittest/unittest.dart';
import 'package:unittest/vm_config.dart';
import 'ppw/_ppw_runner.dart';

main() {
  testCore(new VMConfiguration());
}

void testCore(Configuration config) {
  unittestConfiguration = config;
  groupSep = ' - ';

  runppwTests();
}
