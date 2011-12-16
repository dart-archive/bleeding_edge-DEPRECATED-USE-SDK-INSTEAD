#import('../../core/actors-term.dart');
#import('sssp-util.dart');
#import('graph.dart');

main() {
  sssp(6, 4, "rome99.gr");
}

sssp(int numIsols, int numActors, String filename) {
  ActorManager ac = new ActorManager(numIsols);
  ac.create(const SSSPManagerFactory(), "init", [numActors, filename, 1]);
  print("Runs ${filename} with ${numIsols} isolates and ${numActors} actors.");
}

// Copies a list from index 'from' (inclusive) to index 'to' (exclusive)
List copyFromTo(List l, int from, int to) {
  List c = new List(to - from);
  for (int i = from; i < to; i++) {
   c[i - from] = l[i];
  }
  return c;
}

// Copies the whole list 'src' into the list 'dst' from index 'from'  
void copyInFrom(List src, List dst, int from) {
  for (int i = 0; i < src.length; i++) {
   dst[i + from] = src[i];
  }
}

class SSSPManager extends Actor {
  List<ActorId> _workers;
  Graph _graph;
  int _numActors;
  int _rets;
  int _rounds;
  int _edgeUpdates;
  Stopwatch sw;
  bool _isWarmup;
  String _fileName;
  int _source;
  
  SSSPManager() : super() {
    on["init"] = (int numActors, String fileName, int source) {
      this._source = source;
      this._fileName = fileName;
      this._numActors = numActors;
      me.send("warmup", [numActors]);
    };
    
    on["warmup"] = (int numActors) {
      ui.print('In warmup...');
      _isWarmup = true;
      _rets = 0;
      _rounds = 0;
      _edgeUpdates = 0;
      _numActors = numActors;
      _workers = new List(_numActors);
      _graph = readGraph('rome99.gr');
      _graph.vertices[0].distance = 0;
      _graph.vertices[0].predecessor = -1;
      int verticesPerActor = 
          (_graph.vertices.length / _numActors).ceil().toInt();
      for (int i = 0; i < _numActors; i++) {
        int l = i * verticesPerActor;
        int h;        
        if (i + 1 == _numActors) {
          h = _graph.vertices.length;
        }
        else {
          h = (i + 1) * verticesPerActor;
        }
        create(const WorkerFactory(), "init", 
            [i, l, verticesPerActor, copyFromTo(_graph.vertices, l, h), me, 
             _graph.vertices.length, _graph.edges.length]);
      }
    };
    
    // This message handler will read the graph from file 'fileName'.
    // It then creates numActors actors to process the nodes. 
    // The shortest path is computed from the source node to all other nodes.
    on["setup"] = (int numActors, String fileName, int source) {
      _isWarmup = false;
      _rets = 0;
      _rounds = 0;
      _edgeUpdates = 0;
      _numActors = numActors;
      _workers = new List(_numActors);
      _graph = readGraph(fileName);
      _graph.vertices[source - 1].distance = 0;
      _graph.vertices[source - 1].predecessor = -1;      
      int verticesPerActor = 
          (_graph.vertices.length / _numActors).ceil().toInt();
      for (int i = 0; i < _numActors; i++) {
        int l = i * verticesPerActor;
        int h;        
        if (i + 1 == _numActors) {
          h = _graph.vertices.length;
        }
        else {
          h = (i + 1) * verticesPerActor;
        }
        create(const WorkerFactory(), "init", 
            [i, l, verticesPerActor, copyFromTo(_graph.vertices, l, h), me, 
             _graph.vertices.length, _graph.edges.length]);
      }
    };
    
    // This message is sent by the worker actors after they are created and 
    // initialized. They sent back their id and their index. This handler 
    // will put them in the proper location in the _workers list.
    // This handler, upon receiving the reply from all the workers, will 
    // send a message to all the workers to start processing. 
    on["ready"] = (ActorId aid, int indx) {
      _workers[indx] = aid;
      _rets++;
      if (_rets == _numActors) {
        _rets = 0;
        sw = new Stopwatch();
        sw.start();
        for (ActorId a in _workers) {
          send(a, "set-workers-go", [_workers]);
        }
      }      
    };
    
    // This message handler receive the final result from each worker actor.
    // Upon receiving enough number of result, it will print the graph and
    // elapsed time.
    on["result"] = (int from, List<Vertex> vertices) {
      _rets++;
      copyInFrom(vertices, _graph.vertices, from);
      if (_rets == _numActors) {
        sw.stop();
        _rets = 0;
        if (_isWarmup) {
          _isWarmup = false;
          me.send("setup", [_numActors, _fileName, _source]);
        }
        else {
          ui.print("Time: ${sw.elapsedInMs()} ms");
          halt();
        }
      }
    };
  }
}

