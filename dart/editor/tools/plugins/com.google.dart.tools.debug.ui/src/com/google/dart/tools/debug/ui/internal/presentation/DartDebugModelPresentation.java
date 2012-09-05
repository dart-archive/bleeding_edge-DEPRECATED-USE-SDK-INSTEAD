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

import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.dartium.DartiumDebugStackFrame;
import com.google.dart.tools.debug.core.dartium.DartiumDebugValue;
import com.google.dart.tools.debug.core.dartium.DartiumDebugVariable;
import com.google.dart.tools.debug.core.server.ServerDebugStackFrame;
import com.google.dart.tools.debug.core.server.ServerDebugVariable;
import com.google.dart.tools.debug.core.util.IExceptionStackFrame;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.DebuggerEditorInput;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IInstructionPointerPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
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
public class DartDebugModelPresentation implements IDebugModelPresentation,
    IInstructionPointerPresentation {

  private static final String DART_EDITOR_ID = "com.google.dart.tools.ui.text.editor.CompilationUnitEditor";

  private static final String BREAK_ON_EXCEPTION_ANNOTAION = "org.eclipse.debug.ui.currentIPEx";

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

  public String getFormattedValueText(IValue value) throws DebugException {
    String valueString = null;

    if (value instanceof DartiumDebugValue) {
      DartiumDebugValue dartiumValue = (DartiumDebugValue) value;

      valueString = getValueText(dartiumValue);
    } else if (value != null) {
      valueString = value.getValueString();

      if (valueString == null) {
        valueString = "<unknown value>";
      }
    }

    if (valueString == null) {
      valueString = "<unknown value>";
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

      if (variable.isThrownException()) {
        return DartDebugUIPlugin.getImage("obj16/object_exception.png");
      } else if (variable.isThisObject()) {
        return DartDebugUIPlugin.getImage("obj16/object_this.png");
      } else if (variable.isLibraryObject()) {
        return DartDebugUIPlugin.getImage("obj16/object_library.png");
      } else if (variable.isStatic()) {
        return DartDebugUIPlugin.getImage("obj16/object_static.png");
      } else {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      }
    } else if (element instanceof ServerDebugVariable) {
      ServerDebugVariable variable = (ServerDebugVariable) element;

      if (variable.isThrownException()) {
        return DartDebugUIPlugin.getImage("obj16/object_exception.png");
      } else if (variable.isThisObject()) {
        return DartDebugUIPlugin.getImage("obj16/object_this.png");
      } else if (variable.isLibraryObject()) {
        return DartDebugUIPlugin.getImage("obj16/object_library.png");
      } else if (variable.isStatic()) {
        return DartDebugUIPlugin.getImage("obj16/object_static.png");
      } else {
        return DartDebugUIPlugin.getImage("obj16/object_obj.png");
      }
    } else if (element instanceof ServerDebugStackFrame) {
      ServerDebugStackFrame frame = (ServerDebugStackFrame) element;

      return DartDebugUIPlugin.getImage(DartElementImageProvider.getMethodImageDescriptor(
          false,
          frame.isPrivate()));
    } else if (element instanceof DartiumDebugStackFrame) {
      DartiumDebugStackFrame frame = (DartiumDebugStackFrame) element;

      return DartDebugUIPlugin.getImage(DartElementImageProvider.getMethodImageDescriptor(
          false,
          frame.isPrivate()));
    } else if (element instanceof DartBreakpoint) {
      return null;
    } else {
      return null;
    }
  }

  @Override
  public Annotation getInstructionPointerAnnotation(IEditorPart editorPart, IStackFrame frame) {
    return null;
  }

  @Override
  public String getInstructionPointerAnnotationType(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        return BREAK_ON_EXCEPTION_ANNOTAION;
      }
    }

    return null;
  }

  @Override
  public Image getInstructionPointerImage(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        return DartDebugUIPlugin.getImage("obj16/inst_ptr_exception.png");
      }
    }

    try {
      IStackFrame topOfStack = frame.getThread().getTopStackFrame();

      if (frame.equals(topOfStack)) {
        return DartDebugUIPlugin.getImage("obj16/inst_ptr_current.png");
      }
    } catch (DebugException de) {

    }

    return DartDebugUIPlugin.getImage("obj16/inst_ptr_normal.png");
  }

  @Override
  public String getInstructionPointerText(IEditorPart editorPart, IStackFrame frame) {
    if (frame instanceof IExceptionStackFrame) {
      IExceptionStackFrame f = (IExceptionStackFrame) frame;

      if (f.hasException()) {
        return f.getExceptionDisplayText();
      }
    }

    return null;
  }

  @Override
  public String getText(Object element) {
    return null;
  }

  public String getVariableText(IVariable var) {
    try {
      StringBuffer buff = new StringBuffer();

      buff.append(var.getName());

      IValue value = var.getValue();

      String valueString = getFormattedValueText(value);

      if (valueString.length() != 0) {
        buff.append(" = ");
        buff.append(valueString);
      }

      return buff.toString();
    } catch (DebugException e) {
      return null;
    }
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

  /**
   * Build the text for an {@link DartiumDebugValue}. This can be a long running call since we wait
   * for the toString call to get back with the value.
   */
  protected String getValueText(DartiumDebugValue value) throws DebugException {
    boolean isPrimitive = value.isPrimitive();
    boolean isArray = value.isList();

    final String valueString[] = new String[1];

    if (!isPrimitive) {
      final CountDownLatch latch = new CountDownLatch(1);

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

}
