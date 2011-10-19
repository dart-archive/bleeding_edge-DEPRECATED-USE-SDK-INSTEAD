#library("Sample Code");

typedef int FunctionType(int a, int b, int c);

class PublicClass {
  int publicField;
  FunctionType _privateField;

//  const int publicConstField;
//  const int _privateConstField;

  final int publicFinalField;
  final int _privateFinalField;

  static int publicStaticField;
  static int _privateStaticField;

  static final double publicStaticFinalField = 0.0;
  static final double _privateStaticFinalField = 1.0;

//  static const double publicStaticConstField = 0.0;
//  static const double _privateStaticConstField = -1.0;

  PublicClass() {}

  PublicClass.factoryMethod() {}

  int publicMethod() {
    f(int x) {
      return x;
    };
    return f(0);
  }

  void _privateMethod(int parameter) {
    String localVariable = null;
  }

  abstract double publicAbstractMethod();

  abstract double _privateAbstractMethod();

  FunctionType get getMethod() {
    return _privateField;
  }

  void set setMethod(FunctionType method) {
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

final double PublicTopLevelFinalVariable = 3.14;

String _PrivateTopLevelVariable = "";

String get topLevelGetter() {
  return _PrivateTopLevelVariable;
}

void set topLevelSetter(String value) {
  _PrivateTopLevelVariable = value;
}

double topLevelFunction(double offset) {
  return PublicTopLevelFinalVariable + offset;
}
