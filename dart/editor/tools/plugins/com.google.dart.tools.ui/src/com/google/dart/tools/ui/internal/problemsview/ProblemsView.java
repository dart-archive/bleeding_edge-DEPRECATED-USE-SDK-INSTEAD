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
package com.google.dart.tools.ui.internal.problemsview;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.FontPreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This view is a replacement for the default Problems view. It has much less UI surface area, and
 * actively sorts the problems and warnings so the most import one shows up first in the list.
 */
@SuppressWarnings("restriction")
public class ProblemsView extends ViewPart implements MarkersChangeService.MarkerChangeListener {
  static class TypeLabelProvider extends ColumnLabelProvider {
    @Override
    public Color getBackground(Object element) {
      return ProblemsView.getBackground(element);
    }

    @Override
    public Color getForeground(Object element) {
      return ProblemsView.getForeground(element);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof IMarker) {
        return getMarkerTypeLabel((IMarker) element);
      } else {
        return "";
      }
    }
  }

  private class CopyMarkerAction extends SelectionProviderAction {
    public CopyMarkerAction() {
      super(tableViewer, "&Copy");

      setEnabled(false);
      setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
          ISharedImages.IMG_TOOL_COPY));
      setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
          ISharedImages.IMG_TOOL_COPY_DISABLED));
    }

    @Override
    public void run() {
      StringBuilder builder = new StringBuilder();

      for (Object obj : getStructuredSelection().toArray()) {
        if (obj instanceof IMarker) {
          IMarker marker = (IMarker) obj;

          if (builder.length() > 0) {
            builder.append("\n");
          }

          try {
            builder.append(marker.getAttribute(IMarker.MESSAGE));
          } catch (CoreException e) {

          }
        }
      }

      if (builder.length() > 0) {
        copyToClipboard(builder.toString());
      }
    }

    @Override
    public void selectionChanged(ISelection selection) {
      setEnabled(!selection.isEmpty());
    }

    @Override
    public void selectionChanged(IStructuredSelection selection) {
      setEnabled(!selection.isEmpty());
    }

    private void copyToClipboard(String str) {
      clipboard.setContents(new Object[] {str}, new Transfer[] {TextTransfer.getInstance()});
    }
  }

  private static class DescriptionLabelProvider extends ColumnLabelProvider {
    private Font boldFont;

    @Override
    public void dispose(ColumnViewer viewer, ViewerColumn column) {
      if (boldFont != null) {
        boldFont.dispose();
      }

      super.dispose(viewer, column);
    }

    @Override
    public Color getBackground(Object element) {
      return ProblemsView.getBackground(element);
    }

    @Override
    public Color getForeground(Object element) {
      return ProblemsView.getForeground(element);
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof IMarker) {
        IMarker marker = (IMarker) element;

        if (marker != null && marker.exists()) {
          Image image = AnnotationTypesExtManager.getModel().getImageForMarker(marker);

          if (image != null) {
            image = decorateImage(marker, image);
          } else {
            try {
              image = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(marker);
            } catch (Throwable t) {

            }
          }

          return image;
        }
      }

      return null;
    }

    // @Override
    // public Font getFont(Object element) {
    // return null;
    // }

    @Override
    public String getText(Object element) {
      if (element instanceof IMarker) {
        IMarker marker = (IMarker) element;

        return marker.getAttribute(IMarker.MESSAGE, null);
      } else {
        return super.getText(element);
      }
    }

    @Override
    protected void initialize(ColumnViewer viewer, ViewerColumn column) {
      super.initialize(viewer, column);

      Font font = viewer.getControl().getFont();

      if (font != null) {
        FontData data = font.getFontData()[0];

        boldFont = new Font(font.getDevice(), data.getName(), data.getHeight(), SWT.BOLD);
      }
    }

    private Image decorateImage(IMarker marker, Image image) {
      if (image == null) {
        return null;
      }

      if (IDE.getMarkerHelpRegistry().hasResolutions(marker)) {
        ImageDescriptor[] descriptors = new ImageDescriptor[5];

        descriptors[IDecoration.BOTTOM_RIGHT] = DartToolsPlugin.getBundledImageDescriptor("icons/full/ovr16/contassist_ovr.gif");

        image = getImageManager().createImage(new DecorationOverlayIcon(image, descriptors));
      }

      return image;
    }
  }

  private class ErrorViewerFilter extends ViewerFilter {

    public ErrorViewerFilter() {

    }

    @Override
    public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
      preFilter();

      List<IMarker> results = new ArrayList<IMarker>();

      for (Object object : elements) {
        if (object instanceof IMarker) {
          if (select(viewer, parent, object)) {
            results.add((IMarker) object);
          }
        }
      }

      postFilter(results);

      return results.toArray();
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      IMarker marker = (IMarker) element;

      if (!showInfosAction.isChecked()) {
        if (marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_INFO) {
          return false;
        }
      }

      return true;
    }

    protected void preFilter() {

    }

    private void postFilter(List<IMarker> results) {
      updateContentDescription(results);
    }
  }

  private static class ErrorViewTreeContentProvider implements ITreeContentProvider {

    private List<IMarker> markers = new ArrayList<IMarker>();

    private static final Object[] EMPTY_ARRAY = new Object[0];

    @Override
    public void dispose() {

    }

    @Override
    public Object[] getChildren(Object parentElement) {
      return EMPTY_ARRAY;
    }

    @Override
    public Object[] getElements(Object inputElement) {
      List<Object> list = new ArrayList<Object>();

      list.addAll(markers);

      return list.toArray();
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      markers = (List<IMarker>) newInput;
    }
  }

  private static class FileNameLabelProvider extends DelegatingStyledCellLabelProvider {

    public FileNameLabelProvider() {
      super(new FileNameStyledLabelProvider());
    }
  }

  private static class FileNameStyledLabelProvider extends LabelProvider implements
      IStyledLabelProvider, IColorProvider {
    @Override
    public Color getBackground(Object element) {
      return ProblemsView.getBackground(element);
    }

    @Override
    public Color getForeground(Object element) {
      return ProblemsView.getForeground(element);
    }

    @Override
    public StyledString getStyledText(Object element) {
      StyledString str = new StyledString();

      if (element instanceof IMarker) {
        IMarker marker = (IMarker) element;

        IResource resource = marker.getResource();

        if (resource != null) {
          String name = resource.getName();

          if (resource instanceof IWorkspaceRoot) {
            name = "Workspace";
          }

          if (name == null) {
            name = "";
          }

          str.append(name);

          if (name.length() > 0) {
            int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);

            if (lineNumber != -1) {
              String num = NumberFormat.getIntegerInstance().format(lineNumber);

              str.append(" [line " + num + "]", StyledString.DECORATIONS_STYLER);
            }
          }
        }
      }

      return str;
    }
  }

  private class FontPropertyChangeListener implements IPropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (tableViewer != null) {
        if (FontPreferencePage.BASE_FONT_KEY.equals(event.getProperty())) {
          updateTableFont();
        }
      }
    }
  }

  private class GoToMarkerAction extends SelectionProviderAction {

    private static final String GOINTO_RESOURCE_IMG_PATH = "elcl16/gotoobj_tsk.gif"; //$NON-NLS-1$
    private static final String GOINTO_RESOURCE_DISABLED_IMG_PATH = "dlcl16/gotoobj_tsk.gif"; //$NON-NLS-1$

    public GoToMarkerAction() {
      super(tableViewer, "Go to");
      setEnabled(false);
      setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_GO_INTO);
      setImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor(GOINTO_RESOURCE_IMG_PATH));
      setDisabledImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor(GOINTO_RESOURCE_DISABLED_IMG_PATH));
    }

    @Override
    public void run() {
      openSelectedMarker();
    }

    @Override
    public void selectionChanged(ISelection selection) {
      setEnabled(!selection.isEmpty());
    }

    @Override
    public void selectionChanged(IStructuredSelection selection) {
      setEnabled(!selection.isEmpty() && selection.size() == 1);
    }
  }

  private class ShowInfosAction extends Action {
    private static final boolean SHOW_INFOS_DEFAULT = false;

    public ShowInfosAction() {
      super("Show informational messages", AS_CHECK_BOX);

      setImageDescriptor(DartToolsPlugin.getBundledImageDescriptor("icons/full/eview16/tasks_tsk.gif"));

      // restore state
      if (getMemento() != null) {
        Boolean val = getMemento().getBoolean("showInfos");

        if (val != null) {
          setChecked(val.booleanValue());
        } else {
          setChecked(SHOW_INFOS_DEFAULT);
        }
      } else {
        setChecked(SHOW_INFOS_DEFAULT);
      }
    }

    @Override
    public void run() {
      updateFilters();
    }
  }

  private static class TableSorter extends ViewerSorter {
    private static final int DESCENDING = 1;

    private int sortColumn;
    private int direction;

    private static final int CAT_BUILDPATH = 10; // CategorizedProblem.CAT_BUILDPATH

    public TableSorter() {
      direction = DESCENDING;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (direction == DESCENDING) {
        return compareMarkers((IMarker) e1, (IMarker) e2);
      } else {
        return -1 * compareMarkers((IMarker) e1, (IMarker) e2);
      }
    }

    public void setColumn(int column) {
      if (sortColumn == column) {
        direction = -1 * direction;
      } else {
        sortColumn = column;

        direction = DESCENDING;
      }
    }

    @Override
    public void sort(final Viewer viewer, Object[] elements) {
      try {
        super.sort(viewer, elements);
      } catch (IllegalArgumentException iae) {
        // We can get exceptions because the markers are changing out from under us. See
        // https://code.google.com/p/dart/issues/detail?id=3937. This is fairly infrequent
        //behavior. If we encounter an exception, we try a re-sort.
        try {
          super.sort(viewer, elements);
        } catch (IllegalArgumentException ex) {

        }
      }
    }

    private int compareLineNumber(IMarker marker1, IMarker marker2) {
      int line1 = marker1.getAttribute(IMarker.LINE_NUMBER, 0);
      int line2 = marker2.getAttribute(IMarker.LINE_NUMBER, 0);

      return line1 - line2;
    }

    private final int compareMarkers(IMarker marker1, IMarker marker2) {
      // sort by severity
      // then by resource name
      // then by line number
      // then by problem description

      if (marker1 == null || marker2 == null || !marker1.exists() || !marker2.exists()) {
        return 0;
      }

      int val = 0;

      if (sortColumn == 0) {
        val = compareSeverity(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareProblemDescription(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareResourceName(marker1, marker2);

        if (val != 0) {
          return val;
        }

        return compareLineNumber(marker1, marker2);
      } else if (sortColumn == 1) {
        val = compareSeverity(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareResourceName(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareLineNumber(marker1, marker2);

        if (val != 0) {
          return val;
        }

        return compareProblemDescription(marker1, marker2);
      } else {
        val = compareType(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareSeverity(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareResourceName(marker1, marker2);

        if (val != 0) {
          return val;
        }

        val = compareLineNumber(marker1, marker2);

        if (val != 0) {
          return val;
        }

        return compareProblemDescription(marker1, marker2);
      }
    }

    private int compareProblemDescription(IMarker marker1, IMarker marker2) {
      String desc1 = marker1.getAttribute(IMarker.MESSAGE, "");
      String desc2 = marker2.getAttribute(IMarker.MESSAGE, "");

      return desc1.compareToIgnoreCase(desc2);
    }

    private int compareResourceName(IMarker marker1, IMarker marker2) {
      String name1 = marker1.getResource().getName();
      String name2 = marker2.getResource().getName();

      return name1.compareToIgnoreCase(name2);
    }

    private final int compareSeverity(IMarker marker1, IMarker marker2) {
      int sev1 = marker1.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
      int sev2 = marker2.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);

      if (sev1 == sev2 && sev1 == IMarker.SEVERITY_ERROR) {
        int category1 = marker1.getAttribute("categoryId", 0);
        int category2 = marker2.getAttribute("categoryId", 0);

        int compare1 = (category1 == CAT_BUILDPATH ? 0 : 1);
        int compare2 = (category2 == CAT_BUILDPATH ? 0 : 1);

        return compare1 - compare2;
      }

      // This order (sev2 - sev1) is deliberate.
      return sev2 - sev1;
    }

    private int compareType(IMarker marker1, IMarker marker2) {
      String desc1 = MarkersExtManager.getInstance().getLabelForMarkerType(marker1);
      String desc2 = MarkersExtManager.getInstance().getLabelForMarkerType(marker2);

      return desc1.compareToIgnoreCase(desc2);
    }
  }

  private static Object REFRESH_MARKERS_JOB_FAMILY = new Object();

  private static ResourceManager resourceManager;

  private static Color getBackground(Object element) {
    // if (BK_COLOR == null) {
    // BK_COLOR = new Color(Display.getDefault(), 0xDD, 0xDD, 0xDD);
    // }

    return null;
  }

  private static Color getForeground(Object element) {
    return null;
  }

  private static ResourceManager getImageManager() {
    if (resourceManager == null) {
      resourceManager = new LocalResourceManager(JFaceResources.getResources());
    }

    return resourceManager;
  }

  @SuppressWarnings("unused")
  private static String getMarkerToolTipText(IMarker marker) {
    StringBuilder builder = new StringBuilder();

    builder.append(marker.getAttribute(IMarker.MESSAGE, null));

    return builder.toString();
  }

  private static String getMarkerTypeLabel(IMarker marker) {
    try {
      String typeId = marker.getType();

      return MarkersExtManager.getInstance().getLabelforTypeId(typeId);
    } catch (CoreException ce) {
      return "";
    }
  }

  private IMemento memento;

  protected TableViewer tableViewer;

  protected TableSorter tableSorter;

  private ErrorViewerFilter tableFilter;

  private ShowInfosAction showInfosAction;

  private Clipboard clipboard;

  private CopyMarkerAction copyAction;
  private GoToMarkerAction goToMarkerAction;

  private Job job;

  private boolean rescheduleJob;

  private long lastShowTime;

  private IPropertyChangeListener fontPropertyChangeListener = new FontPropertyChangeListener();

  public ProblemsView() {
    JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
  }

  @Override
  public void createPartControl(Composite parent) {
    clipboard = new Clipboard(parent.getDisplay());

    tableViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.VIRTUAL | SWT.V_SCROLL | SWT.MULTI
        | SWT.FULL_SELECTION);
    tableViewer.setContentProvider(new ErrorViewTreeContentProvider());
    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        openSelectedMarker();
      }
    });
    tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          updateStatusLine((IStructuredSelection) selection);
        }
      }
    });

    // Create actions; must be done after the construction of the tableViewer
    goToMarkerAction = new GoToMarkerAction();
    copyAction = new CopyMarkerAction();

    tableViewer.addSelectionChangedListener(copyAction);
    tableViewer.addSelectionChangedListener(goToMarkerAction);

    tableSorter = new TableSorter();
    tableSorter.setColumn(1);
    tableViewer.setComparator(tableSorter);
    tableViewer.getTable().setSortDirection(SWT.UP);

    Table table = tableViewer.getTable();

    TableViewerColumn descriptionColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    descriptionColumn.setLabelProvider(new DescriptionLabelProvider());
    descriptionColumn.getColumn().setText("Description");
    descriptionColumn.getColumn().setWidth(520);
    descriptionColumn.getColumn().setResizable(true);
    enableSorting(descriptionColumn.getColumn(), 0);

    TableViewerColumn fileNameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    fileNameColumn.setLabelProvider(new FileNameLabelProvider());
    fileNameColumn.getColumn().setText("Location");
    fileNameColumn.getColumn().setWidth(220);
    fileNameColumn.getColumn().setResizable(true);
    enableSorting(fileNameColumn.getColumn(), 1);

    tableViewer.getTable().setSortColumn(fileNameColumn.getColumn());

