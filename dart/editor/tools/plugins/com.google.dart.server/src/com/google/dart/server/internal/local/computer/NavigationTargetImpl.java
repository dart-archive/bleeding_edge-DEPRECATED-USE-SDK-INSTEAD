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

import com.google.dart.server.Element;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.NavigationTarget;

/**
 * A concrete implementation of {@link NavigationRegion}.
 * 
 * @coverage dart.server.local
 */
public class NavigationTargetImpl extends SourceRegionImpl implements NavigationTarget {
  private final String filePath;
  private final Element element;

  public NavigationTargetImpl(String filePath, int offset, int length, Element element) {
    super(offset, length);
    this.filePath = filePath;
    this.element = element;
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public String getFile() {
    return filePath;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("file=");
    builder.append(getFile());
    builder.append(", offset=");
    builder.append(getOffset());
    builder.append(", length=");
    builder.append(getLength());
    builder.append(", element=");
    builder.append(getElement());
    builder.append("]");
    return builder.toString();
  }

}
