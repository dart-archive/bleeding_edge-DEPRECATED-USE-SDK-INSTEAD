// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** The one true [World]. */
World world;

/**
 * Experimental phase to enable await, only set when using the
 * await/awaitc.dart entrypoint.
 */
Function experimentalAwaitPhase;

/**
 * Set when the leg compiler is available.  Should always be set
 * to leg/frog_leg.dart/compile.
 */
typedef bool LegCompile(World world);
LegCompile legCompile;


/**
 * Should be called exactly once to setup singleton world.
 * Can use world.reset() to reinitialize.
 */
void initializeWorld(FileSystem files) {
  assert(world == null);
  world = new World(files);
  world.init();
}

/**
 * Compiles the [target] dart file using the given [corelib].
 */
bool compile(String homedir, List<String> args, FileSystem files) {
  parseOptions(homedir, args, files);
  initializeWorld(files);

  var success = world.compile();
  if (options.outfile != null) {
    if (success) {
      var code = world.getGeneratedCode();
      if (!options.outfile.endsWith('.js')) {
        // Add in #! to invoke node.js on files with non-js extensions
        code = '#!/usr/bin/env node\n' + code;
      }
      world.files.writeString(options.outfile, code);
    } else {
      // Throw here so we get a non-zero exit code when running.
      world.files.writeString(options.outfile,
        "throw 'Sorry, but I could not generate reasonable code to run.\\n';");
    }
  }
  return success;
}


/** Can be thrown on any compiler error and includes source location. */
class CompilerException implements Exception {
  final String _message;
  final SourceSpan _location;

  CompilerException(this._message, this._location);

  String toString() {
    if (_location != null) {
      return 'CompilerException: ${_location.toMessageString(_message)}';
    } else {
      return 'CompilerException: $_message';
    }
  }
}


/** Represents a Dart "world" of code. */
class World {
  WorldGenerator gen;
  String legCode; // TODO(kasperl): Remove this temporary kludge.

  FileSystem files;
  final LibraryReader reader;

  Map<String, Library> libraries;
  Library corelib;

  Library get coreimpl() => libraries['dart:coreimpl'];
  Library get dom() => libraries['dart:dom'];

  List<Library> _todo;

  /** Internal map to track name conflicts in the generated javascript. */
  Map<String, Element> _topNames;

  Map<String, MemberSet> _members;

  int errors = 0, warnings = 0;
  int dartBytesRead = 0, jsBytesWritten = 0;
  bool seenFatal = false;

  // Special types to Dart.
  DefinedType varType;
  // TODO(jimhug): Is this ever not === varType?
  DefinedType dynamicType;

  DefinedType voidType;

  DefinedType objectType;
  DefinedType numType;
  DefinedType intType;
  DefinedType doubleType;
  DefinedType boolType;
  DefinedType stringType;
  DefinedType listType;
  DefinedType mapType;
  DefinedType functionType;

  NonNullableType nonNullBool;

  World(this.files)
    : libraries = {}, _todo = [], _members = {}, _topNames = {},
      // TODO(jmesserly): these two types don't actually back our Date and
      // RegExp yet, so we need to add them manually.
      reader = new LibraryReader() {
  }

  void reset() {
    // TODO(jimhug): Use a smaller hammer in the future.
    libraries = {}; _todo = []; _members = {}; _topNames = {};
    errors = warnings = 0;
    seenFatal = false;
    init();
  }

  init() {
    // Setup well-known libraries and types.
    corelib = new Library(readFile('dart:core'));
    libraries['dart:core'] = corelib;
    _todo.add(corelib);

    voidType = _addToCoreLib('void', false);
    dynamicType = _addToCoreLib('Dynamic', false);
    varType = dynamicType;

    objectType = _addToCoreLib('Object', true);
    numType = _addToCoreLib('num', false);
    intType = _addToCoreLib('int', false);
    doubleType = _addToCoreLib('double', false);
    boolType = _addToCoreLib('bool', false);
    stringType = _addToCoreLib('String', false);
    listType = _addToCoreLib('List', false);
    mapType = _addToCoreLib('Map', false);
    functionType = _addToCoreLib('Function', false);

    nonNullBool = new NonNullableType(boolType);
  }

  _addMember(Member member) {
    // Private members are only visible in their own library.
    assert(!member.isPrivate);
    if (member.isStatic) {
      if (member.declaringType.isTop) {
        _addTopName(member);
      }
      return;
    }

    var mset = _members[member.name];
    if (mset == null) {
      mset = new MemberSet(member, isVar:true);
      _members[mset.name] = mset;
    } else {
      mset.members.add(member);
    }
  }

