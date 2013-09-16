/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.preferences;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalCategory;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalComputerRegistry;
import org.eclipse.wst.sse.ui.internal.util.SWTUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A helpful preference configuration block implementation for allowing a user to set their
 * preferences for which content assist categories to show on the default content assist page as
 * well as on their own separate page and page ordering for a specific content type
 * </p>
 * 
 * @base org.eclipse.jdt.internal.ui.preferences.CodeAssistAdvancedConfigurationBlock
 */
public final class CodeAssistCyclingConfigurationBlock {

  /**
   * <p>
   * Used to compare categories based on their assigned page rank
   * </p>
   */
  private final Comparator fCategoryPageComparator = new Comparator() {
    private int getRank(Object o) {
      return ((ModelElement) o).getOwnPageRank();
    }

    public int compare(Object o1, Object o2) {
      int result = getRank(o1) - getRank(o2);
      if (result == 0) {
        result = ((ModelElement) o1).getId().compareTo(((ModelElement) o2).getId());
      }
      return result;
    }
  };

  /**
   * <p>
   * Used to compare categories based on their assigned default page rank
   * </p>
   */
  private final Comparator fCategoryDefaultPageComparator = new Comparator() {
    private int getRank(Object o) {
      return ((ModelElement) o).getDefaultPageRank();
    }

    public int compare(Object o1, Object o2) {
      int result = getRank(o1) - getRank(o2);
      if (result == 0) {
        result = ((ModelElement) o1).getId().compareTo(((ModelElement) o2).getId());
      }
      return result;
    }
  };

  /** the preference model for this block */
  private final PreferenceModel fModel;

  /**
   * <code>{@link Map}<{@link ImageDescriptor}, {@link Image}></code>
   */
  private final Map fImages = new HashMap();

  /** table viewer to configure which categories are displayed on the default page */
  private CheckboxTableViewer fDefaultPageViewer;

  /**
   * table viewer to configure which categories are displayed on their own page, as well as their
   * ordering
   */
  private CheckboxTableViewer fOwnPageViewer;

  /** categories pages sort order up button */
  private Button fPageOrderUpButton;

  /** categories pages sort order down button */
  private Button fPageOrderDownButton;

  /** categories default page sort order up button */
  private Button fDefaultPageOrderUpButton;

  /** categories default page sort order down button */
  private Button fDefaultPageOrderDownButton;

  /** The content type ID this configuration block is for */
  private String fContentTypeID;

  /** The writable categories configuration */
  private ICompletionProposalCategoriesConfigurationWriter fConfigurationWriter;

  /**
   * <p>
   * Creates a new content assist preference block for the given content type using the given
   * configuration writer
   * </p>
   * 
   * @param contentTypeID content type this content assist preference block is for
   * @param configurationWriter {@link ICompletionProposalCategoriesConfigurationWriter} used to
   *          read and write the user preferences
   */
  public CodeAssistCyclingConfigurationBlock(String contentTypeID,
      ICompletionProposalCategoriesConfigurationWriter configurationWriter) {
    this.fContentTypeID = contentTypeID;
    this.fConfigurationWriter = configurationWriter;

    List categories = CompletionProposalComputerRegistry.getDefault().getProposalCategories(
        this.fContentTypeID);
    this.fModel = new PreferenceModel(categories);
  }

  /**
   * <p>
   * Saves the user configuration
   * </p>
   * 
   * @return <code>true</code> if store was successful, <code>false</code> otherwise
   */
  public boolean storeValues() {
    return this.fConfigurationWriter.saveConfiguration();
  }

  /**
   * <p>
   * Loads the preference defaults
   * </p>
   */
  public void performDefaults() {
    this.fConfigurationWriter.loadDefaults();
    this.initializeValues();
    this.fModel.performDefaults();
  }

  /**
   * <p>
   * Disposes of this preference block
   * </p>
   */
  public void dispose() {
    for (Iterator it = fImages.values().iterator(); it.hasNext();) {
      Image image = (Image) it.next();
      image.dispose();
    }
  }

