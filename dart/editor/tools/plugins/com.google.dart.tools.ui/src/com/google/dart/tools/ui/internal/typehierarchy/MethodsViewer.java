package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.actions.MemberFilterActionGroup;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.typehierarchy.TypeHierarchyContentProvider_OLD.TypeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

import java.util.List;

/**
 * {@link MethodsViewer} shows a list of methods of {@link ClassElement}.
 */
public class MethodsViewer extends TableViewer {
  private class ShowInheritedMembersAction extends Action {
    public ShowInheritedMembersAction() {
      super(TypeHierarchyMessages.ShowInheritedMembersAction_label, IAction.AS_CHECK_BOX);
      setDescription(TypeHierarchyMessages.ShowInheritedMembersAction_description);
      setToolTipText(TypeHierarchyMessages.ShowInheritedMembersAction_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "inher_co.gif"); //$NON-NLS-1$
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.SHOW_INHERITED_ACTION);
    }

    @Override
    public void run() {
      BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
        @Override
        public void run() {
          showInheritedMethods(isChecked());
        }
      });
    }

  }

  private static final String TAG_SHOWINHERITED = "showinherited"; //$NON-NLS-1$

  private boolean showInheritedMembers;
  private ClassElement inputType;
  private final IAction showInheritedMembersAction = new ShowInheritedMembersAction();
  private final MemberFilterActionGroup fMemberFilterActionGroup;

  public MethodsViewer(Composite parent) {
    super(parent, SWT.FULL_SELECTION);
    setContentProvider(new ArrayContentProvider());
    setLabelProvider(new DartElementLabelProvider());
    setComparator(new DartElementComparator());
    fMemberFilterActionGroup = new MemberFilterActionGroup(
        this,
        "HierarchyMethodView",
        false,
        MemberFilterActionGroup.ALL_FILTERS);
    addFilter(new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof Element) {
          if (((Element) element).isSynthetic()) {
            return false;
          }
        }
        return true;
      }
    });
  }

  /**
   * Fills the {@link ToolBarManager} with items for the {@link MethodsViewer}.
   */
  public void contributeToToolBar(IToolBarManager tbm) {
    tbm.add(showInheritedMembersAction);
    fMemberFilterActionGroup.contributeToToolBar(tbm);
  }

  /**
   * Restores the state of the filter actions.
   */
  public void restoreState(IMemento memento) {
    fMemberFilterActionGroup.restoreState(memento);
    {
      boolean showInherited = Boolean.valueOf(memento.getString(TAG_SHOWINHERITED)).booleanValue();
      showInheritedMembersAction.setChecked(showInherited);
      showInheritedMethods(showInherited);
    }
  }

  /**
   * Saves the state of the filter actions.
   */
  public void saveState(IMemento memento) {
    fMemberFilterActionGroup.saveState(memento);
    memento.putString(TAG_SHOWINHERITED, String.valueOf(showInheritedMembers));
  }

  public void setInputType(Object _inputType) {
    if (_inputType instanceof TypeItem) {
      _inputType = ((TypeItem) _inputType).element;
    }
    if (!(_inputType instanceof ClassElement)) {
      setInput(null);
      return;
    }
    this.inputType = (ClassElement) _inputType;
    // may be no type
    if (inputType == null) {
      setInput(null);
      return;
    }
    // show methods
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        List<Element> members;
        if (showInheritedMembers) {
          members = HierarchyUtils.getMembers(inputType, false);
        } else {
          members = HierarchyUtils.getDirectMembers(inputType, false);
        }
        setInput(members);
      }
    });
  }

  private void showInheritedMethods(boolean checked) {
    showInheritedMembers = checked;
    if (inputType != null) {
      setInputType(inputType);
    }
  }
}
