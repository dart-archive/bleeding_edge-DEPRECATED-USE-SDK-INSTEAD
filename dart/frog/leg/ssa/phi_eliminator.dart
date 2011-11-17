// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaPhiEliminator extends HGraphVisitor {
  HBasicBlock entry;
  HBasicBlock currentBlock;

  visitGraph(HGraph graph) {
    entry = graph.entry;
    visitDominatorTree(graph);
  }

  visitBasicBlock(HBasicBlock block) {
    currentBlock = block;
    block.forEachPhi((phi) => visitPhi(phi));
  }

  visitPhi(HPhi phi) {
    assert(phi !== null);
    HLocal local = new HLocal(phi.element);
    entry.addAtEntry(local);

    List<HBasicBlock> predecessors = currentBlock.predecessors;
    for (int i = 0, len = predecessors.length; i < len; i++) {
      predecessors[i].addAtExit(new HStore(local, phi.inputs[i]));
    }

    HLoad load = new HLoad(local);
    currentBlock.addAtEntry(load);
    currentBlock.rewrite(phi, load);

    // Let the codegen know that this instruction does not need to be
    // generated.
    load.setGenerateAtUseSite();

    currentBlock.removePhi(phi);
  }
}
