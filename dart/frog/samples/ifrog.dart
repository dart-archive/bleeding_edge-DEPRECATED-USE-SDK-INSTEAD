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

String _getPrompt(Token incompleteToken) {
  if (incompleteToken == null) return ">>> ";
  switch (incompleteToken.kind) {
    case TokenKind.INCOMPLETE_MULTILINE_STRING_DQ: return '""" ';
    case TokenKind.INCOMPLETE_MULTILINE_STRING_SQ: return "''' ";
    default: return "... ";
  }
}

void main() {
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));
  Evaluator.initWorld(homedir, [], new NodeFileSystem());

  var eval = new Evaluator(new NodeJsEvaluator());
  var rl = readline.createInterface(process.stdin, process.stdout);
  var incompleteToken = null;
  var priorCommand = null;

  rl.setPrompt(">>> ");
  rl.on("line", (command) {
    if (priorCommand != null) command = priorCommand + "\n" + command;
    try {
      var result = eval.eval(command);
      if (result !== null) print(result);
      incompleteToken = null;
      priorCommand = null;
    } catch (CompilerException e) {
      // Do nothing, since a message was already printed
      incompleteToken = null;
      priorCommand = null;
    } catch (IncompleteSourceException e) {
      incompleteToken = e.token;
      priorCommand = command;
    } catch (var e, stack) {
      incompleteToken = null;
      priorCommand = null;
      if (stack != null) {
        print(stack);
      } else {
        print(e);
      }
    }

    rl.setPrompt(_getPrompt(incompleteToken));
    rl.prompt();
  });
  rl.on("close", () { process.exit(0); });
  rl.prompt();
}
