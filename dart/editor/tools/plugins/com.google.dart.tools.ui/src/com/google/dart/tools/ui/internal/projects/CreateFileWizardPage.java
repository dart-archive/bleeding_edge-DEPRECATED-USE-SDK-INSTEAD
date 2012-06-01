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

import com.google.dart.tools.core.generator.NewFileGenerator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import java.io.InputStream;

/**
 * The wizard page for CreateNewFileWizard
 */
public class CreateFileWizardPage extends WizardNewFileCreationPage {

  private static final String[] dartKeywords = {
      "break", "case", "catch", "class", "const", "continue", "default", "do", "else", "extends",
      "false", "final", "finally", "for", "if", "in", "is", "new", "null", "return", "super",
      "switch", "this", "throw", "true", "try", "var", "void", "while",

      // Pseudo keywords:
      "abstract", "assert", "call",
      //"Dynamic",
      "factory", "get", "implements", "import", "interface", "library", "native", "negate",
      "operator", "set", "source", "static", "typedef"};

  /**
   * @param pageName
   * @param selection
   */
  public CreateFileWizardPage(String pageName, IStructuredSelection selection) {
    super(pageName, selection);
  }

  @Override
  public IFile createNewFile() {

    String fileName = getFileName();
    if (fileName.indexOf(".") == -1) {
      setFileName(fileName + ".dart");
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
    // Else, no problems were discovered by the Eclipse-resource framework
    String keyword = isADartKeyword(getFileName());
    if (keyword != null) {
      setErrorMessage("'" + keyword + "' is a Dart keyword.");
      return false;
    }
    return true;
  }

  /**
   * Returns <code>null</code> if the passed String is not a Dart keyword, or the keyword if it is a
   * Dart keyword.
   * 
   * @param str the String to test against
   * @return <code>null</code> if the passed String is not a Dart keyword, or the keyword if it is a
   *         Dart keyword
   */
  private String isADartKeyword(String str) {
    for (String keyword : dartKeywords) {
      if (str.equalsIgnoreCase(keyword)) {
        return keyword;
      }
    }
    return null;
  }
}
