#library("Sample Code");

typedef int FunctionTypeAlias(int a, int b, int c);

typedef int _PrivateFunctionTypeAlias(int a, int b);

class PublicClass {
  int publicField;
  FunctionTypeAlias _privateField;

  final int publicFinalField;
  final int _privateFinalField;

  static int publicStaticField;
  static int _privateStaticField;

  static final double publicStaticFinalField = 0.0;
  static final double _privateStaticFinalField = 1.0;

  PublicClass() {}

  PublicClass.factoryMethod(int value) {
    publicField = value;
  }

  int publicMethod(int a, int b, PublicInterface ignored) {
    f(int x) {
      return _privateField(x - 1, x, x + 1);
    };
    return f(a) + f(b);
  }

  void _privateMethod(int parameter) {
    String localVariable = null;
  }

  void nativeMethodNoBody() native;

  void nativeMethodWithString() native "window.alert('?')";

  static void nativeMethodWithBody() native {
  }

  abstract double publicAbstractMethod();

  abstract double _privateAbstractMethod();

  FunctionTypeAlias get getter {
    return _privateField;
  }

  void set setter(FunctionTypeAlias method) {
    _privateField = method;
  }
}

class _PrivateClass {
  final int constantField;

  const _PrivateClass(this.constantField);
}

interface PublicInterface {
}

interface _PrivateInterface {
}

class NativeClass native "something" {
}

final double PublicTopLevelFinalVariable = 3.14;

String _PrivateTopLevelVariable = "";

String get topLevelGetter {
  return _PrivateTopLevelVariable;
}

void set topLevelSetter(String value) {
  _PrivateTopLevelVariable = value;
}

double topLevelFunction(double offset) {
  return PublicTopLevelFinalVariable + offset;
}

int _privateTopLevelFunction(int value) {
  return value + 1;
}