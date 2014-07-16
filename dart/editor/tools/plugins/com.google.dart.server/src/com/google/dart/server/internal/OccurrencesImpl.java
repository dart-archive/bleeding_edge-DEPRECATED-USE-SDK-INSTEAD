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
import com.google.dart.server.Occurrences;
import com.google.dart.server.utilities.general.ObjectUtilities;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * A concrete implementation of {@link Occurrences}.
 * 
 * @coverage dart.server
 */
public class OccurrencesImpl implements Occurrences {

  private final Element element;
  private final int length;
  private final int[] offsets;

  public OccurrencesImpl(Element element, int length, int[] offsets) {
    this.element = element;
    this.length = length;
    this.offsets = offsets;
  }

  @Override
  public boolean contains(int _offset) {
    for (int offset : offsets) {
      if (offset <= _offset && _offset < offset + length) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OccurrencesImpl) {
      OccurrencesImpl other = (OccurrencesImpl) obj;
      return ObjectUtilities.equals(other.element, element) && other.length == length
          && Arrays.equals(other.offsets, offsets);
    }
    return false;
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int[] getOffsets() {
    return offsets;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[element=");
    builder.append(element.toString());
    builder.append(", length=");
    builder.append(length);
    builder.append(", offsets=");
    builder.append(StringUtils.join(offsets, ", "));
    builder.append("]");
    return builder.toString();
  }

}
