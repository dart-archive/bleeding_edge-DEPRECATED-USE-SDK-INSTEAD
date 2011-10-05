/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.dialogs;

import com.google.dart.core.dom.ITypeBinding;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.CodeTemplateContextType;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.internal.util.ViewerPane;
import com.google.dart.tools.ui.internal.viewsupport.BindingLabelProvider;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import java.util.ArrayList;
import java.util.HashSet;

public class OverrideMethodDialog extends SourceActionDialog {

  private class OverrideFlatTreeAction extends Action {

    private boolean fToggle;

    public OverrideFlatTreeAction() {
      setToolTipText(DartUIMessages.OverrideMethodDialog_groupMethodsByTypes);

      DartPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

      fToggle = getOverrideContentProvider().isShowTypes();
      setChecked(fToggle);
    }

    @Override
    public void run() {
      // http://bugs.eclipse.org/bugs/show_bug.cgi?id=39264
      Object[] elementList = getOverrideContentProvider().getViewer().getCheckedElements();
      fToggle = !fToggle;
      setChecked(fToggle);
      getOverrideContentProvider().setShowTypes(fToggle);
      getOverrideContentProvider().getViewer().setCheckedElements(elementList);
    }

    private OverrideMethodContentProvider getOverrideContentProvider() {
      return (OverrideMethodContentProvider) getContentProvider();
    }

  }

//  private static class OverrideMethodComparator extends ViewerComparator {
//
//    private ITypeBinding[] fAllTypes = new ITypeBinding[0];
//
//    public OverrideMethodComparator(ITypeBinding curr) {
//      if (curr != null) {
//        ITypeBinding[] superTypes = Bindings.getAllSuperTypes(curr);
//        fAllTypes = new ITypeBinding[superTypes.length + 1];
//        fAllTypes[0] = curr;
//        System.arraycopy(superTypes, 0, fAllTypes, 1, superTypes.length);
//      }
//    }
//
//    /*
//     * @see ViewerSorter#compare(Viewer, Object, Object)
//     */
//    @Override
//    public int compare(Viewer viewer, Object first, Object second) {
//      if (first instanceof ITypeBinding && second instanceof ITypeBinding) {
//        final ITypeBinding left = (ITypeBinding) first;
//        final ITypeBinding right = (ITypeBinding) second;
//        if (right.getQualifiedName().equals("java.lang.Object")) {
//          return -1;
//        }
//        if (left.isEqualTo(right)) {
//          return 0;
//        }
//        if (Bindings.isSuperType(left, right)) {
//          return +1;
//        } else if (Bindings.isSuperType(right, left)) {
//          return -1;
//        }
//        return 0;
//      } else {
//        return super.compare(viewer, first, second);
//      }
//    }
//  }

  private static class OverrideMethodContentProvider implements ITreeContentProvider {

    private final Object[] fEmpty = new Object[0];

    private Method[] fMethods;

    private IDialogSettings fSettings;

    private boolean fShowTypes;

    // private Object[] fTypes;

    private ContainerCheckedTreeViewer fViewer;

    private final String SETTINGS_SECTION = "OverrideMethodDialog"; //$NON-NLS-1$

    private final String SETTINGS_SHOWTYPES = "showtypes"; //$NON-NLS-1$

    /**
     * Constructor for OverrideMethodContentProvider.
     */
    public OverrideMethodContentProvider() {
      IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
      fSettings = dialogSettings.getSection(SETTINGS_SECTION);
      if (fSettings == null) {
        fSettings = dialogSettings.addNewSection(SETTINGS_SECTION);
        fSettings.put(SETTINGS_SHOWTYPES, true);
      }
      fShowTypes = fSettings.getBoolean(SETTINGS_SHOWTYPES);
    }

    /*
     * @see IContentProvider#dispose()
     */
    @Override
    public void dispose() {
    }

    /*
     * @see ITreeContentProvider#getChildren(Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof CompilationUnit) {
        ArrayList<Method> result = new ArrayList<Method>(fMethods.length);
        for (int index = 0; index < fMethods.length; index++) {
          if (fMethods[index].getCompilationUnit() == parentElement) {
            result.add(fMethods[index]);
          }
        }
        return result.toArray();
      }
      return fEmpty;
    }

    /*
     * @see IStructuredContentProvider#getElements(Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
      return fMethods;
    }

    /*
     * @see ITreeContentProvider#getParent(Object)
     */
    @Override
    public Object getParent(Object element) {
      if (element instanceof Method) {
        return ((Method) element).getCompilationUnit();
      }
      return null;
    }

    public ContainerCheckedTreeViewer getViewer() {
      return fViewer;
    }

    /*
     * @see ITreeContentProvider#hasChildren(Object)
     */
    @Override
    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void init(Method[] methods) {
      fMethods = methods;
      // fTypes= types;
    }

    /*
     * @see IContentProvider#inputChanged(Viewer, Object, Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      fViewer = (ContainerCheckedTreeViewer) viewer;
    }

    public boolean isShowTypes() {
      return fShowTypes;
    }

    public void setShowTypes(boolean showTypes) {
      if (fShowTypes != showTypes) {
        fShowTypes = showTypes;
        fSettings.put(SETTINGS_SHOWTYPES, showTypes);
        if (fViewer != null) {
          fViewer.refresh();
        }
      }
    }
  }

  private static class OverrideMethodValidator implements ISelectionStatusValidator {

    private static int fNumMethods;

    public OverrideMethodValidator(int entries) {
      fNumMethods = entries;
    }

    /*
     * @see ISelectionValidator#validate(Object[])
     */
    @Override
    public IStatus validate(Object[] selection) {
      int count = 0;
      for (int index = 0; index < selection.length; index++) {
        if (selection[index] instanceof Method) {
          count++;
        }
      }
      if (count == 0) {
        return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
      }
      return new StatusInfo(IStatus.INFO, Messages.format(
          DartUIMessages.OverrideMethodDialog_selectioninfo_more,
          new String[] {String.valueOf(count), String.valueOf(fNumMethods)}));
    }
  }