  /**
   * <p>
   * Creates the contents of this configuration block using a {@link Group} if the given
   * <code>groupTitle</code> is not <code>null</code> else creates it as a composite and in either
   * case adds itself to the given parent
   * </p>
   * 
   * @param parent {@link Composite} parent to add this configuration block to
   * @param groupTitle Title to use for the configuration block group, if <code>null</code> then a
   *          {@link Composite} will be used instead of a {@link Group}
   * @return the created configuration block that has already been added to the parent
   */
  public Control createContents(Composite parent, String groupTitle) {
    Composite container;
    if (groupTitle != null) {
      container = new Group(parent, SWT.NULL);
      ((Group) container).setText(groupTitle);
    } else {
      container = new Composite(parent, SWT.NULL);
    }
    int columns = 2;
    GridLayout layout = new GridLayout(columns, false);
    container.setLayout(layout);

    GridData data = new GridData(GridData.FILL);
    data.horizontalIndent = 0;
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    container.setLayoutData(data);

    createDefaultPageLabel(container, columns);
    createDefaultPageSection(container);

    createFiller(container, columns);

    createOwnPageLabel(container, columns);
    createOwnPageSection(container);

    createFiller(container, columns);

    if (fModel.pageElements.size() > 0) {
      fDefaultPageViewer.getTable().select(0);
      fOwnPageViewer.getTable().select(0);
      handleOwnPageTableSelection();
      handleDefaultPageTableSelection();
    }

    return container;
  }

  /**
   * <p>
   * Initialize the values of the configuration block
   * </p>
   */
  public void initializeValues() {
    updateCheckedState();
    fDefaultPageViewer.refresh();
    fOwnPageViewer.refresh();
    handleOwnPageTableSelection();
    handleDefaultPageTableSelection();
  }

  private void createDefaultPageSection(Composite composite) {
    createDefaultPageViewer(composite);
    createDefaultPageButtonList(composite);
  }

  private void createDefaultPageLabel(Composite composite, int h_span) {
    final ICommandService commandSvc = (ICommandService) PlatformUI.getWorkbench().getAdapter(
        ICommandService.class);
    final Command command = commandSvc.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    ParameterizedCommand pCmd = new ParameterizedCommand(command, null);
    String key = getKeyboardShortcut(pCmd);
    if (key == null) {
      key = SSEUIMessages.CodeAssistAdvancedConfigurationBlock_no_shortcut;
    }

    PixelConverter pixelConverter = new PixelConverter(composite);
    int width = pixelConverter.convertWidthInCharsToPixels(40);

    Label label = new Label(composite, SWT.NONE | SWT.WRAP);
    label.setText(NLS.bind(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_page_description,
        new Object[] {key}));
    GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false, h_span, 1);
    gd.widthHint = width;
    label.setLayoutData(gd);

    createFiller(composite, h_span);

