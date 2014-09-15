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

package com.google.dart.engine.services.util;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.SimpleElementVisitor;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * A utility class for dealing with Dart doc text.
 * 
 * @coverage dart.tools.core.utilities
 */
public final class DartDocUtilities {

  private static class DocumentingVisitor extends SimpleElementVisitor<String> {
    private Element targetElement;
    private Type targetType;

    public DocumentingVisitor(Element targetElement, Type targetType) {
      this.targetElement = targetElement;
      this.targetType = targetType;
    }

    @Override
    public String visitClassElement(ClassElement element) {

      LibraryElement library = element.getLibrary();

      if (library != null) {
        String libraryName = library.getDisplayName();
        if (libraryName != null && libraryName.length() > 0) {
          return getTypeName(element) + " - " + libraryName;
        }
      }

      return getTypeName(element);
    }

    @Override
    public String visitConstructorElement(ConstructorElement element) {
      StringBuilder params = describeParams(element.getParameters());
      String typeName = element.getType().getReturnType().getDisplayName();
      String constructorName = element.getDisplayName();
      if (constructorName != null && constructorName.length() != 0) {
        typeName = typeName + "." + constructorName;
      }
      return typeName + "(" + params + ")";
    }

    @Override
    public String visitFieldElement(FieldElement element) {
      return getTypeName(element) + " " + element.getDisplayName();
    }

    @Override
    public String visitFieldFormalParameterElement(FieldFormalParameterElement element) {
      return getTypeName(element) + " " + element.getDisplayName();
    }

    @Override
    public String visitFunctionElement(FunctionElement element) {
      return getDescription(element);
    }

    @Override
    public String visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
      return getDescription(element);
    }

    @Override
    public String visitLocalVariableElement(LocalVariableElement element) {
      return getTypeName(element) + " " + element.getDisplayName();
    }

    @Override
    public String visitMethodElement(MethodElement element) {
      return getDescription(element);
    }

    @Override
    public String visitParameterElement(ParameterElement element) {
      return getDescription(element);
    }

    @Override
    public String visitPropertyAccessorElement(PropertyAccessorElement element) {

      if (element.isGetter()) {

        String returnTypeName = getName(element.getType().getReturnType());

        StringBuilder sb = new StringBuilder();
        if (returnTypeName != null) {
          sb.append(returnTypeName).append(' ');
        }
        sb.append("get ").append(element.getDisplayName());
        return sb.toString();
      }

      if (element.isSetter()) {
        return element.getDisplayName() + "(" + describeParams(element.getParameters()) + ")";
      }

      return getDescription(element);
    }

    @Override
    public String visitTopLevelVariableElement(TopLevelVariableElement element) {
      return getTypeName(element) + " " + element.getDisplayName();
    }

    @Override
    public String visitTypeParameterElement(TypeParameterElement element) {
      StringBuilder sb = new StringBuilder();

      sb.append("<");
      sb.append(element.getType().getDisplayName());
      com.google.dart.engine.type.Type bound = element.getBound();
      if (bound != null) {
        sb.append(" extends ").append(bound.getDisplayName());
      }
      sb.append(">");

      return sb.toString();
    }

    private StringBuilder describeParams(ParameterElement[] parameters) {
      StringBuilder buf = new StringBuilder();

      // Closing ']' or '}' in case of optional params
      String endGroup = "";

      for (int i = 0; i < parameters.length; i++) {
        if (i > 0) {
          buf.append(", ");
        }

        ParameterElement param = parameters[i];

        ParameterKind kind = param.getParameterKind();

        // Start group of optional params
        if (endGroup.length() == 0) {
          if (kind == ParameterKind.NAMED) {
            buf.append("{");
            endGroup = "}";
          } else if (kind == ParameterKind.POSITIONAL) {
            buf.append("[");
            endGroup = "]";
          }
        }

        buf.append(getDescription(param));

      }

      // Close optional list
      buf.append(endGroup);

      return buf;
    }

    private String getDescription(ExecutableElement element) {

      StringBuilder params = describeParams(element.getParameters());
      String returnTypeName = getName(element.getType().getReturnType());

      if (returnTypeName != null) {
        return returnTypeName + " " + element.getDisplayName() + "(" + params + ")";
      } else {
        return element.getDisplayName() + "(" + params + ")";
      }
    }

    private String getDescription(FunctionTypeAliasElement element) {

      StringBuilder params = describeParams(element.getParameters());
      String returnTypeName = getName(element.getType().getReturnType());

      if (returnTypeName != null) {
        return returnTypeName + " " + element.getDisplayName() + "(" + params + ")";
      } else {
        return element.getDisplayName() + "(" + params + ")";
      }
    }

    private String getDescription(ParameterElement param) {

      StringBuilder buf = new StringBuilder();

      String typeName = getTypeName(param);
      String paramName = param.getDisplayName();

      if (typeName.indexOf('(') != -1) {

        // Instead of returning "void(var) callback", return "void callback(var)".
        int index = typeName.indexOf('(');

        buf.append(typeName.substring(0, index));
        buf.append(" ");
        buf.append(paramName);
        buf.append(typeName.substring(index));
      } else {
        buf.append(typeName + " " + paramName);
      }

      String defaultValueCode = param.getDefaultValueCode();
      if (defaultValueCode != null) {
        buf.append(": ");
        buf.append(defaultValueCode);
      }

      return buf.toString();
    }

