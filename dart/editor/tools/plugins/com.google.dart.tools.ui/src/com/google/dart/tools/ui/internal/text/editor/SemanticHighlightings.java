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

import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.LibraryPrefixElement;
import com.google.dart.compiler.resolver.NodeElement;
import com.google.dart.tools.core.utilities.ast.DynamicTypesFinder;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.IDartColorConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Semantic highlightings.
 */
public class SemanticHighlightings {

  /**
   * Abstract clause highlighting.
   */
  private static class AbstractClauseHighlighting extends AbstractDirectiveHighlighting {

    private int clauseOffset;

    public AbstractClauseHighlighting(String token) {
      super(token);
    }

    @Override
    public boolean consumesOfClause(SemanticToken<DartDirective> token) {
      return testForClause(token);
    }

    @Override
    public int getSourceOffset(DartNode node) {
      return node.getSourceInfo().getOffset() + clauseOffset;
    }

    protected boolean testForClause(SemanticToken<DartDirective> token) {
      clauseOffset = token.getSource().indexOf(tokenString);
      return clauseOffset != -1;
    }

  }

  /**
   * Base class for directive semantic highlightings.
   */
  private static abstract class AbstractDirectiveHighlighting extends DefaultSemanticHighlighting {

    private static final RGB KEY_WORD_COLOR = PreferenceConverter.getColor(
        DartToolsPlugin.getDefault().getPreferenceStore(),
        IDartColorConstants.JAVA_KEYWORD);

    protected final String tokenString;

    protected AbstractDirectiveHighlighting(String token) {
      this.tokenString = token;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return KEY_WORD_COLOR;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_directive;
    }

    @Override
    public String getPreferenceKey() {
      return DIRECTIVE;
    }

    @Override
    public int getSourceLength(DartNode node) {
      return tokenString.length();
    }

    @Override
    public int getSourceOffset(DartNode node) {
      return node.getSourceInfo().getOffset();
    }

    @Override
    public boolean isBoldByDefault() {
      return true;
    }
  }

