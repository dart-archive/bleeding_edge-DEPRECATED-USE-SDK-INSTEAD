// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class NativeEmitter {

  Compiler compiler;
  bool addedDynamicFunction = false;

  // Classes that participate in dynamic dispatch. These are the
  // classes that contain used members.
  Set<ClassElement> classesWithDynamicDispatch;

  // Native classes found in the application.
  Set<ClassElement> nativeClasses;

  // Caches the direct subclasses of a class.
  Map<ClassElement, List<ClassElement>> subclasses;

  NativeEmitter(this.compiler)
      : classesWithDynamicDispatch = new Set<ClassElement>(),
        nativeClasses = new Set<ClassElement>(),
        subclasses = new Map<ClassElement, List<ClassElement>>();

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


  String buildDynamicSetMetadataCode() => """
function(inputTable) {
  // TODO: Deal with light isolates.
  var table = [];
  for (var i = 0; i < inputTable.length; i++) {
    var tag = inputTable[i][0];
    var tags = inputTable[i][1];
    var map = {};
    var tagNames = tags.split('|');
    for (var j = 0; j < tagNames.length; j++) {
      map[tagNames[j]] = true;
    }
    table.push({tag: tag, tags: tags, map: map});
  }
  $dynamicMetadataName = table;
}""";


  String get dynamicName() => '${compiler.namer.ISOLATE}.\$dynamic';
  String get defPropName() => '${compiler.namer.ISOLATE}.\$defProp';
  String get typeNameOfName() => '${compiler.namer.ISOLATE}.\$typeNameOf';
  String get dynamicMetadataName() =>
      '${compiler.namer.ISOLATE}.\$dynamicMetatada';
  String get dynamicSetMetadataName() =>
      '${compiler.namer.ISOLATE}.\$dynamicSetMetatada';

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
    nativeClasses.add(classElement);
    if (classElement.members.isEmpty()) return;

    assert(classElement.backendMembers.isEmpty());
    String nativeName = classElement.nativeName.stringValue;
    nativeName = nativeName.substring(2, nativeName.length - 1);
    bool hasUsedSelectors = false;

    for (Element member in classElement.members) {
      if (member.isInstanceMember()) {
        String memberName = compiler.namer.getName(member);
        if (member.kind === ElementKind.FUNCTION
            || member.kind === ElementKind.GENERATIVE_CONSTRUCTOR_BODY
            || member.kind === ElementKind.GETTER
            || member.kind === ElementKind.SETTER) {
          String codeBlock = compiler.universe.generatedCode[member];
          if (codeBlock == null) continue;
          addDynamicFunctionIfNecessary(buffer);
          hasUsedSelectors = true;
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
            addDynamicFunctionIfNecessary(buffer);
            hasUsedSelectors = true;
            String setterName = compiler.namer.setterName(member.name);
            buffer.add(
              "$dynamicName('$setterName').$nativeName = function(v){\n" +
              '  this.${member.name} = v;\n};\n');
          }
          if (compiler.universe.invokedGetters.contains(member.name)) {
            addDynamicFunctionIfNecessary(buffer);
            hasUsedSelectors = true;
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
    if (hasUsedSelectors) classesWithDynamicDispatch.add(classElement);
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

  // TODO(ngeoffray): Temporary solution to find all subclasses until
  // it is done in leg.
  Collection<ClassElement> computeDirectSubclasses(ClassElement element) {
    return subclasses.putIfAbsent(element, () {
        List<ClassElement> result = <ClassElement>[];
        for (ClassElement other in nativeClasses) {
          if (other.superclass == element) result.add(other);
        }
        return result;
      }
    );
  }

  void emitDynamicDispatchMetadata(StringBuffer buffer) {
    if (classesWithDynamicDispatch.isEmpty()) return;
    buffer.add('// ${classesWithDynamicDispatch.length} dynamic classes.\n');

    // Build a pre-order traversal over all the classes and their subclasses.
    Set<ClassElement> seen = new Set<ClassElement>();
    List<ClassElement> classes = <ClassElement>[];
    void visit(ClassElement cls) {
      if (seen.contains(cls)) return;
      seen.add(cls);
      for (final ClassElement subclass in computeDirectSubclasses(cls)) {
        visit(subclass);
      }
      classes.add(cls);
    }
    for (final ClassElement cls in classesWithDynamicDispatch) {
      visit(cls);
    }

    Collection<ClassElement> dispatchClasses = classes.filter(
        (cls) => !computeDirectSubclasses(cls).isEmpty() &&
                  classesWithDynamicDispatch.contains(cls));

    buffer.add('// ${classes.length} classes\n');
    Collection<ClassElement> classesThatHaveSubclasses = classes.filter(
        (ClassElement t) => !computeDirectSubclasses(t).isEmpty());
    buffer.add('// ${classesThatHaveSubclasses.length} !leaf\n');

    // Generate code that builds the map from cls tags used in dynamic dispatch
    // to the set of cls tags of classes that extend (TODO: or implement) those
    // classes.  The set is represented as a string of tags joined with '|'.
    // This is easily split into an array of tags, or converted into a regexp.
    //
    // To reduce the size of the sets, subsets are CSE-ed out into variables.
    // The sets could be much smaller if we could make assumptions about the
    // cls tags of other classes (which are constructor names or part of the
    // result of Object.protocls.toString).  For example, if objects that are
    // Dart objects could be easily excluded, then we might be able to simplify
    // the test, replacing dozens of HTMLxxxElement classes with the regexp
    // /HTML.*Element/.

    // Temporary variables for common substrings.
    List<String> varNames = <String>[];
    // var -> expression
    Map<String, String> varDefns = <String, String>{};
    // tag -> expression (a string or a variable)
    Map<String, String> tagDefns = <String, String>{};

    String makeExpression(ClassElement cls) {
      // Expression fragments for this set of cls keys.
      List<String> expressions = <String>[];
      // TODO: Remove if cls is abstract.
      List<String> subtags = ['${cls.name}'];
      void walk(ClassElement cls) {
        for (final ClassElement subclass in computeDirectSubclasses(cls)) {
          String tag = '${subclass.name}';
          String existing = tagDefns[tag];
          if (existing == null) {
            subtags.add(tag);
            walk(subclass);
          } else {
            if (varDefns.containsKey(existing)) {
              expressions.add(existing);
            } else {
              String varName = 'v${varNames.length}/*${tag}*/';
              varNames.add(varName);
              varDefns[varName] = existing;
              tagDefns[tag] = varName;
              expressions.add(varName);
            }
          }
        }
      }
      walk(cls);
      String constantPart = "'${Strings.join(subtags, '|')}'";
      if (constantPart != "''") expressions.add(constantPart);
      String expression;
      if (expressions.length == 1) {
        expression = expressions[0];
      } else {
        expression = "[${Strings.join(expressions, ',')}].join('|')";
      }
      return expression;
    }

    for (final ClassElement cls in dispatchClasses) {
      tagDefns['${cls.name}'] = makeExpression(cls);
    }

    // Write out a thunk that builds the metadata.

    if (!tagDefns.isEmpty()) {
      buffer.add('$dynamicSetMetadataName = ');
      buffer.add(buildDynamicSetMetadataCode());
      buffer.add(';\n\n');

      buffer.add('(function(){\n');

      for (final String varName in varNames) {
        buffer.add('  var ${varName} = ${varDefns[varName]};\n');
      }

      buffer.add('  var table = [\n');
      buffer.add(
          '    // [dynamic-dispatch-tag, '
          + 'tags of classes implementing dynamic-dispatch-tag]');
      bool needsComma = false;
      List<String> entries = <String>[];
      for (final ClassElement cls in dispatchClasses) {
        entries.add(
            "\n    ['${cls.name}', ${tagDefns[cls.name.toString()]}]");
      }
      buffer.add(Strings.join(entries, ','));
      buffer.add('];\n');
      buffer.add('$dynamicSetMetadataName(table);\n');

      buffer.add('})();\n');
    }
  }
}
