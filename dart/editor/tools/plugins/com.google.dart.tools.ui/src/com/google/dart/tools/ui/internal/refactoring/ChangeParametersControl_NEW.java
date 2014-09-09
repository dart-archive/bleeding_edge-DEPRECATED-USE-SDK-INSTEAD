/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.common.collect.Lists;
import com.google.dart.server.generated.types.RefactoringMethodParameter;
import com.google.dart.tools.ui.internal.dialogs.TableTextCellEditor;
import com.google.dart.tools.ui.internal.dialogs.TextFieldNavigationHandler;
import com.google.dart.tools.ui.internal.refactoring.contentassist.VariableNamesProcessor;
import com.google.dart.tools.ui.internal.util.ControlContentAssistHelper;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.util.TableLayoutComposite;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.contentassist.ContentAssistHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A special control to edit and reorder method parameters.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
@SuppressWarnings("deprecation")
public class ChangeParametersControl_NEW extends Composite {

  public static class Mode {
    private final String fName;
    public static final Mode EXTRACT_METHOD = new Mode("EXTRACT_METHOD"); //$NON-NLS-1$
    public static final Mode CHANGE_METHOD_SIGNATURE = new Mode("CHANGE_METHOD_SIGNATURE"); //$NON-NLS-1$
    public static final Mode INTRODUCE_PARAMETER = new Mode("INTRODUCE_PARAMETER"); //$NON-NLS-1$

    private Mode(String name) {
      fName = name;
    }

    public boolean canAddParameters() {
      return this == Mode.CHANGE_METHOD_SIGNATURE;
    }

    public boolean canChangeDefault() {
      return this == Mode.CHANGE_METHOD_SIGNATURE;
    }

    public boolean canChangeTypes() {
      return this == EXTRACT_METHOD || this == CHANGE_METHOD_SIGNATURE;
    }

    @Override
    public String toString() {
      return fName;
    }
  }

  private static class ParameterInfoContentProvider implements IStructuredContentProvider {
    @Override
    public void dispose() {
      // do nothing
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {
      List<RefactoringMethodParameter> parameters = (List<RefactoringMethodParameter>) inputElement;
      return parameters.toArray(new RefactoringMethodParameter[parameters.size()]);
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // do nothing
    }
  }

  private static class ParameterInfoLabelProvider extends LabelProvider implements
      ITableLabelProvider, ITableFontProvider {
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      RefactoringMethodParameter parameter = (RefactoringMethodParameter) element;
      switch (columnIndex) {
        case TYPE_PROP:
          return parameter.getType();
        case NAME_PROP:
          return parameter.getName();
        case DEFAULT_PROP:
          // TODO(scheglov) implement in future refactorings
          return "-";
//          if (parameter.isAdded()) {
//            return parameter.getDefaultValue();
//          } else {
//            return "-"; //$NON-NLS-1$
//          }
        default:
          throw new IllegalArgumentException(columnIndex + ": " + element); //$NON-NLS-1$
      }
    }

    @Override
    public Font getFont(Object element, int columnIndex) {
      // TODO(scheglov) implement in future refactorings
      return null;
//      RefactoringMethodParameter parameter = (RefactoringMethodParameter) element;
//      if (parameter.isAdded()) {
//        return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
//      } else {
//        return null;
//      }
    }
  }

  private class ParametersCellModifier implements ICellModifier {
    @Override
    public boolean canModify(Object element, String property) {
      Assert.isTrue(element instanceof RefactoringMethodParameter);
      if (property.equals(PROPERTIES[TYPE_PROP])) {
        return fMode.canChangeTypes();
      } else if (property.equals(PROPERTIES[NAME_PROP])) {
        return true;
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        // TODO(scheglov) implement in future refactorings
//        return (((RefactoringMethodParameter) element).isAdded());
      }
      Assert.isTrue(false);
      return false;
    }

