// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaPhiEliminator extends HGraphVisitor {
  HBasicBlock entry;
  HBasicBlock currentBlock;

  // A map from element to local, to avoid creating multiple locals
  // for one variable declaration in Dart source.
  Map<Element, HLocal> namedLocals;

  visitGraph(HGraph graph) {
    entry = graph.entry;
    namedLocals = new Map<Element, HLocal>();
    visitDominatorTree(graph);
  }

  HStore addStore(HBasicBlock predecessor,
                  HBasicBlock dominator,
                  HLocal local,
                  HInstruction value) {
    HStore store = new HStore(local, value);
    if (value.generateAtUseSite()) {
      // The temporary will not be introduced, so no need to push the
      // assignment to the definition.
      predecessor.addAtExit(store);
    } else {
      HBasicBlock current = predecessor;
      do {
        if (value.block === current) {
          if (value is HPhi) {
            current.addAtEntry(store);
          } else {
            current.addAfter(value, store);
          }
          if (value.usedBy.length == 2) { // The store and the phi.
            value.setGenerateAtUseSite();
          }
          return store;
        }
        current = current.dominator;
      } while (current != dominator && !current.isLoopHeader());

      // We could not get to the definition, just put the store in the
      // predecessor.
      predecessor.addAtExit(store);
    }
    return store;
  }

  visitBasicBlock(HBasicBlock block) {
    currentBlock = block;
    List<HLoad> loads = <HLoad>[];
    block.forEachPhi((phi) { visitPhi(phi, loads); });
  }

  visitPhi(HPhi phi, List<HLoad> loads) {
    assert(phi !== null);
    HLocal local;
    if (phi.element != null) {
      // If the phi represents a variable in Dart source, check if we
      // already introduced a local for it.
      local = namedLocals.putIfAbsent(phi.element, () {
        HLocal local = new HLocal(phi.element);
        entry.addAtEntry(local);
        return local;
      });
    } else {
      local = new HLocal(null);
      entry.addAtEntry(local);
    }


    List<HBasicBlock> predecessors = currentBlock.predecessors;
    List<HStore> stores = <HStore>[];

    for (int i = 0, len = predecessors.length; i < len; i++) {
      HInstruction value = phi.inputs[i];

      // Storing a load of itself to a local can be safely eliminated.
      if (value is HLoad && value.dynamic.local === local) continue;

        HStore store = addStore(predecessors[i],
                                currentBlock.dominator,
                                local,
                                value);
        stores.add(store);
    }

    // We propagate the type of the phi to the load instruction rather
    // than the local because we may end up sharing a single local
    // between different phis of different types.
    HLoad load = new HLoad(local, phi.type);
    loads.add(load);

    currentBlock.addAtEntry(load);
    currentBlock.rewrite(phi, load);
    currentBlock.removePhi(phi);

    if (!currentBlock.isLoopHeader() || !hasLoopPhiAsInput(stores, loads)) {
      load.setGenerateAtUseSite();
    }
  }

  bool hasLoopPhiAsInput(List<HStore> stores, List<HLoad> loads) {
    // [stores] contains the stores of a specific phi.
    // [loads] contains the phis that were converted to loads.
    assert(currentBlock.isLoopHeader());
    for (HStore store in stores) {
      HInstruction value = store.value;
      if (value is HPhi && value.block == currentBlock) {
        return true;
      } else if (value is HLoad && loads.indexOf(value) != -1) {
        return true;
      }
    }
    return false;
  }
}
