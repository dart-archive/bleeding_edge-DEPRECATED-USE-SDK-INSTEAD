

class TestSource implements Source {
  String _name;
  TestSource([this._name = '/test.dart']);
  int get hashCode => 0;
  bool operator ==(Object object) {
    return object is TestSource;
  }
  AnalysisContext get context {
    throw new UnsupportedOperationException();
  }
  void getContentsToReceiver(Source_ContentReceiver receiver) {
    throw new UnsupportedOperationException();
  }
  String get fullName {
    return _name;
  }
  String get shortName {
    return _name;
  }
  String get encoding {
    throw new UnsupportedOperationException();
  }
  int get modificationStamp {
    throw new UnsupportedOperationException();
  }
  bool exists() => true;
  bool get isInSystemLibrary {
    throw new UnsupportedOperationException();
  }
  Source resolve(String uri) {
    throw new UnsupportedOperationException();
  }
  Uri resolveRelativeUri(Uri uri) {
    throw new UnsupportedOperationException();
  }
  UriKind get uriKind {
    throw new UnsupportedOperationException();
  }
  Uri get uri {
    throw new UnsupportedOperationException();
  }
  TimestampedData<String> get contents {
    throw new UnsupportedOperationException();
  }
}
