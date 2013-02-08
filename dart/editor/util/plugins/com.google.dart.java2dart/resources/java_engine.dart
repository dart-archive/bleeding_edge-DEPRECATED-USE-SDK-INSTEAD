library java.engine;

import "java_core.dart";
import "source.dart";
import "error.dart";
import "ast.dart";
import "element.dart";

class AnalysisException implements Exception {
  String toString() => "AnalysisException";
}

class AnalysisEngine {
  static getInstance() {
    throw new UnsupportedOperationException();
  }
}

class AnalysisContext {
  Element getElement(ElementLocation location) {
    throw new UnsupportedOperationException();
  }
}

class AnalysisContextImpl extends AnalysisContext {
  getSourceFactory() {
    throw new UnsupportedOperationException();
  }
  LibraryElement getLibraryElementOrNull(Source source) {
    return null;
  }
  LibraryElement getLibraryElement(Source source) {
    throw new UnsupportedOperationException();
  }
  void recordLibraryElements(Map<Source, LibraryElement> elementMap) {
    throw new UnsupportedOperationException();
  }
  getPublicNamespace(LibraryElement library) {
    throw new UnsupportedOperationException();
  }
  CompilationUnit parse(Source source, AnalysisErrorListener errorListener) {
    throw new UnsupportedOperationException();
  }
}

class StringUtilities {
  static List<String> EMPTY_ARRAY = new List.fixedLength(0);
}

class SourceFactory {
}

class JavaFile {
  String path;
  JavaFile(this.path);
}

JavaFile createFile(String path) => new JavaFile(path);

class FileBasedSource extends Source {
  FileBasedSource(SourceFactory factory, JavaFile file) {}
  bool operator ==(Object object) {
    return identical(object, this);
  }
  void getContents(Source_ContentReceiver receiver) {
    throw new UnsupportedOperationException();
  }
  String get fullName {
    throw new UnsupportedOperationException();
  }
  String get shortName {
    throw new UnsupportedOperationException();
  }
  bool isInSystemLibrary() {
    throw new UnsupportedOperationException();
  }
  Source resolve(String uri) {
    throw new UnsupportedOperationException();
  }
}
