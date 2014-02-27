package com.google.dart.dev.util.analysis;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.context.AnalysisContentStatistics.CacheRow;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The view to display analysis statistics.
 */
public class AnalysisView extends ViewPart {
  private class AnalysisContentProvider implements ITreeContentProvider {
    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof AnalysisContextData) {
        AnalysisContextData contentData = (AnalysisContextData) parentElement;
        return contentData.statistics.getCacheRows();
      }
      return null;
    }

    @Override
    public Object[] getElements(Object inputElement) {
      synchronized (contextsLock) {
        if (contexts == null) {
          return ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        return contexts.toArray(new AnalysisContextData[contexts.size()]);
      }
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return element instanceof AnalysisContextData;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  private static class AnalysisContextData {
    private final String name;
    private final AnalysisContentStatistics statistics;
    private final ContextWorkerState workerState;
    private int errorCount;
    private int flushedCount;
    private int inProcessCount;
    private int invalidCount;
    private int validCount;
    private Source[] sources;
    private AnalysisException[] exceptions;

    public AnalysisContextData(String name, AnalysisContentStatistics statistics,
        ContextWorkerState workerState) {
      this.name = name;
      this.statistics = statistics;
      this.workerState = workerState;
      for (CacheRow row : statistics.getCacheRows()) {
        errorCount += row.getErrorCount();
        flushedCount += row.getFlushedCount();
        inProcessCount += row.getInProcessCount();
        invalidCount += row.getInvalidCount();
        validCount += row.getValidCount();
      }
      this.sources = statistics.getSources();
      this.exceptions = statistics.getExceptions();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof AnalysisContextData && ((AnalysisContextData) obj).name.equals(name);
    }

    public int getErrorCount() {
      return errorCount;
    }

    public int getFlushedCount() {
      return flushedCount;
    }

    public int getInProcessCount() {
      return inProcessCount;
    }

    public int getInvalidCount() {
      return invalidCount;
    }

    public Source[] getSources() {
      return sources;
    }

    public int getValidCount() {
      return validCount;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  private static enum ContextWorkerState {
    NONE, IN_QUEUE, ACTIVE;
  }

  private static void addContext(AnalysisWorker[] queueWorkers, AnalysisWorker activeWorker,
      List<AnalysisContextData> contexts, String name, AnalysisContext context) {
    ContextWorkerState workerState = getContextWorkerState(queueWorkers, activeWorker, context);
    contexts.add(new AnalysisContextData(name, ((InternalAnalysisContext) context).getStatistics(),
        workerState));
  }

  private static List<AnalysisContextData> getContexts() {
    AnalysisWorker[] queueWorkers = AnalysisManager.getInstance().getQueueWorkers();
    AnalysisWorker activeWorker = AnalysisManager.getInstance().getActiveWorker();
    List<AnalysisContextData> contexts = Lists.newArrayList();
    for (Project project : DartCore.getProjectManager().getProjects()) {
      String projectName = project.getResource().getName();
      // default context
      AnalysisContext defaultContext = project.getDefaultContext();
      addContext(queueWorkers, activeWorker, contexts, projectName, defaultContext);
      // separate Pub folders
      for (PubFolder pubFolder : project.getPubFolders()) {
        String pubFolderName = projectName + " - " + pubFolder.getResource().getName();
        AnalysisContext context = pubFolder.getContext();
        if (context != defaultContext) {
          addContext(queueWorkers, activeWorker, contexts, pubFolderName, context);
        }
      }
    }
    return contexts;
  }

  private static ContextWorkerState getContextWorkerState(AnalysisWorker[] queueWorkers,
      AnalysisWorker activeWorker, AnalysisContext context) {
    if (activeWorker != null && activeWorker.getContext() == context) {
      return ContextWorkerState.ACTIVE;
    } else {
      for (AnalysisWorker worker : queueWorkers) {
        if (worker.getContext() == context) {
          return ContextWorkerState.IN_QUEUE;
        }
      }
    }
    return ContextWorkerState.NONE;
  }

  private TreeViewer viewer;
  private long lastToggleTime = 0;
  private boolean disposed = false;

  private Font boldFont = null;
  private Font italicFont = null;
  private Color redColor = null;

  private final Object contextsLock = new Object();
  private List<AnalysisContextData> contexts;

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
    viewer.getTree().setHeaderVisible(true);
    viewer.getTree().setLinesVisible(true);
    PixelConverter pixelConverter = new PixelConverter(parent);
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("Item");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(50));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public Font getFont(Object element) {
          if (element instanceof AnalysisContextData) {
            AnalysisContextData contextData = (AnalysisContextData) element;
            if (contextData.workerState == ContextWorkerState.ACTIVE) {
              return getBoldFont();
            } else if (contextData.workerState == ContextWorkerState.IN_QUEUE) {
              return getItalicFont();
            }
          }
          return null;
        }

        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            AnalysisContextData contextData = (AnalysisContextData) element;
            return contextData.name;
          }
          if (element instanceof CacheRow) {
            return ((CacheRow) element).getName();
          }
          return null;
        }
      });
    }
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("ERROR");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(15));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public Color getBackground(Object element) {
          if (element instanceof AnalysisContextData) {
            if (((AnalysisContextData) element).getErrorCount() > 0) {
              return getRedColor();
            }
          } else if (element instanceof CacheRow) {
            if (((CacheRow) element).getErrorCount() > 0) {
              return getRedColor();
            }
          }
          return null;
        }

        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            return "" + ((AnalysisContextData) element).getErrorCount();
          }
          if (element instanceof CacheRow) {
            return "" + ((CacheRow) element).getErrorCount();
          }
          return "";
        }
      });
    }
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("FLUSHED");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(15));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            return "" + ((AnalysisContextData) element).getFlushedCount();
          }
          if (element instanceof CacheRow) {
            return "" + ((CacheRow) element).getFlushedCount();
          }
          return "";
        }
      });
    }
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("IN_PROCESS");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(15));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            return "" + ((AnalysisContextData) element).getInProcessCount();
          }
          if (element instanceof CacheRow) {
            return "" + ((CacheRow) element).getInProcessCount();
          }
          return "";
        }
      });
    }
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("INVALID");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(15));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public Color getBackground(Object element) {
          if (element instanceof AnalysisContextData) {
            AnalysisContextData contextData = (AnalysisContextData) element;
            if (contextData.getInvalidCount() > 0
                && contextData.workerState == ContextWorkerState.NONE) {
              return getRedColor();
            }
          }
          return null;
        }

        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            return "" + ((AnalysisContextData) element).getInvalidCount();
          }
          if (element instanceof CacheRow) {
            return "" + ((CacheRow) element).getInvalidCount();
          }
          return "";
        }
      });
    }
    {
      TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.RIGHT);
      TreeColumn column = viewerColumn.getColumn();
      column.setText("VALID");
      column.setWidth(pixelConverter.convertWidthInCharsToPixels(15));
      viewerColumn.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
          if (element instanceof AnalysisContextData) {
            return "" + ((AnalysisContextData) element).getValidCount();
          }
          if (element instanceof CacheRow) {
            return "" + ((CacheRow) element).getValidCount();
          }
          return "";
        }
      });
    }
    viewer.setContentProvider(new AnalysisContentProvider());
    viewer.setSorter(new ViewerSorter() {
      @Override
      @SuppressWarnings("unchecked")
      public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof AnalysisContextData && e2 instanceof AnalysisContextData) {
          String name1 = ((AnalysisContextData) e1).name;
          String name2 = ((AnalysisContextData) e2).name;
          return getComparator().compare(name1, name2);
        }
        if (e1 instanceof CacheRow && e2 instanceof CacheRow) {
          String name1 = ((CacheRow) e1).getName();
          String name2 = ((CacheRow) e2).getName();
          return getComparator().compare(name1, name2);
        }
        return 0;
      }
    });

    Menu menu = new Menu(viewer.getTree());
    MenuItem copySourcesItem = new MenuItem(menu, SWT.PUSH);
    copySourcesItem.setText("Copy Sources");
    copySourcesItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        ISelection selection = viewer.getSelection();
        if (selection instanceof TreeSelection) {
          AnalysisContextData data = (AnalysisContextData) ((TreeSelection) selection).getFirstElement();
          copySources(data);
        }
      }
    });
    new MenuItem(menu, SWT.SEPARATOR);
    MenuItem copyExceptionsItem = new MenuItem(menu, SWT.PUSH);
    copyExceptionsItem.setText("Copy All Exceptions");
    copyExceptionsItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        copyExceptions();
      }
    });
    viewer.getTree().setMenu(menu);

    viewer.setInput(this);
    // There is a bug in the SWT on OS X.
    // When we update a Tree when user toggle an item, this causes crash.
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=326311 - marked fixed, but actually is not.
    viewer.addTreeListener(new ITreeViewerListener() {
      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        lastToggleTime = System.currentTimeMillis();
      }

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        lastToggleTime = System.currentTimeMillis();
      }
    });
    runUpdateLoop();
  }

  @Override
  public void dispose() {
    if (boldFont != null) {
      boldFont.dispose();
      boldFont = null;
    }
    if (italicFont != null) {
      italicFont.dispose();
      italicFont = null;
    }
    if (redColor != null) {
      redColor.dispose();
      redColor = null;
    }
    disposed = true;
    super.dispose();
  }

  @Override
  public void setFocus() {
  }

  private void copyExceptions() {
    Clipboard clipboard = new Clipboard(viewer.getTree().getDisplay());
    TextTransfer textTransfer = TextTransfer.getInstance();
    clipboard.setContents(new Object[] {getExceptionsText()}, new Transfer[] {textTransfer});
  }

  private void copySources(AnalysisContextData data) {
    Clipboard clipboard = new Clipboard(viewer.getTree().getDisplay());
    TextTransfer textTransfer = TextTransfer.getInstance();
    clipboard.setContents(new Object[] {getSourcesText(data)}, new Transfer[] {textTransfer});
  }

  private Font getBoldFont() {
    if (boldFont == null) {
      Font defaultFont = viewer.getTree().getFont();
      FontData defaultData = defaultFont.getFontData()[0];
      FontData boldData = new FontData(defaultData.getName(), defaultData.getHeight(), SWT.BOLD);
      boldFont = new Font(defaultFont.getDevice(), boldData);
    }
    return boldFont;
  }

  private String getExceptionsText() {
    if (contexts == null) {
      return "- no exceptions -";
    }
    PrintStringWriter writer = new PrintStringWriter();
    boolean first = true;
    for (AnalysisContextData data : contexts) {
      for (AnalysisException exception : data.exceptions) {
        if (first) {
          first = false;
        } else {
          writer.println();
          writer.println("----------------------------------------");
          writer.println();
        }
        exception.printStackTrace(writer);
      }
    }
    if (first) {
      return "- no exceptions -";
    }
    return writer.toString();
  }

  private Font getItalicFont() {
    if (italicFont == null) {
      Font defaultFont = viewer.getTree().getFont();
      FontData defaultData = defaultFont.getFontData()[0];
      FontData boldData = new FontData(defaultData.getName(), defaultData.getHeight(), SWT.ITALIC);
      italicFont = new Font(defaultFont.getDevice(), boldData);
    }
    return italicFont;
  }

  private Color getRedColor() {
    if (redColor == null) {
      Font defaultFont = viewer.getTree().getFont();
      redColor = new Color(defaultFont.getDevice(), 255, 223, 223);
    }
    return redColor;
  }

  private String getSourcesText(AnalysisContextData data) {
    Source[] sources = data.getSources();
    int count = sources.length;
    String[] paths = new String[count];
    for (int i = 0; i < count; i++) {
      paths[i] = sources[i].getFullName();
    }
    Arrays.sort(paths);
    @SuppressWarnings("resource")
    PrintStringWriter writer = new PrintStringWriter();
    for (int i = 0; i < count; i++) {
      writer.println(paths[i]);
    }
    return writer.toString();
  }

  private void refreshUI() {
    Display display = Display.getDefault();
    if (display == null || display.isDisposed()) {
      return;
    }
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (viewer.getControl().isDisposed()) {
          return;
        }
        viewer.refresh();

        Index index = DartCore.getProjectManager().getIndex();
        @SuppressWarnings("resource")
        PrintStringWriter msg = new PrintStringWriter();
        if (index instanceof IndexImpl) {
          try {
            Field field = IndexImpl.class.getDeclaredField("queue");
            field.setAccessible(true);
            OperationQueue queue = (OperationQueue) field.get(index);
            msg.print("Index: queue size = " + queue.size() + "; statistics = "
                + index.getStatistics());
          } catch (Exception exception) {
            msg.print("Index: statistics = " + index.getStatistics());
          }
        } else {
          msg.print("Index: statistics = " + index.getStatistics());
        }
        setContentDescription(msg.toString());
      }
    });
  }

  private void runUpdateLoop() {
    Thread thread = new Thread() {
      @Override
      public void run() {
        updateLoop();
      }
    };
    thread.setDaemon(true);
    thread.setName("AnalysisView update thread");
    thread.start();
  }

  private void updateLoop() {
    while (!disposed) {
      try {
        synchronized (contextsLock) {
          contexts = getContexts();
        }
        // wait while Tree is collapsing
        while (System.currentTimeMillis() - lastToggleTime < 1000) {
          Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        // do refresh
        refreshUI();
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      } catch (Throwable e) {
        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
      }
    }
  }
}
