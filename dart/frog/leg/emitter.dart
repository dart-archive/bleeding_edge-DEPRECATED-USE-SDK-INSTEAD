// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Generates the code for all used classes in the program. Static fields (even
 * in classes) are ignored, since they can be treated as non-class elements.
 *
 * The code for the containing (used) methods must exist in the [:universe:].
 */
class CodeEmitterTask extends CompilerTask {
  static final String INHERIT_FUNCTION = '''
function(child, parent) {
  if (child.prototype.__proto__) {
    child.prototype.__proto__ = parent.prototype;
  } else {
    function tmp() {};
    tmp.prototype = parent.prototype;
    child.prototype = new tmp();
    child.prototype.constructor = child;
  }
}''';

  bool addedInheritFunction = false;
  final Namer namer;

  CodeEmitterTask(Compiler compiler) : namer = compiler.namer, super(compiler);
  String get name() => 'CodeEmitter';

  String get inheritsName() => '${compiler.namer.ISOLATE}.\$inherits';

  void addInheritFunctionIfNecessary(StringBuffer buffer) {
    if (addedInheritFunction) return;
    addedInheritFunction = true;
    buffer.add('$inheritsName = ');
    buffer.add(INHERIT_FUNCTION);
    buffer.add(';\n');
  }

  void addParameterStub(FunctionElement member,
                        String prototype,
                        StringBuffer buffer,
                        Selector selector) {
    FunctionParameters parameters = member.computeParameters(compiler);
    int positionalArgumentCount = selector.positionalArgumentCount;
    if (positionalArgumentCount == parameters.parameterCount) {
      assert(selector.namedArgumentCount == 0);
      return;
    }
    CompileTimeConstantHandler constants = compiler.compileTimeConstantHandler;
    List<SourceString> names = selector.getOrderedNamedArguments();

    String invocationName =
        namer.instanceMethodInvocationName(member.name, selector);
    buffer.add('$prototype.$invocationName = function(');

    // The parameters that this stub takes.
    List<String> parametersBuffer = new List<String>(selector.argumentCount);
    // The arguments that will be passed to the real method.
    List<String> argumentsBuffer = new List<String>(parameters.parameterCount);

    // We fill the lists depending on the selector. For example,
    // take method foo:
    //    foo(a, b, [c, d]);
    //
    // We may have multiple ways of calling foo:
    // (1) foo(1, 2, 3, 4)
    // (2) foo(1, 2);
    // (3) foo(1, 2, 3);
    // (4) foo(1, 2, c: 3);
    // (5) foo(1, 2, d: 4);
    // (6) foo(1, 2, c: 3, d: 4);
    // (7) foo(1, 2, d: 4, c: 3);
    //
    // What we generate at the call sites are:
    // (1) foo$4(1, 2, 3, 4)
    // (2) foo$2(1, 2);
    // (3) foo$3(1, 2, 3);
    // (4) foo$3$c(1, 2, 3);
    // (5) foo$3$d(1, 2, 4);
    // (6) foo$4$c$d(1, 2, 3, 4);
    // (7) foo$4$c$d(1, 2, 3, 4);
    //
    // The stubs we generate are (expressed in Dart):
    // (1) No stub generated, call is direct.
    // (2) foo$2(a, b) => foo$4(a, b, null, null)
    // (3) foo$3(a, b, c) => foo$4(a, b, c, null)
    // (4) foo$3$c(a, b, c) => foo$4(a, b, c, null);
    // (5) foo$3$d(a, b, d) => foo$4(a, b, null, d);
    // (6) foo$4$c$d(a, b, c, d) => foo$4(a, b, c, d);
    // (7) Same as (5).
    //
    // We need to generate a stub for (5) because the order of the
    // stub arguments and the real method may be different.

    int count = 0;
    parameters.forEachParameter((Element element) {
      String jsName = JsNames.getValid('${element.name}');
      if (count < positionalArgumentCount) {
        parametersBuffer[count] = jsName;
        argumentsBuffer[count] = jsName;
      } else {
        int index = names.indexOf(element.name);
        if (index != -1) {
          // The order of the named arguments is not the same as the
          // one in the real method (which is in Dart source order).
          argumentsBuffer[count] = jsName;
          parametersBuffer[selector.positionalArgumentCount + index] = jsName;
        } else {
          var value = constants.writeJsCodeForVariable(new StringBuffer(),
                                                       element);
          argumentsBuffer[count] = value.toString();
        }
      }
      count++;
    });

    buffer.add('${Strings.join(parametersBuffer, ",")}) {\n');
    buffer.add('  return this.${namer.getName(member)}');
    buffer.add('(${Strings.join(argumentsBuffer, ",")})\n}\n');
  }

