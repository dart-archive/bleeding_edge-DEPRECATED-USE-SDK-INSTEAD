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
package com.google.dart.indexer.index;

public class IndexSessionStats {
  private final Object storageStats;
  private final long timeSpentParsing;

  public IndexSessionStats(Object storageStats, long timeSpentParsing) {
    if (storageStats == null) {
      throw new NullPointerException("storageStats is null");
    }
    this.storageStats = storageStats;
    this.timeSpentParsing = timeSpentParsing;
  }

  public Object getStorageStats() {
    return storageStats;
  }

  public long getTimeSpentParsing() {
    return timeSpentParsing;
  }
}
