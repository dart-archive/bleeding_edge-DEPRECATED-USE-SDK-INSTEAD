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
package com.google.dart.tools.ui.internal.text.dart;

import junit.framework.TestCase;

public class DartReconcilingRegionTest extends TestCase {

  public void test_add_contiguous() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(30, 0, 6);
    assertResult(target, new DartReconcilingRegion(30, 0, 6), result);
    assertEquals(5, result.getOffset());
    assertEquals(12, result.getOldLength());
    assertEquals(31, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_contiguousDelete() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(3, 2, 0);
    assertResult(target, new DartReconcilingRegion(3, 2, 0), result);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(25, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_contiguousDelete2() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(4, 2, 0);
    assertResult(target, new DartReconcilingRegion(4, 2, 0), result);
    assertEquals(4, result.getOffset());
    assertEquals(13, result.getOldLength());
    assertEquals(24, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_contiguousReplace() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(28, 2, 6);
    assertResult(target, new DartReconcilingRegion(28, 2, 6), result);
    assertEquals(5, result.getOffset());
    assertEquals(12, result.getOldLength());
    assertEquals(29, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_contiguousReplace2() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(10, 2, 6);
    assertResult(target, new DartReconcilingRegion(28, 2, 6), result);
    assertEquals(5, result.getOffset());
    assertEquals(12, result.getOldLength());
    assertEquals(29, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_contiguousReplaceBefore() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertResult(target, new DartReconcilingRegion(3, 2, 6), result);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(31, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_disjointAfter() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(10, 7, 20);
    assertNull(result);
  }

  public void test_add_disjointBefore() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 7, 20);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertNull(result);
  }

  public void test_add_empty() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(10, 0, 0);
    assertResult(target, new DartReconcilingRegion(10, 0, 0), result);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_overlappingAfter() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(5, 5, 25);
    assertNull(result);
  }

  public void test_add_overlappingBefore() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(3, 1, 6);
    assertNull(result);
  }

  public void test_add_toEmpty1() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(0, 0, 0);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertResult(target, new DartReconcilingRegion(3, 2, 6), result);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_toEmpty2() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 0, 0);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertResult(target, new DartReconcilingRegion(3, 2, 6), result);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_new_empty1() {
    DartReconcilingRegion target = new DartReconcilingRegion(0, 0, 0);
    assertEquals(0, target.getOffset());
    assertEquals(0, target.getOldLength());
    assertEquals(0, target.getNewLength());
    assertTrue(target.isEmpty());
  }

  public void test_new_empty2() {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 0, 0);
    assertEquals(10, target.getOffset());
    assertEquals(0, target.getOldLength());
    assertEquals(0, target.getNewLength());
    assertTrue(target.isEmpty());
  }

  public void test_new_insert() {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 0, 20);
    assertEquals(10, target.getOffset());
    assertEquals(0, target.getOldLength());
    assertEquals(20, target.getNewLength());
    assertFalse(target.isEmpty());
  }

  public void test_new_replace() {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 20, 0);
    assertEquals(10, target.getOffset());
    assertEquals(20, target.getOldLength());
    assertEquals(0, target.getNewLength());
    assertFalse(target.isEmpty());
  }

  private String adjust(String code, DartReconcilingRegion region) {
    StringBuilder sb = new StringBuilder();
    sb.append(code.substring(0, region.getOffset()));
    for (int count = 0; count < region.getNewLength(); count++) {
      sb.append('0');
    }
    sb.append(code.substring(region.getOffset() + region.getOldLength()));
    return sb.toString();
  }

  private void assertResult(DartReconcilingRegion target, DartReconcilingRegion added,
      DartReconcilingRegion result) {
    String code = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String expected = adjust(adjust(code, target), added);
    String actual = adjust(code, result);
    assertEquals(expected, actual);
  }
}
