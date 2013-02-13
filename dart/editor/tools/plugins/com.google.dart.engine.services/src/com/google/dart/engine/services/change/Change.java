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
package com.google.dart.engine.services.change;

import com.google.common.base.Preconditions;

/**
 * Describes some abstract operation to perform.
 * <p>
 * {@link Change} implementations in "services" plugin cannot perform operation themselves, they are
 * just descriptions of operation. Actual operation should be performed by client.
 */
public abstract class Change {
  private final String name;

  public Change(String name) {
    Preconditions.checkNotNull(name);
    this.name = name;
  }

  /**
   * @return the human readable name of this {@link Change}.
   */
  public String getName() {
    return name;
  }
}
