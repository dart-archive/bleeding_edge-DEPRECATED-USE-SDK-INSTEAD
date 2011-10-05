/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.SignatureUtil;

/**
 * Proposal info that computes the javadoc lazily when it is queried.
 */
public final class FieldProposalInfo extends MemberProposalInfo {

  /**
   * Creates a new proposal info.
   * 
   * @param project the java project to reference when resolving types
   * @param proposal the proposal to generate information for
   */
  public FieldProposalInfo(DartProject project, CompletionProposal proposal) {
    super(project, proposal);
  }

  /**
   * Resolves the member described by the receiver and returns it if found. Returns
   * <code>null</code> if no corresponding member can be found.
   * 
   * @return the resolved member or <code>null</code> if none is found
   * @throws DartModelException if accessing the java model fails
   */
  @Override
  protected TypeMember resolveMember() throws DartModelException {
    char[] declarationSignature = fProposal.getDeclarationSignature();
    // for synthetic fields on arrays, declaration signatures may be null
    // TODO remove when https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690 gets
    // fixed
    if (declarationSignature == null) {
      return null;
    }
    String typeName = SignatureUtil.stripSignatureToFQN(String.valueOf(declarationSignature));
    Type[] types = this.fJavaProject.findTypes(typeName);
    if (types != null && types.length > 0) {
      for (int i = 0; i < types.length; ++i) {
        Type type = types[i];
        if (type != null) {
          String name = String.valueOf(fProposal.getName());
          Field field = type.getField(name);
          if (field.exists()) {
            return field;
          }
        }
      }
    }

    return null;
  }
}