    label = new Label(composite, SWT.NONE | SWT.WRAP);
    label.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_default_table_description);
    gd = new GridData(GridData.FILL, GridData.FILL, true, false, h_span, 1);
    gd.widthHint = width;
    label.setLayoutData(gd);
  }

  private void createDefaultPageViewer(Composite composite) {
    fDefaultPageViewer = CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER);
    Table table = fDefaultPageViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(false);
    table.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));

    TableColumn nameColumn = new TableColumn(table, SWT.NONE);
    nameColumn.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_default_table_category_column_title);
    nameColumn.setResizable(false);

    fDefaultPageViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        boolean checked = event.getChecked();
        ModelElement element = (ModelElement) event.getElement();
        element.setShouldDisplayOnDefaultPage(checked);
      }
    });

    fDefaultPageViewer.setContentProvider(new ArrayContentProvider());

    DefaultPageTableLabelProvider labelProvider = new DefaultPageTableLabelProvider();
    fDefaultPageViewer.setLabelProvider(labelProvider);
    fDefaultPageViewer.setInput(fModel.defaultPageElements);
    fDefaultPageViewer.setComparator(new ModelViewerComparator(fCategoryDefaultPageComparator));

    final int ICON_AND_CHECKBOX_WITH = 50;
    final int HEADER_MARGIN = 20;
    int minNameWidth = computeWidth(table, nameColumn.getText()) + HEADER_MARGIN;
    for (int i = 0; i < fModel.defaultPageElements.size(); i++) {
      minNameWidth = Math.max(minNameWidth,
          computeWidth(table, labelProvider.getColumnText(fModel.defaultPageElements.get(i), 0))
              + ICON_AND_CHECKBOX_WITH);
    }

    nameColumn.setWidth(minNameWidth);

    table.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleDefaultPageTableSelection();
      }
    });
  }

  /**
   * <p>
   * Create the Up and Down buttons for the default page viewer
   * </p>
   * 
   * @param parent {@link Composite} to add the button list to
   */
  private void createDefaultPageButtonList(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);

    fDefaultPageOrderUpButton = new Button(composite, SWT.PUSH | SWT.CENTER);
    fDefaultPageOrderUpButton.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_Up);
    fDefaultPageOrderUpButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int index = fDefaultPageViewer.getTable().getSelectionIndex();
        if (index != -1) {
          fModel.moveDefaultPageCategoryUp(index);
          fDefaultPageViewer.refresh();
          handleDefaultPageTableSelection();
        }
      }
    });
    fDefaultPageOrderUpButton.setLayoutData(new GridData());

    SWTUtil.setButtonDimensionHint(fDefaultPageOrderUpButton);

    fDefaultPageOrderDownButton = new Button(composite, SWT.PUSH | SWT.CENTER);
    fDefaultPageOrderDownButton.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_Down);
    fDefaultPageOrderDownButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int index = fDefaultPageViewer.getTable().getSelectionIndex();
        if (index != -1) {
          fModel.moveDefaultPageCategoryDown(index);
          fDefaultPageViewer.refresh();
          handleDefaultPageTableSelection();
        }
      }
    });
    fDefaultPageOrderDownButton.setLayoutData(new GridData());
    SWTUtil.setButtonDimensionHint(fDefaultPageOrderDownButton);
  }

  private void createFiller(Composite composite, int h_span) {
    Label filler = new Label(composite, SWT.NONE);
    filler.setVisible(false);
    filler.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, h_span, 1));
  }

  private void createOwnPageLabel(Composite composite, int h_span) {
    PixelConverter pixelConverter = new PixelConverter(composite);
    int width = pixelConverter.convertWidthInCharsToPixels(40);

    Label label = new Label(composite, SWT.NONE | SWT.WRAP);
    label.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_separate_table_description);
    GridData gd = new GridData(GridData.FILL, GridData.FILL, false, false, h_span, 1);
    gd.widthHint = width;
    label.setLayoutData(gd);
  }

  private void createOwnPageSection(Composite composite) {
    createOwnPageViewer(composite);
    createOwnPageButtonList(composite);
  }

  private void createOwnPageViewer(Composite composite) {
    fOwnPageViewer = CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER);
    Table table = fOwnPageViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(false);
    table.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 1, 1));

    TableColumn nameColumn = new TableColumn(table, SWT.NONE);
    nameColumn.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_separate_table_category_column_title);
    nameColumn.setResizable(false);

    fOwnPageViewer.setContentProvider(new ArrayContentProvider());

    ITableLabelProvider labelProvider = new OwnPageTableLabelProvider();
    fOwnPageViewer.setLabelProvider(labelProvider);
    fOwnPageViewer.setInput(fModel.pageElements);
    fOwnPageViewer.setComparator(new ModelViewerComparator(fCategoryPageComparator));

    final int ICON_AND_CHECKBOX_WITH = 50;
    final int HEADER_MARGIN = 20;
    int minNameWidth = computeWidth(table, nameColumn.getText()) + HEADER_MARGIN;
    for (int i = 0; i < fModel.pageElements.size(); i++) {
      minNameWidth = Math.max(minNameWidth,
          computeWidth(table, labelProvider.getColumnText(fModel.pageElements.get(i), 0))
              + ICON_AND_CHECKBOX_WITH);
    }

    nameColumn.setWidth(minNameWidth);

    fOwnPageViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        boolean checked = event.getChecked();
        ModelElement element = (ModelElement) event.getElement();
        element.setShouldDisplayOnOwnPage(checked);
      }
    });

    table.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleOwnPageTableSelection();
      }
    });

  }

  /**
   * <p>
   * Create the Up and Down buttons for the own page viewer
   * </p>
   * 
   * @param parent {@link Composite} to add the button list to
   */
  private void createOwnPageButtonList(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);

    fPageOrderUpButton = new Button(composite, SWT.PUSH | SWT.CENTER);
    fPageOrderUpButton.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_PagesUp);
    fPageOrderUpButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int index = fOwnPageViewer.getTable().getSelectionIndex();
        if (index != -1) {
          fModel.movePageUp(index);
          fOwnPageViewer.refresh();
          handleOwnPageTableSelection();
        }
      }
    });
    fPageOrderUpButton.setLayoutData(new GridData());

    SWTUtil.setButtonDimensionHint(fPageOrderUpButton);

    fPageOrderDownButton = new Button(composite, SWT.PUSH | SWT.CENTER);
    fPageOrderDownButton.setText(SSEUIMessages.CodeAssistAdvancedConfigurationBlock_PagesDown);
    fPageOrderDownButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int index = fOwnPageViewer.getTable().getSelectionIndex();
        if (index != -1) {
          fModel.movePageDown(index);
          fOwnPageViewer.refresh();
          handleOwnPageTableSelection();
        }
      }
    });
    fPageOrderDownButton.setLayoutData(new GridData());
    SWTUtil.setButtonDimensionHint(fPageOrderDownButton);
  }

  /**
   * <p>
   * Update the enablement of the Up and Down buttons for the own page table viewer
   * </p>
   */
  private void handleOwnPageTableSelection() {
    ModelElement item = (ModelElement) ((IStructuredSelection) fOwnPageViewer.getSelection()).getFirstElement();
    if (item != null) {
      int index = fOwnPageViewer.getTable().getSelectionIndex();
      fPageOrderUpButton.setEnabled(index > 0);
      fPageOrderDownButton.setEnabled(index < fModel.pageElements.size() - 1);
    } else {
      fPageOrderUpButton.setEnabled(false);
      fPageOrderDownButton.setEnabled(false);
    }
  }

  /**
   * <p>
   * Update the enablement of the Up and Down buttons for the default page table viewer
   * </p>
   */
  private void handleDefaultPageTableSelection() {
    ModelElement item = (ModelElement) ((IStructuredSelection) fDefaultPageViewer.getSelection()).getFirstElement();
    if (item != null) {
      int index = fDefaultPageViewer.getTable().getSelectionIndex();
      fDefaultPageOrderUpButton.setEnabled(index > 0);
      fDefaultPageOrderDownButton.setEnabled(index < fModel.defaultPageElements.size() - 1);
    } else {
      fDefaultPageOrderUpButton.setEnabled(false);
      fDefaultPageOrderDownButton.setEnabled(false);
    }
  }

  private void updateCheckedState() {
    /*
     * does not matter which set of elements we use here because order does not matter in this case
     */
    final int size = fModel.pageElements.size();
    List defaultChecked = new ArrayList(size);
    List separateChecked = new ArrayList(size);

    for (Iterator it = fModel.pageElements.iterator(); it.hasNext();) {
      ModelElement element = (ModelElement) it.next();
      if (element.shouldDisplayOnDefaultPage())
        defaultChecked.add(element);
      if (element.shouldDisplayOnOwnPage())
        separateChecked.add(element);
    }

    fDefaultPageViewer.setCheckedElements(defaultChecked.toArray(new Object[defaultChecked.size()]));
    fOwnPageViewer.setCheckedElements(separateChecked.toArray(new Object[separateChecked.size()]));
  }

  private int computeWidth(Control control, String name) {
    if (name == null)
      return 0;
    GC gc = new GC(control);
    try {
      gc.setFont(JFaceResources.getDialogFont());
      return gc.stringExtent(name).x + 10;
    } finally {
      gc.dispose();
    }
  }

  private static BindingManager fgLocalBindingManager;
  static {
    fgLocalBindingManager = new BindingManager(new ContextManager(), new CommandManager());
    final IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getService(
        IBindingService.class);
    final Scheme[] definedSchemes = bindingService.getDefinedSchemes();
    if (definedSchemes != null) {
      try {
        for (int i = 0; i < definedSchemes.length; i++) {
          final Scheme scheme = definedSchemes[i];
          final Scheme copy = fgLocalBindingManager.getScheme(scheme.getId());
          copy.define(scheme.getName(), scheme.getDescription(), scheme.getParentId());
        }
      } catch (final NotDefinedException e) {
        Logger.logException(e);
      }
    }
    fgLocalBindingManager.setLocale(bindingService.getLocale());
    fgLocalBindingManager.setPlatform(bindingService.getPlatform());
  }

  private static String getKeyboardShortcut(ParameterizedCommand command) {
    IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(
        IBindingService.class);
    fgLocalBindingManager.setBindings(bindingService.getBindings());
    try {
      Scheme activeScheme = bindingService.getActiveScheme();
      if (activeScheme != null)
        fgLocalBindingManager.setActiveScheme(activeScheme);
    } catch (NotDefinedException e) {
      Logger.logException(e);
    }

    TriggerSequence[] bindings = fgLocalBindingManager.getActiveBindingsDisregardingContextFor(command);
    if (bindings.length > 0)
      return bindings[0].format();
    return null;
  }

  /**
   * <p>
   * Gets and image based on an image descriptor, and stores the image so it does not have to be
   * created more then once
   * </p>
   * 
   * @param imgDesc {@link ImageDescriptor} to get the {@link Image} for
   * @return {@link Image} created from the {@link ImageDescriptor}, or stored {@link Image}
   *         associated with the given {@link ImageDescriptor} if an {@link Image} had already been
   *         created for the given {@link ImageDescriptor}
   */
  private Image getImage(ImageDescriptor imgDesc) {
    if (imgDesc == null)
      return null;

    Image img = (Image) fImages.get(imgDesc);
    if (img == null) {
      img = imgDesc.createImage(false);
      fImages.put(imgDesc, img);
    }
    return img;
  }

  /**
   * <p>
   * Label provider for the table for configuring which categories should be displayed on the
   * default assist page
   * </p>
   */
  private final class DefaultPageTableLabelProvider extends LabelProvider implements
      ITableLabelProvider {
    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0)
        return ((ModelElement) element).getImage();
      return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
      switch (columnIndex) {
        case 0:
          return ((ModelElement) element).getName();
        default:
          Assert.isTrue(false);
          return null;
      }
    }

    /**
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
      return getColumnText(element, 0); // needed to make the sorter work
    }
  }

  /**
   * <p>
   * Label provider for the table for configuring which categories should be displayed on their own
   * content assist page
   * </p>
   */
  private final class OwnPageTableLabelProvider extends LabelProvider implements
      ITableLabelProvider {
    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0)
        return ((ModelElement) element).getImage();
      return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
      switch (columnIndex) {
        case 0:
          return ((ModelElement) element).getName();
        default:
          Assert.isTrue(false);
          return null;
      }
    }
  }

  /**
   * <p>
   * PreferenceModel used to read and write the user preferences using the
   * {@link ICompletionProposalCategoriesConfigurationWriter}
   * </p>
   */
  private final class PreferenceModel {
    /** private modifiable page element list */
    private final List fPageElements;

    /** public unmodifiable page element list */
    final List pageElements;

    /** private modifiable default page element list */
    private final List fDefaultPageElements;

    /** public unmodifiable default page element list */
    final List defaultPageElements;

    /**
     * <p>
     * Create the preference model for the given categories
     * </p>
     * 
     * @param categories <code>{@link List}<{@link CompletionProposalCategory}></code>
     */
    public PreferenceModel(List categories) {
      //need separate lists because they will be ordered differently
      this.fPageElements = new ArrayList();
      this.fDefaultPageElements = new ArrayList();
      for (Iterator it = categories.iterator(); it.hasNext();) {
        CompletionProposalCategory category = (CompletionProposalCategory) it.next();
        if (category.hasComputers()) {
          ModelElement elem = new ModelElement(category);
          fPageElements.add(elem);
          fDefaultPageElements.add(elem);
        }
      }
      //sort the lists
      this.performDefaults();

      pageElements = Collections.unmodifiableList(fPageElements);
      defaultPageElements = Collections.unmodifiableList(fDefaultPageElements);
    }

    /**
     * <p>
     * Move the model element specified by the given index up in the content assist page order
     * </p>
     * 
     * @param elementIndex the index of the model element to move up
     */
    public void movePageUp(int elementIndex) {
      Object removed = fPageElements.remove(elementIndex);
      fPageElements.add(elementIndex - 1, removed);

      fConfigurationWriter.setPageOrder(getPageOrderedCategoryIDs());
    }

    /**
     * <p>
     * Move the model element specified by the given index down in the content assist page order
     * </p>
     * 
     * @param elementIndex the index of the model element to move up
     */
    public void movePageDown(int elementIndex) {
      Object removed = fPageElements.remove(elementIndex);
      fPageElements.add(elementIndex + 1, removed);

      fConfigurationWriter.setPageOrder(getPageOrderedCategoryIDs());
    }

    /**
     * <p>
     * Move the model element specified by the given index up in the content assist page order
     * </p>
     * 
     * @param elementIndex the index of the model element to move up
     */
    public void moveDefaultPageCategoryUp(int elementIndex) {
      Object removed = fDefaultPageElements.remove(elementIndex);
      fDefaultPageElements.add(elementIndex - 1, removed);

      fConfigurationWriter.setDefaultPageOrder(getDefaultPageOrderedCategoryIDs());
    }

    /**
     * <p>
     * Move the model element specified by the given index down in the content assist page order
     * </p>
     * 
     * @param elementIndex the index of the model element to move up
     */
    public void moveDefaultPageCategoryDown(int elementIndex) {
      Object removed = fDefaultPageElements.remove(elementIndex);
      fDefaultPageElements.add(elementIndex + 1, removed);

      fConfigurationWriter.setDefaultPageOrder(getDefaultPageOrderedCategoryIDs());
    }

    /**
     * @return <code>{@link List}<{@link String}></code> - List of category IDs by page order
     */
    private List getPageOrderedCategoryIDs() {
      List ordered = new ArrayList(pageElements.size());
      for (int i = 0; i < pageElements.size(); ++i) {
        ordered.add(((ModelElement) pageElements.get(i)).getId());
      }

      return ordered;
    }

    /**
     * @return <code>{@link List}<{@link String}></code> - List of category IDs by default page
     *         order
     */
    private List getDefaultPageOrderedCategoryIDs() {
      List ordered = new ArrayList(defaultPageElements.size());
      for (int i = 0; i < defaultPageElements.size(); ++i) {
        ordered.add(((ModelElement) defaultPageElements.get(i)).getId());
      }

      return ordered;
    }

    /**
     * <p>
     * need to re-sort the lists after performing defaults
     * </p>
     */
    public void performDefaults() {
      Collections.sort(fPageElements, fCategoryPageComparator);
      Collections.sort(fDefaultPageElements, fCategoryDefaultPageComparator);
    }
  }

  /**
   * <p>
   * Wraps a {@link CompletionProposalCategory} for use in the {@link PreferenceModel}
   * </p>
   */
  private final class ModelElement {
    /** The wrapped category */
    private final CompletionProposalCategory fCategory;

    /**
     * <p>
     * Create a new model element wrapping the given category
     * </p>
     * 
     * @param category {@link CompletionProposalCategory} to be wrapped by this model element for
     *          use in the {@link PreferenceModel}
     */
    ModelElement(CompletionProposalCategory category) {
      fCategory = category;
    }

    /**
     * @return {@link Image} associated with the wrapped category
     */
    Image getImage() {
      return CodeAssistCyclingConfigurationBlock.this.getImage(fCategory.getImageDescriptor());
    }

    /**
     * @return name of the wrapped category
     */
    String getName() {
      return fCategory.getDisplayName();
    }

    String getId() {
      return fCategory.getId();
    }

    /**
     * @return <code>true</code> if the wrapped category should be displayed on the default content
     *         assist page, <code>false</code> otherwise
     */
    boolean shouldDisplayOnDefaultPage() {
      return fConfigurationWriter.shouldDisplayOnDefaultPage(this.getId());
    }

    /**
     * @param included <code>true</code> if the wrapped category should be displayed on the default
     *          content assist page, <code>false</code> otherwise
     */
    void setShouldDisplayOnDefaultPage(boolean included) {
      fConfigurationWriter.setShouldDisplayOnDefaultPage(this.getId(), included);
    }

    /**
     * @return <code>true</code> if the wrapped category should be displayed on the its own content
     *         assist page, <code>false</code> otherwise
     */
    boolean shouldDisplayOnOwnPage() {
      return fConfigurationWriter.shouldDisplayOnOwnPage(this.getId());
    }

    /**
     * @param shouldDisplay <code>true</code> if the wrapped category should be displayed on the its
     *          own content assist page, <code>false</code> otherwise
     */
    void setShouldDisplayOnOwnPage(boolean shouldDisplay) {
      fConfigurationWriter.setShouldDisplayOnOwnPage(this.getId(), shouldDisplay);
    }

    /**
     * @return the wrapped categories content assist page sort rank compared to the other categories
     */
    int getOwnPageRank() {
      return fConfigurationWriter.getPageSortOrder(this.getId());
    }

    /**
     * @return the wrapped categories content assist page sort rank compared to the other categories
     */
    int getDefaultPageRank() {
      return fConfigurationWriter.getDefaultPageSortOrder(this.getId());
    }
  }

  private class ModelViewerComparator extends ViewerComparator {
    /**
		 * 
		 */
    public ModelViewerComparator(Comparator comparator) {
      super(comparator);
    }

    /**
     * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    public int compare(Viewer viewer, Object e1, Object e2) {
      return this.getComparator().compare(e1, e2);
    }
  }
}
