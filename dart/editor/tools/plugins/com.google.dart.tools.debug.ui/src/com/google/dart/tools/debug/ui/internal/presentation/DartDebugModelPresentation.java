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
package com.google.dart.tools.debug.ui.internal.presentation;

import com.google.dart.tools.debug.core.dartium.DartiumDebugValue;
import com.google.dart.tools.debug.core.dartium.DartiumDebugVariable;
import com.google.dart.tools.debug.core.server.ServerDebugVariable;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.DebuggerEditorInput;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A debug model presentation is responsible for providing labels, images, and editors associated
 * with debug elements in a specific debug model.
 */
public class DartDebugModelPresentation implements IDebugModelPresentation {

  private static final String DART_EDITOR_ID = "com.google.dart.tools.ui.text.editor.CompilationUnitEditor";

  private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

  public DartDebugModelPresentation() {

  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);
  }

  /**
   * Computes a detailed description of the given value, reporting the result to the specified
   * listener. This allows a presentation to provide extra details about a selected value in the
   * variable detail portion of the variables view. Since this can be a long-running operation, the
   * details are reported back to the specified listener asynchronously. If <code>null</code> is
   * reported, the value's value string is displayed (<code>IValue.getValueString()</code>).
   * 
   * @param value the value for which a detailed description is required
   * @param listener the listener to report the details to asynchronously
   */
  @Override
  public void computeDetail(final IValue value, final IValueDetailListener listener) {
    if (value instanceof DartiumDebugValue) {
      DartiumDebugValue debugValue = (DartiumDebugValue) value;

      debugValue.computeDetail(new DartiumDebugValue.ValueCallback() {
        @Override
        public void detailComputed(String stringValue) {
          listener.detailComputed(value, stringValue);
        }
      });
    } else {
      listener.detailComputed(value, null);
    }
  }

  @Override
  public void dispose() {

  }

  @Override
  public String getEditorId(IEditorInput input, Object element) {
    if (element instanceof IFile || element instanceof ILineBreakpoint
        || element instanceof LocalFileStorage) {
      return DART_EDITOR_ID;
    }

    return null;
  }

  @Override
  public IEditorInput getEditorInput(Object element) {
    if (element instanceof IFile) {
      return new FileEditorInput((IFile) element);
    }

    if (element instanceof ILineBreakpoint) {
      return new FileEditorInput((IFile) ((ILineBreakpoint) element).getMarker().getResource());
    }

    if (element instanceof LocalFileStorage) {
      try {
        URI fileUri = ((LocalFileStorage) element).getFile().toURI();

        return new DebuggerEditorInput(EFS.getStore(fileUri));
      } catch (CoreException e) {
        DartUtil.logError(e);
      }
    }

    return null;
  }

  public String getFormattedValueText(DartiumDebugValue value) throws DebugException {
    String valueString = "<unknown value>";

    if (value != null) {
      valueString = getValueText(value);

      if (valueString == null) {
        valueString = "<unknown value>";
      }
    }

    return valueString;
  }

  /**
   * This method allows us to customize images for Dart objects that are displayed in the debugger.
   */
  @Override
  public Image getImage(Object element) {
    if (element instanceof DartiumDebugVariable) {
      DartiumDebugVariable variable = (DartiumDebugVariable) element;

      if (variable.isThisObject()) {
        return DartDebugUIPlugin.getImage("obj16/this_obj.gif");
      } else if (variable.isPrimitiveValue()) {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      } else if (variable.isListValue()) {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
        //return DartDebugUIPlugin.getImage("obj16/list_obj.png");
      } else {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      }
    } else if (element instanceof ServerDebugVariable) {
      ServerDebugVariable variable = (ServerDebugVariable) element;

      if (variable.isThisObject()) {
        return DartDebugUIPlugin.getImage("obj16/this_obj.gif");
      } else if (variable.isListValue()) {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      } else {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      }
    } else {
      return null;
    }
  }

  @Override
  public String getText(Object element) {
    return null;
  }

  /**
   * Build the text for an {@link DartiumDebugValue}. This can be a long running call since we wait
   * for the toString call to get back with the value.
   */
  public String getValueText(DartiumDebugValue value) throws DebugException {
    boolean isPrimitive = value.isPrimitive();
    boolean isArray = value.isList();

    final String valueString[] = new String[1];

    if (!isPrimitive) {

      final CountDownLatch latch = new CountDownLatch(1);
      this.getClass();

      computeDetail(value, new IValueDetailListener() {
        @Override
        public void detailComputed(IValue value, String result) {
          valueString[0] = result;
          latch.countDown();
        }
      });
      try {
        latch.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        return null;
      }
      if (isArray) {
        valueString[0] = "[" + valueString[0] + "]";
      }
      return valueString[0];

    } else {
      return value.getDisplayString();
    }

  }

  public String getVariableText(DartiumDebugVariable var) {
    String varLabel = "<unknown name>";
    varLabel = var.getName();

    DartiumDebugValue value = null;

    try {
      value = (DartiumDebugValue) var.getValue();
    } catch (DebugException e1) {

    }

    StringBuffer buff = new StringBuffer();
    buff.append(varLabel);

    String valueString = null;
    try {
      valueString = getFormattedValueText(value);
    } catch (DebugException e) {
      return null;
    }

    if (valueString.length() != 0) {
      buff.append(" = "); //$NON-NLS-1$
      buff.append(valueString);
    }

    return buff.toString();
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void setAttribute(String attribute, Object value) {

  }

}
