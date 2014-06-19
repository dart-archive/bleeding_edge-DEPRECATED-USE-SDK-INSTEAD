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
package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.generator.NewFileGenerator;
import com.google.dart.tools.core.utilities.general.StringUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import java.io.InputStream;

/**
 * The wizard page for CreateNewFileWizard
 */
public class CreateFileWizardPage extends WizardNewFileCreationPage {

  public CreateFileWizardPage(String pageName, IStructuredSelection selection) {
    super(pageName, selection);
  }

  @Override
  public IFile createNewFile() {

    String fileName = getFileName().trim();
    if (fileName.indexOf(".") == -1 && !fileName.equals("BUILD")) { //$NON-NLS-1$
      setFileName(fileName + ".dart"); //$NON-NLS-1$
    }

    return super.createNewFile();
  }

  @Override
  protected void createAdvancedControls(Composite parent) {
    //no-op to ensure we don't get silly resource linking options
  }

  @Override
  protected void createLinkTarget() {
    //no-op since we're not supporting linked resources
  }

  @Override
  protected InputStream getInitialContents() {
    NewFileGenerator generator = new NewFileGenerator();
    generator.setFileName(getFileName());
    try {
      generator.execute(new NullProgressMonitor());
    } catch (CoreException e) {
      // fall through
    }
    return generator.getStream();
  }

  @Override
  protected IStatus validateLinkedResource() {
    //no-op since we're not supporting linked resources
    return Status.OK_STATUS;
  }

  @Override
  protected boolean validatePage() {
    boolean workspaceValidation = super.validatePage();
    if (!workspaceValidation) {
      return workspaceValidation;
    }

    setMessage(null);

    String fileName = getFileName();

    boolean hasExtension = fileName.contains("."); //$NON-NLS-1$

    if ((!hasExtension || hasExtension && DartCore.isDartLikeFileName(fileName))
        && (StringUtilities.containsUpperCase(fileName) || StringUtilities.containsWhitespace(fileName))) {
      setMessage(
          ProjectMessages.CreateFileWizardPage_filename_content_warning_label,
          IMessageProvider.WARNING);
    }

    return true;
  }

}
