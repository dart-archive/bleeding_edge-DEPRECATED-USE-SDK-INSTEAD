// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library simple_types_inferrer;

import '../closure.dart' show ClosureClassMap, ClosureScope;
import '../cps_ir/cps_ir_nodes.dart' as cps_ir show Node;
import '../dart_types.dart'
    show DartType, InterfaceType, FunctionType, TypeKind;
import '../elements/elements.dart';
import '../js_backend/js_backend.dart' as js;
import '../native/native.dart' as native;
import '../resolution/operators.dart' as op;
import '../tree/tree.dart' as ast;
import '../types/types.dart'
    show TypesInferrer, FlatTypeMask, TypeMask, ContainerTypeMask,
         ElementTypeMask, ValueTypeMask, TypeSystem, MinimalInferrerEngine;
import '../util/util.dart' show Link, Spannable, Setlet;
import 'inferrer_visitor.dart';

// BUG(8802): There's a bug in the analyzer that makes the re-export
// of Selector from dart2jslib.dart fail. For now, we work around that
// by importing universe.dart explicitly and disabling the re-export.
import '../dart2jslib.dart' hide Selector, TypedSelector;
import '../universe/universe.dart'
    show Selector, SideEffects, TypedSelector, CallStructure;

/**
 * An implementation of [TypeSystem] for [TypeMask].
 */
class TypeMaskSystem implements TypeSystem<TypeMask> {
  final Compiler compiler;
  final ClassWorld classWorld;
  TypeMaskSystem(Compiler compiler)
      : this.compiler = compiler,
        this.classWorld = compiler.world;

  TypeMask narrowType(TypeMask type,
                      DartType annotation,
                      {bool isNullable: true}) {
    if (annotation.treatAsDynamic) return type;
    if (annotation.element == compiler.objectClass) return type;
    TypeMask otherType;
    if (annotation.isTypedef || annotation.isFunctionType) {
      otherType = functionType;
    } else if (annotation.isTypeVariable) {
      // TODO(ngeoffray): Narrow to bound.
      return type;
    } else if (annotation.isVoid) {
      otherType = nullType;
    } else {
      assert(annotation.isInterfaceType);
      otherType = new TypeMask.nonNullSubtype(annotation.element, classWorld);
    }
    if (isNullable) otherType = otherType.nullable();
    if (type == null) return otherType;
    return type.intersection(otherType, classWorld);
  }

  TypeMask computeLUB(TypeMask firstType, TypeMask secondType) {
    if (firstType == null) {
      return secondType;
    } else if (secondType == dynamicType || firstType == dynamicType) {
      return dynamicType;
    } else if (firstType == secondType) {
      return firstType;
    } else {
      TypeMask union = firstType.union(secondType, classWorld);
      // TODO(kasperl): If the union isn't nullable it seems wasteful
      // to use dynamic. Fix that.
      return union.containsAll(classWorld) ? dynamicType : union;
    }
  }

  TypeMask allocateDiamondPhi(TypeMask firstType, TypeMask secondType) {
    return computeLUB(firstType, secondType);
  }

  TypeMask get dynamicType => compiler.typesTask.dynamicType;
  TypeMask get nullType => compiler.typesTask.nullType;
  TypeMask get intType => compiler.typesTask.intType;
  TypeMask get uint32Type => compiler.typesTask.uint32Type;
  TypeMask get uint31Type => compiler.typesTask.uint31Type;
  TypeMask get positiveIntType => compiler.typesTask.positiveIntType;
  TypeMask get doubleType => compiler.typesTask.doubleType;
  TypeMask get numType => compiler.typesTask.numType;
  TypeMask get boolType => compiler.typesTask.boolType;
  TypeMask get functionType => compiler.typesTask.functionType;
  TypeMask get listType => compiler.typesTask.listType;
  TypeMask get constListType => compiler.typesTask.constListType;
  TypeMask get fixedListType => compiler.typesTask.fixedListType;
  TypeMask get growableListType => compiler.typesTask.growableListType;
  TypeMask get mapType => compiler.typesTask.mapType;
  TypeMask get constMapType => compiler.typesTask.constMapType;
  TypeMask get stringType => compiler.typesTask.stringType;
  TypeMask get typeType => compiler.typesTask.typeType;
  bool isNull(TypeMask mask) => mask.isEmpty && mask.isNullable;

  TypeMask stringLiteralType(ast.DartString value) => stringType;

  TypeMask nonNullSubtype(ClassElement type)
      => new TypeMask.nonNullSubtype(type.declaration, classWorld);
  TypeMask nonNullSubclass(ClassElement type)
      => new TypeMask.nonNullSubclass(type.declaration, classWorld);
  TypeMask nonNullExact(ClassElement type)
      => new TypeMask.nonNullExact(type.declaration, classWorld);
  TypeMask nonNullEmpty() => new TypeMask.nonNullEmpty();

  TypeMask allocateList(TypeMask type,
                        ast.Node node,
                        Element enclosing,
                        [TypeMask elementType, int length]) {
    return new ContainerTypeMask(type, node, enclosing, elementType, length);
  }

  TypeMask allocateMap(TypeMask type, ast.Node node, Element element,
                       [List<TypeMask> keys, List<TypeMask> values]) {
    return type;
  }

  TypeMask allocateClosure(ast.Node node, Element element) {
    return functionType;
  }

  Selector newTypedSelector(TypeMask receiver, Selector selector) {
    return new TypedSelector(receiver, selector, compiler.world);
  }

  TypeMask addPhiInput(Local variable,
                       TypeMask phiType,
                       TypeMask newType) {
    return computeLUB(phiType, newType);
  }

  TypeMask allocatePhi(ast.Node node,
                       Local variable,
                       TypeMask inputType) {
    return inputType;
  }

  TypeMask allocateLoopPhi(ast.Node node,
                           Local variable,
                           TypeMask inputType) {
    return inputType;
  }

  TypeMask simplifyPhi(ast.Node node,
                       Local variable,
                       TypeMask phiType) {
    return phiType;
  }

  bool selectorNeedsUpdate(TypeMask type, Selector selector) {
    return type != selector.mask;
  }

  TypeMask refineReceiver(Selector selector, TypeMask receiverType) {
    TypeMask newType = compiler.world.allFunctions.receiverType(selector);
    return receiverType.intersection(newType, classWorld);
  }

  TypeMask getConcreteTypeFor(TypeMask mask) => mask;
}

/**
 * Common super class used by [SimpleTypeInferrerVisitor] to propagate
 * type information about visited nodes, as well as to request type
 * information of elements.
 */
