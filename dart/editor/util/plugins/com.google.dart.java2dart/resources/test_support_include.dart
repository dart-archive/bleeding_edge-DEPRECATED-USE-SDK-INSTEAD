

class TestSource implements Source {
  bool operator ==(Object object) {
    return this == object;
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
