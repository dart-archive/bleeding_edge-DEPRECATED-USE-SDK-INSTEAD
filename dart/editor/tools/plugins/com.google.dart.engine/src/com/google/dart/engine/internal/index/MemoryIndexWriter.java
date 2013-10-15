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

import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.internal.index.MemoryIndexStoreImpl.ElementRelationKey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * Helper to write {@link MemoryIndexStoreImpl} to {@link OutputStream}.
 * 
 * @coverage dart.engine.index
 */
class MemoryIndexWriter {
  static int FILE_VERSION_NUMBER = 1;

  private final MemoryIndexStoreImpl impl;
  private final AnalysisContext context;
  private final DataOutputStream dos;

  MemoryIndexWriter(MemoryIndexStoreImpl impl, AnalysisContext context, OutputStream output) {
    this.impl = impl;
    this.context = context;
    this.dos = new DataOutputStream(output);
  }

  /**
   * Write to the given {@link OutputStream}.
   */
  public void write() throws IOException {
    dos.writeInt(FILE_VERSION_NUMBER);
    // prepare Element(s) to write relations for
    List<ElementRelationKey> keysToWrite = Lists.newArrayList();
    for (ElementRelationKey key : impl.keyToLocations.keySet()) {
      Element element = key.element;
      if (!isElementOfContext(element)) {
        continue;
      }
      keysToWrite.add(key);
    }
    // do write Element(s)
    dos.writeInt(keysToWrite.size());
    for (ElementRelationKey key : keysToWrite) {
      // write key
      writeElementLocation(key.element);
      dos.writeUTF(key.relationship.getIdentifier());
      // prepare Location(s) to write
      List<Location> locationsToWrite = Lists.newArrayList();
      Set<Location> contributedLocations = impl.keyToLocations.get(key);
      for (Location location : contributedLocations) {
        // TODO(scheglov) restore when we will share Elements between contexts
//        Element locationElement = location.getElement();
//        if (!isElementOfContext(locationElement)) {
//          continue;
//        }
        locationsToWrite.add(location);
      }
      // write Location(s)
      dos.writeInt(locationsToWrite.size());
      for (Location location : locationsToWrite) {
        writeElementLocation(location.getElement());
        dos.writeInt(location.getOffset());
        dos.writeInt(location.getLength());
      }
    }
  }

  /**
   * @return {@code true} if given {@link Element} belongs to the {@link AnalysisContext} which we
   *         are currently writing.
   */
  private boolean isElementOfContext(Element element) {
    return element.getContext() == context;
  }

  /**
   * Writes {@link ElementLocation} of the given {@link Element}.
   */
  private void writeElementLocation(Element element) throws IOException {
    dos.writeUTF(element.getLocation().getEncoding());
  }
}
