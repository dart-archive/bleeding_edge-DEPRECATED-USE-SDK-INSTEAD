void main() {
  print("1");
  print("2");
  print("3");
  
  Foo foo = new Foo();
  foo.bar();
}

int globalVar = 1;

class Foo {
  static int staticVar = 2;
  
  int instanceVar = 3;
  
  void bar() {
    int localVar = 4;
    
    localVar++;
  }
}
