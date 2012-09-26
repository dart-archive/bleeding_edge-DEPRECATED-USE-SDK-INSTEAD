class Z {
  void g(int x, int y, int ...z) {}
//  void f(int v, int w, [int x, int y, int z]) {}
//  void h(int v, int w, [int x = 10, int y, int z = 42]) {}
}
class A {
  A(int this.x) {
    Logger.print(x); // Will print the field value.
  }
  int x;
}
class X {
  toString() {
    return "tcb { ${task.toString()}@${state.toString()} }";
  }
}

// It's a bug` unclosed r" quote

/**
 * http://www.google.com
 */

/* /* /* /* */

/*-
 * Do
 *   Not
 *     Format
 */

/**-
 *      Really
 */
