/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.views.contentoutline;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.contentoutline.PropertyChangeUpdateAction;
import org.eclipse.wst.sse.ui.internal.contentoutline.PropertyChangeUpdateActionContributionItem;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImageHelper;
import org.eclipse.wst.sse.ui.internal.editor.EditorPluginImages;

/**
 * Basic Configuration class for Outline Pages
 * 
 * @since 1.0
 */
public abstract class ContentOutlineConfiguration {

  /**
   * Add a collapse action to help with navigation.
   */
  private class CollapseTreeAction extends Action {
    private TreeViewer fTreeViewer = null;

    public CollapseTreeAction(TreeViewer viewer) {
      super(SSEUIMessages.ContentOutlineConfiguration_0, AS_PUSH_BUTTON); //$NON-NLS-1$
      setImageDescriptor(COLLAPSE_E);
      setDisabledImageDescriptor(COLLAPSE_D);
      setToolTipText(getText());
      fTreeViewer = viewer;
    }

    public void run() {
      super.run();
      fTreeViewer.collapseAll();
    }
  }

  /**
   * Toggles whether incoming selection notification affects us
   */
  private class ToggleLinkAction extends PropertyChangeUpdateAction {
    public ToggleLinkAction(IPreferenceStore store, String preference) {
      super(SSEUIMessages.ContentOutlineConfiguration_1, store, preference, true); //$NON-NLS-1$
      setToolTipText(getText());
      setDisabledImageDescriptor(SYNCED_D);
      setImageDescriptor(SYNCED_E);
      update();
    }

    public void update() {
      super.update();
      setLinkWithEditor(isChecked());
    }
  }

  ImageDescriptor COLLAPSE_D = EditorPluginImageHelper.getInstance().getImageDescriptor(
      EditorPluginImages.IMG_DLCL_COLLAPSEALL);
  ImageDescriptor COLLAPSE_E = EditorPluginImageHelper.getInstance().getImageDescriptor(
      EditorPluginImages.IMG_ELCL_COLLAPSEALL);

  private boolean fIsLinkWithEditor = false;

  private ILabelProvider fLabelProvider;
  private IContributionItem[] fMenuContributions = null;
  private IContributionItem[] fToolbarContributions = null;
  private final String OUTLINE_LINK_PREF = "outline-link-editor"; //$NON-NLS-1$
  private static final String OUTLINE_FILTER_PREF = "org.eclipse.wst.sse.ui.OutlinePage"; //$NON-NLS-1$
  ImageDescriptor SYNCED_D = EditorPluginImageHelper.getInstance().getImageDescriptor(
      EditorPluginImages.IMG_DLCL_SYNCED);
  ImageDescriptor SYNCED_E = EditorPluginImageHelper.getInstance().getImageDescriptor(
      EditorPluginImages.IMG_ELCL_SYNCED);

  /**
   * Create new instance of ContentOutlineConfiguration
   */
  public ContentOutlineConfiguration() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  /**
   * Creates the contributions for the view's local menu. Subclasses should merge their
   * contributions with these.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return menu contributions
   */
  protected IContributionItem[] createMenuContributions(TreeViewer viewer) {
    IContributionItem toggleLinkItem = new PropertyChangeUpdateActionContributionItem(
        new ToggleLinkAction(getPreferenceStore(), OUTLINE_LINK_PREF));
    IContributionItem[] items = new IContributionItem[] {toggleLinkItem};
    return items;
  }

  /**
   * Creates the toolbar contributions. Subclasses should merge their contributions with these.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return toolbar contributions
   */
  protected IContributionItem[] createToolbarContributions(TreeViewer viewer) {
    IContributionItem collapseAllItem = new ActionContributionItem(new CollapseTreeAction(viewer));
    IContributionItem[] items = new IContributionItem[] {collapseAllItem};
    return items;
  }

  /**
   * Returns the ContentProvider to use with the given viewer.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return the IContentProvider to use with this viewer
   */
  public abstract IContentProvider getContentProvider(TreeViewer viewer);

  /**
   * Returns an array of KeyListeners to attach to the given viewer's control or null.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return an array of KeyListeners to attach to the TreeViewer's Control, or null. The listeners
   *         should adhere to the KeyEvent.doit field to ensure proper behaviors. Ordering of the
   *         event notifications is dependent on the Control in the TreeViewer.
   */
  public KeyListener[] getKeyListeners(TreeViewer viewer) {
    return null;
  }

  /**
   * Returns the LabelProvider for the items within the given viewer.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return the ILabelProvider for items within the viewer
   */
  public ILabelProvider getLabelProvider(TreeViewer viewer) {
    if (fLabelProvider == null)
      fLabelProvider = new LabelProvider();
    return fLabelProvider;
  }

  /**
   * Returns the menu contribution items for the local menu in the outline.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return IContributionItem[] for the local menu
   */
  public final IContributionItem[] getMenuContributions(TreeViewer viewer) {
    if (fMenuContributions == null
        && (viewer.getControl() != null && !viewer.getControl().isDisposed())) {
      fMenuContributions = createMenuContributions(viewer);
    }
    return fMenuContributions;
  }