  void addParameterStubs(FunctionElement member,
                         String prototype,
                         StringBuffer buffer) {
    Set<Selector> selectors = compiler.universe.invokedNames[member.name];
    if (selectors == null) return;
    for (Selector selector in selectors) {
      if (!selector.applies(compiler, member)) continue;
      addParameterStub(member, prototype, buffer, selector);
    }
  }

  void addInstanceMember(Element member,
                         String prototype,
                         StringBuffer buffer) {
    assert(member.isInstanceMember());
    if (member.kind === ElementKind.FUNCTION
        || member.kind === ElementKind.GENERATIVE_CONSTRUCTOR_BODY
        || member.kind === ElementKind.GETTER
        || member.kind === ElementKind.SETTER) {
      String codeBlock = compiler.universe.generatedCode[member];
      if (codeBlock !== null) {
        buffer.add('$prototype.${namer.getName(member)} = $codeBlock;\n');
      }
      codeBlock = compiler.universe.generatedBailoutCode[member];
      if (codeBlock !== null) {
        String name = namer.getBailoutName(member);
        buffer.add('$prototype.$name = $codeBlock;\n');
      }
      FunctionElement function = member;
      if (!function.computeParameters(compiler).optionalParameters.isEmpty()) {
        addParameterStubs(member, prototype, buffer);
      }
    } else if (member.kind === ElementKind.FIELD) {
      // TODO(ngeoffray): Have another class generate the code for the
      // fields.
      if (compiler.universe.invokedSetters.contains(member.name)) {
        String setterName = namer.setterName(member.name);
        buffer.add('$prototype.$setterName = function(v){\n' +
          '  this.${namer.getName(member)} = v;\n};\n');
      }
      if (compiler.universe.invokedGetters.contains(member.name)) {
        String getterName = namer.getterName(member.name);
        buffer.add('$prototype.$getterName = function(){\n' +
          '  return this.${namer.getName(member)};\n};\n');
      }
    } else {
      compiler.internalError('unexpected kind: "${member.kind}"',
                             element: member);
    }
  }

  bool generateFieldInits(ClassElement classElement,
                          StringBuffer argumentsBuffer,
                          StringBuffer bodyBuffer) {
    bool isFirst = true;
    do {
      // TODO(floitsch): make sure there are no name clashes.
      String className = namer.getName(classElement);

      void generateFieldInit(Element member) {
        if (member.isInstanceMember() && member.kind == ElementKind.FIELD) {
          if (!isFirst) argumentsBuffer.add(', ');
          isFirst = false;
          String memberName = namer.instanceFieldName(member.name);
          argumentsBuffer.add('${className}_$memberName');
          bodyBuffer.add('  this.$memberName = ${className}_$memberName;\n');
        }        
      }

      for (Element element in classElement.members) {
        generateFieldInit(element);
      }
      for (Element element in classElement.backendMembers) {
        generateFieldInit(element);
      }

      classElement = classElement.superclass;
    } while(classElement !== null);
  }

