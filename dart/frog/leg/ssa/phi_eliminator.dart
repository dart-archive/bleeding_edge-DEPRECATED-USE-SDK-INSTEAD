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
    HBasicBlock current = predecessor;
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
          value.setGenerateAtUseSite();
        }
        return store;
      }
      current = current.dominator;
    } while (current != dominator && !current.isLoopHeader());

    // We could not get to the definition, just put the store in the
    // predecessor.
    assert(store !== null);
    predecessor.addAtExit(store);
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
        if (phi.element.kind === ElementKind.PARAMETER) {
          // No need to generate the local, so move it out of the
          // graph.
          entry.detach(local);
        }
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

      // Storing a phi referencing the same element can be safely eliminated.
      if ((value is HPhi)
          && (local.element !== null)
          && (value.dynamic.element === local.element)) continue;

      HStore store = addStore(predecessors[i],
                              currentBlock.dominator,
                              local,
                              value);

      if (store != null) {
        if (local.declaredBy === local) {
          HBasicBlock storeBlock = store.block;
          // Check if the store occurs in or just after the entry block.
          if (storeBlock === entry || storeBlock === entry.successors[0]) {
            entry.detach(local);
            local.declaredBy = store;
          }
        }
        stores.add(store);
      }
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
