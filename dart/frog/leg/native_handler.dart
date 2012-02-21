// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('native');
#import('leg.dart');
#import('elements/elements.dart');
#import('scanner/scannerlib.dart');
#import('ssa/ssa.dart');
#import('tree/tree.dart');
#import('util/util.dart');

void processNativeClasses(Compiler compiler,
                          CompilationUnitElement compilationUnit) {
  for (Link<Element> link = compilationUnit.topLevelElements;
       !link.isEmpty(); link = link.tail) {
    Element element = link.head;
    if (element.kind == ElementKind.CLASS) {
      ClassElement classElement = element;
      if (element.isNative()) {
        compiler.registerInstantiatedClass(classElement);
        // Also parse the node to know all its methods because
        // otherwise it will only be parsed if there is a call to
        // one of its constructor.
        element.parseNode(compiler);
      }
    }
  }
}

void checkNativeSupport(Compiler compiler, LibraryElement library) {
  if (library.script.name.contains('dart/frog/tests/native/src')) {
    library.define(new ForeignElement(
        const SourceString('native'), library), compiler);
  }
}

Token handleNativeBlockToSkip(Listener listener, Token token) {
 token = token.next;
  if (token.kind === STRING_TOKEN) {
    token = token.next;
  }
  if (token.stringValue === '{') {
    BeginGroupToken beginGroupToken = token;
    token = beginGroupToken.endGroup;
  }
  return token;
}

Token handleNativeClassBodyToSkip(Listener listener, Token token) {
  listener.handleIdentifier(token);
  token = token.next;
  if (token.kind !== STRING_TOKEN) {
    return listener.unexpected(token);
  }
  token = token.next;
  if (token.stringValue !== '{') {
    return listener.unexpected(token);
  }
  BeginGroupToken beginGroupToken = token;
  token = beginGroupToken.endGroup;
  return token;
}

Token handleNativeClassBody(Listener listener, Token token) {
  token = token.next;
  if (token.kind !== STRING_TOKEN) {
    listener.unexpected(token);
  } else {
    token = token.next;
  }
  return token;
}

Token handleNativeFunctionBody(Listener listener, Token token) {
  Token begin = token;
  listener.beginExpressionStatement(token);
  listener.handleIdentifier(token);
  token = token.next;
  if (token.kind === STRING_TOKEN) {
    listener.beginLiteralString(token);
    listener.endLiteralString(0);
    listener.pushNode(new NodeList.singleton(listener.popNode()));
    listener.endSend(token);
    token = token.next;
    listener.endExpressionStatement(token);
  } else {
    listener.pushNode(new NodeList.empty());
    listener.endSend(token);
    listener.endReturnStatement(true, begin, token);
  }
  listener.endFunctionBody(1, begin, token);
  // TODO(ngeoffray): expect a ';'.
  return token.next;
}

SourceString checkForNativeClass(Listener listener) {
  SourceString nativeName;
  Node node = listener.nodes.head;
  if (node != null
      && node.asIdentifier() != null
      && node.asIdentifier().source.stringValue == 'native') {
    nativeName = node.asIdentifier().token.next.value;
    listener.popNode();
  }
  return nativeName;
}

void handleSsaNative(SsaBuilder builder, Send node) {
  if (node.arguments.isEmpty()) {
    List<String> arguments = <String>[];
    List<HInstruction> inputs = <HInstruction>[];
    Element element = builder.work.element;
    FunctionParameters parameters = element.computeParameters(builder.compiler);
    int i = 0;
    String receiver = '';
    if (element.isInstanceMember()) {
      receiver = '\$$i';
      i++;
      inputs.add(builder.localsHandler.readThis());
    }
    parameters.forEachParameter((Element element) {
      arguments.add('\$$i');
      inputs.add(builder.localsHandler.readLocal(element));
      i++;
    });
    String foreignParameters = Strings.join(arguments, ',');
    SourceString jsCode = new SourceString(
        '$receiver${element.name}($foreignParameters)');
    builder.push(new HForeign(jsCode, const SourceString('Object'), inputs));
  } else if (!node.arguments.tail.isEmpty()) {
    builder.compiler.cancel('More than one argument to native');
  } else {
    LiteralString jsCode = node.arguments.head;
    builder.push(new HForeign(builder.unquote(jsCode, 0),
                              const SourceString('Object'),
                              <HInstruction>[]));
  }
}
