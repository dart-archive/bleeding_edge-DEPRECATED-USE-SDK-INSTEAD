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
package com.google.dart.indexer.workspace.index;

public abstract class QueueState {

  public static final QueueState NORMAL = new QueueState("NORMAL") {

    @Override
    public boolean isAbnormal() {
      return false;
    }

  };

  public static final QueueState NEEDS_RESYNC = new QueueState("NEEDS_RESYNC") {

  };

  public static final QueueState NEEDS_REBUILD = new QueueState("NEEDS_REBUILD") {

  };

  private final String name;

  private QueueState(String name) {
    this.name = name;
  }

  public boolean isAbnormal() {
    return true;
  }

  @Override
  public String toString() {
    return name;
  }

}
