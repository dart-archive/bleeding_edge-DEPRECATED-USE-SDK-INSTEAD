/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.ui;

import com.ibm.icu.text.NumberFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.IReconcileStep;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionCollection;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.util.Utilities;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nsd A Status Line contribution intended to display the selected offsets in an editor.
 *         Double-clicking shows information about partitions, document regions, annotations, and
 *         selection.
 */
public class OffsetStatusLineContributionItem extends StatusLineContributionItem {

  class AnnotationPropertySource implements IPropertySource {
    Annotation fAnnotation = null;
    IPropertyDescriptor[] fDescriptors = null;
    String[] TEMPORARY_ANNOTATION_KEYS = new String[] {
        "Partition Type", "Step", "Scope", "Offset", "Length", "Description"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    public AnnotationPropertySource(Annotation annotation) {
      super();
      fAnnotation = annotation;
    }

    public Object getEditableValue() {
      return null;
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
      if (fDescriptors == null) {
        try {
          if (fAnnotation instanceof SimpleMarkerAnnotation) {
            Map attrs = ((SimpleMarkerAnnotation) fAnnotation).getMarker().getAttributes();
            Object[] keys = attrs.keySet().toArray();

            fDescriptors = new IPropertyDescriptor[keys.length];
            for (int i = 0; i < keys.length; i++) {
              TextPropertyDescriptor descriptor = new TextPropertyDescriptor(keys[i].toString(),
                  keys[i].toString());
              fDescriptors[i] = descriptor;
            }
          } else if (fAnnotation instanceof TemporaryAnnotation) {
            Object key = ((TemporaryAnnotation) fAnnotation).getKey();
            if (key != null && key instanceof ReconcileAnnotationKey) {
              String[] keys = TEMPORARY_ANNOTATION_KEYS;
              fDescriptors = new IPropertyDescriptor[keys.length];
              for (int i = 0; i < keys.length; i++) {
                TextPropertyDescriptor descriptor = new TextPropertyDescriptor(keys[i].toString(),
                    keys[i].toString());
                fDescriptors[i] = descriptor;
              }
            }
          }
        } catch (CoreException e) {
          e.printStackTrace();
        }
      }
      if (fDescriptors == null)
        fDescriptors = new IPropertyDescriptor[0];
      return fDescriptors;
    }

    public Object getPropertyValue(Object id) {
      String value = null;
      if (fAnnotation instanceof SimpleMarkerAnnotation) {
        Object o;
        try {
          o = ((SimpleMarkerAnnotation) fAnnotation).getMarker().getAttributes().get(id);
          if (o != null) {
            value = o.toString();
          }
        } catch (CoreException e) {
        }
      } else if (fAnnotation instanceof TemporaryAnnotation) {
        if (TEMPORARY_ANNOTATION_KEYS[0].equals(id)) {
          Object key = ((TemporaryAnnotation) fAnnotation).getKey();
          if (key != null && key instanceof ReconcileAnnotationKey) {
            value = ((ReconcileAnnotationKey) key).getPartitionType();
          }
        } else if (TEMPORARY_ANNOTATION_KEYS[1].equals(id)) {
          Object key = ((TemporaryAnnotation) fAnnotation).getKey();
          if (key != null && key instanceof ReconcileAnnotationKey) {
            IReconcileStep step = ((ReconcileAnnotationKey) key).getStep();
            if (step != null) {
              value = step.toString();
            }
          }
        } else if (TEMPORARY_ANNOTATION_KEYS[2].equals(id)) {
          Object key = ((TemporaryAnnotation) fAnnotation).getKey();
          if (key != null && key instanceof ReconcileAnnotationKey) {
            int scope = ((ReconcileAnnotationKey) key).getScope();
            if (scope == ReconcileAnnotationKey.PARTIAL) {
              value = "PARTIAL"; //$NON-NLS-1$
            }
            if (scope == ReconcileAnnotationKey.TOTAL) {
              value = "TOTAL"; //$NON-NLS-1$
            }
          }
        } else if (TEMPORARY_ANNOTATION_KEYS[3].equals(id)) {
          IAnnotationModel annotationModel = fTextEditor.getDocumentProvider().getAnnotationModel(
              fTextEditor.getEditorInput());
          Position p = annotationModel.getPosition(fAnnotation);
          if (p != null) {
            value = String.valueOf(p.getOffset());
          }
        } else if (TEMPORARY_ANNOTATION_KEYS[4].equals(id)) {
          IAnnotationModel annotationModel = fTextEditor.getDocumentProvider().getAnnotationModel(
              fTextEditor.getEditorInput());
          Position p = annotationModel.getPosition(fAnnotation);
          if (p != null) {
            value = String.valueOf(p.getLength());
          }
        } else if (TEMPORARY_ANNOTATION_KEYS[5].equals(id)) {
          value = ((TemporaryAnnotation) fAnnotation).getDescription();
        }
      }
      return value;
    }

    public boolean isPropertySet(Object id) {
      return false;
    }

    public void resetPropertyValue(Object id) {
      try {
        if (fAnnotation instanceof SimpleMarkerAnnotation) {
          ((SimpleMarkerAnnotation) fAnnotation).getMarker().getAttributes().remove(id);
        } else if (fAnnotation instanceof TemporaryAnnotation) {
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }

    public void setPropertyValue(Object id, Object value) {
      try {
        if (fAnnotation instanceof SimpleMarkerAnnotation) {
          ((MarkerAnnotation) fAnnotation).getMarker().setAttribute(id.toString(), value);
        } else if (fAnnotation instanceof TemporaryAnnotation) {
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
  }

  class InformationDialog extends Dialog {

    IDocument fDocument = fTextEditor.getDocumentProvider().getDocument(
        fTextEditor.getEditorInput());

    public InformationDialog(Shell parentShell) {
      super(parentShell);
      setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    private void createAnnotationTabContents(Composite annotationsTabComposite) {
      annotationsTabComposite.setLayout(new GridLayout());
      annotationsTabComposite.setLayoutData(new GridData());

      final Composite annotationsComposite = new Composite(annotationsTabComposite, SWT.NONE);
      annotationsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      final TableViewer annotationsTable = new TableViewer(annotationsComposite, SWT.SINGLE
          | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
      annotationsTable.setComparator(new ViewerComparator(new Comparator() {
        public int compare(Object o1, Object o2) {
          Annotation annotation1 = (Annotation) o1;
          Annotation annotation2 = (Annotation) o2;
          String line1 = getLineNumber(annotation1);
          String line2 = getLineNumber(annotation2);
          return Integer.parseInt(line1) - Integer.parseInt(line2);
        }
      }));
      annotationsTable.setContentProvider(new ArrayContentProvider());
      annotationsTable.getTable().setHeaderVisible(true);
      annotationsTable.getTable().setLinesVisible(true);
      String[] columns = new String[] {"Line", "Owner", "Type", "Class", "Message"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
      annotationsTable.setLabelProvider(new ITableLabelProvider() {
        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public Image getColumnImage(Object element, int columnIndex) {
          return null;
        }

        public String getColumnText(Object element, int columnIndex) {
          Annotation annotation = (Annotation) element;
          String text = null;
          switch (columnIndex) {
            case 0:
              text = getLineNumber(annotation);
              break;
            case 1:
              text = getOwner(annotation);
              break;
            case 2:
              text = getType(annotation); //$NON-NLS-1$
              break;
            case 3:
              text = annotation.getClass().getName();
              break;
            case 4:
              text = annotation.getText();
              break;
          }
          if (text == null)
            text = ""; //$NON-NLS-1$
          return text;
        }

        private String getOwner(Annotation annotation) {
          String owner = null;
          if (annotation instanceof MarkerAnnotation) {
            owner = ((MarkerAnnotation) annotation).getMarker().getAttribute("owner", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$				
          } else if (annotation instanceof TemporaryAnnotation) {
            Object key = ((TemporaryAnnotation) annotation).getKey();
            if (key != null) {
              if (key instanceof ReconcileAnnotationKey) {
                key = key.getClass().getName();
              }
              if (key != null)
                owner = key.toString();
            }
          }
          return owner;
        }

        private String getType(Annotation annotation) {
          String type = null;
          if (annotation instanceof MarkerAnnotation) {
            type = "M:" + MarkerUtilities.getMarkerType(((MarkerAnnotation) annotation).getMarker()); //$NON-NLS-1$
          } else {
            type = "A:" + annotation.getType(); //$NON-NLS-1$
          }
          if (type == null)
            type = ""; //$NON-NLS-1$
          return type;
        }

        public boolean isLabelProperty(Object element, String property) {
          return true;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
      });

      TableLayout tlayout = new TableLayout();
      CellEditor[] cellEditors = new CellEditor[columns.length];
      int columnWidths[] = new int[] {
          Display.getCurrent().getBounds().width / 14, Display.getCurrent().getBounds().width / 7,
          Display.getCurrent().getBounds().width / 7, Display.getCurrent().getBounds().width / 14,
          Display.getCurrent().getBounds().width / 7};
      for (int i = 0; i < columns.length; i++) {
        tlayout.addColumnData(new ColumnWeightData(1));
        TableColumn tc = new TableColumn(annotationsTable.getTable(), SWT.NONE);
        tc.setText(columns[i]);
        tc.setResizable(true);
        tc.setWidth(columnWidths[i]);
      }
      annotationsTable.setCellEditors(cellEditors);
      annotationsTable.setColumnProperties(columns);
      List matchingAnnotations = new ArrayList(0);
      if (fTextEditor != null) {
        IAnnotationModel annotationModel = fTextEditor.getDocumentProvider().getAnnotationModel(
            fTextEditor.getEditorInput());
        if (annotationModel != null) {
          Iterator iterator = annotationModel.getAnnotationIterator();
          while (iterator.hasNext()) {
            Annotation element = (Annotation) iterator.next();
            if (true) {
              matchingAnnotations.add(element);
            }
          }
        }
      }
      annotationsTable.setSorter(new ViewerSorter());
      annotationsTable.setInput(matchingAnnotations);

      final Sash sash = new Sash(annotationsComposite, SWT.HORIZONTAL);

      final PropertySheetPage propertySheet = new PropertySheetPage();
      propertySheet.createControl(annotationsComposite);
      propertySheet.setPropertySourceProvider(new IPropertySourceProvider() {
        public IPropertySource getPropertySource(Object object) {
          if (object instanceof Annotation) {
            IPropertySource annotationPropertySource = new AnnotationPropertySource(
                ((Annotation) object));
            return annotationPropertySource;
          }
          return null;
        }
      });

      annotationsTable.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          propertySheet.selectionChanged(null, event.getSelection());
        }
      });

      final FormLayout form = new FormLayout();
      annotationsComposite.setLayout(form);

      FormData tableData = new FormData();
      tableData.top = new FormAttachment(0, 0);
      tableData.bottom = new FormAttachment(sash, 2);
      tableData.left = new FormAttachment(0, 0);
      tableData.right = new FormAttachment(100, 0);
      annotationsTable.getControl().setLayoutData(tableData);

      FormData propertiesData = new FormData();
      propertiesData.top = new FormAttachment(sash, 2);
      propertiesData.left = new FormAttachment(0, 0);
      propertiesData.right = new FormAttachment(100, 0);
      propertiesData.bottom = new FormAttachment(100, 0);
      propertySheet.getControl().setLayoutData(propertiesData);

      final FormData sashData = new FormData();
      sashData.top = new FormAttachment(60, 0);
      sashData.left = new FormAttachment(0, 0);
      sashData.right = new FormAttachment(100, 0);
      sash.setLayoutData(sashData);
      sash.addListener(SWT.Selection, new org.eclipse.swt.widgets.Listener() {
        public void handleEvent(Event e) {
          sashData.top = new FormAttachment(0, e.y);
          annotationsComposite.layout();
        }
      });
      annotationsComposite.pack(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent) {
      ISelection selection = fTextEditor.getSelectionProvider().getSelection();
      ITextSelection textSelection = (ITextSelection) selection;
      IStructuredSelection structuredSelection = null;
      if (selection instanceof IStructuredSelection)
        structuredSelection = (IStructuredSelection) selection;

      parent.getShell().setText(
          SSEUIMessages.OffsetStatusLineContributionItem_0 + textSelection.getOffset()
              + "-" + (textSelection.getOffset() + textSelection.getLength())); //$NON-NLS-1$ //$NON-NLS-2$
      Composite composite = (Composite) super.createDialogArea(parent);

      Text documentTypeLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 2;
      documentTypeLabel.setLayoutData(gd);
      documentTypeLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_6
          + fDocument.getClass().getName()); //$NON-NLS-1$

      Text documentProviderLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 2;
      documentProviderLabel.setLayoutData(gd);
      documentProviderLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_7
          + fTextEditor.getDocumentProvider().getClass().getName()); //$NON-NLS-1$

      Text editorInputLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 2;
      editorInputLabel.setLayoutData(gd);
      editorInputLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_12
          + fTextEditor.getEditorInput().getClass().getName()); //$NON-NLS-1$

      final Text bomLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 2;
      bomLabel.setLayoutData(gd);
      bomLabel.setEnabled(false);
      bomLabel.setText("Byte Order Mark: ");

      IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
          fDocument);
      if (model != null) {
        Text modelIdLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        modelIdLabel.setLayoutData(gd);
        modelIdLabel.setText("ID: " + model.getId()); //$NON-NLS-1$

        Text modelBaseLocationLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        modelBaseLocationLabel.setLayoutData(gd);
        modelBaseLocationLabel.setText("Base Location: " + model.getBaseLocation()); //$NON-NLS-1$

        Text modelContentTypeLabel = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        modelContentTypeLabel.setLayoutData(gd);
        modelContentTypeLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_4
            + model.getContentTypeIdentifier()); //$NON-NLS-1$

        Text modelHandlerContentTypeLabel = new Text(composite, SWT.MULTI | SWT.WRAP
            | SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        modelHandlerContentTypeLabel.setLayoutData(gd);
        modelHandlerContentTypeLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_5
            + model.getModelHandler().getAssociatedContentTypeId()
            + " (" + model.getModelHandler() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final Text counts = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        counts.setLayoutData(gd);
        counts.setText("Counting...");
        counts.setEnabled(false);
        final IStructuredModel finalModel = model;
        final Display display = Display.getCurrent();
        Job counter = new Job("Counting regions") {
          protected IStatus run(IProgressMonitor monitor) {
            IStructuredDocumentRegion[] structuredDocumentRegions = finalModel.getStructuredDocument().getStructuredDocumentRegions();
            int length = finalModel.getStructuredDocument().getLength();
            int regionCount = 0;
            for (int i = 0; i < structuredDocumentRegions.length; i++) {
              regionCount += structuredDocumentRegions[i].getNumberOfRegions();
            }
            NumberFormat formatter = NumberFormat.getIntegerInstance();
            final String regioncount = "Count: " + formatter.format(structuredDocumentRegions.length) + " document regions containing " + formatter.format(regionCount) + " text regions representing " + formatter.format(length) + " characters";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            display.asyncExec(new Runnable() {
              public void run() {
                if (!counts.isDisposed()) {
                  counts.setText(regioncount);
                  counts.setEnabled(true);
                }
                if (!bomLabel.isDisposed()) {
                  bomLabel.setText("Byte Order Mark: " + getBOMText(fTextEditor.getEditorInput())); //$NON-NLS-1$
                  bomLabel.setEnabled(true);
                }
              }
            });
            return Status.OK_STATUS;
          }
        };
        counter.schedule(1000);

        Label blankRow = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        blankRow.setLayoutData(gd);
      }
      if (model != null) {
        model.releaseFromRead();
      }

      TabFolder tabfolder = new TabFolder(composite, SWT.NONE);
      tabfolder.setLayoutData(new GridData(GridData.FILL_BOTH));

      TabItem partitionTab = new TabItem(tabfolder, SWT.BORDER);
      partitionTab.setText(SSEUIMessages.OffsetStatusLineContributionItem_2); //$NON-NLS-1$
      SashForm partitions = new SashForm(tabfolder, SWT.NONE);
      partitions.setOrientation(SWT.VERTICAL);
      partitionTab.setControl(partitions);
      createPartitionTabContents(partitions);
      partitions.setWeights(new int[] {2, 1});

      TabItem annotationsTab = new TabItem(tabfolder, SWT.BORDER);
      annotationsTab.setText("Annotations"); //$NON-NLS-1$
      Composite annotations = new Composite(tabfolder, SWT.NONE);
      annotationsTab.setControl(annotations);
      createAnnotationTabContents(annotations);

      // only create the ITextRegions tab for IStructuredDocuments
      if (fDocument instanceof IStructuredDocument) {
        TabItem regionTab = new TabItem(tabfolder, SWT.BORDER);
        regionTab.setText(SSEUIMessages.OffsetStatusLineContributionItem_3); //$NON-NLS-1$
        SashForm regions = new SashForm(tabfolder, SWT.NONE);
        regions.setOrientation(SWT.HORIZONTAL);
        regionTab.setControl(regions);
        createRegionTabContents(regions);
        regions.setWeights(new int[] {3, 2});
      }

      if (structuredSelection != null) {
        TabItem editorSelectionTab = new TabItem(tabfolder, SWT.BORDER);
        editorSelectionTab.setText(SSEUIMessages.OffsetStatusLineContributionItem_14);
        Composite editorSelectionComposite = new Composite(tabfolder, SWT.NONE);
        editorSelectionTab.setControl(editorSelectionComposite);
        fillSelectionTabContents(editorSelectionComposite, structuredSelection.toList(),
            "Class: " + structuredSelection.getClass().getName()); //$NON-NLS-1$
      }

      model = StructuredModelManager.getModelManager().getExistingModelForRead(fDocument);
      if (model != null) {
        TabItem overlappingIndexedRegionsTab = new TabItem(tabfolder, SWT.BORDER);
        overlappingIndexedRegionsTab.setText(SSEUIMessages.OffsetStatusLineContributionItem_20);
        Composite overlappingIndexedRegionsTabComposite = new Composite(tabfolder, SWT.NONE);
        overlappingIndexedRegionsTab.setControl(overlappingIndexedRegionsTabComposite);
        fillSelectionTabContents(overlappingIndexedRegionsTabComposite,
            getIndexedRegions(textSelection), "All IndexedRegions overlapping text selection"); //$NON-NLS-1$
        model.releaseFromRead();
      }

      IEditorSite site = fTextEditor.getEditorSite();
      if (site != null) {
        IWorkbenchWindow window = site.getWorkbenchWindow();
        if (window != null) {
          ISelectionService service = window.getSelectionService();
          ISelection selectionFromService = service.getSelection();
          if (service != null && !selectionFromService.equals(structuredSelection)
              && selectionFromService instanceof IStructuredSelection) {
            TabItem selectionServiceTab = new TabItem(tabfolder, SWT.BORDER);
            selectionServiceTab.setText(SSEUIMessages.OffsetStatusLineContributionItem_19);
            Composite selectionServiceComposite = new Composite(tabfolder, SWT.NONE);
            selectionServiceTab.setControl(selectionServiceComposite);
            fillSelectionTabContents(selectionServiceComposite,
                ((IStructuredSelection) selectionFromService).toList(),
                "Class: " + selectionFromService.getClass().getName()); //$NON-NLS-1$
          }
        }
      }

      return composite;
    }

    /**
     * @param editorInput
     * @return
     */
    private String getBOMText(IEditorInput editorInput) {
      IFile file = (IFile) editorInput.getAdapter(IFile.class);
      String detectedBOM = "none"; //$NON-NLS-1$
      if (file != null) {
        InputStream s = null;
        try {
          s = file.getContents(true);
          if (s != null) {
            int b1 = s.read() & 0xFF;
            int b2 = s.read() & 0xFF;
            if (b1 == 0xFE && b2 == 0xFF) {
              detectedBOM = "FE FF (UTF-16BE)"; //$NON-NLS-1$
            } else if (b1 == 0xFF && b2 == 0xFE) {
              detectedBOM = "FF FE (UTF-16LE)"; //$NON-NLS-1$
            } else {
              int b3 = s.read() & 0xFF;
              if (b1 == 0xEF && b2 == 0xBB && b3 == 0xBF) {
                detectedBOM = "EF BB BF (UTF-8)"; //$NON-NLS-1$
              }
            }
          }
        } catch (Exception e) {
          detectedBOM = e.getMessage();
        } finally {
          if (s != null)
            try {
              s.close();
            } catch (IOException e) {
            }
        }
      } else {
        detectedBOM = "N/A"; //$NON-NLS-1$
      }
      return detectedBOM;
    }

    private List getIndexedRegions(ITextSelection textSelection) {
      Set overlappingIndexedRegions = new HashSet(2);
      int start = textSelection.getOffset();
      int end = start + textSelection.getLength();
      IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
          fDocument);
      if (model != null) {
        for (int i = start; i <= end; i++) {
          IndexedRegion r = model.getIndexedRegion(i);
          if (r != null) {
            overlappingIndexedRegions.add(r);
          }
        }
        model.releaseFromRead();
      }

      return Arrays.asList(overlappingIndexedRegions.toArray());
    }

    /**
     * @param sash
     */
    private void createPartitionTabContents(SashForm sash) {
      Composite partioningComposite = new Composite(sash, SWT.NONE);
      partioningComposite.setLayout(new GridLayout(2, false));
      partioningComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      Label label = new Label(partioningComposite, SWT.SINGLE);
      label.setText(SSEUIMessages.OffsetStatusLineContributionItem_8); //$NON-NLS-1$
      label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      final Combo partitioningCombo = new Combo(partioningComposite, SWT.READ_ONLY);
      partitioningCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      final Label partitionerInstanceLabel = new Label(partioningComposite, SWT.SINGLE);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 2;
      partitionerInstanceLabel.setLayoutData(gd);

      final TableViewer fPartitionTable = new TableViewer(partioningComposite, SWT.FULL_SELECTION);
      gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd.horizontalSpan = 2;
      fPartitionTable.getControl().setLayoutData(gd);
      fPartitionTable.setContentProvider(new ArrayContentProvider());
      fPartitionTable.getTable().setHeaderVisible(true);
      fPartitionTable.getTable().setLinesVisible(true);
      String[] columns = new String[] {
          SSEUIMessages.OffsetStatusLineContributionItem_9,
          SSEUIMessages.OffsetStatusLineContributionItem_10,
          SSEUIMessages.OffsetStatusLineContributionItem_11}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      fPartitionTable.setLabelProvider(new ITableLabelProvider() {
        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public Image getColumnImage(Object element, int columnIndex) {
          return null;
        }

        public String getColumnText(Object element, int columnIndex) {
          ITypedRegion partition = (ITypedRegion) element;
          String text = null;
          switch (columnIndex) {
            case 0:
              text = Integer.toString(partition.getOffset());
              break;
            case 1:
              text = Integer.toString(partition.getLength());
              break;
            case 2:
              text = partition.getType();
              break;
          }
          if (text == null)
            text = ""; //$NON-NLS-1$
          return text;
        }

        public boolean isLabelProperty(Object element, String property) {
          return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
      });
      TableLayout tlayout = new TableLayout();
      CellEditor[] cellEditors = new CellEditor[columns.length];
      int columnWidths[] = new int[] {
          Display.getCurrent().getBounds().width / 14, Display.getCurrent().getBounds().width / 14,
          Display.getCurrent().getBounds().width / 5};
      for (int i = 0; i < columns.length; i++) {
        tlayout.addColumnData(new ColumnWeightData(1));
        TableColumn tc = new TableColumn(fPartitionTable.getTable(), SWT.NONE);
        tc.setText(columns[i]);
        tc.setResizable(true);
        tc.setWidth(columnWidths[i]);
      }
      fPartitionTable.setCellEditors(cellEditors);
      fPartitionTable.setColumnProperties(columns);
      final String[] partitionings = (fDocument instanceof IDocumentExtension3)
          ? ((IDocumentExtension3) fDocument).getPartitionings()
          : new String[] {IDocumentExtension3.DEFAULT_PARTITIONING};
      partitioningCombo.setItems(partitionings);
      partitioningCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          ISelection sel = fTextEditor.getSelectionProvider().getSelection();
          ITextSelection textSelection = (ITextSelection) sel;
          try {
            String partitionerText = fDocument instanceof IDocumentExtension3
                ? ((IDocumentExtension3) fDocument).getDocumentPartitioner(
                    partitioningCombo.getItem(partitioningCombo.getSelectionIndex())).toString()
                : ("" + fDocument.getDocumentPartitioner()); //$NON-NLS-1$
            partitionerInstanceLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_13
                + partitionerText); //$NON-NLS-1$
            fPartitionTable.setInput(TextUtilities.computePartitioning(fDocument,
                partitioningCombo.getItem(partitioningCombo.getSelectionIndex()),
                textSelection.getOffset(), textSelection.getLength(), true));
          } catch (BadLocationException e1) {
            fPartitionTable.setInput(new ITypedRegion[0]);
          }
        }
      });
      try {
        if (partitionings.length > 0) {
          String selectedPartitioning = partitioningCombo.getItem(0);
          if (Utilities.contains(partitionings,
              IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING)) {
            selectedPartitioning = IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING;
            for (int i = 0; i < partitionings.length; i++) {
              if (partitionings[i].equals(IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING)) {
                partitioningCombo.select(i);
              }
            }
          } else {
            partitioningCombo.select(0);
          }
          ISelection sel = fTextEditor.getSelectionProvider().getSelection();
          ITextSelection textSelection = (ITextSelection) sel;
          ITypedRegion[] partitions = TextUtilities.computePartitioning(fDocument,
              selectedPartitioning, textSelection.getOffset(), textSelection.getLength(), true);
          fPartitionTable.setInput(partitions);
          String partitionerText = fDocument instanceof IDocumentExtension3
              ? ((IDocumentExtension3) fDocument).getDocumentPartitioner(
                  partitioningCombo.getItem(partitioningCombo.getSelectionIndex())).toString()
              : ("" + fDocument.getDocumentPartitioner()); //$NON-NLS-1$
          partitionerInstanceLabel.setText(SSEUIMessages.OffsetStatusLineContributionItem_13
              + partitionerText); //$NON-NLS-1$
        } else {
          ISelection sel = fTextEditor.getSelectionProvider().getSelection();
          ITextSelection textSelection = (ITextSelection) sel;
          fPartitionTable.setInput(fDocument.computePartitioning(textSelection.getOffset(),
              textSelection.getLength()));
        }
      } catch (BadLocationException e1) {
        fPartitionTable.setInput(new ITypedRegion[0]);
      }
      partitioningCombo.setFocus();

      final StyledText text = new StyledText(sash, SWT.MULTI | SWT.READ_ONLY);
      fPartitionTable.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          if (event.getSelection() instanceof IStructuredSelection) {
            IRegion partition = (IRegion) ((IStructuredSelection) event.getSelection()).getFirstElement();
            IDocument document = fTextEditor.getDocumentProvider().getDocument(
                fTextEditor.getEditorInput());
            String source;
            try {
              source = document.get(partition.getOffset(), partition.getLength());
              text.setEnabled(true);
              text.setText(source);
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
          }
        }
      });
      text.setEnabled(false);
    }

