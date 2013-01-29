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

package com.google.dart.tools.debug.core.sourcemaps;

import junit.framework.TestCase;

import static org.junit.Assert.assertArrayEquals;

public class VlqDecoderTest extends TestCase {

  public void testBase64VLQSelectedSignedValues1() {
    for (int i = -(64 * 64 - 1); i < (64 * 64 - 1); i++) {
      testValue(i);
    }
  }

  public void testBase64VLQSelectedSignedValues2() {
    int base = 1;
    for (int i = 0; i < 30; i++) {
      testValue(base - 1);
      testValue(base);
      base *= 2;
    }
    base = -1;
    for (int i = 0; i < 30; i++) {
      testValue(base - 1);
      testValue(base);
      base *= 2;
    }
  }

  public void testBase64VLQSelectedValues1() {
    for (int i = 0; i < 63; i++) {
      testValue(i);
    }
  }

  public void testBase64VLQSelectedValues2() {
    int base = 1;
    for (int i = 0; i < 30; i++) {
      testValue(base - 1);
      testValue(base);
      base *= 2;
    }
  }

  public void testDecode1() throws Exception {
    int[] actual = VlqDecoder.decode("AAgBC");

    assertArrayEquals(new int[] {0, 0, 16, 1}, actual);
  }

  private void testValue(int value) {
    try {
      String result = VlqDecoder.encode(new int[] {value});
      int[] resultValue = VlqDecoder.decode(result);
      assertEquals(value, resultValue[0]);
    } catch (Exception e) {
      throw new RuntimeException("failed for value " + value, e);
    }
  }

}
