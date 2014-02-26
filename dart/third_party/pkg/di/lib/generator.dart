library di.generator;

import 'package:analyzer/src/generated/java_io.dart';
import 'package:analyzer/src/generated/source_io.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/sdk.dart' show DartSdk;
import 'package:analyzer/src/generated/sdk_io.dart' show DirectoryBasedDartSdk;
import 'package:analyzer/src/generated/element.dart';
import 'package:analyzer/src/generated/engine.dart';

import 'dart:io';

const String PACKAGE_PREFIX = 'package:';
const String DART_PACKAGE_PREFIX = 'dart:';

main(args) {
  if (args.length < 4) {
    print('Usage: generator path_to_sdk file_to_resolve annotations output [package_roots+]');
    exit(0);
  }

  var pathToSdk = args[0];
  var entryPoint = args[1];
  var classAnnotations = args[2].split(',');
  var output = args[3];
  var packageRoots = (args.length < 5) ? [Platform.packageRoot] : args.sublist(4);

  print('pathToSdk: $pathToSdk');
  print('entryPoint: $entryPoint');
  print('classAnnotations: ${classAnnotations.join(', ')}');
  print('output: $output');
  print('packageRoots: $packageRoots');

  var c = new SourceCrawler(pathToSdk, packageRoots);
  List<String> imports = <String>[];
  List<ClassElement> typeFactoryTypes = <ClassElement>[];
  Map<String, String> typeToImport = new Map<String, String>();
  c.crawl(entryPoint, (CompilationUnitElement compilationUnit, SourceFile source) {
      new CompilationUnitVisitor(c.context, source, classAnnotations, imports,
          typeToImport, typeFactoryTypes).visit(compilationUnit);
  });
  var code = printLibraryCode(typeToImport, imports, typeFactoryTypes);
  new File(output).writeAsStringSync(code);
}

String printLibraryCode(Map<String, String> typeToImport, List<String> imports,
                      List<ClassElement> typeFactoryTypes) {
  List<String> requiredImports = <String>[];
  StringBuffer factories = new StringBuffer();

  String resolveClassIdentifier(InterfaceType type) {
    if (type.element.library.isDartCore) {
      return type.name;
    }
    String import = typeToImport[getCanonicalName(type)];
    if (!requiredImports.contains(import)) {
      requiredImports.add(import);
    }
    return 'import_${imports.indexOf(import)}.${type.name}';
  }

  typeFactoryTypes.forEach((ClassElement clazz) {
    factories.write(
        'typeFactories[${resolveClassIdentifier(clazz.type)}] = (f) => ');
    factories.write('new ${resolveClassIdentifier(clazz.type)}(');
    ConstructorElement constr =
        clazz.constructors.firstWhere((c) => c.name.isEmpty,
        orElse: () {
          throw 'Unable to find default constructor for $clazz in ${clazz.source}';
        });
    factories.write(constr.parameters.map((param) {
      if (param.type.element is! ClassElement) {
        throw 'Unable to resolve type for constructor parameter '
              '"${param.name}" for type "$clazz" in ${clazz.source}';
      }
      return 'f(${resolveClassIdentifier(param.type)})';
    }).join(', '));
    factories.write(');\n');
  });
  StringBuffer code = new StringBuffer();
  code.write('library di.generated.type_factories;\n');
  requiredImports.forEach((import) {
    code.write ('import "$import" as import_${imports.indexOf(import)};\n');
  });
  code..write('var typeFactories = new Map();\n')
    ..write('main() {\n')
    ..write(factories)
    ..write('}\n');

  return code.toString();
}

class CompilationUnitVisitor {
  List<String> imports;
  Map<String, String> typeToImport;
  List<ClassElement> typeFactoryTypes;
  List<String> classAnnotations;
  SourceFile source;
  AnalysisContext context;

  CompilationUnitVisitor(this.context, this.source,
      this.classAnnotations, this.imports, this.typeToImport,
      this.typeFactoryTypes);

