// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart2js.new_js_emitter.model;

import '../js/js.dart' as js show Expression, Statement;
import '../constants/values.dart' show ConstantValue;

import '../deferred_load.dart' show OutputUnit;

import 'js_emitter.dart' show MetadataCollector;

import '../common.dart';

class Program {
  final List<Fragment> fragments;
  final List<Holder> holders;
  final bool outputContainsConstantList;
  final bool outputContainsNativeClasses;
  final bool hasIsolateSupport;
  /// A map from load id to the list of fragments that need to be loaded.
  final Map<String, List<Fragment>> loadMap;

  // If this field is not `null` then its value must be emitted in the embedded
  // global `TYPE_TO_INTERCEPTOR_MAP`. The map references constants and classes.
  final js.Expression typeToInterceptorMap;

  // TODO(floitsch): we should store the metadata directly instead of storing
  // the collector. However, the old emitter still updates the data.
  final MetadataCollector _metadataCollector;

  Program(this.fragments,
          this.holders,
          this.loadMap,
          this.typeToInterceptorMap,
          this._metadataCollector,
          {this.outputContainsNativeClasses,
           this.outputContainsConstantList,
           this.hasIsolateSupport}) {
    assert(outputContainsNativeClasses != null);
    assert(outputContainsConstantList != null);
    assert(hasIsolateSupport != null);
  }

  /// A list of pretty-printed JavaScript expressions.
  ///
  /// This list must be emitted in the `METADATA` embedded global.
  /// The list references constants and must hence be emitted after constants
  /// have been initialized.
  ///
  /// Note: the metadata is derived from the task's `metadataCollector`. The
  /// list must not be emitted before all operations on it are done. For
  /// example, the old emitter generates metadata when emitting reflection
  /// data.
  List<String> get metadata => _metadataCollector.globalMetadata;

  bool get isSplit => fragments.length > 1;
  Iterable<Fragment> get deferredFragments => fragments.skip(1);
}

/**
 * This class represents a JavaScript object that contains static state, like
 * classes or functions.
 */
class Holder {
  final String name;
  final int index;
  Holder(this.name, this.index);
}

/**
 * This class represents one output file.
 *
 * If no library is deferred, there is only one [Fragment] of type
 * [MainFragment].
 */
abstract class Fragment {
  /// The outputUnit should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final OutputUnit outputUnit;

  final List<Library> libraries;
  final List<Constant> constants;
  // TODO(floitsch): should we move static fields into libraries or classes?
  final List<StaticField> staticNonFinalFields;
  // TODO(floitsch): lazy fields should be in their library or even class.
  final List<StaticField> staticLazilyInitializedFields;

  /// Output file name without extension.
  final String outputFileName;

  Fragment(this.outputUnit,
           this.outputFileName,
           this.libraries,
           this.staticNonFinalFields,
           this.staticLazilyInitializedFields,
           this.constants);

  bool get isMainFragment;
}

/**
 * The main output file.
 *
 * This code emitted from this [Fragment] must be loaded first. It can then load
 * other [DeferredFragment]s.
 */
class MainFragment extends Fragment {
  final js.Statement invokeMain;

  MainFragment(OutputUnit outputUnit,
               String outputFileName,
               this.invokeMain,
               List<Library> libraries,
               List<StaticField> staticNonFinalFields,
               List<StaticField> staticLazilyInitializedFields,
               List<Constant> constants)
      : super(outputUnit,
              outputFileName,
              libraries,
              staticNonFinalFields,
              staticLazilyInitializedFields,
              constants);

  bool get isMainFragment => true;
}

/**
 * An output (file) for deferred code.
 */
class DeferredFragment extends Fragment {
  final String name;

  DeferredFragment(OutputUnit outputUnit,
                   String outputFileName,
                   this.name,
                   List<Library> libraries,
                   List<StaticField> staticNonFinalFields,
                   List<StaticField> staticLazilyInitializedFields,
                   List<Constant> constants)
      : super(outputUnit,
              outputFileName,
              libraries,
              staticNonFinalFields,
              staticLazilyInitializedFields,
              constants);

  bool get isMainFragment => false;
}

class Constant {
  final String name;
  final Holder holder;
  final ConstantValue value;

  Constant(this.name, this.holder, this.value);
}

abstract class FieldContainer {
  List<Field> get staticFieldsForReflection;
}

class Library implements FieldContainer {
  /// The element should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final Element element;

  final String uri;
  final List<StaticMethod> statics;
  final List<Class> classes;

  final List<Field> staticFieldsForReflection;

  Library(this.element, this.uri, this.statics, this.classes,
          this.staticFieldsForReflection);
}

