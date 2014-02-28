/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularDirectiveElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularHasAttributeSelectorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeElementName;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * {@link Refactoring} for renaming {@link AngularHasAttributeSelectorElement}.
 */
public class RenameAngularHasAttributeSelectorRefactoringImpl extends
    RenameAngularElementRefactoringImpl {
  /**
   * If the given {@link AngularPropertyElement} is a property of an {@link AngularDirectiveElement}
   * then returns the {@link AngularHasAttributeSelectorElement} with the same name as the
   * {@link AngularPropertyElement}, or {@code null}.
   */
  public static AngularHasAttributeSelectorElement getAttributeSelectorElement(
      AngularPropertyElement property) {
    Element enclosing = property.getEnclosingElement();
    if (enclosing instanceof AngularDirectiveElement) {
      AngularDirectiveElement directive = (AngularDirectiveElement) enclosing;
      AngularSelectorElement selector = directive.getSelector();
      if (selector instanceof AngularHasAttributeSelectorElement) {
        AngularHasAttributeSelectorElement attributeSelector = (AngularHasAttributeSelectorElement) selector;
        if (StringUtils.equals(property.getName(), attributeSelector.getName())) {
          return attributeSelector;
        }
      }
    }
    return null;
  }

  private final AngularHasAttributeSelectorElement element;

  public RenameAngularHasAttributeSelectorRefactoringImpl(SearchEngine searchEngine,
      AngularHasAttributeSelectorElement element) {
    super(searchEngine, element);
    this.element = element;
  }

  @Override
  public String getRefactoringName() {
    return "Rename Attribute Selector";
  }

  @Override
  protected RefactoringStatus checkNameConflicts(String newName) {
    AngularApplication application = element.getApplication();
    if (application != null) {
      for (AngularElement angularElement : application.getElements()) {
        if (angularElement instanceof AngularComponentElement) {
          AngularComponentElement component = (AngularComponentElement) angularElement;
          AngularSelectorElement selector = component.getSelector();
          if (selector instanceof AngularHasAttributeSelectorElement) {
            AngularHasAttributeSelectorElement attrSelector = (AngularHasAttributeSelectorElement) selector;
            if (attrSelector.getName().equals(newName)) {
              String message = MessageFormat.format(
                  "Application already defines component with attribute selector ''{0}''.",
                  newName);
              return RefactoringStatus.createErrorStatus(message);
            }
          }
        }
      }
    }
    return new RefactoringStatus();
  }

  @Override
  protected RefactoringStatus checkNameSyntax(String newName) {
    return NamingConventions.validateAngularTagSelectorName(newName);
  }

  @Override
  protected void createAdditionalChanges() throws Exception {
    Element enclosing = element.getEnclosingElement();
    if (enclosing instanceof AngularDirectiveElement) {
      AngularDirectiveElement directive = (AngularDirectiveElement) enclosing;
      for (AngularPropertyElement property : directive.getProperties()) {
        if (property.getName().equals(element.getName())) {
          Source source = element.getSource();
          SourceChange sourceChange = changeManager.get(source);
          addReferenceEdit(sourceChange, new SourceReference(
              null,
              source,
              rangeElementName(property)));
        }
      }
    }
  }
}
