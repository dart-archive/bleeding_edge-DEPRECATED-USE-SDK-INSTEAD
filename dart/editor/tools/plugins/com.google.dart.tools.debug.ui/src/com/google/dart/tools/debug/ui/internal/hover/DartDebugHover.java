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
package com.google.dart.tools.debug.ui.internal.hover;

import com.google.dart.tools.debug.core.util.IVariableResolver;
import com.google.dart.tools.debug.ui.internal.presentation.DartDebugModelPresentation;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Hover text for variables while debugging.
 */
public class DartDebugHover implements ITextHover, ITextHoverExtension2 {
  private static DartDebugModelPresentation presentation = new DartDebugModelPresentation();

  /**
   * Returns a configured model presentation for use displaying variables.
   */
  private static DartDebugModelPresentation getModelPresentation() {
    return presentation;
  }

  /**
   * Returns HTML text for the given variable
   */
  private static String getVariableText(IVariable variable) {
    StringBuffer buffer = new StringBuffer();
    DartDebugModelPresentation modelPresentation = getModelPresentation();
    buffer.append("<p><pre>"); //$NON-NLS-1$
    String variableText = modelPresentation.getVariableText(variable);
    buffer.append(variableText);
    buffer.append("</pre></p>"); //$NON-NLS-1$
    modelPresentation.dispose();

    if (buffer.length() > 0) {
      return buffer.toString();
    }

    return null;
  }

  public DartDebugHover() {

  }

  @Override
  public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
    Object object = getHoverInfo2(textViewer, hoverRegion);

    if (object instanceof IVariable) {
      IVariable var = (IVariable) object;

      return getVariableText(var);
    }

    return null;
  }

  @Override
  public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
    IStackFrame frame = getFrame();

    if (frame != null && frame instanceof IVariableResolver) {
      IVariableResolver resolver = (IVariableResolver) frame;

      IDocument document = textViewer.getDocument();

      if (document != null) {
        try {
          String variableName = document.get(hoverRegion.getOffset(), hoverRegion.getLength());

          try {
            IVariable variable = resolver.findVariable(variableName);

            if (variable != null) {
              return variable;
            }
          } catch (DebugException e) {
            return null;
          }
        } catch (BadLocationException e) {
          return null;
        }
      }
    }

    return null;
  }

  @Override
  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    return new Region(offset, 0);
  }

  private IStackFrame getFrame() {
    IAdaptable adaptable = DebugUITools.getDebugContext();

    if (adaptable != null) {
      return (IStackFrame) adaptable.getAdapter(IStackFrame.class);
    }

    return null;
  }

}
