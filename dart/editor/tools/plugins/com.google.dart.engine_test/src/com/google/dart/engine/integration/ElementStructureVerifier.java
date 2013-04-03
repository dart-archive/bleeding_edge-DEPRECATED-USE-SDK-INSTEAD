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
package com.google.dart.engine.integration;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Instances of the class {@code ElementStructureVerifier} verify that a consistent element
 * structure was created.
 */
public class ElementStructureVerifier extends GeneralizingElementVisitor<Void> {
  /**
   * A list containing the errors found while traversing the element structure.
   */
  private ArrayList<String> errors = new ArrayList<String>();

  /**
   * Assert that no errors were found while traversing any of the element structures that have been
   * visited.
   */
  public void assertValid() {
    if (!errors.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      builder.append("Invalid element structure:");
      for (String message : errors) {
        builder.append("\r\n   ");
        builder.append(message);
      }
      Assert.fail(builder.toString());
    }
  }

  @Override
  public Void visitElement(Element element) {
    validate(element);
    return super.visitElement(element);
  }

  /**
   * Validate that the given element is correctly constructed.
   * 
   * @param element the element being validated
   */
  private void validate(Element element) {
    if (element instanceof LibraryElement) {
      if (element.getEnclosingElement() != null) {
        errors.add("Libraries should not have a parent");
      }
    } else {
      if (element.getEnclosingElement() == null) {
        errors.add("Missing parent for " + element);
      }
    }
  }
}
