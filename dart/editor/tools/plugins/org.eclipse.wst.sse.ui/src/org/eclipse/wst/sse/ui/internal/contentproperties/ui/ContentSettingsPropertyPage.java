/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentproperties.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.internal.contentproperties.ContentSettings;
import org.eclipse.wst.sse.internal.contentproperties.ContentSettingsCreator;
import org.eclipse.wst.sse.internal.contentproperties.IContentSettings;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

import java.util.Hashtable;
import java.util.Map;

/**
 * @deprecated Please use
 *             org.eclipse.wst.html.ui.internal.contentproperties.ui.WebContentSettingsPropertyPage
 *             or eclipse.wst.css.ui.internal.contentproperties.ui.CSSWebContentSettingsPropertyPage
 *             instead
 */
public abstract class ContentSettingsPropertyPage extends PropertyPage {
  private static final IStatus STATUS_ERROR = new Status(IStatus.ERROR, SSEUIPlugin.ID,
      IStatus.INFO, "ERROR", null); //$NON-NLS-1$

  // for validateEdit()
  private static final IStatus STATUS_OK = new Status(IStatus.OK, SSEUIPlugin.ID, IStatus.OK,
      "OK", null); //$NON-NLS-1$

  /**
   * Method validateEdit.
   * 
   * @param file org.eclipse.core.resources.IFile
   * @param context org.eclipse.swt.widgets.Shell
   * @return IStatus
   */
  private static IStatus validateEdit(IFile file, Shell context) {
    if (file == null || !file.exists())
      return STATUS_ERROR;
    if (!(file.isReadOnly()))
      return STATUS_OK;

    IPath location = file.getLocation();

    final long beforeModifiedFromJavaIO = (location != null) ? location.toFile().lastModified()
        : IResource.NULL_STAMP;
    final long beforeModifiedFromIFile = file.getModificationStamp();

    IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, context);
    if (!status.isOK())
      return status;

    final long afterModifiedFromJavaIO = (location != null) ? location.toFile().lastModified()
        : IResource.NULL_STAMP;
    final long afterModifiedFromIFile = file.getModificationStamp();

    if (beforeModifiedFromJavaIO != afterModifiedFromJavaIO
        || beforeModifiedFromIFile != afterModifiedFromIFile) {
      IModelManager manager = StructuredModelManager.getModelManager();
      IStructuredModel model = manager.getExistingModelForRead(file);
      if (model != null) {
        if (!model.isDirty()) {
          try {
            file.refreshLocal(IResource.DEPTH_ONE, null);
          } catch (CoreException e) {
            return STATUS_ERROR;
          } finally {
            model.releaseFromRead();
          }
        } else {
          model.releaseFromRead();
        }
      }
    }

