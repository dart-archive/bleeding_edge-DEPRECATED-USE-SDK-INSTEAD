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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * Smoke tests for {@link DartIgnoreFile}.
 */
public class DartIgnoreFileTest extends TestCase {

  private DartIgnoreFile ignoreFile;

  public void testAdd() throws Exception {
    add("/Users/foo/bar/", true);
    assertContains("/Users/foo/bar/");
  }

  public void testAddAlreadyContained() throws Exception {
    add("/Users/foo/bar/", true);
    add("/Users/foo/bar/baz.dart", false); //should get dropped since it's contained
    assertContainsExactly("/Users/foo/bar/");
  }

  public void testAddThatPrunes() throws Exception {
    add("/Users/foo/bar/baz.dart", true);
    add("/Users/foo/bar/", true); //contained baz.dart should get dropped
    assertContainsExactly("/Users/foo/bar/");
  }

  public void testContainment() throws Exception {
    assertSubsumedIn("/foo/bar", "/foo/");
    assertSubsumedIn("/foo/bar/baz", "/foo/");
    assertSubsumedIn("/foo/bar/baz/", "/foo/");
    assertSubsumedIn("/foo/", "/foo/");
  }

  public void testRemove() throws Exception {
    add("/Users/foo/bar/", true);
    remove("/Users/foo/bar/", "/Users/foo/bar/");
    assertEmpty();
  }

  public void testRemoveContained() throws Exception {
    add("/Users/foo/bar/", true);
    add("/Users/foo/bar/baz.dart", false);
    remove("/Users/foo/bar/baz.dart", "/Users/foo/bar/");
    assertEmpty();
  }

  @Override
  protected void setUp() throws Exception {
    //NOTE: file is not written to...
    ignoreFile = new DartIgnoreFile(new File("test-ignore"));
  }

  private void add(String pattern, boolean changed) {
    assertEquals(changed, ignoreFile.add(pattern));
  }

  private void assertContains(String pattern) {
    assertTrue(ignoreFile.getPatterns().contains(pattern));
  }

  private void assertContainsExactly(String... patterns) {
    TestUtilities.assertEqualsIgnoreOrder(patterns, ignoreFile.getPatterns().toArray());
  }

  private void assertEmpty() {
    assertEquals(0, ignoreFile.getPatterns().size());
  }

  private void assertSubsumedIn(String pattern, String... ignores) {
    assertTrue(DartIgnoreFile.isSubsumedIn(pattern, Arrays.asList(ignores)));
  }

  private void remove(String pattern, String expectedRemoved) {
    Collection<String> actualRemoved = ignoreFile.remove(pattern);
    assertEquals(Arrays.asList(new String[] {expectedRemoved}), actualRemoved);
  }

}
