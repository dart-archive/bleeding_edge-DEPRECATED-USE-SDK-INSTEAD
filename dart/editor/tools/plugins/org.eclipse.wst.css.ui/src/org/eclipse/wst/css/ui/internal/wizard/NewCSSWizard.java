/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.wizard;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;
import org.eclipse.wst.css.ui.internal.Logger;
import org.eclipse.wst.css.ui.internal.editor.CSSEditorPluginImages;
import org.eclipse.wst.css.ui.internal.image.CSSImageHelper;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.sse.core.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

public class NewCSSWizard extends Wizard implements INewWizard {
  private NewCSSFileWizardPage fNewFilePage;
  private NewCSSTemplatesWizardPage fNewFileTemplatesPage;
  private IStructuredSelection fSelection;

  public void addPages() {
    fNewFilePage = new NewCSSFileWizardPage(
        "CSSWizardNewFileCreationPage", new StructuredSelection(IDE.computeSelectedResources(fSelection))); //$NON-NLS-1$
    fNewFilePage.setTitle(CSSUIMessages._UI_WIZARD_NEW_HEADING); //$NON-NLS-1$
    fNewFilePage.setDescription(CSSUIMessages._UI_WIZARD_NEW_DESCRIPTION); //$NON-NLS-1$
    addPage(fNewFilePage);

    fNewFileTemplatesPage = new NewCSSTemplatesWizardPage();
    addPage(fNewFileTemplatesPage);
  }

  private String applyLineDelimiter(IFile file, String text) {
    String lineDelimiter = Platform.getPreferencesService().getString(
        Platform.PI_RUNTIME,
        Platform.PREF_LINE_SEPARATOR,
        System.getProperty("line.separator"), new IScopeContext[] {new ProjectScope(file.getProject()), new InstanceScope()});//$NON-NLS-1$
    String convertedText = StringUtils.replace(text, "\r\n", "\n");
    convertedText = StringUtils.replace(convertedText, "\r", "\n");
    convertedText = StringUtils.replace(convertedText, "\n", lineDelimiter);
    return convertedText;
  }

  public void init(IWorkbench aWorkbench, IStructuredSelection aSelection) {
    fSelection = aSelection;
    setWindowTitle(CSSUIMessages._UI_WIZARD_NEW_TITLE); //$NON-NLS-1$
    ImageDescriptor descriptor = CSSImageHelper.getInstance().getImageDescriptor(
        CSSEditorPluginImages.IMG_WIZBAN_NEWCSSFILE);
    setDefaultPageImageDescriptor(descriptor);
  }

  private void openEditor(final IFile file) {
    if (file != null) {
      getShell().getDisplay().asyncExec(new Runnable() {
        public void run() {
          try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IDE.openEditor(page, file, true);
          } catch (PartInitException e) {
            Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
          }
        }
      });
    }
  }

  public boolean performFinish() {
    boolean performedOK = false;

    // save user options for next use
    fNewFileTemplatesPage.saveLastSavedPreferences();

    // no file extension specified so add default extension
    String fileName = fNewFilePage.getFileName();
    if (fileName.lastIndexOf('.') == -1) {
      String newFileName = fNewFilePage.addDefaultExtension(fileName);
      fNewFilePage.setFileName(newFileName);
    }

    // create a new empty file
    IFile file = fNewFilePage.createNewFile();

    // if there was problem with creating file, it will be null, so make
    // sure to check
    if (file != null) {
      // put template contents into file
      String templateString = fNewFileTemplatesPage.getTemplateString();
      if (templateString != null) {
        templateString = applyLineDelimiter(file, templateString);
        // determine the encoding for the new file
        Preferences preference = CSSCorePlugin.getDefault().getPluginPreferences();
        String charSet = preference.getString(CommonEncodingPreferenceNames.OUTPUT_CODESET);

        try {
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          OutputStreamWriter outputStreamWriter = null;
          if (charSet == null || charSet.trim().equals("")) { //$NON-NLS-1$
            // just use default encoding
            outputStreamWriter = new OutputStreamWriter(outputStream);
          } else {
            outputStreamWriter = new OutputStreamWriter(outputStream, charSet);
          }
          outputStreamWriter.write(templateString);
          outputStreamWriter.flush();
          outputStreamWriter.close();
          ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
          file.setContents(inputStream, true, false, null);
          inputStream.close();
        } catch (Exception e) {
          Logger.log(Logger.WARNING_DEBUG, "Could not create contents for new CSS file", e); //$NON-NLS-1$
        }
      }
      // open the file in editor
      openEditor(file);

      // everything's fine
      performedOK = true;
    }
    return performedOK;
  }

}
