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

import com.google.dart.tools.core.internal.model.info.DartTypeParameterInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * Implementation of {@link DartTypeParameter}.
 */
public class DartTypeParameterImpl extends SourceReferenceImpl implements DartTypeParameter {
  private String name;

  public DartTypeParameterImpl(DartElementImpl parent, String name) {
    super(parent);
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DartTypeParameterImpl)) {
      return false;
    }
    return super.equals(o);
  }

  @Override
  public String getBoundName() throws DartModelException {
    DartTypeParameterInfo info = (DartTypeParameterInfo) getElementInfo();
    char[] chars = info.getBoundName();
    return chars == null ? null : new String(chars);
  }

  @Override
  public String getElementName() {
    return name;
  }

  @Override
  public int getElementType() {
    return DartElement.TYPE_PARAMETER;
  }

  @Override
  public SourceRange getNameRange() throws DartModelException {
    DartTypeParameterInfo info = (DartTypeParameterInfo) getElementInfo();
    return info.getNameRange();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return DartElementImpl.MEMENTO_DELIMITER_TYPE_PARAMETER;
  }
}
