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

import com.google.dart.tools.core.internal.model.info.DartFunctionTypeAliasInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>DartFunctionTypeAliasImpl</code>
 */
public class DartFunctionTypeAliasImpl extends SourceReferenceImpl implements DartFunctionTypeAlias {
  /**
   * The name of the function type alias.
   */
  private String name;

  /**
   * Initialize a newly created function type alias to be defined within the given compilation unit.
   * 
   * @param parent the compilation unit containing the type
   * @param name the name of the function type alias
   */
  protected DartFunctionTypeAliasImpl(CompilationUnitImpl parent, String name) {
    super(parent);
    this.name = name;
  }

  @Override
  public String getElementName() {
    return name;
  }

  @Override
  public int getElementType() {
    return DartElement.FUNCTION_TYPE_ALIAS;
  }

  @Override
  public String[] getFullParameterTypeNames() throws DartModelException {
    ArrayList<String> typeNames = new ArrayList<String>();
    for (DartVariableDeclaration variable : getChildrenOfType(DartVariableDeclaration.class)) {
      if (variable.isParameter()) {
        typeNames.add(variable.getFullTypeName());
      }
    }
    return typeNames.toArray(new String[typeNames.size()]);
  }

  @Override
  public SourceRange getNameRange() throws DartModelException {
    return ((DartFunctionTypeAliasInfo) getElementInfo()).getNameRange();
  }

  @Override
  public String[] getParameterNames() throws DartModelException {
    ArrayList<String> names = new ArrayList<String>();
    for (DartVariableDeclaration variable : getChildrenOfType(DartVariableDeclaration.class)) {
      if (variable.isParameter()) {
        names.add(variable.getElementName());
      }
    }
    return names.toArray(new String[names.size()]);
  }

  @Override
  public String[] getParameterTypeNames() throws DartModelException {
    ArrayList<String> typeNames = new ArrayList<String>();
    for (DartVariableDeclaration variable : getChildrenOfType(DartVariableDeclaration.class)) {
      if (variable.isParameter()) {
        typeNames.add(variable.getTypeName());
      }
    }
    return typeNames.toArray(new String[typeNames.size()]);
  }

  @Override
  public String getReturnTypeName() throws DartModelException {
    DartFunctionTypeAliasInfo info = (DartFunctionTypeAliasInfo) getElementInfo();
    char[] name = info.getReturnTypeName();
    if (name == null) {
      return null;
    }
    return new String(name);
  }

  @Override
  public DartTypeParameter[] getTypeParameters() throws DartModelException {
    List<DartTypeParameter> list = getChildrenOfType(DartTypeParameter.class);
    return list.toArray(new DartTypeParameter[list.size()]);
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
    return this;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_FUNCTION_TYPE_ALIAS;
  }

}
