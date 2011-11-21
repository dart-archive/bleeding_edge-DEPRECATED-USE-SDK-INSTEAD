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

  void addStore(HBasicBlock predecessor,
                HBasicBlock dominator,
                HLocal local,
                HInstruction value) {
    HStore store = new HStore(local, value);
    if (currentBlock.isLoopHeader()) {
      // The phi is a loop phi, just add the store at the end of the
      // predecessor.
      predecessor.addAtExit(store);
    } else if (value.generateAtUseSite()) {
      // The temporary will not be introduced, so no need to push the
      // assignment to the definition.
      predecessor.addAtExit(store);
    } else {
      HBasicBlock current = predecessor;
      do {
        if (current.contains(value)) {
          current.addAfter(value, store);
          if (value.usedBy.length == 2) { // the store and the phi.
            value.setGenerateAtUseSite();;
          }
          return;
        }
        current = current.dominator;
      } while (current != dominator && !current.isLoopHeader());

      // We could not get to the definition, just put the store in the
      // predecessor.
      predecessor.addAtExit(store);
    }
  }

  visitBasicBlock(HBasicBlock block) {
    currentBlock = block;
    block.forEachPhi((phi) { visitPhi(phi); });
  }

  visitPhi(HPhi phi) {
    assert(phi !== null);
    HLocal local = new HLocal(phi.element);
    entry.addAtEntry(local);

    List<HBasicBlock> predecessors = currentBlock.predecessors;
    for (int i = 0, len = predecessors.length; i < len; i++) {
      addStore(predecessors[i], currentBlock.dominator, local, phi.inputs[i]);
    }

    HLoad load = new HLoad(local);
    currentBlock.addAtEntry(load);
    currentBlock.rewrite(phi, load);
    currentBlock.removePhi(phi);
    // TODO(ngeoffray): handle loops.
    if (!currentBlock.isLoopHeader()) load.setGenerateAtUseSite();
  }
}
