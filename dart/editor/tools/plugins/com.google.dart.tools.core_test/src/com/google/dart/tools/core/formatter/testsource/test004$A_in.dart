// Copyright (c) 2011, the Dart project authors.
// All Rights Reserved.

/**
 * Class CFG
 *
 * A simple class simulating the concept of
 * a control flow graph.
 *
 * CFG maintains a list of nodes, plus a start node.
 * That's it.
 */
class CFG {

  Map<int, BasicBlock> basicBlockMap;
  BasicBlock startnode;
  Queue<BasicBlockEdge> edgeList;

  CFG() {
    startnode = null;
    Out.write('set bbm');
    basicBlockMap = new HashMap<int, BasicBlock>();
    edgeList = new QueueImplementation<BasicBlockEdge>(); // error if < is (
  }

  BasicBlock createNode(int name) {
    BasicBlock node=null;
    if (!basicBlockMap.containsKey(name)) {
      node = new BasicBlock(name);
      basicBlockMap[name]=node;
    } else {
      node = basicBlockMap[name];
    }
    if (getNumNodes() == 1) {
      startnode = node; // CCE if spelled 'startNode'
    }
    return node;
  }

  void dump() {
    basicBlockMap.forEach(function f(int n, BasicBlock bb) {
      bb.dump();
    });
  }

  void addEdge(BasicBlockEdge edge) {
    edgeList.add(edge);
  }

  int getNumNodes() {
    return basicBlockMap.length;
  }

  BasicBlock getStartBasicBlock() {
    return startnode;
  }

  BasicBlock getDst(BasicBlockEdge edge) {
    return edge.getDst();
  }

  BasicBlock getSrc(BasicBlockEdge edge) {
    return edge.getSrc();
  }

  Map<int, BasicBlock> getBasicBlocks() {
    return basicBlockMap;
  }
}

/**
 * class BasicBlock
 *
 * BasicBlock only maintains a vector of in-edges and
 * a vector of out-edges.
 */
class BasicBlock {

  Queue<BasicBlock> inEdges, outEdges;
  int name;

  static int numBasicBlocks; // init to 0

  static int getNumBasicBlocks() {
    return numBasicBlocks;
  }

  BasicBlock(this.name) {
    inEdges = new QueueImplementation<BasicBlock>();
    outEdges = new QueueImplementation<BasicBlock>();
    ++numBasicBlocks;
  }

  void dump() {
    Out.write("BB#");
    Out.write(getName());
    Out.write(": ");
    if (!inEdges.isEmpty) {
      Out.write("in: ");
      inEdges.forEach(function f(BasicBlock bb) {
        Out.write("BB#");
        Out.write(bb.getName());
        Out.write(" ");
      });
    }
    if (!outEdges.isEmpty) {
      Out.write("out: ");
      outEdges.forEach(function f(BasicBlock bb) {
        Out.write("BB#");
        Out.write(bb.getName());
        Out.write(" ");
      });
    }
  }

  int getName() {
    return name;
  }

  Queue<BasicBlock> getInEdges() {
    return inEdges;
  }

  Queue<BasicBlock> getOutEdges() {
    return outEdges;
  }

  int getNumPred() {
    return inEdges.length;
  }

  int getNumSucc() {
    return outEdges.length;
  }

  void addOutEdge(BasicBlock to) {
    outEdges.addLast(to);
  }

  void addInEdge(BasicBlock from) {
    inEdges.add(from);
  }
}

/**
 * class BasicBlockEdge
 *
 * These data structures are stubbed out to make the code below easier
 * to review.
 *
 * BasicBlockEdge only maintains two pointers to BasicBlocks.
 */
class BasicBlockEdge {

  BasicBlock from, to;

  BasicBlockEdge(CFG cfg, int fromName, int toName) : from = cfg.createNode(fromName), to = cfg.createNode(toName) {
    from.addOutEdge(to);
    to.addInEdge(from);
    cfg.addEdge(this);
  }

  BasicBlock getSrc() {
    return from;
  }

  BasicBlock getDst() {
    return to;
  }
}
// Copyright (c) 2011, the Dart project authors.
// All Rights Reserved.

/**
 * Test Program for the Havlak loop finder.
 *
 * This program constructs a fairly large control flow
 * graph and performs loop recognition. This is the Dart
 * version.
 *
 * Adapted from Java code written by rhundt
 */
class Havlak {

  static Window window;

  CFG cfg;
  LSG lsg;
  BasicBlock root;

  Havlak() : cfg = new CFG(), lsg = new LSG() {
    root = cfg.createNode(0);
  }

