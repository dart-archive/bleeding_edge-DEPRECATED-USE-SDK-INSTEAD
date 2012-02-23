// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class NativeEmitter {

  Compiler compiler;
  bool addedDynamicFunction = false;

  NativeEmitter(this.compiler);

  /**
   * Code for finding the type name of a JavaScript object.
   */
  static final String TYPE_NAME_OF_FUNCTION = '''
function(obj) {
  var constructor = obj.constructor;
  if (typeof(constructor) == 'function') {
    // The constructor isn't null or undefined at this point. Try
    // to grab hold of its name.
    var name = constructor.name;
    // If the name is a non-empty string, we use that as the type
    // name of this object. On Firefox, we often get 'Object' as
    // the constructor name even for more specialized objects so
    // we have to fall through to the toString() based implementation
    // below in that case.
    if (name && typeof(name) == 'string' && name != 'Object') return name;
  }
  var string = Object.prototype.toString.call(obj);
  var name = string.substring(8, string.length - 1);
  if (name == 'Window') {
    name = 'DOMWindow';
  } else if (name == 'Document') {
    name = 'HTMLDocument';
  }
  return name;
}''';

  /**
   * Code for defining a property in a JavaScript object that will not
   * be visible through for-in (aka enumerable is false).
   */
  static final String DEF_PROP_FUNCTION = '''
function(obj, prop, value) {
  Object.defineProperty(obj, prop,
      {value: value, enumerable: false, writable: true, configurable: true});
}''';

  /**
   * Code for doing the dynamic dispatch on JavaScript prototypes that are not
   * available at compile-time. Each property of a native Dart class
   * is registered through this function, which is called with the
   * following pattern:
   *
   * $dynamic('propertyName').prototypeName = // JS code
   *
   * What this function does is:
   * - Creates a map of { prototypeName: JS code }.
   * - Attaches 'propertyName' to the JS Object prototype that will
   *   intercept at runtime all calls to propertyName.
   * - Sets the value of 'propertyName' to a function that queries the
   *   map with the prototype of 'this', patches the prototype of
   *   'this' with the found JS code, and invokes the JS code.
   *
   */
  String buildDynamicFunctionCode() => '''
function(name) {
  var f = Object.prototype[name];
  if (f && f.methods) return f.methods;

  var methods = {};
  // If there is a method attached to the Dart Object class, use it as
  // the method to call in case no method is registered for that type.
  var dartMethod = ${compiler.emitter.objectClassName}.prototype[name];
  if (dartMethod) methods.Object = dartMethod;
  function dynamicBind() {
    // Find the target method
    var obj = this;
    var tag = $typeNameOfName(obj);
    var method = methods[tag];
    if (!method) {
      var table = $dynamicMetadataName;
      for (var i = 0; i < table.length; i++) {
        var entry = table[i];
        if (entry.map.hasOwnProperty(tag)) {
          method = methods[entry.tag];
          if (method) break;
        }
      }
    }
    method = method || methods.Object;
    var proto = Object.getPrototypeOf(obj);
    if (!proto.hasOwnProperty(name)) {
      $defPropName(proto, name, method);
    }

    return method.apply(this, Array.prototype.slice.call(arguments));
  };
  dynamicBind.methods = methods;
  $defPropName(Object.prototype, name, dynamicBind);
  return methods;
}''';

  String buildDynamicMetadataCode() => '''
if (typeof $dynamicMetadataName == 'undefined') $dynamicMetadataName = [];''';


  String get dynamicName() => '${compiler.namer.ISOLATE}.\$dynamic';
  String get defPropName() => '${compiler.namer.ISOLATE}.\$defProp';
  String get typeNameOfName() => '${compiler.namer.ISOLATE}.\$typeNameOf';
  String get dynamicMetadataName() =>
      '${compiler.namer.ISOLATE}.\$dynamicMetatada';

  void addDynamicFunctionIfNecessary(StringBuffer buffer) {
    if (addedDynamicFunction) return;
    addedDynamicFunction = true;
    buffer.add('$defPropName = ');
    buffer.add(DEF_PROP_FUNCTION);
    buffer.add('\n');
    buffer.add('$typeNameOfName = ');
    buffer.add(TYPE_NAME_OF_FUNCTION);
    buffer.add('\n');
    buffer.add('$dynamicName = ');
    buffer.add(buildDynamicFunctionCode());
    buffer.add('\n');
    buffer.add(buildDynamicMetadataCode());
    buffer.add('\n');
  }

  void generateNativeClass(ClassElement classElement, StringBuffer buffer) {
    addDynamicFunctionIfNecessary(buffer);
    assert(classElement.backendMembers.isEmpty());
    String nativeName = classElement.nativeName.stringValue;
    nativeName = nativeName.substring(2, nativeName.length - 1);
    for (Element member in classElement.members) {
      if (member.isInstanceMember()) {
        String memberName = compiler.namer.getName(member);
        if (member.kind === ElementKind.FUNCTION
            || member.kind === ElementKind.GENERATIVE_CONSTRUCTOR_BODY
            || member.kind === ElementKind.GETTER
            || member.kind === ElementKind.SETTER) {
          String codeBlock = compiler.universe.generatedCode[member];
          if (codeBlock == null) continue;
          buffer.add(
              "$dynamicName('$memberName').$nativeName = $codeBlock;\n");
          codeBlock = compiler.universe.generatedBailoutCode[member];
          if (codeBlock !== null) {
            String name = compiler.namer.getBailoutName(member);
            buffer.add("$dynamicName('$name').$nativeName = $codeBlock;\n");
          }
          FunctionElement function = member;
          FunctionParameters parameters = function.computeParameters(compiler);
          if (!parameters.optionalParameters.isEmpty()) {
            compiler.emitter.addParameterStubs(
                member, (name) => "$dynamicName('$name').$nativeName", buffer,
                isNative: true);
          }
        } else if (member.kind === ElementKind.FIELD) {
          if (compiler.universe.invokedSetters.contains(member.name)) {
            String setterName = compiler.namer.setterName(member.name);
            buffer.add(
              "$dynamicName('$setterName').$nativeName = function(v){\n" +
              '  this.${member.name} = v;\n};\n');
          }
          if (compiler.universe.invokedGetters.contains(member.name)) {
            String getterName = compiler.namer.getterName(member.name);
            buffer.add(
              "$dynamicName('$getterName').$nativeName = function(){\n" +
              '  return this.${member.name};\n};\n');
          }
        } else {
          compiler.internalError('unexpected kind: "${member.kind}"',
                                 element: member);
        }
      }
    }
  }

  void emitParameterStub(Element member,
                         String invocationName,
                         String arguments,
                         StringBuffer buffer) {
    // If the method is native, we must check if the prototype of
    // 'this' has the method available. Otherwise, we may end up
    // calling the method from the super class. If the method is not
    // available, we make a direct call to
    // Object.prototype.$invocationName. This method will patch the
    // prototype of 'this' to the real method.
    // TODO(ngeoffray): We can avoid this if we know the class of this
    // method does not have subclasses.
    buffer.add('  if (Object.getPrototypeOf(this).hasOwnProperty(');
    buffer.add("'$invocationName')) {\n");
    buffer.add('    return this.${compiler.namer.getName(member)}($arguments)');
    buffer.add('\n  }\n');
    buffer.add('  return Object.prototype.$invocationName.call(this');
    buffer.add(arguments == '' ? '' : ', $arguments');
    buffer.add(');');
  }
}
