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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.internal.viewsupport.ColoredString.Style;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.resources.IResource;

public class ColoredDartElementLabels {

  public static final Style QUALIFIER_STYLE = new Style(ColoredViewersManager.QUALIFIER_COLOR_NAME);
  public static final Style COUNTER_STYLE = new Style(ColoredViewersManager.COUNTER_COLOR_NAME);
  public static final Style DECORATIONS_STYLE = new Style(
      ColoredViewersManager.DECORATIONS_COLOR_NAME);

  public final static long COLORIZE = 1L << 55;

  public static ColoredString decorateColoredString(ColoredString string, String decorated,
      Style color) {
    String label = string.getString();
    int originalStart = decorated.indexOf(label);
    if (originalStart == -1) {
      return new ColoredString(decorated); // the decorator did something wild
    }
    if (originalStart > 0) {
      ColoredString newString = new ColoredString(decorated.substring(0, originalStart), color);
      newString.append(string);
      string = newString;
    }
    if (decorated.length() > originalStart + label.length()) { // decorator
                                                               // appended
                                                               // something
      return string.append(decorated.substring(originalStart + label.length()), color);
    }
    return string; // no change
  }

  /**
   * Returns the label of the given object. The object must be of type {@link DartElement} or adapt
   * to {@link org.eclipse.ui.model.IWorkbenchAdapter}. The empty string is returned if the element
   * type is not known.
   * 
   * @param obj Object to get the label from.
   * @param flags The rendering flags
   * @return Returns the label or the empty string if the object type is not supported.
   */
  public static ColoredString getTextLabel(Object obj, long flags) {
    if (obj instanceof IResource) {
      return new ColoredString(((IResource) obj).getName());
//    } else if (obj instanceof JsGlobalScopeContainer) {
//      JsGlobalScopeContainer container = (JsGlobalScopeContainer) obj;
//      return getContainerEntryLabel(container.getClasspathEntry().getPath(),
//          container.getJavaProject());
    }
    return new ColoredString(DartElementLabels.getTextLabel(obj, flags));
  }

  private static final boolean getFlag(long flags, long flag) {
    return (flags & flag) != 0;
  }

  private static void getTypeArgumentSignaturesLabel(String[] typeArgsSig, long flags,
      ColoredString result) {
    if (typeArgsSig.length > 0) {
      result.append('<');
      for (int i = 0; i < typeArgsSig.length; i++) {
        if (i > 0) {
          result.append(DartElementLabels.COMMA_STRING);
        }
        getTypeSignatureLabel(typeArgsSig[i], flags, result);
      }
      result.append('>');
    }
  }

  @SuppressWarnings("unused")
  private static void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags,
      ColoredString result) {
    if (typeParamSigs.length > 0) {
      result.append('<');
      for (int i = 0; i < typeParamSigs.length; i++) {
        if (i > 0) {
          result.append(DartElementLabels.COMMA_STRING);
        }
        result.append(Signature.getTypeVariable(typeParamSigs[i]));
      }
      result.append('>');
    }
  }

  private static void getTypeSignatureLabel(String typeSig, long flags, ColoredString result) {
    int sigKind = Signature.getTypeSignatureKind(typeSig);
    switch (sigKind) {
      case Signature.BASE_TYPE_SIGNATURE:
        result.append(Signature.toString(typeSig));
        break;
      case Signature.ARRAY_TYPE_SIGNATURE:
        getTypeSignatureLabel(Signature.getElementType(typeSig), flags, result);
        for (int dim = Signature.getArrayCount(typeSig); dim > 0; dim--) {
          result.append('[').append(']');
        }
        break;
      case Signature.CLASS_TYPE_SIGNATURE:
        String baseType = Signature.toString(typeSig);
        result.append(Signature.getSimpleName(baseType));

        getTypeArgumentSignaturesLabel(new String[0], flags, result);
        break;
      default:
        // unknown
    }
  }
}
