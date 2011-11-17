// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaPhiEliminator extends HBaseVisitor {
  HBasicBlock entry;

  visitGraph(HGraph graph) {
    entry = graph.entry;
    visitDominatorTree(graph);
    // TODO(ngeoffray): Visit just phis once phis are in their own
    // instruction list.
  }

  visitPhi(HPhi phi) {
    HLocal local = new HLocal(phi.element);
    entry.addAtEntry(local);

    List<HBasicBlock> predecessors = currentBlock.predecessors;
    for (int i = 0, len = predecessors.length; i < len; i++) {
      predecessors[i].addAtExit(new HStore(local, phi.inputs[i]));
    }

    HLoad load = new HLoad(local);
    currentBlock.addAfter(phi, load);
    currentBlock.rewrite(phi, load);

    // Let the codegen know that this instruction does not need to be
    // generated.
    load.setGenerateAtUseSite();

    currentBlock.remove(phi);
  }
}