  /**
   * Abstract {@link SemanticHighlighting} with empty methods by default.
   */
  private static abstract class DefaultSemanticHighlighting extends SemanticHighlighting {

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0);
    }

    @Override
    public boolean isBoldByDefault() {
      return false;
    }

    @Override
    public boolean isEnabledByDefault() {
      return false;
    }

    @Override
    public boolean isItalicByDefault() {
      return false;
    }

    @Override
    public boolean isStrikethroughByDefault() {
      return false;
    }

    @Override
    public boolean isUnderlineByDefault() {
      return false;
    }
  }

  /**
   * Semantic highlighting deprecated elements.
   */
  private static final class DeprecatedElementHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {
      DartIdentifier node = token.getNode();
      NodeElement element = node.getElement();
      return element != null && element.getMetadata().isDeprecated();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_deprecatedElement;
    }

    @Override
    public String getPreferenceKey() {
      return DEPRECATED_ELEMENT;
    }

    @Override
    public boolean isStrikethroughByDefault() {
      return true;
    }
  }

  /**
   * Semantic highlighting for variables with dynamic types.
   */
  private static final class DynamicTypeHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {
      DartIdentifier node = token.getNode();
      return DynamicTypesFinder.isDynamic(node);
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
//      return new RGB(237, 145, 33); //carrot
//      return new RGB(184, 115, 51); //copper
//      return new RGB(0xd7, 0x96, 0x7d); //taupe
      return new RGB(0x67, 0x4C, 0x47); //dark taupe
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_dynamicType;
    }

    @Override
    public String getPreferenceKey() {
      return DYNAMIC_TYPE;
    }
  }

  /**
   * Export clause highlighting.
   */
  private static class ExportClauseHighlighting extends AbstractClauseHighlighting {

    public ExportClauseHighlighting() {
      super("export");
    }

    @Override
    public boolean consumesExportClause(SemanticToken<DartDirective> token) {
      return testForClause(token);
    }

  }

  /**
   * Semantic highlighting for fields.
   */
  private static class FieldHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {
      DartIdentifier node = token.getNode();
      NodeElement element = node.getElement();
      if (element == null || element.isDynamic()) {
        return false;
      }
      if (element instanceof FieldElement) {
        FieldElement field = (FieldElement) element;
        //skip getters/setters
        Modifiers modifiers = field.getModifiers();
        return !modifiers.isGetter() && !modifiers.isSetter();
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 192);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_field;
    }

    @Override
    public String getPreferenceKey() {
      return FIELD;
    }
  }

  /**
   * Hide clause highlighting.
   */
  private static class HideClauseHighlighting extends AbstractClauseHighlighting {

    public HideClauseHighlighting() {
      super("hide");
    }

    @Override
    public boolean consumesHideClause(SemanticToken<DartDirective> token) {
      return testForClause(token);
    }

  }

  /**
   * Import directive highlighting.
   */
  private static class ImportDirectiveHighlighting extends AbstractDirectiveHighlighting {

    public ImportDirectiveHighlighting() {
      super("import");
    }

    @Override
    public boolean consumesImportDirective(SemanticToken<DartDirective> directiveToken) {
      return true;
    }

  }

  /**
   * Library directive highlighting.
   */
  private static class LibraryDirectiveHighlighting extends AbstractDirectiveHighlighting {

    public LibraryDirectiveHighlighting() {
      super("library");
    }

    @Override
    public boolean consumesLibraryDirective(SemanticToken<DartDirective> directiveToken) {
      return true;
    }

  }

  /**
   * Of clause highlighting.
   */
  private static class OfClauseHighlighting extends AbstractClauseHighlighting {

    public OfClauseHighlighting() {
      super("of");
    }

    @Override
    public boolean consumesOfClause(SemanticToken<DartDirective> token) {
      return testForClause(token);
    }

  }

  /**
   * Part directive highlighting.
   */
  private static class PartDirectiveHighlighting extends AbstractDirectiveHighlighting {

    public PartDirectiveHighlighting() {
      super("part");
    }

    @Override
    public boolean consumesPartDirective(SemanticToken<DartDirective> directiveToken) {
      return true;
    }

  }

  /**
   * Part of directive highlighting.
   */
  private static class PartOfDirectiveHighlighting extends AbstractDirectiveHighlighting {

    public PartOfDirectiveHighlighting() {
      super("part");
    }

    @Override
    public boolean consumesPartOfDirective(SemanticToken<DartDirective> token) {
      return true;
    }

  }

  /**
   * Show clause highlighting.
   */
  private static class ShowClauseHighlighting extends AbstractClauseHighlighting {

    public ShowClauseHighlighting() {
      super("show");
    }

    @Override
    public boolean consumesShowClause(SemanticToken<DartDirective> token) {
      return testForClause(token);
    }

  }

  /**
   * Semantic highlighting for static fields.
   */
  private static class StaticFieldHighlighting extends FieldHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {
      DartIdentifier node = token.getNode();
      NodeElement element = node.getElement();
      if (element == null || element.isDynamic()) {
        return false;
      }
      if (element instanceof FieldElement) {
        return ((FieldElement) element).isStatic();
      }
      return false;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_staticField;
    }

    @Override
    public String getPreferenceKey() {
      return STATIC_FIELD;
    }

    @Override
    public boolean isItalicByDefault() {
      return true;
    }

  }

  /**
   * Semantic highlighting for top level members.
   */
  private static class TopLevelMemberHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken<DartIdentifier> token) {

      DartIdentifier node = token.getNode();
      NodeElement element = node.getElement();

      if (element == null || element instanceof ClassElement
          || element instanceof LibraryPrefixElement) {
        return false;
      }

      DartNode parent = node.getParent();
      if (parent instanceof DartDeclaration<?>) {
        if (((DartDeclaration<?>) parent).getName().equals(node)) {
          return false;
        }
      }

      return element.getEnclosingElement() instanceof LibraryElement;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0x40, 0x40, 0x40);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_topLevelMember;
    }

    @Override
    public String getPreferenceKey() {
      return TOP_LEVEL_MEMBER;
    }

    @Override
    public boolean isBoldByDefault() {
      return true;
    }

    @Override
    public boolean isItalicByDefault() {
      return false;
    }

  }

  /**
   * A named preference part that controls the highlighting of deprecated elements.
   */
  public static final String DEPRECATED_ELEMENT = "deprecated"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static final fields.
   */
  public static final String STATIC_FINAL_FIELD = "staticFinalField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static fields.
   */
  public static final String STATIC_FIELD = "staticField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of top level members.
   */
  public static final String TOP_LEVEL_MEMBER = "topLevelMember"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of directives.
   */
  public static final String DIRECTIVE = "directive"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of fields.
   */
  public static final String FIELD = "field"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of method declarations.
   */
  public static final String METHOD_DECLARATION = "methodDeclarationName"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static method invocations.
   */
  public static final String STATIC_METHOD_INVOCATION = "staticMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of inherited method invocations.
   */
  public static final String INHERITED_METHOD_INVOCATION = "inheritedMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of abstract method invocations.
   */
  public static final String ABSTRACT_METHOD_INVOCATION = "abstractMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE_DECLARATION = "localVariableDeclaration"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE = "localVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of parameter variables.
   */
  public static final String PARAMETER_VARIABLE = "parameterVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of dynamic types.
   */
  public static final String DYNAMIC_TYPE = "dynamicType"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   */
  public static final String TYPE_VARIABLE = "typeParameter"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   * 
   * @author STP
   */
  public static final String OBJECT_INITIALIZER = "objectInitializer"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of methods (invocations and
   * declarations).
   */
  public static final String METHOD = "method"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of classes.
   */
  public static final String CLASS = "class"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of interfaces.
   */
  public static final String INTERFACE = "interface"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of annotations.
   */
  public static final String ANNOTATION = "annotation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type arguments.
   */
  public static final String TYPE_ARGUMENT = "typeArgument"; //$NON-NLS-1$

  /**
   * Semantic highlightings
   */
  private static SemanticHighlighting[] SEMANTIC_HIGHTLIGHTINGS;

  /**
   * Tests whether <code>event</code> in <code>store</code> affects the enablement of semantic
   * highlighting.
   * 
   * @param store the preference store where <code>event</code> was observed
   * @param event the property change under examination
   * @return <code>true</code> if <code>event</code> changed semantic highlighting enablement,
   *         <code>false</code> if it did not
   */
  public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event) {
    String relevantKey = null;
    SemanticHighlighting[] highlightings = getSemanticHighlightings();
    for (int i = 0; i < highlightings.length; i++) {
      if (event.getProperty().equals(getEnabledPreferenceKey(highlightings[i]))) {
        relevantKey = event.getProperty();
        break;
      }
    }
    if (relevantKey == null) {
      return false;
    }

    for (int i = 0; i < highlightings.length; i++) {
      String key = getEnabledPreferenceKey(highlightings[i]);
      if (key.equals(relevantKey)) {
        continue;
      }
      if (store.getBoolean(key)) {
        return false; // another is still enabled or was enabled before
      }
    }

    // all others are disabled, so toggling relevantKey affects the enablement
    return true;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * bold.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the bold preference key
   */
  public static String getBoldPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
  }

  /**
   * A named preference that controls the given semantic highlighting's color.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the color preference key
   */
  public static String getColorPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting is enabled.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the enabled preference key
   */
  public static String getEnabledPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * italic.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the italic preference key
   */
  public static String getItalicPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
  }

  /**
   * @return The semantic highlightings, the order defines the precedence of matches, the first
   *         match wins.
   */
  public static SemanticHighlighting[] getSemanticHighlightings() {
    if (SEMANTIC_HIGHTLIGHTINGS == null) {
      SEMANTIC_HIGHTLIGHTINGS = new SemanticHighlighting[] {
          new LibraryDirectiveHighlighting(), new ImportDirectiveHighlighting(),
          new ExportClauseHighlighting(), new HideClauseHighlighting(),
          new ShowClauseHighlighting(), new PartDirectiveHighlighting(),
          new PartOfDirectiveHighlighting(), new OfClauseHighlighting(),
          new DeprecatedElementHighlighting(), new StaticFieldHighlighting(),
          new FieldHighlighting(), new DynamicTypeHighlighting(), new TopLevelMemberHighlighting()};
    }
    return SEMANTIC_HIGHTLIGHTINGS;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * strikethrough.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the strikethrough preference key
   */
  public static String getStrikethroughPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * underline.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the underline preference key
   */
  public static String getUnderlinePreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
  }

  /**
   * Initialize default preferences in the given preference store.
   * 
   * @param store The preference store
   */
  public static void initDefaults(IPreferenceStore store) {
    SemanticHighlighting[] semanticHighlightings = getSemanticHighlightings();
    for (int i = 0, n = semanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = semanticHighlightings[i];
      setDefaultAndFireEvent(
          store,
          SemanticHighlightings.getColorPreferenceKey(semanticHighlighting),
          semanticHighlighting.getDefaultTextColor());
      store.setDefault(
          SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting),
          semanticHighlighting.isBoldByDefault());
      store.setDefault(
          SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting),
          semanticHighlighting.isItalicByDefault());
      store.setDefault(
          SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting),
          semanticHighlighting.isStrikethroughByDefault());
      store.setDefault(
          SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting),
          semanticHighlighting.isUnderlineByDefault());
      store.setDefault(
          SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting),
          semanticHighlighting.isEnabledByDefault());
    }
  }

  /**
   * Tests whether semantic highlighting is currently enabled.
   * 
   * @param store the preference store to consult
   * @return <code>true</code> if semantic highlighting is enabled, <code>false</code> if it is not
   */
  public static boolean isEnabled(IPreferenceStore store) {
    SemanticHighlighting[] highlightings = getSemanticHighlightings();
    boolean enable = false;
    for (int i = 0; i < highlightings.length; i++) {
      String enabledKey = getEnabledPreferenceKey(highlightings[i]);
      if (store.getBoolean(enabledKey)) {
        enable = true;
        break;
      }
    }

    return enable;
  }

  /**
   * Sets the default value and fires a property change event if necessary.
   * 
   * @param store the preference store
   * @param key the preference key
   * @param newValue the new value
   */
  private static void setDefaultAndFireEvent(IPreferenceStore store, String key, RGB newValue) {
    RGB oldValue = null;
    if (store.isDefault(key)) {
      oldValue = PreferenceConverter.getDefaultColor(store, key);
    }

    PreferenceConverter.setDefault(store, key, newValue);

    if (oldValue != null && !oldValue.equals(newValue)) {
      store.firePropertyChangeEvent(key, oldValue, newValue);
    }
  }

  /**
   * Do not instantiate
   */
  private SemanticHighlightings() {
  }

  public RGB getDefaultDefaultTextColor() {
    return new RGB(13, 100, 0);
  }
}