    @Override
    public Object getValue(Object element, String property) {
      Assert.isTrue(element instanceof RefactoringMethodParameter);
      if (property.equals(PROPERTIES[TYPE_PROP])) {
        return ((RefactoringMethodParameter) element).getType();
      } else if (property.equals(PROPERTIES[NAME_PROP])) {
        return ((RefactoringMethodParameter) element).getName();
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        // TODO(scheglov) implement in future refactorings
        return null;
//        return ((RefactoringMethodParameter) element).getDefaultValue();
      }
      Assert.isTrue(false);
      return null;
    }

    @Override
    public void modify(Object element, String property, Object value) {
      if (element instanceof TableItem) {
        element = ((TableItem) element).getData();
      }
      if (!(element instanceof RefactoringMethodParameter)) {
        return;
      }
      boolean unchanged;
      RefactoringMethodParameter parameter = (RefactoringMethodParameter) element;
      if (property.equals(PROPERTIES[NAME_PROP])) {
        unchanged = parameter.getName().equals(value);
        parameter.setName((String) value);
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        // TODO(scheglov) implement in future refactorings
        unchanged = true;
//        unchanged = parameter.getDefaultValue().equals(value);
//        parameter.setDefaultValue((String) value);
      } else if (property.equals(PROPERTIES[TYPE_PROP])) {
        unchanged = parameter.getType().equals(value);
        parameter.setType((String) value);
      } else {
        throw new IllegalStateException();
      }
      if (!unchanged) {
        ChangeParametersControl_NEW.this.fListener.parameterChanged(parameter);
        ChangeParametersControl_NEW.this.fTableViewer.update(parameter, new String[] {property});
      }
    }
  }

  private static final IElementComparer PARAMETER_COMPARER = new IElementComparer() {
    @Override
    public boolean equals(Object a, Object b) {
      if (a == b) {
        return true;
      }
      if (!(a instanceof RefactoringMethodParameter) || !(b instanceof RefactoringMethodParameter)) {
        return false;
      }
      RefactoringMethodParameter pa = (RefactoringMethodParameter) a;
      RefactoringMethodParameter pb = (RefactoringMethodParameter) b;
      return StringUtils.equals(pa.getId(), pb.getId());
    }

    @Override
    public int hashCode(Object element) {
      String id = ((RefactoringMethodParameter) element).getId();
      if (id == null) {
        return 0;
      }
      return id.hashCode();
    }
  };

  private static final String[] PROPERTIES = {"type", "name", "default"};
  private static final int TYPE_PROP = 0;
  private static final int NAME_PROP = 1;
  private static final int DEFAULT_PROP = 2;

  private static final int ROW_COUNT = 7;

  private static void moveUp(List<RefactoringMethodParameter> elements,
      List<RefactoringMethodParameter> move) {
    List<RefactoringMethodParameter> res = Lists.newArrayList();
    RefactoringMethodParameter floating = null;
    for (Iterator<RefactoringMethodParameter> iter = elements.iterator(); iter.hasNext();) {
      RefactoringMethodParameter curr = iter.next();
      if (move.contains(curr)) {
        res.add(curr);
      } else {
        if (floating != null) {
          res.add(floating);
        }
        floating = curr;
      }
    }
    if (floating != null) {
      res.add(floating);
    }
    elements.clear();
    for (Iterator<RefactoringMethodParameter> iter = res.iterator(); iter.hasNext();) {
      elements.add(iter.next());
    }
  }

  private final Mode fMode;
  private final IParameterListChangeListener_NEW fListener;
  private List<RefactoringMethodParameter> fParameters;

  private final String[] fParamNameProposals;
  private ContentAssistHandler fNameContentAssistHandler;
  private TableViewer fTableViewer;
  private Button fUpButton;
  private Button fDownButton;
  private Button fEditButton;

  private Button fAddButton;

  private Button fRemoveButton;

  public ChangeParametersControl_NEW(Composite parent, int style, String label,
      IParameterListChangeListener_NEW listener, Mode mode) {
    this(parent, style, label, listener, mode, new String[0]);
  }

