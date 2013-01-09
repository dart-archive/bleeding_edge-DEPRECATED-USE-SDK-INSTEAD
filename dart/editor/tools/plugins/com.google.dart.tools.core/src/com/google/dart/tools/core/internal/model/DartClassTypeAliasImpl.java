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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartTypeInfo;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartClassTypeAlias;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import java.util.List;

/**
 * Instances of the class <code>DartClassTypeAliasImpl</code> implement the representation of class
 * type alist defined in compilation units.
 */
public class DartClassTypeAliasImpl extends SourceReferenceImpl implements DartClassTypeAlias {
  /**
   * The name of the type.
   */
  private String name;

  /**
   * Initialize a newly created type to be defined within the given compilation unit.
   * 
   * @param parent the compilation unit containing the type
   * @param name the name of the type
   */
  public DartClassTypeAliasImpl(CompilationUnitImpl parent, String name) {
    super(parent);
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DartClassTypeAliasImpl)) {
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
    return DartElement.CLASS_TYPE_ALIAS;
  }

  @Override
  public SourceRange getNameRange() throws DartModelException {
    return ((DartTypeInfo) getElementInfo()).getNameRange();
  }

  @Override
  public String getSuperclassName() throws DartModelException {
    DartTypeInfo info = (DartTypeInfo) getElementInfo();
    char[] superclassName = info.getSuperclassName();
    if (superclassName == null) {
      return null;
    }
    return new String(superclassName);
  }

  @Override
  public String[] getSuperInterfaceNames() throws DartModelException {
    DartTypeInfo info = (DartTypeInfo) getElementInfo();
    char[][] names = info.getInterfaceNames();
    return CharOperation.toStrings(names);
  }

  @Override
  public String[] getSupertypeNames() throws DartModelException {
    DartTypeInfo info = (DartTypeInfo) getElementInfo();
    char[] superclassName = info.getSuperclassName();
    char[][] names = info.getInterfaceNames();
    int count = names.length;
    if (superclassName != null) {
      count++;
    }
    String[] supertypeNames = new String[count];
    int index = 0;
    if (superclassName != null) {
      supertypeNames[0] = new String(superclassName);
      index++;
    }
    for (char[] interfaceName : names) {
      supertypeNames[index++] = new String(interfaceName);
    }
    return supertypeNames;
  }

  @Override
  public DartTypeParameter[] getTypeParameters() throws DartModelException {
    List<DartTypeParameter> list = getChildrenOfType(DartTypeParameter.class);
    return list.toArray(new DartTypeParameter[list.size()]);
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return null;
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    switch (token.charAt(0)) {
      case MEMENTO_DELIMITER_TYPE_PARAMETER:
        if (!tokenizer.hasMoreTokens()) {
          return this;
        }
        DartTypeParameterImpl typeParameter = new DartTypeParameterImpl(this, tokenizer.nextToken());
        return typeParameter.getHandleFromMemento(tokenizer, owner);
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_CLASS_TYPE_ALIAS;
  }
}
