// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.


#import('file_system.dart');
#import('lang.dart');

interface JsEvaluator {
  var eval(String source);
}

class Evaluator {
  JsEvaluator _jsEvaluator;
  static Set<String> _marked;
  static String _prelude;
  Library _lib;

  static void initWorld(String homedir, List<String> args, FileSystem files) {
    parseOptions(homedir, args, files);
    initializeWorld(files);
    world.process();
    world.resolveAll();

    _marked = new Set();

    world.gen = new WorldGenerator(null, new CodeWriter());
    _markAllUsed(world.corelib);
    world.gen.writeTypes(world.coreimpl);
    world.gen.writeTypes(world.corelib);
    _prelude = world.gen.writer.text;

    // Set these here so that we can compile the corelib without its errors
    // killing us
    options.throwOnErrors = true;
    options.throwOnFatal = true;
  }

  // TODO(jimhug): Should be calling world.genMethod - but I'm scared to
  //   make the change myself because we don't have any test coverage here.
  static void _markMethodUsed(Member m) {
    if (m == null || m.isGenerated || m.definition == null || m.isAbstract) {
      return;
    }
    new MethodGenerator(m, null).run();
  }

  // TODO(nweiz): use this logic for the --compile_all flag
  static void _markAllUsed(Library l) {
    if (_marked.contains(l.name)) return;
    _marked.add(l.name);

    l.imports.forEach((i) => _markAllUsed(i.library));
    for (var type in l.types.getValues()) {
      if (!type.isClass) return;

      type.markUsed();
      for (var member in type.members.getValues()) {
        if (member is PropertyMember) {
          _markMethodUsed(member.getter);
          _markMethodUsed(member.setter);
        }

        if (member.isMethod) _markMethodUsed(member);
      }
    }
  }

  _removeMember(String name) {
    _lib.topType._resolvedMembers.remove(name);
    _lib.topType.members.remove(name);
    // Don't rely on the existing member's jsname, because the existing member
    // may be null but the jsname may still be defined. This can happen if
    // multiple Evaluators are instantiated against the same world.
    var jsname = '${_lib.jsname}_$name';
    world._topNames.remove(name);
    world._topNames.remove(jsname);
  }

  Evaluator(JsEvaluator this._jsEvaluator) {
    if (_marked == null) {
      throw new UnsupportedOperationException(
          "Must call Evaluator.initWorld before creating a Evaluator.");
    }
    this._jsEvaluator.eval(_prelude);
    _lib = new Library(new SourceFile("_ifrog_", ""));
    _lib.imports.add(new LibraryImport(world.corelib));
    _lib.resolve();
  }

  void _ensureVariableDefined(Identifier name, [List<Token> modifiers = [],
      TypeReference type = null]) {
    var member = _lib.topType.getMember(name.name);
    if (member is FieldMember || member is PropertyMember) return;
    _removeMember(name.name);
    _lib.topType.addField(
        new VariableDefinition(modifiers, type, [name], [null], name.span));
    _lib.topType.getMember(name.name).resolve(_lib.topType);
  }

  var eval(String dart) {
    var source = new SourceFile("_ifrog_", dart);
    world.gen.writer = new CodeWriter();

    var code;
    var parsed = new Parser(source, throwOnIncomplete: true,
        optionalSemicolons: true).evalUnit();
    var method = new MethodMember("_ifrog_dummy", _lib.topType, null);
    var methGen = new MethodGenerator(method, null);

    if (parsed is ExpressionStatement) {
      var body = parsed.body;
      // Auto-declare variables that haven't been declared yet, so users can
      // write "a = 1" rather than "var a = 1"
      if (body is BinaryExpression && body.op.kind == TokenKind.ASSIGN &&
          body.x is VarExpression) {
        _ensureVariableDefined(body.x.dynamic.name);
      }
      code = body.visit(methGen).code;

    } else if (parsed is VariableDefinition) {
      var assignments = <Statement>[];
      zip(parsed.names, parsed.values, (name, value) {
        _ensureVariableDefined(name, parsed.modifiers, parsed.type);
        var expr = new BinaryExpression(
            new Token.fake(TokenKind.ASSIGN, parsed.span),
            new VarExpression(name, name.span), value, parsed.span);
        new ExpressionStatement(expr, parsed.span).visit(methGen);
      });
      code = methGen.writer.text;

    } else if (parsed is FunctionDefinition) {
      var methodName = parsed.name.name;
      _removeMember(methodName);
      _lib.topType.addMethod(methodName, parsed);
      MethodMember definedMethod = _lib.topType.getMember(methodName);
      definedMethod.resolve(_lib.topType);
      var definedMethGen = new MethodGenerator(definedMethod, null);
      definedMethGen.run();
      definedMethGen.writeDefinition(world.gen.writer, null);
      code = world.gen.writer.text;
    } else if (parsed is TypeDefinition) {
      _removeMember(parsed.name.name);
      var type = _lib.addType(parsed.name.name, parsed, parsed.isClass);
      type.resolve();
      world.gen.writeType(type);
      code = world.gen.writer.text;
    } else {
      parsed.visit(methGen);
      code = methGen.writer.text;
    }

    return this._jsEvaluator.eval(code);
  }
}