    if ((beforeModifiedFromJavaIO != afterModifiedFromJavaIO)
        || (beforeModifiedFromIFile != afterModifiedFromIFile)) {
      // the file is replaced. Modification cannot be
      // applied.
      return STATUS_ERROR;
    }
    return STATUS_OK;
  }

  protected ComboListOnPropertyPage[] combo;
  protected Composite composite;

  protected IContentSettings contentSettings;
  protected final String CSS_LABEL = SSEUIMessages.UI_CSS_profile___2; //$NON-NLS-1$
  protected final String DEVICE_LABEL = SSEUIMessages.UI_Target_Device___3; //$NON-NLS-1$

  protected final String DOCUMENT_LABEL = SSEUIMessages.UI_Default_HTML_DOCTYPE_ID___1; //$NON-NLS-1$
  protected int numberOfCombo;
  protected int numCols = 1;
  protected int numRows = 1;
  protected Composite propertyPage;

  public ContentSettingsPropertyPage() {
    super();
  }

  private void cleanUp() {
    // Are There any way to guarantee to call cleanUp() to re-load
    // downloaded .contentsettings file
    // after ContentSettings.releaseCache() in ContentSettingSelfHandler
    // class, which is called by resourceChangeEvent
  }

  protected ComboListOnPropertyPage createComboBoxOf(String title) {

    Label label = new Label(propertyPage, SWT.LEFT);
    label.setText(title);
    if (title != null && title.startsWith(SSEUIMessages.UI_Default_HTML_DOCTYPE_ID___1)) { //$NON-NLS-1$
      GridData data = new GridData();
      data.horizontalIndent = 10;
      label.setLayoutData(data);
    }
    ComboListOnPropertyPage combo = new ComboListOnPropertyPage(propertyPage, SWT.READ_ONLY);
    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    // data.horizontalAlignment= GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    combo.setLayoutData(data);
    return combo;
  }

  protected Composite createComposite(Composite parent, int numColumns, int numRows) {
    Composite composite = new Composite(parent, SWT.NONE);

    // GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    composite.setLayout(layout);

    // GridData
    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    // data.horizontalSpan=numColumns;
    // data.verticalSpan=numRows;

    composite.setLayoutData(data);
    return composite;
  }

  protected Control createContents(Composite parent) {

    contentSettings = ContentSettingsCreator.create();

    propertyPage = createComposite(parent, numCols, numRows);

    createSettingsPageGUI();

    return propertyPage;

  }

  protected abstract void createSettingsPageGUI();

  // protected abstract void applySelectedPropertyValue(String str,int
  // index);
  protected abstract void deleteNoneProperty(int index);

  protected boolean isInitValueChanged(String before, String after) {
    if (before == null && after == null)
      return false;
    if (before != null && before.equals(after))
      return false;
    return true;
  }

  protected void performDefaults() {
    super.performDefaults();
    // selected(applied) item is restored.
    for (int i = 0; i < numberOfCombo; i++) {
      // String appliedValue = combo[i].getApplyValue();
      // setSelectionItem(combo[i],appliedValue);
      combo[i].select(0); // select none.

    }

  }

  public boolean performOk() {
    Map properties = new Hashtable();
    if (validateState() == false) {
      cleanUp();
      return true;
    }
    for (int i = 0; i < numberOfCombo; i++) {
      // get selected item in Combo box.
      String str = combo[i].getSelectedValue();
      if (str == null)
        continue;
      // if no change, skip setProperty
      if (!isInitValueChanged(combo[i].getApplyValue(), str))
        continue;
      // if NONE is selected, delete attr instance (and write
      // .contentsettings.)
      if (str != null && str.length() == 0)
        deleteNoneProperty(i);
      else
        putSelectedPropertyInto(properties, str, i);
      // applySelectedPropertyValue(str,i);

      // set apply value in ComboListOnPropertyPage.
      combo[i].setApplyValue(str);
    }
    if (properties != null && !properties.isEmpty())
      contentSettings.setProperties((IResource) super.getElement(), properties);
    if (properties != null)
      properties.clear();
    properties = null;
    if (!contentSettings.existsProperties((IResource) super.getElement()))
      contentSettings.deleteAllProperties((IResource) super.getElement());
    return true;
  }

  protected abstract void putSelectedPropertyInto(Map properties, String str, int i);

  protected void setSelectionItem(ComboListOnPropertyPage combo, String value) {
    if (combo.getItemCount() <= 0)
      return;
    combo.setApplyValue(value);
    String item = combo.getKey(value);
    if (item != null)
      combo.select(combo.indexOf(item));
    else
      combo.select(0);

  }

  /*
   * Validate Edit. Similar function will be in HTMLCommand.java ContentSettingsPropertyPage.java
   * CSSActionManager.java DesignRedoAction.java DesignUndoAction.java HTMLConversionProcessor.java
   */
  private boolean validateState() {

    // get IFile of .contentsettings
    final String name = ContentSettings.getContentSettingsName();
    final IResource resource = (IResource) super.getElement();
    final IProject project = resource.getProject();
    IFile file = project.getFile(name);

    if (file != null && !file.exists())
      return true; // Is this really OK?
    // If false should be returned,
    // This statemant can be removed,
    // since ModelManagerUtil.validateEdit()
    // returns error to this case.

    Shell shell = getControl().getShell();
    IStatus status = validateEdit(file, shell);
    return status.isOK() ? true : false;
  }

}