abstract class InferrerEngine<T, V extends TypeSystem>
    implements MinimalInferrerEngine<T> {
  final Compiler compiler;
  final ClassWorld classWorld;
  final V types;
  final Map<ast.Node, T> concreteTypes = new Map<ast.Node, T>();
  final Set<Element> generativeConstructorsExposingThis = new Set<Element>();

  InferrerEngine(Compiler compiler, this.types)
      : this.compiler = compiler,
        this.classWorld = compiler.world;

  /**
   * Records the default type of parameter [parameter].
   */
  void setDefaultTypeOfParameter(ParameterElement parameter, T type);

  /**
   * This helper breaks abstractions but is currently required to work around
   * the wrong modelling of default values of optional parameters of
   * synthetic constructors.
   *
   * TODO(johnniwinther): Remove once default values of synthetic parameters
   * are fixed.
   */
  bool hasAlreadyComputedTypeOfParameterDefault(ParameterElement paramemter);

  /**
   * Returns the type of [element].
   */
  T typeOfElement(Element element);

  /**
   * Returns the return type of [element].
   */
  T returnTypeOfElement(Element element);

  /**
   * Records that [node] sets final field [element] to be of type [type].
   *
   * [nodeHolder] is the element holder of [node].
   */
  void recordTypeOfFinalField(ast.Node node,
                              Element nodeHolder,
                              Element field,
                              T type);

  /**
   * Records that [node] sets non-final field [element] to be of type
   * [type].
   */
  void recordTypeOfNonFinalField(Spannable node, Element field, T type);

  /**
   * Records that [element] is of type [type].
   */
  void recordType(Element element, T type);

  /**
   * Records that the return type [element] is of type [type].
   */
  void recordReturnType(Element element, T type);

  /**
   * Registers that [caller] calls [callee] at location [node], with
   * [selector], and [arguments]. Note that [selector] is null for
   * forwarding constructors.
   *
   * [sideEffects] will be updated to incorporate [callee]'s side
   * effects.
   *
   * [inLoop] tells whether the call happens in a loop.
   */
  T registerCalledElement(Spannable node,
                          Selector selector,
                          Element caller,
                          Element callee,
                          ArgumentsTypes<T> arguments,
                          SideEffects sideEffects,
                          bool inLoop);

  /**
   * Registers that [caller] calls [selector] with [receiverType] as
   * receiver, and [arguments].
   *
   * [sideEffects] will be updated to incorporate the potential
   * callees' side effects.
   *
   * [inLoop] tells whether the call happens in a loop.
   */
  T registerCalledSelector(ast.Node node,
                           Selector selector,
                           T receiverType,
                           Element caller,
                           ArgumentsTypes<T> arguments,
                           SideEffects sideEffects,
                           bool inLoop);

  /**
   * Registers that [caller] calls [closure] with [arguments].
   *
   * [sideEffects] will be updated to incorporate the potential
   * callees' side effects.
   *
   * [inLoop] tells whether the call happens in a loop.
   */
  T registerCalledClosure(ast.Node node,
                          Selector selector,
                          T closure,
                          Element caller,
                          ArgumentsTypes<T> arguments,
                          SideEffects sideEffects,
                          bool inLoop);

  /**
   * Registers a call to await with an expression of type [argumentType] as
   * argument.
   */
  T registerAwait(ast.Node node, T argumentType);

  /**
   * Notifies to the inferrer that [analyzedElement] can have return
   * type [newType]. [currentType] is the type the [InferrerVisitor]
   * currently found.
   *
   * Returns the new type for [analyzedElement].
   */
  T addReturnTypeFor(Element analyzedElement, T currentType, T newType);

  /**
   * Applies [f] to all elements in the universe that match
   * [selector]. If [f] returns false, aborts the iteration.
   */
  void forEachElementMatching(Selector selector, bool f(Element element)) {
    Iterable<Element> elements = compiler.world.allFunctions.filter(selector);
    for (Element e in elements) {
      if (!f(e.implementation)) return;
    }
  }

  /**
   * Update [sideEffects] with the side effects of [callee] being
   * called with [selector].
   */
  void updateSideEffects(SideEffects sideEffects,
                         Selector selector,
                         Element callee) {
    if (callee.isField) {
      if (callee.isInstanceMember) {
        if (selector.isSetter) {
          sideEffects.setChangesInstanceProperty();
        } else if (selector.isGetter) {
          sideEffects.setDependsOnInstancePropertyStore();
        } else {
          sideEffects.setAllSideEffects();
          sideEffects.setDependsOnSomething();
        }
      } else {
        if (selector.isSetter) {
          sideEffects.setChangesStaticProperty();
        } else if (selector.isGetter) {
          sideEffects.setDependsOnStaticPropertyStore();
        } else {
          sideEffects.setAllSideEffects();
          sideEffects.setDependsOnSomething();
        }
      }
    } else if (callee.isGetter && !selector.isGetter) {
      sideEffects.setAllSideEffects();
      sideEffects.setDependsOnSomething();
    } else {
      sideEffects.add(compiler.world.getSideEffectsOfElement(callee));
    }
  }

  /**
   * Returns the type for [nativeBehavior]. See documentation on
   * [native.NativeBehavior].
   */
  T typeOfNativeBehavior(native.NativeBehavior nativeBehavior) {
    if (nativeBehavior == null) return types.dynamicType;
    List typesReturned = nativeBehavior.typesReturned;
    if (typesReturned.isEmpty) return types.dynamicType;
    T returnType;
    for (var type in typesReturned) {
      T mappedType;
      if (type == native.SpecialType.JsObject) {
        mappedType = types.nonNullExact(compiler.objectClass);
      } else if (type.element == compiler.stringClass) {
        mappedType = types.stringType;
      } else if (type.element == compiler.intClass) {
        mappedType = types.intType;
      } else if (type.element == compiler.doubleClass) {
        mappedType = types.doubleType;
      } else if (type.element == compiler.numClass) {
        mappedType = types.numType;
      } else if (type.element == compiler.boolClass) {
        mappedType = types.boolType;
      } else if (type.element == compiler.nullClass) {
        mappedType = types.nullType;
      } else if (type.isVoid) {
        mappedType = types.nullType;
      } else if (type.isDynamic) {
        return types.dynamicType;
      } else {
        mappedType = types.nonNullSubtype(type.element);
      }
      returnType = types.computeLUB(returnType, mappedType);
      if (returnType == types.dynamicType) {
        break;
      }
    }
    return returnType;
  }

  void updateSelectorInTree(
      AstElement owner, Spannable node, Selector selector) {
    if (node is cps_ir.Node) {
      // TODO(lry): update selector for IrInvokeDynamic.
      throw "updateSelector for IR node $node";
    }
    ast.Node astNode = node;
    TreeElements elements = owner.resolvedAst.elements;
    if (astNode.asSendSet() != null) {
      if (selector.isSetter || selector.isIndexSet) {
        elements.setSelector(node, selector);
      } else if (selector.isGetter || selector.isIndex) {
        elements.setGetterSelectorInComplexSendSet(node, selector);
      } else {
        assert(selector.isOperator);
        elements.setOperatorSelectorInComplexSendSet(node, selector);
      }
    } else if (astNode.asSend() != null) {
      elements.setSelector(node, selector);
    } else {
      assert(astNode.asForIn() != null);
      if (selector.asUntyped == compiler.iteratorSelector) {
        elements.setIteratorSelector(node, selector);
      } else if (selector.asUntyped == compiler.currentSelector) {
        elements.setCurrentSelector(node, selector);
      } else {
        assert(selector.asUntyped == compiler.moveNextSelector);
        elements.setMoveNextSelector(node, selector);
      }
    }
  }

  bool isNativeElement(Element element) {
    if (element.isNative) return true;
    return element.isClassMember
        && element.enclosingClass.isNative
        && element.isField;
  }

  void analyze(Element element, ArgumentsTypes arguments);

  bool checkIfExposesThis(Element element) {
    element = element.implementation;
    return generativeConstructorsExposingThis.contains(element);
  }

  void recordExposesThis(Element element, bool exposesThis) {
    element = element.implementation;
    if (exposesThis) {
      generativeConstructorsExposingThis.add(element);
    }
  }
}

