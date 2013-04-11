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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.util.IDartDebugValue;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.objectinspector.ExpressionEvaluateJob.ExpressionListener;
import com.google.dart.tools.debug.ui.internal.presentation.DartDebugModelPresentation;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.actions.InstrumentedAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartDocumentSetupParticipant;
import com.google.dart.tools.ui.internal.text.functions.PreferencesAdapter;
import com.google.dart.tools.ui.internal.util.SelectionUtil;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;
import com.google.dart.tools.ui.text.DartTextTools;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.IUndoManagerExtension;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IUpdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: multiple inspected objects

// TODO: code completion

// TODO: forward, back, clear toolbar icons

// TODO: have a keystroke sequence for evaluation

// TODO: insert the eval'd text w/ right justification

// TODO: insert the eval'd text using italics

// TODO: be able to right-click on an eval area and inspect the associated object

// TODO: insert after the expression being evaluated

// TODO: try and reduce the async apis

// TODO: right click, inspect it action on the value area

// TODO: ctrl-enter to eval and print the selection?

// TODO: refactor this class to reduce its complexity

// TODO: the view needs to clear itself when the connection shuts down

// TODO: we really need to populate the tree in a lazy manner

/**
 * The Dart object inspector view.
 */
public class ObjectInspectorView extends ViewPart implements IDebugEventSetListener {
  class TextViewerAction extends Action implements IUpdate {
    private int actionId;

    TextViewerAction(int actionId) {
      this.actionId = actionId;
    }

    @Override
    public boolean isEnabled() {
      return sourceViewer.canDoOperation(actionId);
    }

    @Override
    public void run() {
      sourceViewer.doOperation(actionId);

      updateActions();
    }

    @Override
    public void update() {
      if (super.isEnabled() != isEnabled()) {
        setEnabled(isEnabled());
      }
    }
  }

  private class DoItAction extends InstrumentedAction implements IUpdate {
    public DoItAction() {
      super("Do It");

      setId(getText());
    }

    @Override
    public void update() {
      setEnabled(sourceViewer.canDoOperation(ITextOperationTarget.COPY));
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {

      try {
        instrumentation.data("Value", getValue().getValueString());
      } catch (Exception e) {
      }
      instrumentation.data("Expression", getExpressionText());

      Job job = new ExpressionEvaluateJob(
          getValue(),
          getExpressionText(),
          new ExpressionListener() {
            @Override
            public void watchEvaluationFinished(IWatchExpressionResult result, String stringValue) {
              if (result.hasErrors()) {
                displayError(result);
              }
            }
          });

      job.schedule();

    }
  }

  private class InspectItAction extends InstrumentedAction implements IUpdate {
    public InspectItAction() {
      super("Inspect It...", DartDebugUIPlugin.getImageDescriptor("obj16/watchlist_view.gif"));

      setId(getText());
    }

    @Override
    public void update() {
      setEnabled(sourceViewer.canDoOperation(ITextOperationTarget.COPY));
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {

      try {
        instrumentation.data("Value", getValue().getValueString());
      } catch (Exception e) {
      }
      instrumentation.data("Expression", getExpressionText());

      Job job = new ExpressionEvaluateJob(
          getValue(),
          getExpressionText(),
          new ExpressionListener() {
            @Override
            public void watchEvaluationFinished(IWatchExpressionResult result, String stringValue) {
              if (result.hasErrors()) {
                displayError(result);
              } else {
                inspectAsync(result.getValue());
              }
            }
          });

      job.schedule();

    }
  }

  private class PrintItAction extends InstrumentedAction implements IUpdate {
    public PrintItAction() {
      super("Print It");

      setId(getText());
    }

    @Override
    public void update() {
      setEnabled(sourceViewer.canDoOperation(ITextOperationTarget.COPY));
    }

    @Override
    protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {

      try {
        instrumentation.data("Value", getValue().getValueString());
      } catch (Exception e) {
      }
      instrumentation.data("Expression", getExpressionText());

      Job job = new ExpressionEvaluateJob(
          getValue(),
          getExpressionText(),
          new ExpressionListener() {
            @Override
            public void watchEvaluationFinished(IWatchExpressionResult result, String stringValue) {
              if (result.hasErrors()) {
                displayError(result);
              } else {
                appendToTextArea(stringValue);
              }
            }
          });

      job.schedule();

    }
  }

  private static DartDebugModelPresentation presentation = new DartDebugModelPresentation();

  public static void inspect(IValue value) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

    try {
      ObjectInspectorView view = (ObjectInspectorView) page.showView(DartUI.ID_INSPECTOR_VIEW);

      view.inspectValue(value);
    } catch (PartInitException e) {
      DartDebugUIPlugin.logError(e);
    }
  }

