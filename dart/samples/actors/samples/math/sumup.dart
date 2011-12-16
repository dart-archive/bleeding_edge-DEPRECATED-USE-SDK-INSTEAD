#import('../../core/actors-term.dart');

class SumUp extends Actor {
  final int N = 4;
  var total;
  var rets;
  Stopwatch sw;

  SumUp() : super() {
    on["init"] = (var arr) {
      sw = new Stopwatch();
      sw.start();
      total = 0;
      rets = 0;
      for (int i = 0; i < N; i++) {
        create(const SummerFactory(), "init", [i, arr, N]);
      }
    };

    on["sum"] = (double t) {
      total += t;
      rets++;
      if (rets == N) {
        ui.print("Total = "+total);
        sw.stop();
        ui.print("SumUp took "+sw.elapsedInMs()+" ms with "+N+" thread.");
        halt();
      }
    };
  }
}


class Summer extends Actor {
  Summer() : super() {
    on["init"] = (int batch, var arr, int N) {
      int len = arr.length;
      int p = len ~/ N;
      int low, high;
      low = batch*p;
      if (batch +1 == N) {
        high = len;
      }
      else {
        high = (batch+1)*p;
      }
      double t = 0.0;
      for (int i = low; i < high; i++) {
        t+=arr[i];
      }
      reply("sum", [t]);
    };
  }
}


class SummerFactory implements ActorFactory {
  const SummerFactory();
  Actor create() => new Summer();
}

class SumUpFactory implements ActorFactory {
  const SumUpFactory();
  Actor create() => new SumUp();
}

main() {
  Stopwatch sw = new Stopwatch();
  print("Creating an array...");
  sw.start();
  int N = 500000;
  List<double> arr = new List(N);
  for (int i = 0; i < N; i++) {
    arr[i] = Math.random();
  }
  sw.stop();
  print("Array creation took "+sw.elapsedInMs()+" ms.");
  print("Summing up the array...");
  sw.start();
  double t = 0.0;
  for (int i = 0; i < N; i++) {
    t += arr[i];
  }
  sw.stop();
  print("Sum took "+sw.elapsedInMs()+" ms with one thread.");

  print("Expected sum: "+t);
  new ActorManager(4).create(const SumUpFactory(), "init", [arr]);
}