  /**
   * Create 4 basic blocks, corresponding to and if/then/else clause
   * with a CFG that looks like a diamond.
   */
  int buildDiamond(int start) {
    int bb0 = start;
    new BasicBlockEdge(cfg, bb0, bb0 + 1);
    new BasicBlockEdge(cfg, bb0, bb0 + 2);
    new BasicBlockEdge(cfg, bb0 + 1, bb0 + 3);
    new BasicBlockEdge(cfg, bb0 + 2, bb0 + 3);
    return bb0 + 3;
  }

  /**
   * Connect two existing nodes.
   */
  void buildConnect(int start, int end) {
    new BasicBlockEdge(cfg, start, end);
  }

  /**
   * Form a straight connected sequence of n basic blocks.
   */
  int buildStraight(int start, int n) {
    for (int i = 0; i < n; i++) {
      buildConnect(start + i, start + i + 1);
    }
    return start + n;
  }

  /**
   * Construct a simple loop with two diamonds in it
   */
  int buildBaseLoop(int from) {
    int header = buildStraight(from, 1);
    int diamond1 = buildDiamond(header);
    int d11 = buildStraight(diamond1, 1);
    int diamond2 = buildDiamond(d11);
    int footer = buildStraight(diamond2, 1);
    buildConnect(diamond2, d11);
    buildConnect(diamond1, header);
    buildConnect(footer, from);
    footer = buildStraight(footer, 1);
    return footer;
  }

  static void main(arguments) {
    window = arguments[0];
    Out.write("<p>Havlak Loop Finder Benchmark</p>");
    Havlak app = new Havlak();
    app.cfg.createNode(0);
    app.cfg.createnode(1);
    new BasicBlockEdge(app.cfg, 0, 2);
    for (int dummyloop = 0; dummyloop < 15000; dummyloop++) {
      HavlakLoopFinder finder = new HavlakLoopFinder(app.cfg, app.lsg);
      finder.findLoops();
    }
    int n = 2;
    for (int parlooptrees = 0; parlooptrees < 10; parlooptrees++) {
      app.cfg.createNode(n + 1);
      app.buildConnect(2, n + 1);
      n += 1;
      for (int i = 0; i < 100; i++) {
        int top = n;
        n = app.buildStraight(n, 1);
        for (int j = 0; j < 25; j++) {
          n = app.buildBaseLoop(n);
        }
        int bottom = app.buildStraight(n, 1);
        app.buildConnect(n, top);
        n = bottom;
      }
      app.buildConnect(n, 1);
    }
  }
}
// Copyright (c) 2011, the Dart project authors.
// All Rights Reserved.

class BB {
    static final int TOP = 0;          // uninitialized
    static final int NONHEADER = 1;    // a regular BB
    static final int REDUCIBLE = 2;    // reducible loop
    static final int SELF = 3;         // single BB loop
    static final int IRREDUCIBLE = 4;  // irreducible loop
    static final int DEAD = 5;         // a dead BB
    static final int LAST = 6;         // Sentinel
}

/**
 * Class HavlakLoopFinder
 *
 * This class encapsulates the complete finder algorithm.
 */
class HavlakLoopFinder {

  CFG cfg;
  LSG lsg;

  static final int UNVISITED = 99999999;
  static final int MAXNONBACKPREDS = (32 * 1024);

    static final Queue<Set<int>> nonBackPreds = new QueueImplementation<Set<int>>();
    static final Queue<Queue<int>> backPreds = new QueueImplementation<Queue<int>>();
    static const Map<BasicBlock, int> number = new HashMap<BasicBlock, int>();
    static int maxSize; // init to 0
    static Array<int> header;
    static Array<BasicBlock> type;
    static Array<int> last;
    static Array<UnionFindNode> nodes;

    HavlakLoopFinder(this.cfg, this.lsg) {}

    /**
     * IsAncestor
     *
     * As described in the paper, determine whether a node 'w' is a
     * "true" ancestor for node 'v'.
     *
     * Dominance can be tested quickly using a pre-order trick
     * for depth-first spanning trees. This is why DFS is the first
     * thing we run below.
     */
    bool isAncestor(int w, int v, Array<int> last) {
      return ((w <= v) && (v <= last[w]));
    }

    /**
     * DFS - Depth-First-Search
     *
     * DESCRIPTION:
     * Simple depth first traversal along out edges with node numbering.
     */
    int doDFS(BasicBlock currentNode, Array<UnionFindNode> nodes,
              Map<BasicBlock, int> number, Array<int> last, int current) {
      nodes[current].initNode(currentNode, current);
      number[currentNode] = current;
      int lastid = current;
      currentNode.getOutEdges().forEach(function f(BasicBlock target) {
        if (number[target] == UNVISITED) {
          lastid = doDFS(target, nodes, number. last. lastid + 1);
        }
      });
      last[number[currentNode]] = lastid;
      return lastid;
    }

