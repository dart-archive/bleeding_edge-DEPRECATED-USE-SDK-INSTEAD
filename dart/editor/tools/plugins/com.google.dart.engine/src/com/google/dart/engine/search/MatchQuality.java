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
package com.google.dart.engine.search;

/**
 * Instances of the enum <code>MatchQuality</code> represent the quality of a match by representing
 * the conditions under which a match is valid.
 * 
 * @coverage dart.engine.search
 */
public enum MatchQuality {
  /**
   * The match is an exact match for whatever was being searched for.
   */
  EXACT,

  /**
   * The match is an inexact match, unresolved reference to the element with the same name.
   */
  NAME;

  /**
   * Return whichever of this quality or the given quality is "better", where "better" is loosely
   * defined to be the quality closest to exact.
   * 
   * @param quality the quality being compared to this quality
   * @return the higher of the two given qualities
   */
  public MatchQuality max(MatchQuality quality) {
    //
    // Note that the implementation of this method currently depends on the
    // order of the declarations being equal to the sort order defined by this
    // method, with the most exact quality being first.
    //
    if (ordinal() <= quality.ordinal()) {
      return this;
    } else {
      return quality;
    }
  }
}
