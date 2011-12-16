#library('graph');

class Edge {
  int destination;
  int source;
  int weight;
  Edge(this.source, this.destination, this.weight);
  String toString() => "${source} ${destination} ${weight}";
}

class Vertex {
  // This is the maximum value for distance. 
  // The int type is chosen for performance reasons. 
  static final int INFINITY = 1073741823;
  int id;
  List<Edge> outgoings;
  int distance;
  int oldDist;
  int predecessor;
  Vertex(this.id) {
    distance = INFINITY;
    oldDist = INFINITY;
    predecessor = -1;
    outgoings = <Edge>[];
  }
  String toString() {
    StringBuffer sb = new StringBuffer("${id}, ${distance}, ${predecessor}");
    return sb.toString();
  }
}

class Graph {
  List<Vertex> vertices;
  List<Edge> edges;
  
  Graph(this.vertices, this.edges);
}

void printGraph(Graph g) {
  for (Vertex v in g.vertices) {
    print(v.toString());
  }
}