  /**
   * @param label the label before the table or <code>null</code>
   * @param typeContext the package in which to complete types
   */
  public ChangeParametersControl_NEW(Composite parent, int style, String label,
      IParameterListChangeListener_NEW listener, Mode mode, String[] paramNameProposals) {
    super(parent, style);
    Assert.isNotNull(listener);
    fListener = listener;
    fMode = mode;
    fParamNameProposals = paramNameProposals;

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    if (label != null) {
      Label tableLabel = new Label(this, SWT.NONE);
      GridData labelGd = new GridData();
      labelGd.horizontalSpan = 2;
      tableLabel.setLayoutData(labelGd);
      tableLabel.setText(label);
    }

    createParameterList(this);
    createButtonComposite(this);
  }

  public void editParameter(RefactoringMethodParameter parameter) {
    fTableViewer.getControl().setFocus();
    fTableViewer.setSelection(new StructuredSelection(parameter), true);
    updateButtonsEnabledState();
    editColumnOrNextPossible(NAME_PROP);
  }

  // ---- Parameter table -----------------------------------------------------------------------------------

  public void setInput(List<RefactoringMethodParameter> parameters) {
    Assert.isNotNull(parameters);
    fParameters = parameters;
    fTableViewer.setInput(fParameters);
    if (fParameters.size() > 0) {
      fTableViewer.setSelection(new StructuredSelection(fParameters.get(0)));
    }
  }

  private void addCellEditors() {
    fTableViewer.setColumnProperties(PROPERTIES);

    final TableTextCellEditor editors[] = new TableTextCellEditor[PROPERTIES.length];

    editors[TYPE_PROP] = new TableTextCellEditor(fTableViewer, TYPE_PROP);
    editors[NAME_PROP] = new TableTextCellEditor(fTableViewer, NAME_PROP);
    editors[DEFAULT_PROP] = new TableTextCellEditor(fTableViewer, DEFAULT_PROP);

    if (fMode.canChangeTypes()) {
      // TODO(scheglov)
//      SubjectControlContentAssistant assistant = installParameterTypeContentAssist(editors[TYPE_PROP].getText());
//      editors[TYPE_PROP].setContentAssistant(assistant);
    }
    if (fParamNameProposals.length > 0) {
      SubjectControlContentAssistant assistant = installParameterNameContentAssist(editors[NAME_PROP].getText());
      editors[NAME_PROP].setContentAssistant(assistant);
    }

    for (int i = 0; i < editors.length; i++) {
      final int editorColumn = i;
      final TableTextCellEditor editor = editors[i];
      // support tabbing between columns while editing:
      editor.getText().addTraverseListener(new TraverseListener() {
        @Override
        public void keyTraversed(TraverseEvent e) {
          switch (e.detail) {
            case SWT.TRAVERSE_TAB_NEXT:
              editColumnOrNextPossible(nextColumn(editorColumn));
              e.detail = SWT.TRAVERSE_NONE;
              break;

            case SWT.TRAVERSE_TAB_PREVIOUS:
              editColumnOrPrevPossible(prevColumn(editorColumn));
              e.detail = SWT.TRAVERSE_NONE;
              break;

            default:
              break;
          }
        }
      });
      TextFieldNavigationHandler.install(editor.getText());
    }

    editors[NAME_PROP].setActivationListener(new TableTextCellEditor.IActivationListener() {
      @Override
      public void activate() {
        // TODO(scheglov) not used in "Extract Method"
//        RefactoringMethodParameter[] selected = getSelectedElements();
//        if (selected.length == 1 && fNameContentAssistHandler != null) {
//          fNameContentAssistHandler.setEnabled(selected[0].isAdded());
//        }
      }
    });

    fTableViewer.setCellEditors(editors);
    fTableViewer.setCellModifier(new ParametersCellModifier());
  }

  private void addColumnLayoutData(TableLayoutComposite layouter) {
    if (fMode.canChangeDefault()) {
      layouter.addColumnData(new ColumnWeightData(33, true));
      layouter.addColumnData(new ColumnWeightData(33, true));
      layouter.addColumnData(new ColumnWeightData(34, true));
    } else {
      layouter.addColumnData(new ColumnWeightData(50, true));
      layouter.addColumnData(new ColumnWeightData(50, true));
    }
  }

