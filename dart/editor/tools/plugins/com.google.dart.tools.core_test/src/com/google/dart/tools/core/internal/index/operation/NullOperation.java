/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.operation;

import com.google.dart.tools.core.index.Resource;

public class NullOperation implements IndexOperation {

  private final boolean isQuery;

  public NullOperation() {
    this(false);
  }

  public NullOperation(boolean isQuery) {
    this.isQuery = isQuery;
  }

  @Override
  public boolean isQuery() {
    return isQuery;
  }

  @Override
  public void performOperation() {
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return false;
  }
}
