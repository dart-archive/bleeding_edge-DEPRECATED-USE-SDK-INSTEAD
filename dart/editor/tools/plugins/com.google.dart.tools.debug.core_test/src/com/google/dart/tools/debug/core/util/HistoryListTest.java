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

package com.google.dart.tools.debug.core.util;

import junit.framework.TestCase;

public class HistoryListTest extends TestCase {

  class MockListener implements HistoryListListener<String> {
    public String changed;

    public void clear() {
      changed = null;
    }

    @Override
    public void historyAboutToChange(String current) {

    }

    @Override
    public void historyChanged(String current) {
      changed = current;
    }
  }

  protected HistoryList<String> historyList;
  protected MockListener mockListener;

  public void testAdd() {
    historyList.add("foo");

    assertEquals("foo", mockListener.changed);
    assertFalse(historyList.hasNext());
    assertFalse(historyList.hasPrevious());
  }

  public void testClear() {
    historyList.add("foo");
    historyList.clear();

    assertEquals(null, mockListener.changed);
    assertFalse(historyList.hasNext());
    assertFalse(historyList.hasPrevious());
  }

  public void testHasNext() {
    historyList.add("foo");
    historyList.add("bar");
    historyList.navigatePrevious();

    assertEquals("foo", mockListener.changed);
    assertTrue(historyList.hasNext());
    assertFalse(historyList.hasPrevious());
  }

  public void testNavigateNext() {
    historyList.add("foo");
    historyList.add("bar");
    historyList.navigatePrevious();
    historyList.navigateNext();

    assertEquals("bar", mockListener.changed);
    assertFalse(historyList.hasNext());
    assertTrue(historyList.hasPrevious());
  }

  public void testRemoveMatching() {
    historyList.add("a");
    historyList.add("1");
    historyList.add("b");
    historyList.add("1");
    historyList.add("c");
    historyList.navigatePrevious();
    historyList.navigatePrevious();

    assertEquals("b", historyList.getCurrent());
    assertEquals(5, historyList.size());

    historyList.removeMatching(new HistoryListMatcher<String>() {
      @Override
      public boolean matches(String t) {
        return "1".equals(t);
      }
    });

    assertEquals("b", historyList.getCurrent());
    assertEquals(3, historyList.size());
    assertTrue(historyList.hasNext());

    historyList.removeMatching(new HistoryListMatcher<String>() {
      @Override
      public boolean matches(String t) {
        return "b".equals(t);
      }
    });

    assertEquals("c", historyList.getCurrent());
    assertEquals(2, historyList.size());
    assertFalse(historyList.hasNext());
  }

  @Override
  protected void setUp() throws Exception {
    mockListener = new MockListener();

    historyList = new HistoryList<String>();
    historyList.addListener(mockListener);
  }

}
