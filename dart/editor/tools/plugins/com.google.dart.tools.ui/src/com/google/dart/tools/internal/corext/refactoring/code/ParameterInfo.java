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

/**
 * Information about method parameter.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ParameterInfo {
  private final String oldName;
  private String newName;
  private String oldTypeName;
  private String newTypeName;
  private String defaultValue;

  public ParameterInfo(VariableElement variable) {
    oldName = variable.getName();
    newName = oldName;
    oldTypeName = ExtractUtils.getTypeSource(variable.getType());
    newTypeName = oldTypeName;
    defaultValue = "null";
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getNewName() {
    return newName;
  }

  public String getNewTypeName() {
    return newTypeName;
  }

  public String getOldName() {
    return oldName;
  }

  public boolean isAdded() {
    // TODO(scheglov)
    return false;
  }

  public boolean isDeleted() {
    // TODO(scheglov)
    return false;
  }

  public boolean isRenamed() {
    return !oldName.equals(newName);
  }

  public boolean isTypeNameChanged() {
    return !oldTypeName.equals(newTypeName);
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }

  public void setNewTypeName(String newTypeName) {
    this.newTypeName = newTypeName;
  }

}