  visit(CompilationUnitElement compilationUnit) {
    visitLibrary(compilationUnit.enclosingElement);

    List<ClassElement> types = <ClassElement>[];
    types.addAll(compilationUnit.types);

    for (CompilationUnitElement part in compilationUnit.enclosingElement.parts) {
      types.addAll(part.types);
    }

    types.forEach(visitClassElement);
  }

  visitLibrary(LibraryElement libElement) {
    CompilationUnit resolvedUnit = context
        .resolveCompilationUnit(libElement.source, libElement);

    resolvedUnit.directives.forEach((Directive directive) {
      if (directive is LibraryDirective) {
        LibraryDirective library = directive;
        int annotationIdx = 0;
        library.metadata.forEach((Annotation ann) {
          if (ann.element is ConstructorElement &&
            getQualifiedName(
                (ann.element as ConstructorElement).enclosingElement.type) ==
                'di.annotations.Injectables') {
            var listLiteral =
                library.metadata[annotationIdx].arguments.arguments.first;
            for (Expression expr in listLiteral.elements) {
              Element element = (expr as SimpleIdentifier).bestElement;
              if (element == null || element is! ClassElement) {
                throw 'Unable to resolve type "$expr" from @Injectables '
                      'in ${library.element.source}';
              }
              typeFactoryTypes.add(element as ClassElement);
            }
          }
          annotationIdx++;
        });
      }
    });
  }

  visitClassElement(ClassElement classElement) {
    if (classElement.name.startsWith('_')) {
      return; // ignore private classes.
    }
    typeToImport[getCanonicalName(classElement.type)] =
        source.entryPointImport;
    if (!imports.contains(source.entryPointImport)) {
      imports.add(source.entryPointImport);
    }
    for (ElementAnnotation ann in classElement.metadata) {
      if (ann.element is ConstructorElement) {
        ConstructorElement con = ann.element;
        if (classAnnotations
            .contains(getQualifiedName(con.enclosingElement.type))) {
          typeFactoryTypes.add(classElement);
        }
      }
    }
  }
}

String getQualifiedName(InterfaceType type) {
  var lib = type.element.library.displayName;
  var name = type.name;
  return lib == null ? name : '$lib.$name';
}

String getCanonicalName(InterfaceType type) {
  var source = type.element.source.toString();
  var name = type.name;
  return '$source:$name';
}

typedef CompilationUnitCrawler(CompilationUnitElement compilationUnit,
                               SourceFile source);

class SourceCrawler {
  final List<String> packageRoots;
  final String sdkPath;
  AnalysisContext context = AnalysisEngine.instance.createAnalysisContext();

  SourceCrawler(this.sdkPath, this.packageRoots);

  void crawl(String entryPoint, CompilationUnitCrawler _visitor) {
    JavaSystemIO.setProperty("com.google.dart.sdk", sdkPath);
    DartSdk sdk = DirectoryBasedDartSdk.defaultSdk;

    AnalysisOptionsImpl contextOptions = new AnalysisOptionsImpl();
    contextOptions.cacheSize = 256;
    contextOptions.preserveComments = false;
    contextOptions.analyzeFunctionBodies = false;
    context.analysisOptions = contextOptions;
    sdk.context.analysisOptions = contextOptions;

    var packageUriResolver =
        new PackageUriResolver(packageRoots.map(
            (pr) => new JavaFile.fromUri(new Uri.file(pr))).toList());
    context.sourceFactory = new SourceFactory.con2([
      new DartUriResolver(sdk),
      new FileUriResolver(),
      packageUriResolver
    ]);

    var entryPointFile;
    var entryPointImport;
    if (entryPoint.startsWith(PACKAGE_PREFIX)) {
      entryPointFile = new JavaFile(packageUriResolver
          .resolveAbsolute(context.sourceFactory.contentCache,
              Uri.parse(entryPoint)).toString());
      entryPointImport = entryPoint;
    } else {
      entryPointFile = new JavaFile(entryPoint);
      entryPointImport = entryPointFile.getAbsolutePath();
    }

    Source source = new FileBasedSource.con1(
        context.sourceFactory.contentCache, entryPointFile);
    ChangeSet changeSet = new ChangeSet();
    changeSet.added(source);
    context.applyChanges(changeSet);
    LibraryElement rootLib = context.computeLibraryElement(source);
    CompilationUnit resolvedUnit =
        context.resolveCompilationUnit(source, rootLib);

    var sourceFile = new SourceFile(
        entryPointFile.getAbsolutePath(),
        entryPointImport,
        resolvedUnit.element);
    List<SourceFile> visited = <SourceFile>[];
    List<SourceFile> toVisit = <SourceFile>[sourceFile];

    while (toVisit.isNotEmpty) {
      SourceFile currentFile = toVisit.removeAt(0);
      visited.add(currentFile);
      _visitor(currentFile.compilationUnit, currentFile);
      var visitor = new CrawlerVisitor(currentFile, context);
      visitor.accept(currentFile.compilationUnit);
      visitor.toVisit.forEach((SourceFile todo) {
        if (!toVisit.contains(todo) && !visited.contains(todo)) {
          toVisit.add(todo);
        }
      });
    }
  }
}