class StaticField {
  /// The element should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final Element element;

  final String name;
  // TODO(floitsch): the holder for static fields is the isolate object. We
  // could remove this field and use the isolate object directly.
  final Holder holder;
  final js.Expression code;
  final bool isFinal;
  final bool isLazy;

  StaticField(this.element,
              this.name, this.holder, this.code,
              this.isFinal, this.isLazy);
}

class Class implements FieldContainer {
  /// The element should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final Element element;

  final String name;
  final Holder holder;
  Class _superclass;
  final List<Method> methods;
  final List<Field> fields;
  final List<StubMethod> isChecks;

  /// Stub methods for this class that are call stubs for getters.
  final List<StubMethod> callStubs;

  /// Stub methods for this class handling reads to type variables.
  final List<StubMethod> typeVariableReaderStubs;

  /// noSuchMethod stubs in the special case that the class is Object.
  final List<StubMethod> noSuchMethodStubs;
  final List<Field> staticFieldsForReflection;
  final bool onlyForRti;
  final bool isDirectlyInstantiated;
  final bool isNative;

  // If the class implements a function type, and the type is encoded in the
  // metatada table, then this field contains the index into that field.
  final int functionTypeIndex;

  /// Whether the class must be evaluated eagerly.
  bool isEager = false;

  /// Data that must be emitted with the class for native interop.
  String nativeInfo;

  Class(this.element, this.name, this.holder,
        this.methods,
        this.fields,
        this.staticFieldsForReflection,
        this.callStubs,
        this.typeVariableReaderStubs,
        this.noSuchMethodStubs,
        this.isChecks,
        this.functionTypeIndex,
        {this.onlyForRti,
         this.isDirectlyInstantiated,
         this.isNative}) {
    assert(onlyForRti != null);
    assert(isDirectlyInstantiated != null);
    assert(isNative != null);
  }

  bool get isMixinApplication => false;
  Class get superclass => _superclass;

  void setSuperclass(Class superclass) {
    _superclass = superclass;
  }

  String get superclassName
      => (superclass == null) ? "" : superclass.name;
  int get superclassHolderIndex
      => (superclass == null) ? 0 : superclass.holder.index;
}

class MixinApplication extends Class {
  Class _mixinClass;

  MixinApplication(Element element, String name, Holder holder,
                   List<Field> instanceFields,
                   List<Field> staticFieldsForReflection,
                   List<StubMethod> callStubs,
                   List<StubMethod> typeVariableReaderStubs,
                   List<StubMethod> isChecks,
                   int functionTypeIndex,
                   {bool onlyForRti,
                    bool isDirectlyInstantiated})
      : super(element,
              name, holder,
              const <Method>[],
              instanceFields,
              staticFieldsForReflection,
              callStubs,
              typeVariableReaderStubs,
              const <StubMethod>[],
              isChecks, functionTypeIndex,
              onlyForRti: onlyForRti,
              isDirectlyInstantiated: isDirectlyInstantiated,
              isNative: false);

  bool get isMixinApplication => true;
  Class get mixinClass => _mixinClass;

  void setMixinClass(Class mixinClass) {
    _mixinClass = mixinClass;
  }
}

/// A field.
///
/// In general represents an instance field, but for reflection may also
/// represent static fields.
class Field {
  /// The element should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final Element element;

  final String name;
  final String accessorName;

  /// 00: Does not need any getter.
  /// 01:  function() { return this.field; }
  /// 10:  function(receiver) { return receiver.field; }
  /// 11:  function(receiver) { return this.field; }
  final int getterFlags;

  /// 00: Does not need any setter.
  /// 01:  function(value) { this.field = value; }
  /// 10:  function(receiver, value) { receiver.field = value; }
  /// 11:  function(receiver, value) { this.field = value; }
  final int setterFlags;

  final bool needsCheckedSetter;

  // TODO(floitsch): support renamed fields.
  Field(this.element, this.name, this.accessorName,
        this.getterFlags, this.setterFlags,
        this.needsCheckedSetter);

  bool get needsGetter => getterFlags != 0;
  bool get needsUncheckedSetter => setterFlags != 0;

  bool get needsInterceptedGetter => getterFlags > 1;
  bool get needsInterceptedSetter => setterFlags > 1;
}

abstract class Method {
  /// The element should only be used during the transition to the new model.
  /// Uses indicate missing information in the model.
  final Element element;
  final String name;
  final js.Expression code;

  Method(this.element, this.name, this.code);
}

/// A method that corresponds to a method in the original Dart program.
class DartMethod extends Method {
  final bool needsTearOff;
  final String tearOffName;
  final List<ParameterStubMethod> parameterStubs;
  final bool canBeApplied;
  final bool canBeReflected;

