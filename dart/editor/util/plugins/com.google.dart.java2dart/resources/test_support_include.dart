

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
  bool exists() => true;
  bool isInSystemLibrary() {
    throw new UnsupportedOperationException();
  }
  Source resolve(String uri) {
    throw new UnsupportedOperationException();
  }
  Source resolveRelative(Uri uri) {
    throw new UnsupportedOperationException();
  }
}

/**
 * Wrapper around [Function] which should be called with [target] and [arguments].
 */
class MethodTrampoline {
  int parameterCount;
  Function trampoline;
  MethodTrampoline(this.parameterCount, this.trampoline);
  Object invoke(target, List arguments) {
    if (arguments.length != parameterCount) {
      throw new IllegalArgumentException("${arguments.length} != $parameterCount");
    }
    switch (parameterCount) {
      case 0:
        return trampoline(target);
      case 1:
        return trampoline(target, arguments[0]);
      case 2:
        return trampoline(target, arguments[0], arguments[1]);
      case 3:
        return trampoline(target, arguments[0], arguments[1], arguments[2]);
      case 4:
        return trampoline(target, arguments[0], arguments[1], arguments[2], arguments[3]);
      default:
        throw new IllegalArgumentException("Not implemented for > 4 arguments");
    }
  }
}
