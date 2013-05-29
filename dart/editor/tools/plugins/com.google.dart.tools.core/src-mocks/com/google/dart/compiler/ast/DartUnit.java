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

import java.util.List;
import java.util.Set;

public class DartUnit extends DartNode {

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return null;
  }

  public List<DartComment> getComments() {
    return null;
  }

  public Set<String> getDeclarationNames() {
    return null;
  }

  public List<DartDirective> getDirectives() {
    return null;
  }

  public LibraryUnit getLibrary() {
    return null;
  }

  public String getSourceName() {
    return null;
  }

  public Set<String> getTopDeclarationNames() {
    return null;
  }

  public List<DartNode> getTopLevelNodes() {
    return null;
  }

  public boolean hasParseErrors() {
    return false;
  }

  public boolean isDiet() {
    return false;
  }

  public void setHasParseErrors(boolean hasParseErrors) {
  }

  public void setLibrary(LibraryUnit library) {

  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {

  }
}
