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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.tools.ui.internal.dialogs.TableTextCellEditor;
import com.google.dart.tools.ui.internal.dialogs.TextFieldNavigationHandler;
import com.google.dart.tools.ui.internal.refactoring.contentassist.VariableNamesProcessor;
import com.google.dart.tools.ui.internal.util.ControlContentAssistHelper;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.internal.util.TableLayoutComposite;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
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

import java.util.ArrayList;
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
public class ChangeParametersControl extends Composite {

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
      return removeMarkedAsDeleted((List<Parameter>) inputElement);
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // do nothing
    }

    private Parameter[] removeMarkedAsDeleted(List<Parameter> parameters) {
      List<Parameter> result = new ArrayList<Parameter>(parameters.size());
      for (Iterator<Parameter> iter = parameters.iterator(); iter.hasNext();) {
        Parameter parameter = iter.next();
        if (!parameter.isDeleted()) {
          result.add(parameter);
        }
      }
      return result.toArray(new Parameter[result.size()]);
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
      Parameter parameter = (Parameter) element;
      switch (columnIndex) {
        case TYPE_PROP:
          return parameter.getNewTypeName();
        case NEWNAME_PROP:
          return parameter.getNewName();
        case DEFAULT_PROP:
          if (parameter.isAdded()) {
            return parameter.getDefaultValue();
          } else {
            return "-"; //$NON-NLS-1$
          }
        default:
          throw new IllegalArgumentException(columnIndex + ": " + element); //$NON-NLS-1$
      }
    }

    @Override
    public Font getFont(Object element, int columnIndex) {
      Parameter parameter = (Parameter) element;
      if (parameter.isAdded()) {
        return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
      } else {
        return null;
      }
    }
  }

  private class ParametersCellModifier implements ICellModifier {
    @Override
    public boolean canModify(Object element, String property) {
      Assert.isTrue(element instanceof Parameter);
      if (property.equals(PROPERTIES[TYPE_PROP])) {
        return fMode.canChangeTypes();
      } else if (property.equals(PROPERTIES[NEWNAME_PROP])) {
        return true;
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        return (((Parameter) element).isAdded());
      }
      Assert.isTrue(false);
      return false;
    }

    @Override
    public Object getValue(Object element, String property) {
      Assert.isTrue(element instanceof Parameter);
      if (property.equals(PROPERTIES[TYPE_PROP])) {
        return ((Parameter) element).getNewTypeName();
      } else if (property.equals(PROPERTIES[NEWNAME_PROP])) {
        return ((Parameter) element).getNewName();
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        return ((Parameter) element).getDefaultValue();
      }
      Assert.isTrue(false);
      return null;
    }

    @Override
    public void modify(Object element, String property, Object value) {
      if (element instanceof TableItem) {
        element = ((TableItem) element).getData();
      }
      if (!(element instanceof Parameter)) {
        return;
      }
      boolean unchanged;
      Parameter parameter = (Parameter) element;
      if (property.equals(PROPERTIES[NEWNAME_PROP])) {
        unchanged = parameter.getNewName().equals(value);
        parameter.setNewName((String) value);
      } else if (property.equals(PROPERTIES[DEFAULT_PROP])) {
        unchanged = parameter.getDefaultValue().equals(value);
        parameter.setDefaultValue((String) value);
      } else if (property.equals(PROPERTIES[TYPE_PROP])) {
        unchanged = parameter.getNewTypeName().equals(value);
        parameter.setNewTypeName((String) value);
      } else {
        throw new IllegalStateException();
      }
      if (!unchanged) {
        ChangeParametersControl.this.fListener.parameterChanged(parameter);
        ChangeParametersControl.this.fTableViewer.update(parameter, new String[] {property});
      }
    }
  }

  private static final String[] PROPERTIES = {"type", "new", "default"}; //$NON-NLS-2$ //$NON-NLS-1$ //$NON-NLS-3$
  private static final int TYPE_PROP = 0;
  private static final int NEWNAME_PROP = 1;

  private static final int DEFAULT_PROP = 2;

  private static final int ROW_COUNT = 7;

  private static void moveUp(List<Parameter> elements, List<Parameter> move) {
    List<Parameter> res = new ArrayList<Parameter>(elements.size());
    List<Parameter> deleted = new ArrayList<Parameter>();
    Parameter floating = null;
    for (Iterator<Parameter> iter = elements.iterator(); iter.hasNext();) {
      Parameter curr = iter.next();
      if (move.contains(curr)) {
        res.add(curr);
      } else if (curr.isDeleted()) {
        deleted.add(curr);
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
    res.addAll(deleted);
    elements.clear();
    for (Iterator<Parameter> iter = res.iterator(); iter.hasNext();) {
      elements.add(iter.next());
    }
  }

  private final Mode fMode;
  private final IParameterListChangeListener fListener;
  private List<Parameter> fParameters;

  private final String[] fParamNameProposals;
  private ContentAssistHandler fNameContentAssistHandler;
  private TableViewer fTableViewer;
  private Button fUpButton;
  private Button fDownButton;
  private Button fEditButton;

  private Button fAddButton;

  private Button fRemoveButton;

  public ChangeParametersControl(Composite parent, int style, String label,
      IParameterListChangeListener listener, Mode mode) {
    this(parent, style, label, listener, mode, new String[0]);
  }

  /**
   * @param label the label before the table or <code>null</code>
   * @param typeContext the package in which to complete types
   */
  public ChangeParametersControl(Composite parent, int style, String label,
      IParameterListChangeListener listener, Mode mode, String[] paramNameProposals) {
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

  public void editParameter(Parameter parameter) {
    fTableViewer.getControl().setFocus();
    if (!parameter.isDeleted()) {
      fTableViewer.setSelection(new StructuredSelection(parameter), true);
      updateButtonsEnabledState();
      editColumnOrNextPossible(NEWNAME_PROP);
      return;
    }
  }

  // ---- Parameter table -----------------------------------------------------------------------------------

  public void setInput(List<Parameter> parameters) {
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
    editors[NEWNAME_PROP] = new TableTextCellEditor(fTableViewer, NEWNAME_PROP);
    editors[DEFAULT_PROP] = new TableTextCellEditor(fTableViewer, DEFAULT_PROP);

    if (fMode.canChangeTypes()) {
      // TODO(scheglov)
//      SubjectControlContentAssistant assistant = installParameterTypeContentAssist(editors[TYPE_PROP].getText());
//      editors[TYPE_PROP].setContentAssistant(assistant);
    }
    if (fParamNameProposals.length > 0) {
      SubjectControlContentAssistant assistant = installParameterNameContentAssist(editors[NEWNAME_PROP].getText());
      editors[NEWNAME_PROP].setContentAssistant(assistant);
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

    editors[NEWNAME_PROP].setActivationListener(new TableTextCellEditor.IActivationListener() {
      @Override
      public void activate() {
        Parameter[] selected = getSelectedElements();
        if (selected.length == 1 && fNameContentAssistHandler != null) {
          fNameContentAssistHandler.setEnabled(selected[0].isAdded());
        }
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
        Parameter[] selection = getSelectedElements();
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
          Parameter[] selected = getSelectedElements();
          Assert.isTrue(selected.length == 1);
          Parameter parameter = selected[0];
          ParameterEditDialog dialog = new ParameterEditDialog(
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

    tc = new TableColumn(table, SWT.NONE, NEWNAME_PROP);
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
    Parameter[] selected = getSelectedElements();
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
    Parameter[] selected = getSelectedElements();
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
    int result = 0;
    for (Iterator<Parameter> iter = fParameters.iterator(); iter.hasNext();) {
      Parameter parameter = iter.next();
      if (!parameter.isDeleted()) {
        result++;
      }
    }
    return result;
  }

  private Parameter[] getSelectedElements() {
    ISelection selection = fTableViewer.getSelection();
    if (selection == null) {
      return new Parameter[0];
    }

    if (!(selection instanceof IStructuredSelection)) {
      return new Parameter[0];
    }

    List<?> selected = ((IStructuredSelection) selection).toList();
    return selected.toArray(new Parameter[selected.size()]);
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

  private void moveDown(Parameter[] selection) {
    Collections.reverse(fParameters);
    moveUp(fParameters, Arrays.asList(selection));
    Collections.reverse(fParameters);
  }

  private void moveUp(Parameter[] selection) {
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
