// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class EnqueueTask extends CompilerTask {
  EnqueueTask(Compiler compiler) : super(compiler);
  String get name() => 'Enqueue';

  void enqueueInvokedInstanceMethods() {
    // TODO(floitsch): find a more efficient way of doing this.
    measure(() {
      // Run through the classes and see if we need to compile methods.
      for (ClassElement classElement in compiler.universe.instantiatedClasses) {
        for (ClassElement currentClass = classElement;
             currentClass !== null;
             currentClass = currentClass.superclass) {
          processInstantiatedClass(currentClass);
        }
      }
    });
  }

  void processInstantiatedClass(ClassElement cls) {
    cls.members.forEach(processInstantiatedClassMember);
  }

  void registerFieldClosureInvocations() {
    measure(() {
      // Make sure that the closure understands a call with the given
      // selector. For a method-invocation of the form o.foo(a: 499), we
      // need to make sure that closures can handle the optional argument if
      // there exists a field or getter 'foo'.
      var names = compiler.universe.instantiatedClassInstanceFields;
      for (SourceString name in names) {
        Set<Selector> invokedSelectors = compiler.universe.invokedNames[name];
        if (invokedSelectors != null) {
          for (Selector selector in invokedSelectors) {
            compiler.registerDynamicInvocation(Namer.CLOSURE_INVOCATION_NAME,
                                               selector);
          }
        }
      }
    });
  }

  void processInstantiatedClassMember(Element member) {
    if (compiler.universe.generatedCode.containsKey(member)) return;

    if (!member.isInstanceMember()) return;

    if (member.kind == ElementKind.FUNCTION) {
      if (member.name == Compiler.NO_SUCH_METHOD) {
        compiler.enableNoSuchMethod(member);
      }
      Set<Selector> selectors = compiler.universe.invokedNames[member.name];
      if (selectors != null) {
        FunctionElement functionMember = member;
        FunctionParameters parameters =
            functionMember.computeParameters(compiler);
        for (Selector selector in selectors) {
          if (selector.applies(parameters)) {
            return compiler.addToWorkList(member);
          }
        }
      }
      // If there is a property access with the same name as a method we
      // need to emit the method.
      if (compiler.universe.invokedGetters.contains(member.name)) {
        // We will emit a closure, so make sure the closure class is
        // generated.
        compiler.closureClass.ensureResolved(compiler);
        compiler.registerInstantiatedClass(compiler.closureClass);
        return compiler.addToWorkList(member);
      }
    } else if (member.kind == ElementKind.GETTER) {
      if (compiler.universe.invokedGetters.contains(member.name)) {
        return compiler.addToWorkList(member);
      }
      // A method invocation like in o.foo(x, y) might actually be an
      // invocation of the getter foo followed by an invocation of the
      // returned closure.
      Set<Selector> invokedSelectors =
        compiler.universe.invokedNames[member.name];
      // We don't know what selectors the returned closure accepts. If
      // the set contains any selector we have to assume that it matches.
      if (invokedSelectors !== null && !invokedSelectors.isEmpty()) {
        return compiler.addToWorkList(member);
      }
    } else if (member.kind === ElementKind.SETTER) {
      if (compiler.universe.invokedSetters.contains(member.name)) {
        return compiler.addToWorkList(member);
      }
    }
  }

  void processUnseenInstantiatedClass(ClassElement cls) {
    measure(() {
      cls.members.forEach((member) {
        if (member.kind === ElementKind.GETTER ||
            member.kind === ElementKind.FIELD) {
          compiler.universe.instantiatedClassInstanceFields.add(member.name);
        }
      });
    });
  }
}
