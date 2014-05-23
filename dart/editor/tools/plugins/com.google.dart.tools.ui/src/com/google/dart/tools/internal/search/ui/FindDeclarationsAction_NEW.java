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
package com.google.dart.tools.internal.search.ui;

import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

/**
 * Finds declarations similar to the selected in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindDeclarationsAction_NEW extends AbstractDartSelectionAction {
  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  public static void doSearch(final String name) {
    // TODO(scheglov) restore or remove for the new API
//    try {
//      SearchView view = (SearchView) DartToolsPlugin.showView(SearchView.ID);
//      if (view == null) {
//        return;
//      }
//      String taskName = "Searching for declarations of '" + name + "'...";
//      view.showPage(new SearchResultPage_NEW(view, taskName, null) {
//        @Override
//        protected boolean canUseFilterPotential() {
//          return false;
//        }
//
//        @Override
//        protected IProject getCurrentProject() {
//          return FindReferencesAction.findCurrentProject();
//        }
//
//        @Override
//        protected String getQueryElementName() {
//          return name;
//        }
//
//        @Override
//        protected String getQueryKindName() {
//          return "declarations";
//        }
//
//        @Override
//        protected List<SearchResult> runQuery() {
//          final List<SearchResult> allResults = Lists.newArrayList();
//          final CountDownLatch latch = new CountDownLatch(1);
//          DartCore.getAnalysisServer().searchClassMemberDeclarations(
//              name,
//              new SearchResultsConsumer() {
//                @Override
//                public void computed(SearchResult[] searchResults, boolean isLastResult) {
//                  Collections.addAll(allResults, searchResults);
//                  if (isLastResult) {
//                    latch.countDown();
//                  }
//                }
//              });
//          Uninterruptibles.awaitUninterruptibly(latch, 15, TimeUnit.MINUTES);
//          return allResults;
//        }
//      });
//    } catch (Throwable e) {
//      ExceptionHandler.handle(e, "Find Declarations", "Exception during search.");
//    }
  }

//  /**
//   * When one {@link Source} (one file) is used in more than one context, {@link SearchEngine} will
//   * return separate {@link SearchMatch} for each context. But we want to show {@link Source} only
//   * once.
//   */
//  static List<SearchMatch> getUniqueMatches(List<SearchMatch> matches) {
//    Set<SearchMatch> uniqueMatches = Sets.newHashSet();
//    for (SearchMatch match : matches) {
//      uniqueMatches.add(match);
//    }
//    return Lists.newArrayList(uniqueMatches);
//  }
//
//  static boolean isInvocationNameOrPropertyAccessSelected(DartSelection selection) {
//    AstNode node = getSelectionNode(selection);
//    if (!(node instanceof SimpleIdentifier)) {
//      return false;
//    }
//    SimpleIdentifier name = (SimpleIdentifier) node;
//    AstNode parent = name.getParent();
//    // method name
//    if (parent instanceof MethodInvocation) {
//      MethodInvocation invocation = (MethodInvocation) parent;
//      return invocation.getMethodName() == name && invocation.getRealTarget() != null;
//    }
//    // property name
//    if (parent instanceof PropertyAccess) {
//      PropertyAccess access = (PropertyAccess) parent;
//      return access.getPropertyName() == name && access.getRealTarget() != null;
//    }
//    // property name (cannot be distinguished from prefixed)
//    if (parent instanceof PrefixedIdentifier) {
//      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
//      return prefixed.getIdentifier() == name;
//    }
//    // we don't know this node
//    return false;
//  }
//
//  /**
//   * @return {@code true} if given {@link DartSelection} looks valid.
//   */
//  private static boolean isValidSelection(DartSelection selection) {
//    // If we know the exact Element, no need to search for singleton declarations
//    Element element = getSelectionElement(selection);
//    if (element != null) {
//      return element instanceof MethodElement || element instanceof FieldElement
//          || element instanceof PropertyAccessorElement;
//    }
//    // we want to search only method and property declarations
//    return isInvocationNameOrPropertyAccessSelected(selection);
//  }

  public FindDeclarationsAction_NEW(DartEditor editor) {
    super(editor);
  }

  public FindDeclarationsAction_NEW(IWorkbenchSite site) {
    super(site);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
//    setEnabled(isValidSelection(selection));
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
//    Element element = getSelectionElement(selection);
//    setEnabled(element != null);
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
//    // may be resolved element
//    Element element = ActionUtil.getActionElement(selection);
//    if (element != null) {
//      String name = element.getName();
//      doSearch(name);
//    }
//    // may be identifier
//    AstNode node = getSelectionNode(selection);
//    if (node instanceof SimpleIdentifier) {
//      String name = ((SimpleIdentifier) node).getName();
//      doSearch(name);
//    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
//    Element element = getSelectionElement(selection);
//    if (element != null) {
//      String name = element.getDisplayName();
//      doSearch(name);
//    }
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
