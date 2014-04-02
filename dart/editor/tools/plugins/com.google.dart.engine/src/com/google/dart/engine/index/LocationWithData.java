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

package com.google.dart.engine.index;

import com.google.dart.engine.element.Element;

/**
 * {@link Location} with attached data.
 */
public class LocationWithData<D> extends Location {
  private final D data;

  public LocationWithData(Location location, D data) {
    super(location.getElement(), location.getOffset(), location.getLength());
    this.data = data;
  }

  private LocationWithData(Element element, int offset, int length, D data) {
    super(element, offset, length);
    this.data = data;
  }

  /**
   * @return the attached data.
   */
  public D getData() {
    return data;
  }

  @Override
  public Location newClone() {
    return new LocationWithData<D>(getElement(), getOffset(), getLength(), data);
  }
}