  private void addSpacer(Composite parent) {
    Label label = new Label(parent, SWT.NONE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.heightHint = 5;
    label.setLayoutData(gd);
  }

  private boolean canMove(boolean up) {
    int notDeletedInfosCount = getNotDeletedInfosCount();
    if (notDeletedInfosCount == 0) {
      return false;
    }
    int[] indc = getTable().getSelectionIndices();
    if (indc.length == 0) {
      return false;
    }
    int invalid = up ? 0 : notDeletedInfosCount - 1;
    for (int i = 0; i < indc.length; i++) {
      if (indc[i] == invalid) {
        return false;
      }
    }
    return true;
  }

  private Button createAddButton(Composite buttonComposite) {
    Button button = new Button(buttonComposite, SWT.PUSH);
    button.setText(RefactoringMessages.ChangeParametersControl_buttons_add);
    button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    SWTUtil.setButtonDimensionHint(button);
    // TODO(scheglov) not used in "Extract Method"
//    button.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        String[] excludedParamNames = new String[fParameterInfos.size()];
//        for (int i = 0; i < fParameterInfos.size(); i++) {
//          ParameterInfo info = fParameterInfos.get(i);
//          excludedParamNames[i] = info.getNewName();
//        }
//        IJavaProject javaProject = fTypeContext.getCuHandle().getJavaProject();
//        String newParamName = StubUtility.suggestArgumentName(
//            javaProject,
//            RefactoringMessages.ChangeParametersControl_new_parameter_default_name,
//            excludedParamNames);
//        ParameterInfo newInfo = ParameterInfo.createInfoForAddedParameter(
//            "Object", newParamName, "null"); //$NON-NLS-1$ //$NON-NLS-2$
//        int insertIndex = fParameterInfos.size();
//        for (int i = fParameterInfos.size() - 1; i >= 0; i--) {
//          ParameterInfo info = fParameterInfos.get(i);
//          if (info.isNewVarargs()) {
//            insertIndex = i;
//            break;
//          }
//        }
//        fParameterInfos.add(insertIndex, newInfo);
//        fListener.parameterAdded(newInfo);
//        fTableViewer.refresh();
//        fTableViewer.getControl().setFocus();
//        fTableViewer.setSelection(new StructuredSelection(newInfo), true);
//        updateButtonsEnabledState();
//        editColumnOrNextPossible(0);
//      }
//    });
    return button;
  }

  private Button createButton(Composite buttonComposite, String text, final boolean up) {
    Button button = new Button(buttonComposite, SWT.PUSH);
    button.setText(text);
    button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    SWTUtil.setButtonDimensionHint(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ISelection savedSelection = fTableViewer.getSelection();
        if (savedSelection == null) {
          return;
        }
        RefactoringMethodParameter[] selection = getSelectedElements();
        if (selection.length == 0) {
          return;
        }

        if (up) {
          moveUp(selection);
        } else {
          moveDown(selection);
        }
        fTableViewer.refresh();
        fTableViewer.setSelection(savedSelection);
        fListener.parameterListChanged();
        fTableViewer.getControl().setFocus();
      }
    });
    return button;
  }

  // ---- Button bar --------------------------------------------------------------------------------------

  private void createButtonComposite(Composite parent) {
    Composite buttonComposite = new Composite(parent, SWT.NONE);
    buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    GridLayout gl = new GridLayout();
    gl.marginHeight = 0;
    gl.marginWidth = 0;
    buttonComposite.setLayout(gl);

    if (fMode.canAddParameters()) {
      fAddButton = createAddButton(buttonComposite);
    }

    fEditButton = createEditButton(buttonComposite);

    if (fMode.canAddParameters()) {
      fRemoveButton = createRemoveButton(buttonComposite);
    }

    if (buttonComposite.getChildren().length != 0) {
      addSpacer(buttonComposite);
    }

    fUpButton = createButton(
        buttonComposite,
        RefactoringMessages.ChangeParametersControl_buttons_move_up,
        true);
    fDownButton = createButton(
        buttonComposite,
        RefactoringMessages.ChangeParametersControl_buttons_move_down,
        false);

    updateButtonsEnabledState();
  }

  private Button createEditButton(Composite buttonComposite) {
    Button button = new Button(buttonComposite, SWT.PUSH);
    button.setText(RefactoringMessages.ChangeParametersControl_buttons_edit);
    button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    SWTUtil.setButtonDimensionHint(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          RefactoringMethodParameter[] selected = getSelectedElements();
          Assert.isTrue(selected.length == 1);
          RefactoringMethodParameter parameter = selected[0];
          ParameterEditDialog_NEW dialog = new ParameterEditDialog_NEW(
              getShell(),
              parameter,
              fMode.canChangeTypes(),
              fMode.canChangeDefault());
          dialog.open();
          fListener.parameterChanged(parameter);
          fTableViewer.update(parameter, PROPERTIES);
        } finally {
          fTableViewer.getControl().setFocus();
        }
      }
    });
    return button;
  }

  private void createParameterList(Composite parent) {
    TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
    addColumnLayoutData(layouter);

    final Table table = new Table(layouter, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    TableColumn tc;
    tc = new TableColumn(table, SWT.NONE, TYPE_PROP);
    tc.setResizable(true);
    tc.setText(RefactoringMessages.ChangeParametersControl_table_type);

    tc = new TableColumn(table, SWT.NONE, NAME_PROP);
    tc.setResizable(true);
    tc.setText(RefactoringMessages.ChangeParametersControl_table_name);

    if (fMode.canChangeDefault()) {
      tc = new TableColumn(table, SWT.NONE, DEFAULT_PROP);
      tc.setResizable(true);
      tc.setText(RefactoringMessages.ChangeParametersControl_table_defaultValue);
    }

    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = SWTUtil.getTableHeightHint(table, ROW_COUNT);
    gd.widthHint = 40;
    layouter.setLayoutData(gd);

    fTableViewer = new TableViewer(table);
    fTableViewer.setUseHashlookup(true);
    fTableViewer.setComparer(PARAMETER_COMPARER);
    fTableViewer.setContentProvider(new ParameterInfoContentProvider());
    fTableViewer.setLabelProvider(new ParameterInfoLabelProvider());
    fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonsEnabledState();
      }
    });

    table.addTraverseListener(new TraverseListener() {
      @Override
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_RETURN && e.stateMask == SWT.NONE) {
          editColumnOrNextPossible(0);
          e.detail = SWT.TRAVERSE_NONE;
        }
      }
    });
    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.F2 && e.stateMask == SWT.NONE) {
          editColumnOrNextPossible(0);
          e.doit = false;
        }
      }
    });

    addCellEditors();
  }

  private Button createRemoveButton(Composite buttonComposite) {
    final Button button = new Button(buttonComposite, SWT.PUSH);
    button.setText(RefactoringMessages.ChangeParametersControl_buttons_remove);
    button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    SWTUtil.setButtonDimensionHint(button);
    // TODO(scheglov)
//    button.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        int index = getTable().getSelectionIndices()[0];
//        ParameterInfo[] selected = getSelectedElements();
//        for (int i = 0; i < selected.length; i++) {
//          if (selected[i].isAdded()) {
//            fParameterInfos.remove(selected[i]);
//          } else {
//            selected[i].markAsDeleted();
//          }
//        }
//        restoreSelection(index);
//      }
//
//      private void restoreSelection(int index) {
//        fTableViewer.refresh();
//        fTableViewer.getControl().setFocus();
//        int itemCount = getTableItemCount();
//        if (itemCount != 0) {
//          if (index >= itemCount) {
//            index = itemCount - 1;
//          }
//          getTable().setSelection(index);
//        }
//        fListener.parameterListChanged();
//        updateButtonsEnabledState();
//      }
//    });
    return button;
  }

  private void editColumnOrNextPossible(int column) {
    RefactoringMethodParameter[] selected = getSelectedElements();
    if (selected.length != 1) {
      return;
    }
    int nextColumn = column;
    do {
      fTableViewer.editElement(selected[0], nextColumn);
      if (fTableViewer.isCellEditorActive()) {
        return;
      }
      nextColumn = nextColumn(nextColumn);
    } while (nextColumn != column);
  }

  private void editColumnOrPrevPossible(int column) {
    RefactoringMethodParameter[] selected = getSelectedElements();
    if (selected.length != 1) {
      return;
    }
    int prevColumn = column;
    do {
      fTableViewer.editElement(selected[0], prevColumn);
      if (fTableViewer.isCellEditorActive()) {
        return;
      }
      prevColumn = prevColumn(prevColumn);
    } while (prevColumn != column);
  }

  private int getNotDeletedInfosCount() {
    if (fParameters == null) {
      return 0;
    }
    return fParameters.size();
  }

  private RefactoringMethodParameter[] getSelectedElements() {
    ISelection selection = fTableViewer.getSelection();
    if (selection == null) {
      return new RefactoringMethodParameter[0];
    }

    if (!(selection instanceof IStructuredSelection)) {
      return new RefactoringMethodParameter[0];
    }

    List<?> selected = ((IStructuredSelection) selection).toList();
    return selected.toArray(new RefactoringMethodParameter[selected.size()]);
  }

  private Table getTable() {
    return fTableViewer.getTable();
  }

