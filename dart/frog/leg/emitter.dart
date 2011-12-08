// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
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

  CodeEmitterTask(Compiler compiler) : super(compiler);
  String get name() => 'CodeEmitter';

  String get inheritsName() => '${compiler.namer.isolate}.\$inherits';

  void addInheritFunctionIfNecessary(StringBuffer buffer) {
    if (addedInheritFunction) return;
    addedInheritFunction = true;
    buffer.add('$inheritsName = ');
    buffer.add(INHERIT_FUNCTION);
    buffer.add(';\n');
  }

  void generateClass(ClassElement classElement,
                     StringBuffer buffer,
                     Set<ClassElement> seenClasses) {
    Namer namer = compiler.namer;
    if (seenClasses.contains(classElement)) return;
    seenClasses.add(classElement);
    Type supertype = classElement.supertype;
    ClassElement superClass = (supertype === null ? null : supertype.element);
    if (superClass !== null) {
      generateClass(superClass, buffer, seenClasses);
    }

    String className = namer.isolatePropertyAccess(classElement);
    buffer.add('$className = function() {};\n');
    if (superClass !== null) {
      addInheritFunctionIfNecessary(buffer);
      String superName = namer.isolatePropertyAccess(superClass);
      buffer.add('${inheritsName}($className, $superName);\n');
    }
    String prototype = '$className.prototype';
    // TODO(floitsch): run through classElement.members() instead of [].
    for (Element member in []) {
      if (member.kind !== ElementKind.FUNCTION) continue;
      assert(member is FunctionElement);
      // TODO(floitsch): if (element.isStatic()) continue;
      String codeBlock = compiler.universe.generatedCode[member];
      assert(codeBlock !== null);
      buffer.add('$prototype.${namer.methodName(member)} = $codeBlock;\n');
    }

    Element synthesized = classElement.synthesizedConstructor;
    if (synthesized !== null) {
      String codeBlock = compiler.universe.generatedCode[synthesized];
      assert(codeBlock !== null);
      buffer.add('$prototype.${synthesized.name} = $codeBlock;\n');
    }
  }

  String compileClasses(StringBuffer buffer) {
    Set seenClasses = new Set<ClassElement>();
    for (ClassElement element in compiler.universe.instantiatedClasses) {
      generateClass(element, buffer, seenClasses);
    }
    return buffer.toString();
  }

  String assembleProgram() {
    measure(() {
      Namer namer = compiler.namer;
      StringBuffer buffer = new StringBuffer();
      buffer.add('function ${namer.isolate}() {};\n\n');
      compileClasses(buffer);
      Map<Element, String> generatedCode = compiler.universe.generatedCode;
      generatedCode.forEach((Element element, String codeBlock) {
        if (element.isStatic()) {
          buffer.add('${namer.isolatePropertyAccess(element)} = ');
          buffer.add(codeBlock);
          buffer.add(';\n\n');
        }
      });
      buffer.add('var ${namer.currentIsolate} = new ${namer.isolate}();\n');
      buffer.add('${namer.currentIsolate}.main();\n');
      compiler.assembledCode = buffer.toString();
    });
    return compiler.assembledCode;
  }
}