  void generateClass(ClassElement classElement,
                     StringBuffer buffer,
                     Set<ClassElement> seenClasses) {
    if (seenClasses.contains(classElement)) return;
    seenClasses.add(classElement);
    ClassElement superclass = classElement.superclass;
    if (superclass !== null) {
      generateClass(classElement.superclass, buffer, seenClasses);
    }

    String className = namer.isolatePropertyAccess(classElement);
    buffer.add('$className = function ${classElement.name}(');
    StringBuffer bodyBuffer = new StringBuffer();
    // If the class is never instantiated we still need to set it up for
    // inheritance purposes, but we can leave its JavaScript constructor empty.
    if (compiler.universe.instantiatedClasses.contains(classElement)) {
      generateFieldInits(classElement, buffer, bodyBuffer);
    }
    buffer.add(') {\n');
    buffer.add(bodyBuffer);
    buffer.add('};\n');
    if (superclass !== null) {
      addInheritFunctionIfNecessary(buffer);
      String superName = namer.isolatePropertyAccess(superclass);
      buffer.add('${inheritsName}($className, $superName);\n');
    }
    String prototype = '$className.prototype';

    for (Element member in classElement.members) {
      if (member.isInstanceMember()) {
        addInstanceMember(member, prototype, buffer);
      }
    }
    for (Element member in classElement.backendMembers) {
      if (member.isInstanceMember()) {
        addInstanceMember(member, prototype, buffer);
      }
    }
    buffer.add('$prototype.${namer.operatorIs(classElement)} = true;\n');
    for (Type ifc in classElement.interfaces) {
      buffer.add('$prototype.${namer.operatorIs(ifc.element)} = true;\n');
    }
  }

  void emitClasses(StringBuffer buffer) {
    Set seenClasses = new Set<ClassElement>();
    for (ClassElement element in compiler.universe.instantiatedClasses) {
      generateClass(element, buffer, seenClasses);
    }
  }

  void emitStaticFunctionsWithNamer(StringBuffer buffer,
                                    Map<Element, String> generatedCode,
                                    String functionNamer(Element element)) {
    generatedCode.forEach((Element element, String codeBlock) {
      if (!element.isInstanceMember()) {
        buffer.add('${functionNamer(element)} = ');
        buffer.add(codeBlock);
        buffer.add(';\n\n');
      }
    });
  }

  void emitStaticFunctions(StringBuffer buffer) {
    emitStaticFunctionsWithNamer(buffer,
                                 compiler.universe.generatedCode,
                                 namer.isolatePropertyAccess);
    emitStaticFunctionsWithNamer(buffer,
                                 compiler.universe.generatedBailoutCode,
                                 namer.isolateBailoutPropertyAccess);
  }

  void emitStaticFunctionGetters(StringBuffer buffer) {
    Set<FunctionElement> functionsNeedingGetter =
        compiler.universe.staticFunctionsNeedingGetter;
    for (FunctionElement element in functionsNeedingGetter) {
      // The static function does not have the correct name. Since
      // [addParameterStubs] use the name to create its stubs we simply
      // create a fake element with the correct name.
      // Note: the callElement will not have the correct modifiers (in case
      // of static functions) and will not have any enclosingElement.
      FunctionElement callElement =
          new FunctionElement.from(Namer.CLOSURE_INVOCATION_NAME,
                                   element,
                                   null);
      String staticName = namer.isolatePropertyAccess(element);
      int parameterCount = element.parameterCount(compiler);
      String invocationName =
          namer.instanceMethodName(callElement.name, parameterCount);
      buffer.add("$staticName.$invocationName = $staticName;\n");
      addParameterStubs(callElement, staticName, buffer);
    }
  }

