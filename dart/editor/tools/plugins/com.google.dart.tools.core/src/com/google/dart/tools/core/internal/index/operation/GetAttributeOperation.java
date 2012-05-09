/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.operation;

import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.AttributeCallback;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;

/**
 * Instances of the class <code>GetAttributeOperation</code> implement an operation used to access
 * the value of a specified attribute for a specified element.
 */
public class GetAttributeOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The element that was specified.
   */
  private Element element;

  /**
   * The attribute that was specified.
   */
  private Attribute attribute;

  /**
   * The callback that will be invoked when results are available.
   */
  private AttributeCallback callback;

  /**
   * Initialize a newly created operation that will access the value of a specified attribute for a
   * specified element.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param element the element that was specified
   * @param attribute the attribute that was specified
   * @param callback the callback that will be invoked when the attribute value is available
   */
  public GetAttributeOperation(IndexStore indexStore, Element element, Attribute attribute,
      AttributeCallback callback) {
    this.indexStore = indexStore;
    this.element = element;
    this.attribute = attribute;
    this.callback = callback;
  }

  @Override
  public void performOperation() {
    String value;
    synchronized (indexStore) {
      value = indexStore.getAttribute(element, attribute);
    }
    callback.hasValue(element, attribute, value);
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return false;
  }

  @Override
  public String toString() {
    return "GetAttribute(" + element + ", " + attribute + ")";
  }
}