class CrawlerVisitor {
  List<SourceFile> toVisit = <SourceFile>[];
  SourceFile currentFile;
  AnalysisContext context;
  String currentDir;

  CrawlerVisitor(this.currentFile, this.context);

  void accept(CompilationUnitElement cu) {
    cu.enclosingElement.imports.forEach((ImportElement import) =>
        visitImportElement(import.uri, import.importedLibrary.source));
    cu.enclosingElement.exports.forEach((ExportElement import) =>
        visitImportElement(import.uri, import.exportedLibrary.source));
  }

  visitImportElement(String uri, Source source) {
    if (uri == null) return; // dart:core

    String systemImport;
    bool isSystem = false;
    if (uri.startsWith(DART_PACKAGE_PREFIX)) {
      isSystem = true;
      systemImport = uri;
    } else if (currentFile.entryPointImport.startsWith(DART_PACKAGE_PREFIX)) {
      isSystem = true;
      systemImport = currentFile.entryPointImport;
    }
    // check if it's some internal hidden library
    if (isSystem &&
        systemImport.substring(DART_PACKAGE_PREFIX.length).startsWith('_')) {
      return;
    }

    var nextCompilationUnit = context
        .resolveCompilationUnit(source, context.computeLibraryElement(source));

    if (uri.startsWith(PACKAGE_PREFIX)) {
      toVisit.add(new SourceFile(source.toString(), uri, nextCompilationUnit.element));
    } else { // relative import.
      var newImport;
      if (isSystem) {
        newImport = systemImport; // original uri
      } else {
        // relative import
        String import = currentFile.entryPointImport;
        import = import.replaceAll('\\', '/'); // if at all needed, on Windows
        import = import.substring(0, import.lastIndexOf('/'));
        var currentDir = new File(currentFile.canonicalPath).parent.path;
        currentDir = currentDir.replaceAll('\\', '/'); // if at all needed, on Windows
        if (uri.startsWith('../')) {
          while (uri.startsWith('../')) {
            uri = uri.substring('../'.length);
            import = import.substring(0, import.lastIndexOf('/'));
            currentDir = currentDir.substring(0, currentDir.lastIndexOf('/'));
          }
        }
        newImport = '$import/$uri';
      }
      toVisit.add(new SourceFile(
          source.toString(), newImport, nextCompilationUnit.element));
    }
  }
}

class SourceFile {
  String canonicalPath;
  String entryPointImport;
  CompilationUnitElement compilationUnit;

  SourceFile(this.canonicalPath, this.entryPointImport, this.compilationUnit);

  operator ==(o) {
    if (o is String) return o == canonicalPath;
    if (o is! SourceFile) return false;
    return o.canonicalPath == canonicalPath;
  }
}