  // Is non-null if [needsTearOff] or [canBeReflected].
  //
  // If the type is encoded in the metadata table this field contains an index
  // into the table. Otherwise the type contains type variables in which case
  // this field holds a function computing the function signature.
  final js.Expression functionType;

  // Signature information for this method. This is only required and stored
  // here if the method [canBeApplied] or [canBeReflected]
  final int requiredParameterCount;
  final /* Map | List */ optionalParameterDefaultValues;

  // If this method can be torn off, contains the name of the corresponding
  // call method. For example, for the member `foo$1$name` it would be
  // `call$1$name` (in unminified mode).
  final String callName;

  DartMethod(Element element, String name, js.Expression code,
             this.parameterStubs, this.callName,
             {this.needsTearOff, this.tearOffName, this.canBeApplied,
              this.canBeReflected, this.requiredParameterCount,
              this.optionalParameterDefaultValues, this.functionType})
      : super(element, name, code) {
    assert(needsTearOff != null);
    assert(!needsTearOff || tearOffName != null);
    assert(canBeApplied != null);
    assert(canBeReflected != null);
    assert((!canBeReflected && !canBeApplied) ||
           (requiredParameterCount != null &&
            optionalParameterDefaultValues != null));
  }
}

class InstanceMethod extends DartMethod {
  /// An alternative name for this method. This is used to model calls to
  /// a method via `super`. If [aliasName] is non-null, the emitter has to
  /// ensure that this method is registered on the prototype under both [name]
  /// and [aliasName].
  final String aliasName;
  final bool isClosure;


  InstanceMethod(Element element, String name, js.Expression code,
                 List<ParameterStubMethod> parameterStubs,
                 String callName,
                 {bool needsTearOff,
                  String tearOffName,
                  this.aliasName,
                  bool canBeApplied,
                  bool canBeReflected,
                  int requiredParameterCount,
                  /* List | Map */ optionalParameterDefaultValues,
                  this.isClosure,
                  js.Expression functionType})
      : super(element, name, code, parameterStubs, callName,
              needsTearOff: needsTearOff,
              tearOffName: tearOffName,
              canBeApplied: canBeApplied,
              canBeReflected: canBeReflected,
              requiredParameterCount: requiredParameterCount,
              optionalParameterDefaultValues: optionalParameterDefaultValues,
              functionType: functionType) {
    assert(isClosure != null);
  }
}

/// A method that is generated by the backend and has not direct correspondence
/// to a method in the original Dart program. Examples are getter and setter
/// stubs and stubs to dispatch calls to methods with optional parameters.
class StubMethod extends Method {
  StubMethod(String name, js.Expression code,
             {Element element})
      : super(element, name, code);
}

/// A stub that adapts and redirects to the main method (the one containing)
/// the actual code.
///
/// For example, given a method `foo$2(x, [y: 499])` a possible parameter
/// stub-method could be `foo$1(x) => foo$2(x, 499)`.
///
/// ParameterStubMethods are always attached to (static or instance) methods.
class ParameterStubMethod extends StubMethod {
  /// The `call` name of this stub.
  ///
  /// When an instance method is torn off, it is invoked as a `call` member and
  /// not it's original name anymore. The [callName] provides the stub's
  /// name when it is used this way.
  ///
  /// If a stub's member can not be torn off, the [callName] is `null`.
  String callName;

  ParameterStubMethod(String name, this.callName, js.Expression code)
      : super(name, code);
}

abstract class StaticMethod implements Method {
  Holder get holder;
}

class StaticDartMethod extends DartMethod implements StaticMethod {
  final Holder holder;

  StaticDartMethod(Element element, String name, this.holder,
                   js.Expression code, List<ParameterStubMethod> parameterStubs,
                   String callName,
                   {bool needsTearOff, String tearOffName, bool canBeApplied,
                    bool canBeReflected, int requiredParameterCount,
                    /* List | Map */ optionalParameterDefaultValues,
                    js.Expression functionType})
      : super(element, name, code, parameterStubs, callName,
              needsTearOff: needsTearOff,
              tearOffName : tearOffName,
              canBeApplied : canBeApplied,
              canBeReflected : canBeReflected,
              requiredParameterCount: requiredParameterCount,
              optionalParameterDefaultValues: optionalParameterDefaultValues,
              functionType: functionType);
}

class StaticStubMethod extends StubMethod implements StaticMethod {
  Holder holder;
  StaticStubMethod(String name, this.holder, js.Expression code)
      : super(name, code);
}
