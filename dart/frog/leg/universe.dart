// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Universe {
  final Element scope;

  Map<SourceString, Element> elements;
  Map<Element, String> generatedCode;
  final Set<ClassElement> instantiatedClasses;

  Universe() : elements = {}, generatedCode = {},
               instantiatedClasses = new Set<ClassElement>(),
               scope = new Element(const SourceString('global scope'),
                                   null, null);

  Element find(SourceString name) {
    return elements[name];
  }

  void define(Element element) {
    assert(elements[element.name] == null);
    elements[element.name] = element;
  }

  void addGeneratedCode(Element element, String code) {
    generatedCode[element] = code;
  }
}