  protected static void inspectAsync(final IValue value) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        inspect(value);
      }
    });
  }

  private TreeViewer treeViewer;

  private UndoActionHandler undoAction;

  private RedoActionHandler redoAction;

  private IUndoContext undoContext;

  private SourceViewer sourceViewer;

  private Map<String, IUpdate> textActions = new HashMap<String, IUpdate>();

  private DoItAction doItAction;

  private PrintItAction printItAction;
  private InspectItAction inspectItAction;
  static Object EXPRESSION_EVAL_JOB_FAMILY = new Object();

  public ObjectInspectorView() {
    DebugPlugin.getDefault().addDebugEventListener(this);
  }

  @Override
  public void createPartControl(Composite parent) {
    SashForm sash = new SashForm(parent, SWT.VERTICAL);

    treeViewer = new TreeViewer(sash, SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
    treeViewer.setLabelProvider(new NameLabelProvider());
    treeViewer.setContentProvider(new ObjectInspectorContentProvider());
    treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection sel = event.getSelection();
        Object object = SelectionUtil.getSingleElement(sel);

        if (object instanceof IVariable) {
          IVariable variable = (IVariable) object;

          try {
            presentation.computeDetail(variable.getValue(), new IValueDetailListener() {
              @Override
              public void detailComputed(IValue value, String result) {
                updateStatusLine(result);
              }
            });
          } catch (DebugException e) {
            DartDebugUIPlugin.logError(e);
          }
        } else {
          clearStatusLine();
        }
      }
    });

    TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
    nameColumn.setLabelProvider(new NameLabelProvider());
    nameColumn.getColumn().setText("Name");
    nameColumn.getColumn().setWidth(120);
    nameColumn.getColumn().setResizable(true);

    TreeViewerColumn valueColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
    valueColumn.setLabelProvider(new ValueLabelProvider());
    valueColumn.getColumn().setText("Value");
    valueColumn.getColumn().setWidth(140);
    valueColumn.getColumn().setResizable(true);

    Tree tree = treeViewer.getTree();
    tree.setHeaderVisible(true);

    sourceViewer = new SourceViewer(sash, null, SWT.V_SCROLL | SWT.WRAP);
    sourceViewer.configure(getSourceViewerConfiguration());
    sourceViewer.setDocument(createDocument());
    sourceViewer.setUndoManager(new TextViewerUndoManager(100));
    sourceViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
    sourceViewer.getTextWidget().setTabs(2);
    sourceViewer.getDocument().addDocumentListener(new IDocumentListener() {
      @Override
      public void documentAboutToBeChanged(DocumentEvent event) {

      }

      @Override
      public void documentChanged(DocumentEvent event) {
        updateActions();
      }
    });
    sourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateActions();
      }
    });

    sourceViewer.activatePlugins();

    createActions();
    createGlobalActionHandlers();
    hookContextMenu();
    updateActions();

