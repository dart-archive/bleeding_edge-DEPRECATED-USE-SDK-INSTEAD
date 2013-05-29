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
package com.google.dart.compiler.ast;

import com.google.dart.compiler.common.HasSourceInfo;
import com.google.dart.compiler.common.HasSourceInfoSetter;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.type.Type;

public abstract class DartNode implements HasSourceInfo, HasSourceInfoSetter {

  public <R> R accept(ASTVisitor<R> visitor) {
    return null;
  }

  public Element getElement() {
    return null;
  }

  public String getObjectIdentifier() {
    return null;
  }

  public final DartNode getParent() {
    return null;
  }

  public final DartNode getRoot() {
    return null;
  }

  @Override
  public SourceInfo getSourceInfo() {
    return null;
  }

  public Type getType() {
    return null;
  }

  public void setElement(Element element) {

  }

  @Override
  public void setSourceInfo(SourceInfo sourceInfo) {

  }

  public void setType(Type type) {

  }

  public final String toSource() {
    return null;
  }

  @Override
  public String toString() {
    return null;
  }

  public void visitChildren(ASTVisitor<?> visitor) {
  }

  protected void safelyVisitChild(DartNode child, ASTVisitor<?> visitor) {

  }

}
