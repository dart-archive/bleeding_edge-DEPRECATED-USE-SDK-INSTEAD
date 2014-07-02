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

import com.google.dart.server.Element;
import com.google.dart.server.NavigationRegion;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link NavigationRegion}.
 * 
 * @coverage dart.server
 */
public class NavigationRegionImpl extends SourceRegionImpl implements NavigationRegion {
  private Element[] elements;

  public NavigationRegionImpl(int offset, int length, Element[] elements) {
    super(offset, length);
    this.elements = elements;
  }

  @Override
  public Element[] getTargets() {
    return elements;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(super.toString());
    builder.append(" -> [");
    builder.append(StringUtils.join(elements, ", "));
    builder.append("]");
    return builder.toString();
  }
}
