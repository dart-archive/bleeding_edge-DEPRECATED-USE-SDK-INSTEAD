/*
 * Copyright (c) 2011, the Dart project authors.
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
import com.google.dart.tools.core.internal.model.info.DartFieldInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * Instances of the class <code>DartFieldImpl</code> implement the representation of fields defined
 * in types.
 */
public class DartFieldImpl extends NamedTypeMemberImpl implements Field {
  /**
   * Initialize a newly created field to be defined within the given type.
   * 
   * @param parent the type containing the field
   * @param name the name of the member
   */
  public DartFieldImpl(DartTypeImpl parent, String name) {
    super(parent, name);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DartFieldImpl && super.equals(o);
  }

  @Override
  public int getElementType() {
    return DartElement.FIELD;
  }

  @Override
  public SourceRange getNameRange() throws DartModelException {
    return ((DartFieldInfo) getElementInfo()).getNameRange();
  }

  @Override
  public String getTypeName() throws DartModelException {
    DartFieldInfo info = (DartFieldInfo) getElementInfo();
    char[] chars = info.getTypeName();
    return chars == null ? null : new String(chars);
  }

  @Override
  public boolean isConstant() {
    return getModifiers().isConstant();
  }

  @Override
  public boolean isFinal() {
    return getModifiers().isFinal();
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
    return MEMENTO_DELIMITER_FIELD;
  }
}
