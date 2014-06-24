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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.server.Outline;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.analysis.model.AnalysisServerData;
import com.google.dart.tools.core.analysis.model.AnalysisServerOutlineListener;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.internal.search.ui.DartSearchActionGroup;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.actions.DartdocActionGroup;
import com.google.dart.tools.ui.actions.OpenEditorActionGroup;
import com.google.dart.tools.ui.actions.OpenViewActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup;
import com.google.dart.tools.ui.actions.ShowSelectionLabelAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.actions.FoldingActionGroup;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.formatter.DartFormatter;
import com.google.dart.tools.ui.internal.formatter.DartFormatter.FormattedSource;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.IProductConstants;
import com.google.dart.tools.ui.internal.text.ProductProperties;
import com.google.dart.tools.ui.internal.text.dart.DartPriorityFileEditor;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingEditor;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingStrategy;
import com.google.dart.tools.ui.internal.text.dart.hover.SourceViewerInformationControl;
import com.google.dart.tools.ui.internal.text.editor.saveactions.RemoveTrailingWhitespaceAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.GoToNextPreviousMemberAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.SelectionHistory;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectEnclosingAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectHistoryAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectNextAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectPreviousAction;
import com.google.dart.tools.ui.internal.text.editor.selectionactions.StructureSelectionAction;
import com.google.dart.tools.ui.internal.text.functions.DartChangeHover;
import com.google.dart.tools.ui.internal.text.functions.DartPairMatcher;
import com.google.dart.tools.ui.internal.text.functions.DartWordFinder;
import com.google.dart.tools.ui.internal.text.functions.DartWordIterator;
import com.google.dart.tools.ui.internal.text.functions.DocumentCharacterIterator;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.internal.util.DartUIHelp;
import com.google.dart.tools.ui.internal.viewsupport.IViewPartInputProvider;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;
import com.google.dart.tools.ui.text.folding.IDartFoldingStructureProvider;
import com.google.dart.tools.ui.text.folding.IDartFoldingStructureProviderExtension;

import com.ibm.icu.text.BreakIterator;

import org.eclipse.core.commands.operations.IOperationApprover;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.MarkSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.DefaultEncodingSupport;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.operations.NonLocalUndoUserApprover;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dart specific text editor.
 */
