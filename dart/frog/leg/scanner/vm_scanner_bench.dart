// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('vm_scanner_bench');
#import('scannerlib.dart');
#import('scanner_implementation.dart');
#import('scanner_bench.dart');
#source('byte_strings.dart');
#source('byte_array_scanner.dart');

class VmScannerBench extends ScannerBench {
  int getBytes(String filename, void callback(List<int> bytes)) {
    File file = new File(filename);
    file.openSync();
    int size = file.lengthSync();
    List<int> bytes = new List<int>(size + 1);
    file.readListSync(bytes, 0, size);
    bytes[size] = -1;
    file.closeSync();
    callback(bytes);
    return size;
  }

  void checkExistence(String filename) {
    File file = new File(filename);
    if (!file.existsSync()) {
      print("no such file: ${filename}");
    }
  }

  Scanner makeScanner(bytes) => new ByteArrayScanner(bytes);
}

main() {
  new VmScannerBench().main(argv);
}
