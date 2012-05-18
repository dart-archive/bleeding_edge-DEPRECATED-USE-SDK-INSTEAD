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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.OverrideIndicatorLabelDecorator;
import com.google.dart.tools.ui.ProblemsLabelDecorator;
import com.google.dart.tools.ui.StandardDartElementContentProvider;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.util.StringMatcher;
import com.google.dart.tools.ui.internal.viewsupport.AppearanceAwareLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.ColoredViewersManager;
import com.google.dart.tools.ui.internal.viewsupport.MemberFilter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;

import java.util.HashMap;
import java.util.Map;

/**
 * Show outline in light-weight control.
 */
public class DartOutlineInformationControl extends AbstractInformationControl {

  private class LexicalSortingAction extends Action {

    private static final String STORE_LEXICAL_SORTING_CHECKED = "LexicalSortingAction.isChecked"; //$NON-NLS-1$

    private TreeViewer fOutlineViewer;

    private LexicalSortingAction(TreeViewer outlineViewer) {
      super(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_label,
          IAction.AS_CHECK_BOX);
      setToolTipText(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_tooltip);
      setDescription(TextMessages.JavaOutlineInformationControl_LexicalSortingAction_description);

      DartPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$

      fOutlineViewer = outlineViewer;

      boolean checked = getDialogSettings().getBoolean(STORE_LEXICAL_SORTING_CHECKED);
      setChecked(checked);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
          DartHelpContextIds.LEXICAL_SORTING_BROWSING_ACTION);
    }

    @Override
    public void run() {
      valueChanged(isChecked(), true);
    }

