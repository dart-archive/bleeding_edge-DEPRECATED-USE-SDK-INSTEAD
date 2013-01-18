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
package com.google.dart.tools.core.index;

import com.google.dart.tools.core.test.util.TestUtilities;

import static junit.framework.Assert.assertTrue;

/**
 * The class <code>IndexTestUtilities</code> defines utility methods that can be used while testing
 * the index.
 */
public class IndexTestUtilities {
  private static class StoringAttributeCallback implements AttributeCallback {
    private boolean resultComputed = false;
    private String value = null;

    @Override
    public void hasValue(Element element, Attribute attribute, String value) {
      this.value = value;
      resultComputed = true;
    }
  }

  private static class StoringRelationshipCallback implements RelationshipCallback {
    private boolean resultComputed = false;
    private Location[] locations = null;

    @Override
    public void hasRelationships(Element element, Relationship relationship, Location[] locations) {
      this.locations = locations;
      resultComputed = true;
    }
  }

  public static String getAttribute(final Index index, final Element element,
      final Attribute attribute) {
    final StoringAttributeCallback listener = new StoringAttributeCallback();
    TestUtilities.wait(2000, new TestUtilities.ThreadController() {
      @Override
      public void startThread() {
        index.getAttribute(element, attribute, listener);
      }

      @Override
      public boolean threadCompleted() {
        return listener.resultComputed;
      }
    });
    assertTrue("Listener was never invoked", listener.resultComputed);
    return listener.value;
  }

  public static Location[] getRelationships(final Index index, final Element element,
      final Relationship relationship) {
    final StoringRelationshipCallback callback = new StoringRelationshipCallback();
    TestUtilities.wait(2000, new TestUtilities.ThreadController() {
      @Override
      public void startThread() {
        index.getRelationships(element, relationship, callback);
      }

      @Override
      public boolean threadCompleted() {
        return callback.resultComputed;
      }
    });
    assertTrue("Listener was never invoked", callback.resultComputed);
    return callback.locations;
  }
}
