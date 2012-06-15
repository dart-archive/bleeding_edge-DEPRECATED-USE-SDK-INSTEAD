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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartVariableInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * Instances of the class <code>DartVariableImpl</code> implement an element that represents a
 * variable (global variable, local variable or parameter).
 */
public class DartVariableImpl extends SourceReferenceImpl implements DartVariableDeclaration {
  /**
   * The name of the variable.
   */
  private String name;

  /**
   * Initialize a newly created variable to have the given name.
   * 
   * @param parent the method or function containing the variable
   * @param name the name of the variable
   */
  public DartVariableImpl(DartElementImpl parent, String name) {
    super(parent);
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DartVariableImpl)) {
      return false;
    }
    return super.equals(o);
  }

  @Override
  public String getElementName() {
    return name;
  }

  @Override
  public int getElementType() {
    return DartElement.VARIABLE;
  }

  @Override
  public String getFullTypeName() throws DartModelException {
    DartVariableInfo info = (DartVariableInfo) getElementInfo();
    char[] chars = info.getFullTypeName();
    return chars == null ? null : new String(chars);
  }

  @Override
  public SourceRange getNameRange() throws DartModelException {
    DartVariableInfo info = (DartVariableInfo) getElementInfo();
    return info.getNameRange();
  }

  @Override
  public String getTypeName() throws DartModelException {
    DartVariableInfo info = (DartVariableInfo) getElementInfo();
    char[] chars = info.getTypeName();
    return chars == null ? null : new String(chars);
  }

  @Override
  public SourceRange getVisibleRange() throws DartModelException {
    DartVariableInfo info = (DartVariableInfo) getElementInfo();
    return info.getVisibleRange();
  }

  @Override
  public boolean isGlobal() {
    return getParent() instanceof CompilationUnit;
  }

  @Override
  public boolean isLocal() {
    return !isGlobal();
  }

  @Override
  public boolean isParameter() {
    try {
      DartVariableInfo info = (DartVariableInfo) getElementInfo();
      return info.isParameter();
    } catch (DartModelException exception) {
      return false;
    }
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    DartCore.notYetImplemented();
    // TODO(brianwilkerson) This doesn't handle unnamed functions.
    switch (token.charAt(0)) {
      case MEMENTO_DELIMITER_FUNCTION:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        DartFunctionImpl function = new DartFunctionImpl(this, tokenizer.nextToken());
        return function.getHandleFromMemento(tokenizer, owner);
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return DartElementImpl.MEMENTO_DELIMITER_VARIABLE;
  }
}