//    PresentationReconciler presentationReconciler = new PresentationReconciler();
//    presentationReconciler.install(textViewer);

    sash.setWeights(new int[] {60, 40});
  }

  @Override
  public void dispose() {
    DebugPlugin.getDefault().removeDebugEventListener(this);

    super.dispose();
  }

  @Override
  public void handleDebugEvents(DebugEvent[] events) {
    for (DebugEvent event : events) {
      if (event.getKind() == DebugEvent.TERMINATE && event.getSource() instanceof IDebugTarget) {
        handleDebugTargetTerminated((IDebugTarget) event.getSource());
      }
    }
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);

    IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getAdapter(
        IWorkbenchSiteProgressService.class);

    if (progressService != null) {
      initProgressService(progressService);
    }
  }

  @Override
  public void setFocus() {
    treeViewer.getControl().setFocus();
  }

  protected void addTextAction(ActionFactory actionFactory, int textOperation) {
    IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
    IWorkbenchAction globalAction = actionFactory.create(window);

    // Create our text action.
    TextViewerAction textAction = new TextViewerAction(textOperation);

    textActions.put(actionFactory.getId(), textAction);

    // Copy its properties from the global action.
    textAction.setText(globalAction.getText());
    textAction.setToolTipText(globalAction.getToolTipText());
    textAction.setDescription(globalAction.getDescription());
    textAction.setImageDescriptor(globalAction.getImageDescriptor());
    textAction.setDisabledImageDescriptor(globalAction.getDisabledImageDescriptor());
    textAction.setAccelerator(globalAction.getAccelerator());

    // Make sure it's up to date.
    textAction.update();

    // Register our text action with the global action handler.
    IActionBars actionBars = getViewSite().getActionBars();

    actionBars.setGlobalActionHandler(actionFactory.getId(), textAction);
  }

  protected void clearStatusLine() {
    setStatusLine(null);
  }

  protected void clearViewer() {
    Display display = Display.getDefault();

    if (display != null) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          setContentDescription(" ");
          treeViewer.setInput(null);
        }
      });
    }
  }

  protected void fillContextMenu(IMenuManager manager) {
    manager.add(doItAction);
    manager.add(printItAction);
    manager.add(inspectItAction);
  }

  protected String getExpressionText() {
    // TODO: if there's a selection, return that; else return the entire text?

    return ((ITextSelection) sourceViewer.getSelection()).getText();
  }

  protected void initProgressService(IWorkbenchSiteProgressService progressService) {
    progressService.showBusyForFamily(EXPRESSION_EVAL_JOB_FAMILY);
  }

  protected void inspectValue(IValue value) {
    presentation.computeDetail(value, new IValueDetailListener() {
      @Override
      public void detailComputed(final IValue value, final String result) {
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            try {
              setContentDescription("UNDER CONSTRUCTION! " + value.getReferenceTypeName() + ": "
                  + result);
            } catch (DebugException e) {

            }
          }
        });
      }
    });

    treeViewer.setInput(value);
  }

  protected void setStatusLine(String message) {
    getViewSite().getActionBars().getStatusLineManager().setMessage(message);
  }

  private void appendToTextArea(final String result) {
    // TODO: display the results in italic and right-aligned

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        IDocument document = sourceViewer.getDocument();

        String insert = result;

        try {
          String current = document.get();

          if (!current.endsWith("\n")) {
            insert = "\n" + insert;
          }

          if (!insert.endsWith("\n")) {
            insert += "\n";
          }

          document.replace(document.getLength(), 0, insert);
        } catch (BadLocationException e) {
          DartDebugUIPlugin.logError(e);
        }
      }
    });
  }

  private void createActions() {
    doItAction = new DoItAction();
    printItAction = new PrintItAction();
    inspectItAction = new InspectItAction();

    textActions.put(doItAction.getId(), doItAction);
    textActions.put(printItAction.getId(), printItAction);
    textActions.put(inspectItAction.getId(), inspectItAction);
  }

  private IDocument createDocument() {
    IDocument document = new Document();
    IDocumentSetupParticipant setupParticipant = new DartDocumentSetupParticipant();

    setupParticipant.setup(document);

    return document;
  }

  private void createGlobalActionHandlers() {
    undoContext = ((IUndoManagerExtension) sourceViewer.getUndoManager()).getUndoContext();

    // set up action handlers that operate on the current context
    undoAction = new UndoActionHandler(getSite(), undoContext);
    redoAction = new RedoActionHandler(getSite(), undoContext);

    IActionBars actionBars = getViewSite().getActionBars();

    actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
    actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);

    // Install the standard text actions.
    addTextAction(ActionFactory.CUT, ITextOperationTarget.CUT);
    addTextAction(ActionFactory.COPY, ITextOperationTarget.COPY);
    addTextAction(ActionFactory.PASTE, ITextOperationTarget.PASTE);
    addTextAction(ActionFactory.DELETE, ITextOperationTarget.DELETE);
    addTextAction(ActionFactory.SELECT_ALL, ITextOperationTarget.SELECT_ALL);
  }

  private IPreferenceStore createPreferenceStore() {
    List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>();

    stores.add(DartToolsPlugin.getDefault().getPreferenceStore());
    stores.add(new PreferencesAdapter(DartCore.getPlugin().getPluginPreferences()));
    stores.add(EditorsUI.getPreferenceStore());

    return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
  }

  private void displayError(final IWatchExpressionResult result) {
    // TODO(devoncarew): what's the best way to display these errors?
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        getViewSite().getActionBars().getStatusLineManager().setErrorMessage(
            result.getErrorMessages()[0]);
      }
    });
  }

  private SourceViewerConfiguration getSourceViewerConfiguration() {
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();

    return new DartSourceViewerConfiguration(
        textTools.getColorManager(),
        createPreferenceStore(),
        null,
        DartPartitions.DART_PARTITIONING);
  }

  private IDartDebugValue getValue() {
    Object object = treeViewer.getInput();

    if (object instanceof IDartDebugValue) {
      return (IDartDebugValue) object;
    } else {
      return null;
    }
  }

  private void handleDebugTargetTerminated(IDebugTarget target) {
    Object object = treeViewer.getInput();

    if (object instanceof IValue) {
      IValue value = (IValue) object;

      if (value.getDebugTarget() == target) {
        clearViewer();
      }
    }
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");

    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        ObjectInspectorView.this.fillContextMenu(manager);
      }
    });

    Menu menu = menuMgr.createContextMenu(sourceViewer.getControl());
    sourceViewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, sourceViewer);
  }

  private void updateActions() {
    for (IUpdate action : textActions.values()) {
      action.update();
    }
  }

  private void updateStatusLine(final String message) {
    Display display = Display.getDefault();

    if (display != null) {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          setStatusLine(message);
        }
      });
    }
  }

}
