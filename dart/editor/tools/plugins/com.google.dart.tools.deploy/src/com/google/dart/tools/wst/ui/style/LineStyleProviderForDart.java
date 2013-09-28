/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.wst.ui.style;

import com.google.common.collect.Maps;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor.EclipsePreferencesAdapter;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.Highlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IColorManagerExtension;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.provisional.style.AbstractLineStyleProvider;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.sse.ui.internal.provisional.style.ReconcilerHighlighter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("restriction")
public class LineStyleProviderForDart extends AbstractLineStyleProvider implements
    LineStyleProvider {

  private static class StyleRangeComparator implements Comparator<StyleRange> {
    @Override
    public int compare(StyleRange arg0, StyleRange arg1) {
      if (arg0 == arg1) {
        return 0;
      }
      if (arg0.start < arg1.start) {
        return -1;
      }
      if (arg0.start > arg1.start) {
        return 1;
      }
      return 0;
    }
  }

  private static final Map<Highlighting, SemanticHighlighting> styleToHighlighting = Maps.newHashMap();
  private static final SemanticHighlighting[] highlighters = SemanticHighlightings.getSemanticHighlightings();
  private static final Highlighting[] styles = new Highlighting[highlighters.length];
  static {
    for (int i = 0; i < highlighters.length; i++) {
      styles[i] = new Highlighting(null, true);
      styleToHighlighting.put(styles[i], highlighters[i]);
    }
  }

  // Parsing follows the model used in junit tests. Feels like a hack. Almost certainly has the wrong
  // analysis context, which will cause problems.
  // TODO(messick) Work with danrubel to get this hooked up correctly.
  private AnalysisContext analysisContext = DartCore.getProjectManager().getSdkContext();
  private SourceFactory sourceFactory = analysisContext.getSourceFactory();
  private SemanticHighlighting[] semanticHighlightings;
  private Highlighting[] highlightings;
  private IPreferenceStore preferenceStore;
  private IColorManager colorManager = DartToolsPlugin.getDefault().getDartTextTools().getColorManager();

  public LineStyleProviderForDart() {
    super();
    preferenceStore = createCombinedPreferenceStore(null);
    initializeHighlightings();
  }

  @Override
  public void init(IStructuredDocument doc, ReconcilerHighlighter lit) {
    super.init(doc, lit);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public boolean prepareRegions(ITypedRegion typedRegion, int lineRequestStart,
      int lineRequestLength, Collection holdResults) {

    List<StyleRange> positions = new ArrayList<StyleRange>();
    // semantic highlighting
    try {
      int offset = typedRegion.getOffset();
      int length = typedRegion.getLength();
      String source = getDocument().get(offset, length);
      CompilationUnit unit = parseUnit(source);
      SemanticHighlightingEngine engine = new SemanticHighlightingEngine(
          semanticHighlightings,
          highlightings);
      engine.analyze(getDocument(), offset, unit, positions);
    } catch (BadLocationException ex) {
      // do nothing
    } catch (Exception all) {
      // do nothing
    }

    // syntax highlighting
    {
      DartTextTools tools = DartToolsPlugin.getDefault().getDartTextTools();
      IDocumentPartitioner part = tools.createDocumentPartitioner();
      part.connect(getDocument());
      IDocumentExtension3 doc3 = (IDocumentExtension3) getDocument();
      doc3.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, part);
      SyntacticHighlightingEngine engine = new SyntacticHighlightingEngine(
          colorManager,
          preferenceStore);
      engine.analyze(getDocument(), typedRegion, positions);
      doc3.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, null);
      part.disconnect();
    }

    Collections.sort(positions, new StyleRangeComparator());
    holdResults.addAll(positions);
    return true;
  }

  @Override
  protected TextAttribute getAttributeFor(ITextRegion region) {
    return null; // not used since prepareRegions() is defined
  }

  @Override
  protected IPreferenceStore getColorPreferences() {
    return preferenceStore;
  }

  @Override
  protected void loadColors() {
    // not used since prepareRegions() is defined
  }

  private void addColor(String colorKey) {
    if (colorManager != null && colorKey != null && colorManager.getColor(colorKey) == null) {
      RGB rgb = PreferenceConverter.getColor(preferenceStore, colorKey);
      if (colorManager instanceof IColorManagerExtension) {
        IColorManagerExtension ext = (IColorManagerExtension) colorManager;
        ext.unbindColor(colorKey);
        ext.bindColor(colorKey, rgb);
      }
    }
  }

  private String convertPath(String path) {
    if (File.separator.equals("/")) {
      // We're on a unix-ish OS.
      return path;
    } else {
      // On windows, the path separator is '\'.
      return path.replaceAll("/", "\\\\");
    }
  }

  // TODO(messick) Refactor to unify with copy in DartEditor.
  @SuppressWarnings("deprecation")
  private IPreferenceStore createCombinedPreferenceStore(IEditorInput input) {
    List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>(3);

    IProject project = EditorUtility.getProject(input);
    if (project != null) {
      stores.add(new EclipsePreferencesAdapter(new ProjectScope(project), DartCore.PLUGIN_ID));
    }

    stores.add(DartToolsPlugin.getDefault().getPreferenceStore());
    stores.add(new PreferencesAdapter(DartCore.getPlugin().getPluginPreferences()));
    stores.add(EditorsUI.getPreferenceStore());

    return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
  }

  private void ensureAnalysisContext() {
    if (analysisContext == null) {
      analysisContext = AnalysisEngine.getInstance().createAnalysisContext();
      analysisContext.setSourceFactory(sourceFactory);
      AnalysisOptionsImpl analysisOptionsImpl = new AnalysisOptionsImpl();
      analysisOptionsImpl.setHint(false);
      analysisContext.setAnalysisOptions(analysisOptionsImpl);
    }
  }

  // Copied from SemanticHighlightingManager
  private void initializeHighlightings() {
    semanticHighlightings = SemanticHighlightings.getSemanticHighlightings();
    highlightings = new Highlighting[semanticHighlightings.length];

    for (int i = 0, n = semanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = semanticHighlightings[i];
      String colorKey = SemanticHighlightings.getColorPreferenceKey(semanticHighlighting);
      addColor(colorKey);

      String boldKey = SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting);
      int style = preferenceStore.getBoolean(boldKey) ? SWT.BOLD : SWT.NORMAL;

      String italicKey = SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting);
      if (preferenceStore.getBoolean(italicKey)) {
        style |= SWT.ITALIC;
      }

      String strikethroughKey = SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting);
      if (preferenceStore.getBoolean(strikethroughKey)) {
        style |= TextAttribute.STRIKETHROUGH;
      }

      String underlineKey = SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting);
      if (preferenceStore.getBoolean(underlineKey)) {
        style |= TextAttribute.UNDERLINE;
      }

      boolean isEnabled = preferenceStore.getBoolean(SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting));

      highlightings[i] = new Highlighting(new TextAttribute(
          colorManager.getColor(PreferenceConverter.getColor(preferenceStore, colorKey)),
          null,
          style), isEnabled);
    }
  }

  private CompilationUnit parseUnit(String code) throws Exception {
    return parseUnit("/highlight.dart", code);
  }

  private CompilationUnit parseUnit(String path, String code) throws Exception {
    ensureAnalysisContext();
    // configure Source
    Source source = new FileBasedSource(
        sourceFactory.getContentCache(),
        new File(convertPath(path)).getAbsoluteFile());
    {
      sourceFactory.setContents(source, "");
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(source);
      analysisContext.applyChanges(changeSet);
    }
    // update Source
    analysisContext.setContents(source, code);
    // parse and resolve
    LibraryElement library = analysisContext.computeLibraryElement(source);
    CompilationUnit libraryUnit = analysisContext.resolveCompilationUnit(source, library);
    return libraryUnit;
  }

}
