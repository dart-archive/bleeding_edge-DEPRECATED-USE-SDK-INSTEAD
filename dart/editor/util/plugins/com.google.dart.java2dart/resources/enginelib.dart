library engine.element;

import "javalib.dart";

class Element {}
class CompilationUnitElement extends Element {}
class ExecutableElement extends Element {}
class FunctionElement extends Element {}
class TypeAliasElement extends Element {}
class ClassElement extends Element {}
class FieldElement extends Element {
  bool isStatic() => false;
  bool isConst() => false;
}
class ConstructorElement extends Element {}
class MethodElement extends Element {}
class ParameterElement extends Element {}
class VariableElement extends Element {}

