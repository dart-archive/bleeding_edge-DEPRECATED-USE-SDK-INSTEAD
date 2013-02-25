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
package com.google.dart.indexer.pagedstorage.treestore;

public class TreeLeaf {
  private final String[] path;
  private final PageRecPos pos;

  public TreeLeaf(String[] path, PageRecPos pos) {
    if (path == null) {
      throw new NullPointerException("path is null");
    }
    if (pos == null) {
      throw new NullPointerException("pos is null");
    }
    this.path = path;
    this.pos = pos;
  }

  public String[] getPath() {
    return path;
  }

  public PageRecPos getPos() {
    return pos;
  }
}
