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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchMatch;

import java.util.List;

/**
 * Container for declarations (and overrides) and reference to the {@link TypeMember}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public final class MemberDeclarationsReferences {
  public final TypeMember member;
  public final List<TypeMember> declarations;
  public final List<SearchMatch> references;

  public MemberDeclarationsReferences(TypeMember member, List<TypeMember> declarations,
      List<SearchMatch> references) {
    this.member = member;
    this.declarations = declarations;
    this.references = references;
  }
}