@SuppressWarnings({"unused", "deprecation"})
public abstract class DartEditor extends AbstractDecoratedTextEditor implements
    IViewPartInputProvider, DartReconcilingEditor, DartPriorityFileEditor {

  /**
   * Adapts an options {@link IEclipsePreferences} to
   * {@link org.eclipse.jface.preference.IPreferenceStore}.
   * <p>
   * This preference store is read-only i.e. write access throws an
   * {@link java.lang.UnsupportedOperationException}.
   * </p>
   */
  public static class EclipsePreferencesAdapter implements IPreferenceStore {

    /**
     * Preference change listener. Listens for events preferences fires a
     * {@link org.eclipse.jface.util.PropertyChangeEvent} on this adapter with arguments from the
     * received event.
     */
    private class PreferenceChangeListener implements IEclipsePreferences.IPreferenceChangeListener {

      /**
       * {@inheritDoc}
       */
      @Override
      public void preferenceChange(final IEclipsePreferences.PreferenceChangeEvent event) {
        if (Display.getCurrent() == null) {
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              firePropertyChangeEvent(event.getKey(), event.getOldValue(), event.getNewValue());
            }
          });
        } else {
          firePropertyChangeEvent(event.getKey(), event.getOldValue(), event.getNewValue());
        }
      }
    }

    /** Listeners on on this adapter */
    private final ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

    /** Listener on the node */
    private final IEclipsePreferences.IPreferenceChangeListener fListener = new PreferenceChangeListener();

    /** wrapped node */
    private final IScopeContext fContext;
    private final String fQualifier;

    /**
     * Initialize with the node to wrap
     * 
     * @param context the context to access
     * @param qualifier the qualifier
     */
    public EclipsePreferencesAdapter(IScopeContext context, String qualifier) {
      fContext = context;
      fQualifier = qualifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {
      if (fListeners.size() == 0) {
        getNode().addPreferenceChangeListener(fListener);
      }
      fListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(String name) {
      return getNode().get(name, null) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
      PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ((IPropertyChangeListener) listeners[i]).propertyChange(event);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(String name) {
      return getNode().getBoolean(name, BOOLEAN_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getDefaultBoolean(String name) {
      return BOOLEAN_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDefaultDouble(String name) {
      return DOUBLE_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getDefaultFloat(String name) {
      return FLOAT_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultInt(String name) {
      return INT_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultLong(String name) {
      return LONG_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultString(String name) {
      return STRING_DEFAULT_DEFAULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(String name) {
      return getNode().getDouble(name, DOUBLE_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat(String name) {
      return getNode().getFloat(name, FLOAT_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(String name) {
      return getNode().getInt(name, INT_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(String name) {
      return getNode().getLong(name, LONG_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String name) {
      return getNode().get(name, STRING_DEFAULT_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDefault(String name) {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsSaving() {
      try {
        return getNode().keys().length > 0;
      } catch (BackingStoreException e) {
        // ignore
      }
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putValue(String name, String value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {
      fListeners.remove(listener);
      if (fListeners.size() == 0) {
        getNode().removePreferenceChangeListener(fListener);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, boolean value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, double value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, float value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, int value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, long value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault(String name, String defaultObject) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToDefault(String name) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, boolean value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, double value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, float value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, int value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, long value) {
      throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String name, String value) {
      throw new UnsupportedOperationException();
    }

    protected IEclipsePreferences getNode() {
      return fContext.getNode(fQualifier);
    }

  }

  /**
   * Internal implementation class for a change listener.
   */
  protected abstract class AbstractSelectionChangedListener implements ISelectionChangedListener {

    /**
     * Installs this selection changed listener with the given selection provider. If the selection
     * provider is a post selection provider, post selection changed events are the preferred
     * choice, otherwise normal selection changed events are requested.
     * 
     * @param selectionProvider
     */
    public void install(ISelectionProvider selectionProvider) {
      if (selectionProvider == null) {
        return;
      }

      if (selectionProvider instanceof IPostSelectionProvider) {
        IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
        provider.addPostSelectionChangedListener(this);
      } else {
        selectionProvider.addSelectionChangedListener(this);
      }
    }

    /**
     * Removes this selection changed listener from the given selection provider.
     * 
     * @param selectionProvider the selection provider
     */
    public void uninstall(ISelectionProvider selectionProvider) {
      if (selectionProvider == null) {
        return;
      }

      if (selectionProvider instanceof IPostSelectionProvider) {
        IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
        provider.removePostSelectionChangedListener(this);
      } else {
        selectionProvider.removeSelectionChangedListener(this);
      }
    }
  }

  /**
   * Text operation action to delete the next sub-word.
   */
  protected class DeleteNextSubWordAction extends NextSubWordAction implements IUpdate {

    /**
     * Creates a new delete next sub-word action.
     */
    public DeleteNextSubWordAction() {
      super(ST.DELETE_WORD_NEXT);
    }

    @Override
    public void update() {
      setEnabled(isEditorInputModifiable());
    }

    @Override
    protected void setCaretPosition(final int position) {
      if (!validateEditorInputState()) {
        return;
      }

      final ISourceViewer viewer = getSourceViewer();
      StyledText text = viewer.getTextWidget();
      Point widgetSelection = text.getSelection();
      if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
        final int caret = text.getCaretOffset();
        final int offset = modelOffset2WidgetOffset(viewer, position);

        if (caret == widgetSelection.x) {
          text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
        } else {
          text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
        }
        text.invokeAction(ST.DELETE_NEXT);
      } else {
        Point selection = viewer.getSelectedRange();
        final int caret, length;
        if (selection.y != 0) {
          caret = selection.x;
          length = selection.y;
        } else {
          caret = widgetOffset2ModelOffset(viewer, text.getCaretOffset());
          length = position - caret;
        }

        try {
          viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
        } catch (BadLocationException exception) {
          // Should not happen
        }
      }
    }
  }

  /**
   * Text operation action to delete the previous sub-word.
   */
  protected class DeletePreviousSubWordAction extends PreviousSubWordAction implements IUpdate {

    /**
     * Creates a new delete previous sub-word action.
     */
    public DeletePreviousSubWordAction() {
      super(ST.DELETE_WORD_PREVIOUS);
    }

    @Override
    public void update() {
      setEnabled(isEditorInputModifiable());
    }

    @Override
    protected void setCaretPosition(int position) {
      if (!validateEditorInputState()) {
        return;
      }

      final int length;
      final ISourceViewer viewer = getSourceViewer();
      StyledText text = viewer.getTextWidget();
      Point widgetSelection = text.getSelection();
      if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
        final int caret = text.getCaretOffset();
        final int offset = modelOffset2WidgetOffset(viewer, position);

        if (caret == widgetSelection.x) {
          text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
        } else {
          text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
        }
        text.invokeAction(ST.DELETE_PREVIOUS);
      } else {
        Point selection = viewer.getSelectedRange();
        if (selection.y != 0) {
          position = selection.x;
          length = selection.y;
        } else {
          length = widgetOffset2ModelOffset(viewer, text.getCaretOffset()) - position;
        }

        try {
          viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
        } catch (BadLocationException exception) {
          // Should not happen
        }
      }
    }
  }

  /**
   * Format element action to format the enclosing Dart element.
   * <p>
   * The format element action works as follows:
   * <ul>
   * <li>If there is no selection and the caret is positioned on a Dart element, only this element
   * is formatted. If the element has some accompanying comment, then the comment is formatted as
   * well.</li>
   * <li>If the selection spans one or more partitions of the document, then all partitions covered
   * by the selection are entirely formatted.</li>
   * <p>
   * Partitions at the end of the selection are not completed, except for comments.
   */
  protected class FormatElementAction extends Action implements IUpdate {

    private class FormatElementJob extends Job {

      // For debugging.
      private final boolean PRINT_TO_CONSOLE = false;

      private final IFile file;

      FormatElementJob(IFile file) {
        super("Formatting " + file.getLocation());
        this.file = file;
      }

      @Override
      protected IStatus run(IProgressMonitor monitor) {

        MessageConsole console = DartCore.getConsole();

        if (PRINT_TO_CONSOLE) {
          console.clear();
          console.println("Formatting " + file.getName() + " ...");
        }

        final IDocument document = getSourceViewer().getDocument();

        try {

          final Point[] selection = new Point[1];
          Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
              selection[0] = getSourceViewer().getSelectedRange();
            }
          });

          final String unformattedSource = document.get();
          final FormattedSource formatResult = DartFormatter.format(
              unformattedSource,
              selection[0],
              monitor);
          Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
              if (!formatResult.source.equals(unformattedSource)) {
                document.set(formatResult.source);
                getSourceViewer().revealRange(
                    formatResult.selectionOffset,
                    formatResult.selectionLength);
                getSourceViewer().setSelectedRange(
                    formatResult.selectionOffset,
                    formatResult.selectionLength);
              }
            }
          });
        } catch (Exception e) {

          String msg = null;

          if (isParseFailure(e)) {
            msg = "fix parse errors and try again.";
          } else {
            msg = e.getMessage();
            DartToolsPlugin.log(e);
          }

          if (PRINT_TO_CONSOLE) {
            console.clear();
            console.println("Unable to format " + file.getName() + ": " + msg);
          } else {
            showMessage("Unable to format " + file.getName() + ": " + msg);
          }

          return Status.CANCEL_STATUS;
        }

        if (PRINT_TO_CONSOLE) {
          console.clear();
        }

        return Status.OK_STATUS;
      }

      private boolean isParseFailure(Exception e) {
        //TODO (pquitslund): use a return code or something better to signal parse failures
        return e.getMessage().startsWith("Uncaught Error: An error occured while parsing");
      }
    };

    FormatElementAction() {
      setEnabled(isEditorInputModifiable());
      setText("Format");
    }

    @Override
    public boolean isEnabled() {
      return super.isEnabled() && DartFormatter.isAvailable();
    }

    @Override
    public void run() {
      IFile file = getInputResourceFile();
      new FormatElementJob(file).schedule();
    }

    @Override
    public void update() {
      setEnabled(isEditorInputModifiable());
    }

    private void showMessage(final String msg) {
      final Display display = getSite().getShell().getDisplay();
      display.syncExec(new Runnable() {
        @Override
        public void run() {
          IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
          if (statusLine != null) {
            statusLine.setMessage(true, msg, null);
          }
          display.beep();
        }
      });
    }

  }

  /**
   * Text navigation action to navigate to the next sub-word.
   */
  protected class NavigateNextSubWordAction extends NextSubWordAction {

    /**
     * Creates a new navigate next sub-word action.
     */
    public NavigateNextSubWordAction() {
      super(ST.WORD_NEXT);
    }

    @Override
    protected void setCaretPosition(final int position) {
      getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
    }
  }

  /**
   * Text navigation action to navigate to the previous sub-word.
   */
  protected class NavigatePreviousSubWordAction extends PreviousSubWordAction {

    /**
     * Creates a new navigate previous sub-word action.
     */
    public NavigatePreviousSubWordAction() {
      super(ST.WORD_PREVIOUS);
    }

    @Override
    protected void setCaretPosition(final int position) {
      getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
    }
  }

  /**
   * Text navigation action to navigate to the next sub-word.
   */
  protected abstract class NextSubWordAction extends TextNavigationAction {

    protected DartWordIterator fIterator = new DartWordIterator();

    /**
     * Creates a new next sub-word action.
     * 
     * @param code Action code for the default operation. Must be an action code from @see
     *          org.eclipse.swt.custom.ST.
     */
    protected NextSubWordAction(int code) {
      super(getSourceViewer().getTextWidget(), code);
    }

    @Override
    public void run() {
      // Check whether we are in a Dart code partition and the preference is enabled
      final IPreferenceStore store = getPreferenceStore();
      if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
        super.run();
        return;
      }

      final ISourceViewer viewer = getSourceViewer();
      final IDocument document = viewer.getDocument();
      fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
      int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
      if (position == -1) {
        return;
      }

      int next = findNextPosition(position);
      try {
        if (isBlockSelectionModeEnabled()
            && document.getLineOfOffset(next) != document.getLineOfOffset(position)) {
          super.run(); // may navigate into virtual white space
        } else if (next != BreakIterator.DONE) {
          setCaretPosition(next);
          getTextWidget().showSelection();
          fireSelectionChanged();
        }
      } catch (BadLocationException x) {
        // ignore
      }
    }

    /**
     * Finds the next position after the given position.
     * 
     * @param position the current position
     * @return the next position
     */
    protected int findNextPosition(int position) {
      ISourceViewer viewer = getSourceViewer();
      int widget = -1;
      int next = position;
      while (next != BreakIterator.DONE && widget == -1) { // TODO: optimize
        next = fIterator.following(next);
        if (next != BreakIterator.DONE) {
          widget = modelOffset2WidgetOffset(viewer, next);
        }
      }

      IDocument document = viewer.getDocument();
      LinkedModeModel model = LinkedModeModel.getModel(document, position);
      if (model != null) {
        LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
        if (linkedPosition != null) {
          int linkedPositionEnd = linkedPosition.getOffset() + linkedPosition.getLength();
          if (position != linkedPositionEnd && linkedPositionEnd < next) {
            next = linkedPositionEnd;
          }
        } else {
          LinkedPosition nextLinkedPosition = model.findPosition(new LinkedPosition(
              document,
              next,
              0));
          if (nextLinkedPosition != null) {
            int nextLinkedPositionOffset = nextLinkedPosition.getOffset();
            if (position != nextLinkedPositionOffset && nextLinkedPositionOffset < next) {
              next = nextLinkedPositionOffset;
            }
          }
        }
      }

      return next;
    }

    /**
     * Sets the caret position to the sub-word boundary given with <code>position</code>.
     * 
     * @param position Position where the action should move the caret
     */
    protected abstract void setCaretPosition(int position);
  }

  /**
   * Text navigation action to navigate to the previous sub-word.
   */
  protected abstract class PreviousSubWordAction extends TextNavigationAction {

    protected DartWordIterator fIterator = new DartWordIterator();

    /**
     * Creates a new previous sub-word action.
     * 
     * @param code Action code for the default operation. Must be an action code from @see
     *          org.eclipse.swt.custom.ST.
     */
    protected PreviousSubWordAction(final int code) {
      super(getSourceViewer().getTextWidget(), code);
    }

    @Override
    public void run() {
      // Check whether we are in a Dart code partition and the preference is enabled
      final IPreferenceStore store = getPreferenceStore();
      if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
        super.run();
        return;
      }

      final ISourceViewer viewer = getSourceViewer();
      final IDocument document = viewer.getDocument();
      fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
      int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
      if (position == -1) {
        return;
      }

      int previous = findPreviousPosition(position);
      try {
        if (isBlockSelectionModeEnabled()
            && document.getLineOfOffset(previous) != document.getLineOfOffset(position)) {
          super.run(); // may navigate into virtual white space
        } else if (previous != BreakIterator.DONE) {
          setCaretPosition(previous);
          getTextWidget().showSelection();
          fireSelectionChanged();
        }
      } catch (BadLocationException x) {
        // ignore - getLineOfOffset failed
      }

    }

    /**
     * Finds the previous position before the given position.
     * 
     * @param position the current position
     * @return the previous position
     */
    protected int findPreviousPosition(int position) {
      ISourceViewer viewer = getSourceViewer();
      int widget = -1;
      int previous = position;
      while (previous != BreakIterator.DONE && widget == -1) { // TODO: optimize
        previous = fIterator.preceding(previous);
        if (previous != BreakIterator.DONE) {
          widget = modelOffset2WidgetOffset(viewer, previous);
        }
      }

      IDocument document = viewer.getDocument();
      LinkedModeModel model = LinkedModeModel.getModel(document, position);
      if (model != null) {
        LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
        if (linkedPosition != null) {
          int linkedPositionOffset = linkedPosition.getOffset();
          if (position != linkedPositionOffset && previous < linkedPositionOffset) {
            previous = linkedPositionOffset;
          }
        } else {
          LinkedPosition previousLinkedPosition = model.findPosition(new LinkedPosition(
              document,
              previous,
              0));
          if (previousLinkedPosition != null) {
            int previousLinkedPositionEnd = previousLinkedPosition.getOffset()
                + previousLinkedPosition.getLength();
            if (position != previousLinkedPositionEnd && previous < previousLinkedPositionEnd) {
              previous = previousLinkedPositionEnd;
            }
          }
        }
      }

      return previous;
    }

    /**
     * Sets the caret position to the sub-word boundary given with <code>position</code>.
     * 
     * @param position Position where the action should move the caret
     */
    protected abstract void setCaretPosition(int position);
  }

  /**
   * Text operation action to select the next sub-word.
   */
  protected class SelectNextSubWordAction extends NextSubWordAction {

    /**
     * Creates a new select next sub-word action.
     */
    public SelectNextSubWordAction() {
      super(ST.SELECT_WORD_NEXT);
    }

    @Override
    protected void setCaretPosition(final int position) {
      final ISourceViewer viewer = getSourceViewer();

      final StyledText text = viewer.getTextWidget();
      if (text != null && !text.isDisposed()) {

        final Point selection = text.getSelection();
        final int caret = text.getCaretOffset();
        final int offset = modelOffset2WidgetOffset(viewer, position);

        if (caret == selection.x) {
          text.setSelectionRange(selection.y, offset - selection.y);
        } else {
          text.setSelectionRange(selection.x, offset - selection.x);
        }
      }
    }
  }

  /**
   * Text operation action to select the previous sub-word.
   */
  protected class SelectPreviousSubWordAction extends PreviousSubWordAction {

    /**
     * Creates a new select previous sub-word action.
     */
    public SelectPreviousSubWordAction() {
      super(ST.SELECT_WORD_PREVIOUS);
    }

    @Override
    protected void setCaretPosition(final int position) {
      final ISourceViewer viewer = getSourceViewer();

      final StyledText text = viewer.getTextWidget();
      if (text != null && !text.isDisposed()) {

        final Point selection = text.getSelection();
        final int caret = text.getCaretOffset();
        final int offset = modelOffset2WidgetOffset(viewer, position);

        if (caret == selection.x) {
          text.setSelectionRange(selection.y, offset - selection.y);
        } else {
          text.setSelectionRange(selection.x, offset - selection.x);
        }
      }
    }
  }

  /**
   * This action implements smart home. Instead of going to the start of a line it does the
   * following: - if smart home/end is enabled and the caret is after the line's first
   * non-whitespace then the caret is moved directly before it, taking JavaDoc and multi-line
   * comments into account. - if the caret is before the line's first non-whitespace the caret is
   * moved to the beginning of the line - if the caret is at the beginning of the line see first
   * case.
   */
  protected class SmartLineStartAction extends LineStartAction {

    /**
     * Creates a new smart line start action
     * 
     * @param textWidget the styled text widget
     * @param doSelect a boolean flag which tells if the text up to the beginning of the line should
     *          be selected
     */
    public SmartLineStartAction(final StyledText textWidget, final boolean doSelect) {
      super(textWidget, doSelect);
    }

    @Override
    protected int getLineStartPosition(final IDocument document, final String line,
        final int length, final int offset) {

      String type = IDocument.DEFAULT_CONTENT_TYPE;
      try {
        type = TextUtilities.getContentType(
            document,
            DartPartitions.DART_PARTITIONING,
            offset,
            true);
      } catch (BadLocationException exception) {
        // Should not happen
      }

      int index = super.getLineStartPosition(document, line, length, offset);
      if (type.equals(DartPartitions.DART_DOC)
          || type.equals(DartPartitions.DART_MULTI_LINE_COMMENT)) {
        if (index < length - 1 && line.charAt(index) == '*' && line.charAt(index + 1) != '/') {
          do {
            ++index;
          } while (index < length && Character.isWhitespace(line.charAt(index)));
        }
      } else {
        if (index < length - 1 && line.charAt(index) == '/' && line.charAt(index + 1) == '/') {
          if (type.equals(DartPartitions.DART_SINGLE_LINE_DOC)
              && (index < length - 2 && line.charAt(index + 2) == '/')) {
            index++;
          }
          index++;
          do {
            ++index;
          } while (index < length && Character.isWhitespace(line.charAt(index)));
        }
      }
      return index;
    }
  }

  /**
   * Finds and marks occurrence annotations.
   */
  class OccurrencesFinderJob extends Job {

    private final IDocument fDocument;
    private final ISelection fSelection;
    private ISelectionValidator fPostSelectionValidator;
    private boolean fCanceled = false;
    private IProgressMonitor fProgressMonitor;
    private final Position[] fPositions;

    public OccurrencesFinderJob(IDocument document, Position[] positions, ISelection selection) {
      super(DartEditorMessages.JavaEditor_markOccurrences_job_name);
      fDocument = document;
      fSelection = selection;
      fPositions = positions;

      if (getSelectionProvider() instanceof ISelectionValidator) {
        fPostSelectionValidator = (ISelectionValidator) getSelectionProvider();
      }
    }

    @Override
    public IStatus run(IProgressMonitor progressMonitor) {

      fProgressMonitor = progressMonitor;

      if (isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      ITextViewer textViewer = getViewer();
      if (textViewer == null) {
        return Status.CANCEL_STATUS;
      }

      IDocument document = textViewer.getDocument();
      if (document == null) {
        return Status.CANCEL_STATUS;
      }

      IDocumentProvider documentProvider = getDocumentProvider();
      if (documentProvider == null) {
        return Status.CANCEL_STATUS;
      }

      IAnnotationModel annotationModel = documentProvider.getAnnotationModel(getEditorInput());
      if (annotationModel == null) {
        return Status.CANCEL_STATUS;
      }

      // Add occurrence annotations
      int length = fPositions.length;
      Map<Annotation, Position> annotationMap = new HashMap<Annotation, Position>(length);
      for (int i = 0; i < length; i++) {

        if (isCanceled()) {
          return Status.CANCEL_STATUS;
        }

        String message;
        Position position = fPositions[i];

        // Create & add annotation
        try {
          message = document.get(position.offset, position.length);
        } catch (BadLocationException ex) {
          // Skip this match
          continue;
        }
        annotationMap.put(new Annotation("com.google.dart.tools.ui.occurrences", false, message), //$NON-NLS-1$
            position);
      }

      if (isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      synchronized (getLockObject(annotationModel)) {
        if (annotationModel instanceof IAnnotationModelExtension) {
          ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
              fOccurrenceAnnotations,
              annotationMap);
        } else {
          removeOccurrenceAnnotations();
          Iterator<Map.Entry<Annotation, Position>> iter = annotationMap.entrySet().iterator();
          while (iter.hasNext()) {
            Map.Entry<Annotation, Position> mapEntry = iter.next();
            annotationModel.addAnnotation(mapEntry.getKey(), mapEntry.getValue());
          }
        }
        fOccurrenceAnnotations = annotationMap.keySet().toArray(
            new Annotation[annotationMap.keySet().size()]);
      }

      return Status.OK_STATUS;
    }

    // cannot use cancel() because it is declared final
    void doCancel() {
      fCanceled = true;
      cancel();
    }

    private boolean isCanceled() {
      return fCanceled
          || fProgressMonitor.isCanceled()
          || fPostSelectionValidator != null
          && !(fPostSelectionValidator.isValid(fSelection) || fForcedMarkOccurrencesSelection == fSelection)
          || LinkedModeModel.hasInstalledModel(fDocument);
    }
  }

  /**
   * Cancels the occurrences finder job upon document changes.
   */
  class OccurrencesFinderJobCanceler implements IDocumentListener, ITextInputListener {

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
      if (fOccurrencesFinderJob != null) {
        fOccurrencesFinderJob.doCancel();
      }
    }

    @Override
    public void documentChanged(DocumentEvent event) {
    }

    @Override
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
      if (oldInput == null) {
        return;
      }

      oldInput.removeDocumentListener(this);
    }

    @Override
    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
      if (newInput == null) {
        return;
      }
      newInput.addDocumentListener(this);
    }

    public void install() {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer == null) {
        return;
      }

      StyledText text = sourceViewer.getTextWidget();
      if (text == null || text.isDisposed()) {
        return;
      }

      sourceViewer.addTextInputListener(this);

      IDocument document = sourceViewer.getDocument();
      if (document != null) {
        document.addDocumentListener(this);
      }
    }

    public void uninstall() {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        sourceViewer.removeTextInputListener(this);
      }

      IDocumentProvider documentProvider = getDocumentProvider();
      if (documentProvider != null) {
        IDocument document = documentProvider.getDocument(getEditorInput());
        if (document != null) {
          document.removeDocumentListener(this);
        }
      }
    }
  }

  /**
   * Internal activation listener.
   */
  private class ActivationListener implements IWindowListener {

    @Override
    public void windowActivated(IWorkbenchWindow window) {
      if (window == getEditorSite().getWorkbenchWindow() && fMarkOccurrenceAnnotations
          && isActivePart()) {
        fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
        com.google.dart.engine.ast.CompilationUnit unit = getInputUnit();
        if (unit != null) {
          updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, unit);
        }
      }
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
      if (window == getEditorSite().getWorkbenchWindow() && fMarkOccurrenceAnnotations
          && isActivePart()) {
        removeOccurrenceAnnotations();
      }
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
    }
  }

  private class DartSelectionProvider extends SelectionProvider {
    private final Map<ISelectionChangedListener, ISelectionChangedListener> listeners = Maps.newHashMap();

    @Override
    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
      ISelectionChangedListener dartListener = newDartSelectionListener(listener);
      super.addPostSelectionChangedListener(dartListener);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
      ISelectionChangedListener dartListener = newDartSelectionListener(listener);
      super.addSelectionChangedListener(dartListener);
    }

    @Override
    public ISelection getSelection() {
      ISelection selection = super.getSelection();
      return newDartSelection(selection);
    }

    @Override
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
      ISelectionChangedListener dartListener = listeners.remove(listener);
      if (dartListener != null) {
        super.removePostSelectionChangedListener(dartListener);
      }
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
      ISelectionChangedListener dartListener = listeners.remove(listener);
      if (dartListener != null) {
        super.removeSelectionChangedListener(dartListener);
      }
    }

    private ISelection newDartSelection(ISelection selection) {
      if (selection == null) {
        return new DartSelection(DartEditor.this, null, -1, 0);
      }
      if (selection instanceof MarkSelection) {
        MarkSelection sel = (MarkSelection) selection;
        IDocument doc = sel.getDocument();
        return new DartSelection(DartEditor.this, doc, sel.getOffset(), sel.getLength());
      }
      ITextSelection textSelection = (ITextSelection) selection;
      // prepare document
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer == null) {
        return new DartSelection(DartEditor.this, null, -1, 0);
      }
      IDocument document = sourceViewer.getDocument();
      // prepare AssistContext
      AssistContext assistContext = getAssistContext(textSelection);
      // OK, wrap into DartSelection
      return new DartSelection(
          DartEditor.this,
          document,
          textSelection.getOffset(),
          textSelection.getLength());
    }

    private ISelectionChangedListener newDartSelectionListener(
        final ISelectionChangedListener listener) {
      ISelectionChangedListener dartListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          listener.selectionChanged(new SelectionChangedEvent(
              event.getSelectionProvider(),
              newDartSelection(event.getSelection())));
        }
      };
      listeners.put(listener, dartListener);
      return dartListener;
    }
  }

  /**
   * Updates the outline page selection and this editor's range indicator.
   */
  private class EditorSelectionChangedListener extends AbstractSelectionChangedListener {

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      // XXX: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=56161
      DartEditor.this.selectionChanged();
    }
  }

  /**
   * An adapter from new to old source reference abstractions.
   */
  private static final class SourceReferenceAdapter implements SourceReference {

    private final SourceRange sourceRange;

    private SourceReferenceAdapter(com.google.dart.engine.element.Element element) {
      sourceRange = new SourceRange(element.getNameOffset(), element.getDisplayName().length());
    }

    @Override
    public boolean exists() {
      // Unused in this context
      return true;
    }

    @Override
    public SourceRange getNameRange() throws DartModelException {
      return sourceRange;
    }

    @Override
    public String getSource() throws DartModelException {
      // Unused in this context
      return "";
    }

    @Override
    public SourceRange getSourceRange() throws DartModelException {
      // Unused in this context
      return sourceRange;
    }

  }

  /**
   * Runner that will toggle folding either instantly (if the editor is visible) or the next time it
   * becomes visible. If a runner is started when there is already one registered, the registered
   * one is canceled as toggling folding twice is a no-op.
   * <p>
   * The access to the fFoldingRunner field is not thread-safe, it is assumed that
   * <code>runWhenNextVisible</code> is only called from the UI thread.
   * </p>
   */
  private final class ToggleFoldingRunner implements IPartListener2 {
    /**
     * The workbench page we registered the part listener with, or <code>null</code>.
     */
    private IWorkbenchPage fPage;

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
      if (DartEditor.this.equals(partRef.getPart(false))) {
        cancel();
      }
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
      if (DartEditor.this.equals(partRef.getPart(false))) {
        cancel();
        toggleFolding();
      }
    }

    /**
     * Makes sure that the editor's folding state is correct the next time it becomes visible. If it
     * already is visible, it toggles the folding state. If not, it either registers a part listener
     * to toggle folding when the editor becomes visible, or cancels an already registered runner.
     */
    public void runWhenNextVisible() {
      // if there is one already: toggling twice is the identity
      if (fFoldingRunner != null) {
        fFoldingRunner.cancel();
        return;
      }
      IWorkbenchPartSite site = getSite();
      if (site != null) {
        IWorkbenchPage page = site.getPage();
        if (!page.isPartVisible(DartEditor.this)) {
          // if we're not visible - defer until visible
          fPage = page;
          fFoldingRunner = this;
          page.addPartListener(this);
          return;
        }
      }
      // we're visible - run now
      toggleFolding();
    }

    /**
     * Remove the listener and clear the field.
     */
    private void cancel() {
      if (fPage != null) {
        fPage.removePartListener(this);
        fPage = null;
      }
      if (fFoldingRunner == this) {
        fFoldingRunner = null;
      }
    }

    /**
     * Does the actual toggling of projection.
     */
    private void toggleFolding() {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer instanceof ProjectionViewer) {
        ProjectionViewer pv = (ProjectionViewer) sourceViewer;
        if (pv.isProjectionMode() != isFoldingEnabled()) {
          if (pv.canDoOperation(ProjectionViewer.TOGGLE)) {
            pv.doOperation(ProjectionViewer.TOGGLE);
          }
        }
      }
    }
  }

  private static boolean isBracket(char character) {
    for (int i = 0; i != BRACKETS.length; ++i) {
      if (character == BRACKETS[i]) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSurroundedByBrackets(IDocument document, int offset) {
    if (offset == 0 || offset == document.getLength()) {
      return false;
    }

    try {
      return isBracket(document.getChar(offset - 1)) && isBracket(document.getChar(offset));

    } catch (BadLocationException e) {
      return false;
    }
  }

  private IFile inputResourceFile;

  private File inputJavaFile;
  private volatile com.google.dart.engine.ast.CompilationUnit resolvedUnit;
  private SourceRange textSelectionRange;

  /** Preference key for matching brackets */
  protected final static String MATCHING_BRACKETS = PreferenceConstants.EDITOR_MATCHING_BRACKETS;

  /** Preference key for matching brackets color */
  protected final static String MATCHING_BRACKETS_COLOR = PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

  protected final static char[] BRACKETS = {'{', '}', '(', ')', '[', ']', '<', '>'};

  /** The outline page */
  protected DartOutlinePage fOutlinePage_OLD;

  /** The outline page */
  protected DartOutlinePage_NEW fOutlinePage_NEW;
  private Outline outline;

  private AnalysisServerOutlineListener analysisServerOutlineListener = new AnalysisServerOutlineListener() {
    @Override
    public void computedOutline(String file, final Outline outline) {
      DartEditor.this.outline = outline;
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          if (fOutlinePage_NEW != null) {
            int offset = getSourceViewer().getSelectedRange().x;
            fOutlinePage_NEW.setInput(outline, offset);
          }
        }
      });
    }
  };

  private SemanticHighlightingManager_NEW analysisServerHighlightManager;

  /** Outliner context menu Id */
  protected String fOutlinerContextMenuId;
  /**
   * The editor selection changed listener.
   */
  private EditorSelectionChangedListener fEditorSelectionChangedListener;
  /** The editor's bracket matcher */
  protected DartPairMatcher fBracketMatcher = new DartPairMatcher(BRACKETS);

  /** This editor's encoding support */
  private DefaultEncodingSupport fEncodingSupport;

  /** History for structure select action */
  private SelectionHistory fSelectionHistory;
  protected CompositeActionGroup fActionGroups;
  /**
   * The action group for folding.
   */
  private FoldingActionGroup fFoldingGroup;
  private CompositeActionGroup fOpenEditorActionGroup;

  /**
   * Removes trailing whitespace on editor saves.
   */
  private RemoveTrailingWhitespaceAction removeTrailingWhitespaceAction;

  /**
   * Holds the current occurrence annotations.
   */
  private Annotation[] fOccurrenceAnnotations = null;
  /**
   * Tells whether all occurrences of the element at the current caret location are automatically
   * marked in this editor.
   */
  private boolean fMarkOccurrenceAnnotations;
  /**
   * Tells whether the occurrence annotations are sticky i.e. whether they stay even if there's no
   * valid Dart element at the current caret position. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fStickyOccurrenceAnnotations;
  /**
   * Tells whether to mark type occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkTypeOccurrences;
  /**
   * Tells whether to mark method occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkMethodOccurrences;
  /**
   * Tells whether to mark constant occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkConstantOccurrences;
  /**
   * Tells whether to mark field occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkFieldOccurrences;

  /**
   * Tells whether to mark local variable occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkLocalVariableypeOccurrences;

  /**
   * Tells whether to mark exception occurrences in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkExceptions;
  /**
   * Tells whether to mark method exits in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkMethodExitPoints;
  /**
   * Tells whether to mark targets of <code>break</code> and <code>continue</code> statements in
   * this editor. Only valid if {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkBreakContinueTargets;
  /**
   * Tells whether to mark implementors in this editor. Only valid if
   * {@link #fMarkOccurrenceAnnotations} is <code>true</code>.
   */
  private boolean fMarkImplementors;

  /**
   * The selection used when forcing occurrence marking through code.
   */
  private ISelection fForcedMarkOccurrencesSelection;
  /**
   * The document modification stamp at the time when the last occurrence marking took place.
   */
  private long fMarkOccurrenceModificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
  /**
   * The region of the word under the caret used to when computing the current occurrence markings.
   */
  private IRegion fMarkOccurrenceTargetRegion;
  /**
   * The internal shell activation listener for updating occurrences.
   */
  private ActivationListener fActivationListener = new ActivationListener();
  private ISelectionChangedListener occurrencesResponder;
  private OccurrencesFinderJob fOccurrencesFinderJob;
  /** The occurrences finder job canceler */
  private OccurrencesFinderJobCanceler fOccurrencesFinderJobCanceler;
  /**
   * This editor's projection support
   */
  private ProjectionSupport fProjectionSupport;
  /**
   * This editor's projection model updater
   */
  private IDartFoldingStructureProvider fProjectionModelUpdater;

  /**
   * The override and implements indicator manager for this editor.
   */
  protected OverrideIndicatorManager fOverrideIndicatorManager;

  /**
   * Semantic highlighting manager
   */
  protected SemanticHighlightingManager fSemanticManager;

  /**
   * The folding runner.
   */
  private ToggleFoldingRunner fFoldingRunner;

  /**
   * Tells whether the selection changed event is caused by a call to
   * {@link #gotoAnnotation(boolean)}.
   */
  private boolean fSelectionChangedViaGotoAnnotation;

  /**
   * The cached selected range.
   * 
   * @see ITextViewer#getSelectedRange()
   */
  private Point fCachedSelectedRange;

  private ShowSelectionLabelAction showSelectionLabel = new ShowSelectionLabelAction();

  private SelectionProvider selectionProvider = new DartSelectionProvider();

  private final List<ISelectionChangedListener> dartSelectionListeners = Lists.newArrayList();

  private final CaretListener dartSelectionCaretListener = new CaretListener() {
    private boolean caretMovedScheduled = false;

    @Override
    public void caretMoved(CaretEvent event) {
      // already scheduled and not executed yet
      if (caretMovedScheduled) {
        return;
      }
      caretMovedScheduled = true;
      // schedule selection update
      Display.getCurrent().asyncExec(new Runnable() {
        @Override
        public void run() {
          caretMovedScheduled = false;
          if (isDisposed()) {
            return;
          }
          applySelectionToOutline();
          fireDartSelectionListeners();
        }
      });
    }
  };
  //Patched to address dartbug.com/7998
  private ISelectionChangedListener patchedSelectionChangedListener;

  /**
   * Default constructor.
   */
  public DartEditor() {
    super();
  }

  /**
   * Specifies that given {@link ISelectionChangedListener} should be invoked on any
   * {@link DartSelection} change - selection, {@link CompilationUnit}.
   */
  public void addDartSelectionListener(ISelectionChangedListener listener) {
    dartSelectionListeners.add(listener);
  }

  @Override
  public void applyResolvedUnit(com.google.dart.engine.ast.CompilationUnit unit) {
    if (isDisposed()) {
      return;
    }
    // ignore if already know that we don't have resolved unit
    if (resolvedUnit == null && unit == null) {
      return;
    }
    // ignore if this unit has already been set
    if (resolvedUnit == unit) {
      return;
    }
    // OK, schedule selection update
    resolvedUnit = unit;
    ExecutionUtils.runLogAsync(new RunnableEx() {
      @Override
      public void run() {
        if (isDisposed()) {
          return;
        }
        // update Outline
        if (resolvedUnit != null && fOutlinePage_OLD != null) {
          if (fOutlinePage_OLD != null) {
            fOutlinePage_OLD.setInput(resolvedUnit);
          }
          applySelectionToOutline();
        }
        // update selection listeners
        fireDartSelectionListeners();
        // update occurrences
        fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
        updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, resolvedUnit);
      }
    });
  }

  /**
   * Collapses all foldable comments if supported by the folding structure provider.
   */
  public void collapseComments() {
    DartX.todo("folding");
    if (fProjectionModelUpdater instanceof IDartFoldingStructureProviderExtension) {
      IDartFoldingStructureProviderExtension extension = (IDartFoldingStructureProviderExtension) fProjectionModelUpdater;
      extension.collapseComments();
    }
  }

  /**
   * Collapses all foldable members if supported by the folding structure provider.
   */
  public void collapseMembers() {
    DartX.todo("folding");
    if (fProjectionModelUpdater instanceof IDartFoldingStructureProviderExtension) {
      IDartFoldingStructureProviderExtension extension = (IDartFoldingStructureProviderExtension) fProjectionModelUpdater;
      extension.collapseMembers();
    }
  }

  public ISelection createElementSelection() {
    ITextSelection textSelection = (ITextSelection) getSelectionProvider().getSelection();
    IDocument document = getSourceViewer().getDocument();
    return textSelection;
  }

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    patchSelectionChangeParticipation();
    getSourceViewer().getTextWidget().addCaretListener(dartSelectionCaretListener);

    // Set tab width
    IPreferenceStore store = getPreferenceStore();
    if (store != null) {
      int tabWidth = store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
      getSourceViewer().getTextWidget().setTabs(tabWidth);
    }

    fEditorSelectionChangedListener = new EditorSelectionChangedListener();
    fEditorSelectionChangedListener.install(getSelectionProvider());

    if (isSemanticHighlightingEnabled()) {
      installSemanticHighlighting();
    }

    PlatformUI.getWorkbench().addWindowListener(fActivationListener);
  }

  @Override
  public void dispose() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
      String file = getInputFilePath();
      if (file != null) {
        analysisServerData.unsubscribeNavigation(file);
        analysisServerData.unsubscribeOutline(file, analysisServerOutlineListener);
      }
    }

    DartX.todo("folding");
    if (fProjectionModelUpdater != null) {
      fProjectionModelUpdater.uninstall();
      fProjectionModelUpdater = null;
    }

    if (fProjectionSupport != null) {
      fProjectionSupport.dispose();
      fProjectionSupport = null;
    }

    // cancel possible running computation
    fMarkOccurrenceAnnotations = false;
    uninstallOccurrencesFinder();

    uninstallOverrideIndicator();

    uninstallSemanticHighlighting();

    if (fActivationListener != null) {
      PlatformUI.getWorkbench().removeWindowListener(fActivationListener);
      fActivationListener = null;
    }

    if (fEncodingSupport != null) {
      fEncodingSupport.dispose();
      fEncodingSupport = null;
    }

    if (fBracketMatcher != null) {
      fBracketMatcher.dispose();
      fBracketMatcher = null;
    }

    if (fSelectionHistory != null) {
      fSelectionHistory.dispose();
      fSelectionHistory = null;
    }

    if (fEditorSelectionChangedListener != null) {
      fEditorSelectionChangedListener.uninstall(getSelectionProvider());
      fEditorSelectionChangedListener = null;
    }

    if (fActionGroups != null) {
      fActionGroups.dispose();
      fActionGroups = null;
    }

    selectionProvider = null;

    super.dispose();

    resolvedUnit = null;
  }

  /**
   * Run the editor format action.
   */
  public void doFormat() {
    getAction(DartEditorActionDefinitionIds.QUICK_FORMAT).run();
  }

  @Override
  public void editorContextMenuAboutToShow(IMenuManager menu) {
    menu.add(new Separator(ITextEditorActionConstants.GROUP_OPEN));
    menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
    menu.add(new Separator(ITextEditorActionConstants.GROUP_RESTORE));
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    ISelection selection = createElementSelection();

    // Open Declaration action
    ActionContext context = new ActionContext(selection);
    setContextMenuContext(menu, context); // This context contains a DartElementSelection for menus.

    // Quick Type Hierarchy

    if (selection instanceof DartSelection) {
      DartSelection dartSelection = (DartSelection) selection;
      if (ActionUtil.isOpenHierarchyAvailable(dartSelection)) {
        IAction action = getAction(DartEditorActionDefinitionIds.OPEN_HIERARCHY);
        menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
      }
    }

    // Quick Outline
    if ((selection instanceof TextSelection) && ((TextSelection) selection).getLength() == 0) {
      IAction action = getAction(DartEditorActionDefinitionIds.SHOW_OUTLINE);
      menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, action);
    }

    // Revert action
    addAction(
        menu,
        ITextEditorActionConstants.GROUP_RESTORE,
        ITextEditorActionConstants.REVERT_TO_SAVED);

    // Cut/Copy/Paste actions
    addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.UNDO);
    addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.CUT);
    addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.COPY);
    addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.PASTE);

    // Quick Assist
    {
      IAction action = getAction(ITextEditorActionConstants.QUICK_ASSIST);
      if (action != null && action.isEnabled()) {
        addAction(menu, RefactorActionGroup.GROUP_REORG, ITextEditorActionConstants.QUICK_ASSIST);
      }
    }

    // Format
    IAction action = getAction(DartEditorActionDefinitionIds.QUICK_FORMAT);
    if (action != null && action.isEnabled()) {
      addAction(menu, RefactorActionGroup.GROUP_REORG, DartEditorActionDefinitionIds.QUICK_FORMAT);
    }

    if (selection instanceof DartSelection) {
      DartSelection dartSelection = (DartSelection) selection;
      if (ActionUtil.hasItemsInGroup(menu, IContextMenuConstants.GROUP_OPEN)) {
        showSelectionLabel.update(dartSelection);
        showSelectionLabel.setEnabled(false);
        if (showSelectionLabel.getText() != null) {
          menu.prependToGroup(IContextMenuConstants.GROUP_OPEN, showSelectionLabel);
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class required) {

    DartX.todo("outline");
    if (IContentOutlinePage.class.equals(required)) {
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        if (fOutlinePage_NEW == null) {
          fOutlinePage_NEW = createOutlinePage_NEW();
        }
        return fOutlinePage_NEW;
      } else {
        if (fOutlinePage_OLD == null) {
          fOutlinePage_OLD = createOutlinePage_OLD();
        }
        return fOutlinePage_OLD;
      }
    }

    if (IEncodingSupport.class.equals(required)) {
      return fEncodingSupport;
    }

    if (required == IShowInTargetList.class) {
      return new IShowInTargetList() {
        @Override
        public String[] getShowInTargetIds() {
          String explorerViewID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
          // make sure the specified view ID is known
          if (PlatformUI.getWorkbench().getViewRegistry().find(explorerViewID) == null) {
            explorerViewID = IPageLayout.ID_PROJECT_EXPLORER;
          }
          return new String[] {explorerViewID, IPageLayout.ID_OUTLINE, IPageLayout.ID_RES_NAV};
        }

      };
    }

    DartX.todo("hover");
    if (required == IShowInSource.class) {
      return new IShowInSource() {
        @Override
        public ShowInContext getShowInContext() {
          return new ShowInContext(getEditorInput(), null) {
            @Override
            public ISelection getSelection() {
              DartElement je = null;
              try {
                je = SelectionConverter.getElementAtOffset(DartEditor.this);
                if (je == null) {
                  return null;
                }
                return new StructuredSelection(je);
              } catch (DartModelException ex) {
                return null;
              }
            }
          };
        }
      };
    }

    DartX.todo("folding");
    if (required == IDartFoldingStructureProvider.class) {
      return fProjectionModelUpdater;
    }

    if (fProjectionSupport != null) {
      Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), required);
      if (adapter != null) {
        return adapter;
      }
    }

    if (required == IContextProvider.class) {
      return DartUIHelp.getHelpContextProvider(this, DartHelpContextIds.JAVA_EDITOR);
    }

    return super.getAdapter(required);
  }

  /**
   * @return the {@link AssistContext} with resolved {@link CompilationUnit}, selection and
   *         {@link SearchEngine}. May be <code>null</code> if underlying {@link CompilationUnit} is
   *         not resolved.
   */
  public AssistContext getAssistContext() {
    ITextSelection textSelection = (ITextSelection) getSelectionProvider().getSelection();
    return getAssistContext(textSelection);
  }

  /**
   * Returns the cached selected range, which allows to query it from a non-UI thread.
   * <p>
   * The result might be outdated if queried from a non-UI thread.</em>
   * </p>
   * 
   * @return the caret offset in the master document
   * @see ITextViewer#getSelectedRange()
   */
  public Point getCachedSelectedRange() {
    return fCachedSelectedRange;
  }

  /**
   * Answer the {@link DartReconcilingStrategy} associated with this editor.
   * 
   * @return the strategy or {@code null} if none
   */
  public DartReconcilingStrategy getDartReconcilingStrategy() {
    return null;
  }

  public Point getDocumentSelectionRange() {
    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer == null) {
      return null;
    }

    StyledText styledText = sourceViewer.getTextWidget();
    if (styledText == null) {
      return null;
    }

    int widgetOffset = styledText.getCaretOffset();
    int widgetLength = styledText.getSelection().y;
    if (sourceViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
      int docOffset = extension.widgetOffset2ModelOffset(widgetOffset);
      return new Point(docOffset, widgetLength);
    } else {
      int offset = sourceViewer.getVisibleRegion().getOffset();
      int docOffset = offset + widgetOffset;
      return new Point(docOffset, widgetLength);
    }
  }

  @Override
  public AnalysisContext getInputAnalysisContext() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      throw new IllegalStateException("This method cannot be used with Analysis Server enabled.");
    }
    if (inputResourceFile != null) {
      return DartCore.getProjectManager().getContext(inputResourceFile);
    }
    if (inputJavaFile != null) {
      return DartCore.getProjectManager().getSdkContext();
    }
    return null;
  }

  @Override
  public String getInputAnalysisContextId() {
    if (inputResourceFile != null) {
      return DartCore.getProjectManager().getContextId(inputResourceFile);
    }
    if (inputJavaFile != null) {
      return DartCore.getProjectManager().getSdkContextId();
    }
    return null;
  }

  /**
   * Alternative to {@link #getInputDartElement()} that returns the Analysis engine
   * {@link com.google.dart.engine.element.Element} instead of a {@link DartElement}.
   */
  public com.google.dart.engine.element.CompilationUnitElement getInputElement() {
    com.google.dart.engine.ast.CompilationUnit unit = getInputUnit();
    return unit == null ? null : unit.getElement();
  }

  @Override
  public String getInputFilePath() {
    // may be workspace IFile
    if (inputResourceFile != null) {
      return inputResourceFile.getLocation().toOSString();
    }
    // may be Java file
    if (inputJavaFile != null) {
      return inputJavaFile.getAbsolutePath();
    }
    // TODO(scheglov) Analysis Server
    return null;
  }

  @Override
  public Project getInputProject() {
    if (inputResourceFile != null) {
      IProject resourceProject = inputResourceFile.getProject();
      return DartCore.getProjectManager().getProject(resourceProject);
    }
    return null;
  }

  /**
   * @return the input {@link IFile}, may be <code>null</code> if different kind of input.
   */
  public IFile getInputResourceFile() {
    return inputResourceFile;
  }

  @Override
  public Source getInputSource() {
    ProjectManager projectManager = DartCore.getProjectManager();
    // may be workspace IFile
    if (inputResourceFile != null) {
      return projectManager.getSource(inputResourceFile);
    }
    // may be SDK
    if (inputJavaFile != null) {
      // TODO(scheglov) Analysis Server: remove this?
//      AnalysisContext context = getInputAnalysisContext();
//      if (context == null) {
//        return null;
//      }
      DartSdk sdk = projectManager.getSdk();
      if (sdk instanceof DirectoryBasedDartSdk) {
        if (inputJavaFile.getAbsolutePath().startsWith(
            ((DirectoryBasedDartSdk) sdk).getDirectory().getAbsolutePath() + File.separator)) {
          return new FileBasedSource(inputJavaFile, UriKind.DART_URI);
        }
      }
      return new FileBasedSource(inputJavaFile);
    }
    // some random external file
    return null;
  }

  public com.google.dart.engine.ast.CompilationUnit getInputUnit() {
    return resolvedUnit;
  }

  @Override
  public int getOrientation() {
    return SWT.LEFT_TO_RIGHT; // Dart editors are always left to right by default
  }

  /**
   * Returns the latest known outline.
   */
  public Outline getOutline() {
    return outline;
  }

  public IPreferenceStore getPreferences() {
    return super.getPreferenceStore();
  }

  @Override
  public ISelectionProvider getSelectionProvider() {
    return selectionProvider;
  }

  /**
   * @return the last reported text selection range in underlying {@link #getSourceViewer()}. May be
   *         <code>null</code> if selection in not known yet. This method will not make any UI
   *         method invocation.
   */
  public SourceRange getTextSelectionRange() {
    return textSelectionRange;
  }

  @Override
  public String getTitleToolTip() {
    if (getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) getEditorInput();

      if (input.getFile().getLocation() != null) {
        return input.getFile().getLocation().toFile().toString();
      }
    }

    return super.getTitleToolTip();
  }

  public final ISourceViewer getViewer() {
    return getSourceViewer();
  }

  @Override
  public Object getViewPartInput() {
    return getEditorInput().getAdapter(DartElement.class);
  }

  @Override
  public Annotation gotoAnnotation(boolean forward) {
    fSelectionChangedViaGotoAnnotation = true;
    return super.gotoAnnotation(forward);
  }

  /**
   * Jumps to the matching bracket.
   */
  public void gotoMatchingBracket() {

    ISourceViewer sourceViewer = getSourceViewer();
    IDocument document = sourceViewer.getDocument();
    if (document == null) {
      return;
    }

    IRegion selection = getSignedSelection(sourceViewer);

    int selectionLength = Math.abs(selection.getLength());
    if (selectionLength > 1) {
      setStatusLineErrorMessage(DartEditorMessages.GotoMatchingBracket_error_invalidSelection);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    // #26314
    int sourceCaretOffset = selection.getOffset() + selection.getLength();
    if (isSurroundedByBrackets(document, sourceCaretOffset)) {
      sourceCaretOffset -= selection.getLength();
    }

    IRegion region = fBracketMatcher.match(document, sourceCaretOffset);
    if (region == null) {
      setStatusLineErrorMessage(DartEditorMessages.GotoMatchingBracket_error_noMatchingBracket);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    int offset = region.getOffset();
    int length = region.getLength();

    if (length < 1) {
      return;
    }

    int anchor = fBracketMatcher.getAnchor();
    // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
    int targetOffset = ICharacterPairMatcher.RIGHT == anchor ? offset + 1 : offset + length;

    boolean visible = false;
    if (sourceViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
      visible = extension.modelOffset2WidgetOffset(targetOffset) > -1;
    } else {
      IRegion visibleRegion = sourceViewer.getVisibleRegion();
      // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
      visible = targetOffset >= visibleRegion.getOffset()
          && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength();
    }

    if (!visible) {
      setStatusLineErrorMessage(DartEditorMessages.GotoMatchingBracket_error_bracketOutsideSelectedElement);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    if (selection.getLength() < 0) {
      targetOffset -= selection.getLength();
    }

    sourceViewer.setSelectedRange(targetOffset, selection.getLength());
    sourceViewer.revealRange(targetOffset, selection.getLength());
  }

  @Override
  public boolean isDirty() {
    return isContentEditable() ? super.isDirty() : false;
  }

  @Override
  public boolean isEditable() {
    return isContentEditable() ? super.isEditable() : false;
  }

  @Override
  public boolean isEditorInputModifiable() {
    return isContentEditable() ? super.isEditorInputModifiable() : false;
  }

  @Override
  public boolean isEditorInputReadOnly() {
    return isContentEditable() ? super.isEditorInputReadOnly() : true;
  }

  /**
   * @return {@code true} if the editor's content is visible
   */
  @Override
  public boolean isVisible() {
    ISourceViewer viewer = getViewer();
    if (viewer != null) {
      StyledText widget = viewer.getTextWidget();
      if (widget != null) {
        return widget.isVisible();
      }
    }
    return false;
  }

  /**
   * Informs the editor that its outliner has been closed.
   */
  public void outlinePageClosed() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (fOutlinePage_NEW != null) {
        fOutlinePage_NEW = null;
        resetHighlightRange();
      }
    } else {
      if (fOutlinePage_OLD != null) {
        fOutlinePage_OLD = null;
        resetHighlightRange();
      }
    }
  }

  public void removeDartSelectionListener(ISelectionChangedListener listener) {
    dartSelectionListeners.remove(listener);
  }

  /**
   * Resets the foldings structure according to the folding preferences.
   */
  public void resetProjection() {
    DartX.todo("folding");
    if (fProjectionModelUpdater != null) {
      fProjectionModelUpdater.initialize();
    }
  }

  public void selectEndReveal(com.google.dart.engine.element.Element element) {
    if (element == null || element instanceof CompilationUnitElement) {
      return;
    }

    markInNavigationHistory();

    int offset = element.getNameOffset();
    int length = element.getDisplayName().length();
    selectAndReveal(offset, length);
  }

  public void setPreferences(IPreferenceStore store) {
    uninstallSemanticHighlighting();
    super.setPreferenceStore(store);
    installSemanticHighlighting();
  }

  public void setSelection_NEW(Outline outline, boolean moveCursor) {
    // validate Outline
    if (outline == null) {
      return;
    }
    // prepare range
    int offset = outline.getElement().getOffset();
    int length = outline.getElement().getLength();
    // prepare ISourceViewer
    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer == null) {
      return;
    }
    // highlight range (not selection - just highlighting on left editor band)
    if (offset < 0) {
      return;
    }
    setHighlightRange(offset, length, moveCursor);
    // do we want to change selection?
    if (!moveCursor) {
      return;
    }
    // prepare StyledText
    StyledText textWidget = sourceViewer.getTextWidget();
    if (textWidget == null) {
      return;
    }
    // set selection in StyledText
    try {
      textWidget.setRedraw(false);
      sourceViewer.revealRange(offset, length);
      sourceViewer.setSelectedRange(offset, length);
      fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
      updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, getInputUnit());
    } finally {
      textWidget.setRedraw(true);
    }
  }

  public void setSelection_OLD(LightNodeElement element, boolean moveCursor) {
    // validate LightNodeElement
    if (element == null) {
      return;
    }
    // prepare range
    int offset = element.getNameOffset();
    int length = element.getNameLength();
    // prepare ISourceViewer
    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer == null) {
      return;
    }
    // highlight range (not selection - just highlighting on left editor band)
    if (offset < 0) {
      return;
    }
    setHighlightRange(offset, length, moveCursor);
    // do we want to change selection?
    if (!moveCursor) {
      return;
    }
    // prepare StyledText
    StyledText textWidget = sourceViewer.getTextWidget();
    if (textWidget == null) {
      return;
    }
    // set selection in StyledText
    try {
      textWidget.setRedraw(false);
      sourceViewer.revealRange(offset, length);
      sourceViewer.setSelectedRange(offset, length);
      fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
      updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, getInputUnit());
    } finally {
      textWidget.setRedraw(true);
    }
  }

  /**
   * Synchronizes the outliner selection with the actual cursor position in the editor.
   */
  public void synchronizeOutlinePageSelection() {

    // TODO(scheglov)
//      synchronizeOutlinePage(computeHighlightRangeSourceElement());
  }

  public void updatedTitleImage(Image image) {
    setTitleImage(image);
  }

  @Override
  protected void adjustHighlightRange(int offset, int length) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (fOutlinePage_NEW != null) {
        fOutlinePage_NEW.select(offset);
      }
    } else {
      Object element = getElementAt(offset, false);

      if (element instanceof LightNodeElement) {
        LightNodeElement sourceElement = (LightNodeElement) element;
        int elementOffset = sourceElement.getNameOffset();
        int elementLength = sourceElement.getNameLength();

        ISourceViewer viewer = getSourceViewer();
        if (viewer instanceof ITextViewerExtension5) {
          ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
          extension.exposeModelRange(new Region(elementOffset, elementLength));
        }

        setHighlightRange(elementOffset, elementLength, true);
        if (fOutlinePage_OLD != null) {
          fOutlinePage_OLD.select((LightNodeElement) element);
        }
      }
    }

    ISourceViewer viewer = getSourceViewer();
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      extension.exposeModelRange(new Region(offset, length));
    } else {
      resetHighlightRange();
    }

  }

  /**
   * Determines whether the preference change encoded by the given event changes the override
   * indication.
   * 
   * @param event the event to be investigated
   * @return <code>true</code> if event causes a change
   */
  protected boolean affectsOverrideIndicatorAnnotations(PropertyChangeEvent event) {
    String key = event.getProperty();
    AnnotationPreference preference = getAnnotationPreferenceLookup().getAnnotationPreference(
        OverrideIndicatorManager.ANNOTATION_TYPE);
    if (key == null || preference == null) {
      return false;
    }

    return key.equals(preference.getHighlightPreferenceKey())
        || key.equals(preference.getVerticalRulerPreferenceKey())
        || key.equals(preference.getOverviewRulerPreferenceKey())
        || key.equals(preference.getTextPreferenceKey());
  }

  @Override
  protected boolean affectsTextPresentation(PropertyChangeEvent event) {
    return ((DartSourceViewerConfiguration) getSourceViewerConfiguration()).affectsTextPresentation(event)
        || super.affectsTextPresentation(event);
  }

  protected void checkEditableState() {
//    isEditableStateKnown = false;
  }

  @Override
  protected String[] collectContextMenuPreferencePages() {
    String[] inheritedPages = super.collectContextMenuPreferencePages();
    int length = 10;
    String[] result = new String[inheritedPages.length + length];
    result[0] = "com.google.dart.tools.ui.internal.preferences.JavaEditorPreferencePage"; //$NON-NLS-1$
    result[1] = "com.google.dart.tools.ui.internal.preferences.JavaTemplatePreferencePage"; //$NON-NLS-1$
    result[2] = "com.google.dart.tools.ui.internal.preferences.CodeAssistPreferencePage"; //$NON-NLS-1$
    result[3] = "com.google.dart.tools.ui.internal.preferences.CodeAssistPreferenceAdvanced"; //$NON-NLS-1$
    result[4] = "com.google.dart.tools.ui.internal.preferences.JavaEditorHoverPreferencePage"; //$NON-NLS-1$
    result[5] = "com.google.dart.tools.ui.internal.preferences.JavaEditorColoringPreferencePage"; //$NON-NLS-1$
    result[6] = "com.google.dart.tools.ui.internal.preferences.FoldingPreferencePage"; //$NON-NLS-1$
    result[7] = "com.google.dart.tools.ui.internal.preferences.MarkOccurrencesPreferencePage"; //$NON-NLS-1$
    result[8] = "com.google.dart.tools.ui.internal.preferences.SmartTypingPreferencePage"; //$NON-NLS-1$
    result[9] = "com.google.dart.tools.ui.internal.preferences.SaveParticipantPreferencePage"; //$NON-NLS-1$
    System.arraycopy(inheritedPages, 0, result, length, inheritedPages.length);
    return result;
  }

  /**
   * Computes and returns the source element that includes the caret and serves as provider for the
   * outline page selection and the editor range indication.
   * <p>
   * to replace {@link #computeHighlightRangeSourceReference()}.
   * 
   * @return the computed source element
   */
  protected LightNodeElement computeHighlightRangeSourceElement(
      com.google.dart.engine.ast.CompilationUnit unit, int caret) {
    if (unit == null) {
      return null;
    }

//    ISourceViewer sourceViewer = getSourceViewer();
//    if (sourceViewer == null) {
//      return null;
//    }
//
//    StyledText styledText = sourceViewer.getTextWidget();
//    if (styledText == null) {
//      return null;
//    }
//
//    int caret = 0;
//    if (sourceViewer instanceof ITextViewerExtension5) {
//      ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
//      caret = extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
//    } else {
//      int offset = sourceViewer.getVisibleRegion().getOffset();
//      caret = offset + styledText.getCaretOffset();
//    }

    AstNode node = new NodeLocator(caret).searchWithin(unit);

    // May be whitespace between class declaration {}, try to find class member.
    if (node instanceof ClassDeclaration) {
      ClassDeclaration classDeclaration = (ClassDeclaration) node;
      if (classDeclaration.getLeftBracket().getOffset() + 1 < caret
          && caret < classDeclaration.getRightBracket().getOffset()) {
        for (ClassMember member : classDeclaration.getMembers()) {
          if (caret < member.getOffset()) {
            node = member;
            break;
          }
        }
      }
    }

    // May be unit whitespace, try to find unit member.
    if (node instanceof com.google.dart.engine.ast.CompilationUnit) {
      NodeList<CompilationUnitMember> declarations = ((com.google.dart.engine.ast.CompilationUnit) node).getDeclarations();
      for (CompilationUnitMember member : declarations) {
        if (caret < member.getOffset()) {
          node = member;
          break;
        }
      }
    }

    return LightNodeElements.createLightNodeElement(inputResourceFile, node);
  }

  /**
   * Computes and returns the source reference that includes the caret and serves as provider for
   * the outline page selection and the editor range indication.
   * 
   * @return the computed source reference
   */
  protected SourceReference computeHighlightRangeSourceReference() {
    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer == null) {
      return null;
    }

    StyledText styledText = sourceViewer.getTextWidget();
    if (styledText == null) {
      return null;
    }

    int caret = 0;
    if (sourceViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
      caret = extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
    } else {
      int offset = sourceViewer.getVisibleRegion().getOffset();
      caret = offset + styledText.getCaretOffset();
    }

    Object element = getElementAt(caret, false);

    if (!(element instanceof SourceReference)) {
      return null;
    }

    return (SourceReference) element;
  }

  @Override
  protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {

    support.setCharacterPairMatcher(fBracketMatcher);
    support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);

    super.configureSourceViewerDecorationSupport(support);
  }

  @Override
  protected void createActions() {
    installEncodingSupport();

    super.createActions();

    DartX.todo("actions");
    ActionGroup oeg, ovg;
    ActionGroup dsg, ddg;
    fActionGroups = new CompositeActionGroup(new ActionGroup[] {
        oeg = new OpenEditorActionGroup(this), ovg = new OpenViewActionGroup(this),
        dsg = new DartSearchActionGroup(this), ddg = new DartdocActionGroup(this)});
    fOpenEditorActionGroup = new CompositeActionGroup(new ActionGroup[] {ovg, oeg, dsg, ddg});

    // Registers the folding actions with the editor
    fFoldingGroup = new FoldingActionGroup(this, getViewer());

    Action action = new GotoMatchingBracketAction(this);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);
    setAction(GotoMatchingBracketAction.GOTO_MATCHING_BRACKET, action);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "ShowOutline.", this, DartSourceViewer.SHOW_OUTLINE, true); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.SHOW_OUTLINE);
    setAction(DartEditorActionDefinitionIds.SHOW_OUTLINE, action);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.SHOW_OUTLINE_ACTION);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "OpenStructure.", this, DartSourceViewer.OPEN_STRUCTURE, true); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_STRUCTURE);
    setAction(DartEditorActionDefinitionIds.OPEN_STRUCTURE, action);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.OPEN_STRUCTURE_ACTION);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "OpenHierarchy.", this, DartSourceViewer.SHOW_HIERARCHY, true); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.OPEN_HIERARCHY);
    setAction(DartEditorActionDefinitionIds.OPEN_HIERARCHY, action);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.OPEN_HIERARCHY_ACTION);

    fSelectionHistory = new SelectionHistory(this);

    action = new StructureSelectEnclosingAction(this, fSelectionHistory);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_ENCLOSING);
    setAction(StructureSelectionAction.ENCLOSING, action);

    action = new StructureSelectNextAction(this, fSelectionHistory);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_NEXT);
    setAction(StructureSelectionAction.NEXT, action);

    action = new StructureSelectPreviousAction(this, fSelectionHistory);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_PREVIOUS);
    setAction(StructureSelectionAction.PREVIOUS, action);

    StructureSelectHistoryAction historyAction = new StructureSelectHistoryAction(
        this,
        fSelectionHistory);
    historyAction.setActionDefinitionId(DartEditorActionDefinitionIds.SELECT_LAST);
    setAction(StructureSelectionAction.HISTORY, historyAction);
    fSelectionHistory.setHistoryAction(historyAction);

    action = GoToNextPreviousMemberAction.newGoToNextMemberAction(this);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
    setAction(GoToNextPreviousMemberAction.NEXT_MEMBER, action);

    action = GoToNextPreviousMemberAction.newGoToPreviousMemberAction(this);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);
    setAction(GoToNextPreviousMemberAction.PREVIOUS_MEMBER, action);

    action = new FormatElementAction();
    action.setActionDefinitionId(DartEditorActionDefinitionIds.QUICK_FORMAT);
    setAction(DartEditorActionDefinitionIds.QUICK_FORMAT, action);
    markAsStateDependentAction(DartEditorActionDefinitionIds.QUICK_FORMAT, true);

    action = new RemoveOccurrenceAnnotations(this);
    action.setActionDefinitionId(DartEditorActionDefinitionIds.REMOVE_OCCURRENCE_ANNOTATIONS);
    setAction("RemoveOccurrenceAnnotations", action); //$NON-NLS-1$

    // add annotation actions for roll-over expand hover
    action = new DartSelectMarkerRulerAction2(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Editor.RulerAnnotationSelection.", this); //$NON-NLS-1$
    setAction("AnnotationAction", action); //$NON-NLS-1$

    DartX.todo();
    // action = new ShowInPackageViewAction(this);
    // action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_PACKAGE_VIEW);
    //    setAction("ShowInPackageView", action); //$NON-NLS-1$

    // replace cut/copy paste actions with a version that implement 'add imports
    // on paste'

    action = new ClipboardOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Editor.Cut.", this, ITextOperationTarget.CUT); //$NON-NLS-1$
    setAction(ITextEditorActionConstants.CUT, action);

    action = new ClipboardOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Editor.Copy.", this, ITextOperationTarget.COPY); //$NON-NLS-1$
    setAction(ITextEditorActionConstants.COPY, action);

    action = new ClipboardOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Editor.Paste.", this, ITextOperationTarget.PASTE); //$NON-NLS-1$
    setAction(ITextEditorActionConstants.PASTE, action);

    DartX.todo();
    // action = new CopyQualifiedNameAction(this);
    // setAction(IDartEditorActionConstants.COPY_QUALIFIED_NAME, action);

    removeTrailingWhitespaceAction = new RemoveTrailingWhitespaceAction(getSourceViewer());
  }

  @Override
  protected IVerticalRulerColumn createAnnotationRulerColumn(CompositeRuler ruler) {
    DartX.todo();
    return super.createAnnotationRulerColumn(ruler);
    // if (!getPreferenceStore().getBoolean(
    // PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER))
    // return super.createAnnotationRulerColumn(ruler);
    //
    // AnnotationRulerColumn column = new AnnotationRulerColumn(
    // VERTICAL_RULER_WIDTH, getAnnotationAccess());
    // column.setHover(new JavaExpandHover(ruler, getAnnotationAccess(),
    // new IDoubleClickListener() {
    //
    // public void doubleClick(DoubleClickEvent event) {
    // // for now: just invoke ruler double click action
    // triggerAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK);
    // }
    //
    // private void triggerAction(String actionID) {
    // IAction action = getAction(actionID);
    // if (action != null) {
    // if (action instanceof IUpdate)
    // ((IUpdate) action).update();
    // // hack to propagate line change
    // if (action instanceof ISelectionListener) {
    // ((ISelectionListener) action).selectionChanged(null, null);
    // }
    // if (action.isEnabled())
    // action.run();
    // }
    // }
    //
    // }));
    //
    // return column;
  }

  @Override
  protected LineChangeHover createChangeHover() {
    return new DartChangeHover(DartPartitions.DART_PARTITIONING, getOrientation());
  }

  protected ISourceViewer createDartSourceViewer(Composite parent, IVerticalRuler verticalRuler,
      IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles,
      IPreferenceStore store) {
    return new DartSourceViewer(
        parent,
        verticalRuler,
        getOverviewRuler(),
        isOverviewRulerVisible(),
        styles,
        store);
  }

  /**
   * Returns a new Dart source viewer configuration.
   * 
   * @return a new <code>DartSourceViewerConfiguration</code>
   */
  protected DartSourceViewerConfiguration createDartSourceViewerConfiguration() {
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    return new DartSourceViewerConfiguration(
        textTools.getColorManager(),
        getPreferenceStore(),
        this,
        DartPartitions.DART_PARTITIONING);
  }

  @Override
  protected void createNavigationActions() {
    super.createNavigationActions();

    final StyledText textWidget = getSourceViewer().getTextWidget();

    IAction action = new SmartLineStartAction(textWidget, false);
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_START);
    setAction(ITextEditorActionDefinitionIds.LINE_START, action);

    action = new SmartLineStartAction(textWidget, true);
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_START);
    setAction(ITextEditorActionDefinitionIds.SELECT_LINE_START, action);

    action = new NavigatePreviousSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_PREVIOUS);
    setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

    action = new NavigateNextSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_NEXT);
    setAction(ITextEditorActionDefinitionIds.WORD_NEXT, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

    action = new SelectPreviousSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
    setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);

    action = new SelectNextSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
    setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);
  }

  /**
   * Creates the outline page used with this editor.
   * 
   * @return the created Dart outline page
   */
  protected DartOutlinePage_NEW createOutlinePage_NEW() {
    DartOutlinePage_NEW page = new DartOutlinePage_NEW(fOutlinerContextMenuId, this);
    setOutlinePageInput_NEW(page, getEditorInput());
    return page;
  }

  /**
   * Creates the outline page used with this editor.
   * 
   * @return the created Dart outline page
   */
  protected DartOutlinePage createOutlinePage_OLD() {
    DartOutlinePage page = new DartOutlinePage(fOutlinerContextMenuId, this);
    setOutlinePageInput_OLD(page, getEditorInput());
    return page;
  }

  @Override
  protected final ISourceViewer createSourceViewer(Composite parent, IVerticalRuler verticalRuler,
      int styles) {

    IPreferenceStore store = getPreferenceStore();
    final ISourceViewer viewer = createDartSourceViewer(
        parent,
        verticalRuler,
        getOverviewRuler(),
        isOverviewRulerVisible(),
        styles,
        store);

    DartUIHelp.setHelp(this, viewer.getTextWidget(), DartHelpContextIds.JAVA_EDITOR);

    DartSourceViewer dartSourceViewer = null;
    if (viewer instanceof DartSourceViewer) {
      dartSourceViewer = (DartSourceViewer) viewer;
    }

    /*
     * This is a performance optimization to reduce the computation of the text presentation
     * triggered by {@link #setVisibleDocument(IDocument)}
     */
    if (dartSourceViewer != null && isFoldingEnabled()
        && (store == null || !store.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS))) {
      dartSourceViewer.prepareDelayedProjection();
    }

    ProjectionViewer projectionViewer = (ProjectionViewer) viewer;
    fProjectionSupport = new ProjectionSupport(
        projectionViewer,
        getAnnotationAccess(),
        getSharedColors());
    fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
    fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$

    DartX.todo("hover");
    fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell shell) {
        return new SourceViewerInformationControl(
            shell,
            SWT.TOOL | SWT.NO_TRIM | getOrientation(),
            SWT.NONE,
            EditorsUI.getTooltipAffordanceString());
      }
    });
    fProjectionSupport.setInformationPresenterControlCreator(new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell shell) {
        int shellStyle = SWT.RESIZE | SWT.TOOL | getOrientation();
        int style = SWT.V_SCROLL | SWT.H_SCROLL;
        return new SourceViewerInformationControl(shell, shellStyle, style);
      }
    });
    fProjectionSupport.install();

    fProjectionModelUpdater = DartToolsPlugin.getDefault().getFoldingStructureProviderRegistry().getCurrentFoldingProvider();
    if (fProjectionModelUpdater != null) {
      fProjectionModelUpdater.install(this, projectionViewer);
    }

    // ensure source viewer decoration support has been created and configured
    getSourceViewerDecorationSupport(viewer);

    // track text selection range
    viewer.getTextWidget().addCaretListener(new CaretListener() {
      @Override
      public void caretMoved(CaretEvent event) {
        textSelectionRange = SourceRangeFactory.forStartLength(event.caretOffset, 0);
      }
    });
    viewer.getTextWidget().addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        textSelectionRange = SourceRangeFactory.forStartEnd(event.x, event.y);
      }
    });

    EditorUtility.addGTKPasteHack(viewer);

    return viewer;
  }

  protected void doSelectionChanged(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for (Object selectionObject : structuredSelection.toList()) {
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          if (selectionObject instanceof Outline) {
            setSelection_NEW((Outline) selectionObject, !isActivePart());
            break;
          }
        } else {
          if (selectionObject instanceof LightNodeElement) {
            setSelection_OLD((LightNodeElement) selectionObject, !isActivePart());
            break;
          }
        }
      }
    }
  }

  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    AnalysisContext sdkContext = DartCore.getProjectManager().getSdkContext();
    if (input instanceof IFileEditorInput) {
      IFileEditorInput fileInput = (IFileEditorInput) input;
      inputResourceFile = fileInput.getFile();
    } else if (input instanceof IURIEditorInput) {
      IURIEditorInput uriInput = (IURIEditorInput) input;
      URI uri = uriInput.getURI();
      if (uri != null && uri.getScheme().equals("file") && uri.isAbsolute()) {
        try {
          inputJavaFile = new File(uri);
        } catch (Throwable e) {
        }
      }
    }

    // TODO(scheglov) This code is intentionally left commented.
    // If we don't follow the "!(sourceViewer instanceof ISourceViewerExtension2)",
    // it causes https://code.google.com/p/dart/issues/detail?id=18297
    // To reproduce it, open editor, rename input file and try to use completion or Quick Assist.
    // So, we just do it always.
    {
      setPreferenceStore(createCombinedPreferenceStore(input));
      internalDoSetInput(input);
      return;
    }

