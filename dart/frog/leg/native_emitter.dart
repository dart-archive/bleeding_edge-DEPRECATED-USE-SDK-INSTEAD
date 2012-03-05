// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class NativeEmitter {

  Compiler compiler;
  bool addedDynamicFunction = false;
  bool addedTypeNameOfFunction = false;
  bool addedDefPropFunction = false;
  bool addedNativeProperty = false;

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
  String buildDynamicFunctionCode() {
    ClassElement noSuchMethodException =
        compiler.coreLibrary.find(Compiler.NO_SUCH_METHOD_EXCEPTION);
    Element helper = compiler.findHelper(new SourceString('captureStackTrace'));
    String capture = compiler.namer.isolateAccess(helper);
    String exception = compiler.namer.isolateAccess(noSuchMethodException);

    return '''
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

    if (method == null) {
      method = function() {
        throw $capture(new $exception(obj, name, arguments));
      };
    }

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
}

  String buildDynamicMetadataCode() => '''
if (typeof $dynamicMetadataName == 'undefined') $dynamicMetadataName = [];''';

  // This method will be called for 'is' checks on native types.
  // It takes the object on which the 'is' check is being done, and the
  // property name for the type check. The method patches the real
  // prototype of the object with the value from the Dart object
  // (see [generateNativeClass]).
  String buildDynamicIsCheckCode() {
    ClassElement objectClass =
        compiler.coreLibrary.find(const SourceString('Object'));
    return '''
function(obj, isCheck) {
  if (obj.constructor === Array) return false;
  var proto = Object.getPrototypeOf(obj);
  // Check if the Dart object corresponding to this class has the property.
  var res =
      !!${compiler.namer.CURRENT_ISOLATE}.native[$typeNameOfName(obj)][isCheck];
  res = res || false;
  $defPropName(proto, isCheck, res);
  return res;
}''';
  }

  String buildNativePropertyCode() => '''
${compiler.namer.ISOLATE}.prototype.native = {};''';

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
  String get dynamicIsCheckName() =>
      '${compiler.namer.ISOLATE}.\$dynamicIsCheck';
  String get dynamicSetMetadataName() =>
      '${compiler.namer.ISOLATE}.\$dynamicSetMetatada';

  void addDynamicFunctionIfNecessary(StringBuffer buffer) {
    if (addedDynamicFunction) return;
    addedDynamicFunction = true;
    addTypeNameOfFunctionIfNecessary(buffer);
    buffer.add('$dynamicName = ');
    buffer.add(buildDynamicFunctionCode());
    buffer.add('\n');
    buffer.add(buildDynamicMetadataCode());
    buffer.add('\n');
  }

  void addTypeNameOfFunctionIfNecessary(StringBuffer buffer) {
    if (addedTypeNameOfFunction) return;
    addedTypeNameOfFunction = true;
    addDefPropFunctionIfNecessary(buffer);
    buffer.add('$typeNameOfName = ');
    buffer.add(TYPE_NAME_OF_FUNCTION);
    buffer.add('\n');
  }

  void addDefPropFunctionIfNecessary(StringBuffer buffer) {
    if (addedDefPropFunction) return;
    addedDefPropFunction = true;
    buffer.add('$defPropName = ');
    buffer.add(DEF_PROP_FUNCTION);
    buffer.add('\n');
  }

  void addNativePropertyIfNecessary(StringBuffer buffer) {
    if (addedNativeProperty) return;
    addedNativeProperty = true;
    buffer.add(buildNativePropertyCode());
    buffer.add('\n');
  }

  void generateNativeLiteral(ClassElement classElement, StringBuffer buffer) {
    String quotedNative = classElement.nativeName.slowToString();
    String nativeCode = quotedNative.substring(2, quotedNative.length - 1);
    String className = compiler.namer.getName(classElement);
    buffer.add(className);
    buffer.add(' = ');
    buffer.add(nativeCode);
    buffer.add(';\n');

    String attachTo(name) => "$className.$name";

    for (Element member in classElement.members) {
      if (member.isInstanceMember()) {
        compiler.emitter.addInstanceMember(
            member, attachTo, buffer, isNative: true);
      }
    }
  }

  bool isNativeLiteral(String nativeName) {
    return nativeName[1] === '=';
  }

  bool isNativeGlobal(String nativeName) {
    return nativeName[1] === '@';
  }

  void generateNativeClass(ClassElement classElement, StringBuffer buffer) {
    nativeClasses.add(classElement);

    assert(classElement.backendMembers.isEmpty());
    String nativeName = classElement.nativeName.slowToString();
    if (isNativeLiteral(nativeName)) {
      generateNativeLiteral(classElement, buffer);
      // The native literal kind needs to be dealt with specially when
      // generating code for it.
      return;
    }

    if (isNativeGlobal(nativeName)) {
      // Global object, just be like the other types for now.
      nativeName = nativeName.substring(3, nativeName.length - 1);
    } else {
      nativeName = nativeName.substring(2, nativeName.length - 1);
    }
    bool hasUsedSelectors = false;

    String attachTo(String name) {
      hasUsedSelectors = true;
      addDynamicFunctionIfNecessary(buffer);
      return "$dynamicName('$name').$nativeName";
    }

    for (Element member in classElement.members) {
      if (member.isInstanceMember()) {
        compiler.emitter.addInstanceMember(
            member, attachTo, buffer, isNative: true);
      }
    }

    addNativePropertyIfNecessary(buffer);
    // Create an object that contains the is checks properties. The
    // object will be used when entering [buildDynamicIsCheckCode].
    buffer.add('${compiler.namer.ISOLATE}.prototype.native.$nativeName = { ');
    List<String> tests = <String>[];

    ClassElement objectClass =
        compiler.coreLibrary.find(const SourceString('Object'));
    Element element = classElement;
    // We need to put the super class is checks too, since a check on
    // the subclass can happen before a check on the super class
    // (which does the patching on the prototype).
    do {
      compiler.emitter.generateTypeTests(element, (Element other) {
        tests.add("${compiler.namer.operatorIs(other)}:true");
      });
      element = element.superclass;
    } while (element !== objectClass);

    buffer.add('${Strings.join(tests, ",")}');
    buffer.add('};\n');

    if (hasUsedSelectors) classesWithDynamicDispatch.add(classElement);
  }

  void emitParameterStub(Element member,
                         String invocationName,
                         String stubParameters,
                         List<String> argumentsBuffer,
                         int indexOfLastOptionalArgumentInParameters,
                         StringBuffer buffer) {
    // If the method is native, we must check if the prototype of
    // 'this' has the method available. Otherwise, we may end up
    // calling the method from the super class. If the method is not
    // available, we make a direct call to
    // Object.prototype.$invocationName. This method will patch the
    // prototype of 'this' to the real method.
    // TODO(ngeoffray): We can avoid this if we know the class of this
    // method does not have subclasses.

    // The target JS function may check arguments.length so we need to
    // make sure not to pass any unspecified optional arguments to it.
    // For example, for the following Dart method:
    //   foo([x, y, z]);
    // The call:
    //   foo(y: 1)
    // must be turned into a JS call to:
    //   foo(null, y).

    List<String> nativeArgumentsBuffer = argumentsBuffer.getRange(
        0, indexOfLastOptionalArgumentInParameters + 1);

    ClassElement classElement = member.enclosingElement;
    String nativeName = classElement.nativeName.slowToString();
    String nativeArguments = Strings.join(nativeArgumentsBuffer, ",");

    if (isNativeLiteral(nativeName)) {
      // If it's the native literal, we know there is no subclass, so
      // we can call the method directly.
      buffer.add('    return this.${member.name.slowToString()}');
      buffer.add('($nativeArguments)');
      return;
    }

    buffer.add('  if (Object.getPrototypeOf(this).hasOwnProperty(');
    buffer.add("'$invocationName')) {\n");
    buffer.add('    return this.${member.name.slowToString()}');
    buffer.add('($nativeArguments)');
    buffer.add('\n  }\n');
    buffer.add('  return Object.prototype.$invocationName.call(this');
    buffer.add(stubParameters == '' ? '' : ', $stubParameters');
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
    // TODO(ngeoffray): emit this conditionally.
    addTypeNameOfFunctionIfNecessary(buffer);
    buffer.add('$dynamicIsCheckName = ');
    buffer.add(buildDynamicIsCheckCode());
    buffer.add('\n');

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
    Map<String, String> varDefns = <String>{};
    // tag -> expression (a string or a variable)
    Map<String, String> tagDefns = <String>{};

    String makeExpression(ClassElement cls) {
      // Expression fragments for this set of cls keys.
      List<String> expressions = <String>[];
      // TODO: Remove if cls is abstract.
      List<String> subtags = [cls.name.slowToString()];
      void walk(ClassElement cls) {
        for (final ClassElement subclass in computeDirectSubclasses(cls)) {
          String tag = subclass.name.slowToString();
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
      tagDefns[cls.name.slowToString()] = makeExpression(cls);
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
        String clsName = cls.name.slowToString();
        entries.add("\n    ['$clsName', ${tagDefns[clsName]}]");
      }
      buffer.add(Strings.join(entries, ','));
      buffer.add('];\n');
      buffer.add('$dynamicSetMetadataName(table);\n');

      buffer.add('})();\n');
    }
  }
}
