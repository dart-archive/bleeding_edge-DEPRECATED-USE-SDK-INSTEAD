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

import com.google.common.collect.Lists;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import java.util.List;

/**
 * {@link DartRenameProcessor} for {@link Type}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameTypeProcessor extends RenameTopLevelProcessor {

  public static final String IDENTIFIER = "com.google.dart.tools.ui.renameTypeProcessor"; //$NON-NLS-1$

  @SuppressWarnings("restriction")
  private static List<Change> getRenameUnitChange(
      final CompilationUnit unit,
      final String newUnitName) {
    final List<Change> changes = Lists.newArrayList();
    ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        IResource unitResource = unit.getUnderlyingResource();
        if (RefactoringAvailabilityTester.isRenameAvailable(unitResource)) {
          org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor resourceProcessor = new org.eclipse.ltk.internal.core.refactoring.resource.RenameResourceProcessor(
              unitResource);
          if (resourceProcessor.isApplicable()) {
            resourceProcessor.setNewResourceName(newUnitName + ".dart");
            ProcessorBasedRefactoring refactoring = new ProcessorBasedRefactoring(resourceProcessor);
            RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
            if (status.isOK()) {
              Change resourceChange = refactoring.createChange(new NullProgressMonitor());
              changes.add(resourceChange);
            }
          }
        }
      }
    });
    return changes;
  }

  /**
   * @return the name of {@link CompilationUnit} which corresponds to given {@link Type} name.
   */
  private static String getUnitNameForType(String typeName) {
    StringBuilder unitName = new StringBuilder();
    char[] typeChars = typeName.toCharArray();
    boolean notUpperCase = false;
    for (int i = 0; i < typeChars.length; i++) {
      char c = typeChars[i];
      if (Character.isUpperCase(c)) {
        if (notUpperCase) {
          unitName.append('_');
        }
        notUpperCase = false;
      }
      if (!Character.isUpperCase(c)) {
        notUpperCase = true;
      }
      unitName.append(Character.toLowerCase(c));
    }
    return unitName.toString();
  }

  /**
   * @return the unit name for given new type name, may be <code>null</code> if old names of unit
   *         and type were not corresponding each other according to Dart conventions.
   */
  private static String getUnitNameForTypeName(
      String oldUnitName,
      String oldTypeName,
      String newTypeName) {
    if (oldTypeName.equals(oldUnitName)) {
      return newTypeName;
    }
    if (getUnitNameForType(oldTypeName).equals(oldUnitName)) {
      return getUnitNameForType(newTypeName);
    }
    return null;
  }

  private final Type type;

  private boolean renameUnit;

  /**
   * @param type the {@link Type} to rename, not <code>null</code>.
   */
  public RenameTypeProcessor(Type type) {
    super(type);
    this.type = type;
  }

  @Override
  public RefactoringStatus checkNewElementName(String newName) throws CoreException {
    RefactoringStatus result = Checks.checkTypeName(newName);
    result.merge(super.checkNewElementName(newName));
    return result;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Object getNewElement() {
    return type.getCompilationUnit().getType(getNewElementName());
  }

  @Override
  public String getProcessorName() {
    return RefactoringCoreMessages.RenameTypeProcessor_name;
  }

  @Override
  public boolean isApplicable() throws CoreException {
    return RefactoringAvailabilityTester.isRenameAvailable(type);
  }

  /**
   * Specifies if enclosing {@link CompilationUnit} should be renamed, if its old name corresponds
   * to the old name of renaming {@link Type}.
   */
  public void setRenameUnit(boolean renameUnit) {
    this.renameUnit = renameUnit;
  }

  @Override
  protected List<Change> contributeAdditionalChanges() {
    if (renameUnit) {
      CompilationUnit unit = type.getCompilationUnit();
      String oldUnitName = StringUtils.removeEnd(unit.getElementName(), ".dart");
      String oldTypeName = oldName;
      String newTypeName = newName;
      String newUnitName = getUnitNameForTypeName(oldUnitName, oldTypeName, newTypeName);
      if (newUnitName != null) {
        return getRenameUnitChange(unit, newUnitName);
      }
    }
    return super.contributeAdditionalChanges();
  }
}