  private static final boolean SHOW_ONLY_SUPER = true;

  @SuppressWarnings("unused")
  private static ITypeBinding getSuperType(final ITypeBinding binding, final String name) {

    if (binding.isArray() || binding.isPrimitive()) {
      return null;
    }

    if (binding.getQualifiedName().startsWith(name)) {
      return binding;
    }

    final ITypeBinding type = binding.getSuperclass();
    if (type != null) {
      final ITypeBinding result = getSuperType(type, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private CompilationUnit fUnit = null;

  public OverrideMethodDialog(Shell shell, CompilationUnitEditor editor, Type type,
      boolean isSubType) throws DartModelException {
    super(shell, new BindingLabelProvider(), new OverrideMethodContentProvider(), editor, type,
        false);

//		IMethod[] methods = type.getMethods();
    String parentName = type.getSuperclassName();
    Type superType = type.getLibrary().findType(parentName);
    // TODO(brianwilkerson) Do the following two lines need to filter out
    // constructors?
    Method pMethods[] = superType.getMethods();
    Method tMethods[] = type.getMethods();

    Method parentMethods[] = new Method[0];

    if (OverrideMethodDialog.SHOW_ONLY_SUPER) {

      ArrayList<Method> show = new ArrayList<Method>();
      start : for (int i = 0; pMethods != null && i < pMethods.length; i++) {
        for (int k = 0; k < tMethods.length; k++) {
          if (tMethods[k].getElementName().equals(pMethods[i].getElementName())) {
            continue start;
          }
        }
        show.add(pMethods[i]);

      }
      parentMethods = show.toArray(new Method[show.size()]);

    }

    // IMethodBinding[] toImplementArray= (IMethodBinding[])
// toImplement.toArray(new IMethodBinding[toImplement.size()]);
    setInitialSelections(parentMethods);

    HashSet<Method> expanded = new HashSet<Method>(parentMethods.length);
    for (int i = 0; i < parentMethods.length; i++) {
      expanded.add(parentMethods[i]);
    }

    HashSet<Method> types = new HashSet<Method>(parentMethods.length);
    for (int i = 0; i < parentMethods.length; i++) {
      types.add(parentMethods[i]);
    }

    Method[] typesArrays = types.toArray(new Method[types.size()]);
    //OverrideMethodComparator comparator = null;// new OverrideMethodComparator(binding);
    if (expanded.isEmpty() && typesArrays.length > 0) {
      //comparator.sort(null, typesArrays);
      expanded.add(typesArrays[0]);
    }
    setExpandedElements(expanded.toArray());

    ((OverrideMethodContentProvider) getContentProvider()).init(parentMethods);

    setTitle(DartUIMessages.OverrideMethodDialog_dialog_title);
    setMessage(null);
    setValidator(new OverrideMethodValidator(parentMethods.length));
    // TODO(devoncarew): replace this with OverrideMethodComparator
    setComparator(new ViewerComparator());
    setContainerMode(true);
    setSize(60, 18);
    setInput(new Object());
  }

  public CompilationUnit getCompilationUnit() {
    return fUnit;
  }

  public boolean hasMethodsToOverride() {
    return getContentProvider().getElements(null).length > 0;
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
        IJavaHelpContextIds.OVERRIDE_TREE_SELECTION_DIALOG);
  }

  /*
   * @see com.google.dart.tools.ui.dialogs.SourceActionDialog#createLinkControl(org
   * .eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createLinkControl(Composite composite) {
    Link link = new Link(composite, SWT.WRAP);
    link.setText(DartUIMessages.OverrideMethodDialog_link_message);
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openCodeTempatePage(CodeTemplateContextType.OVERRIDECOMMENT_ID);
      }
    });
    link.setToolTipText(DartUIMessages.OverrideMethodDialog_link_tooltip);

    GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
    gridData.widthHint = convertWidthInCharsToPixels(40); // only expand further
// if anyone else requires it
    link.setLayoutData(gridData);
    return link;
  }

  /*
   * @see CheckedTreeSelectionDialog#createTreeViewer(Composite)
   */
  @Override
  protected CheckboxTreeViewer createTreeViewer(Composite composite) {
    initializeDialogUnits(composite);
    ViewerPane pane = new ViewerPane(composite, SWT.BORDER | SWT.FLAT);
    pane.setText(DartUIMessages.OverrideMethodDialog_dialog_description);
    CheckboxTreeViewer treeViewer = super.createTreeViewer(pane);
    pane.setContent(treeViewer.getControl());
    GridLayout paneLayout = new GridLayout();
    paneLayout.marginHeight = 0;
    paneLayout.marginWidth = 0;
    paneLayout.numColumns = 1;
    pane.setLayout(paneLayout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(55);
    gd.heightHint = convertHeightInCharsToPixels(15);
    pane.setLayoutData(gd);
    ToolBarManager manager = pane.getToolBarManager();
    manager.add(new OverrideFlatTreeAction()); // create after tree is created
    manager.update(true);
    treeViewer.getTree().setFocus();
    return treeViewer;
  }

}
