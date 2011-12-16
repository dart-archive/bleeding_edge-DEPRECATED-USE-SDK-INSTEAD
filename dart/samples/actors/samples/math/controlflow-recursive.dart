#import('../../core/actors-term.dart');

class Recursion extends Actor {

  Recursion() : super() {
    on["ack"] = (int m, int n) {
      Stopwatch sw = new Stopwatch();
      sw.start();
      int r = ack(m, n);
      sw.stop();
      ui.print("ack("+m+", "+n+") = "+r+" in "+sw.elapsedInMs()+" ms");
      reply("done");
    };
    on["fib"] = (int n) {
      Stopwatch sw = new Stopwatch();
      sw.start();
      int r = fib(n);
      sw.stop();
      ui.print("fib("+n+") = "+r+" in "+sw.elapsedInMs()+" ms");
      reply("done");
    };
    on["tak"] = (int x, int y, int z) {
      Stopwatch sw = new Stopwatch();
      sw.start();
      int r = tak(x, y, z);
      sw.stop();
      ui.print("tak("+x+", "+y+", "+z+") = "+r+" in "+ sw.elapsedInMs()+" ms");
      reply("done");
    };
  }

  static int ack(int m, int n){
     if (m==0) { return n+1; }
     if (n==0) { return ack(m-1,1); }
     return ack(m-1, ack(m,n-1) );
  }

  static int fib(int n) {
      if (n < 2){ return 1; }
      return fib(n-2) + fib(n-1);
  }

  static int tak(int x, int y, int z) {
      if (y >= x) return z;
      return tak(tak(x-1,y,z), tak(y-1,z,x), tak(z-1,x,y));
  }
}

class BenchmarkRecursion extends Actor {
  int dones;

  BenchmarkRecursion() : super() {
    on["init"] = () {
      dones = 0;
      for ( var i = 3; i <= 5; i++ ) {
        create(const RecursionFactory(), "ack", [3, i]);
        create(const RecursionFactory(), "fib", [37+i]);
        create(const RecursionFactory(), "tak", [3*i+3, 2*i+2, i+1]);
      }
    };
    on["done"] = () {
      dones++;
      if (dones == 9) {
        halt();
      }
    };
  }
}


class BenchmarkRecursionFactory implements ActorFactory {
  const BenchmarkRecursionFactory();
  Actor create() => new BenchmarkRecursion();
}

class RecursionFactory implements ActorFactory {
  const RecursionFactory();
  Actor create() => new Recursion();
}

main() {
  new ActorManager(4).create(const BenchmarkRecursionFactory(), "init", []);
}

