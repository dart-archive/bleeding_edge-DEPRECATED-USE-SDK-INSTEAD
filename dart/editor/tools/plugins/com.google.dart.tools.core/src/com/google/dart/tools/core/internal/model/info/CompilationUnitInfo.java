/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.DartTypeImpl;
import com.google.dart.tools.core.model.DartElement;

import java.util.ArrayList;

/**
 * Instances of the class <code>CompilationUnitInfo</code> maintain the cached data shared by all
 * equal compilation units.
 */
public class CompilationUnitInfo extends OpenableElementInfo {

  /**
   * The time stamp of the original resource at the time this element was opened or last updated.
   */
  private long timestamp;

  /**
   * A flag indicating whether this compilation unit defines a library.
   */
  private boolean definesLibrary = false;

  /**
   * Return <code>true</code> if this compilation unit defines a library.
   * 
   * @return <code>true</code> if this compilation unit defines a library
   */
  public boolean getDefinesLibrary() {
    return definesLibrary;
  }

  /**
   * Return the time stamp of the original resource at the time this element was opened or last
   * updated.
   * 
   * @return the time stamp of the original resource
   */
  public long getTimestamp() {
    return timestamp;
  }

  public DartTypeImpl[] getTypes() {
    ArrayList<DartTypeImpl> types = new ArrayList<DartTypeImpl>();
    for (DartElement child : getChildren()) {
      if (child instanceof DartTypeImpl) {
        types.add((DartTypeImpl) child);
      }
    }
    return types.toArray(new DartTypeImpl[types.size()]);
  }

  /**
   * Set whether this compilation unit defines a library to the given value.
   * 
   * @param defines <code>true</code> if this compilation unit defines a library
   */
  public void setDefinesLibrary(boolean defines) {
    definesLibrary = defines;
  }

  /**
   * Set the time stamp of the original resource to the given time stamp.
   * 
   * @param newTimestamp the time stamp of the original resource
   */
  public void setTimestamp(long newTimestamp) {
    timestamp = newTimestamp;
  }
}
