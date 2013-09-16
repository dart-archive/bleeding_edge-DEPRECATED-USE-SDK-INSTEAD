/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentoutline;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * A listener on the given action's PreferenceStore. It calls .update() on the action when the given
 * key changes value.
 */
public class PropertyChangeUpdateActionContributionItem extends ActionContributionItem {

  private class PreferenceUpdateListener implements IPropertyChangeListener {
    public void propertyChange(PropertyChangeEvent event) {
      if (event.getProperty().equals(fProperty)) {
        if (debug) {
          System.out.println(fProperty + " preference changed, updating " + getAction()); //$NON-NLS-1$
        }
        ((IUpdate) getAction()).update();
      }
    }
  }

  static final boolean debug = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.ui/propertyChangeUpdateActionContributionItem")); //$NON-NLS-1$  //$NON-NLS-2$;

  private IPropertyChangeListener fListener = null;

  protected String fProperty = null;
  private IPreferenceStore fStore;

  public PropertyChangeUpdateActionContributionItem(PropertyChangeUpdateAction action) {
    super(action);
    fProperty = action.getPreferenceKey();
    fStore = action.getPreferenceStore();
    fListener = new PreferenceUpdateListener();
    connect();
  }

  public void connect() {
    if (debug) {
      System.out.println("PropertyChangeUpdateActionContributionItem started listening for " + fProperty); //$NON-NLS-1$
    }
    if (fStore != null) {
      fStore.addPropertyChangeListener(fListener);
    }
  }

  public void disconnect() {
    if (debug) {
      System.out.println("PropertyChangeUpdateActionContributionItem stopped listening for " + fProperty); //$NON-NLS-1$
    }
    if (fStore != null) {
      fStore.removePropertyChangeListener(fListener);
    }
  }

  public void dispose() {
    super.dispose();
    disconnect();
    fProperty = null;
    fStore = null;
  }

  public String toString() {
    if (getAction().getId() != null)
      return super.toString();
    else
      return getClass().getName() + "(text=" + getAction().getText() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
  }
}
