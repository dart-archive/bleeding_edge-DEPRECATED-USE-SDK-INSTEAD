// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('node_scanner_bench');
#import('../../lib/node/node.dart');
#import('scannerlib.dart');
#import('scanner_implementation.dart');
#import('scanner_bench.dart');
#source('byte_strings.dart');
#source('byte_array_scanner.dart');

class NodeScannerBench extends ScannerBench {
  int getBytes(String filename, void callback(bytes)) {
    // This actually returns a buffer, not a String.
    var s = fs.readFileSync(filename, null);
    callback(s);
    return s.length;
  }

  Scanner makeScanner(bytes) => new ByteArrayScanner(bytes);

  void checkExistence(String filename) {
    if (!path.existsSync(filename)) {
      throw "no such file: ${filename}";
    }
  }
}

main() {
  new NodeScannerBench().main(argv);
}
