// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library visitor;

import 'elements.dart';
import '../ssa/ssa.dart'
    show InterceptedElement;
import '../closure.dart'
    show ThisElement,
         BoxElement,
         BoxFieldElement,
         ClosureClassElement,
         ClosureFieldElement;

abstract class ElementVisitor<R> {
  R visit(Element e) => e.accept(this);

  R visitElement(Element e);
  R visitErroneousElement(ErroneousElement e) => visitFunctionElement(e);
  R visitWarnOnUseElement(WarnOnUseElement e) => visitElement(e);
  R visitAmbiguousElement(AmbiguousElement e) => visitElement(e);
  R visitScopeContainerElement(ScopeContainerElement e) => visitElement(e);
  R visitCompilationUnitElement(CompilationUnitElement e) => visitElement(e);
  R visitLibraryElement(LibraryElement e) => visitScopeContainerElement(e);
  R visitPrefixElement(PrefixElement e) => visitElement(e);
  R visitTypedefElement(TypedefElement e) => visitElement(e);
  R visitVariableElement(VariableElement e) => visitElement(e);
  R visitFieldElement(FieldElement e) => visitVariableElement(e);
  R visitFieldParameterElement(FieldParameterElement e) => visitElement(e);
  R visitAbstractFieldElement(AbstractFieldElement e) => visitElement(e);
  R visitFunctionElement(FunctionElement e) => visitElement(e);
  R visitConstructorBodyElement(ConstructorBodyElement e) => visitElement(e);
  R visitClassElement(ClassElement e) => visitScopeContainerElement(e);
  R visitTypeDeclarationElement(TypeDeclarationElement e) => visitElement(e);
  R visitMixinApplicationElement(MixinApplicationElement e) {
    return visitClassElement(e);
  }
  R visitVoidElement(VoidElement e) => visitElement(e);
  R visitLabelElement(LabelElement e) => visitElement(e);
  R visitTargetElement(TargetElement e) => visitElement(e);
  R visitTypeVariableElement(TypeVariableElement e) => visitElement(e);
  R visitInterceptedElement(InterceptedElement e) => visitElement(e);
  R visitThisElement(ThisElement e) => visitElement(e);
  R visitBoxElement(BoxElement e) => visitElement(e);
  R visitBoxFieldElement(BoxFieldElement e) => visitElement(e);
  R visitClosureClassElement(ClosureClassElement e) => visitClassElement(e);
  R visitClosureFieldElement(ClosureFieldElement e) => visitVariableElement(e);
}