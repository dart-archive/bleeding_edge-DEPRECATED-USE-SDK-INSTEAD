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

package com.google.dart.tools.core.utilities.dartdoc;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartDocumentable;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.ast.DartElementLocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * A utility class for dealing with Dart doc text.
 */
public final class DartDocUtilities {

  /**
   * Convert from a Dart doc string with slashes and stars to a plain text representation of the
   * comment.
   * 
   * @param str
   * @return
   */
  public static String cleanDartDoc(String str) {
    // Remove /** */
    if (str.startsWith("/**")) {
      str = str.substring(3);
    }

    if (str.endsWith("*/")) {
      str = str.substring(0, str.length() - 2);
    }

    str = str.trim();

    // Remove leading '* ', and turn empty lines into \n's.
    StringBuilder builder = new StringBuilder();

    BufferedReader reader = new BufferedReader(new StringReader(str));

    try {
      String line = reader.readLine();
      int lineCount = 0;

      while (line != null) {
        line = line.trim();

        if (line.startsWith("*")) {
          line = line.substring(1);

          if (line.startsWith(" ")) {
            line = line.substring(1);
          }
        }

        if (line.length() == 0) {
          lineCount = 0;
          builder.append("\n\n");
        } else {
          if (lineCount > 0) {
            builder.append("\n");
          }

          builder.append(line);
          lineCount++;
        }

        line = reader.readLine();
      }
    } catch (IOException exception) {
      // this will never be thrown
    }

    return builder.toString();
  }

  /**
   * Return the prettified DartDoc text for the given DartDocumentable element.
   * 
   * @param documentable
   * @return
   * @throws DartModelException
   */
  public static String getDartDoc(DartDocumentable documentable) throws DartModelException {
    SourceRange sourceRange = documentable.getDartDocRange();

    if (sourceRange != null) {
      SourceRange range = sourceRange;

      String dartDoc = documentable.getOpenable().getBuffer().getText(
          range.getOffset(),
          range.getLength());

      return cleanDartDoc(dartDoc);
    }

    // Check if there is sidecar documentation for the element.
    return ExternalDartDocManager.getManager().getExtDartDoc(documentable);
  }

  /**
   * Return the prettified DartDoc text for the given DartDocumentable element.
   * 
   * @param documentable
   * @return
   * @throws DartModelException
   */
  public static String getDartDocAsHtml(DartDocumentable documentable) throws DartModelException {
    // Check if the element is dartdoc'd.
    SourceRange sourceRange = documentable.getDartDocRange();

    if (sourceRange != null) {
      SourceRange range = sourceRange;

      String dartDoc = documentable.getOpenable().getBuffer().getText(
          range.getOffset(),
          range.getLength());

      return convertToHtml(cleanDartDoc(dartDoc));
    }

    // Check if there is sidecar documentation for the element.
    String dartDoc = ExternalDartDocManager.getManager().getExtDartDoc(documentable);

    if (dartDoc != null) {
      return convertToHtml(dartDoc);
    }

    return null;
  }

  /**
   * Return the DartDocumentable element in the given location, or <code>null</code> if there is no
   * element at that location.
   * 
   * @param compilationUnit the compilation unit containing the location
   * @param unit the AST corresponding to the given compilation unit
   * @param start the index of the start of the range identifying the location
   * @param end the index of the end of the range identifying the location
   * @return the DartDocumentable element in the given location
   * @throws DartModelException if the source containing the DartDoc cannot be accessed
   */
  public static DartDocumentable getDartDocumentable(CompilationUnit compilationUnit,
      DartUnit unit, int start, int end) throws DartModelException {
    if (compilationUnit == null || unit == null) {
      return null;
    }

    DartElementLocator locator = new DartElementLocator(compilationUnit, start, end);

    try {
      DartElement element = locator.searchWithin(unit);

      if (element instanceof DartDocumentable) {
        DartDocumentable documentable = (DartDocumentable) element;

        return documentable;
      }
    } catch (Exception exception) {
      DartCore.logInformation(
          "Could not get DartDoc for element in " + compilationUnit.getElementName() + ", start = "
              + start + ", end = " + end,
          exception);
    }

    return null;
  }

  /**
   * Return a one-line description of the given documentable DartElement.
   * 
   * @param documentable
   * @return
   */
  public static String getTextSummary(DartDocumentable documentable) {
    // TODO(devoncarew): this method should be re-written as a visitor to remove all the instanceof checks.

    try {
      if (documentable instanceof Field) {
        Field field = (Field) documentable;

        // TODO(devoncarew): why/when is this null?
        if (field.getTypeName() != null) {
          return field.getTypeName() + " " + field.getElementName();
        } else {
          return field.getElementName();
        }
      } else if (documentable instanceof DartVariableDeclaration) {
        DartVariableDeclaration decl = (DartVariableDeclaration) documentable;

        return decl.getTypeName() + " " + decl.getElementName();
      } else if (documentable instanceof Type) {
        Type type = (Type) documentable;

        if (type.getLibrary() != null) {
          return type.getElementName() + " - " + type.getLibrary().getDisplayName();
        } else {
          return type.getElementName();
        }
      } else if (documentable instanceof DartFunction) {
        DartFunction method = (DartFunction) documentable;

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < method.getParameterNames().length; i++) {
          if (i > 0) {
            buf.append(", ");
          }

          buf.append(method.getParameterTypeNames()[i] + " " + method.getParameterNames()[i]);
        }

        if (method.getReturnTypeName() != null) {
          return method.getReturnTypeName() + " " + method.getElementName() + "(" + buf + ")";
        } else {
          return method.getElementName() + "(" + buf + ")";
        }
      } else {
        return documentable.getElementName();
      }
    } catch (DartModelException exception) {
      return documentable.getElementName();
    }
  }

  /**
   * @return a one-line description of the given documentable DartElement
   */
  public static String getTextSummaryAsHtml(DartDocumentable documentable) {
    return convertToHtml(getTextSummary(documentable));
  }

  private static String convertListItems(String[] lines) {
    StringBuffer buf = new StringBuffer();

    for (String line : lines) {
      if (line.startsWith("  ")) {
        buf.append("<li>" + line + "</li>");
      } else {
        buf.append(line);
      }

      buf.append("\n");
    }

    return buf.toString();
  }

  private static String convertToHtml(String str) {
    if (str == null) {
      return null;
    }

    // escape html entities
    str = str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

    // convert lines starting with "  " to list items
    str = convertListItems(str.split("\n"));

    // insert line breaks where appropriate
    str = str.replace("\n\n", "\n<br><br>\n");

    // handle too much whitespace in front of list items
    str = str.replace("<br>\n<li>", "<li>");

    return str;
  }

  private DartDocUtilities() {

  }

}
