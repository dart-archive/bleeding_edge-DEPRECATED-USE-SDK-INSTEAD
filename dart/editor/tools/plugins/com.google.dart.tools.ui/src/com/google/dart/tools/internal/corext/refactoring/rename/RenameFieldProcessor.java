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

import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * {@link DartRenameProcessor} for {@link Field}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameFieldProcessor extends RenameTypeMemberProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameFieldProcessor"; //$NON-NLS-1$

  /**
   * @param field the {@link Field} to rename, not <code>null</code>.
   */
  public RenameFieldProcessor(Field field) {
    super(field);
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkFieldName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return member.getDeclaringType().getField(getNewElementName());
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameFieldProcessor_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable((Field) member);
  }
}
