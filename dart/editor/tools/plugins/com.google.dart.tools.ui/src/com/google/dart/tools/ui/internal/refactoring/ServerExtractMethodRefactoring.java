/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.common.collect.ImmutableList;
import com.google.dart.server.generated.types.ExtractMethodFeedback;
import com.google.dart.server.generated.types.ExtractMethodOptions;
import com.google.dart.server.generated.types.RefactoringFeedback;
import com.google.dart.server.generated.types.RefactoringKind;
import com.google.dart.server.generated.types.RefactoringMethodParameter;
import com.google.dart.server.generated.types.RefactoringOptions;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.List;

/**
 * LTK wrapper around Analysis Server 'Extract Method' refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServerExtractMethodRefactoring extends ServerRefactoring {
  private final ExtractMethodOptions options = new ExtractMethodOptions(
      "returnType",
      false,
      "name",
      ImmutableList.<RefactoringMethodParameter> of(),
      false);
  private ExtractMethodFeedback feedback;

  public ServerExtractMethodRefactoring(String file, int offset, int length) {
    super(RefactoringKind.EXTRACT_METHOD, "Extract Method", file, offset, length);
  }

  public boolean canExtractGetter() {
    return feedback.canCreateGetter();
  }

  public String[] getNames() {
    return toStringArray(feedback.getNames());
  }

  public int getOccurrences() {
    return feedback.getOffsets().length;
  }

  public List<RefactoringMethodParameter> getParameters() {
    return options.getParameters();
  }

  public String getSignature() {
    // TODO(scheglov) consider moving to server
    StringBuilder sb = new StringBuilder();
    sb.append(options.getReturnType());
    sb.append(" ");
    boolean createGetter = options.createGetter();
    if (createGetter) {
      sb.append("get ");
    }
    sb.append(options.getName());
    if (!createGetter) {
      sb.append("(");
      boolean firstParameter = true;
      for (RefactoringMethodParameter parameter : options.getParameters()) {
        if (!firstParameter) {
          sb.append(", ");
        }
        firstParameter = false;
        sb.append(parameter.getType());
        sb.append(" ");
        sb.append(parameter.getName());
      }
      sb.append(")");
    }
    return sb.toString();
  }

  public void setCreateGetter(boolean value) {
    options.setCreateGetter(value);
  }

  public void setExtractAll(boolean extractAll) {
    options.setExtractAll(extractAll);
  }

  public RefactoringStatus setName(String name) {
    options.setName(name);
    return setOptions(true);
  }

  @Override
  protected RefactoringOptions getOptions() {
    return options;
  }

  @Override
  protected void setFeedback(RefactoringFeedback _feedback) {
    boolean firstFeedback = feedback == null;
    feedback = (ExtractMethodFeedback) _feedback;
    if (firstFeedback) {
      options.setExtractAll(true);
      options.setReturnType(feedback.getReturnType());
      options.setCreateGetter(feedback.canCreateGetter());
      options.setParameters(feedback.getParameters());
    }
  }
}