  void emitDynamicFunctionGetter(StringBuffer buffer,
                                 ClassElement enclosingClass,
                                 FunctionElement member) {
    // For every method that has the same name as a property-get we create a
    // getter that returns a bound closure. Say we have a class 'A' with method
    // 'foo' and somewhere in the code there is a dynamic property get of
    // 'foo'. Then we generate the following code (in pseudo Dart):
    //
    // class A {
    //    foo(x, y, z) { ... } // Original function.
    //    get foo() { return new BoundClosure499(this); }
    // }
    // class BoundClosure499 {
    //   var self;
    //   BoundClosure499(this.self);
    //   $call3(x, y, z) { return self.foo(x, y, z); }
    // }

    // TODO(floitsch): share the closure classes with other classes
    // if they share methods with the same signature.

    // The closure class.
    SourceString name = const SourceString("BoundClosure");
    CompilationUnitElement compilationUnit = member.getCompilationUnit();
    ClassElement closureClassElement = new ClassElement(name, compilationUnit);
    String isolateAccess = namer.isolatePropertyAccess(closureClassElement);
    buffer.add("$isolateAccess = function(self) { this.self = self; };\n");
    String prototype = "$isolateAccess.prototype";

    // Now add the methods on the closure class. The instance method does not
    // have the correct name. Since [addParameterStubs] use the name to create
    // its stubs we simply create a fake element with the correct name.
    // Note: the callElement will not have the correct modifiers (in case
    // of static functions) and will not have any enclosingElement.
    FunctionElement callElement =
        new FunctionElement.from(Namer.CLOSURE_INVOCATION_NAME, member, null);

    int parameterCount = member.parameterCount(compiler);
    String invocationName =
        namer.instanceMethodName(callElement.name, parameterCount);
    String targetName = namer.instanceMethodName(member.name, parameterCount);
    List<String> arguments = new List<String>(parameterCount);
    for (int i = 0; i < parameterCount; i++) {
      arguments[i] = "arg$i";
    }
    String joinedArgs = Strings.join(arguments, ", ");
    buffer.add("$prototype.$invocationName = function($joinedArgs) {\n");
    buffer.add("  return this.self.$targetName($joinedArgs);\n");
    buffer.add("};\n");
    addParameterStubs(callElement, prototype, buffer);

    // And finally the getter.
    String enclosingClassAccess = namer.isolatePropertyAccess(enclosingClass);
    String enclosingClassPrototype = "$enclosingClassAccess.prototype";
    String getterName = namer.getterName(member.name);
    String closureClass = namer.isolateAccess(closureClassElement);
    buffer.add("$enclosingClassPrototype.$getterName = function() {\n");
    buffer.add("  return new $closureClass(this);\n");
    buffer.add("};\n");
  }

  void emitDynamicFunctionGetters(StringBuffer buffer) {
    for (ClassElement classElement in compiler.universe.instantiatedClasses) {
      for (ClassElement currentClass = classElement;
           currentClass !== null;
           currentClass = currentClass.superclass) {
        // TODO(floitsch): we don't need to deal with members that have been
        // overwritten by subclasses.
        for (Element member in currentClass.members) {
          if (!member.isInstanceMember()) continue;
          if (member.kind == ElementKind.FUNCTION) {
            if (compiler.universe.invokedGetters.contains(member.name)) {
              emitDynamicFunctionGetter(buffer, currentClass, member);
            }
          }
        }
      }
    }
  }

  void emitCallStubForGetter(StringBuffer buffer,
                             ClassElement enclosingClass,
                             Element member,
                             Set<Selector> selectors) {
    String prototype =
        "${namer.isolatePropertyAccess(enclosingClass)}.prototype";
    String getter;
    if (member.kind == ElementKind.GETTER) {
      getter = "this.${namer.getterName(member.name)}()";
    } else {
      getter = "this.${namer.instanceFieldName(member.name)}";
    }
    for (Selector selector in selectors) {
      String invocationName =
          namer.instanceMethodInvocationName(member.name, selector);
      SourceString callName = Namer.CLOSURE_INVOCATION_NAME;
      String closureCallName =
          namer.instanceMethodInvocationName(callName, selector);
      List<String> arguments = <String>[];
      for (int i = 0; i < selector.argumentCount; i++) {
        arguments.add("arg$i");
      }
      String joined = Strings.join(arguments, ", ");
      buffer.add("$prototype.$invocationName = function($joined) {\n");
      buffer.add("  return $getter.$closureCallName($joined);\n");
      buffer.add("};\n");
    }
  }

  void emitCallStubForGetters(StringBuffer buffer) {
    for (ClassElement classElement in compiler.universe.instantiatedClasses) {
      for (ClassElement currentClass = classElement;
           currentClass !== null;
           currentClass = currentClass.superclass) {
        // TODO(floitsch): we don't need to deal with members that have been
        // overwritten by subclasses.
        for (Element member in currentClass.members) {
          if (!member.isInstanceMember()) continue;
          if (member.kind == ElementKind.GETTER ||
              member.kind == ElementKind.FIELD) {
            Set<Selector> selectors =
                compiler.universe.invokedNames[member.name];
            if (selectors == null || selectors.isEmpty()) continue;
            emitCallStubForGetter(buffer, currentClass, member, selectors);
          }
        }
      }
    }    
  }

