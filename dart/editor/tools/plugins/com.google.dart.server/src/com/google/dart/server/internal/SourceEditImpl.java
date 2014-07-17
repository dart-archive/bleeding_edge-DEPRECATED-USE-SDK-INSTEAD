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
package com.google.dart.server.internal;

import com.google.dart.server.SourceEdit;
import com.google.dart.server.utilities.general.ObjectUtilities;

/**
 * A concrete implementation of {@link SourceEdit}.
 * 
 * @coverage dart.server
 */
public class SourceEditImpl implements SourceEdit {

  private final int offset;
  private final int length;
  private final String replacement;

  public SourceEditImpl(int offset, int length, String replacement) {
    this.offset = offset;
    this.length = length;
    this.replacement = replacement;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SourceEditImpl) {
      SourceEditImpl other = (SourceEditImpl) obj;
      return ObjectUtilities.equals(other.replacement, replacement) && other.offset == offset
          && other.length == length;
    }
    return false;
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
  public String getReplacement() {
    return replacement;
  }

  @Override
  public int hashCode() {
    int hash = replacement.hashCode();
    hash = hash * 31 + offset;
    hash = hash * 31 + length;
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", replacement=");
    builder.append(replacement);
    builder.append("]");
    return builder.toString();
  }

}