//    ISourceViewer sourceViewer = getSourceViewer();
//    if (!(sourceViewer instanceof ISourceViewerExtension2)) {
//      setPreferenceStore(createCombinedPreferenceStore(input));
//      internalDoSetInput(input);
//      return;
//    }
//
//    // uninstall & unregister preference store listener
//    getSourceViewerDecorationSupport(sourceViewer).uninstall();
//    ((ISourceViewerExtension2) sourceViewer).unconfigure();
//
//    setPreferenceStore(createCombinedPreferenceStore(input));
//
//    // install & register preference store listener
//    sourceViewer.configure(getSourceViewerConfiguration());
//    getSourceViewerDecorationSupport(sourceViewer).install(getPreferenceStore());
//
//    internalDoSetInput(input);
  }

  @Override
  protected void doSetSelection(ISelection selection) {
    super.doSetSelection(selection);
    synchronizeOutlinePageSelection();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overrides the default implementation to handle {@link IJavaAnnotation}.
   * </p>
   * 
   * @param offset the region offset
   * @param length the region length
   * @param forward <code>true</code> for forwards, <code>false</code> for backward
   * @param annotationPosition the position of the found annotation
   * @return the found annotation
   */
  @Override
  protected Annotation findAnnotation(final int offset, final int length, boolean forward,
      Position annotationPosition) {

    Annotation nextAnnotation = null;
    Position nextAnnotationPosition = null;
    Annotation containingAnnotation = null;
    Position containingAnnotationPosition = null;
    boolean currentAnnotation = false;

    IDocument document = getDocumentProvider().getDocument(getEditorInput());
    int endOfDocument = document.getLength();
    int distance = Integer.MAX_VALUE;

    IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
    @SuppressWarnings("rawtypes")
    Iterator e = new DartAnnotationIterator(model, true, true);
    while (e.hasNext()) {
      Annotation a = (Annotation) e.next();
      if (a instanceof IJavaAnnotation && ((IJavaAnnotation) a).hasOverlay()
          || !isNavigationTarget(a)) {
        continue;
      }

      Position p = model.getPosition(a);
      if (p == null) {
        continue;
      }

      if (forward && p.offset == offset || !forward && p.offset + p.getLength() == offset + length) {// ||
                                                                                                     // p.includes(offset))
                                                                                                     // {
        if (containingAnnotation == null || forward
            && p.length >= containingAnnotationPosition.length || !forward
            && p.length >= containingAnnotationPosition.length) {
          containingAnnotation = a;
          containingAnnotationPosition = p;
          currentAnnotation = p.length == length;
        }
      } else {
        int currentDistance = 0;

        if (forward) {
          currentDistance = p.getOffset() - offset;
          if (currentDistance < 0) {
            currentDistance = endOfDocument + currentDistance;
          }

          if (currentDistance < distance || currentDistance == distance
              && p.length < nextAnnotationPosition.length) {
            distance = currentDistance;
            nextAnnotation = a;
            nextAnnotationPosition = p;
          }
        } else {
          currentDistance = offset + length - (p.getOffset() + p.length);
          if (currentDistance < 0) {
            currentDistance = endOfDocument + currentDistance;
          }

          if (currentDistance < distance || currentDistance == distance
              && p.length < nextAnnotationPosition.length) {
            distance = currentDistance;
            nextAnnotation = a;
            nextAnnotationPosition = p;
          }
        }
      }
    }
    if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
      annotationPosition.setOffset(containingAnnotationPosition.getOffset());
      annotationPosition.setLength(containingAnnotationPosition.getLength());
      return containingAnnotation;
    }
    if (nextAnnotationPosition != null) {
      annotationPosition.setOffset(nextAnnotationPosition.getOffset());
      annotationPosition.setLength(nextAnnotationPosition.getLength());
    }

    return nextAnnotation;
  }

  /**
   * Returns the standard action group of this editor.
   * 
   * @return returns this editor's standard action group
   */
  protected ActionGroup getActionGroup() {
    return fActionGroups;
  }

  /**
   * Returns the Dart element of this editor's input corresponding to the given DartElement.
   * 
   * @param element the Dart element
   * @return the corresponding Dart element
   */
  abstract protected DartElement getCorrespondingElement(DartElement element);

  /**
   * Returns the most narrow Dart element including the given offset.
   * 
   * @param offset the offset inside of the requested element
   * @return the most narrow Dart element
   */
  abstract protected Object /*Element*/getElementAt(int offset);

  /**
   * Returns the most narrow Dart element including the given offset.
   * 
   * @param offset the offset inside of the requested element
   * @param reconcile <code>true</code> if editor input should be reconciled in advance
   * @return the most narrow element
   */
  protected Object /*Element*/getElementAt(int offset, boolean reconcile) {
    return getElementAt(offset);
  }

  /**
   * Returns the folding action group, or <code>null</code> if there is none.
   * 
   * @return the folding action group, or <code>null</code> if there is none
   */
  protected FoldingActionGroup getFoldingActionGroup() {
    return fFoldingGroup;
  }

  /**
   * Returns the Dart element wrapped by this editors input.
   * 
   * @return the Dart element wrapped by this editors input.
   */
  protected DartElement getInputDartElement() {
    return EditorUtility.getEditorInputDartElement(this, false);
  }

  protected ISelectionChangedListener getPatchedSelectionChangedListener() {

    if (patchedSelectionChangedListener == null) {
      patchedSelectionChangedListener = new ISelectionChangedListener() {

        private Runnable runnable = new Runnable() {
          @Override
          public void run() {
            if (isDisposed()) {
              return;
            }
            updateSelectionDependentActions();
          }
        };

        private Display display;

        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          Display current = Display.getCurrent();
          if (current != null) {
            // Don't execute asynchronously if we're in a thread that has a display.
            // Fix for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=368354 (the rationale
            // is that the actions were not being enabled because they were previously
            // updated in an async call).
            // but just patching getSelectionChangedListener() properly.
            runnable.run();
          } else {
            if (display == null) {
              display = getSite().getShell().getDisplay();
            }
            display.asyncExec(runnable);
          }
          handleCursorPositionChanged();
        }
      };
    }

    return patchedSelectionChangedListener;
  }

  /**
   * Returns the signed current selection. The length will be negative if the resulting selection is
   * right-to-left (RtoL).
   * <p>
   * The selection offset is model based.
   * </p>
   * 
   * @param sourceViewer the source viewer
   * @return a region denoting the current signed selection, for a resulting RtoL selections length
   *         is < 0
   */
  protected IRegion getSignedSelection(ISourceViewer sourceViewer) {
    StyledText text = sourceViewer.getTextWidget();
    Point selection = text.getSelectionRange();

    if (text.getCaretOffset() == selection.x) {
      selection.x = selection.x + selection.y;
      selection.y = -selection.y;
    }

    selection.x = widgetOffset2ModelOffset(sourceViewer, selection.x);

    return new Region(selection.x, selection.y);
  }

  @Override
  protected String getStatusBanner(IStatus status) {
    if (fEncodingSupport != null) {
      String message = fEncodingSupport.getStatusBanner(status);
      if (message != null) {
        return message;
      }
    }
    return super.getStatusBanner(status);
  }

  @Override
  protected String getStatusHeader(IStatus status) {
    if (fEncodingSupport != null) {
      String message = fEncodingSupport.getStatusHeader(status);
      if (message != null) {
        return message;
      }
    }
    return super.getStatusHeader(status);
  }

  @Override
  protected String getStatusMessage(IStatus status) {
    if (fEncodingSupport != null) {
      String message = fEncodingSupport.getStatusMessage(status);
      if (message != null) {
        return message;
      }
    }
    return super.getStatusMessage(status);
  }

  @Override
  protected IOperationApprover getUndoRedoOperationApprover(IUndoContext undoContext) {
    return new NonLocalUndoUserApprover(
        undoContext,
        this,
        new Object[] {inputResourceFile},
        IResource.class);
  }

  @Override
  protected void handleCursorPositionChanged() {
    super.handleCursorPositionChanged();
    fCachedSelectedRange = getViewer().getSelectedRange();
  }

  @Override
  protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

    String property = event.getProperty();

    try {

      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer == null) {
        return;
      }

      if (AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
        IPreferenceStore store = getPreferenceStore();
        if (store != null) {
          sourceViewer.getTextWidget().setTabs(
              store.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
        }
        if (isTabsToSpacesConversionEnabled()) {
          uninstallTabsToSpacesConverter();
          installTabsToSpacesConverter();
        }
        return;
      }

      if (isEditorHoverProperty(property)) {
        updateHoverBehavior();
      }

      boolean newBooleanValue = false;
      Object newValue = event.getNewValue();
      if (newValue != null) {
        newBooleanValue = Boolean.valueOf(newValue.toString()).booleanValue();
      }

      if (PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE.equals(property)) {
        if (newBooleanValue) {
          selectionChanged();
        }
        return;
      }

      if (PreferenceConstants.EDITOR_MARK_OCCURRENCES.equals(property)) {
        if (newBooleanValue != fMarkOccurrenceAnnotations) {
          fMarkOccurrenceAnnotations = newBooleanValue;
          if (!fMarkOccurrenceAnnotations) {
            uninstallOccurrencesFinder();
          } else {
            installOccurrencesFinder(true);
          }
        }
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES.equals(property)) {
        fMarkTypeOccurrences = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES.equals(property)) {
        fMarkMethodOccurrences = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES.equals(property)) {
        fMarkConstantOccurrences = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES.equals(property)) {
        fMarkFieldOccurrences = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES.equals(property)) {
        fMarkLocalVariableypeOccurrences = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES.equals(property)) {
        fMarkExceptions = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS.equals(property)) {
        fMarkMethodExitPoints = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS.equals(property)) {
        fMarkBreakContinueTargets = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_MARK_IMPLEMENTORS.equals(property)) {
        fMarkImplementors = newBooleanValue;
        return;
      }
      if (PreferenceConstants.EDITOR_STICKY_OCCURRENCES.equals(property)) {
        fStickyOccurrenceAnnotations = newBooleanValue;
        return;
      }
      if (SemanticHighlightings.affectsEnablement(getPreferenceStore(), event)) {
        if (isSemanticHighlightingEnabled()) {
          installSemanticHighlighting();
        } else {
          uninstallSemanticHighlighting();
        }
        return;
      }

      ((DartSourceViewerConfiguration) getSourceViewerConfiguration()).handlePropertyChangeEvent(event);

      if (affectsOverrideIndicatorAnnotations(event)) {
        if (isShowingOverrideIndicators()) {
          if (fOverrideIndicatorManager == null) {
            installOverrideIndicator(true);
          }
        } else {
          if (fOverrideIndicatorManager != null) {
            uninstallOverrideIndicator();
          }
        }
        return;
      }

      if (PreferenceConstants.EDITOR_FOLDING_PROVIDER.equals(property)) {
        if (sourceViewer instanceof ProjectionViewer) {
          DartX.todo("folding");
          ProjectionViewer projectionViewer = (ProjectionViewer) sourceViewer;
          if (fProjectionModelUpdater != null) {
            fProjectionModelUpdater.uninstall();
          }
          // either freshly enabled or provider changed
          fProjectionModelUpdater = DartToolsPlugin.getDefault().getFoldingStructureProviderRegistry().getCurrentFoldingProvider();
          if (fProjectionModelUpdater != null) {
            fProjectionModelUpdater.install(this, projectionViewer);
          }
        }
        return;
      }

      if (DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE.equals(property)
          || DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE.equals(property)
          || DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR.equals(property)) {
        StyledText textWidget = sourceViewer.getTextWidget();
        int tabWidth = getSourceViewerConfiguration().getTabWidth(sourceViewer);
        if (textWidget.getTabs() != tabWidth) {
          textWidget.setTabs(tabWidth);
        }
        return;
      }

      if (PreferenceConstants.EDITOR_FOLDING_ENABLED.equals(property)) {
        if (sourceViewer instanceof ProjectionViewer) {
          new ToggleFoldingRunner().runWhenNextVisible();
        }
        return;
      }

    } finally {
      super.handlePreferenceStoreChanged(event);
    }

    if (AbstractDecoratedTextEditorPreferenceConstants.SHOW_RANGE_INDICATOR.equals(property)) {
      // superclass already installed the range indicator
      Object newValue = event.getNewValue();
      ISourceViewer viewer = getSourceViewer();
      if (newValue != null && viewer != null) {
        if (Boolean.valueOf(newValue.toString()).booleanValue()) {
          // adjust the highlightrange in order to get the magnet right after
          // changing the selection
          Point selection = viewer.getSelectedRange();
          adjustHighlightRange(selection.x, selection.y);
        }
      }

    }
  }

  @Override
  protected void initializeDragAndDrop(ISourceViewer viewer) {
    // Disabled in Dart Editor.
//    super.initializeDragAndDrop(viewer);
  }

  @Override
  protected void initializeEditor() {
    IPreferenceStore store = createCombinedPreferenceStore(null);
    setPreferenceStore(store);
    setSourceViewerConfiguration(createDartSourceViewerConfiguration());
    fMarkOccurrenceAnnotations = store.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
    fStickyOccurrenceAnnotations = store.getBoolean(PreferenceConstants.EDITOR_STICKY_OCCURRENCES);
    fMarkTypeOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_TYPE_OCCURRENCES);
    fMarkMethodOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_METHOD_OCCURRENCES);
    fMarkConstantOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_CONSTANT_OCCURRENCES);
    fMarkFieldOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_FIELD_OCCURRENCES);
    fMarkLocalVariableypeOccurrences = store.getBoolean(PreferenceConstants.EDITOR_MARK_LOCAL_VARIABLE_OCCURRENCES);
    fMarkExceptions = store.getBoolean(PreferenceConstants.EDITOR_MARK_EXCEPTION_OCCURRENCES);
    fMarkImplementors = store.getBoolean(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS);
    fMarkMethodExitPoints = store.getBoolean(PreferenceConstants.EDITOR_MARK_METHOD_EXIT_POINTS);
    fMarkBreakContinueTargets = store.getBoolean(PreferenceConstants.EDITOR_MARK_BREAK_CONTINUE_TARGETS);
  }

  @Override
  protected void initializeKeyBindingScopes() {
    setKeyBindingScopes(new String[] {"com.google.dart.tools.ui.dartViewScope"}); //$NON-NLS-1$
  }

  /**
   * Initializes the given viewer's colors.
   * 
   * @param viewer the viewer to be initialized
   */
  @Override
  protected void initializeViewerColors(ISourceViewer viewer) {
    // is handled by DartSourceViewer
  }

  /**
   * Installs the encoding support on the given text editor.
   * <p>
   * Subclasses may override to install their own encoding support or to disable the default
   * encoding support.
   * </p>
   */
  protected void installEncodingSupport() {
    fEncodingSupport = new DefaultEncodingSupport();
    fEncodingSupport.initialize(this);
  }

  protected void installOccurrencesFinder(boolean forceUpdate) {
    fMarkOccurrenceAnnotations = true;

    occurrencesResponder = new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof ITextSelection) {
          fForcedMarkOccurrencesSelection = selection;
          updateOccurrenceAnnotations((ITextSelection) selection, getInputUnit());
        }
      }
    };
    getSelectionProvider().addSelectionChangedListener(occurrencesResponder);
    if (forceUpdate && getSelectionProvider() != null) {
      fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
      com.google.dart.engine.ast.CompilationUnit unit = getInputUnit();
      if (unit != null) {
        updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, unit);
      }
    }

    if (fOccurrencesFinderJobCanceler == null) {
      fOccurrencesFinderJobCanceler = new OccurrencesFinderJobCanceler();
      fOccurrencesFinderJobCanceler.install();
    }
  }

  protected void installOverrideIndicator(boolean provideAST) {
    uninstallOverrideIndicator();
    IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
    final com.google.dart.engine.ast.CompilationUnit cu = getInputUnit();
    if (model == null) {
      return;
    }
    fOverrideIndicatorManager = new OverrideIndicatorManager(model, cu);
    fOverrideIndicatorManager.install(this);
  }

  protected boolean isActivePart() {
    IWorkbenchPart part = getActivePart();
    return part != null && part.equals(this);
  }

  protected boolean isMarkingOccurrences() {
    IPreferenceStore store = getPreferenceStore();
    return store != null && store.getBoolean(PreferenceConstants.EDITOR_MARK_OCCURRENCES);
  }

  /**
   * Tells whether override indicators are shown.
   * 
   * @return <code>true</code> if the override indicators are shown
   */
  protected boolean isShowingOverrideIndicators() {
    AnnotationPreference preference = getAnnotationPreferenceLookup().getAnnotationPreference(
        OverrideIndicatorManager.ANNOTATION_TYPE);
    IPreferenceStore store = getPreferenceStore();
    return getBoolean(store, preference.getHighlightPreferenceKey())
        || getBoolean(store, preference.getVerticalRulerPreferenceKey())
        || getBoolean(store, preference.getOverviewRulerPreferenceKey())
        || getBoolean(store, preference.getTextPreferenceKey());
  }

  @Override
  protected boolean isTabsToSpacesConversionEnabled() {
    return getPreferenceStore() != null
        && getPreferenceStore().getBoolean(
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
  }

  @Override
  protected void performRevert() {
    ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
    projectionViewer.setRedraw(false);
    try {

      boolean projectionMode = projectionViewer.isProjectionMode();
      if (projectionMode) {
        projectionViewer.disableProjection();
        DartX.todo("folding");
        if (fProjectionModelUpdater != null) {
          fProjectionModelUpdater.uninstall();
        }
      }

      super.performRevert();

      if (projectionMode) {
        DartX.todo("folding");
        if (fProjectionModelUpdater != null) {
          fProjectionModelUpdater.install(this, projectionViewer);
        }
        projectionViewer.enableProjection();
      }

    } finally {
      projectionViewer.setRedraw(true);
      checkEditableState();
    }
  }

  @Override
  protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("Editor-Save-Perf");
    try {

      performSaveActions();

      instrumentation.metric("Save-Actions", "complete");

      super.performSave(overwrite, progressMonitor);

      instrumentation.metric("Save", "complete");

      int lines = getDocumentProvider().getDocument(getEditorInput()).getNumberOfLines();
      instrumentation.metric("Lines", lines);

    } finally {
      instrumentation.log();
    }
  }

  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
    super.rulerContextMenuAboutToShow(menu);

    // Need to remove preferences item from the list; can't supress it via activities
    // because there is no command defined for this item
    menu.remove(ITextEditorActionConstants.RULER_PREFERENCES);

    IAction action = getAction("FoldingToggle"); //$NON-NLS-1$

    menu.add(action);
    action = getAction("FoldingExpandAll"); //$NON-NLS-1$

    menu.add(action);
    action = getAction("FoldingCollapseAll"); //$NON-NLS-1$

    menu.add(action);
    action = getAction("FoldingRestore"); //$NON-NLS-1$
    menu.add(action);
  }

  /**
   * React to changed selection.
   */
  protected void selectionChanged() {

    if (getSelectionProvider() == null) {
      return;
    }

    fForcedMarkOccurrencesSelection = getSelectionProvider().getSelection();
    updateOccurrenceAnnotations((ITextSelection) fForcedMarkOccurrencesSelection, getInputUnit());
    // TODO(scheglov)
    LightNodeElement element = computeHighlightRangeSourceElement(
        resolvedUnit,
        ((ITextSelection) fForcedMarkOccurrencesSelection).getOffset());
//      if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE)) {
//        synchronizeOutlinePage(element);
//      }
    setSelectionRange(element, false);

    if (!fSelectionChangedViaGotoAnnotation) {
      updateStatusLine();
    }
    fSelectionChangedViaGotoAnnotation = false;
  }

  protected void setContextMenuContext(IMenuManager menu, ActionContext context) {
    fOpenEditorActionGroup.setContext(context);
    fOpenEditorActionGroup.fillContextMenu(menu);
    fOpenEditorActionGroup.setContext(null);
  }

  /**
   * Sets the input of the editor's outline page.
   * 
   * @param page the Dart outline page
   * @param input the editor input
   */
  protected void setOutlinePageInput_NEW(DartOutlinePage_NEW page, IEditorInput input) {
    if (page == null) {
      return;
    }
    AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
    String file = getInputFilePath();
    if (file != null) {
      analysisServerData.subscribeOutline(file, analysisServerOutlineListener);
    }
  }

  /**
   * Sets the input of the editor's outline page.
   * 
   * @param page the Dart outline page
   * @param input the editor input
   */
  protected void setOutlinePageInput_OLD(DartOutlinePage page, IEditorInput input) {
    if (page == null) {
      return;
    }
    page.setInput(resolvedUnit);
  }

  /**
   * Sets the outliner's context menu ID.
   * 
   * @param menuId the menu ID
   */
  protected void setOutlinerContextMenuId(String menuId) {
    fOutlinerContextMenuId = menuId;
  }

  @Override
  protected void setPreferenceStore(IPreferenceStore store) {
    super.setPreferenceStore(store);
    if (getSourceViewerConfiguration() instanceof DartSourceViewerConfiguration) {
      DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
      setSourceViewerConfiguration(new DartSourceViewerConfiguration(
          textTools.getColorManager(),
          store,
          this,
          DartPartitions.DART_PARTITIONING));
    }
    if (getSourceViewer() instanceof DartSourceViewer) {
      ((DartSourceViewer) getSourceViewer()).setPreferenceStore(store);
    }
  }

  protected void setSelection(SourceReference reference, boolean moveCursor) {
    // NOTUSED
    if (getSelectionProvider() == null) {
      return;
    }

    ISelection selection = getSelectionProvider().getSelection();
    if (selection instanceof ITextSelection) {
      ITextSelection textSelection = (ITextSelection) selection;
      // PR 39995: [navigation] Forward history cleared after going back in
      // navigation history:
      // mark only in navigation history if the cursor is being moved (which it
      // isn't if
      // this is called from a PostSelectionEvent that should only update the
      // magnet)
      if (moveCursor && (textSelection.getOffset() != 0 || textSelection.getLength() != 0)) {
        markInNavigationHistory();
      }
    }

    if (reference != null) {

      StyledText textWidget = null;

      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        textWidget = sourceViewer.getTextWidget();
      }

      if (textWidget == null) {
        return;
      }

      try {
        SourceRange range = reference.getSourceRange();

        if (range == null) {
          return;
        }

        int offset = range.getOffset();
        int length = range.getLength();

        if (offset < 0 || length < 0) {
          return;
        }

        setHighlightRange(offset, length, moveCursor);

        if (!moveCursor) {
          return;
        }

        offset = -1;
        length = -1;

        range = reference.getNameRange();
        if (range != null) {
          offset = range.getOffset();
          length = range.getLength();
        }

        if (offset > -1 && length > 0) {

          try {
            textWidget.setRedraw(false);
            sourceViewer.revealRange(offset, length);
            sourceViewer.setSelectedRange(offset, length);
          } finally {
            textWidget.setRedraw(true);
          }

          markInNavigationHistory();
        }

      } catch (DartModelException x) {
      } catch (IllegalArgumentException x) {
      }

    } else if (moveCursor) {
      resetHighlightRange();
      markInNavigationHistory();
    }
  }

  protected void setSelectionRange(LightNodeElement reference, boolean moveCursor) {
    if (getSelectionProvider() == null) {
      return;
    }
    ISelection selection = getSelectionProvider().getSelection();
    if (selection instanceof ITextSelection) {
      ITextSelection textSelection = (ITextSelection) selection;
      // PR 39995: [navigation] Forward history cleared after going back in navigation history:
      // mark only in navigation history if the cursor is being moved (which it isn't if
      // this is called from a PostSelectionEvent that should only update the magnet)
      if (moveCursor && (textSelection.getOffset() != 0 || textSelection.getLength() != 0)) {
        markInNavigationHistory();
      }
    }
    if (reference != null) {
      StyledText textWidget = null;
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        textWidget = sourceViewer.getTextWidget();
      }
      if (textWidget == null) {
        return;
      }
      try {
        int offset = reference.getNode().getOffset();
        int length = reference.getNode().getLength();
        if (offset < 0 || length < 0) {
          return;
        }
        setHighlightRange(offset, length, moveCursor);
        if (!moveCursor) {
          return;
        }
        offset = -1;
        length = -1;
        offset = reference.getNameOffset();
        length = reference.getNameLength();
        if (offset > -1 && length > 0) {
          try {
            textWidget.setRedraw(false);
            sourceViewer.revealRange(offset, length);
            sourceViewer.setSelectedRange(offset, length);
          } finally {
            textWidget.setRedraw(true);
          }
          markInNavigationHistory();
        }
      } catch (IllegalArgumentException x) {
      }
    } else if (moveCursor) {
      resetHighlightRange();
      markInNavigationHistory();
    }
  }

  /**
   * Synchronizes the outliner selection with the given element position in the editor.
   * 
   * @param element the dart element to select
   */
  protected void synchronizeOutlinePage(Object /* Element */element) {
    synchronizeOutlinePage(element, true);
  }

  /**
   * Synchronizes the outliner selection with the given element position in the editor.
   * 
   * @param element the Dart element to select
   * @param checkIfOutlinePageActive <code>true</code> if check for active outline page needs to be
   *          done
   */
  protected void synchronizeOutlinePage(Object /* ASTNode */element,
      boolean checkIfOutlinePageActive) {
    if (fOutlinePage_OLD != null && element != null
        && !(checkIfOutlinePageActive && isOutlinePageActive())) {
      // TODO(scheglov)
//        ((DartOutlinePage) fOutlinePage).select((LightNodeElement) element);
//        ((DartOutlinePage) fOutlinePage).setInput(
//            (com.google.dart.engine.ast.CompilationUnit) ((LightNodeElement) element).getNode().getRoot(),
//            (LightNodeElement) element);
    }
  }

  protected void uninstallOccurrencesFinder() {
    fMarkOccurrenceAnnotations = false;

    if (fOccurrencesFinderJob != null) {
      fOccurrencesFinderJob.cancel();
      fOccurrencesFinderJob = null;
    }

    if (fOccurrencesFinderJobCanceler != null) {
      fOccurrencesFinderJobCanceler.uninstall();
      fOccurrencesFinderJobCanceler = null;
    }

    if (occurrencesResponder != null) {
      getSelectionProvider().removeSelectionChangedListener(occurrencesResponder);
      occurrencesResponder = null;
    }
    removeOccurrenceAnnotations();
  }

  protected void uninstallOverrideIndicator() {
    if (fOverrideIndicatorManager != null) {
      fOverrideIndicatorManager.removeAnnotations();
      fOverrideIndicatorManager.uninstall();
      fOverrideIndicatorManager = null;
    }
  }

  @Override
  protected void updateMarkerViews(Annotation annotation) {
    if (annotation instanceof IJavaAnnotation) {
      Iterator<IJavaAnnotation> e = ((IJavaAnnotation) annotation).getOverlaidIterator();
      if (e != null) {
        while (e.hasNext()) {
          Object o = e.next();
          if (o instanceof MarkerAnnotation) {
            super.updateMarkerViews((MarkerAnnotation) o);
            return;
          }
        }
      }
      return;
    }
    super.updateMarkerViews(annotation);
  }

  /**
   * Updates the occurrences annotations based on the current selection.
   * 
   * @param selection the text selection
   * @param astRoot the compilation unit AST
   */
  protected void updateOccurrenceAnnotations(ITextSelection selection,
      com.google.dart.engine.ast.CompilationUnit unit) {

    if (fOccurrencesFinderJob != null) {
      fOccurrencesFinderJob.cancel();
    }

    if (!fMarkOccurrenceAnnotations) {
      return;
    }

    if (unit == null || selection == null) {
      return;
    }

    IDocument document = getSourceViewer().getDocument();
    if (document == null) {
      return;
    }

    if (document instanceof IDocumentExtension4) {
      int offset = selection.getOffset();
      long currentModificationStamp = ((IDocumentExtension4) document).getModificationStamp();
      IRegion markOccurrenceTargetRegion = fMarkOccurrenceTargetRegion;
      if (markOccurrenceTargetRegion != null
          && currentModificationStamp == fMarkOccurrenceModificationStamp) {
        if (markOccurrenceTargetRegion.getOffset() <= offset
            && offset <= markOccurrenceTargetRegion.getOffset()
                + markOccurrenceTargetRegion.getLength()) {
          if (selection.getLength() > 0
              && selection.getLength() != fMarkOccurrenceTargetRegion.getLength()) {
            removeOccurrenceAnnotations();
          }
          return;
        }
      }
      fMarkOccurrenceTargetRegion = DartWordFinder.findWord(document, offset);
      fMarkOccurrenceModificationStamp = currentModificationStamp;
      if (selection.getLength() > 0
          && selection.getLength() != fMarkOccurrenceTargetRegion.getLength()) {
        removeOccurrenceAnnotations();
        return;
      }
    }

    DartX.todo("marking");
    Collection<AstNode> matches = null;

    NodeLocator locator = new NodeLocator(selection.getOffset(), selection.getOffset()
        + selection.getLength());
    AstNode selectedNode = locator.searchWithin(unit);

//    try {
//      if (astRoot.getLibrary() == null) {
//        // if astRoot is from ExternalCompilationUnit then it needs to be resolved; it is apparently not cached
//        astRoot = DartCompilerUtilities.resolveUnit(input); // TODO clean up astRoot
//      }
//    } catch (DartModelException e) {
//      DartToolsPlugin.log(e);
//    }
//    /* DartElement selectedModelNode = */locator.searchWithin(astRoot);
//    Element selectedNode = locator.getResolvedElement();

//    if (fMarkExceptions || fMarkTypeOccurrences) {
//      ExceptionOccurrencesFinder exceptionFinder = new ExceptionOccurrencesFinder();
//      String message = exceptionFinder.initialize(astRoot, selectedNode);
//      if (message == null) {
//        matches = exceptionFinder.perform();
//        if (!fMarkExceptions && !matches.isEmpty())
//          matches.clear();
//      }
//    }

//    if ((matches == null || matches.isEmpty())
//        && (fMarkMethodExitPoints || fMarkTypeOccurrences)) {
//      MethodExitsFinder finder = new MethodExitsFinder();
//      String message = finder.initialize(astRoot, selectedNode);
//      if (message == null) {
//        matches = finder.perform();
//        if (!fMarkMethodExitPoints && !matches.isEmpty())
//          matches.clear();
//      }
//    }

//    if ((matches == null || matches.isEmpty())
//        && (fMarkBreakContinueTargets || fMarkTypeOccurrences)) {
//      BreakContinueTargetFinder finder = new BreakContinueTargetFinder();
//      String message = finder.initialize(astRoot, selectedNode);
//      if (message == null) {
//        matches = finder.perform();
//        if (!fMarkBreakContinueTargets && !matches.isEmpty())
//          matches.clear();
//      }
//    }

//    if ((matches == null || matches.isEmpty())
//        && (fMarkImplementors || fMarkTypeOccurrences)) {
//      ImplementOccurrencesFinder finder = new ImplementOccurrencesFinder();
//      String message = finder.initialize(astRoot, selectedNode);
//      if (message == null) {
//        matches = finder.perform();
//        if (!fMarkImplementors && !matches.isEmpty())
//          matches.clear();
//      }
//    }

//    if (matches == null) {
//      IBinding binding = null;
//      if (selectedNode instanceof Name) {
//        binding = ((Name) selectedNode).resolveBinding();
//      }
//
//      if (binding != null && markOccurrencesOfType(binding)) {
//        // Find the matches && extract positions so we can forget the AST
//        NameOccurrencesFinder finder = new NameOccurrencesFinder(binding);
//        String message = finder.initialize(astRoot, selectedNode);
//        if (message == null) {
//          matches = finder.perform();
//        }
//      }
//    }

    if (matches == null && selectedNode != null) {
      if (selectedNode instanceof SimpleIdentifier) {
        SimpleIdentifier ident = (SimpleIdentifier) selectedNode;
        matches = com.google.dart.engine.services.util.NameOccurrencesFinder.findIn(ident, unit);
      }
    }
    if (matches == null || matches.size() == 0) {
      if (!fStickyOccurrenceAnnotations) {
        removeOccurrenceAnnotations();
      }
      return;
    }

    Position[] positions = new Position[matches.size()];
    int i = 0;
    for (Iterator<AstNode> each = matches.iterator(); each.hasNext();) {
      AstNode currentNode = each.next();
      positions[i++] = new Position(currentNode.getOffset(), currentNode.getLength());
    }

    fOccurrencesFinderJob = new OccurrencesFinderJob(document, positions, selection);
//      fOccurrencesFinderJob.setPriority(Job.DECORATE);
//      fOccurrencesFinderJob.setSystem(true);
//      fOccurrencesFinderJob.schedule();
    fOccurrencesFinderJob.run(new NullProgressMonitor());
  }

  @Override
  protected void updatePropertyDependentActions() {
    super.updatePropertyDependentActions();
    if (fEncodingSupport != null) {
      fEncodingSupport.reset();
    }
  }

  protected void updateStatusLine() {
    ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
    Annotation annotation = getAnnotation(selection.getOffset(), selection.getLength());
    setStatusLineErrorMessage(null);
    setStatusLineMessage(null);
    if (annotation != null) {
      updateMarkerViews(annotation);
      if (annotation instanceof IJavaAnnotation && ((IJavaAnnotation) annotation).isProblem()) {
        setStatusLineMessage(annotation.getText());
      }
    }
  }

  boolean isFoldingEnabled() {
    return DartToolsPlugin.getDefault().getPreferenceStore().getBoolean(
        PreferenceConstants.EDITOR_FOLDING_ENABLED);
  }

  boolean markOccurrencesOfType(/* IBinding */Object binding) {
    DartX.todo("modifiers,marking");
    if (binding == null) {
      return false;
    }

//    int kind = binding.getKind();
//
//    if (fMarkTypeOccurrences && kind == IBinding.TYPE)
//      return true;
//
//    if (fMarkMethodOccurrences && kind == IBinding.METHOD)
//      return true;
//
//    if (kind == IBinding.VARIABLE) {
//      IVariableBinding variableBinding = (IVariableBinding) binding;
//      if (variableBinding.isField()) {
//        int constantModifier = IModifierConstants.ACC_STATIC
//            | IModifierConstants.ACC_FINAL;
//        boolean isConstant = (variableBinding.getModifiers() & constantModifier) == constantModifier;
//        if (isConstant)
//          return fMarkConstantOccurrences;
//        else
//          return fMarkFieldOccurrences;
//      }
//
//      return fMarkLocalVariableypeOccurrences;
//    }

    return false;
  }

  void removeOccurrenceAnnotations() {
    fMarkOccurrenceModificationStamp = IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
    fMarkOccurrenceTargetRegion = null;

    IDocumentProvider documentProvider = getDocumentProvider();
    if (documentProvider == null) {
      return;
    }

    IAnnotationModel annotationModel = documentProvider.getAnnotationModel(getEditorInput());
    if (annotationModel == null || fOccurrenceAnnotations == null) {
      return;
    }

    synchronized (getLockObject(annotationModel)) {
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
            fOccurrenceAnnotations,
            null);
      } else {
        for (int i = 0, length = fOccurrenceAnnotations.length; i < length; i++) {
          annotationModel.removeAnnotation(fOccurrenceAnnotations[i]);
        }
      }
      fOccurrenceAnnotations = null;
    }
  }

  /**
   * Applies the current selection to the Outline view.
   */
  private void applySelectionToOutline() {
    // prepare selection
    ISelection selection = getSelectionProvider().getSelection();
    if (!(selection instanceof TextSelection)) {
      return;
    }
    int offset = ((TextSelection) selection).getOffset();
    // apply selected element
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (fOutlinePage_NEW != null) {
        fOutlinePage_NEW.select(offset);
      }
    } else {
      // prepare resolved unit
      com.google.dart.engine.ast.CompilationUnit unit = resolvedUnit;
      if (unit == null) {
        return;
      }
      // apply selected element
      LightNodeElement element = computeHighlightRangeSourceElement(resolvedUnit, offset);
      if (element != null && fOutlinePage_OLD != null) {
        fOutlinePage_OLD.select(element);
      }
    }
  }

  /**
   * Creates and returns the preference store for this Dart editor with the given input.
   * 
   * @param input The editor input for which to create the preference store
   * @return the preference store for this editor
   */
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

  /**
   * Notifies all listeners of {@link DartSelection}.
   */
  private void fireDartSelectionListeners() {
    ISelection selection = getSelectionProvider().getSelection();
    SelectionChangedEvent event = new SelectionChangedEvent(selectionProvider, selection);
    for (ISelectionChangedListener listener : ImmutableList.copyOf(dartSelectionListeners)) {
      listener.selectionChanged(event);
    }
  }

  private IWorkbenchPart getActivePart() {
    IWorkbenchWindow window = getSite().getWorkbenchWindow();
    IPartService service = window.getPartService();
    IWorkbenchPart part = service.getActivePart();
    return part;
  }

  /**
   * Returns the annotation overlapping with the given range or <code>null</code>.
   * 
   * @param offset the region offset
   * @param length the region length
   * @return the found annotation or <code>null</code>
   */
  private Annotation getAnnotation(int offset, int length) {
    IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
    @SuppressWarnings("rawtypes")
    Iterator e = new DartAnnotationIterator(model, true, false);
    while (e.hasNext()) {
      Annotation a = (Annotation) e.next();
      Position p = model.getPosition(a);
      if (p != null && p.overlapsWith(offset, length)) {
        return a;
      }
    }
    return null;
  }

  /**
   * @return the {@link AssistContext} with resolved {@link CompilationUnit}, selection and
   *         {@link SearchEngine}. May be <code>null</code>.
   */
  private AssistContext getAssistContext(ITextSelection textSelection) {
    try {
      Source source = getInputSource();
      if (source == null) {
        return null;
      }
      // prepare selection
      if (textSelection == null) {
        return null;
      }
      int selectionOffset = 0;
      int selectionLength = 0;
      {
        selectionOffset = textSelection.getOffset();
        selectionLength = textSelection.getLength();
      }
      // TODO(scheglov) Analysis Server
      if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
        String file = getInputFilePath();
        if (file == null) {
          return null;
        }
        return new AssistContext(
            null/*SearchEngineFactory.createSearchEngine(index)*/,
            null,
            file,
            source,
            null,
            selectionOffset,
            selectionLength);
      }
      // prepare AnalysisContext
      AnalysisContext analysisContext = getInputAnalysisContext();
      if (analysisContext == null) {
        return null;
      }
      // prepare input CompilationUnit
      com.google.dart.engine.ast.CompilationUnit unit = getInputUnit();
      if (unit == null) {
        return null;
      }
      if (unit.getElement() == null) {
        return null;
      }
      // return AssistContext
      Index index = DartCore.getProjectManager().getIndex();
      return new AssistContext(
          SearchEngineFactory.createSearchEngine(index),
          analysisContext,
          null,
          source,
          unit,
          selectionOffset,
          selectionLength);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  /**
   * Returns the boolean preference for the given key.
   * 
   * @param store the preference store
   * @param key the preference key
   * @return <code>true</code> if the key exists in the store and its value is <code>true</code>
   */
  private boolean getBoolean(IPreferenceStore store, String key) {
    return key != null && store.getBoolean(key);
  }

  /**
   * Returns the lock object for the given annotation model.
   * 
   * @param annotationModel the annotation model
   * @return the annotation model's lock object
   */
  private Object getLockObject(IAnnotationModel annotationModel) {
    if (annotationModel instanceof ISynchronizable) {
      Object lock = ((ISynchronizable) annotationModel).getLockObject();
      if (lock != null) {
        return lock;
      }
    }
    return annotationModel;
  }

  /**
   * Install Semantic Highlighting.
   */
  private void installSemanticHighlighting() {
    // TODO(scheglov) Analysis Server: remove this old method
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
      String file = getInputFilePath();
      if (file != null) {
        analysisServerHighlightManager = new SemanticHighlightingManager_NEW(
            (DartSourceViewer) getSourceViewer(),
            file);
      }
      return;
    }
    if (fSemanticManager == null) {
      fSemanticManager = new SemanticHighlightingManager();
      fSemanticManager.install(
          this,
          (DartSourceViewer) getSourceViewer(),
          DartToolsPlugin.getDefault().getDartTextTools().getColorManager(),
          getPreferenceStore());
    }
  }

  private void internalDoSetInput(IEditorInput input) throws CoreException {
    ISourceViewer sourceViewer = getSourceViewer();
    DartSourceViewer dartSourceViewer = null;
    if (sourceViewer instanceof DartSourceViewer) {
      dartSourceViewer = (DartSourceViewer) sourceViewer;
    }

    IPreferenceStore store = getPreferenceStore();
    if (dartSourceViewer != null && isFoldingEnabled()
        && (store == null || !store.getBoolean(PreferenceConstants.EDITOR_SHOW_SEGMENTS))) {
      dartSourceViewer.prepareDelayedProjection();
    }

    super.doSetInput(input);

    if (dartSourceViewer != null && dartSourceViewer.getReconciler() == null) {
      IReconciler reconciler = getSourceViewerConfiguration().getReconciler(dartSourceViewer);
      if (reconciler != null) {
        reconciler.install(dartSourceViewer);
        dartSourceViewer.setReconciler(reconciler);
      }
    }

    if (fEncodingSupport != null) {
      fEncodingSupport.reset();
    }

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      AnalysisServerData analysisServerData = DartCore.getAnalysisServerData();
      String file = getInputFilePath();
      if (file != null) {
        analysisServerData.subscribeNavigation(file);
      }
    }

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      setOutlinePageInput_NEW(fOutlinePage_NEW, input);
    } else {
      setOutlinePageInput_OLD(fOutlinePage_OLD, input);
    }

    if (isShowingOverrideIndicators()) {
      installOverrideIndicator(false);
    }
  }

  /*
   * Tells whether the content is editable.
   */
  private boolean isContentEditable() {
    return inputResourceFile != null && !DartCore.isContainedInPackages(inputResourceFile)
        && !inputResourceFile.isReadOnly();
  }

  /**
   * @return {@code true} if this {@link DartEditor} was already disposed.
   */
  private boolean isDisposed() {
    return getSourceViewer() == null;
  }

  private boolean isEditorHoverProperty(String property) {
    return PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS.equals(property);
  }

  private boolean isOutlinePageActive() {
    IWorkbenchPart part = getActivePart();
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      return part instanceof ContentOutline
          && ((ContentOutline) part).getCurrentPage() == fOutlinePage_NEW;
    } else {
      return part instanceof ContentOutline
          && ((ContentOutline) part).getCurrentPage() == fOutlinePage_OLD;
    }
  }

  private boolean isRemoveTrailingWhitespaceEnabled() {
    return PreferenceConstants.getPreferenceStore().getBoolean(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS);
  }

  /**
   * @return <code>true</code> if Semantic Highlighting is enabled.
   */
  private boolean isSemanticHighlightingEnabled() {
    return true;
//    return SemanticHighlightings.isEnabled(getPreferenceStore());
  }

  private void patchSelectionChangeParticipation() {
    // To address dartbug.com/7998
    //TODO (pquitslund): revisit when we move to 3.8 and see if this hack is still needed
    getSelectionProvider().removeSelectionChangedListener(getSelectionChangedListener());
    getSelectionProvider().addSelectionChangedListener(getPatchedSelectionChangedListener());
  }

  private void performSaveActions() {
    if (isRemoveTrailingWhitespaceEnabled()) {
      try {
        removeTrailingWhitespaceAction.run();
      } catch (InvocationTargetException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  /**
   * Uninstall Semantic Highlighting.
   */
  private void uninstallSemanticHighlighting() {
    // TODO(scheglov) Analysis Server: remove this old method
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (analysisServerHighlightManager != null) {
        analysisServerHighlightManager.dispose();
        analysisServerHighlightManager = null;
      }
      return;
    }
    if (fSemanticManager != null) {
      fSemanticManager.uninstall();
      fSemanticManager = null;
    }
  }

  /*
   * Update the hovering behavior depending on the preferences.
   */
  private void updateHoverBehavior() {
    SourceViewerConfiguration configuration = getSourceViewerConfiguration();
    String[] types = configuration.getConfiguredContentTypes(getSourceViewer());

    for (int i = 0; i < types.length; i++) {

      String t = types[i];

      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer instanceof ITextViewerExtension2) {
        // Remove existing hovers
        ((ITextViewerExtension2) sourceViewer).removeTextHovers(t);

        int[] stateMasks = configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

        if (stateMasks != null) {
          for (int j = 0; j < stateMasks.length; j++) {
            int stateMask = stateMasks[j];
            ITextHover textHover = configuration.getTextHover(sourceViewer, t, stateMask);
            ((ITextViewerExtension2) sourceViewer).setTextHover(textHover, t, stateMask);
          }
        } else {
          ITextHover textHover = configuration.getTextHover(sourceViewer, t);
          ((ITextViewerExtension2) sourceViewer).setTextHover(
              textHover,
              t,
              ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
        }
      } else {
        sourceViewer.setTextHover(configuration.getTextHover(sourceViewer, t), t);
      }
    }
  }
}
