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
import com.google.dart.tools.core.pub.RunPubJob;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFolderMainPage;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.wizards.newresource.BasicNewFolderResourceWizard;

import java.util.Arrays;
import java.util.List;

/**
 * Create a new folder wizard
 */
@SuppressWarnings("restriction")
public class CreateFolderWizard extends BasicNewFolderResourceWizard {

  private class CreateFolderWizardPage extends WizardNewFolderMainPage {

    public CreateFolderWizardPage(String pageName, IStructuredSelection selection) {
      super(pageName, selection);
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
    protected IStatus validateLinkedResource() {
      //no-op since we're not supporting linked resources
      return Status.OK_STATUS;
    }

  }

  /**
   * The wizard id for creating new folders in the workspace.
   */
  public static final String WIZARD_ID = "com.google.dart.tools.ui.new.folder"; //$NON-NLS-1$

  /**
   * The directories in the pub white list, ones which have symlinks to the packages directory
   */
  // (TODO:keertip) Make sure this stays in sync with documentation 
  // http://pub.dartlang.org/doc/pub-install.html
  public static final List<String> pubDirectories = Arrays.asList("web", "example", "test", "bin");

  private WizardNewFolderMainPage mainPage;

  @Override
  public void addPages() {

    mainPage = new CreateFolderWizardPage("newFolderPage1", getSelection());
    mainPage.setTitle(ResourceMessages.NewFolder_title);
    mainPage.setDescription("Create a new folder resource");
    addPage(mainPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    super.init(workbench, currentSelection);
    setWindowTitle(ResourceMessages.FileResource_shellTitle);
    setNeedsProgressMonitor(true);
  }

  @Override
  public boolean performFinish() {
    IFolder folder = mainPage.createNewFolder();
    if (folder == null) {
      return false;
    }
    // if new folder is pub white listed directory/sub-directory, run pub install
    // to create the link to packages
    if (pubDirectories.contains(folder.getName()) || isPubDirectoryParent(folder)) {
      IContainer pubspecDir = getPubWorkingDir(folder);
      if (pubspecDir != null) {
        RunPubJob job = new RunPubJob(pubspecDir, RunPubJob.INSTALL_COMMAND);
        job.schedule();
      }
    }
    selectAndReveal(folder);

    return true;
  }

  private IContainer getPubWorkingDir(IFolder folder) {
    IContainer container = folder;
    while (container != null) {
      if (container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
        return container;
      }
      container = container.getParent();
    }
    return null;
  }

  private boolean isPubDirectoryParent(IFolder folder) {
    IContainer parent = folder.getParent();
    while (parent != null) {
      if (pubDirectories.contains(parent.getName())) {
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }

}