class SimpleTypeInferrerVisitor<T>
    extends InferrerVisitor<T, InferrerEngine<T, TypeSystem<T>>> {
  T returnType;
  bool visitingInitializers = false;
  bool isConstructorRedirect = false;
  bool seenSuperConstructorCall = false;
  SideEffects sideEffects = new SideEffects.empty();
  final Element outermostElement;
  final InferrerEngine<T, TypeSystem<T>> inferrer;
  final Setlet<Element> capturedVariables = new Setlet<Element>();

  SimpleTypeInferrerVisitor.internal(analyzedElement,
                                     this.outermostElement,
                                     inferrer,
                                     compiler,
                                     locals)
    : super(analyzedElement, inferrer, inferrer.types, compiler, locals),
      this.inferrer = inferrer {
    assert(outermostElement != null);
  }

  SimpleTypeInferrerVisitor(Element element,
                            Compiler compiler,
                            InferrerEngine<T, TypeSystem<T>> inferrer,
                            [LocalsHandler<T> handler])
    : this.internal(element,
        element.outermostEnclosingMemberOrTopLevel.implementation,
        inferrer, compiler, handler);

  void analyzeSuperConstructorCall(Element target, ArgumentsTypes arguments) {
    inferrer.analyze(target, arguments);
    isThisExposed = isThisExposed || inferrer.checkIfExposesThis(target);
  }

  T run() {
    var node = analyzedElement.node;
    ast.Expression initializer;
    if (analyzedElement.isField) {
      VariableElement fieldElement = analyzedElement;
      initializer = fieldElement.initializer;
      if (initializer == null) {
        // Eagerly bailout, because computing the closure data only
        // works for functions and field assignments.
        return types.nullType;
      }
    }
    // Update the locals that are boxed in [locals]. These locals will
    // be handled specially, in that we are computing their LUB at
    // each update, and reading them yields the type that was found in a
    // previous analysis of [outermostElement].
    ClosureClassMap closureData =
        compiler.closureToClassMapper.computeClosureToClassMapping(
            analyzedElement, node, elements);
    closureData.forEachCapturedVariable((variable, field) {
      locals.setCaptured(variable, field);
    });
    closureData.forEachBoxedVariable((variable, field) {
      locals.setCapturedAndBoxed(variable, field);
    });
    if (analyzedElement.isField) {
      return visit(initializer);
    }

    FunctionElement function = analyzedElement;
    FunctionSignature signature = function.functionSignature;
    signature.forEachOptionalParameter((ParameterElement element) {
      ast.Expression defaultValue = element.initializer;
      // If this is a default value from a different context (because
      // the current function is synthetic, e.g., a constructor from
      // a mixin application), we have to start a new inferrer visitor
      // with the correct context.
      // TODO(johnniwinther): Remove once function signatures are fixed.
      SimpleTypeInferrerVisitor visitor = this;
      if (inferrer.hasAlreadyComputedTypeOfParameterDefault(element)) return;
      if (element.functionDeclaration != analyzedElement) {
        visitor = new SimpleTypeInferrerVisitor(
            element.functionDeclaration, compiler, inferrer);
      }
      T type =
          (defaultValue == null) ? types.nullType : visitor.visit(defaultValue);
      inferrer.setDefaultTypeOfParameter(element, type);
    });

    if (analyzedElement.isNative) {
      // Native methods do not have a body, and we currently just say
      // they return dynamic.
      return types.dynamicType;
    }

    if (analyzedElement.isGenerativeConstructor) {
      isThisExposed = false;
      signature.forEachParameter((ParameterElement element) {
        T parameterType = inferrer.typeOfElement(element);
        if (element.isInitializingFormal) {
          InitializingFormalElement initializingFormal = element;
          if (initializingFormal.fieldElement.isFinal) {
            inferrer.recordTypeOfFinalField(
                node,
                analyzedElement,
                initializingFormal.fieldElement,
                parameterType);
          } else {
            locals.updateField(initializingFormal.fieldElement, parameterType);
            inferrer.recordTypeOfNonFinalField(
                initializingFormal.node,
                initializingFormal.fieldElement,
                parameterType);
          }
        }
        locals.update(element, parameterType, node);
      });
      ClassElement cls = analyzedElement.enclosingClass;
      Spannable spannable = node;
      if (analyzedElement.isSynthesized) {
        spannable = analyzedElement;
        ConstructorElement constructor = analyzedElement;
        synthesizeForwardingCall(spannable, constructor.definingConstructor);
      } else {
        visitingInitializers = true;
        visit(node.initializers);
        visitingInitializers = false;
        // For a generative constructor like: `Foo();`, we synthesize
        // a call to the default super constructor (the one that takes
        // no argument). Resolution ensures that such a constructor
        // exists.
        if (!isConstructorRedirect
            && !seenSuperConstructorCall
            && !cls.isObject) {
          FunctionElement target = cls.superclass.lookupDefaultConstructor();
          ArgumentsTypes arguments = new ArgumentsTypes([], {});
          analyzeSuperConstructorCall(target, arguments);
          inferrer.registerCalledElement(node,
                                         null,
                                         outermostElement,
                                         target.implementation,
                                         arguments,
                                         sideEffects,
                                         inLoop);
        }
        visit(node.body);
        inferrer.recordExposesThis(analyzedElement, isThisExposed);
      }
      if (!isConstructorRedirect) {
        // Iterate over all instance fields, and give a null type to
        // fields that we haven't initialized for sure.
        cls.forEachInstanceField((_, FieldElement field) {
          if (field.isFinal) return;
          T type = locals.fieldScope.readField(field);
          if (type == null && field.initializer == null) {
            inferrer.recordTypeOfNonFinalField(
                spannable, field, types.nullType);
          }
        });
      }
      returnType = types.nonNullExact(cls);
    } else {
      signature.forEachParameter((element) {
        locals.update(element, inferrer.typeOfElement(element), node);
      });
      visit(node.body);
      if (function.asyncMarker != AsyncMarker.SYNC) {
        // TODO(herhut): Should be type Future/Iterable/Stream instead of
        // dynamic.
        returnType = inferrer.addReturnTypeFor(
            analyzedElement, returnType, types.dynamicType);
      } else if (returnType == null) {
        // No return in the body.
        returnType = locals.seenReturnOrThrow
            ? types.nonNullEmpty()  // Body always throws.
            : types.nullType;
      } else if (!locals.seenReturnOrThrow) {
        // We haven't seen returns on all branches. So the method may
        // also return null.
        returnType = inferrer.addReturnTypeFor(
            analyzedElement, returnType, types.nullType);
      }
    }

    compiler.world.registerSideEffects(analyzedElement, sideEffects);
    assert(breaksFor.isEmpty);
    assert(continuesFor.isEmpty);
    return returnType;
  }

  T visitFunctionExpression(ast.FunctionExpression node) {
    // We loose track of [this] in closures (see issue 20840). To be on
    // the safe side, we mark [this] as exposed here. We could do better by
    // analyzing the closure.
    // TODO(herhut): Analyze whether closure exposes this.
    isThisExposed = true;
    LocalFunctionElement element = elements.getFunctionDefinition(node);
    // We don't put the closure in the work queue of the
    // inferrer, because it will share information with its enclosing
    // method, like for example the types of local variables.
    LocalsHandler closureLocals = new LocalsHandler<T>.from(
        locals, node, useOtherTryBlock: false);
    SimpleTypeInferrerVisitor visitor = new SimpleTypeInferrerVisitor<T>(
        element, compiler, inferrer, closureLocals);
    visitor.run();
    inferrer.recordReturnType(element, visitor.returnType);

    // Record the types of captured non-boxed variables. Types of
    // these variables may already be there, because of an analysis of
    // a previous closure.
    ClosureClassMap nestedClosureData =
        compiler.closureToClassMapper.getMappingForNestedFunction(node);
    nestedClosureData.forEachCapturedVariable((variable, field) {
      if (!nestedClosureData.isVariableBoxed(variable)) {
        if (variable == nestedClosureData.thisLocal) {
          inferrer.recordType(field, thisType);
        }
        // The type is null for type parameters.
        if (locals.locals[variable] == null) return;
        inferrer.recordType(field, locals.locals[variable]);
      }
      capturedVariables.add(variable);
    });

    return inferrer.concreteTypes.putIfAbsent(node, () {
      return types.allocateClosure(node, element);
    });
  }

  T visitFunctionDeclaration(ast.FunctionDeclaration node) {
    LocalFunctionElement element = elements.getFunctionDefinition(node.function);
    T type = inferrer.concreteTypes.putIfAbsent(node.function, () {
      return types.allocateClosure(node.function, element);
    });
    locals.update(element, type, node);
    visit(node.function);
    return type;
  }

  T visitStringInterpolation(ast.StringInterpolation node) {
    // Interpolation could have any effects since it could call any toString()
    // method.
    // TODO(sra): This could be modelled by a call to toString() but with a
    // guaranteed String return type.  Interpolation of known types would get
    // specialized effects.  This would not currently be effective since the JS
    // code in the toString methods for intercepted primitive types is assumed
    // to have all effects.  Effect annotations on JS code would be needed to
    // get the benefit.
    sideEffects.setAllSideEffects();
    return super.visitStringInterpolation(node);
  }

  T visitLiteralList(ast.LiteralList node) {
    // We only set the type once. We don't need to re-visit the children
    // when re-analyzing the node.
    return inferrer.concreteTypes.putIfAbsent(node, () {
      T elementType;
      int length = 0;
      for (ast.Node element in node.elements.nodes) {
        T type = visit(element);
        elementType = elementType == null
            ? types.allocatePhi(null, null, type)
            : types.addPhiInput(null, elementType, type);
        length++;
      }
      elementType = elementType == null
          ? types.nonNullEmpty()
          : types.simplifyPhi(null, null, elementType);
      T containerType = node.isConst
          ? types.constListType
          : types.growableListType;
      return types.allocateList(
          containerType,
          node,
          outermostElement,
          elementType,
          length);
    });
  }

  T visitLiteralMap(ast.LiteralMap node) {
    return inferrer.concreteTypes.putIfAbsent(node, () {
      ast.NodeList entries = node.entries;
      List<T> keyTypes = [];
      List<T> valueTypes = [];

      for (ast.LiteralMapEntry entry in entries) {
        keyTypes.add(visit(entry.key));
        valueTypes.add(visit(entry.value));
      }

      T type = node.isConst ? types.constMapType : types.mapType;
      return types.allocateMap(type,
                               node,
                               outermostElement,
                               keyTypes,
                               valueTypes);
    });
  }

  bool isThisOrSuper(ast.Node node) => node.isThis() || node.isSuper();

  bool isInClassOrSubclass(Element element) {
    ClassElement cls = outermostElement.enclosingClass.declaration;
    ClassElement enclosing = element.enclosingClass.declaration;
    return compiler.world.isSubclassOf(enclosing, cls);
  }

  void checkIfExposesThis(Selector selector) {
    if (isThisExposed) return;
    inferrer.forEachElementMatching(selector, (element) {
      if (element.isField) {
        if (!selector.isSetter
            && isInClassOrSubclass(element)
            && !element.modifiers.isFinal
            && locals.fieldScope.readField(element) == null
            && element.initializer == null) {
          // If the field is being used before this constructor
          // actually had a chance to initialize it, say it can be
          // null.
          inferrer.recordTypeOfNonFinalField(
              analyzedElement.node, element,
              types.nullType);
        }
        // Accessing a field does not expose [:this:].
        return true;
      }
      // TODO(ngeoffray): We could do better here if we knew what we
      // are calling does not expose this.
      isThisExposed = true;
      return false;
    });
  }

  bool get inInstanceContext {
    return (outermostElement.isInstanceMember && !outermostElement.isField)
        || outermostElement.isGenerativeConstructor;
  }

  bool treatAsInstanceMember(Element element) {
    return (Elements.isUnresolved(element) && inInstanceContext)
        || (element != null && element.isInstanceMember);
  }

  @override
  T handleSendSet(ast.SendSet node) {
    Element element = elements[node];
    if (!Elements.isUnresolved(element) && element.impliesType) {
      node.visitChildren(this);
      return types.dynamicType;
    }

    Selector getterSelector =
        elements.getGetterSelectorInComplexSendSet(node);
    Selector operatorSelector =
        elements.getOperatorSelectorInComplexSendSet(node);
    Selector setterSelector = elements.getSelector(node);

    String op = node.assignmentOperator.source;
    bool isIncrementOrDecrement = op == '++' || op == '--';

    T receiverType;
    bool isCallOnThis = false;
    if (node.receiver == null) {
      if (treatAsInstanceMember(element)) {
        receiverType = thisType;
        isCallOnThis = true;
      }
    } else {
      receiverType = visit(node.receiver);
      isCallOnThis = isThisOrSuper(node.receiver);
    }

    T rhsType;
    T indexType;

    if (isIncrementOrDecrement) {
      rhsType = types.uint31Type;
      if (node.isIndex) indexType = visit(node.arguments.head);
    } else if (node.isIndex) {
      indexType = visit(node.arguments.head);
      rhsType = visit(node.arguments.tail.head);
    } else {
      rhsType = visit(node.arguments.head);
    }

    if (!visitingInitializers && !isThisExposed) {
      for (ast.Node node in node.arguments) {
        if (isThisOrSuper(node)) {
          isThisExposed = true;
          break;
        }
      }
      if (!isThisExposed && isCallOnThis) {
        checkIfExposesThis(
            types.newTypedSelector(receiverType, setterSelector));
        if (getterSelector != null) {
          checkIfExposesThis(
              types.newTypedSelector(receiverType, getterSelector));
        }
      }
    }

    if (node.isIndex) {
      return internalError(node, "Unexpected index operation");
    } else if (op == '=') {
      return handlePlainAssignment(
          node, element, setterSelector, receiverType, rhsType,
          node.arguments.head);
    } else {
      // [: foo++ :] or [: foo += 1 :].
      ArgumentsTypes operatorArguments = new ArgumentsTypes<T>([rhsType], null);
      T getterType;
      T newType;
      if (Elements.isErroneous(element)) {
        getterType = types.dynamicType;
        newType = types.dynamicType;
      } else if (Elements.isStaticOrTopLevelField(element)) {
        Element getterElement = elements[node.selector];
        getterType =
            handleStaticSend(node, getterSelector, getterElement, null);
        newType = handleDynamicSend(
            node, operatorSelector, getterType, operatorArguments);
        handleStaticSend(
            node, setterSelector, element,
            new ArgumentsTypes<T>([newType], null));
      } else if (Elements.isUnresolved(element)
                 || element.isSetter
                 || element.isField) {
        getterType = handleDynamicSend(
            node, getterSelector, receiverType, null);
        newType = handleDynamicSend(
            node, operatorSelector, getterType, operatorArguments);
        handleDynamicSend(node, setterSelector, receiverType,
                          new ArgumentsTypes<T>([newType], null));
      } else if (element.isLocal) {
        LocalElement local = element;
        getterType = locals.use(local);
        newType = handleDynamicSend(
            node, operatorSelector, getterType, operatorArguments);
        locals.update(element, newType, node);
      } else {
        // Bogus SendSet, for example [: myMethod += 42 :].
        getterType = types.dynamicType;
        newType = handleDynamicSend(
            node, operatorSelector, getterType, operatorArguments);
      }

      if (node.isPostfix) {
        return getterType;
      } else {
        return newType;
      }
    }
  }

  /// Handle compound index set, like `foo[0] += 42` or `foo[0]++`.
  T handleCompoundIndexSet(
      ast.SendSet node,
      T receiverType,
      T indexType,
      T rhsType) {
    Selector getterSelector =
         elements.getGetterSelectorInComplexSendSet(node);
     Selector operatorSelector =
         elements.getOperatorSelectorInComplexSendSet(node);
     Selector setterSelector = elements.getSelector(node);

    T getterType = handleDynamicSend(
        node,
        getterSelector,
        receiverType,
        new ArgumentsTypes<T>([indexType], null));
    T returnType = handleDynamicSend(
        node,
        operatorSelector,
        getterType,
        new ArgumentsTypes<T>([rhsType], null));
    handleDynamicSend(
        node,
        setterSelector,
        receiverType,
        new ArgumentsTypes<T>([indexType, returnType], null));

    if (node.isPostfix) {
      return getterType;
    } else {
      return returnType;
    }
  }

  /// Handle compound prefix/postfix operations, like `a[0]++`.
  T handleCompoundPrefixPostfix(
      ast.Send node,
      T receiverType,
      T indexType) {
    return handleCompoundIndexSet(
        node, receiverType, indexType, types.uint31Type);
  }

  @override
  T visitIndexPostfix(
      ast.Send node,
      ast.Node receiver,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T receiverType = visit(receiver);
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, receiverType, indexType);
  }

  @override
  T visitIndexPrefix(
      ast.Send node,
      ast.Node receiver,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T receiverType = visit(receiver);
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, receiverType, indexType);
  }

  @override
  T visitCompoundIndexSet(
      ast.SendSet node,
      ast.Node receiver,
      ast.Node index,
      op.AssignmentOperator operator,
      ast.Node rhs,
      _) {
    T receiverType = visit(receiver);
    T indexType = visit(index);
    T rhsType = visit(rhs);
    return handleCompoundIndexSet(node, receiverType, indexType, rhsType);
  }

  @override
  T visitSuperIndexPrefix(
      ast.Send node,
      MethodElement getter,
      MethodElement setter,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  @override
  T visitSuperIndexPostfix(
      ast.Send node,
      MethodElement getter,
      MethodElement setter,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  /// Handle compound super index set, like `super[42] =+ 2`.
  T handleSuperCompoundIndexSet(
      ast.SendSet node,
      ast.Node index,
      ast.Node rhs) {
    T receiverType = superType;
    T indexType = visit(index);
    T rhsType = visit(rhs);
    return handleCompoundIndexSet(node, receiverType, indexType, rhsType);
  }

  @override
  T visitSuperCompoundIndexSet(
      ast.SendSet node,
      FunctionElement getter,
      FunctionElement setter,
      ast.Node index,
      op.AssignmentOperator operator,
      ast.Node rhs,
      _) {
    return handleSuperCompoundIndexSet(node, index, rhs);
  }

  @override
  T visitUnresolvedSuperGetterCompoundIndexSet(
      ast.SendSet node,
      Element element,
      ast.Node index,
      op.AssignmentOperator operator,
      ast.Node rhs,
      _) {
    return handleSuperCompoundIndexSet(node, index, rhs);
  }

  @override
  T visitUnresolvedSuperSetterCompoundIndexSet(
      ast.SendSet node,
      FunctionElement getter,
      Element element,
      ast.Node index,
      op.AssignmentOperator operator,
      ast.Node rhs,
      _) {
    return handleSuperCompoundIndexSet(node, index, rhs);
  }

  @override
  T visitUnresolvedSuperGetterIndexPrefix(
      ast.SendSet node,
      Element element,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  @override
  T visitUnresolvedSuperSetterIndexPrefix(
      ast.SendSet node,
      FunctionElement getter,
      Element element,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  @override
  T visitUnresolvedSuperGetterIndexPostfix(
      ast.SendSet node,
      Element element,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  @override
  T visitUnresolvedSuperSetterIndexPostfix(
      ast.SendSet node,
      FunctionElement getter,
      Element element,
      ast.Node index,
      op.IncDecOperator operator,
      _) {
    T indexType = visit(index);
    return handleCompoundPrefixPostfix(node, superType, indexType);
  }

  /// Handle index set, like `foo[0] = 42`.
  T handleIndexSet(ast.SendSet node, T receiverType, T indexType, T rhsType) {
    Selector setterSelector = elements.getSelector(node);
    handleDynamicSend(
        node,
        setterSelector,
        receiverType,
        new ArgumentsTypes<T>([indexType, rhsType], null));
    return rhsType;
  }

  @override
  T visitIndexSet(
      ast.SendSet node,
      ast.Node receiver,
      ast.Node index,
      ast.Node rhs,
      _) {
    T receiverType = visit(receiver);
    T indexType = visit(index);
    T rhsType = visit(rhs);
    return handleIndexSet(node, receiverType, indexType, rhsType);
  }

  /// Handle super index set, like `super[42] = true`.
  T handleSuperIndexSet(
      ast.SendSet node,
      ast.Node index,
      ast.Node rhs) {
    T receiverType = superType;
    T indexType = visit(index);
    T rhsType = visit(rhs);
    return handleIndexSet(node, receiverType, indexType, rhsType);
  }

  @override
  T visitSuperIndexSet(
      ast.SendSet node,
      FunctionElement function,
      ast.Node index,
      ast.Node rhs,
      _) {
    return handleSuperIndexSet(node, index, rhs);
  }

  @override
  T visitUnresolvedSuperIndexSet(
      ast.SendSet node,
      Element element,
      ast.Node index,
      ast.Node rhs,
      _) {
    return handleSuperIndexSet(node, index, rhs);
  }

  T handlePlainAssignment(ast.Node node,
                          Element element,
                          Selector setterSelector,
                          T receiverType,
                          T rhsType,
                          ast.Node rhs) {
    ArgumentsTypes arguments = new ArgumentsTypes<T>([rhsType], null);
    if (Elements.isErroneous(element)) {
      // Code will always throw.
    } else if (Elements.isStaticOrTopLevelField(element)) {
      handleStaticSend(node, setterSelector, element, arguments);
    } else if (Elements.isUnresolved(element) || element.isSetter) {
      if (analyzedElement.isGenerativeConstructor
          && (node.asSendSet() != null)
          && (node.asSendSet().receiver != null)
          && node.asSendSet().receiver.isThis()) {
        Iterable<Element> targets = compiler.world.allFunctions.filter(
            types.newTypedSelector(thisType, setterSelector));
        // We just recognized a field initialization of the form:
        // `this.foo = 42`. If there is only one target, we can update
        // its type.
        if (targets.length == 1) {
          Element single = targets.first;
          if (single.isField) {
            locals.updateField(single, rhsType);
          }
        }
      }
      handleDynamicSend(
          node, setterSelector, receiverType, arguments);
    } else if (element.isField) {
      if (element.isFinal) {
        inferrer.recordTypeOfFinalField(
            node, outermostElement, element, rhsType);
      } else {
        if (analyzedElement.isGenerativeConstructor) {
          locals.updateField(element, rhsType);
        }
        if (visitingInitializers) {
          inferrer.recordTypeOfNonFinalField(node, element, rhsType);
        } else {
          handleDynamicSend(
              node, setterSelector, receiverType, arguments);
        }
      }
    } else if (element.isLocal) {
      locals.update(element, rhsType, node);
    }
    return rhsType;
  }

  /// Handle a super access or invocation that results in a `noSuchMethod` call.
  T handleErroneousSuperSend(ast.Send node) {
    ArgumentsTypes arguments = node.isPropertyAccess
        ? null
        : analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    // TODO(herhut): We could do better here if we knew what we
    // are calling does not expose this.
    isThisExposed = true;
    // Ensure we create a node, to make explicit the call to the
    // `noSuchMethod` handler.
    return handleDynamicSend(node, selector, superType, arguments);
  }

  /// Handle a .call invocation on the values retrieved from the super
  /// [element]. For instance `super.foo(bar)` where `foo` is a field or getter.
  T handleSuperClosureCall(
      ast.Send node,
      Element element,
      ast.NodeList arguments) {
    ArgumentsTypes argumentTypes = analyzeArguments(arguments.nodes);
    Selector selector = elements.getSelector(node);
    // TODO(herhut): We could do better here if we knew what we
    // are calling does not expose this.
    isThisExposed = true;
    return inferrer.registerCalledClosure(
        node, selector, inferrer.typeOfElement(element),
        outermostElement, argumentTypes, sideEffects, inLoop);
  }

  /// Handle an invocation of super [method].
  T handleSuperMethodInvoke(ast.Send node,
                            MethodElement method,
                            ArgumentsTypes arguments) {
    // TODO(herhut): We could do better here if we knew what we
    // are calling does not expose this.
    isThisExposed = true;
    return handleStaticSend(
        node, elements.getSelector(node), method, arguments);
  }

  /// Handle access to a super field or getter [element].
  T handleSuperGet(ast.Send node,
                   Element element) {
    // TODO(herhut): We could do better here if we knew what we
    // are calling does not expose this.
    isThisExposed = true;
    return handleStaticSend(
        node, elements.getSelector(node), element, null);
  }

  /// Handle super constructor invocation.
  @override
  T handleSuperConstructorInvoke(ast.Send node) {
    Element element = elements[node];
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    assert(visitingInitializers);
    seenSuperConstructorCall = true;
    analyzeSuperConstructorCall(element, arguments);
    return handleStaticSend(
        node, elements.getSelector(node), element, arguments);
  }

  @override
  T visitUnresolvedSuperIndex(
      ast.Send node,
      Element element,
      ast.Node index,
      _) {
    return handleErroneousSuperSend(node);
  }

  @override
  T visitUnresolvedSuperUnary(
      ast.Send node,
      op.UnaryOperator operator,
      Element element,
      _) {
    return handleErroneousSuperSend(node);
  }

  @override
  T visitUnresolvedSuperBinary(
      ast.Send node,
      Element element,
      op.BinaryOperator operator,
      ast.Node argument,
      _) {
    return handleErroneousSuperSend(node);
  }

  @override
  T visitUnresolvedSuperGet(
      ast.Send node,
      Element element,
      _) {
    return handleErroneousSuperSend(node);
  }

  @override
  T visitUnresolvedSuperInvoke(
      ast.Send node,
      Element element,
      ast.Node argument,
      Selector selector,
      _) {
    return handleErroneousSuperSend(node);
  }

  @override
  T visitSuperFieldGet(
      ast.Send node,
      FieldElement field,
      _) {
    return handleSuperGet(node, field);
  }

  @override
  T visitSuperGetterGet(
      ast.Send node,
      MethodElement method,
      _) {
    return handleSuperGet(node, method);
  }

  @override
  T visitSuperMethodGet(
      ast.Send node,
      MethodElement method,
      _) {
    return handleSuperGet(node, method);
  }

  @override
  T visitSuperFieldInvoke(
      ast.Send node,
      FieldElement field,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleSuperClosureCall(node, field, arguments);
  }

  @override
  T visitSuperGetterInvoke(
      ast.Send node,
      MethodElement getter,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleSuperClosureCall(node, getter, arguments);
  }

  @override
  T visitSuperMethodInvoke(
      ast.Send node,
      MethodElement method,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleSuperMethodInvoke(
        node, method, analyzeArguments(arguments.nodes));
  }

  @override
  T visitSuperIndex(
      ast.Send node,
      MethodElement method,
      ast.Node index,
      _) {
    return handleSuperMethodInvoke(
        node, method, analyzeArguments(node.arguments));
  }

  @override
  T visitSuperEquals(
      ast.Send node,
      MethodElement method,
      ast.Node argument,
      _) {
    return handleSuperMethodInvoke(
        node, method, analyzeArguments(node.arguments));
  }

  @override
  T visitSuperBinary(
      ast.Send node,
      MethodElement method,
      op.BinaryOperator operator,
      ast.Node argument,
      _) {
    return handleSuperMethodInvoke(
        node, method, analyzeArguments(node.arguments));
  }

  @override
  T visitSuperUnary(
      ast.Send node,
      op.UnaryOperator operator,
      MethodElement method,
      _) {
    return handleSuperMethodInvoke(
        node, method, analyzeArguments(node.arguments));
  }

  @override
  T visitSuperMethodIncompatibleInvoke(
      ast.Send node,
      MethodElement method,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleErroneousSuperSend(node);
  }

  // Try to find the length given to a fixed array constructor call.
  int findLength(ast.Send node) {
    ast.Node firstArgument = node.arguments.head;
    Element element = elements[firstArgument];
    ast.LiteralInt length = firstArgument.asLiteralInt();
    if (length != null) {
      return length.value;
    } else if (element != null
               && element.isField
               && Elements.isStaticOrTopLevelField(element)
               && compiler.world.fieldNeverChanges(element)) {
      var constant =
          compiler.backend.constants.getConstantForVariable(element);
      if (constant != null && constant.value.isInt) {
        return constant.value.primitiveValue;
      }
    }
    return null;
  }

  T visitAwait(ast.Await node) {
    T futureType = node.expression.accept(this);
    return inferrer.registerAwait(node, futureType);
  }

  @override
  T handleAssert(ast.Send node, ast.Node expression) {
    js.JavaScriptBackend backend = compiler.backend;
    Element element = backend.assertMethod;
    ArgumentsTypes<T> arguments =
        new ArgumentsTypes<T>(<T>[expression.accept(this)], null);
    return handleStaticSend(
        node, new Selector.fromElement(element), element, arguments);
  }

  @override
  T handleTypeLiteralInvoke(ast.NodeList arguments) {
    // This is reached when users forget to put a `new` in front of a type
    // literal. The emitter will generate an actual call (even though it is
    // likely invalid), and for that it needs to have the arguments processed
    // as well.
    analyzeArguments(arguments.nodes);
    return super.handleTypeLiteralInvoke(arguments);
  }

  T visitStaticSend(ast.Send node) {
    assert(!elements.isAssert(node));
    Element element = elements[node];
    return handleConstructorSend(node, element);
  }

  /// Handle constructor invocation of [element].
  T handleConstructorSend(ast.Send node, ConstructorElement element) {
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    if (visitingInitializers) {
      if (ast.Initializers.isConstructorRedirect(node)) {
        isConstructorRedirect = true;
      } else if (ast.Initializers.isSuperConstructorCall(node)) {
        seenSuperConstructorCall = true;
        analyzeSuperConstructorCall(element, arguments);
      }
    }
    // If we are looking at a new expression on a forwarding factory,
    // we have to forward the call to the effective target of the
    // factory.
    if (element.isFactoryConstructor) {
      // TODO(herhut): Remove the while loop once effectiveTarget forwards to
      //               patches.
      while (element.isFactoryConstructor) {
        ConstructorElement constructor = element;
        if (!constructor.isRedirectingFactory) break;
        element = constructor.effectiveTarget.implementation;
      }
    }
    if (element.isForeign(compiler.backend)) {
      return handleForeignSend(node, element);
    }
    Selector selector = elements.getSelector(node);
    // In erroneous code the number of arguments in the selector might not
    // match the function element.
    // TODO(polux): return nonNullEmpty and check it doesn't break anything
    if (!selector.applies(element, compiler.world)) {
      return types.dynamicType;
    }

    T returnType = handleStaticSend(node, selector, element, arguments);
    if (Elements.isGrowableListConstructorCall(element, node, compiler)) {
      return inferrer.concreteTypes.putIfAbsent(
          node, () => types.allocateList(
              types.growableListType, node, outermostElement,
              types.nonNullEmpty(), 0));
    } else if (Elements.isFixedListConstructorCall(element, node, compiler)
        || Elements.isFilledListConstructorCall(element, node, compiler)) {

      int length = findLength(node);
      T elementType =
          Elements.isFixedListConstructorCall(element, node, compiler)
              ? types.nullType
              : arguments.positional[1];

      return inferrer.concreteTypes.putIfAbsent(
          node, () => types.allocateList(
              types.fixedListType, node, outermostElement,
              elementType, length));
    } else if (Elements.isConstructorOfTypedArraySubclass(element, compiler)) {
      int length = findLength(node);
      ConstructorElement constructor = element.implementation;
      constructor = constructor.effectiveTarget;
      T elementType = inferrer.returnTypeOfElement(
          constructor.enclosingClass.lookupMember('[]'));
      return inferrer.concreteTypes.putIfAbsent(
        node, () => types.allocateList(
          types.nonNullExact(constructor.enclosingClass), node,
          outermostElement, elementType, length));
    } else {
      return returnType;
    }
  }

  T handleNewExpression(ast.NewExpression node) {
    return visitStaticSend(node.send);
  }

  /// Handle invocation of a top level or static field or getter [element].
  T handleStaticFieldOrGetterInvoke(ast.Send node, Element element) {
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    handleStaticSend(node, selector, element, arguments);
    return inferrer.registerCalledClosure(
              node, selector, inferrer.typeOfElement(element),
              outermostElement, arguments, sideEffects, inLoop);
  }

  /// Handle invocation of a top level or static [function].
  T handleStaticFunctionInvoke(ast.Send node, MethodElement function) {
    if (function.isForeign(compiler.backend)) {
      return handleForeignSend(node, function);
    }
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    return handleStaticSend(node, selector, function, arguments);
  }

  /// Handle an static invocation of an unresolved target or with incompatible
  /// arguments to a resolved target.
  T handleInvalidStaticInvoke(ast.Send node) {
    analyzeArguments(node.arguments);
    return types.dynamicType;
  }

  @override
  T visitStaticFieldInvoke(
      ast.Send node,
      FieldElement field,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFieldOrGetterInvoke(node, field);
  }

  @override
  T visitStaticFunctionInvoke(
      ast.Send node,
      MethodElement function,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFunctionInvoke(node, function);
  }

  @override
  T visitStaticFunctionIncompatibleInvoke(
      ast.Send node,
      MethodElement function,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleInvalidStaticInvoke(node);
  }

  @override
  T visitStaticGetterInvoke(
      ast.Send node,
      FunctionElement getter,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFieldOrGetterInvoke(node, getter);
  }

  @override
  T visitTopLevelFieldInvoke(
      ast.Send node,
      FieldElement field,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFieldOrGetterInvoke(node, field);
  }

  @override
  T visitTopLevelFunctionInvoke(
      ast.Send node,
      MethodElement function,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFunctionInvoke(node, function);
  }

  @override
  T visitTopLevelFunctionIncompatibleInvoke(
      ast.Send node,
      MethodElement function,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleInvalidStaticInvoke(node);
  }

  @override
  T visitTopLevelGetterInvoke(
      ast.Send node,
      FunctionElement getter,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleStaticFieldOrGetterInvoke(node, getter);
  }

  @override
  T visitUnresolvedInvoke(
      ast.Send node,
      Element element,
      ast.NodeList arguments,
      Selector selector,
      _) {
    return handleInvalidStaticInvoke(node);
  }

  T handleForeignSend(ast.Send node, Element element) {
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    String name = element.name;
    handleStaticSend(node, selector, element, arguments);
    if (name == 'JS' || name == 'JS_EMBEDDED_GLOBAL' || name == 'JS_BUILTIN') {
      native.NativeBehavior nativeBehavior =
          compiler.enqueuer.resolution.nativeEnqueuer.getNativeBehaviorOf(node);
      sideEffects.add(nativeBehavior.sideEffects);
      return inferrer.typeOfNativeBehavior(nativeBehavior);
    } else if (name == 'JS_NULL_CLASS_NAME'
               || name == 'JS_OBJECT_CLASS_NAME'
               || name == 'JS_OPERATOR_AS_PREFIX'
               || name == 'JS_STRING_CONCAT') {
      return types.stringType;
    } else {
      sideEffects.setAllSideEffects();
      return types.dynamicType;
    }
  }

  ArgumentsTypes analyzeArguments(Link<ast.Node> arguments) {
    List<T> positional = [];
    Map<String, T> named;
    for (var argument in arguments) {
      ast.NamedArgument namedArgument = argument.asNamedArgument();
      if (namedArgument != null) {
        argument = namedArgument.expression;
        if (named == null) named = new Map<String, T>();
        named[namedArgument.name.source] = argument.accept(this);
      } else {
        positional.add(argument.accept(this));
      }
      // TODO(ngeoffray): We could do better here if we knew what we
      // are calling does not expose this.
      isThisExposed = isThisExposed || argument.isThis();
    }
    return new ArgumentsTypes<T>(positional, named);
  }

  T visitGetterSend(ast.Send node) {
    if (elements[node] is! PrefixElement) {
      // TODO(johnniwinther): Remove this when no longer called from
      // [handleSendSet].
      internalError(node, "Unexpected visitGetterSend");
    }
    return null;
  }

  /// Read a local variable, function or parameter.
  T handleLocalGet(ast.Send node, LocalElement local) {
    assert(locals.use(local) != null);
    return locals.use(local);
  }

  /// Read a static or top level field.
  T handleStaticFieldGet(ast.Send node, FieldElement field) {
    return handleStaticSend(node, elements.getSelector(node), field, null);
  }

  /// Invoke a static or top level getter.
  T handleStaticGetterGet(ast.Send node, MethodElement getter) {
    return handleStaticSend(node, elements.getSelector(node), getter, null);
  }

  /// Closurize a static or top level function.
  T handleStaticFunctionGet(ast.Send node, MethodElement function) {
    return handleStaticSend(node, elements.getSelector(node), function, null);
  }

  @override
  T visitDynamicPropertyGet(
      ast.Send node,
      ast.Node receiver,
      Selector selector,
      _) {
    return handleDynamicGet(node);
  }

  @override
  T visitLocalVariableGet(
      ast.Send node,
      LocalVariableElement variable,
      _) {
    return handleLocalGet(node, variable);
  }

  @override
  T visitParameterGet(
      ast.Send node,
      ParameterElement parameter,
      _) {
    return handleLocalGet(node, parameter);
  }

  @override
  T visitLocalFunctionGet(
      ast.Send node,
      LocalFunctionElement function,
      _) {
    return handleLocalGet(node, function);
  }

  @override
  T visitStaticFieldGet(
      ast.Send node,
      FieldElement field,
      _) {
    return handleStaticFieldGet(node, field);
  }

  @override
  T visitStaticFunctionGet(
      ast.Send node,
      MethodElement function,
      _) {
    return handleStaticFunctionGet(node, function);
  }

  @override
  T visitStaticGetterGet(
      ast.Send node,
      FunctionElement getter,
      _) {
    return handleStaticGetterGet(node, getter);
  }

  @override
  T visitThisPropertyGet(
      ast.Send node,
      Selector selector,
      _) {
    return handleDynamicGet(node);
  }

  @override
  T visitTopLevelFieldGet(
      ast.Send node,
      FieldElement field,
      _) {
    return handleStaticFieldGet(node, field);
  }

  @override
  T visitTopLevelFunctionGet(
      ast.Send node,
      MethodElement function,
      _) {
    return handleStaticFunctionGet(node, function);
  }

  @override
  T visitTopLevelGetterGet(
      ast.Send node,
      FunctionElement getter,
      _) {
    return handleStaticGetterGet(node, getter);
  }

  @override
  T visitUnresolvedGet(
      ast.Send node,
      Element element,
      _) {
    return types.dynamicType;
  }

  /// Handle .call invocation on [closure].
  T handleCallInvoke(ast.Send node, T closure) {
    ArgumentsTypes arguments = analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    return inferrer.registerCalledClosure(
        node, selector, closure, outermostElement, arguments,
        sideEffects, inLoop);
  }

  @override
  T visitExpressionInvoke(
      ast.Send node,
      ast.Node expression,
      ast.NodeList arguments,
      Selector selector,
      _) {
    return handleCallInvoke(node, expression.accept(this));
   }

  @override
  T visitThisInvoke(
      ast.Send node,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleCallInvoke(node, thisType);
  }

  @override
  T visitParameterInvoke(
      ast.Send node,
      ParameterElement parameter,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleCallInvoke(node, locals.use(parameter));
  }

  @override
  T visitLocalVariableInvoke(
      ast.Send node,
      LocalVariableElement variable,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    return handleCallInvoke(node, locals.use(variable));
  }

  @override
  T visitLocalFunctionInvoke(
      ast.Send node,
      LocalFunctionElement function,
      ast.NodeList arguments,
      CallStructure callStructure,
      _) {
    ArgumentsTypes argumentTypes = analyzeArguments(node.arguments);
    Selector selector = elements.getSelector(node);
    if (!selector.applies(function, compiler.world)) return types.dynamicType;
    // This only works for function statements. We need a
    // more sophisticated type system with function types to support
    // more.
    return inferrer.registerCalledElement(
        node, selector, outermostElement, function, argumentTypes,
        sideEffects, inLoop);
  }

  T handleStaticSend(ast.Node node,
                     Selector selector,
                     Element element,
                     ArgumentsTypes arguments) {
    assert(!element.isFactoryConstructor ||
           !(element as ConstructorElement).isRedirectingFactory);
    // Erroneous elements may be unresolved, for example missing getters.
    if (Elements.isUnresolved(element)) return types.dynamicType;
    // TODO(herhut): should we follow redirecting constructors here? We would
    // need to pay attention if the constructor is pointing to an erroneous
    // element.
    return inferrer.registerCalledElement(
        node, selector, outermostElement, element, arguments,
        sideEffects, inLoop);
  }

  T handleDynamicSend(ast.Node node,
                      Selector selector,
                      T receiverType,
                      ArgumentsTypes arguments) {
    assert(receiverType != null);
    if (types.selectorNeedsUpdate(receiverType, selector)) {
      selector = (receiverType == types.dynamicType)
          ? selector.asUntyped
          : types.newTypedSelector(receiverType, selector);
      inferrer.updateSelectorInTree(analyzedElement, node, selector);
    }

    // If the receiver of the call is a local, we may know more about
    // its type by refining it with the potential targets of the
    // calls.
    if (node.asSend() != null) {
      ast.Node receiver = node.asSend().receiver;
      if (receiver != null) {
        Element element = elements[receiver];
        if (Elements.isLocal(element) && !capturedVariables.contains(element)) {
          T refinedType = types.refineReceiver(selector, receiverType);
          locals.update(element, refinedType, node);
        }
      }
    }

    return inferrer.registerCalledSelector(
        node, selector, receiverType, outermostElement, arguments,
        sideEffects, inLoop);
  }

  T handleDynamicInvoke(ast.Send node) {
    return _handleDynamicSend(node);
  }

  T handleDynamicGet(ast.Send node) {
    return _handleDynamicSend(node);
  }

  T _handleDynamicSend(ast.Send node) {
    Element element = elements[node];
    T receiverType;
    bool isCallOnThis = false;
    if (node.receiver == null) {
      if (treatAsInstanceMember(element)) {
        isCallOnThis = true;
        receiverType = thisType;
      }
    } else {
      ast.Node receiver = node.receiver;
      isCallOnThis = isThisOrSuper(receiver);
      receiverType = visit(receiver);
    }

    Selector selector = elements.getSelector(node);
    if (!isThisExposed && isCallOnThis) {
      checkIfExposesThis(types.newTypedSelector(receiverType, selector));
    }

    ArgumentsTypes arguments = node.isPropertyAccess
        ? null
        : analyzeArguments(node.arguments);
    if (selector.name == '=='
        || selector.name == '!=') {
      if (types.isNull(receiverType)) {
        potentiallyAddNullCheck(node, node.arguments.head);
        return types.boolType;
      } else if (types.isNull(arguments.positional[0])) {
        potentiallyAddNullCheck(node, node.receiver);
        return types.boolType;
      }
    }
    return handleDynamicSend(node, selector, receiverType, arguments);
  }

  void recordReturnType(T type) {
    returnType = inferrer.addReturnTypeFor(analyzedElement, returnType, type);
  }

  T synthesizeForwardingCall(Spannable node, FunctionElement element) {
    element = element.implementation;
    FunctionElement function = analyzedElement;
    FunctionSignature signature = function.functionSignature;
    FunctionSignature calleeSignature = element.functionSignature;
    if (!calleeSignature.isCompatibleWith(signature)) {
      return types.nonNullEmpty();
    }

    List<T> unnamed = <T>[];
    signature.forEachRequiredParameter((ParameterElement element) {
      assert(locals.use(element) != null);
      unnamed.add(locals.use(element));
    });

    Map<String, T> named;
    if (signature.optionalParametersAreNamed) {
      named = new Map<String, T>();
      signature.forEachOptionalParameter((ParameterElement element) {
        named[element.name] = locals.use(element);
      });
    } else {
      signature.forEachOptionalParameter((ParameterElement element) {
        unnamed.add(locals.use(element));
      });
    }

    ArgumentsTypes arguments = new ArgumentsTypes<T>(unnamed, named);
    return inferrer.registerCalledElement(node,
                                          null,
                                          outermostElement,
                                          element,
                                          arguments,
                                          sideEffects,
                                          inLoop);
  }

  T visitRedirectingFactoryBody(ast.RedirectingFactoryBody node) {
    Element element = elements.getRedirectingTargetConstructor(node);
    if (Elements.isErroneous(element)) {
      recordReturnType(types.dynamicType);
    } else {
      // We don't create a selector for redirecting factories, and
      // the send is just a property access. Therefore we must
      // manually create the [ArgumentsTypes] of the call, and
      // manually register [analyzedElement] as a caller of [element].
      T mask = synthesizeForwardingCall(node.constructorReference, element);
      recordReturnType(mask);
    }
    locals.seenReturnOrThrow = true;
    return null;
  }

  T visitReturn(ast.Return node) {
    ast.Node expression = node.expression;
    recordReturnType(expression == null
        ? types.nullType
        : expression.accept(this));
    locals.seenReturnOrThrow = true;
    return null;
  }

  T handleForInLoop(ast.ForIn node, T iteratorType, Selector currentSelector,
                    Selector moveNextSelector) {
    handleDynamicSend(node, moveNextSelector, iteratorType,
                      new ArgumentsTypes<T>.empty());
    T currentType = handleDynamicSend(node, currentSelector, iteratorType,
                                      new ArgumentsTypes<T>.empty());

    if (node.expression.isThis()) {
      // Any reasonable implementation of an iterator would expose
      // this, so we play it safe and assume it will.
      isThisExposed = true;
    }

    ast.Node identifier = node.declaredIdentifier;
    Element element = elements.getForInVariable(node);
    Selector selector = elements.getSelector(identifier);

    T receiverType;
    if (element != null && element.isInstanceMember) {
      receiverType = thisType;
    } else {
      receiverType = types.dynamicType;
    }

    handlePlainAssignment(identifier, element, selector,
                          receiverType, currentType,
                          node.expression);
    return handleLoop(node, () {
      visit(node.body);
    });
  }

  T visitAsyncForIn(ast.AsyncForIn node) {
    T expressionType = visit(node.expression);

    Selector currentSelector = elements.getCurrentSelector(node);
    Selector moveNextSelector = elements.getMoveNextSelector(node);

    js.JavaScriptBackend backend = compiler.backend;
    Element ctor = backend.getStreamIteratorConstructor();

    /// Synthesize a call to the [StreamIterator] constructor.
    T iteratorType = handleStaticSend(node, null, ctor,
                                      new ArgumentsTypes<T>([expressionType],
                                                            null));

    return handleForInLoop(node, iteratorType, currentSelector,
                           moveNextSelector);
  }

  T visitSyncForIn(ast.SyncForIn node) {
    T expressionType = visit(node.expression);
    Selector iteratorSelector = elements.getIteratorSelector(node);
    Selector currentSelector = elements.getCurrentSelector(node);
    Selector moveNextSelector = elements.getMoveNextSelector(node);

    T iteratorType = handleDynamicSend(node, iteratorSelector, expressionType,
                                       new ArgumentsTypes<T>.empty());

    return handleForInLoop(node, iteratorType, currentSelector,
                           moveNextSelector);
  }
}
