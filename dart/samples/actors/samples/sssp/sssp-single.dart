#import('sssp-util.dart');
#import('graph.dart');

var ups;

sssp(String filename) {
  Graph g = readGraph('rome99.gr');
  bellmanFord(g.vertices, g.edges, 1);
  
  g = readGraph(filename);
  Stopwatch sw = new Stopwatch();
  sw.start();
  bellmanFord(g.vertices, g.edges, 1);
  sw.stop();
  print("Single threaded time for ${filename}: ${sw.elapsedInMs()} ms");
  print("$ups");
}

void bellmanFord(List<Vertex> vertices, List<Edge> edges, int source) {
  ups = 0;
  vertices[source - 1].distance = 0;
  vertices[source - 1].predecessor = -1;      
  for (int i = 0; i < vertices.length; i++) {
    for (Vertex u in vertices) {
      if (u.oldDist != u.distance) {
        u.oldDist = u.distance;
        for (Edge uv in u.outgoings) {
          int newDist = u.distance + uv.weight;
          Vertex v = vertices[uv.destination - 1];
          if (newDist < v.distance) {
            ups++;
            v.distance = newDist;
            v.predecessor = uv.source;
          }
        }
      }
    }
  }
}

main() {
  sssp("rome99.gr");
}