//  private int getTableItemCount() {
//    return getTable().getItemCount();
//  }

  //---- editing -----------------------------------------------------------------------------------------------

  private int getTableSelectionCount() {
    return getTable().getSelectionCount();
  }

  private SubjectControlContentAssistant installParameterNameContentAssist(Text text) {
    VariableNamesProcessor processor = new VariableNamesProcessor(fParamNameProposals);
    SubjectControlContentAssistant contentAssistant = ControlContentAssistHelper.createJavaContentAssistant(processor);
    fNameContentAssistHandler = ContentAssistHandler.createHandlerForText(text, contentAssistant);
    return contentAssistant;
  }

  // TODO(scheglov)
//  private SubjectControlContentAssistant installParameterTypeContentAssist(Text text) {
//    DartTypeCompletionProcessor processor = new DartTypeCompletionProcessor(true, false);
//    if (fTypeContext == null) {
//      processor.setCompletionContext(null, null, null);
//    } else {
//      processor.setCompletionContext(
//          fTypeContext.getCuHandle(),
//          fTypeContext.getBeforeString(),
//          fTypeContext.getAfterString());
//    }
//    SubjectControlContentAssistant contentAssistant = ControlContentAssistHelper.createJavaContentAssistant(processor);
//    ContentAssistHandler.createHandlerForText(text, contentAssistant);
//    return contentAssistant;
//  }

  //---- change order ----------------------------------------------------------------------------------------

  private void moveDown(RefactoringMethodParameter[] selection) {
    Collections.reverse(fParameters);
    moveUp(fParameters, Arrays.asList(selection));
    Collections.reverse(fParameters);
  }

  private void moveUp(RefactoringMethodParameter[] selection) {
    moveUp(fParameters, Arrays.asList(selection));
  }

  private int nextColumn(int column) {
    return (column >= getTable().getColumnCount() - 1) ? 0 : column + 1;
  }

  private int prevColumn(int column) {
    return (column <= 0) ? getTable().getColumnCount() - 1 : column - 1;
  }

  private void updateButtonsEnabledState() {
    fUpButton.setEnabled(canMove(true));
    fDownButton.setEnabled(canMove(false));
    if (fEditButton != null) {
      fEditButton.setEnabled(getTableSelectionCount() == 1);
    }
    if (fAddButton != null) {
      fAddButton.setEnabled(true);
    }
    if (fRemoveButton != null) {
      fRemoveButton.setEnabled(getTableSelectionCount() != 0);
    }
  }
}
