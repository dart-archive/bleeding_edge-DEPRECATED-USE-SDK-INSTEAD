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
package com.google.dart.server.internal.shared;

import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.server.Location;

/**
 * A concrete implementation of {@link Location}.
 * 
 * @coverage dart.server.shared
 */
public class LocationImpl implements Location {

  private final String file;
  private final int offset;
  private final int length;

  public LocationImpl(String file, int offset, int length) {
    this.file = file;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LocationImpl) {
      LocationImpl other = (LocationImpl) obj;
      return ObjectUtilities.equals(other.file, file) && other.offset == offset
          && other.length == length;
    }
    return false;
  }

  @Override
  public String getFile() {
    return file;
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
    builder.append("[file=");
    builder.append(file);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append("]");
    return builder.toString();
  }

}
