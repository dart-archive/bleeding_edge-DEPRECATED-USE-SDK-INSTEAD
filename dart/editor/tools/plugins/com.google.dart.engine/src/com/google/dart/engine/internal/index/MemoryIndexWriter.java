/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.engine.internal.index;

import com.google.dart.engine.context.AnalysisContext;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper to write {@link MemoryIndexStoreImpl} to {@link OutputStream}.
 */
class MemoryIndexWriter {
  /**
   * The version number of the file format being generated.
   */
  private static int FILE_VERSION_NUMBER = 1;

//  private final MemoryIndexStoreImpl impl;
//  private int nextElementId = 0;
//  private final Map<Source, Integer> sourceToId = Maps.newHashMap();
//  private final Map<Element, Integer> elementToId = Maps.newHashMap();

//  private final AnalysisContext context;
  private final DataOutputStream dos;

  MemoryIndexWriter(MemoryIndexStoreImpl impl, AnalysisContext context, OutputStream output) {
//    this.impl = impl;
//    this.context = context;
    this.dos = new DataOutputStream(output);
  }

  /**
   * Write to the given {@link OutputStream}.
   */
  public void write() throws IOException {
    dos.writeInt(FILE_VERSION_NUMBER);
    // TODO(scheglov)
//    // write Source table
//    {
//      int nextId = 0;
//      for (Source source : impl.sources) {
//        int id = nextId++;
//        sourceToId.put(source, id);
//        String encoding = source.getEncoding();
//        dos.writeUTF(encoding);
//        dos.writeInt(id);
//      }
//    }
//    // prepare Element table
//    for (Entry<Element, Map<Relationship, List<ContributedLocation>>> entryRels : impl.relationshipMap.entrySet()) {
//      addElement(entryRels.getKey());
//      for (Entry<Relationship, List<ContributedLocation>> entryRel : entryRels.getValue().entrySet()) {
//        for (ContributedLocation location : entryRel.getValue()) {
//          addElement(location.getLocation().getElement());
//        }
//      }
//    }
//    for (Entry<Source, List<Element>> entry : impl.sourceToDeclarations.entrySet()) {
//      for (Element element : entry.getValue()) {
//        addElement(element);
//      }
//    }
//    for (Entry<Source, List<ContributedLocation>> entry : impl.sourceToLocations.entrySet()) {
//      for (ContributedLocation location : entry.getValue()) {
//        addElement(location.getLocation().getElement());
//      }
//    }
  }

//  private void addElement(Element element) {
//    if (!elementToId.containsKey(element)) {
//      int id = nextElementId++;
//      elementToId.put(element, id);
//    }
//  }
}
