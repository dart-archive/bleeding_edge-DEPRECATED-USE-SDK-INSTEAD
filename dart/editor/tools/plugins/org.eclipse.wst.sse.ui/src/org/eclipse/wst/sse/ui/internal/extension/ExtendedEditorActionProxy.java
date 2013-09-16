/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.extension;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.sse.ui.internal.IExtendedSimpleEditor;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISelfValidateEditAction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Begins and ends UndoManager recording around run() and runWithEvent(...)
 */
public class ExtendedEditorActionProxy implements InvocationHandler {
  public static Object newInstance(Object obj) {
    Set set = new HashSet();
    Class clazz = obj.getClass();
    while (clazz != null) {
      Class[] interfaces = clazz.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        set.add(interfaces[i]);
      }
      clazz = clazz.getSuperclass();
    }
    Class[] classes = new Class[set.size()];
    Iterator itr = set.iterator();
    int i = 0;
    while (itr.hasNext()) {
      classes[i] = (Class) itr.next();
      i++;
    }
    return Proxy.newProxyInstance(obj.getClass().getClassLoader(), classes,
        new ExtendedEditorActionProxy(obj));
  }

  private IExtendedSimpleEditor editor = null;
  private IStructuredModel fRecorder;
  private Object obj;

  private ExtendedEditorActionProxy(Object obj) {
    this.obj = obj;
  }

  private void beginRecording() {
    IDocument document = null;
    if (editor != null) {
      document = editor.getDocument();
      if (document != null)
        fRecorder = StructuredModelManager.getModelManager().getExistingModelForEdit(document);
      // Prepare for Undo
      if (fRecorder != null) {
        IStructuredTextUndoManager um = fRecorder.getUndoManager();
        if (um != null) {
          um.beginRecording(this, ((IAction) this.obj).getText(),
              ((IAction) this.obj).getDescription());
        }
      }
    }
  }

  private void endRecording() {
    if (fRecorder != null) {
      IStructuredTextUndoManager um = fRecorder.getUndoManager();
      if (um != null) {
        um.endRecording(this);
      }
      fRecorder.releaseFromEdit();
      fRecorder = null;
    }
  }

  /**
   * @see java.lang.reflect.InvocationHandler#invoke(Object, Method, Object[])
   */
  public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
    Object result = null;
    String name = m.getName();
    try {
      if (name.equals("equals")) { //$NON-NLS-1$
        // Workaround for JDK's bug 4652876
        // "equals" always returns false even if both
        // InvocationHandler
        // class
        // hold the same objects
        // See
        // http://developer.java.sun.com/developer/bugParade/bugs/4652876.html
        // This problem is in the IBM SDK 1.3.1
        // but I don't see the bug in Sun's JDK 1.4.1 (beta)
        Object arg = args[0];
        return (proxy.getClass() == arg.getClass() && equals(Proxy.getInvocationHandler(arg)))
            ? Boolean.TRUE : Boolean.FALSE;
      } else if (name.equals("runWithEvent") || name.equals("run")) { //$NON-NLS-1$  //$NON-NLS-2$
        beginRecording();
        if ((editor != null) && !(this.obj instanceof ISelfValidateEditAction)) {

          // TODO: cleanup validateEdit
          // just leaving this check and following code here for transition. 
          // I assume we'll remove all need for 'validateEdit'
          // or move to platform editor's validateState 

//					IStatus status = editor.validateEdit(getDisplay().getActiveShell());
//					if (!status.isOK()) {
//						return null;
//					}
        }
      } else if (name.equals("setActiveExtendedEditor")) { //$NON-NLS-1$
        if (args[0] instanceof IExtendedSimpleEditor) {
          editor = (IExtendedSimpleEditor) args[0];
        }
      }
      result = m.invoke(this.obj, args);
    } catch (InvocationTargetException e) {
      Logger.logException(e.getTargetException());
      //throw e.getTargetException();
    } catch (Exception e) {
      Logger.logException(e);
      if (name.equals("runWithEvent") || name.equals("run")) { //$NON-NLS-1$  //$NON-NLS-2$
        // only expose user-driven exceptions from "running" to the
        // user
        throw new RuntimeException(e.getMessage());
      }
    } finally {
      if (name.equals("runWithEvent") || name.equals("run")) { //$NON-NLS-1$  //$NON-NLS-2$
        endRecording();
      }
    }
    return result;
  }
}
