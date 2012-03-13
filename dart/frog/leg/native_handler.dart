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
                          Collection<LibraryElement> libraries) {
  for (LibraryElement library in libraries) {
    processNativeClassesInLibrary(compiler, library);
  }
}

void processNativeClassesInLibrary(Compiler compiler,
                                   LibraryElement library) {
  for (Link<Element> link = library.topLevelElements;
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
        element.ensureResolved(compiler);
        // Add the information that this class is a direct subclass of
        // its superclass. The code emitter and the ssa builder use that
        // information.
        NativeEmitter emitter = compiler.emitter.nativeEmitter;
        List<Element> subclasses = emitter.subclasses.putIfAbsent(
            classElement.superclass,
            () => <ClassElement>[]);
        subclasses.add(classElement);
      }
    }
  }
}

void maybeEnableNative(Compiler compiler,
                       LibraryElement library,
                       Uri uri) {
  String libraryName = uri.toString();
  if (library.script.name.contains('dart/frog/tests/native/src')
      || libraryName == 'dart:dom'
      || libraryName == 'dart:isolate'
      || libraryName == 'dart:html') {
    library.define(new ForeignElement(
        const SourceString('native'), library), compiler);
    library.canUseNative = true;
  }

  // Additionaly, if this is a test, we allow access to foreign functions.
  if (library.script.name.contains('dart/frog/tests/native/src')) {
    compiler.addForeignFunctions(library);
  }
}

void checkAllowedLibrary(ElementListener listener, Token token) {
  LibraryElement currentLibrary = listener.compilationUnitElement.getLibrary();
  if (!currentLibrary.canUseNative) {
    listener.recoverableError("Unexpected token", token: token);
  }
}

Token handleNativeBlockToSkip(Listener listener, Token token) {
  checkAllowedLibrary(listener, token);
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
  checkAllowedLibrary(listener, token);
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
  checkAllowedLibrary(listener, token);
  token = token.next;
  if (token.kind !== STRING_TOKEN) {
    listener.unexpected(token);
  } else {
    token = token.next;
  }
  return token;
}

