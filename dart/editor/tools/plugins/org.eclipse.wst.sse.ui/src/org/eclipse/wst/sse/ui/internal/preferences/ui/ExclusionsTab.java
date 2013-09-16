/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.sse.core.internal.SSECorePlugin;
import org.eclipse.wst.sse.core.internal.tasks.FileTaskScannerRegistryReader;
import org.eclipse.wst.sse.core.internal.tasks.TaskTagPreferenceKeys;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.util.Sorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

class ExclusionsTab implements IPreferenceTab {
  private class ArrayTreeContentProvider implements ITreeContentProvider {
    public ArrayTreeContentProvider() {
      super();
    }

    public void dispose() {
    }

    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof Object[])
        return fContentTypeSorter.sort((Object[]) parentElement);
      return new Object[0];
    }

    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  private class ContentTypeLabelProvider extends LabelProvider {
    public String getText(Object element) {
      if (element != null) {
        Object o = element;
        if (o instanceof String) {
          o = fContentTypeManager.getContentType(o.toString());
        }
        if (o instanceof IContentType) {
          return ((IContentType) o).getName();
        }
      }
      return super.getText(element);
    }
  }

  /**
   * A QuickSorter that sorts and returns a IContentType-typed array
   */
  private class ContentTypeSorter extends Sorter {
    private Collator collator = Collator.getInstance(Locale.ENGLISH);

    public boolean compare(Object elementOne, Object elementTwo) {
      return (collator.compare(((IContentType) elementOne).getName(),
          ((IContentType) elementTwo).getName())) < 0;
    }

    public Object[] sort(Object[] unSortedCollection) {
      Object[] types = super.sort(unSortedCollection);
      IContentType[] sortedTypes = new IContentType[types.length];
      if (types.length > 0) {
        System.arraycopy(types, 0, sortedTypes, 0, sortedTypes.length);
      }
      return sortedTypes;
    }
  }

  private class ContentTypeTreeProvider implements ITreeContentProvider {
    public ContentTypeTreeProvider() {
      super();
    }

    public void dispose() {
    }

    boolean equals(Object left, Object right) {
      return left == null ? right == null : ((right != null) && left.equals(right));
    }

    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof Object[]) {
        return (Object[]) parentElement;
      }
      if (parentElement instanceof IContentType) {
        List elements = new ArrayList(0);
        IContentType[] allTypes = fContentTypeManager.getAllContentTypes();
        for (int i = 0; i < allTypes.length; i++) {
          if (!fSupportedContentTypes.contains(allTypes[i])
              && equals(allTypes[i].getBaseType(), parentElement)) {
            elements.add(allTypes[i]);
          }
        }
        return fContentTypeSorter.sort(elements.toArray());
      }
      return new Object[0];
    }

    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    public Object getParent(Object element) {
      Object parent = null;
      if (element instanceof IContentType) {
        parent = ((IContentType) element).getBaseType();
      }
      return parent;
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  /**
   * A QuickSorter that sorts and returns a String-typed array
   */
  private class StringSorter extends Sorter {
    Collator collator = Collator.getInstance(Locale.ENGLISH);

    public boolean compare(Object elementOne, Object elementTwo) {
      return (collator.compare(elementOne.toString(), elementTwo.toString()) < 0);
    }

    public Object[] sort(Object[] unSortedCollection) {
      Object[] sortedCollection = super.sort(unSortedCollection);
      String[] strings = new String[sortedCollection.length];
      // copy the array so can return a new sorted collection
      if (strings.length > 0) {
        System.arraycopy(sortedCollection, 0, strings, 0, strings.length);
      }
      return strings;
    }
  }

  private static final boolean _debugPreferences = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.core/tasks/preferences")); //$NON-NLS-1$ //$NON-NLS-2$

  private CheckboxTreeViewer fContentTypeList;

  private IContentTypeManager fContentTypeManager = null;

  ContentTypeSorter fContentTypeSorter = new ContentTypeSorter();

  private IContentType[] fIgnoreContentTypes = null;

  private IContentType[] fOriginalIgnoreContentTypes = null;

  private TaskTagPreferencePage fOwner = null;

  private IScopeContext[] fPreferencesLookupOrder = null;

  private IPreferencesService fPreferencesService = null;

  private List fSupportedContentTypes = null;

  public ExclusionsTab(TaskTagPreferencePage parent, IPreferencesService preferencesService,
      IScopeContext[] lookupOrder) {
    super();
    fOwner = parent;
    fPreferencesLookupOrder = lookupOrder;
    fPreferencesService = preferencesService;
    fContentTypeManager = Platform.getContentTypeManager();

    String[] supportedContentTypeIDs = FileTaskScannerRegistryReader.getInstance().getSupportedContentTypeIds();
    fSupportedContentTypes = new ArrayList(supportedContentTypeIDs.length);
    for (int i = 0; i < supportedContentTypeIDs.length; i++) {
      IContentType type = fContentTypeManager.getContentType(supportedContentTypeIDs[i]);
      if (type != null) {
        fSupportedContentTypes.add(type);
      }
    }
    String[] ignoreContentTypes = StringUtils.unpack(fPreferencesService.getString(
        TaskTagPreferenceKeys.TASK_TAG_NODE,
        TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED,
        SSECorePlugin.getDefault().getPluginPreferences().getDefaultString(
            TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED), fPreferencesLookupOrder));
    List contentTypes = new ArrayList();
    for (int i = 0; i < ignoreContentTypes.length; i++) {
      IContentType type = fContentTypeManager.getContentType(ignoreContentTypes[i]);
      if (type != null) {
        contentTypes.add(type);
      }
    }
    fOriginalIgnoreContentTypes = fIgnoreContentTypes = (IContentType[]) contentTypes.toArray(new IContentType[contentTypes.size()]);
  }

  public Control createContents(Composite tabFolder) {
    SashForm sash = new SashForm(tabFolder, SWT.VERTICAL);
    Composite composite = new Composite(sash, SWT.NONE);
    composite.setLayout(new GridLayout(2, true));
    Label description = new Label(composite, SWT.NONE);
    description.setText(SSEUIMessages.TaskTagExclusionTab_02);
//		description.setBackground(composite.getBackground());

    fContentTypeList = new CheckboxTreeViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.BORDER);
    fContentTypeList.setLabelProvider(new ContentTypeLabelProvider());
    fContentTypeList.setContentProvider(new ArrayTreeContentProvider());

    fContentTypeList.setInput(fSupportedContentTypes.toArray());
    fContentTypeList.setCheckedElements(fSupportedContentTypes.toArray());
    fContentTypeList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    for (int i = 0; i < fIgnoreContentTypes.length; i++) {
      fContentTypeList.setChecked(fIgnoreContentTypes[i], false);
    }

    Button selectAll = new Button(composite, SWT.PUSH);
    selectAll.setText(SSEUIMessages.TaskTagPreferenceTab_17);
    selectAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    selectAll.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        fContentTypeList.setCheckedElements(fSupportedContentTypes.toArray());
      }
    });

    Button selectNone = new Button(composite, SWT.PUSH);
    selectNone.setText(SSEUIMessages.TaskTagPreferenceTab_18);
    selectNone.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    selectNone.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        fContentTypeList.setCheckedElements(new Object[0]);
      }
    });

    composite = new Composite(sash, SWT.NONE);
    composite.setLayout(new GridLayout(2, true));
    new Label(composite, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

    Label affectedTypesLabel = new Label(composite, SWT.NONE);
    affectedTypesLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
//		affectedTypesLabel.setBackground(composite.getBackground());
    affectedTypesLabel.setText(SSEUIMessages.TaskTagExclusionTab_03);

    final TreeViewer contentTypeTreeViewer = new TreeViewer(composite, SWT.SINGLE | SWT.H_SCROLL
        | SWT.V_SCROLL | SWT.BORDER);
    contentTypeTreeViewer.setLabelProvider(new ContentTypeLabelProvider());
    contentTypeTreeViewer.setContentProvider(new ContentTypeTreeProvider());
    contentTypeTreeViewer.setInput(new Object[0]);
    contentTypeTreeViewer.getTree().setLayoutData(
        new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

    fContentTypeList.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if (event.getSelection() instanceof IStructuredSelection) {
          Object[] o = ((IStructuredSelection) event.getSelection()).toArray();
          contentTypeTreeViewer.setInput(o);
          contentTypeTreeViewer.expandAll();
          if (o.length > 0) {
            contentTypeTreeViewer.reveal(o[0]);
          }
        }
      }
    });

    return sash;
  }

  public String getTitle() {
    return SSEUIMessages.TaskTagExclusionTab_01;
  }

  public void performApply() {
    save();
  }

  public void performDefaults() {
    if (_debugPreferences) {
      System.out.println("Loading defaults in " + getClass().getName()); //$NON-NLS-1$
    }
    IEclipsePreferences[] defaultPreferences = new IEclipsePreferences[fPreferencesLookupOrder.length - 1];
    for (int i = 1; i < defaultPreferences.length; i++) {
      defaultPreferences[i - 1] = fPreferencesLookupOrder[i].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE);
    }
    String[] defaultIgnoreTypes = StringUtils.unpack(fPreferencesService.get(
        TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED, null, defaultPreferences)); //$NON-NLS-1$

    List contentTypes = new ArrayList();
    for (int i = 0; i < defaultIgnoreTypes.length; i++) {
      IContentType type = fContentTypeManager.getContentType(defaultIgnoreTypes[i]);
      if (type != null) {
        contentTypes.add(type);
      }
    }
    fIgnoreContentTypes = (IContentType[]) contentTypes.toArray(new IContentType[contentTypes.size()]);
    fContentTypeList.setCheckedElements(fContentTypeManager.getAllContentTypes());
    for (int i = 0; i < fIgnoreContentTypes.length; i++) {
      fContentTypeList.setChecked(fIgnoreContentTypes[i], false);
    }
  }

  public void performOk() {
    List ignoredIds = save();
    String[] ignoreIDs = (String[]) new StringSorter().sort(ignoredIds.toArray());
    fIgnoreContentTypes = new IContentType[ignoreIDs.length];
    for (int i = 0; i < ignoreIDs.length; i++) {
      fIgnoreContentTypes[i] = fContentTypeManager.getContentType(ignoreIDs[i]);
    }

    if (!Arrays.equals(fOriginalIgnoreContentTypes, fIgnoreContentTypes)) {
      fOwner.requestRedetection();
    }
    fOriginalIgnoreContentTypes = fIgnoreContentTypes;
  }

  private List save() {
    List ignoredIds = new ArrayList();
    List checked = Arrays.asList(fContentTypeList.getCheckedElements());
    for (int i = 0; i < fSupportedContentTypes.size(); i++) {
      if (!checked.contains(fSupportedContentTypes.get(i))) {
        ignoredIds.add(((IContentType) fSupportedContentTypes.get(i)).getId());
      }
    }
    IEclipsePreferences[] defaultPreferences = new IEclipsePreferences[fPreferencesLookupOrder.length - 1];
    for (int i = 1; i < defaultPreferences.length; i++) {
      defaultPreferences[i - 1] = fPreferencesLookupOrder[i].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE);
    }

    String defaultIgnoredContentTypeIds = StringUtils.pack((String[]) new StringSorter().sort(StringUtils.unpack(fPreferencesService.get(
        TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED, "", defaultPreferences)))); //$NON-NLS-1$
    String ignoredContentTypeIds = StringUtils.pack((String[]) new StringSorter().sort(ignoredIds.toArray()));
    if (ignoredContentTypeIds.equals(defaultIgnoredContentTypeIds)) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED + " from scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).remove(
          TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED);
    } else {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " setting " + TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED + " \"" + ignoredContentTypeIds + "\" in scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).put(
          TaskTagPreferenceKeys.TASK_TAG_CONTENTTYPES_IGNORED, ignoredContentTypeIds);
    }
    return ignoredIds;
  }
}