    private String getName(com.google.dart.engine.type.Type type) {
      if (type != null) {
        String name = type.getDisplayName();
        if (name != null) {
          return name;
        }
      }
      return "dynamic";
    }

    private String getTypeName(ClassElement element) {
      return getName(element.getType());
    }

    private String getTypeName(VariableElement element) {
      Type type = element.getType();
      if (targetType != null && targetElement != null && targetElement.equals(element)) {
        type = targetType;
      }
      return getName(type);
    }

  }

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
        } else if (line.startsWith("///")) {
          line = line.substring(3);

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
   * Return the prettified DartDoc text for the given element.
   */
  public static String getDartDocAsHtml(Element element) {
    if (element == null) {
      return null;
    }
    try {
      String docString = element.computeDocumentationComment();
      return getDartDocAsHtml(docString);
    } catch (AnalysisException e) {
      AnalysisEngine.getInstance().getLogger().logError("Exception in gettting documentation", e);
    }
    return null;
  }

  /**
   * Return the prettified DartDoc text for the given element.
   */
  public static String getDartDocAsHtml(String docString) {
    if (docString != null) {
      return convertToHtml(cleanDartDoc(docString));
    }
    return null;
  }

  /**
   * Return a one-line description of the given Element.
   * 
   * @param type the best known {@link Type} of the given {@link Element}
   * @param element the {@link Element} to document
   * @return a String summarizing this element, or {@code null} if there is no suitable
   *         documentation
   */
  public static String getTextSummary(Type type, Element element) {
    if (element != null) {
      String description = element.accept(new DocumentingVisitor(element, type));
      if (description != null) {
        return description;
      }
    }
    return null;
  }

  /**
   * Return a one-line description of the given Element as html
   */
  public static String getTextSummaryAsHtml(Type type, Element element) {
    String summary = getTextSummary(type, element);
    if (summary == null) {
      return null;
    }
    return escapeHtmlEntities(summary);
  }

  private static String convertListItems(String[] lines) {
    StringBuffer buf = new StringBuffer();

    boolean wasCode = false;
    for (String line : lines) {
      // code block
      if (line.startsWith("    ")) {
        buf.append("<pre>");
        if (wasCode) {
          buf.append("\n");
        }
        buf.append(line);
        buf.append("</pre>");
        wasCode = true;
        continue;
      }
      wasCode = false;
      // empty line
      if (line.isEmpty()) {
        buf.append("\n\n");
        continue;
      }
      // unordered list item
      if (line.startsWith("* ")) {
        buf.append("<li>" + line.substring(2) + "</li>\n");
        continue;
      }
      // ordered list
      {
        line = line.trim();
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) {
          i++;
        }
        if (i < line.length() && line.charAt(i) == '.') {
          buf.append("<pre>    </pre>" + line + "<br>");
          continue;
        }
      }
      // text line
      buf.append(line);
      buf.append("\n");
    }
    return buf.toString();
  }

  private static String convertToHtml(String str) {
    if (str == null) {
      return null;
    }

    // escape html entities
    str = escapeHtmlEntities(str);

    // convert lines starting with "  " to list items
    str = convertListItems(str.split("\n"));

    // insert line breaks where appropriate
    str = str.replace("\n\n", "\n<br><br>\n");

    // handle too much whitespace in front of list items
    str = str.replace("<br>\n<li>", "<li>");

    // process non-code lines
    {
      String[] lines = StringUtils.splitPreserveAllTokens(str, "\n");
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        if (line.startsWith("    ") || line.startsWith("<pre>    ")) {
          // ignore code lines
        } else {
          // convert code and reference blocks
          line = line.replace("[:", "<code>").replace(":]", "</code>");
          line = line.replace("[", "<code>").replace("]", "</code>");
          line = replacePairs(line, "`", "<code>", "</code>");

          // convert bold and italic
          line = replacePairs(line, "**", "<b>", "</b>");
          line = replacePairs(line, "__", "<b>", "</b>");
          line = replacePairs(line, "*", "<i>", "</i>");
          line = replacePairs(line, "_", "<i>", "</i>");
        }
        sb.append(line);
        sb.append("\n");
      }
      str = sb.toString();
      if (str.length() > 0) {
        str = str.substring(0, str.length() - 1);
      }
    }

    return str;
  }

  private static String escapeHtmlEntities(String str) {
    return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String replacePairs(String string, String delimiter, String leftReplacement,
      String rightReplacement) {
    int index = string.indexOf(delimiter);
    if (index < 0) {
      return string;
    }
    StringBuilder builder = new StringBuilder();
    boolean left = true;
    int start = 0;
    int length = delimiter.length();
    while (index >= 0) {
      builder.append(string, start, index);
      builder.append(left ? leftReplacement : rightReplacement);
      start = index + length;
      left = !left;
      index = string.indexOf(delimiter, start);
    }
    builder.append(string, start, string.length());
    return builder.toString();
  }

  private DartDocUtilities() {

  }

}
