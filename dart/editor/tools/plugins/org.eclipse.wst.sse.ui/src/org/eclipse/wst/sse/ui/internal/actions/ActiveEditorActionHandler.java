/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @deprecated actions are not properly activated with this handler so do not use
 */
public class ActiveEditorActionHandler implements IAction {

  private String fActionId;
  private IWorkbenchSite fSite;
  private IAction fTargetAction;

  public ActiveEditorActionHandler(IWorkbenchSite site, String id) {
    super();
    fActionId = id;
    fSite = site;
  }

  /**
   * @see org.eclipse.jface.action.IAction#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
   */
  public void addPropertyChangeListener(IPropertyChangeListener listener) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#getAccelerator()
   */
  public int getAccelerator() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getAccelerator() : 0);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getActionDefinitionId()
   */
  public String getActionDefinitionId() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getActionDefinitionId() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getDescription()
   */
  public String getDescription() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getDescription() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getDisabledImageDescriptor()
   */
  public ImageDescriptor getDisabledImageDescriptor() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getDisabledImageDescriptor() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getHelpListener()
   */
  public HelpListener getHelpListener() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getHelpListener() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getHoverImageDescriptor()
   */
  public ImageDescriptor getHoverImageDescriptor() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getHoverImageDescriptor() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getId()
   */
  public String getId() {
    return getClass().getName() + hashCode();
  }

  /**
   * @see org.eclipse.jface.action.IAction#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getImageDescriptor() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getMenuCreator()
   */
  public IMenuCreator getMenuCreator() {
    return null;
  }

  /**
   * @see org.eclipse.jface.action.IAction#getStyle()
   */
  public int getStyle() {
    return IAction.AS_PUSH_BUTTON;
  }

  /**
   * @see org.eclipse.jface.action.IAction#getText()
   */
  public String getText() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getText() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#getToolTipText()
   */
  public String getToolTipText() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.getToolTipText() : null);
  }

  /**
   * @see org.eclipse.jface.action.IAction#isChecked()
   */
  public boolean isChecked() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.isChecked() : true);
  }

  /**
   * @see org.eclipse.jface.action.IAction#isEnabled()
   */
  public boolean isEnabled() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.isEnabled() : false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#isHandled()
   */
  public boolean isHandled() {
    updateTargetAction();
    return (fTargetAction != null ? fTargetAction.isHandled() : false);
  }

  /**
   * @see org.eclipse.jface.action.IAction#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
   */
  public void removePropertyChangeListener(IPropertyChangeListener listener) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#run()
   */
  public void run() {
    updateTargetAction();
    if (fTargetAction != null)
      fTargetAction.run();
  }

  /**
   * @see org.eclipse.jface.action.IAction#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  public void runWithEvent(Event event) {
    updateTargetAction();
    if (fTargetAction != null)
      fTargetAction.runWithEvent(event);
  }

  /**
   * NOT SUPPORTED
   * 
   * @see org.eclipse.jface.action.IAction#setAccelerator(int)
   */
  public void setAccelerator(int keycode) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setActionDefinitionId(java.lang.String)
   */
  public void setActionDefinitionId(String id) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setChecked(boolean)
   */
  public void setChecked(boolean checked) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setDescription(java.lang.String)
   */
  public void setDescription(String text) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setDisabledImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
   */
  public void setDisabledImageDescriptor(ImageDescriptor newImage) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setEnabled(boolean)
   */
  public void setEnabled(boolean enabled) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setHelpListener(org.eclipse.swt.events.HelpListener)
   */
  public void setHelpListener(HelpListener listener) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setHoverImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
   */
  public void setHoverImageDescriptor(ImageDescriptor newImage) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setId(java.lang.String)
   */
  public void setId(String id) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
   */
  public void setImageDescriptor(ImageDescriptor newImage) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setMenuCreator(org.eclipse.jface.action.IMenuCreator)
   */
  public void setMenuCreator(IMenuCreator creator) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setText(java.lang.String)
   */
  public void setText(String text) {
  }

  /**
   * @see org.eclipse.jface.action.IAction#setToolTipText(java.lang.String)
   */
  public void setToolTipText(String text) {
  }

  private void updateTargetAction() {
    if (fSite != null && fSite.getWorkbenchWindow() != null
        && fSite.getWorkbenchWindow().getActivePage() != null) {
      IEditorPart part = fSite.getWorkbenchWindow().getActivePage().getActiveEditor();
      ITextEditor editor = null;
      if (part instanceof ITextEditor)
        editor = (ITextEditor) part;
      else
        editor = (ITextEditor) (part != null ? part.getAdapter(ITextEditor.class) : null);
      if (editor != null) {
        fTargetAction = editor.getAction(fActionId);
      } else {
        fTargetAction = null;
      }
    } else
      fTargetAction = null;
  }
}
