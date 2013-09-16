/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.reconcile.validator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Right now we'll only use one reporter per validator.
 */
public class IncrementalReporter implements IReporter {
  private IProgressMonitor fProgressMonitor;
  private HashMap messages = new HashMap();

  public IncrementalReporter(IProgressMonitor progressMonitor) {
    super();
    fProgressMonitor = progressMonitor;
  }

  public void addMessage(IValidator validator, IMessage message) {
    AnnotationInfo info = new AnnotationInfo(message);
    addAnnotationInfo(validator, info);
  }

  public final void addAnnotationInfo(IValidator validator, AnnotationInfo info) {
    Object existingValue = messages.get(validator);
    if (existingValue != null) {
      ((HashSet) existingValue).add(info);
    } else {
      HashSet newValue = new HashSet(1);
      newValue.add(info);
      messages.put(validator, newValue);
    }
  }

  public void displaySubtask(IValidator validator, IMessage message) {
    if ((message == null) || (message.equals(""))) { //$NON-NLS-1$
      return;
    }
    if (fProgressMonitor != null) {
      fProgressMonitor.subTask(message.getText(validator.getClass().getClassLoader()));
    }
  }

  public List getMessages() {
    List result = new ArrayList();
    // messages is a list of:
    // validators => HashSet(AnnotationInfo1, AnnotationInfo2, ...)
    // (one HashSet per validator...)
    Object[] lists = messages.values().toArray();
    for (int i = 0; i < lists.length; i++) {
      Iterator it = ((HashSet) lists[i]).iterator();
      while (it.hasNext()) {
        AnnotationInfo info = (AnnotationInfo) it.next();
        result.add(info.getMessage());
      }
    }
    return result;
  }

  public AnnotationInfo[] getAnnotationInfo() {
    List result = new ArrayList();
    Object[] infos = messages.values().toArray();
    for (int i = 0; i < infos.length; i++) {
      result.addAll((HashSet) infos[i]);
    }
    return (AnnotationInfo[]) result.toArray(new AnnotationInfo[result.size()]);
  }

  public boolean isCancelled() {
    if (fProgressMonitor == null)
      return false;
    return fProgressMonitor.isCanceled();
  }

  public void removeAllMessages(IValidator validator) {
    Object o = messages.get(validator);
    if (o != null && o instanceof HashSet) {
      ((HashSet) o).clear();
    }
  }

  public void removeAllMessages(IValidator validator, Object object) {
    removeAllMessages(validator);
  }

  // group names are unsupported
  public void removeMessageSubset(IValidator validator, Object obj, String groupName) {
    removeAllMessages(validator);
  }
}
