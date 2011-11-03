// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('node_scanner_bench');
#import('../../lib/node/node.dart');
#import('../scanner.dart');
#import('../scanner_implementation.dart');
#import('scanner_bench.dart');

class NodeScannerBench extends ScannerBench {
  int getBytes(String filename, void callback(bytes)) {
    // TODO(ahe): Pass in "null" to get a buffer instead.
    String s = fs.readFileSync(filename, 'UTF8');
    callback(s);
    return s.length;
  }

  Scanner makeScanner(bytes) => new StringScanner(bytes);

  void checkExistence(String filename) {
    if (!path.existsSync(filename)) {
      throw "no such file: ${filename}";
    }
  }
}

main() {
  new NodeScannerBench().main(argv);
}
