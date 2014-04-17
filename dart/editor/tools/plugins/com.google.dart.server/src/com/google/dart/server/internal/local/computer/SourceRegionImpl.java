/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.dart.server.SourceRegion;

/**
 * A concrete implementation of {@link SourceRegion}.
 * 
 * @coverage dart.server.local
 */
public class SourceRegionImpl implements SourceRegion {
  private final int offset;
  private final int length;

  public SourceRegionImpl(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean containsInclusive(int x) {
    return offset <= x && x <= offset + length;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append("]");
    return builder.toString();
  }
}
