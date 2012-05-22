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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.callhierarchy.OpenCallHierarchyAction;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.Page;

/**
 * Action group that adds actions to open a new view part or an external viewer to a context menu
 * and the global menu bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OpenViewActionGroup extends ActionGroup {

  private boolean fEditorIsOwner;
//	private boolean fIsTypeHiararchyViewerOwner;
  private boolean fIsCallHiararchyViewerOwner;

  private ISelectionProvider fSelectionProvider;

//	private OpenSuperImplementationAction fOpenSuperImplementation;
//	private OpenImplementationAction fOpenImplementation;

//	private OpenAttachedJavadocAction fOpenAttachedJavadoc;
//	private OpenTypeHierarchyAction fOpenTypeHierarchy;
  private OpenCallHierarchyAction fOpenCallHierarchy;
  private PropertyDialogAction fOpenPropertiesDialog;

  private boolean fShowOpenPropertiesAction = true;

//  private boolean fShowShowInMenu = true;

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param part the editor part
   * @noreference This constructor is not intended to be referenced by clients.
   */
  public OpenViewActionGroup(DartEditor part) {
    fEditorIsOwner = true;
//    fShowShowInMenu = false;

//		fOpenImplementation= new OpenImplementationAction(part);
//		fOpenImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_IMPLEMENTATION);
//		part.setAction("OpenImplementation", fOpenImplementation); //$NON-NLS-1$

//		fOpenSuperImplementation= new OpenSuperImplementationAction(part);
//		fOpenSuperImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION);
//		part.setAction("OpenSuperImplementation", fOpenSuperImplementation); //$NON-NLS-1$

//		fOpenAttachedJavadoc= new OpenAttachedJavadocAction(part);
//		fOpenAttachedJavadoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_ATTACHED_JAVADOC);
//		part.setAction("OpenAttachedJavadoc", fOpenAttachedJavadoc); //$NON-NLS-1$

//		fOpenTypeHierarchy= new OpenTypeHierarchyAction(part);
//		fOpenTypeHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
//		part.setAction("OpenTypeHierarchy", fOpenTypeHierarchy); //$NON-NLS-1$

    fOpenCallHierarchy = new OpenCallHierarchyAction(part);
    fOpenCallHierarchy.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_CALL_HIERARCHY);
    part.setAction("OpenCallHierarchy", fOpenCallHierarchy); //$NON-NLS-1$

    initialize(part.getEditorSite().getSelectionProvider());
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the part's selection provider is of type {@link IStructuredSelection}.
   * 
   * @param part the view part that owns this action group
   */
  public OpenViewActionGroup(IViewPart part) {
    this(part, null);
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param part the view part that owns this action group
   * @param selectionProvider the selection provider used instead of the page selection provider.
   */
  public OpenViewActionGroup(IViewPart part, ISelectionProvider selectionProvider) {
    createSiteActions(part.getSite(), selectionProvider);
    // we do a name check here to avoid class loading.
    String partName = part.getClass().getName();
//		fIsTypeHiararchyViewerOwner= "org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart".equals(partName); //$NON-NLS-1$
    fIsCallHiararchyViewerOwner = "com.google.dart.tools.ui.callhierarchy.CallHierarchyViewPart".equals(partName); //$NON-NLS-1$
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param site the site that will own the action group.
   * @param selectionProvider the selection provider used instead of the page selection provider.
   */
  public OpenViewActionGroup(IWorkbenchSite site, ISelectionProvider selectionProvider) {
    createSiteActions(site, selectionProvider);
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the page's selection provider is of type {@link IStructuredSelection}.
   * 
   * @param page the page that owns this action group
   */
  public OpenViewActionGroup(Page page) {
    createSiteActions(page.getSite(), null);
  }

  /**
   * Creates a new <code>OpenActionGroup</code>. The group requires that the selection provided by
   * the given selection provider is of type {@link IStructuredSelection}.
   * 
   * @param page the page that owns this action group
   * @param selectionProvider the selection provider used instead of the page selection provider.
   */
  public OpenViewActionGroup(Page page, ISelectionProvider selectionProvider) {
    createSiteActions(page.getSite(), selectionProvider);
  }

  /**
   * Specifies if this action group also contains the 'Properties' action (
   * {@link PropertyDialogAction}). By default, the action is contained in the group.
   * 
   * @param enable If set, the 'Properties' action is part of this action group
   */
  public void containsOpenPropertiesAction(boolean enable) {
    fShowOpenPropertiesAction = enable;
  }

  /**
   * Specifies if this action group also contains the 'Show In' menu (See
   * {@link ContributionItemFactory#VIEWS_SHOW_IN}). By default, the action is contained in the
   * group except for editors.
   * 
   * @param enable If set, the 'Show In' menu is part of this action group
   */
  public void containsShowInMenu(boolean enable) {
//    fShowShowInMenu = enable;
  }

  /*
   * @see ActionGroup#dispose()
   */
  @Override
  public void dispose() {
//		fSelectionProvider.removeSelectionChangedListener(fOpenImplementation);
//		fSelectionProvider.removeSelectionChangedListener(fOpenSuperImplementation);
//		fSelectionProvider.removeSelectionChangedListener(fOpenAttachedJavadoc);
//		fSelectionProvider.removeSelectionChangedListener(fOpenTypeHierarchy);
    fSelectionProvider.removeSelectionChangedListener(fOpenCallHierarchy);
    super.dispose();
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillActionBars(IActionBars actionBar) {
    super.fillActionBars(actionBar);
    setGlobalActionHandlers(actionBar);
  }

  /*
   * (non-Javadoc) Method declared in ActionGroup
   */
  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
//		if (!fIsTypeHiararchyViewerOwner)
//			appendToGroup(menu, fOpenTypeHierarchy);
    if (!fIsCallHiararchyViewerOwner) {
      appendToGroup(menu, fOpenCallHierarchy);
    }

//    if (fShowShowInMenu) {
//			MenuManager showInSubMenu= new MenuManager(getShowInMenuLabel());
//			IWorkbenchWindow workbenchWindow= fOpenSuperImplementation.getSite().getWorkbenchWindow();
//			showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(workbenchWindow));
//			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, showInSubMenu);
//    }

    IStructuredSelection selection = getStructuredSelection();
    if (fShowOpenPropertiesAction && selection != null
        && fOpenPropertiesDialog.isApplicableForSelection()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fOpenPropertiesDialog);
    }
  }

  private void appendToGroup(IMenuManager menu, IAction action) {
    if (action.isEnabled()) {
      menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
    }
  }

  private void createSiteActions(IWorkbenchSite site, ISelectionProvider specialProvider) {
//		fOpenImplementation= new OpenImplementationAction(site);
//		fOpenImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_IMPLEMENTATION);
//		fOpenImplementation.setSpecialSelectionProvider(specialProvider);

//		fOpenSuperImplementation= new OpenSuperImplementationAction(site);
//		fOpenSuperImplementation.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION);
//		fOpenSuperImplementation.setSpecialSelectionProvider(specialProvider);

//		fOpenAttachedJavadoc= new OpenAttachedJavadocAction(site);
//		fOpenAttachedJavadoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_ATTACHED_JAVADOC);
//		fOpenAttachedJavadoc.setSpecialSelectionProvider(specialProvider);

//		fOpenTypeHierarchy= new OpenTypeHierarchyAction(site);
//		fOpenTypeHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_TYPE_HIERARCHY);
//		fOpenTypeHierarchy.setSpecialSelectionProvider(specialProvider);

    fOpenCallHierarchy = new OpenCallHierarchyAction(site);
    fOpenCallHierarchy.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_CALL_HIERARCHY);
    fOpenCallHierarchy.setSpecialSelectionProvider(specialProvider);

    ISelectionProvider provider = specialProvider != null ? specialProvider
        : site.getSelectionProvider();

    fOpenPropertiesDialog = new PropertyDialogAction(site, provider);
    fOpenPropertiesDialog.setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);

    initialize(provider);
  }

  @SuppressWarnings("unused")
  private String getShowInMenuLabel() {
    String keyBinding = null;

    IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(
        IBindingService.class);
    if (bindingService != null) {
      keyBinding = bindingService.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
    }

    if (keyBinding == null) {
      keyBinding = ""; //$NON-NLS-1$
    }

    return ActionMessages.OpenViewActionGroup_showInAction_label + '\t' + keyBinding;
  }

  private IStructuredSelection getStructuredSelection() {
    ISelection selection = getContext().getSelection();
    if (selection instanceof IStructuredSelection) {
      return (IStructuredSelection) selection;
    }
    return null;
  }

  private void initialize(ISelectionProvider provider) {
    fSelectionProvider = provider;
    ISelection selection = provider.getSelection();
//		fOpenImplementation.update(selection);
//		fOpenSuperImplementation.update(selection);
//		fOpenAttachedJavadoc.update(selection);
//		fOpenTypeHierarchy.update(selection);
    fOpenCallHierarchy.update(selection);
    if (!fEditorIsOwner) {
      if (fShowOpenPropertiesAction) {
        if (selection instanceof IStructuredSelection) {
          fOpenPropertiesDialog.selectionChanged((IStructuredSelection) selection);
        } else {
          fOpenPropertiesDialog.selectionChanged(selection);
        }
      }
//			provider.addSelectionChangedListener(fOpenImplementation);
//			provider.addSelectionChangedListener(fOpenSuperImplementation);
//			provider.addSelectionChangedListener(fOpenAttachedJavadoc);
//			provider.addSelectionChangedListener(fOpenTypeHierarchy);
      provider.addSelectionChangedListener(fOpenCallHierarchy);
      // no need to register the open properties dialog action since it registers itself
    }
  }

  private void setGlobalActionHandlers(IActionBars actionBars) {
//		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_IMPLEMENTATION, fOpenImplementation);
//		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_SUPER_IMPLEMENTATION, fOpenSuperImplementation);
//		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_ATTACHED_JAVA_DOC, fOpenAttachedJavadoc);
//		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_TYPE_HIERARCHY, fOpenTypeHierarchy);
    actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_CALL_HIERARCHY, fOpenCallHierarchy);

    if (!fEditorIsOwner && fShowOpenPropertiesAction) {
      actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fOpenPropertiesDialog);
    }
  }

}