  _addTopName(Element named) {
    var existing = _topNames[named.jsname];
    if (existing != null) {
      info('mangling matching top level name "${named.jsname}" in '
          + 'both "${named.library.jsname}" and "${existing.library.jsname}"');

      if (named.isNative) {
        // resolve conflicts in favor first of natives
        if (existing.isNative) {
          world.internalError('conflicting native names "${named.jsname}" '
              + '(already defined in ${existing.span.locationText})',
              named.span);
        } else {
          _topNames[named.jsname] = named;
          _addJavascriptTopName(existing);
        }
      } else if (named.library.isCore) {
        // then in favor of corelib
        if (existing.library.isCore) {
          world.internalError(
              'conflicting top-level names in core "${named.jsname}" '
              + '(previously defined in ${existing.span.locationText})',
              named.span);
        } else {
          _topNames[named.jsname] = named;
          _addJavascriptTopName(existing);
        }
      } else {
        // then just first in wins
        _addJavascriptTopName(named);
      }
    } else {
      _topNames[named.jsname] = named;
    }
  }

  _addJavascriptTopName(Element named) {
    named._jsname = '${named.library.jsname}_${named.jsname}';
    final existing = _topNames[named.jsname];
    if (existing != null && existing != named) {
      world.internalError('name mangling failed for "${named.jsname}" '
          + '("${named.jsname}" defined also in ${existing.span.locationText})',
          named.span);
    }
    _topNames[named.jsname] = named;
  }

  _addType(Type type) {
    // Top types don't have a name - we will capture their members in
    // [_addMember].
    if (!type.isTop) _addTopName(type);
  }

  _addToCoreLib(String name, bool isClass) {
    var ret = new DefinedType(name, corelib, null, isClass);
    corelib.types[name] = ret;
    return ret;
  }

  // TODO(jimhug): Can this just be a const Set?
  Set<String> _jsKeywords;

  /** Ensures that identifiers are legal in the generated JS. */
  String toJsIdentifier(String name) {
    if (name == null) return null;
    if (_jsKeywords == null) {
      // TODO(jmesserly): this doesn't work if I write "new Set<String>.from"
      // List of JS reserved words.
      _jsKeywords = new Set.from([
        'break', 'case', 'catch', 'continue', 'debugger', 'default',
        'delete', 'do', 'else', 'finally', 'for', 'function', 'if',
        'in', 'instanceof', 'new', 'return', 'switch', 'this', 'throw',
        'try', 'typeof', 'var', 'void', 'while', 'with',
        'class', 'enum', 'export', 'extends', 'import', 'super',
        'implements', 'interface', 'let', 'package', 'private',
        'protected', 'public', 'static', 'yield',
        'native']);
    }
    if (_jsKeywords.contains(name)) {
      return name + '_';
    } else {
      // regexs for better perf?
      return name.replaceAll(@'$', @'$$').replaceAll(':', @'$');
    }
  }

  bool compile() {
    // TODO(jimhug): Must have called setOptions - better errors.
    if (options.dartScript == null) {
      fatal('no script provided to compile');
      return false;
    }

    try {
      info('compiling ${options.dartScript} with corelib $corelib');
      if (!runLeg()) runCompilationPhases();
    } catch (var exc) {
      if (hasErrors && !options.throwOnErrors) {
        // TODO(jimhug): If dev mode then throw.
      } else {
        // TODO(jimhug): Handle these in world or in callers?
        throw;
      }
    }
    printStatus();
    return !hasErrors;
  }

  /** Returns true if Leg handled the compilation job. */
  bool runLeg() {
    if (!options.enableLeg) return false;
    if (legCompile === null) {
      fatal('requested leg enabled, but no leg compiler available');
    }
    bool res = withTiming('try leg compile', () => legCompile(this));
    if (!res && options.legOnly) {
      fatal("Leg could not compile ${options.dartScript}");
      return true; // In --leg_only, always "handle" the compilation.
    }
    return res;
  }

  void runCompilationPhases() {
    final lib = withTiming('first pass', () => processDartScript());
    withTiming('resolve top level', resolveAll);
    if (experimentalAwaitPhase != null) {
      withTiming('await translation', experimentalAwaitPhase);
    }
    withTiming('generate code', () { generateCode(lib); });
  }

  String getGeneratedCode() {
    // TODO(jimhug): Assert compilation is all done here.
    if (legCode !== null) {
      assert(options.enableLeg);
      return legCode;
    } else {
      return gen.writer.text;
    }
  }

