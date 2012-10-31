/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.theme;

public class Color {

  private static final String ZERO = "0"; // $NON-NLS-1$
  private static final String COMMA = ","; // $NON-NLS-1$
  private static final String HASH = "#"; // $NON-NLS-1$
  private static final String NONE = ""; // $NON-NLS-1$

  private Integer r = new Integer(0);
  private Integer g = new Integer(0);
  private Integer b = new Integer(0);

  public Color(String value) {
    if (value != null) {
      if (value.startsWith(HASH)) { // $NON-NLS-1$
        r = Integer.parseInt(value.substring(1, 3), 16);
        g = Integer.parseInt(value.substring(3, 5), 16);
        b = Integer.parseInt(value.substring(5, 7), 16);
      }
    }
  }

  public String asHex() {
    String hexr = Integer.toHexString(r).toUpperCase();
    String hexg = Integer.toHexString(g).toUpperCase();
    String hexb = Integer.toHexString(b).toUpperCase();
    return HASH + (hexr.length() == 2 ? hexr : ZERO + hexr) + NONE
        + (hexg.length() == 2 ? hexg : ZERO + hexg) + NONE
        + (hexb.length() == 2 ? hexb : ZERO + hexb);
  }

  public String asRGB() {
    return r + COMMA + g + COMMA + b;
  }

  public Integer getB() {
    return b;
  }

  public Integer getG() {
    return g;
  }

  public Integer getR() {
    return r;
  }

  @Override
  public String toString() {
    return r + COMMA + g + COMMA + b;
  }

}
