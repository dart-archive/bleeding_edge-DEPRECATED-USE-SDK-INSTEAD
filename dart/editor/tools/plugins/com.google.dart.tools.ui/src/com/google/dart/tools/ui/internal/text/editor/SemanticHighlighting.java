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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

/**
 * Base class for semantic highlightings.
 */
public abstract class SemanticHighlighting {

  /**
   * Returns the RGB for the given key in the given color registry.
   * 
   * @param registry the color registry
   * @param key the key for the constant in the registry
   * @param defaultRGB the default RGB if no entry is found
   * @return RGB the RGB
   */
  private static RGB findRGB(ColorRegistry registry, String key, RGB defaultRGB) {
    RGB rgb = registry.getRGB(key);
    if (rgb != null) {
      return rgb;
    }
    return defaultRGB;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the export clause in the given token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesExportClause(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the hide clause in the given token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesHideClause(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the semantic highlighting consumes the semantic token.
   * <p>
   * NOTE: Implementors are not allowed to keep a reference on the token or on any object retrieved
   * from the token.
   * </p>
   */
  public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the given import directive token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesImportDirective(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the semantic highlighting consumes the semantic token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesLibraryDirective(SemanticToken<DartDirective> directiveToken) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the semantic highlighting consumes the semantic token.
   * <p>
   * NOTE: Implementors are not allowed to keep a reference on the token or on any object retrieved
   * from the token.
   * </p>
   * 
   * @param token the semantic token
   * @return <code>true</code> iff the semantic highlighting consumes the semantic token
   */
  public boolean consumesLiteral(SemanticToken<DartLiteral> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the hide clause in the given token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesOfClause(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the given part directive token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesPartDirective(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the given part directive token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */
  public boolean consumesPartOfDirective(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * Returns <code>true</code> iff the highlighting consumes the show clause in the given token.
   * 
   * @see SemanticHighlighting#consumesIdentifier(SemanticToken)
   */

  public boolean consumesShowClause(SemanticToken<DartDirective> token) {
    return false;
  }

  /**
   * @return the default default text color
   */
  public abstract RGB getDefaultDefaultTextColor();

  /**
   * @return default style information
   */
  public int getDefaultStyle() {
    int style = isBoldByDefault() ? SWT.BOLD : SWT.NORMAL;
    if (isItalicByDefault()) {
      style |= SWT.ITALIC;
    }
    if (isStrikethroughByDefault()) {
      style |= TextAttribute.STRIKETHROUGH;
    }
    if (isUnderlineByDefault()) {
      style |= TextAttribute.UNDERLINE;
    }
    return style;
  }

  /**
   * @return the default default text color
   */
  public RGB getDefaultTextColor() {
    return findRGB(
        PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry(),
        getThemeColorKey(),
        getDefaultDefaultTextColor());
  }

  /**
   * @return the display name
   */
  public abstract String getDisplayName();

  /**
   * @return the preference key, will be augmented by a prefix and a suffix for each preference
   */
  public abstract String getPreferenceKey();

  /**
   * Calculate the source length for the given node.
   */
  public int getSourceLength(DartNode node) {
    return node.getSourceInfo().getLength();
  }

  /**
   * Calculate the source offset for the given node.
   */
  public int getSourceOffset(DartNode node) {
    return node.getSourceInfo().getOffset();
  }

  /**
   * @return <code>true</code> if the text attribute bold is set by default
   */
  public abstract boolean isBoldByDefault();

  /**
   * @return <code>true</code> if the text attribute italic is enabled by default
   */
  public abstract boolean isEnabledByDefault();

  /**
   * @return <code>true</code> if the text attribute italic is set by default
   */
  public abstract boolean isItalicByDefault();

  /**
   * @return <code>true</code> if the text attribute strikethrough is set by default
   */
  public boolean isStrikethroughByDefault() {
    return false;
  }

  /**
   * @return <code>true</code> if the text attribute underline is set by default
   */
  public boolean isUnderlineByDefault() {
    return false;
  }

  private String getThemeColorKey() {
    return DartUI.ID_PLUGIN + "." + getPreferenceKey() + "Highlighting"; //$NON-NLS-1$//$NON-NLS-2$
  }

}