  SourceFile readFile(String filename) {
    try {
      final sourceFile = reader.readFile(filename);
      dartBytesRead += sourceFile.text.length;
      return sourceFile;
    } catch (var e) {
      warning('Error reading file: $filename', null);
      return new SourceFile(filename, '');
    }
  }

  Library getOrAddLibrary(String filename) {
    Library library = libraries[filename];

    if (library == null) {
      library = new Library(readFile(filename));
      info('read library ${filename}');
      if (!library.isCore &&
          !library.imports.some((li) => li.library.isCore)) {
        library.imports.add(new LibraryImport(corelib));
      }
      libraries[filename] = library;
      _todo.add(library);
    }
    return library;
  }

  process() {
    while (_todo.length > 0) {
      final todo = _todo;
      _todo = [];
      for (var lib in todo) {
        lib.visitSources();
      }
    }
  }

  Library processDartScript([String script = null]) {
    if (script == null) script = options.dartScript;
    Library library = getOrAddLibrary(script);
    process();
    return library;
  }

  resolveAll() {
    for (var lib in libraries.getValues()) {
      lib.resolve();
    }
  }

  generateCode(Library lib) {
    var mainMembers = lib.topType.resolveMember('main');
    var main = null;
    if (mainMembers == null || mainMembers.members.length == 0) {
      fatal('no main method specified');
    } else if (mainMembers.members.length > 1) {
      for (var m in mainMembers.members) {
        main = m;
        error('more than one main member (using last?)', main.span);
      }
    } else {
      main = mainMembers.members[0];
    }

    var codeWriter = new CodeWriter();
    gen = new WorldGenerator(main, codeWriter);
    gen.run();
    jsBytesWritten = codeWriter.text.length;
  }

  // ********************** Message support ***********************

  void _message(String message, SourceSpan span, SourceSpan span1,
      SourceSpan span2, bool throwing) {
    var text = message;
    if (span != null) {
      text = span.toMessageString(message);
    }
    print(text);
    if (span1 != null) {
      print(span1.toMessageString(message));
    }
    if (span2 != null) {
      print(span2.toMessageString(message));
    }

    if (throwing) {
      throw new CompilerException(message, span);
    }
  }

  /** [message] is considered a static compile-time error by the Dart lang. */
  void error(String message, [SourceSpan span, SourceSpan span1, SourceSpan span2]) {
    errors++;
    _message('error: $message', span, span1, span2, options.throwOnErrors);
  }

  /** [message] is considered a type warning by the Dart lang. */
  void warning(String message, [SourceSpan span, SourceSpan span1, SourceSpan span2]) {
    if (options.warningsAsErrors) {
      error(message, span, span1, span2);
      return;
    }
    warnings++;
    if (options.showWarnings) {
      _message('warning: $message', span, span1, span2, options.throwOnWarnings);
    }
  }

  /** [message] at [location] is so bad we can't generate runnable code. */
  void fatal(String message, [SourceSpan span, SourceSpan span1, SourceSpan span2]) {
    errors++;
    seenFatal = true;
    _message('fatal: $message', span, span1, span2,
     options.throwOnFatal || options.throwOnErrors);
  }

  /** [message] at [location] is about a bug in the compiler. */
  void internalError(String message, [SourceSpan span, SourceSpan span1, SourceSpan span2]) {
    _message('We are sorry, but... $message', span, span1, span2, true);
  }

  /**
   * [message] at [location] will tell the user about what the compiler
   * is doing.
   */
  void info(String message, [SourceSpan span, SourceSpan span1, SourceSpan span2]) {
    if (options.showInfo) {
      _message('info: $message', span, span1, span2, false);
    }
  }

  /** Run [fn] without the forceDynamic option enabeld. */
  withoutForceDynamic(void fn()) {
    var oldForceDynamic = options.forceDynamic;
    options.forceDynamic = false;

    try {
      return fn();
    } finally {
      options.forceDynamic = oldForceDynamic;
    }
  }

  bool get hasErrors() => errors > 0;

  void printStatus() {
    info('compiled $dartBytesRead bytes Dart -> $jsBytesWritten bytes JS');
    if (hasErrors) {
      print('compilation failed with $errors errors');
    } else {
      if (warnings > 0) {
        info('compilation completed successfully with $warnings warnings');
      } else {
        info('compilation completed sucessfully');
      }
    }
  }

  withTiming(String name, f()) {
    final sw = new Stopwatch();
    sw.start();
    var result = f();
    sw.stop();
    info('$name in ${sw.elapsedInMs()}msec');
    return result;
  }
}
