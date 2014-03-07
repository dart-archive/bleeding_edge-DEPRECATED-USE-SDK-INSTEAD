

class TestSource implements Source {
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
    throw new UnsupportedOperationException();
  }
  String get shortName {
    throw new UnsupportedOperationException();
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
  Source resolveRelative(Uri uri) {
    throw new UnsupportedOperationException();
  }
  UriKind get uriKind {
    throw new UnsupportedOperationException();
  }
  TimestampedData<String> get contents {
    throw new UnsupportedOperationException();
  }
}
