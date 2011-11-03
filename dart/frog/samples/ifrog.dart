// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.


#import('../lib/node/node.dart');
#import('../file_system_node.dart');
#import('../lang.dart');
#import('../evaluator.dart');

class NodeJsEvaluator implements Evaluator {
  var _sandbox;

  NodeJsEvaluator(): this._sandbox = createSandbox();

  var eval(String js) => vm.runInNewContext(js, _sandbox);
}

void main() {
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));
  Evaluator.initWorld(homedir, [], new NodeFileSystem());

  var eval = new Evaluator(new NodeJsEvaluator());
  var rl = readline.createInterface(process.stdin, process.stdout);
  rl.setPrompt(">>> ");
  rl.on("line", (command) {
    try {
      var result = eval.eval(command);
      if (result != null) print(result);
    } catch (CompilerException e) {
      // Do nothing, since a message was already printed
    } catch (var e, stack) {
      if (stack != null) {
        print(stack);
      } else {
        print(e);
      }
    }

    rl.prompt();
  });
  rl.on("close", () { process.exit(0); });
  rl.prompt();
}
