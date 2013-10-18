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

  public void test_add_disjoint_after() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(10, 7, 20);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(27, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_disjoint_before() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(10, 7, 20);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(27, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_empty() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(10, 0, 0);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_overlapping_after() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(3, 2, 6);
    DartReconcilingRegion result = target.add(5, 12, 25);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(27, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_overlapping_before() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(5, 12, 25);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertEquals(3, result.getOffset());
    assertEquals(14, result.getOldLength());
    assertEquals(27, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_add_to_empty() throws Exception {
    DartReconcilingRegion target = new DartReconcilingRegion(0, 0, 0);
    DartReconcilingRegion result = target.add(3, 2, 6);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());

    target = new DartReconcilingRegion(10, 0, 0);
    result = target.add(3, 2, 6);
    assertEquals(3, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(6, result.getNewLength());
    assertFalse(result.isEmpty());
  }

  public void test_new_empty() {
    DartReconcilingRegion target = new DartReconcilingRegion(0, 0, 0);
    assertEquals(0, target.getOffset());
    assertEquals(0, target.getOldLength());
    assertEquals(0, target.getNewLength());
    assertTrue(target.isEmpty());

    target = new DartReconcilingRegion(10, 0, 0);
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
}
