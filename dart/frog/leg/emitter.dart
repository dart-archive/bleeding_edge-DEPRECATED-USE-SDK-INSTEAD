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
  static final String INHERIT_FUNCTION = @'''
Isolate.$inherits = function(child, parent) {
  if (child.prototype.__proto__) {
    child.prototype.__proto__ = parent.prototype;
  } else {
    function tmp() {};
    tmp.prototype = parent.prototype;
    child.prototype = new tmp();
    child.prototype.constructor = child;
  }
}
''';

  bool addedInheritFunction = false;

  CodeEmitterTask(Compiler compiler) : super(compiler);
  String get name() => 'CodeEmitter';

  void addInheritFunctionIfNecessary(StringBuffer buffer) {
    if (addedInheritFunction) return;
    addedInheritFunction = true;
    buffer.add(INHERIT_FUNCTION);
  }

  void generateClass(ClassElement classElement,
                     StringBuffer buffer,
                     Set<ClassElement> seenClasses) {
    if (seenClasses.contains(classElement)) return;
    seenClasses.add(classElement);
    Type supertype = classElement.supertype;
    ClassElement superClass = (supertype === null ? null : supertype.element);
    if (superClass !== null) {
      generateClass(superClass, buffer, seenClasses);
    }

    buffer.add('Isolate.prototype.${classElement.name} = function() {};\n');
    if (superClass !== null) {
      addInheritFunctionIfNecessary(buffer);
      buffer.add('Isolate.\$inherits(' +
                 'Isolate.prototype.${classElement.name}, ' +
                 'Isolate.prototype.${superClass.name});\n');
    }
    String prototype = 'Isolate.prototype.${classElement.name}.prototype';
    // TODO(floitsch): run through classElement.members() instead of [].
    for (Element member in []) {
      if (member.kind !== ElementKind.FUNCTION) continue;
      assert(member is FunctionElement);
      // TODO(floitsch): if (element.isStatic()) continue;
      String codeBlock = compiler.universe.generatedCode[member];
      assert(codeBlock !== null);
      buffer.add('$prototype.${member.name} = $codeBlock;\n');
    }
  }

  String compileClasses(StringBuffer buffer) {
    Set seenClasses = new Set<ClassElement>();
    for (ClassElement element in compiler.universe.instantiatedClasses) {
      generateClass(element, buffer, seenClasses);
    }
    assert(() {
      for (Element element in compiler.universe.generatedCode.getKeys()) {
        // TODO(floitsch): check that every element is static.
        // if (!element.isStatic()) return false;
      }
      return true;
    });
    return buffer.toString();
  }

  String assembleProgram() {
    measure(() {
      StringBuffer buffer = new StringBuffer();
      buffer.add('function Isolate() {};\n\n');
      compileClasses(buffer);
      Map<Element, String> generatedCode = compiler.universe.generatedCode;
      generatedCode.forEach((Element element, String codeBlock) {
        buffer.add('Isolate.prototype.${element.name} = ');
        buffer.add(codeBlock);
        buffer.add(';\n\n');
      });
      buffer.add('var currentIsolate = new Isolate();\n');
      buffer.add('currentIsolate.main();\n');
      compiler.assembledCode = buffer.toString();
    });
    return compiler.assembledCode;
  }
}
