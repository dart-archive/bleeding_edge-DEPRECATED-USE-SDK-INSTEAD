// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SsaPhiEliminator extends HGraphVisitor {
  HBasicBlock entry;
  HBasicBlock currentBlock;
  final bool bailoutVersion;

  SsaPhiEliminator(WorkItem work) : bailoutVersion = work.isBailoutVersion();

  visitGraph(HGraph graph) {
    entry = graph.entry;
    visitDominatorTree(graph);
  }

  HStore addStore(HBasicBlock predecessor,
                  HBasicBlock dominator,
                  HLocal local,
                  HInstruction value) {
    HStore store = new HStore(local, value);
    HBasicBlock current = predecessor;
    if (!bailoutVersion) {
      // For bailout methods, we cannot optimize a store, because it
      // needs to happen at the very end. Otherwise, a bailout point
      // will not be able to have the right value for the local.
      do {
        if (value.block === current) {
          HInstruction insertBefore;
          if (value is HPhi) {
            insertBefore = current.first;
          } else {
            insertBefore = value.next;
          }

          // Check if this store is redundant.
          // If there is a store already on that local, it must be the next
          // instruction, because we insert stores right next to
          // their definition.
          if (insertBefore is HStore && insertBefore.dynamic.local === local) {
            assert(store.value === value);
            store = null;
          } else {
            current.addBefore(insertBefore, store);
          }

          // Check if it's only the store and the phi that uses value.
          // This is also valid if the store is null: it checks whether
          // the existing store and the current phi are the only users.
          if (value.usedBy.length == 2) {
            value.tryGenerateAtUseSite();
          }
          return store;
        }
        if (current.isLoopHeader()) break;
        current = current.dominator;
      } while (current != dominator);
    }

    // We could not get to the definition, just put the store in the
    // predecessor.
    assert(store !== null);
    predecessor.addAtExit(store);
    return store;
  }

  visitBasicBlock(HBasicBlock block) {
    currentBlock = block;
    HPhi phi = block.phis.first;
    while (phi != null) {
      HPhi next = phi.next;
      visitPhi(phi);
      phi = next;
    }
  }

  visitPhi(HPhi phi) {
    assert(phi !== null);
    if (phi.isLogicalOperator()) return;
    HLocal local;
    if (phi.element != null) {
      local = new HLocal(phi.element);
      entry.addAtEntry(local);
      if (phi.element.kind === ElementKind.PARAMETER) {
        // No need to generate the local, so move it out of the
        // graph.
        entry.detach(local);
      }
    } else {
      local = new HLocal(null);
      entry.addAtEntry(local);
    }


    List<HBasicBlock> predecessors = currentBlock.predecessors;

    for (int i = 0, len = predecessors.length; i < len; i++) {
      HInstruction value = phi.inputs[i];

      // Storing a load of itself to a local can be safely eliminated.
      if (value is HLoad && value.dynamic.local === local) continue;

      HStore store = addStore(predecessors[i],
                              currentBlock.dominator,
                              local,
                              value);

      if (store != null) {
        if (local.declaredBy === local) {
          HBasicBlock storeBlock = store.block;
          // Check if the store occurs in or just after the entry block.
          if (storeBlock === entry || storeBlock === entry.successors[0]) {
            if (phi.element != null &&
                phi.element.kind !== ElementKind.PARAMETER) {
              entry.detach(local);
            }
            local.declaredBy = store;
          }
        }
      }
    }

    // We propagate the type of the phi to the load instruction rather
    // than the local because we may end up sharing a single local
    // between different phis of different types.
    HLoad load = new HLoad(local, phi.type);
    currentBlock.addAtEntry(load);
    currentBlock.rewrite(phi, load);
    currentBlock.removePhi(phi);

    if (!currentBlock.isLoopHeader()) load.tryGenerateAtUseSite();
  }
}