    /**
     * findLoops
     *
     * Find loops and build loop forest using Havlak's algorithm, which
     * is derived from Tarjan. Variable names and step numbering has
     * been chosen to be identical to the nomenclature in Havlak's
     * paper (which, in turn, is similar to the one used by Tarjan).
     */
    void findLoops() {
      if (cfg.getStartBasicBlock() == null) {
        return;
      }
      int size = cfg.getNumNodes();
      nonBackPreds.clear();
      backPreds.clear();
      number.clear();
      if (size > maxSize) {
        header = new Array<int>(size);
        type = new Array<BasicBlock>(size);
        last = new Array<int>(size);
        nodes = new Array<UnionFindNode>(size);
        maxSize = size;
      }
      for (int i = 0; i < size; i++) {
        nonBackPreds.addLast(new HashSet<int>());
        backPreds.addLast(new QueueImplementation<int>());
        nodes[i] = new UnionFindNode();
      }
        // Step a:
        //   - initialize all nodes as unvisited.
        //   - depth-first traversal and numbering.
        //   - unreached BB's are marked as dead.
      cfg.getBasicBlocks().forEach(function f(int n, BasicBlock bbIter) {
        number[bbIter] = UNVISITED;
      });
      doDFS(cfg.getStartBasicBlock(), nodes, number, last, 0);
        // Step b:
        //   - iterate over all nodes.
        //
        //   A backedge comes from a descendant in the DFS tree, and non-backedges
        //   from non-descendants (following Tarjan).
        //
        //   - check incoming edges 'v' and add them to either
        //     - the list of backedges (backPreds) or
        //     - the list of non-backedges (nonBackPreds)
      for (int w = 0; w < size; w++) {
        header[w] = 0;
        type[w] = BB.NONHEADER;
        BasicBlock nodeW = nodes[w].getBb();
        if (nodeW == null) {
          type[w] = BB.DEAD;
          continue; // dead BB
        }
        if (nodeW.getNumPred() > 0) {
          nodeW.getInEdges().forEach(function f(BasicBlock nodeV) {
            int v = number.get(nodeV);
            if (v == UNVISITED) {
              continue; // dead node
            }
            if (isAncestor(w, v, last)) {
              backPreds[w].addLast(v);
            } else {
              nonBackPreds[w].add(v);
            }
          });
        }
      }
      // Start node is root of all other loops.
      header[0] = 0;
        // Step c:
        //
        // The outer loop, unchanged from Tarjan. It does nothing except
        // for those nodes which are the destinations of backedges.
        // For a header node w, we chase backward from the sources of the
        // backedges adding nodes to the set P, representing the body of
        // the loop headed by w.
        //
        // By running through the nodes in reverse of the DFST preorder,
        // we ensure that inner loop headers will be processed before the
        // headers for surrounding loops.
      for (int w = size - 1; w >= 0; w--) {
        // this is 'P' in Havlak's paper
        Queue<UnionFindNode> nodePool = new QueueImplementation<UnionFindNode>();
        BasicBlock nodeW = nodes[w].getBb();
        if (nodeW == null) {
          continue; // dead BB
        }
        // Setp d:
        backPreds[w].forEach(function f(int v) {
          if (v != w) {
            nodePool.addLast(nodes[v].findSet());
          } else {
            type[w] = BB.SELF;
          }
        });
        // Copy nodePool to workList.
        Queue<UnionFindNode> workList = new QueueImplementation<UnionFindNode>();
        nodePool.forEach(Function f(UnionFindNode niter) {
          workList.addLast(niter);
        });
        if (nodePool.length != 0) {
          type[w] = BB.REDUCIBLE;
        }
        // work the list...
        while (!workList.isEmpty) {
          UnionFindNode x = workList.getFirst();
          workList.removeFirst();
              // Step e:
              //
              // Step e represents the main difference from Tarjan's method.
              // Chasing upwards from the sources of a node w's backedges. If
              // there is a node y' that is not a descendant of w, w is marked
              // the header of an irreducible loop, there is another entry
              // into this loop that avoids w.
          int nonBackSize = nonBackPreds[x.getDfsNumber()].length;
          if (nonBackSize > MAXNONBACKPREDS) {
                    // The algorithm has degenerated. Break and
                    // return in this case.
            return;
          }
          nonBackPreds[x.getDfsNumber()].forEach(function f(int iter) {
            UnionFindNode y = nodes[iter];
            UnionFindNode ydash = y.findSet();
            if (!isAncestor(w. ydash.getDfsNumber(), last)) {
              type[w] = BB.IRREDUCIBLE;
              nonBackPreds[w].add(ydash.getDfsnumber());
            } else {
              if (ydash.getDfsNumber() != w) {
                if (!nodePool.some(function f(UnionFindNode x) {return x == ydash;})) {
                  workList.addLast(ydash);
                  nodePool.addLast(ydash);
                }
              }
            }
          });
        }
          // Collapse/Unionize nodes in a SCC to a single node
          // For every SCC found, create a loop descriptor and link it in.
          if ((nodePool.length > 0) || (type[w] == BB.SELF)) {
            SimpleLoop loop = lsg.createNewLoop();
            loop.setHeader(nodeW);
            loop.setIsReducible(type[w] != BB.IRREDUCIBLE);
              // At this point, one can set attributes to the loop, such as:
              //
              // the bottom node:
              //    iter  = backPreds[w].begin();
              //    loop bottom is: nodes[iter].node);
              //
              // the number of backedges:
              //    backPreds[w].size()
              //
              // whether this loop is reducible:
              //    type[w] != BasicBlockClass.BB_IRREDUCIBLE
            nodes[w].setLoop(loop);
            nodePool.forEach(function f(UnionFindNode node) {
              // Add nodes to loop descriptor.
              header[node.getDfsNumber()] = w;
              node.union(nodes[w]);
              // Nested loops are not added, but linked together.
              if (node.getLoop() != null) {
                node.getLoop().setParent(loop);
              } else {
                loop.addNode(node.getBb());
              }
            });
            lsg.addLoop(loop);
          }
      }
    }
}