//    TableViewerColumn typeColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
//    typeColumn.setLabelProvider(new TypeLabelProvider());
//    typeColumn.getColumn().setText("Type");
//    typeColumn.getColumn().setWidth(130);
//    typeColumn.getColumn().setResizable(true);
//    enableSorting(typeColumn.getColumn(), 2);

    restoreColumnWidths();

    table.setLayoutData(new GridData(GridData.FILL_BOTH));
//    table.setFont(parent.getFont());
    updateTableFont();

    table.setLinesVisible(true);
    table.setHeaderVisible(true);

    table.layout(true);

    getSite().setSelectionProvider(tableViewer);

    IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();

    fillInToolbar(toolbar);
    registerContextMenu();

    tableFilter = new ErrorViewerFilter();

    updateFilters();

    startUpdateJob();

    MarkersChangeService.getService().addListener(this);

    focusOnActiveEditor();
  }

  @Override
  public void dispose() {
    if (copyAction != null) {
      copyAction.dispose();
      copyAction = null;
    }

    if (clipboard != null) {
      clipboard.dispose();
      clipboard = null;
    }

    if (goToMarkerAction != null) {
      goToMarkerAction.dispose();
      goToMarkerAction = null;
    }

    if (fontPropertyChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(fontPropertyChangeListener);
      fontPropertyChangeListener = null;
    }

    MarkersChangeService.getService().removeListener(this);

    super.dispose();
  }

  @Override
  public void handleResourceChange() {
    startUpdateJob();
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);

    this.memento = memento;

    IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getAdapter(
        IWorkbenchSiteProgressService.class);

    if (progressService != null) {
      initProgressService(progressService);
    }

    getSite().getPage().addSelectionListener(new ISelectionListener() {
      @Override
      public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part != ProblemsView.this) {
          focusOn(part, selection);
        }
      }
    });
  }

  // implementation

  @Override
  public void saveState(IMemento memento) {
    super.saveState(memento);

    if (showInfosAction != null) {
      memento.putBoolean("showInfos", showInfosAction.isChecked());
    }

    StringBuilder builder = new StringBuilder();

    for (TableColumn column : tableViewer.getTable().getColumns()) {
      if (builder.length() > 0) {
        builder.append(";");
      }

      builder.append(Integer.toString(column.getWidth()));
    }

    memento.putString("columnWidths", builder.toString());
  }

  @Override
  public void setFocus() {
    tableViewer.getTable().setFocus();
  }

  protected void fillInToolbar(IToolBarManager toolbar) {
    showInfosAction = new ShowInfosAction();

    toolbar.add(showInfosAction);
  }

  protected IMemento getMemento() {
    return memento;
  }

  protected String getStatusSummary(IMarker[] markers) {
    return getStatusSummary(Arrays.asList(markers));
  }

  protected String getStatusSummary(List<IMarker> markers) {
    return MarkersUtils.getInstance().summarizeMarkers(markers);
  }

  protected TableViewer getViewer() {
    return tableViewer;
  }

  protected void initProgressService(IWorkbenchSiteProgressService progressService) {
    progressService.showBusyForFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
    progressService.showBusyForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);

    progressService.showBusyForFamily(REFRESH_MARKERS_JOB_FAMILY);
  }

  protected void showMarkers(final List<IMarker> markers) {
    for (int i = markers.size() - 1; i >= 0; i--) {
      if (!markers.get(i).exists()) {
        markers.remove(i);
      }
    }

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (tableViewer.getControl() != null && !tableViewer.getControl().isDisposed()) {
          tableViewer.setInput(markers);
        }
      }
    });
  }

  protected void startUpdateJob() {
    if (job != null) {
      rescheduleJob = true;
    } else {
      job = new WorkspaceJob("Refresh Problems") {
        @Override
        public boolean belongsTo(Object family) {
          return family == REFRESH_MARKERS_JOB_FAMILY;
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
          // Update problems view at least every 2 to 3 seconds
          if (rescheduleJob && System.currentTimeMillis() - lastShowTime < 2000) {
            rescheduleJob = false;
            schedule(1000);
          } else {
            monitor.beginTask("Refresh problems", IProgressMonitor.UNKNOWN);

            try {
              List<IMarker> markers = new ArrayList<IMarker>();

              for (String markerId : MarkersUtils.getInstance().getErrorsViewMarkerIds()) {
                IMarker[] marks = ResourcesPlugin.getWorkspace().getRoot().findMarkers(
                    markerId,
                    true,
                    IResource.DEPTH_INFINITE);

                markers.addAll(Arrays.asList(marks));
              }

              showMarkers(markers);
              lastShowTime = System.currentTimeMillis();
            } catch (CoreException ce) {
              DartToolsPlugin.log(ce);
            } finally {
              if (rescheduleJob) {
                rescheduleJob = false;
                schedule(1000);
              } else {
                job = null;
              }
            }

            monitor.done();
          }

          return Status.OK_STATUS;
        }
      };

      job.setRule(ResourcesPlugin.getWorkspace().getRoot());

      job.schedule(250);
    }
  }

  // private static Color BK_COLOR = null;

  protected void updateContentDescription(List<IMarker> markers) {
    if (markers == null) {
      markers = new ArrayList<IMarker>();
    }

    String desc = getStatusSummary(markers);

    setContentDescription(desc);
  }

  protected void updateTableFont() {
    Font newFont = JFaceResources.getFont(FontPreferencePage.BASE_FONT_KEY);
    Font oldFont = tableViewer.getTable().getFont();
    Font font = SWTUtil.changeFontSize(oldFont, newFont);
    tableViewer.getTable().setFont(font);
    tableViewer.getTable().layout(true, true); // bug: row height never decreases
  }

  private void addActionsForSelection(IMenuManager menuManager) {
    IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();

    if (selection.size() == 1) {
      Object element = selection.getFirstElement();

      if (!(element instanceof IMarker)) {
        return;
      }

      final IMarker marker = (IMarker) element;

      IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);

      for (final IMarkerResolution resolution : resolutions) {
        Action action = new Action(escapeSpecialChars(resolution.getLabel())) {
          @Override
          public void run() {
            resolution.run(marker);
          }
        };

        if (resolution instanceof IMarkerResolution2) {
          IMarkerResolution2 resolution2 = (IMarkerResolution2) resolution;
          Image image = resolution2.getImage();
          if (image != null) {
            action.setImageDescriptor(ImageDescriptor.createFromImage(image));
          }
        }

        menuManager.add(action);
      }
    }
  }

  private void enableSorting(final TableColumn column, final int index) {
    column.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tableSorter.setColumn(index);
        int dir = tableViewer.getTable().getSortDirection();
        if (tableViewer.getTable().getSortColumn() == column) {
          dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
        } else {
          dir = SWT.UP;
        }
        tableViewer.getTable().setSortDirection(dir);
        tableViewer.getTable().setSortColumn(column);
        tableViewer.refresh();
      }
    });
  }

  private String escapeSpecialChars(String label) {
    if (label == null) {
      return "";
    }

    // handle OS X
    return label.trim().replace("@", "");
  }

  private void fillContextMenu(IMenuManager menuManager) {
    addActionsForSelection(menuManager);

    menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    if (menuManager.getItems().length > 1) {
      menuManager.add(new Separator());
    }

    if (!getViewer().getSelection().isEmpty()) {
      menuManager.add(goToMarkerAction);
      menuManager.add(copyAction);
    }
  }

  private void focusOn(IWorkbenchPart part, ISelection selection) {

  }

  private void focusOnActiveEditor() {
    IEditorPart part = getViewSite().getPage().getActiveEditor();

    if (part == null) {
      focusOn(getViewSite().getPage().getActivePart(), null);
    } else {
      focusOn(part, null);
    }
  }

  @SuppressWarnings("unused")
  private IEditorPart getEditorFor(IFile file) {
    IWorkbenchPage page = getViewSite().getPage();

    for (IEditorReference editorReference : page.getEditorReferences()) {
      try {
        IEditorInput editorInput = editorReference.getEditorInput();

        if (editorInput instanceof IFileEditorInput) {
          IFileEditorInput input = (IFileEditorInput) editorInput;

          if (file.equals(input.getFile())) {
            return editorReference.getEditor(true);
          }
        }
      } catch (PartInitException pie) {
        // ignore
      }
    }

    return null;
  }

  private void openSelectedMarker() {
    ISelection sel = tableViewer.getSelection();

    if (sel instanceof IStructuredSelection) {
      Object element = ((IStructuredSelection) sel).getFirstElement();

      if (element instanceof IMarker) {
        IMarker marker = (IMarker) element;

        if (marker.getResource() instanceof IFile) {
          try {
            IDE.openEditor(getViewSite().getPage(), marker);
          } catch (PartInitException e) {
            ErrorDialog.openError(
                getSite().getShell(),
                "Error Opening Marker",
                "Unable to open an editor for the given marker: " + e.getClass().getSimpleName(),
                new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, e.toString(), e));

            DartToolsPlugin.log(e);
          }
        }
      }
    }
  }

  private void registerContextMenu() {
    getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);

    MenuManager mm = new MenuManager();
    mm.setRemoveAllWhenShown(true);
    mm.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager mgr) {
        fillContextMenu(mgr);
      }
    });

    Viewer viewer = getViewer();

    Menu menu = mm.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);

    // Register menu for extension.
    getSite().registerContextMenu(mm, viewer);
  }

  private void restoreColumnWidths() {
    if (memento != null) {
      String str = memento.getString("columnWidths");

      if (str != null) {
        String[] widths = str.split(";");
        TableColumn[] columns = tableViewer.getTable().getColumns();

        for (int i = 0; i < widths.length; i++) {
          if (columns.length > i) {
            try {
              columns[i].setWidth(Integer.parseInt(widths[i]));
            } catch (NumberFormatException nfe) {

            }
          }
        }
      }
    }
  }

  private void updateFilters() {
    if (tableViewer.getFilters().length == 0) {
      tableViewer.setFilters(new ViewerFilter[] {tableFilter});
    }

    //tableFilter.setEnabled(filterContentsAction.isChecked());

    tableViewer.refresh();
  }

  private void updateStatusLine(IStructuredSelection selection) {
    String message;

    if (selection == null || selection.size() == 0) {
      message = "";
    } else if (selection.size() == 1) {
      Object sel = selection.getFirstElement();

      if (sel instanceof IMarker) {
        IMarker marker = (IMarker) sel;

        message = marker.getAttribute(IMarker.MESSAGE, "");
      } else {
        message = "";
      }
    } else {
      List<IMarker> selMarkers = new ArrayList<IMarker>();

      for (Object obj : selection.toList()) {
        if (obj instanceof IMarker) {
          selMarkers.add((IMarker) obj);
        }
      }

      message = getStatusSummary(selMarkers.toArray(new IMarker[selMarkers.size()]));
    }

    getViewSite().getActionBars().getStatusLineManager().setMessage(message);
  }

}
