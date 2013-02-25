/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.storage.paged;

public class PagedIndexStatistics {
  public final MappingStats fileStats = new MappingStats();

  public final MappingStats locationStats = new MappingStats();

  public void resolve(int pageSize) {
    fileStats.resolve(pageSize);
    locationStats.resolve(pageSize);
  }

  @Override
  public String toString() {
    return "\nFILE " + fileStats.toString() + "\nLOCA " + locationStats.toString() + "\n";
  }
}
