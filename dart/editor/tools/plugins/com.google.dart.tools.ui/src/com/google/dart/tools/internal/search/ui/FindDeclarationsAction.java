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
package com.google.dart.tools.internal.search.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Finds declarations of the {@link Element}s similar to selected in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindDeclarationsAction extends AbstractDartSelectionAction_OLD {
  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  public static void doSearch(final String name) {
    try {
      final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
      if (view == null) {
        return;
      }
      view.showPage(new SearchMatchPage(view, "Searching for '" + name + "' declarations...") {
        @Override
        protected boolean canUseFilterPotential() {
          return false;
        }

        @Override
        protected IProject getCurrentProject() {
          return FindReferencesAction.findCurrentProject();
        }

        @Override
        protected String getQueryElementName() {
          return name;
        }

        @Override
        protected String getQueryKindName() {
          return "declarations";
        }

        @Override
        protected List<SearchMatch> runQuery() {
          List<SearchMatch> declarations = searchEngine.searchDeclarations(name, null, null);
          for (Iterator<SearchMatch> iter = declarations.iterator(); iter.hasNext();) {
            SearchMatch searchMatch = iter.next();
            Element declaringElement = searchMatch.getElement();
            Element enclosingElement = declaringElement.getEnclosingElement();
            // OK, top-level or class member
            if (enclosingElement instanceof CompilationUnitElement
                || enclosingElement instanceof ClassElement) {
              continue;
            }
            // local element, remove
            iter.remove();
          }
          declarations = getUniqueMatches(declarations);
          return declarations;
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Find Declarations", "Exception during search.");
    }
  }

  /**
   * When one {@link Source} (one file) is used in more than one context, {@link SearchEngine} will
   * return separate {@link SearchMatch} for each context. But we want to show {@link Source} only
   * once.
   */
  static List<SearchMatch> getUniqueMatches(List<SearchMatch> matches) {
    Set<SearchMatch> uniqueMatches = Sets.newHashSet();
    for (SearchMatch match : matches) {
      uniqueMatches.add(match);
    }
    return Lists.newArrayList(uniqueMatches);
  }

  static boolean isInvocationNameOrPropertyAccessSelected(DartSelection selection) {
    AstNode node = getSelectionNode(selection);
    if (!(node instanceof SimpleIdentifier)) {
      return false;
    }
    SimpleIdentifier name = (SimpleIdentifier) node;
    AstNode parent = name.getParent();
    // method name
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      return invocation.getMethodName() == name && invocation.getRealTarget() != null;
    }
    // property name
    if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      return access.getPropertyName() == name && access.getRealTarget() != null;
    }
    // property name (cannot be distinguished from prefixed)
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      return prefixed.getIdentifier() == name;
    }
    // we don't know this node
    return false;
  }

  /**
   * @return {@code true} if given {@link DartSelection} looks valid.
   */
  private static boolean isValidSelection(DartSelection selection) {
    // If we know the exact Element, no need to search for singleton declarations
    Element element = getSelectionElement(selection);
    if (element != null) {
      return element instanceof MethodElement || element instanceof FieldElement
          || element instanceof PropertyAccessorElement;
    }
    // we want to search only method and property declarations
    return isInvocationNameOrPropertyAccessSelected(selection);
  }

  public FindDeclarationsAction(DartEditor editor) {
    super(editor);
  }

  public FindDeclarationsAction(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    setEnabled(isValidSelection(selection));
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    Element element = getSelectionElement(selection);
    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // may be resolved element
    Element element = ActionUtil.getActionElement(selection);
    if (element != null) {
      String name = element.getName();
      doSearch(name);
    }
    // may be identifier
    AstNode node = getSelectionNode(selection);
    if (node instanceof SimpleIdentifier) {
      String name = ((SimpleIdentifier) node).getName();
      doSearch(name);
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    if (element != null) {
      String name = element.getDisplayName();
      doSearch(name);
    }
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindDeclarationsAction_label);
    setToolTipText(SearchMessages.Search_FindDeclarationsAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_DECLARATIONS_IN_WORKSPACE_ACTION);
  }
}
