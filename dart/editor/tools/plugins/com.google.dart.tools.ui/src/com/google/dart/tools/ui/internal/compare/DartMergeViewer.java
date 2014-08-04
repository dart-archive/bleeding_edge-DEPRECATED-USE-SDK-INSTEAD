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
package com.google.dart.tools.ui.internal.compare;

import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.PartEventAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class DartMergeViewer extends TextMergeViewer {

  private class CompilationUnitEditorAdapter extends CompilationUnitEditor {
    private boolean fInputSet = false;
    private int fTextOrientation;
    private boolean fEditable;

    CompilationUnitEditorAdapter(int textOrientation) {
      super();
      fTextOrientation = textOrientation;
      // TODO: has to be set here
      setPreferenceStore(createChainedPreferenceStore(null));
    }

    @Override
    public void close(boolean save) {
      getDocumentProvider().disconnect(getEditorInput());
    }

    @Override
    public void createActions() {
      if (fInputSet) {
        super.createActions();
        // to avoid handler conflicts disable extra actions
        // we're not handling by CompareHandlerService
        // TODO(scheglov) do we need this?
//        getCorrectionCommands().deregisterCommands();
        getRefactorActionGroup().dispose();
//        getGenerateActionGroup().dispose();
      }
      // else do nothing, we will create actions later, when input is available
    }

    @Override
    public void createPartControl(Composite composite) {
      SourceViewer sourceViewer = (SourceViewer) createDartSourceViewer(
          composite,
          new CompositeRuler(),
          null,
          false,
          fTextOrientation | SWT.H_SCROLL | SWT.V_SCROLL,
          createChainedPreferenceStore(null));
      setSourceViewer(this, sourceViewer);
      createNavigationActions();
      getSelectionProvider().addSelectionChangedListener(getSelectionChangedListener());
    }

    @Override
    public IWorkbenchPartSite getSite() {
      return DartMergeViewer.this.getSite();
    }

    // called by org.eclipse.ui.texteditor.TextEditorAction.canModifyEditor()
    @Override
    public boolean isEditable() {
      return fEditable;
    }

    @Override
    public boolean isEditorInputModifiable() {
      return fEditable;
    }

    @Override
    public boolean isEditorInputReadOnly() {
      return !fEditable;
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
      super.doSetInput(input);
      // the editor input has been explicitly set
      fInputSet = true;
    }

    // TODO(scheglov) no such method in super
//    @Override
//    protected void setActionsActivated(boolean state) {
//      super.setActionsActivated(state);
//    }

    private void setEditable(boolean editable) {
      fEditable = editable;
    }
  }

  private IPropertyChangeListener fPreferenceChangeListener;
  private IPreferenceStore fPreferenceStore;
  private Map<SourceViewer, DartSourceViewerConfiguration> fSourceViewerConfiguration;
  private Map<SourceViewer, CompilationUnitEditorAdapter> fEditor;

  private ArrayList<SourceViewer> fSourceViewer;

  private IWorkbenchPartSite fSite;

  public DartMergeViewer(Composite parent, int styles, CompareConfiguration mp) {
    super(parent, styles | SWT.LEFT_TO_RIGHT, mp);
  }

  // TODO(scheglov) will improve
//  @Override
//  public ITokenComparator createTokenComparator(String s) {
//    return new DartTokenComparator(s, new ITokenComparatorFactory() {
//      @Override
//      public ITokenComparator createTokenComparator(String text) {
//        return DartMergeViewer.super.createTokenComparator(text);
//      }
//    });
//  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    if (adapter == ITextEditorExtension3.class) {
      IEditorInput activeInput = (IEditorInput) super.getAdapter(IEditorInput.class);
      if (activeInput != null) {
        for (Iterator<CompilationUnitEditorAdapter> iterator = fEditor.values().iterator(); iterator.hasNext();) {
          CompilationUnitEditorAdapter editor = iterator.next();
          if (activeInput.equals(editor.getEditorInput())) {
            return editor;
          }
        }
      }
      return null;
    }
    return super.getAdapter(adapter);
  }

  public DartProject getDartProject(ICompareInput input) {

    if (input == null) {
      return null;
    }

    IResourceProvider rp = null;
    ITypedElement te = input.getLeft();
    if (te instanceof IResourceProvider) {
      rp = (IResourceProvider) te;
    }
    if (rp == null) {
      te = input.getRight();
      if (te instanceof IResourceProvider) {
        rp = (IResourceProvider) te;
      }
    }
    if (rp == null) {
      te = input.getAncestor();
      if (te instanceof IResourceProvider) {
        rp = (IResourceProvider) te;
      }
    }
    return null;
  }

  @Override
  public String getTitle() {
    return CompareMessages.DartMergeViewer_title;
  }

  @Override
  public void setInput(Object input) {
    if (input instanceof ICompareInput) {
      DartProject project = getDartProject((ICompareInput) input);
      if (project != null) {
        setPreferenceStore(createChainedPreferenceStore(project));
      }
    }
    super.setInput(input);
  }

  @Override
  protected void configureTextViewer(TextViewer viewer) {
    if (viewer instanceof SourceViewer) {
      SourceViewer sourceViewer = (SourceViewer) viewer;
      if (fSourceViewer == null) {
        fSourceViewer = new ArrayList<SourceViewer>();
      }
      if (!fSourceViewer.contains(sourceViewer)) {
        fSourceViewer.add(sourceViewer);
      }
      DartTextTools tools = DartCompareUtilities.getDartTextTools();
      if (tools != null) {
        IEditorInput editorInput = getEditorInput(sourceViewer);
        sourceViewer.unconfigure();
        if (editorInput == null) {
          sourceViewer.configure(getSourceViewerConfiguration(sourceViewer, null));
          return;
        }
        getSourceViewerConfiguration(sourceViewer, editorInput);
      }
    }
  }

  @Override
  protected void createControls(Composite composite) {
    super.createControls(composite);
    IWorkbenchPart workbenchPart = getCompareConfiguration().getContainer().getWorkbenchPart();
    if (workbenchPart != null) {
      IContextService service = (IContextService) workbenchPart.getSite().getService(
          IContextService.class);
      if (service != null) {
        service.activateContext("com.google.dart.tools.ui.dartEditorScope"); //$NON-NLS-1$
      }
    }
  }

  @Override
  protected SourceViewer createSourceViewer(Composite parent, int textOrientation) {
    SourceViewer sourceViewer;
    if (getSite() != null) {
      CompilationUnitEditorAdapter editor = new CompilationUnitEditorAdapter(textOrientation);
      editor.createPartControl(parent);

      ISourceViewer iSourceViewer = editor.getViewer();
      Assert.isTrue(iSourceViewer instanceof SourceViewer);
      sourceViewer = (SourceViewer) iSourceViewer;
      if (fEditor == null) {
        fEditor = new HashMap<SourceViewer, CompilationUnitEditorAdapter>(3);
      }
      fEditor.put(sourceViewer, editor);
    } else {
      sourceViewer = super.createSourceViewer(parent, textOrientation);
    }

    if (fSourceViewer == null) {
      fSourceViewer = new ArrayList<SourceViewer>();
    }
    fSourceViewer.add(sourceViewer);

    return sourceViewer;
  }

  @Override
  protected int findInsertionPosition(char type, ICompareInput input) {

    int pos = super.findInsertionPosition(type, input);
    if (pos != 0) {
      return pos;
    }

    if (input instanceof IDiffElement) {

      // find the other (not deleted) element
      DartNode otherDartElement = null;
      ITypedElement otherElement = null;
      switch (type) {
        case 'L':
          otherElement = input.getRight();
          break;
        case 'R':
          otherElement = input.getLeft();
          break;
      }
      if (otherElement instanceof DartNode) {
        otherDartElement = (DartNode) otherElement;
      }

      // find the parent of the deleted elements
      DartNode dartContainer = null;
      IDiffElement diffElement = (IDiffElement) input;
      IDiffContainer container = diffElement.getParent();
      if (container instanceof ICompareInput) {

        ICompareInput parent = (ICompareInput) container;
        ITypedElement element = null;

        switch (type) {
          case 'L':
            element = parent.getLeft();
            break;
          case 'R':
            element = parent.getRight();
            break;
        }

        if (element instanceof DartNode) {
          dartContainer = (DartNode) element;
        }
      }

      if (otherDartElement != null && dartContainer != null) {

        Object[] children;
        Position p;

        switch (otherDartElement.getTypeCode()) {

          case DartNode.PACKAGE:
            return 0;

          case DartNode.IMPORT_CONTAINER:
            // we have to find the place after the package declaration
            children = dartContainer.getChildren();
            if (children.length > 0) {
              DartNode packageDecl = null;
              for (int i = 0; i < children.length; i++) {
                DartNode child = (DartNode) children[i];
                switch (child.getTypeCode()) {
                  case DartNode.PACKAGE:
                    packageDecl = child;
                    break;
                  case DartNode.CLASS:
                    return child.getRange().getOffset();
                }
              }
              if (packageDecl != null) {
                p = packageDecl.getRange();
                return p.getOffset() + p.getLength();
              }
            }
            return dartContainer.getRange().getOffset();

          case DartNode.IMPORT:
            // append after last import
            p = dartContainer.getRange();
            return p.getOffset() + p.getLength();

          case DartNode.CLASS:
            // append after last class
            children = dartContainer.getChildren();
            if (children.length > 0) {
              for (int i = children.length - 1; i >= 0; i--) {
                DartNode child = (DartNode) children[i];
                switch (child.getTypeCode()) {
                  case DartNode.CLASS:
                  case DartNode.IMPORT_CONTAINER:
                  case DartNode.PACKAGE:
                  case DartNode.FIELD:
                    p = child.getRange();
                    return p.getOffset() + p.getLength();
                }
              }
            }
            return dartContainer.getAppendPosition().getOffset();

          case DartNode.METHOD:
            // append in next line after last child
            children = dartContainer.getChildren();
            if (children.length > 0) {
              DartNode child = (DartNode) children[children.length - 1];
              p = child.getRange();
              return findEndOfLine(dartContainer, p.getOffset() + p.getLength());
            }
            // otherwise use position from parser
            return dartContainer.getAppendPosition().getOffset();

          case DartNode.FIELD:
            // append after last field
            children = dartContainer.getChildren();
            if (children.length > 0) {
              DartNode method = null;
              for (int i = children.length - 1; i >= 0; i--) {
                DartNode child = (DartNode) children[i];
                switch (child.getTypeCode()) {
                  case DartNode.METHOD:
                    method = child;
                    break;
                  case DartNode.FIELD:
                    p = child.getRange();
                    return p.getOffset() + p.getLength();
                }
              }
              if (method != null) {
                return method.getRange().getOffset();
              }
            }
            return dartContainer.getAppendPosition().getOffset();
        }
      }

      if (dartContainer != null) {
        // return end of container
        Position p = dartContainer.getRange();
        return p.getOffset() + p.getLength();
      }
    }

    // we give up
    return 0;
  }

  @Override
  protected IDocumentPartitioner getDocumentPartitioner() {
    return DartCompareUtilities.createDartPartitioner();
  }

  @Override
  protected String getDocumentPartitioning() {
    return DartPartitions.DART_PARTITIONING;
  }

  @Override
  protected IEditorInput getEditorInput(ISourceViewer sourceViewer) {
    IEditorInput editorInput = super.getEditorInput(sourceViewer);
    if (editorInput == null) {
      return null;
    }
    if (getSite() == null) {
      return null;
    }
    if (!(editorInput instanceof IStorageEditorInput)) {
      return null;
    }
    return editorInput;
  }

  @Override
  protected void handleDispose(DisposeEvent event) {
    setPreferenceStore(null);
    fSourceViewer = null;
    if (fEditor != null) {
      for (Iterator<CompilationUnitEditorAdapter> iterator = fEditor.values().iterator(); iterator.hasNext();) {
        CompilationUnitEditorAdapter editor = iterator.next();
        editor.dispose();
      }
      fEditor = null;
    }
    fSite = null;
    super.handleDispose(event);
  }

  @Override
  protected boolean isEditorBacked(ITextViewer textViewer) {
    return getSite() != null;
  }

  @Override
  protected void setActionsActivated(SourceViewer sourceViewer, boolean state) {
    if (fEditor != null) {
      Object editor = fEditor.get(sourceViewer);
      if (editor instanceof CompilationUnitEditorAdapter) {
        CompilationUnitEditorAdapter cuea = (CompilationUnitEditorAdapter) editor;
//        cuea.setActionsActivated(state);

        IAction saveAction = cuea.getAction(ITextEditorActionConstants.SAVE);
        if (saveAction instanceof IPageListener) {
          PartEventAction partEventAction = (PartEventAction) saveAction;
          IWorkbenchPart compareEditorPart = getCompareConfiguration().getContainer().getWorkbenchPart();
          if (state) {
            partEventAction.partActivated(compareEditorPart);
          } else {
            partEventAction.partDeactivated(compareEditorPart);
          }
        }
      }
    }
  }

  @Override
  protected void setEditable(ISourceViewer sourceViewer, boolean state) {
    super.setEditable(sourceViewer, state);
    if (fEditor != null) {
      Object editor = fEditor.get(sourceViewer);
      if (editor instanceof CompilationUnitEditorAdapter) {
        ((CompilationUnitEditorAdapter) editor).setEditable(state);
      }
    }
  }

  private ChainedPreferenceStore createChainedPreferenceStore(DartProject project) {
    ArrayList<IPreferenceStore> stores = new ArrayList<IPreferenceStore>(4);
//    if (project != null) {
//      stores.add(new EclipsePreferencesAdapter(
//          new ProjectScope(project.getProject()),
//          DartCore.PLUGIN_ID));
//    }
    stores.add(DartToolsPlugin.getDefault().getPreferenceStore());
//    stores.add(new PreferencesAdapter(DartToolsPlugin.getDartCorePluginPreferences()));
    stores.add(EditorsUI.getPreferenceStore());
    return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
  }

  private int findEndOfLine(DartNode container, int pos) {
    int line;
    IDocument doc = container.getDocument();
    try {
      line = doc.getLineOfOffset(pos);
      pos = doc.getLineOffset(line + 1);
    } catch (BadLocationException ex) {
      // silently ignored
    }

    // ensure that position is within container range
    Position containerRange = container.getRange();
    int start = containerRange.getOffset();
    int end = containerRange.getOffset() + containerRange.getLength();
    if (pos < start) {
      return start;
    }
    if (pos >= end) {
      return end - 1;
    }

    return pos;
  }

  private IPreferenceStore getPreferenceStore() {
    if (fPreferenceStore == null) {
      setPreferenceStore(createChainedPreferenceStore(null));
    }
    return fPreferenceStore;
  }

  private IWorkbenchPartSite getSite() {
    if (fSite == null) {
      IWorkbenchPart workbenchPart = getCompareConfiguration().getContainer().getWorkbenchPart();
      fSite = workbenchPart != null ? workbenchPart.getSite() : null;
    }
    return fSite;
  }

  private DartSourceViewerConfiguration getSourceViewerConfiguration(SourceViewer sourceViewer,
      IEditorInput editorInput) {
    if (fSourceViewerConfiguration == null) {
      fSourceViewerConfiguration = new HashMap<SourceViewer, DartSourceViewerConfiguration>(3);
    }
    if (fPreferenceStore == null) {
      getPreferenceStore();
    }
    DartTextTools tools = DartCompareUtilities.getDartTextTools();
    DartSourceViewerConfiguration configuration = new DartSourceViewerConfiguration(
        tools.getColorManager(),
        fPreferenceStore,
        null,
        getDocumentPartitioning());
    if (editorInput != null) {
      // when input available, use editor
      CompilationUnitEditorAdapter editor = fEditor.get(sourceViewer);
      try {
        editor.init((IEditorSite) editor.getSite(), editorInput);
        editor.createActions();
        configuration = new DartSourceViewerConfiguration(
            tools.getColorManager(),
            fPreferenceStore,
            editor,
            getDocumentPartitioning());
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    }
    fSourceViewerConfiguration.put(sourceViewer, configuration);
    return fSourceViewerConfiguration.get(sourceViewer);
  }

  private void handlePropertyChange(PropertyChangeEvent event) {
    if (fSourceViewerConfiguration != null) {
      for (Iterator<Entry<SourceViewer, DartSourceViewerConfiguration>> iterator = fSourceViewerConfiguration.entrySet().iterator(); iterator.hasNext();) {
        Entry<SourceViewer, DartSourceViewerConfiguration> entry = iterator.next();
        DartSourceViewerConfiguration configuration = entry.getValue();
        if (configuration.affectsTextPresentation(event)) {
          configuration.handlePropertyChangeEvent(event);
          ITextViewer viewer = entry.getKey();
          viewer.invalidateTextPresentation();
        }
      }
    }
  }

  private void setPreferenceStore(IPreferenceStore ps) {
    if (fPreferenceChangeListener != null) {
      if (fPreferenceStore != null) {
        fPreferenceStore.removePropertyChangeListener(fPreferenceChangeListener);
      }
      fPreferenceChangeListener = null;
    }
    fPreferenceStore = ps;
    if (fPreferenceStore != null) {
      fPreferenceChangeListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
          handlePropertyChange(event);
        }
      };
      fPreferenceStore.addPropertyChangeListener(fPreferenceChangeListener);
    }
  }

  // no setter to private field AbstractTextEditor.fSourceViewer
  private void setSourceViewer(ITextEditor editor, SourceViewer viewer) {
    Field field = null;
    try {
      field = AbstractTextEditor.class.getDeclaredField("fSourceViewer"); //$NON-NLS-1$
    } catch (SecurityException ex) {
      DartToolsPlugin.log(ex);
    } catch (NoSuchFieldException ex) {
      DartToolsPlugin.log(ex);
    }
    field.setAccessible(true);
    try {
      field.set(editor, viewer);
    } catch (IllegalArgumentException ex) {
      DartToolsPlugin.log(ex);
    } catch (IllegalAccessException ex) {
      DartToolsPlugin.log(ex);
    }
  }
}