  void emitStaticNonFinalFieldInitializations(StringBuffer buffer) {
    // Adds initializations inside the Isolate constructor.
    // Example:
    //    function Isolate() {
    //       this.staticNonFinal = Isolate.prototype.someVal;
    //       ...
    //    }
    CompileTimeConstantHandler constants = compiler.compileTimeConstantHandler;
    List<VariableElement> staticNonFinalFields =
        constants.getStaticNonFinalFieldsForEmission();
    if (!staticNonFinalFields.isEmpty()) buffer.add('\n');
    for (Element element in staticNonFinalFields) {
      buffer.add('  this.${namer.getName(element)} = ');
      compiler.withCurrentElement(element, () {
          constants.writeJsCodeForVariable(buffer, element);
        });
      buffer.add(';\n');
    }
  }

  void emitStaticFinalFieldInitializations(StringBuffer buffer) {
    CompileTimeConstantHandler constants = compiler.compileTimeConstantHandler;
    List<VariableElement> staticFinalFields =
        constants.getStaticFinalFieldsForEmission();
    for (VariableElement element in staticFinalFields) {
      buffer.add('${namer.isolatePropertyAccess(element)} = ');
      compiler.withCurrentElement(element, () {
          constants.writeJsCodeForVariable(buffer, element);
        });
      buffer.add(';\n');
    }
  }

  void emitNoSuchMethodCalls(StringBuffer buffer) {
    // Do not generate no such method calls if there is no class.
    if (compiler.universe.instantiatedClasses.isEmpty()) return;

    // TODO(ngeoffray): We don't need to generate these methods if
    // nobody overwrites noSuchMethod.

    ClassElement objectClass =
        compiler.coreLibrary.find(const SourceString('Object'));
    String className = namer.isolatePropertyAccess(objectClass);
    String prototype = '$className.prototype';
    String noSuchMethodName =
        namer.instanceMethodName(Compiler.NO_SUCH_METHOD, 2);

    void generateMethod(String methodName, String jsName, Selector selector) {
      buffer.add('$prototype.$jsName = function');
      StringBuffer args = new StringBuffer();
      for (int i = 0; i < selector.argumentCount; i++) {
        if (i != 0) args.add(', ');
        args.add('arg$i');
      }
      buffer.add(' ($args) {\n');
      buffer.add("  return this.$noSuchMethodName('$methodName', [$args]);\n");
      buffer.add('}\n');
    }

    compiler.universe.invokedNames.forEach((SourceString methodName,
                                            Set<Selector> selectors) {
      if (objectClass.lookupLocalMember(methodName) === null
          && methodName != Namer.OPERATOR_EQUALS) {
        for (Selector selector in selectors) {
          String jsName =
              namer.instanceMethodInvocationName(methodName, selector);
          generateMethod(methodName.stringValue, jsName, selector);
        }
      }
    });

    compiler.universe.invokedGetters.forEach((SourceString getterName) {
      String jsName = namer.getterName(getterName);
      generateMethod('get $getterName', jsName, Selector.GETTER);
    });

    compiler.universe.invokedSetters.forEach((SourceString setterName) {
      String jsName = namer.setterName(setterName);
      generateMethod('set $setterName', jsName, Selector.SETTER);
    });
  }

  String assembleProgram() {
    measure(() {
      StringBuffer buffer = new StringBuffer();
      buffer.add('function ${namer.ISOLATE}() {');
      emitStaticNonFinalFieldInitializations(buffer);
      buffer.add('}\n\n');
      emitClasses(buffer);
      emitNoSuchMethodCalls(buffer);
      emitStaticFunctions(buffer);
      emitStaticFunctionGetters(buffer);
      emitDynamicFunctionGetters(buffer);
      emitCallStubForGetters(buffer);
      emitStaticFinalFieldInitializations(buffer);
      buffer.add('var ${namer.CURRENT_ISOLATE} = new ${namer.ISOLATE}();\n');
      Element main = compiler.mainApp.find(Compiler.MAIN);
      buffer.add('${namer.isolateAccess(main)}();\n');
      compiler.assembledCode = buffer.toString();
    });
    return compiler.assembledCode;
  }
}