    /**
     * @param composite
     * @return
     */
    private Composite createRegionTabContents(SashForm sashForm) {
      ISelection sel = fTextEditor.getSelectionProvider().getSelection();
      final ITextSelection textSelection = (ITextSelection) sel;
      final List documentRegions = new ArrayList();
      if (fDocument instanceof IStructuredDocument) {
        IStructuredDocument structuredDocument = (IStructuredDocument) fDocument;
        int pos = textSelection.getOffset();
        int end = textSelection.getOffset() + textSelection.getLength();
        IStructuredDocumentRegion docRegion = structuredDocument.getRegionAtCharacterOffset(pos);
        IStructuredDocumentRegion endRegion = structuredDocument.getRegionAtCharacterOffset(end);
        if (pos < end) {
          while (docRegion != endRegion) {
            documentRegions.add(docRegion);
            docRegion = docRegion.getNext();
          }
        }
        documentRegions.add(docRegion);
      }

      final TreeViewer tree = new TreeViewer(sashForm, SWT.V_SCROLL | SWT.H_SCROLL);
      final String START = SSEUIMessages.OffsetStatusLineContributionItem_15; //$NON-NLS-1$
      final String LENGTH = SSEUIMessages.OffsetStatusLineContributionItem_16; //$NON-NLS-1$
      final String TEXTLENGTH = SSEUIMessages.OffsetStatusLineContributionItem_17; //$NON-NLS-1$
      final String CONTEXT = SSEUIMessages.OffsetStatusLineContributionItem_18; //$NON-NLS-1$
      tree.setContentProvider(new ITreeContentProvider() {
        public void dispose() {
        }

        public Object[] getChildren(Object parentElement) {
          List children = new ArrayList(0);
          if (parentElement instanceof ITextSelection) {
            children.addAll(documentRegions);
          }
          if (parentElement instanceof ITextRegionCollection) {
            children.add(((ITextRegionCollection) parentElement).getRegions().toArray());
          }
          if (parentElement instanceof ITextRegion) {
            children.add(new KeyValuePair(CONTEXT, ((ITextRegion) parentElement).getType()));
            children.add(new KeyValuePair(START,
                Integer.toString(((ITextRegion) parentElement).getStart())));
            children.add(new KeyValuePair(TEXTLENGTH,
                Integer.toString(((ITextRegion) parentElement).getTextLength())));
            children.add(new KeyValuePair(LENGTH,
                Integer.toString(((ITextRegion) parentElement).getLength())));
          }
          if (parentElement instanceof ITextRegionList) {
            children.add(Arrays.asList(((ITextRegionList) parentElement).toArray()));
          }
          if (parentElement instanceof Collection) {
            children.addAll((Collection) parentElement);
          }
          if (parentElement instanceof Object[]) {
            children.addAll(Arrays.asList((Object[]) parentElement));
          }
          return children.toArray();
        }

        public Object[] getElements(Object inputElement) {
          return documentRegions.toArray();
        }

        public Object getParent(Object element) {
          if (element instanceof IStructuredDocumentRegion)
            return ((IStructuredDocumentRegion) element).getParentDocument();
          if (element instanceof ITextRegionContainer) {
            return ((ITextRegionContainer) element).getParent();
          }
          return fDocument;
        }

        public boolean hasChildren(Object element) {
          return !(element instanceof KeyValuePair);
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
      });
      tree.setLabelProvider(new LabelProvider() {
        public String getText(Object element) {
          if (element instanceof KeyValuePair)
            return ((KeyValuePair) element).fKey.toString().toLowerCase()
                + ": " + ((KeyValuePair) element).fValue; //$NON-NLS-1$
          if (element instanceof IStructuredDocumentRegion) {
            IStructuredDocumentRegion documentRegion = (IStructuredDocumentRegion) element;
            int packageNameLength = documentRegion.getClass().getPackage().getName().length();
            if (packageNameLength > 0)
              packageNameLength++;
            String name = documentRegion.getClass().getName().substring(packageNameLength);
            String text = "[" + documentRegion.getStartOffset() + "-" + documentRegion.getEndOffset() + "] " + name + "@" + element.hashCode() + " " + documentRegion.getType(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            return text;
          }
          if (element instanceof ITextRegion) {
            ITextRegion textRegion = (ITextRegion) element;
            int packageNameLength = textRegion.getClass().getPackage().getName().length();
            if (packageNameLength > 0)
              packageNameLength++;
            String name = textRegion.getClass().getName().substring(packageNameLength);
            String text = "[" + textRegion.getStart() + "-" + textRegion.getEnd() + "] " + name + "@" + element.hashCode() + " " + textRegion.getType(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            return text;
          }
          return super.getText(element);
        }
      });
      tree.setInput(fDocument);

      final Text displayText = new Text(sashForm, SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY
          | SWT.BORDER);
      displayText.setBackground(sashForm.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
      tree.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          if (event.getSelection() instanceof IStructuredSelection) {
            Object o = ((IStructuredSelection) event.getSelection()).getFirstElement();
            if (o instanceof KeyValuePair)
              displayText.setText(((KeyValuePair) o).fValue.toString());
            else if (o instanceof ITextSelection) {
              ITextSelection text = (ITextSelection) o;
              try {
                displayText.setText(fDocument.get(text.getOffset(), text.getLength()));
              } catch (BadLocationException e) {
                displayText.setText(""); //$NON-NLS-1$
              }
            } else if (o instanceof ITextRegionCollection) {
              ITextRegionCollection region = (ITextRegionCollection) o;
              displayText.setText(region.getFullText());
            } else
              displayText.setText("" + o); //$NON-NLS-1$
          }
        }
      });
      return sashForm;
    }

    private void fillSelectionTabContents(Composite area, List selection, String description) {
      area.setLayout(new GridLayout());
      area.setLayoutData(new GridData());

      Label typeName = new Label(area, SWT.WRAP);
      typeName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      typeName.setText(description); //$NON-NLS-1$

      (new Label(area, SWT.NONE)).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
      SashForm structuredSashForm = new SashForm(area, SWT.NONE);
      structuredSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      structuredSashForm.setOrientation(SWT.VERTICAL);

      final TableViewer structuredSelectionTable = new TableViewer(structuredSashForm,
          SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

      structuredSelectionTable.getTable().setHeaderVisible(true);
      structuredSelectionTable.getTable().setLinesVisible(true);
      structuredSelectionTable.setSorter(new ViewerSorter() {
        public int category(Object element) {
          if (element instanceof IndexedRegion)
            return ((IndexedRegion) element).getStartOffset();
          return super.category(element);
        }
      });

      structuredSelectionTable.setLabelProvider(new ITableLabelProvider() {
        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public Image getColumnImage(Object element, int columnIndex) {
          if (element instanceof INodeNotifier) {
            INodeAdapter adapterFor = ((INodeNotifier) element).getAdapterFor(IJFaceNodeAdapter.class);
            if (columnIndex == 2 && adapterFor != null && adapterFor instanceof IJFaceNodeAdapter) {
              IJFaceNodeAdapter adapter = (IJFaceNodeAdapter) adapterFor;
              return adapter.getLabelImage((element));
            }
          }
          return null;
        }

        public String getColumnText(Object element, int columnIndex) {
          String text = null;
          if (element != null) {
            switch (columnIndex) {
              case 0: {
                text = String.valueOf(((List) structuredSelectionTable.getInput()).indexOf(element));
              }
                break;
              case 1: {
                text = element.getClass().getName();
              }
                break;
              case 2: {
                text = StringUtils.firstLineOf(element.toString());
              }
                break;
              default:
                text = ""; //$NON-NLS-1$
            }
          }
          return text;
        }

        public boolean isLabelProperty(Object element, String property) {
          return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
      });

      TableLayout tlayout = new TableLayout();
      tlayout.addColumnData(new ColumnWeightData(7, true));
      tlayout.addColumnData(new ColumnWeightData(28, true));
      tlayout.addColumnData(new ColumnWeightData(50, true));
      structuredSelectionTable.getTable().setLayout(tlayout);

      TableColumn tc = new TableColumn(structuredSelectionTable.getTable(), SWT.NONE);
      tc.setText("Item"); //$NON-NLS-1$
      tc.setResizable(true);
      tc.setWidth(40);

      tc = new TableColumn(structuredSelectionTable.getTable(), SWT.NONE);
      tc.setText("Class"); //$NON-NLS-1$
      tc.setResizable(true);
      tc.setWidth(40);

      tc = new TableColumn(structuredSelectionTable.getTable(), SWT.NONE);
      tc.setText("Value"); //$NON-NLS-1$
      tc.setResizable(true);
      tc.setWidth(40);

      structuredSelectionTable.setContentProvider(new ArrayContentProvider());
      final List input = selection;
      structuredSelectionTable.setInput(input);

      final TreeViewer infoTree = new TreeViewer(structuredSashForm, SWT.H_SCROLL | SWT.V_SCROLL
          | SWT.BORDER);
      infoTree.setLabelProvider(new LabelProvider() {
        public Image getImage(Object element) {
          if (element instanceof TreeViewer && infoTree.getInput() instanceof INodeNotifier) {
            INodeAdapter adapterFor = ((INodeNotifier) infoTree.getInput()).getAdapterFor(IJFaceNodeAdapter.class);
            if (adapterFor != null && adapterFor instanceof IJFaceNodeAdapter) {
              IJFaceNodeAdapter adapter = (IJFaceNodeAdapter) adapterFor;
              return adapter.getLabelImage((infoTree.getInput()));
            }
          }
          return super.getImage(element);
        }

        public String getText(Object element) {
          if (element instanceof Class) {
            return "Class: " + ((Class) element).getName(); //$NON-NLS-1$
          }
          if (element instanceof Collection) {
            return "Registered Adapters:"; //$NON-NLS-1$
          }
          if (element instanceof IRegion) {
            return "Indexed Region offset span: [" + ((IRegion) element).getOffset() + "-" + ((IRegion) element).getLength() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          }
          if (element instanceof TreeViewer && infoTree.getInput() instanceof INodeNotifier) {
            IJFaceNodeAdapter adapter = (IJFaceNodeAdapter) ((INodeNotifier) infoTree.getInput()).getAdapterFor(IJFaceNodeAdapter.class);
            if (adapter != null) {
              return adapter.getLabelText((infoTree.getInput()));
            }
          }
          return super.getText(element);
        }
      });
      infoTree.setContentProvider(new ITreeContentProvider() {
        public void dispose() {
        }

        public Object[] getChildren(Object parentElement) {
          if (parentElement instanceof Collection)
            return ((Collection) parentElement).toArray();
          return new Object[0];
        }

        public Object[] getElements(Object inputElement) {
          List elements = new ArrayList(4);
          if (inputElement != null) {
            if (inputElement instanceof INodeNotifier
                && ((INodeNotifier) inputElement).getAdapterFor(IJFaceNodeAdapter.class) != null) {
              elements.add(infoTree);
            }
            elements.add(inputElement.getClass());
            if (inputElement instanceof IndexedRegion) {
              elements.add(new Region(((IndexedRegion) inputElement).getStartOffset(),
                  ((IndexedRegion) inputElement).getEndOffset()));
            }
            if (inputElement instanceof INodeNotifier) {
              elements.add(((INodeNotifier) inputElement).getAdapters());
            }
          }
          return elements.toArray();
        }

        public Object getParent(Object element) {
          return null;
        }

        public boolean hasChildren(Object element) {
          return element instanceof Collection;
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
      });

      structuredSelectionTable.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          int selectionIndex = structuredSelectionTable.getTable().getSelectionIndex();
          if (selectionIndex != -1) {
            infoTree.setInput(structuredSelectionTable.getElementAt(selectionIndex));
          } else {
            infoTree.setInput(event.getSelectionProvider().getSelection());
          }
          infoTree.expandToLevel(2);
        }
      });

      structuredSashForm.setWeights(new int[] {3, 2});
    }

    private String getLineNumber(Annotation annotation) {
      int line = -1;
      if (annotation instanceof MarkerAnnotation) {
        line = MarkerUtilities.getLineNumber(((MarkerAnnotation) annotation).getMarker());//$NON-NLS-1$
      } else {
        IAnnotationModel annotationModel = fTextEditor.getDocumentProvider().getAnnotationModel(
            fTextEditor.getEditorInput());
        Position p = annotationModel.getPosition(annotation);
        if (p != null && !p.isDeleted()) {
          try {
            // don't forget the +1
            line = fDocument.getLineOfOffset(p.getOffset()) + 1;
          } catch (BadLocationException e) {
            return e.getMessage();
          }
        }
      }
      return Integer.toString(line);
    }
  }

  static class KeyValuePair {
    Object fKey;
    String fValue;

    public KeyValuePair(Object key, String value) {
      fKey = key;
      fValue = value;
    }
  }

  class ShowEditorInformationAction extends Action {
    public ShowEditorInformationAction() {
      super();
    }

    public void run() {
      /**
       * TODO: Provide a more useful control, maybe a table where the selection shows you the
       * partition's text in a StyledText pane beneath it.
       */
      super.run();
      new InformationDialog(((Control) fTextEditor.getAdapter(Control.class)).getShell()).open();
    }
  }

  IAction fShowEditorInformationAction = new ShowEditorInformationAction();

  ITextEditor fTextEditor = null;

  /**
   * @param id
   */
  public OffsetStatusLineContributionItem(String id) {
    super(id);
    setToolTipText("Double-click for more information");
  }

  /**
   * @param id
   * @param visible
   * @param widthInChars
   */
  public OffsetStatusLineContributionItem(String id, boolean visible, int widthInChars) {
    super(id, visible, widthInChars);
    setToolTipText("Double-click for more information");
  }

  public void setActiveEditor(ITextEditor textEditor) {
    fTextEditor = textEditor;
    setActionHandler(fShowEditorInformationAction);
  }
}