  /**
   * Returns the menu listener to notify when the given viewer's context menu is about to be shown
   * or null.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return the IMenuListener to notify when the viewer's context menu is about to be shown, or
   *         null
   */
  public IMenuListener getMenuListener(TreeViewer viewer) {
    return null;
  }

  /**
   * Returns the PreferenceStore to use for this configuration.
   * 
   * @return the preference store in which to remember preferences (such as the link-with-editor
   *         toggle state)
   */
  protected IPreferenceStore getPreferenceStore() {
    return SSEUIPlugin.getInstance().getPreferenceStore();
  }

  /**
   * Returns the (filtered) selection from the given selection.
   * 
   * @param selection model selection
   * @param viewer the TreeViewer associated with this configuration
   * @return The (filtered) selection from this event. Uses include mapping model selection onto
   *         elements provided by the content provider. Should only return elements that will be
   *         shown in the Tree Control.
   */
  public ISelection getSelection(TreeViewer viewer, ISelection selection) {
    return selection;
  }

  /**
   * @since 2.0
   * @param treeViewer
   * @return a label provider providing the status line contents
   */
  public ILabelProvider getStatusLineLabelProvider(TreeViewer treeViewer) {
    return null;
  }

  /**
   * Returns contribution items for the local toolbar in the outline.
   * 
   * @param viewer the TreeViewer associated with this configuration
   * @return IContributionItem[] for the local toolbar
   */
  public final IContributionItem[] getToolbarContributions(TreeViewer viewer) {
    if (fToolbarContributions == null
        && (viewer.getControl() != null && !viewer.getControl().isDisposed())) {
      fToolbarContributions = createToolbarContributions(viewer);
    }
    return fToolbarContributions;
  }

  /**
   * Adopted since you can't easily removeDragSupport from StructuredViewers.
   * 
   * @param treeViewer the TreeViewer associated with this configuration
   * @return an array of TransferDragSourceListeners
   */
  public TransferDragSourceListener[] getTransferDragSourceListeners(TreeViewer treeViewer) {
    return new TransferDragSourceListener[0];
  }

  /**
   * Adopted since you can't easily removeDropSupport from StructuredViewers.
   * 
   * @param treeViewer the TreeViewer associated with this configuration
   * @return an array of TransferDropTargetListeners
   */
  public TransferDropTargetListener[] getTransferDropTargetListeners(TreeViewer treeViewer) {
    return new TransferDropTargetListener[0];
  }

  /**
   * Returns true if node selection changes affect selection in the TreeViewer.
   * 
   * @param treeViewer the TreeViewer associated with this configuration
   * @return true if outline is currently linked to selection in editor, false otherwise
   */
  public boolean isLinkedWithEditor(TreeViewer treeViewer) {
    return fIsLinkWithEditor;
  }

  /**
   * Sets whether or not outline view should be linked with selection in editor.
   * 
   * @param isLinkWithEditor The isLinkWithEditor to set.
   */
  void setLinkWithEditor(boolean isLinkWithEditor) {
    fIsLinkWithEditor = isLinkWithEditor;
  }

  /**
   * General hook for resource releasing and listener removal when configurations change or the
   * viewer is disposed of. This implementation stops of any remaining
   * PropertyChangeUpdateActionContributionItem from preference listening.
   * 
   * @param viewer the TreeViewer associated with this configuration
   */
  public void unconfigure(TreeViewer viewer) {
    if (fToolbarContributions != null) {
      for (int i = 0; i < fToolbarContributions.length; i++) {
        if (fToolbarContributions[i] instanceof PropertyChangeUpdateActionContributionItem) {
          ((PropertyChangeUpdateActionContributionItem) fToolbarContributions[i]).disconnect();
        }
      }
      fToolbarContributions = null;
    }
    if (fMenuContributions != null) {
      for (int i = 0; i < fMenuContributions.length; i++) {
        if (fMenuContributions[i] instanceof PropertyChangeUpdateActionContributionItem) {
          ((PropertyChangeUpdateActionContributionItem) fMenuContributions[i]).disconnect();
        }
      }
      fMenuContributions = null;
    }
  }

  /**
   * Provides the target used when associating filters to the outline
   * 
   * @return The target id used when associating filters to the outline
   */
  protected String getOutlineFilterTarget() {
    return OUTLINE_FILTER_PREF;
  }

  /**
   * Returns the content outline filter processor for this configuration
   * 
   * @param viewer the {@link TreeViewer} that is associated with the filter
   * @return A {@link ContentOutlineFilterProcessor} to filter nodes in the outline
   */
  public ContentOutlineFilterProcessor getOutlineFilterProcessor(TreeViewer viewer) {
    return new ContentOutlineFilterProcessor(getPreferenceStore(), getOutlineFilterTarget(), viewer);
  }
}
