// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('sssp-util');
#import('dart:io');
#import('graph.dart');

Graph readGraph(String fileName) {
  File f = new File(fileName);
  StringInputStream sis = new StringInputStream(f.openInputStream());
  String l = sis.readLine();
  List<Vertex> vertices;
  List<Edge> edges = <Edge>[];
  int numEdges, numVertices;
  while(l != null) {
    if (l.startsWith("p")) {
      List<String> ss = l.split(" ");
      numVertices = Math.parseInt(ss[2]);
      numEdges = Math.parseInt(ss[3]);
      vertices = new List(numVertices);
      for (int i = 0; i < numVertices; i++) {
        vertices[i] = new Vertex(i+1);
      }
    }
    if (l.startsWith("a")) {
      List<String> ss = l.split(" ");
      int source = Math.parseInt(ss[1]);
      Edge e = new Edge(source,
          Math.parseInt(ss[2]),
          Math.parseInt(ss[3])
          );
      edges.add(e);
      vertices[source-1].outgoings.add(e);
    }
    l = sis.readLine();
  }
  if (numEdges != edges.length) {
    print("ERROR: Invalid number of edges.");
  }
  return new Graph(vertices, edges);
}