/**
 * Class UnionFindNode
 *
 * The algorithm uses the Union/Find algorithm to collapse
 * complete loops into a single node. These nodes and the
 * corresponding functionality are implemented with this class.
 */
class UnionFindNode {

  UnionFindNode parent;
  BasicBlock bb;
  SimpleLoop loop;
  int dfsNumber;

  UnionFindNode() {}

  void initNode(BasicBlock bb, int dfsNumber) {
    this.parent = this;
      this.bb = bb;
      this.dfsNumber = dfsNumber;
      this.loop = null;
  }

  /**
     * Union/Find Algorithm - The find routine.
     *
     * Implemented with Path Compression (inner loops are only
     * visited and collapsed once, however, deep nests would still
     * result in significant traversals).
   */
  UnionFindNode findSet() {
    Queue<UnionFindNode> nodeList = new QueueImplementation<UnionFindNode>();
    UnionFindNode node = this;
    while (node != node.getParent()) {
      if (node.getParent() != node.getParent().getParent()) {
        nodeList.addLast(node);
      }
      node = node.getParent();
    }
    // Path Compression, all nodes' parents point to the 1st level parent.
    nodeList.forEach(function f(UnionFindNode iter) {
      iter.setParent(node.getParent());
    });
    return node;
  }

  /**
     * Union/Find Algorithm - The union routine.
     *
     * Trivial. Assigning parent pointer is enough,
     * we rely on path compression.
   */
  void union(UnionFindNode basicBlock) {
    setParent(basicBlock);
  }

  UnionFindNode getParent() {
    return parent;
  }

  BasicBlock getBb() {
    return bb;
  }

  SimpleLoop getLoop() {
    return loop;
  }

  int getDfsNumber() {
    return dfsNumber;
  }

  void setParent(UnionFindNode parent) {
    this.parent = parent;
  }

  void setLoop(SimpleLoop loop) {
    this.loop = loop;
  }
}
// Copyright (c) 2011, the Dart project authors.
// All Rights Reserved.

/**
 * Class LoopStructureGraph
 *
 * Loop Structure Graph - Scaffold Code
 *
 * Maintain loop structure for a given CFG.
 *
 * Two values are maintained for this loop graph, depth, and nesting level.
 * For example:
 *
 * loop        nesting level    depth
 *----------------------------------------
 * loop-0      2                0
 *   loop-1    1                1
 *   loop-3    1                1
 *     loop-2  0                2
 */
class LSG {

  SimpleLoop root;
  Array<SimpleLoop> loops;
  int loopCounter;

  LSG() : loopCounter = 0, loops = new GrowableArray<SimpleLoop>(), root = new SimpleLoop() {
    root.setNestingLevel(0);
    root.setCounter(loopCounter++);
    addLoop(root);
  }

