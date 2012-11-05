import "package:meta/lib/meta.dart";

final xs = [1, 2, 3, 4, 5];
const howMany = 3;

void main() {
  new Sample().printXs(howMany);
}

class Sample extends Object {
  static double _zval = 0.3;
  static func() => print('callback');
  final Map mapa = { 'a' : func };
  double get zfetcher => _zval;
  set zsetter(x) => _zval = x;

  /** Print some xs. */
  void printXs(int limit) {
    var other;
    for (int i = 0; i < (limit > xs.length ? xs.length : limit); i++) {
      if (identical(this, other)) print(xs[i]); // Print the indexed xs.
    }
    var msg = oldMethod(zsetter = zfetcher);
    print(msg);
  }

  /* Old code. */
  @deprecated String oldMethod(var qk) {
    String rs = r'string';
    String ls = """
long string
""";
    String x = "prefix $rs${ls.substring(3, qk)} suffix";
    return x;
  }
}