Token handleNativeFunctionBody(ElementListener listener, Token token) {
  checkAllowedLibrary(listener, token);
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

bool isOverriddenMethod(FunctionElement element,
                        ClassElement cls,
                        NativeEmitter nativeEmitter) {
  List<ClassElement> subclasses = nativeEmitter.subclasses[cls];
  if (subclasses == null) return false;
  for (ClassElement subclass in subclasses) {
    if (subclass.lookupLocalMember(element.name) != null) return true;
    if (isOverriddenMethod(element, subclass, nativeEmitter)) return true;
  }
  return false;
}

void handleSsaNative(SsaBuilder builder, Send node) {
  // Register NoSuchMethodException and captureStackTrace in the compiler
  // because the dynamic dispatch for native classes may use them.
  Compiler compiler = builder.compiler;
  ClassElement cls = compiler.coreLibrary.find(
      Compiler.NO_SUCH_METHOD_EXCEPTION);
  cls.ensureResolved(compiler);
  compiler.addToWorkList(cls.lookupConstructor(cls.name));
  compiler.registerStaticUse(
      compiler.findHelper(new SourceString('captureStackTrace')));

  FunctionElement element = builder.work.element;
  NativeEmitter nativeEmitter = compiler.emitter.nativeEmitter;
  // If what we're compiling is a getter named 'typeName' and the native
  // class is named 'DOMType', we generate a call to the typeNameOf
  // function attached on the isolate.
  // The DOM classes assume that their 'typeName' property, which is
  // not a JS property on the DOM types, returns the type name.
  if (element.name == const SourceString('typeName')
      && element.isGetter()
      && nativeEmitter.toNativeName(element.enclosingElement) == 'DOMType') {
    DartString jsCode = new DartString.literal(
        '${nativeEmitter.typeNameOfName}(\$0)');
    List<HInstruction> inputs =
        <HInstruction>[builder.localsHandler.readThis()];
    builder.push(new HForeign(
        jsCode, const LiteralDartString('String'), inputs));
    return;
  }

  if (node.arguments.isEmpty()) {
    List<String> arguments = <String>[];
    List<HInstruction> inputs = <HInstruction>[];
    FunctionParameters parameters = element.computeParameters(builder.compiler);
    int i = 0;
    String receiver = '';
    if (element.isInstanceMember()) {
      receiver = '\$$i.';
      i++;
      inputs.add(builder.localsHandler.readThis());
    }
    Compiler compiler = builder.compiler;
    parameters.forEachParameter((Element parameter) {
      Type type = parameter.computeType(compiler);
      HInstruction input = builder.localsHandler.readLocal(parameter);
      if (type is FunctionType) {
        // TODO(ngeoffray): by better analyzing the function type and
        // its formal parameters, we could just pass, eg closure.$call$0.
        builder.push(new HStatic(builder.interceptors.getClosureConverter()));
        List<HInstruction> callInputs = <HInstruction>[builder.pop(), input];
        input = new HInvokeStatic(Selector.INVOCATION_1, callInputs);
        builder.add(input);
      }
      inputs.add(input);
      arguments.add('\$$i');
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
      DartString jsCode = new DartString.literal(nativeMethodCall);
      thenInstruction =
          new HForeign(jsCode, const LiteralDartString('Object'), inputs);
      builder.add(thenInstruction);
    }

    bool isNativeLiteral = false;
    bool isOverridden = false;
    NativeEmitter nativeEmitter = builder.compiler.emitter.nativeEmitter;
    if (element.enclosingElement.kind == ElementKind.CLASS) {
      ClassElement classElement = element.enclosingElement;
      String nativeName = classElement.nativeName.slowToString();
      isNativeLiteral = nativeEmitter.isNativeLiteral(nativeName);
      isOverridden = isOverriddenMethod(element, classElement, nativeEmitter);
    }
    if (!element.isInstanceMember() || isNativeLiteral || !isOverridden) {
      // We generate a direct call to the native method.
      visitThen();
      builder.stack.add(thenInstruction);
    } else {
      // Record that this method is overridden. In case of optional
      // arguments, the emitter will generate stubs to handle them,
      // and needs to know if the method is overridden.
      nativeEmitter.overriddenMethods.add(element);

      // If the method is an instance method that is overridden, we
      // generate the following code:
      // function(params) {
      //   return Object.getPrototypeOf(this).hasOwnProperty(dartMethodName))
      //      ? this.methodName(params)
      //      : Object.prototype.methodName.call(this, params);
      // }
      //
      // The property check at the beginning is to make sure we won't
      // call the method from the super class, in case the prototype of
      // 'this' does not have the method yet.
      HInstruction elseInstruction;
      void visitElse() {
        DartString jsCode =
            new DartString.literal('Object.prototype.$dartMethodName');
        HInstruction instruction =
            new HForeign(jsCode, const LiteralDartString('Object'), []);
        builder.add(instruction);
        List<HInstruction> elseInputs = new List<HInstruction>.from(inputs);
        elseInputs.add(instruction);
        String params = arguments.isEmpty() ? '' : ', $foreignParameters';
        jsCode = new DartString.literal('\$${i}.call(\$0$params)');
        elseInstruction =
            new HForeign(jsCode, const LiteralDartString('Object'), elseInputs);
        builder.add(elseInstruction);
      }

      HConstant constant = builder.graph.addConstantString(
          new DartString.literal('$dartMethodName'));
      DartString jsCode = new DartString.literal(
          'Object.getPrototypeOf(\$0).hasOwnProperty(\$1)');
      builder.push(new HForeign(
          jsCode, const LiteralDartString('Object'),
          <HInstruction>[builder.localsHandler.readThis(), constant]));

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
    builder.push(new HForeign(jsCode.dartString,
                              const LiteralDartString('Object'),
                              <HInstruction>[]));
  }
}
