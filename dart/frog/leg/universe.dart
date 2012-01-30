// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Universe {
  final Element scope;

  Map<SourceString, Element> elements;
  Map<Element, String> generatedCode;
  Map<Element, String> generatedBailoutCode;
  final Set<ClassElement> instantiatedClasses;
  // TODO(floitsch): we want more information than just the method name. For
  // example the number of arguments, etc.
  final Map<SourceString, Set<Invocation>> invokedNames;
  final Set<SourceString> invokedGetters;
  final Set<SourceString> invokedSetters;

  Universe() : elements = {}, generatedCode = {}, generatedBailoutCode = {},
               instantiatedClasses = new Set<ClassElement>(),
               invokedNames = new Map<SourceString, Set<Invocation>>(),
               invokedGetters = new Set<SourceString>(),
               invokedSetters = new Set<SourceString>(),
               scope = new Element(const SourceString('global scope'),
                                   null, null);

  Element find(SourceString name) {
    return elements[name];
  }

  void define(Element element, Compiler compiler) {
    Element existing = elements.putIfAbsent(element.name, () => element);
    if (existing !== element) {
      compiler.cancel('duplicate definition', token: element.position());
      compiler.cancel('existing definition', token: existing.position());
    }
  }

  void addGeneratedCode(WorkItem work, String code) {
    if (work.isBailoutVersion()) {
      generatedBailoutCode[work.element] = code;
    } else {
      generatedCode[work.element] = code;
    }
  }
}

class Invocation implements Hashable {
  // The numbers of arguments of the invocation. Includes named
  // arguments.
  final int argumentCount;
  final List<SourceString> namedArguments;

  Invocation(int this.argumentCount,
            [List<SourceString> this.namedArguments = const <SourceString>[]]);

  bool applies(Compiler compiler, FunctionElement element) {
    FunctionParameters parameters = element.computeParameters(compiler);
    int parameterCount = parameters.parameterCount;
    if (argumentCount > parameterCount) return false;

    bool hasOptionalParameters = !parameters.optionalParameters.isEmpty();
    if (namedArguments.isEmpty()) {
      if (!hasOptionalParameters) {
        return parameterCount == argumentCount;
      } else {
        int optionalParameterCount = parameters.optionalParameterCount;
        return argumentCount >= parameterCount &&
          argumentCount <= parameterCount + optionalParameterCount;
      }
    } else {
      if (!hasOptionalParameters) return false;
      for (SourceString name in namedArguments) {
        compiler.cancel('unimplemented named constructors');
      }
      return true;
    }
  }

  int hashCode() => argumentCount + 1000 * namedArguments.length;

  static bool sameNames(List<SourceString> first, List<SourceString> second) {
    for (int i = 0; i < first.length; i++) {
      return first[i] == second[i];
    }
    return true;
  }

  bool operator ==(other) {
    if (other is !Invocation) return false;
    return argumentCount == other.argumentCount
           && namedArguments.length == other.namedArguments.length
           && sameNames(namedArguments, other.namedArguments);
  }
}
