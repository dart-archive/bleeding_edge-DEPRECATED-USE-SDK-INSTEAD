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

import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * {@link DartRenameProcessor} for {@link Method}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameMethodProcessor extends RenameTypeMemberProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameMethodProcessor"; //$NON-NLS-1$

  /**
   * @param method the {@link Method} to rename, not <code>null</code>.
   */
  public RenameMethodProcessor(Method method) {
    super(method);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkMethodName(newName);
    result.merge(super.checkNewElementName(newName));
    // method "call()" cannot be renamed
    if (getCurrentElementName().equals("call")) {
      result.addFatalError(RefactoringCoreMessages.RenameMethodProcessor_isCall);
    }
    // done
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return member.getDeclaringType().getMethod(getNewElementName(), null);
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameMethodProcessor_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable((Method) member);
  }

  @Override
  protected String getNewNameSource() {
    // named constructor
    if (oldName.contains(".")) {
      String prefix = StringUtils.substringBefore(oldName, ".");
      return prefix + "." + super.getNewNameSource();
    }
    // normal method
    return super.getNewNameSource();
  }

}
