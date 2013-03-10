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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.engine.services.refactoring.ParameterInfo;

/**
 * Information about method parameter.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ParameterInfo_OLD implements ParameterInfo {
  private final String oldName;
  private String newName;
  private String oldTypeName;
  private String newTypeName;
  private String defaultValue;

  public ParameterInfo_OLD(VariableElement variable) {
    oldName = variable.getName();
    newName = oldName;
    oldTypeName = ExtractUtils.getTypeSource(variable.getType());
    newTypeName = oldTypeName;
    defaultValue = "null";
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String getNewName() {
    return newName;
  }

  @Override
  public String getNewTypeName() {
    return newTypeName;
  }

  @Override
  public String getOldName() {
    return oldName;
  }

  @Override
  public boolean isAdded() {
    // TODO(scheglov)
    return false;
  }

  @Override
  public boolean isDeleted() {
    // TODO(scheglov)
    return false;
  }

  @Override
  public boolean isRenamed() {
    return !oldName.equals(newName);
  }

  public boolean isTypeNameChanged() {
    return !oldTypeName.equals(newTypeName);
  }

  @Override
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public void setNewName(String newName) {
    this.newName = newName;
  }

  @Override
  public void setNewTypeName(String newTypeName) {
    this.newTypeName = newTypeName;
  }

}
