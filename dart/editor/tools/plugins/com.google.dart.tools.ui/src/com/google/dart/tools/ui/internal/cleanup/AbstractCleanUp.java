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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.CleanUpRequirements;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.Map;

public abstract class AbstractCleanUp implements ICleanUp {

  private CleanUpOptions fOptions;

  protected AbstractCleanUp() {
  }

  protected AbstractCleanUp(Map<String, String> settings) {
    setOptions(new MapCleanUpOptions(settings));
  }

  @Override
  public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkPreConditions(DartProject project,
      CompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
    return new RefactoringStatus();
  }

  @Override
  public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
    return null;
  }

  /**
   * @return code snippet complying to current options
   */
  public String getPreview() {
    return ""; //$NON-NLS-1$
  }

  @Override
  public CleanUpRequirements getRequirements() {
    return new CleanUpRequirements(false, false, false, null);
  }

  @Override
  public String[] getStepDescriptions() {
    return new String[0];
  }

  @Override
  public void setOptions(CleanUpOptions options) {
    Assert.isLegal(options != null);
    fOptions = options;
  }

  /**
   * @param key the name of the option
   * @return <code>true</code> if option with <code>key</code> is enabled
   */
  protected boolean isEnabled(String key) {
    Assert.isNotNull(fOptions);
    Assert.isLegal(key != null);
    return fOptions.isEnabled(key);
  }

}
