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

import com.google.dart.engine.source.Source;
import com.google.dart.server.NavigationTarget;

/**
 * A concrete implementation of {@link NavigationTarget}.
 * 
 * @coverage dart.server.local
 */
public class NavigationTargetImpl implements NavigationTarget {
  private final Source source;
  private final String elementId;
  private final int offset;
  private final int length;

  public NavigationTargetImpl(Source source, String elementId, int offset, int length) {
    this.source = source;
    this.elementId = elementId;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public String getElementId() {
    return elementId;
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
  public Source getSource() {
    return source;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", source=");
    builder.append(source);
    builder.append(", element=");
    builder.append(elementId);
    builder.append("]");
    return builder.toString();
  }
}