  SimpleLoop createNewLoop() {
    SimpleLoop loop = new SimpleLoop();
    loop.setCounter(loopCounter++);
    return loop;
  }

  addLoop(SimpleLoop loop) {
    loops.add(loop);
  }

  void dump() {
    dumpRec(root, 0);
  }

  void dumpRec(SimpleLoop loop, int indent) {
    loop.dump(indent);
    loop.getChildren().forEach(
      function f(SimpleLoop liter) {
        dumpRec(liter, indent + 1);
      });
  }

  void calculateNestingLevel() {
    // link up all 1st level loops to artificial root node
    loops.forEach(function f(SimpleLoop liter) {
      if (liter.isRoot()) {
        continue;
      } else if (liter.getParent() == null) {
        liter.setParent(root);
      }
    });
    // recursively traverse the tree and assign levels
    calculateNestingLevelRec(root, 0);
  }

  void calculateNestingLevelRec(SimpleLoop loop, int depth) {
    loop.setDepthLevel(depth);
    loop.getChildren().forEach(
      function f(SimpleLoop liter) {
          calculateNestingLevelRec(liter, depth + 1);
          int m = loop.getNestingLevel();
          int n = 1 + liter.getNestingLevel();
          int k;
          if (m > n) k=m; else k=n;
          loop.setNestingLevel(k);
    });
  }

  int getNumLoops() {
    return loops.length;
  }

  SimpleLoop getRoot() {
    return root;
  }
}

/**
 * Class SimpleLoop
 *
 * Basic representation of loops, a loop has an entry point,
 * one or more exit edges, a set of basic blocks, and potentially
 * an outer loop - a "parent" loop.
 *
 * Furthermore, it can have any set of properties, e.g.,
 * it can be an irreducible loop, have control flow, be
 * a candidate for transformations, and what not.
 */
class SimpleLoop {

    Set<BasicBlock> basicBlocks;
    Set<SimpleLoop> children;
  SimpleLoop parent;
  BasicBlock header;
  bool isRoot;
  bool isReducible;
  int counter;
  int nestingLevel;
  int depthLevel;

  SimpleLoop() {
    parent = null;
    isRoot = false;
    isReducible = true;
    nestingLevel = 0;
    depthLevel = 0;
    basicBlocks = new HashSet<BasicBlock>();
    children = new HashSet<SimpleLoop>();
  }

  void addNode(BasicBlock bb) {
    basicBlocks.add(bb);
  }

  void addChild(SimpleLoop loop) {
    children.add(loop);
  }

  void dump(int indent) {
    for (int i = 0; i < indent; i++) {
      Out.write("  ");
    }
    Out.write("loop-");
    Out.write(counter);
    Out.write(" nest: ");
    Out.write(nestingLevel);
    Out.write(" depth: ");
    Out.write(depthLevel);
    if (isReducible)
      Out.write(" (Irrudicible)");
    if (!getChildren().isEmpty) {
      Out.write("Children:");
      getChildren.forEach(Function f(SimpleLoop loop) {
        Out.write("loop-");
        Out.write(loop.getCounter());
        Out.write(" ");
      });
    }
    if (!basicBlocks.isEmpty) {
      Out.write("(");
      basicBlocks.forEach(function f(BasicBlock bb) {
        Out.write("BB#");
        Out.write(bb.getName());
        if (header == bb)
          Out.write("*");
        Out.write(" ");
      });
      Out.write(")"); // error with \b) ?
    }
    Out.writeln(""); // error with <br>
  }

  Set<SimpleLoop> getChildren() {
    return children;
  }

  SimpleLoop getParent() {
    return parent;
  }

  int getNestingLevel() {
    return nestingLevel;
  }

  int getCounter() {
    return counter;
  }

//  boolean isRoot() {
//    return isRoot;
//  }

  void setParent(SimpleLoop parent) {
    this.parent = parent;
    this.parent.addChildLoop(this);
  }

  void setHeader(BasicBlock bb) {
    basicBlocks.add(bb);
    header = bb;
  }

  void setIsRoot() {
    isRoot = true;
  }

  void setCounter(int value) {
    counter = value;
  }

  void setNestingLevel(int level) {
    nestingLevel = level;
    if (level == 0)
      setIsRoot();
  }

  void setDepthLevel(int level) {
    depthLevel = level;
  }

  void setIsReducible(bool isReducible) {
    this.isReducible = isReducible;
  }
}
class Out {

    static void write(String str) {
        Havlak.window.document.write(str);
    }

    static void writeln(String str) {
        Havlak.window.document.writeln(str);
    }
}
