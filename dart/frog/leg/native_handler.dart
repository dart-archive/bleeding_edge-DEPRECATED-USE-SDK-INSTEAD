// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('native');
#import('../../lib/uri/uri.dart');
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
      if (classElement.isNative()) {
        compiler.registerInstantiatedClass(classElement);
        // Also parse the node to know all its methods because
        // otherwise it will only be parsed if there is a call to
        // one of its constructor.
        element.parseNode(compiler);
        // Resolve to setup the inheritance.
        element.resolve(compiler);
      }
    }
  }
}

void checkNativeSupport(Compiler compiler,
                        LibraryElement library,
                        Uri uri) {
  String libraryName = uri.toString();
  if (library.script.name.contains('dart/frog/tests/native/src')
      || libraryName == 'dart:dom'
      || libraryName == 'dart:html') {
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

Token handleNativeFunctionBody(ElementListener listener, Token token) {
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

SourceString checkForNativeClass(ElementListener listener) {
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
  // Register NoSuchMethodException and captureStackTrace in the compiler
  // because the dynamic dispatch for native classes may use them.
  Compiler compiler = builder.compiler;
  ClassElement cls = compiler.coreLibrary.find(
      Compiler.NO_SUCH_METHOD_EXCEPTION);
  cls.resolve(compiler);
  compiler.addToWorklist(cls.lookupConstructor(cls.name));
  compiler.registerStaticUse(
      compiler.findHelper(new SourceString('captureStackTrace')));

  if (node.arguments.isEmpty()) {
    List<String> arguments = <String>[];
    List<HInstruction> inputs = <HInstruction>[];
    FunctionElement element = builder.work.element;
    FunctionParameters parameters = element.computeParameters(builder.compiler);
    int i = 0;
    String receiver = '';
    if (element.isInstanceMember()) {
      receiver = '\$$i.';
      i++;
      inputs.add(builder.localsHandler.readThis());
    }
    parameters.forEachParameter((Element parameter) {
      arguments.add('\$$i');
      inputs.add(builder.localsHandler.readLocal(parameter));
      i++;
    });
    String foreignParameters = Strings.join(arguments, ',');

    String dartMethodName;
    String nativeMethodName = element.name.slowToString();
    String nativeMethodCall;

    if (element.kind == ElementKind.FUNCTION) {
      dartMethodName = builder.compiler.namer.instanceMethodName(
          element.name, parameters.parameterCount);
      nativeMethodCall = '$receiver$nativeMethodName($foreignParameters)';
    } else if (element.kind == ElementKind.GETTER) {
      dartMethodName = builder.compiler.namer.getterName(element.name);
      nativeMethodCall = '$receiver$nativeMethodName';
    } else if (element.kind == ElementKind.SETTER) {
      dartMethodName = builder.compiler.namer.setterName(element.name);
      nativeMethodCall = '$receiver$nativeMethodName = $foreignParameters';
    } else {
      builder.compiler.internalError('unexpected kind: "${element.kind}"',
                                     element: element);
    }

    HInstruction thenInstruction;
    void visitThen() {
      SourceString jsCode = new SourceString(nativeMethodCall);
      thenInstruction =
          new HForeign(jsCode, const SourceString('Object'), inputs);
      builder.add(thenInstruction);
    }

    bool isNativeLiteral = false;
    if (element.enclosingElement.kind == ElementKind.CLASS) {
      ClassElement classElement = element.enclosingElement;
      String nativeName = classElement.nativeName.slowToString();
      isNativeLiteral =
          builder.compiler.emitter.nativeEmitter.isNativeLiteral(nativeName);
    }
    if (!element.isInstanceMember() || isNativeLiteral) {
      // If the method is a non-instance method, or is a member of a native
      // class that represents a literal, we just generate a direct
      // call to the native method.
      visitThen();
      builder.stack.add(thenInstruction);
    } else {
      // If the method is an instance method, we generate the following code:
      // function(params) {
      //   return Object.getPrototypeOf(this).hasOwnProperty(dartMethodName))
      //      ? this.methodName(params)
      //      : Object.prototype.methodName.call(this, params);
      // }
      //
      // The property check at the beginning is to make sure we won't
      // call the method from the super class, in case the prototype of
      // 'this' does not have the method yet.
      //
      // TODO(ngeoffray): We can avoid this if we know the class of this
      // method does not have subclasses.
      HInstruction elseInstruction;
      void visitElse() {
        SourceString jsCode =
            new SourceString('Object.prototype.$dartMethodName');
        HInstruction instruction =
            new HForeign(jsCode, const SourceString('Object'), []);
        builder.add(instruction);
        List<HInstruction> elseInputs = new List<HInstruction>.from(inputs);
        elseInputs.add(instruction);
        String params = arguments.isEmpty() ? '' : ', $foreignParameters';
        jsCode = new SourceString('\$${i}.call(\$0$params)');
        elseInstruction =
            new HForeign(jsCode, const SourceString('Object'), elseInputs);
        builder.add(elseInstruction);
      }

      HLiteral literal = builder.graph.addNewLiteralString(
          new DartString.literal('$dartMethodName'));
      SourceString jsCode = new SourceString(
          'Object.getPrototypeOf(\$0).hasOwnProperty(\$1)');
      builder.push(new HForeign(
          jsCode, const SourceString('Object'),
          <HInstruction>[builder.localsHandler.readThis(), literal]));

      builder.handleIf(visitThen, visitElse);

      HPhi phi = new HPhi.manyInputs(
          null, <HInstruction>[thenInstruction, elseInstruction]);
      builder.current.addPhi(phi);
      builder.stack.add(phi);
    }

  } else if (!node.arguments.tail.isEmpty()) {
    builder.compiler.cancel('More than one argument to native');
  } else {
    LiteralString jsCode = node.arguments.head;
    int start = jsCode.value.slowToString()[0] === '@' ? 1 : 0;
    builder.push(new HForeign(builder.unquote(jsCode, start),
                              const SourceString('Object'),
                              <HInstruction>[]));
  }
}
