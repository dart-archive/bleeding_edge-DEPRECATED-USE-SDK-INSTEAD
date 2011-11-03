// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('lib/node/node.dart');
#import('file_system_node.dart');
#import('lang.dart');

void main() {
  // Get the home directory from our executable.
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));
  if (compile(homedir, process.argv, new NodeFileSystem())) {
    var code = world.getGeneratedCode();
    if (!options.compileOnly) {
      process.argv = [process.argv[0], process.argv[1]];
      process.argv.addAll(options.childArgs);
      vm.runInNewContext(code, createSandbox());
    }
  } else {
    process.exit(1);
  }
}