class Worker extends Actor {
  int _indx;
  int _from;
  List<Vertex> _vertices;
  List<ActorId> _workers;  
  ActorId _manager;
  int _verticesPerActor;
  int _numWorkers;
  int time;
  List<List<int>> updateList;
  int _totalVertices;
  int _rounds;
  int _totalUps;
  int _totalEdges;
  
  Worker() : super() {
    
    // Initializes a worker actor.
    // 'indx' is the index of this worker in the worker list.
    // 'from' is the starting index of vertices in the original list of vertices
    // which this actor is responsible for processing them. 
    // vercites is the portion of vertices from the graph which this actor
    // is going to process. 
    // manager is the ActorId of the manager actor.
    on["init"] = (int indx, int from, int verticesPerActor, 
        List<Vertex> vertices, ActorId manager, 
        int totalVertices, int totalEdges) {
      _indx = indx;
      _from = from;
      _verticesPerActor = verticesPerActor;
      _vertices = vertices;
      _manager = manager;
      _totalVertices = totalVertices;
      _totalUps = 0;
      _rounds = 0;
      _totalEdges = totalEdges;
      time = 0;
      reply("ready", [me, indx]);
    };
    
    // Each worker actor has the list of all the other workers to directly 
    // send update messages to them.
    on["set-workers-go"] = (List<ActorId> workers) {
      Stopwatch sw = new Stopwatch();
      sw.start();
      this._workers = workers;
      _numWorkers = workers.length;
      updateList = new List(_workers.length);
      go();
      sw.stop();
      time += sw.elapsedInMs();
    };
    
    on["go"] = () {
      Stopwatch sw = new Stopwatch();
      sw.start();
      go();
      sw.stop();
      time += sw.elapsedInMs();
    };
    
    // This update is received from another worker actor, when there is a 
    // change to the source node and that could cause a change to the 
    // destination node. 
    on["u"] = (List<int> l, int ups) {
      Stopwatch sw = new Stopwatch();
      sw.start();
      for (int i = 0; i < l.length; i+= 3)
        _update(l[i], l[i+1], l[i+2]);
      _totalUps += ups + (l.length ~/ 3);
      if (_totalUps >= _totalEdges) {
        _totalUps -= _totalEdges;
        _rounds++;
        if (_rounds < _totalVertices) {
          me.send("go");
        }
        else {
          ui.print("Computation Time in Actor $uid = ${time}");
          _manager.send("result", [_from, _vertices]);
        }
      }
      sw.stop();
      time += sw.elapsedInMs();
    };
  }
  
  // This method possibly updates the current destination of the node
  // indexed by dst to newDist.
  void _update(int src, int dst, int newDist) {
    var v = _vertices[dst-_from-1];
    if (v.distance > newDist) {
      v.distance = newDist;
      v.predecessor = src;        
    }
  }
  
  void go() {
    for (int i = 0; i < _numWorkers; i++) {
      updateList[i] = new List();
    }
    int myups = 0;
    int workerIndx;
    for (Vertex u in _vertices) {
      if (u.oldDist != u.distance) {
        u.oldDist = u.distance;
        for (Edge uv in u.outgoings) {
          workerIndx = ((uv.destination - 1) / _verticesPerActor).toInt();
          if (workerIndx == _indx) {
            _update(uv.source, uv.destination, u.distance + uv.weight);
            myups++;
          }
          else updateList[workerIndx].addAll(
                [uv.source, uv.destination, u.distance + uv.weight]);
        }
      }
      else {
        myups += u.outgoings.length;
      }
    }
    int sentUps = 0;
    for (int i = 0; i < _numWorkers; i++) 
      sentUps += (updateList[i].length ~/ 3);
    for (int i = 0; i < _numWorkers; i++) {
      _workers[i].send("u", [updateList[i], sentUps+myups]);
    }
  }
}

class SSSPManagerFactory implements ActorFactory {
  const SSSPManagerFactory();
  Actor create() => new SSSPManager();
}

class WorkerFactory implements ActorFactory {
  const WorkerFactory();
  Actor create() => new Worker();
}