    private void valueChanged(final boolean on, boolean store) {
      setChecked(on);
      BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          fOutlineViewer.refresh(false);
        }
      });

      if (store) {
        getDialogSettings().put(STORE_LEXICAL_SORTING_CHECKED, on);
      }
    }
  }
  /**
   * String matcher that can match two patterns.
   */
  private static class OrStringMatcher extends StringMatcher {

    private StringMatcher fMatcher1;
    private StringMatcher fMatcher2;

    private OrStringMatcher(String pattern1, String pattern2, boolean ignoreCase, boolean foo) {
      super("", false, false); //$NON-NLS-1$
      fMatcher1 = new StringMatcher(pattern1, ignoreCase, false);
      fMatcher2 = new StringMatcher(pattern2, ignoreCase, false);
    }

    @Override
    public boolean match(String text) {
      return fMatcher2.match(text) || fMatcher1.match(text);
    }

  }
  private class OutlineContentProvider extends StandardDartElementContentProvider {

    private boolean fShowInheritedMembers;

    /**
     * Creates a new Outline content provider.
     * 
     * @param showInheritedMembers <code>true</code> iff inherited members are shown
     */
    private OutlineContentProvider(boolean showInheritedMembers) {
      super(true);
      fShowInheritedMembers = showInheritedMembers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
      super.dispose();
      DartX.todo();
      // if (fCategoryFilterActionGroup != null) {
      // fCategoryFilterActionGroup.dispose();
      // fCategoryFilterActionGroup= null;
      // }
      fTypeHierarchies.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(Object element) {
//      if (fShowOnlyMainType) {
//        if (element instanceof ITypeRoot) {
//          element = ((ITypeRoot) element).findPrimaryType();
//        }
//
//        if (element == null)
//          return NO_CHILDREN;
//      }
//
//      if (fShowInheritedMembers && element instanceof Type) {
//        Type type = (Type) element;
//        if (type.getDeclaringType() == null) {
//          TypeHierarchyImpl th = getSuperTypeHierarchy(type);
//          if (th != null) {
//            List children = new ArrayList();
//            Type[] superClasses = th.getAllSuperclasses(type);
//            children.addAll(Arrays.asList(super.getChildren(type)));
//            for (int i = 0, scLength = superClasses.length; i < scLength; i++)
//              children.addAll(Arrays.asList(super.getChildren(superClasses[i])));
//            return children.toArray();
//          }
//        }
//      }
      return super.getChildren(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      super.inputChanged(viewer, oldInput, newInput);
      fTypeHierarchies.clear();
    }

    public boolean isShowingInheritedMembers() {
      return fShowInheritedMembers;
    }

    public void toggleShowInheritedMembers() {
      Tree tree = getTreeViewer().getTree();

      tree.setRedraw(false);
      fShowInheritedMembers = !fShowInheritedMembers;
      getTreeViewer().refresh();
      getTreeViewer().expandToLevel(2);

      // reveal selection
      Object selectedElement = getSelectedElement();
      if (selectedElement != null) {
        getTreeViewer().reveal(selectedElement);
      }

      tree.setRedraw(true);
    }
  }

  private class OutlineLabelProvider extends AppearanceAwareLabelProvider {

    private boolean fShowDefiningType;

    private OutlineLabelProvider() {
      super(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | DartElementLabels.F_APP_TYPE_SIGNATURE
          | DartElementLabels.ALL_CATEGORY, AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
    }

    /*
     * @see org.eclipse.wst.jsdt.internal.ui.viewsupport.JavaUILabelProvider#
     * getForeground(java.lang.Object)
     */
    @Override
    public Color getForeground(Object element) {
      if (fOutlineContentProvider.isShowingInheritedMembers()) {
        if (element instanceof DartElement) {
          DartElement je = (DartElement) element;
          je = je.getAncestor(CompilationUnit.class);
          if (fInput.equals(je)) {
            return null;
          }
        }
        return JFaceResources.getColorRegistry().get(ColoredViewersManager.INHERITED_COLOR_NAME);
      }
      return null;
    }

    /*
     * @see ILabelProvider#getText
     */
    @Override
    public String getText(Object element) {
      String text = super.getText(element);
      if (fShowDefiningType) {
        try {
          Type type = getDefiningType(element);
          if (type != null) {
            StringBuffer buf = new StringBuffer(super.getText(type));
            buf.append(DartElementLabels.CONCAT_STRING);
            buf.append(text);
            return buf.toString();
          }
        } catch (DartModelException e) {
        }
      }
      return text;
    }

//    public boolean isShowDefiningType() {
//      return fShowDefiningType;
//    }

    public void setShowDefiningType(boolean showDefiningType) {
      fShowDefiningType = showDefiningType;
    }

    private Type getDefiningType(Object element) throws DartModelException {
      int kind = ((DartElement) element).getElementType();

      if (kind != DartElement.METHOD && kind != DartElement.FIELD) {
        return null;
      }
      Type declaringType = ((TypeMember) element).getDeclaringType();
      if (kind != DartElement.METHOD) {
        return declaringType;
      }
      if (declaringType == null) {
        return null;
      }
      DartX.todo();
      return null;
//      TypeHierarchyImpl hierarchy = getSuperTypeHierarchy(declaringType);
//      if (hierarchy == null) {
//        return declaringType;
//      }
//      DartFunction method = (DartFunction) element;
//      MethodOverrideTester tester = new MethodOverrideTester(declaringType,
//          hierarchy);
//      DartFunction res = tester.findDeclaringMethod(method, true);
//      if (res == null || method.equals(res)) {
//        return declaringType;
//      }
//      return res.getDeclaringType();
    }
  }

  private class OutlineSorter extends DartElementComparator /* AbstractHierarchyViewerSorter */{

    /*
     * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter
     * #isSortAlphabetically()
     */
//    public boolean isSortAlphabetically() {
//      return fLexicalSortingAction.isChecked();
//    }

    /*
     * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter
     * #isSortByDefiningType()
     */
//    public boolean isSortByDefiningType() {
//      return fSortByDefiningTypeAction.isChecked();
//    }

    /*
     * @see org.eclipse.wst.jsdt.internal.ui.typehierarchy.AbstractHierarchyViewerSorter
     * #getHierarchy(org.eclipse.wst.jsdt.core.IType)
     */
//    protected TypeHierarchyImpl getHierarchy(Type type) {
//      return getSuperTypeHierarchy(type);
//    }
  }

  private class OutlineTreeViewer extends TreeViewer {

    private boolean fIsFiltering = false;

    private OutlineTreeViewer(Tree tree) {
      super(tree);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object[] getFilteredChildren(Object parent) {
      Object[] result = getRawChildren(parent);
      int unfilteredChildren = result.length;
      ViewerFilter[] filters = getFilters();
      if (filters != null) {
        for (int i = 0; i < filters.length; i++) {
          result = filters[i].filter(this, parent, result);
        }
      }
      fIsFiltering = unfilteredChildren != result.length;
      return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalExpandToLevel(Widget node, int level) {
      if (!fIsFiltering && node instanceof TreeItem && getMatcher() == null) {
        TreeItem treeItem = (TreeItem) node;
        if (treeItem.getParentItem() != null && treeItem.getData() instanceof DartElement) {
          DartElement je = (DartElement) treeItem.getData();
          if (je.getElementType() == DartElement.IMPORT_CONTAINER || isInnerType(je)) {
            setExpanded(treeItem, false);
            return;
          }
        }
      }
      super.internalExpandToLevel(node, level);
    }

    private boolean isInnerType(DartElement element) {
      return false;
//      if (element != null && element.getElementType() == DartElement.TYPE) {
//        Type type = (Type) element;
//        try {
//          return type.isMember();
//        } catch (DartModelException e) {
//          DartElement parent = type.getParent();
//          if (parent != null) {
//            int parentElementType = parent.getElementType();
//            return (parentElementType != DartElement.JAVASCRIPT_UNIT && parentElementType != DartElement.CLASS_FILE);
//          }
//        }
//      }
//      return false;
    }
  }

//  private class ShowOnlyMainTypeAction extends Action {
//
//    private static final String STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED = "GoIntoTopLevelTypeAction.isChecked"; //$NON-NLS-1$
//
//    private TreeViewer fOutlineViewer;
//
//    private ShowOnlyMainTypeAction(TreeViewer outlineViewer) {
//      super(
//          TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_label,
//          IAction.AS_CHECK_BOX);
//      setToolTipText(TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_tooltip);
//      setDescription(TextMessages.JavaOutlineInformationControl_GoIntoTopLevelType_description);
//
//      JavaPluginImages.setLocalImageDescriptors(this,
//          "gointo_toplevel_type.gif"); //$NON-NLS-1$
//
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
//          DartHelpContextIds.GO_INTO_TOP_LEVEL_TYPE_ACTION);
//
//      fOutlineViewer = outlineViewer;
//
//      boolean showclass = getDialogSettings().getBoolean(
//          STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED);
//      setTopLevelTypeOnly(showclass);
//    }
//
//    /*
//     * @see org.eclipse.jface.action.Action#run()
//     */
//    public void run() {
//      setTopLevelTypeOnly(!fShowOnlyMainType);
//    }
//
//    private void setTopLevelTypeOnly(boolean show) {
//      fShowOnlyMainType = show;
//      setChecked(show);
//
//      Tree tree = fOutlineViewer.getTree();
//      tree.setRedraw(false);
//
//      fOutlineViewer.refresh(false);
//      if (!fShowOnlyMainType)
//        fOutlineViewer.expandToLevel(2);
//
//      // reveal selection
//      Object selectedElement = getSelectedElement();
//      if (selectedElement != null)
//        fOutlineViewer.reveal(selectedElement);
//
//      tree.setRedraw(true);
//
//      getDialogSettings().put(STORE_GO_INTO_TOP_LEVEL_TYPE_CHECKED, show);
//    }
//  }

  private class SortByDefiningTypeAction extends Action {

    private static final String STORE_SORT_BY_DEFINING_TYPE_CHECKED = "SortByDefiningType.isChecked"; //$NON-NLS-1$

    private TreeViewer fOutlineViewer;

    /**
     * Creates the action.
     * 
     * @param outlineViewer the outline viewer
     */
    private SortByDefiningTypeAction(TreeViewer outlineViewer) {
      super(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_label);
      setDescription(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_description);
      setToolTipText(TextMessages.JavaOutlineInformationControl_SortByDefiningTypeAction_tooltip);

      DartPluginImages.setLocalImageDescriptors(this, "definingtype_sort_co.gif"); //$NON-NLS-1$

      fOutlineViewer = outlineViewer;

      PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
          DartHelpContextIds.SORT_BY_DEFINING_TYPE_ACTION);

      boolean state = getDialogSettings().getBoolean(STORE_SORT_BY_DEFINING_TYPE_CHECKED);
      setChecked(state);
      fInnerLabelProvider.setShowDefiningType(state);
    }

    /*
     * @see Action#actionPerformed
     */
    @Override
    public void run() {
      BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          fInnerLabelProvider.setShowDefiningType(isChecked());
          getDialogSettings().put(STORE_SORT_BY_DEFINING_TYPE_CHECKED, isChecked());

          setMatcherString(fPattern, false);
          fOutlineViewer.refresh(true);

          // reveal selection
          Object selectedElement = getSelectedElement();
          if (selectedElement != null) {
            fOutlineViewer.reveal(selectedElement);
          }
        }
      });
    }
  }

  private KeyAdapter fKeyAdapter;
  private OutlineContentProvider fOutlineContentProvider;

  private DartElement fInput = null;

  private OutlineSorter fOutlineSorter;

  private OutlineLabelProvider fInnerLabelProvider;

//  private boolean fShowOnlyMainType;

  private LexicalSortingAction fLexicalSortingAction;

  private SortByDefiningTypeAction fSortByDefiningTypeAction;

//  private ShowOnlyMainTypeAction fShowOnlyMainTypeAction;

  private Map fTypeHierarchies = new HashMap();

  /**
   * Category filter action group. DartX.todo()
   */
  // private CategoryFilterActionGroup fCategoryFilterActionGroup;
  private String fPattern;

  /**
   * Creates a new Java outline information control.
   * 
   * @param parent
   * @param shellStyle
   * @param treeStyle
   * @param commandId
   */
  public DartOutlineInformationControl(Shell parent, int shellStyle, int treeStyle, String commandId) {
    super(parent, shellStyle, treeStyle, commandId, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInput(Object information) {
    if (information == null || information instanceof String) {
      inputChanged(null, null);
      return;
    }
    DartElement je = (DartElement) information;
    CompilationUnit cu = je.getAncestor(CompilationUnit.class);
    fInput = cu;

    inputChanged(fInput, information);
    DartX.todo();
    // fCategoryFilterActionGroup.setInput(getInputForCategories());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Text createFilterText(Composite parent) {
    Text text = super.createFilterText(parent);
    text.addKeyListener(getKeyAdapter());
    return text;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected TreeViewer createTreeViewer(Composite parent, int style) {
    Tree tree = new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = tree.getItemHeight() * 12;
    tree.setLayoutData(gd);

    final TreeViewer treeViewer = new OutlineTreeViewer(tree);
    ColoredViewersManager.install(treeViewer);

    // Hard-coded filters
    treeViewer.addFilter(new NamePatternFilter());
    treeViewer.addFilter(new MemberFilter());

    fInnerLabelProvider = new OutlineLabelProvider();
    fInnerLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
    IDecoratorManager decoratorMgr = PlatformUI.getWorkbench().getDecoratorManager();
    if (decoratorMgr.getEnabled("com.google.dart.tools.ui.override.decorator")) {
      fInnerLabelProvider.addLabelDecorator(new OverrideIndicatorLabelDecorator(null));
    }

    treeViewer.setLabelProvider(fInnerLabelProvider);

    fLexicalSortingAction = new LexicalSortingAction(treeViewer);
    fSortByDefiningTypeAction = new SortByDefiningTypeAction(treeViewer);
//    fShowOnlyMainTypeAction = new ShowOnlyMainTypeAction(treeViewer);
    DartX.todo();
    // fCategoryFilterActionGroup= new CategoryFilterActionGroup(treeViewer,
    // getId(), getInputForCategories());

    fOutlineContentProvider = new OutlineContentProvider(false);
    treeViewer.setContentProvider(fOutlineContentProvider);
    fOutlineSorter = new OutlineSorter();
    treeViewer.setComparator(fOutlineSorter);
    treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

    treeViewer.getTree().addKeyListener(getKeyAdapter());

    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = treeViewer.getTree().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    treeViewer.getTree().setFont(font);

    return treeViewer;
  }

  /*
   * @see com.google.dart.tools.ui.functions.AbstractInformationControl#fillViewMenu
   * (org.eclipse.jface.action.IMenuManager)
   */
  @Override
  protected void fillViewMenu(IMenuManager viewMenu) {
    super.fillViewMenu(viewMenu);
//    viewMenu.add(fShowOnlyMainTypeAction);

    viewMenu.add(new Separator("Sorters")); //$NON-NLS-1$
    viewMenu.add(fLexicalSortingAction);

    viewMenu.add(fSortByDefiningTypeAction);
    DartX.todo();
    // fCategoryFilterActionGroup.setInput(getInputForCategories());
    // fCategoryFilterActionGroup.contributeToViewMenu(viewMenu);
  }

  /*
   * @see com.google.dart.tools.ui.functions.AbstractInformationControl#getId()
   */
  @Override
  protected String getId() {
    return "com.google.dart.tools.ui.functions.QuickOutline"; //$NON-NLS-1$
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getStatusFieldText() {
//TODO re-enable when we have support for showing inherited members
//    KeySequence[] sequences = getInvokingCommandKeySequences();
//    if (sequences == null || sequences.length == 0) {
//      return ""; //$NON-NLS-1$
//    }
//
//    String keySequence = sequences[0].format();
//
//    if (fOutlineContentProvider.isShowingInheritedMembers()) {
//      return Messages.format(
//          DartUIMessages.JavaOutlineControl_statusFieldText_hideInheritedMembers, keySequence);
//    } else {
//      return Messages.format(
//          DartUIMessages.JavaOutlineControl_statusFieldText_showInheritedMembers, keySequence);
//    }
    return "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleStatusFieldClicked() {
    toggleShowInheritedMembers();
  }

  /*
   * @see com.google.dart.tools.ui.functions.AbstractInformationControl#setMatcherString
   * (java.lang.String, boolean)
   */
  @Override
  protected void setMatcherString(String pattern, boolean update) {
    fPattern = pattern;
    if (pattern.length() == 0 || !fSortByDefiningTypeAction.isChecked()) {
      super.setMatcherString(pattern, update);
      return;
    }

    boolean ignoreCase = pattern.toLowerCase().equals(pattern);
    String pattern2 = "*" + DartElementLabels.CONCAT_STRING + pattern; //$NON-NLS-1$
    fStringMatcher = new OrStringMatcher(pattern, pattern2, ignoreCase, false);

    if (update) {
      stringMatcherUpdated();
    }

  }

  protected void toggleShowInheritedMembers() {
//TODO re-enable when we have support for showing inherited members
//    long flags = fInnerLabelProvider.getTextFlags();
//    flags ^= DartElementLabels.ALL_POST_QUALIFIED;
//    fInnerLabelProvider.setTextFlags(flags);
//    fOutlineContentProvider.toggleShowInheritedMembers();
//    updateStatusFieldText();
    DartX.todo();
    // fCategoryFilterActionGroup.setInput(getInputForCategories());
  }

  private DartElement[] getInputForCategories() {
    if (fInput == null) {
      return new DartElement[0];
    }

//    if (fOutlineContentProvider.isShowingInheritedMembers()) {
//      DartElement p = fInput;
//      if (p instanceof ITypeRoot) {
//        p = ((ITypeRoot) p).findPrimaryType();
//      }
//      while (p != null && !(p instanceof Type)) {
//        p = p.getParent();
//      }
//      if (!(p instanceof Type))
//        return new DartElement[]{fInput};
//
//      TypeHierarchyImpl hierarchy = getSuperTypeHierarchy((Type) p);
//      if (hierarchy == null)
//        return new DartElement[]{fInput};
//
//      Type[] supertypes = hierarchy.getAllSuperclasses((Type) p);
//      DartElement[] result = new DartElement[supertypes.length + 1];
//      result[0] = fInput;
//      System.arraycopy(supertypes, 0, result, 1, supertypes.length);
//      return result;
//    } else {
    return new DartElement[] {fInput};
//    }
  }

  private KeyAdapter getKeyAdapter() {
    if (fKeyAdapter == null) {
      fKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
          KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
          KeySequence[] sequences = getInvokingCommandKeySequences();
          if (sequences == null) {
            return;
          }
          for (int i = 0; i < sequences.length; i++) {
            if (sequences[i].equals(keySequence)) {
              e.doit = false;
              toggleShowInheritedMembers();
              return;
            }
          }
        }
      };
    }
    return fKeyAdapter;
  }

  private IProgressMonitor getProgressMonitor() {
    IWorkbenchPage wbPage = DartToolsPlugin.getActivePage();
    if (wbPage == null) {
      return null;
    }

    IEditorPart editor = wbPage.getActiveEditor();
    if (editor == null) {
      return null;
    }

    return editor.getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor();
  }

//  private TypeHierarchyImpl getSuperTypeHierarchy(Type type) {
//    TypeHierarchyImpl th = (TypeHierarchyImpl) fTypeHierarchies.get(type);
//    if (th == null) {
//      try {
//        th = SuperTypeHierarchyCache.getTypeHierarchy(type,
//            getProgressMonitor());
//      } catch (DartModelException e) {
//        return null;
//      } catch (OperationCanceledException e) {
//        return null;
//      }
//      fTypeHierarchies.put(type, th);
//    }
//    return th;
//  }

}
