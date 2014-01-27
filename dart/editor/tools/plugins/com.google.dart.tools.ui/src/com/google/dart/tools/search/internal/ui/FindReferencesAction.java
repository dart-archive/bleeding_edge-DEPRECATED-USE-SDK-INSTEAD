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
package com.google.dart.tools.search.internal.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ClassMemberElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.AbstractDartSelectionAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.actions.ActionUtil;
import com.google.dart.tools.ui.internal.search.SearchMessages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import static com.google.dart.tools.search.internal.ui.FindDeclarationsAction.isInvocationNameOrPropertyAccessSelected;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds references of the selected {@link Element} in the workspace.
 * 
 * @coverage dart.editor.ui.search
 */
public class FindReferencesAction extends AbstractDartSelectionAction {
  /**
   * Shows "Search" view with references to non-local elements with given name.
   */
  public static void searchNameUses(final String name) {
    try {
      SearchView view = (SearchView) DartToolsPlugin.getActivePage().showView(SearchView.ID);
      view.showPage(new SearchMatchPage(view, "Searching for references...") {
        @Override
        protected boolean canUseFilterPotential() {
          return false;
        }

        @Override
        protected IProject getCurrentProject() {
          return findCurrentProject();
        }

        @Override
        protected String getQueryElementName() {
          return name;
        }

        @Override
        protected String getQueryKindName() {
          return "references";
        }

        @Override
        protected List<SearchMatch> runQuery() {
          SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
          List<SearchMatch> refs = searchEngine.searchQualifiedMemberReferences(name, null, null);
          return FindDeclarationsAction.getUniqueMatches(refs);
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, "Find references", "Exception during search.");
    }
  }

  /**
   * Finds the "current" project. That is the project of the active editor.
   */
  static IProject findCurrentProject() {
    IEditorPart editor = DartToolsPlugin.getActiveEditor();
    if (editor != null) {
      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        IFile file = fileInput.getFile();
        if (file != null) {
          return file.getProject();
        }
      }
    }
    return null;
  }

  /**
   * @return {@code true} if given {@link DartSelection} looks valid.
   */
  private static boolean isValidSelection(DartSelection selection) {
    Element element = ActionUtil.getActionElement(selection);
    // unresolved
    if (element == null && isInvocationNameOrPropertyAccessSelected(selection)) {
      return true;
    }
    // interesting elements
    ASTNode node = getSelectionNode(selection);
    return isInterestingElement(node, element);
  }

  public FindReferencesAction(DartEditor editor) {
    super(editor);
  }

  public FindReferencesAction(IWorkbenchSite site) {
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
    Element element = ActionUtil.getActionElement(selection);
    ASTNode node = getSelectionNode(selection);
    doSearch(element, node);
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    Element element = getSelectionElement(selection);
    doSearch(element, null);
  }

  @Override
  protected void init() {
    setText(SearchMessages.Search_FindReferencesAction_label);
    setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION);
  }

  /**
   * Asks {@link SearchView} to execute query and display results.
   */
  private void doSearch(Element element, ASTNode node) {
    // tweak
    element = DartElementUtil.getVariableIfSyntheticAccessor(element);
    if (element instanceof ImportElement) {
      element = ((ImportElement) element).getImportedLibrary();
    }
    // prepare name
    String name = null;
    if (node instanceof SimpleIdentifier) {
      name = ((SimpleIdentifier) node).getName();
    }
    // show search results
    try {
      final SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      final Element searchElement = element;
      final String searchName = name;
      SearchView view = (SearchView) DartToolsPlugin.getActivePage().showView(SearchView.ID);
      view.showPage(new SearchMatchPage(view, "Searching for references...") {
        Map<LibraryElement, Set<LibraryElement>> cachedVisibleLibraries = Maps.newHashMap();

        @Override
        protected void beforeRefresh() {
          super.beforeRefresh();
          cachedVisibleLibraries.clear();
        }

        @Override
        protected boolean canUseFilterPotential() {
          return searchElement != null;
        }

        @Override
        protected IProject getCurrentProject() {
          return findCurrentProject();
        }

        @Override
        protected String getQueryElementName() {
          // no element
          if (searchElement == null) {
            return searchName;
          }
          // constructor
          if (searchElement.getKind() == ElementKind.CONSTRUCTOR) {
            String className = searchElement.getEnclosingElement().getDisplayName();
            String constructorName = searchElement.getDisplayName();
            if (StringUtils.isEmpty(constructorName)) {
              return "constructor " + className + "()";
            } else {
              return "constructor " + className + "." + constructorName + "()";
            }
          }
          // some other element
          return searchElement.getDisplayName();
        }

        @Override
        protected String getQueryKindName() {
          return "references";
        }

        @Override
        protected List<SearchMatch> runQuery() {
          List<SearchMatch> allMatches = Lists.newArrayList();
          if (searchElement != null) {
            allMatches.addAll(findVariableElementDeclaration());
            allMatches.addAll(findElementReferences());
          }
          addUniqueNameReferences(allMatches, findNameReferences());
          allMatches = getAccessibleMatches(allMatches);
          allMatches = FindDeclarationsAction.getUniqueMatches(allMatches);
          return allMatches;
        }

        /**
         * Adds given "name" references only if there are no "exact" reference with same location.
         */
        private void addUniqueNameReferences(List<SearchMatch> result, List<SearchMatch> nameMatches) {
          // remember existing locations
          Set<Pair<Element, SourceRange>> existingRefs = Sets.newHashSet();
          for (SearchMatch match : result) {
            existingRefs.add(ImmutablePair.of(match.getElement(), match.getSourceRange()));
          }
          // add new name references
          for (SearchMatch match : nameMatches) {
            if (existingRefs.contains(ImmutablePair.of(match.getElement(), match.getSourceRange()))) {
              continue;
            }
            result.add(match);
          }
        }

        private List<SearchMatch> findElementReferences() {
          Element[] refElements;
          if (searchElement instanceof MethodElement || searchElement instanceof FieldElement) {
            // field or method
            ClassMemberElement member = (ClassMemberElement) searchElement;
            Set<ClassMemberElement> hierarchyMembers = HierarchyUtils.getHierarchyMembers(
                searchEngine,
                member);
            refElements = hierarchyMembers.toArray(new ClassMemberElement[hierarchyMembers.size()]);
          } else if (searchElement.getEnclosingElement() instanceof ClassElement
              && searchElement instanceof PropertyAccessorElement) {
            // class property accessor
            PropertyAccessorElement accessor = (PropertyAccessorElement) searchElement;
            ClassMemberElement property = (ClassMemberElement) accessor.getVariable();
            Set<ClassMemberElement> hierarchyMembers = HierarchyUtils.getHierarchyMembers(
                searchEngine,
                property);
            Set<PropertyAccessorElement> hierarchyAccessors = Sets.newHashSet();
            for (ClassMemberElement hierarchyMember : hierarchyMembers) {
              if (hierarchyMember instanceof FieldElement) {
                FieldElement hierarchyField = (FieldElement) hierarchyMember;
                if (accessor.isGetter()) {
                  hierarchyAccessors.add(hierarchyField.getGetter());
                } else if (accessor.isSetter()) {
                  hierarchyAccessors.add(hierarchyField.getSetter());
                }
              }
            }
            refElements = hierarchyAccessors.toArray(new PropertyAccessorElement[hierarchyAccessors.size()]);
          } else {
            // some other element
            refElements = new Element[] {searchElement};
          }
          // find references to "refElements"
          List<SearchMatch> references = Lists.newArrayList();
          for (Element refElement : refElements) {
            references.addAll(searchEngine.searchReferences(refElement, null, new SearchFilter() {
              @Override
              public boolean passes(SearchMatch match) {
                if (match.getKind() == MatchKind.CONSTRUCTOR_DECLARATION) {
                  return false;
                }
                return true;
              }
            }));
          }
          return references;
        }

        private List<SearchMatch> findNameReferences() {
          if (searchElement != null) {
            // only class members may have potential references
            if (!(searchElement.getEnclosingElement() instanceof ClassElement)) {
              return ImmutableList.of();
            }
            // check kind
            ElementKind elementKind = searchElement.getKind();
            if (elementKind != ElementKind.METHOD && elementKind != ElementKind.FIELD
                && elementKind != ElementKind.GETTER && elementKind != ElementKind.SETTER) {
              return ImmutableList.of();
            }
          }
          // do search
          return searchEngine.searchQualifiedMemberReferences(searchName, null, new SearchFilter() {
            @Override
            public boolean passes(SearchMatch match) {
              return match.getKind() == MatchKind.NAME_REFERENCE_RESOLVED
                  || match.getKind() == MatchKind.NAME_REFERENCE_UNRESOLVED;
            }
          });
        }

        /**
         * For local variable and parameters it is interesting to see their declaration with
         * initializer and type.
         */
        private List<SearchMatch> findVariableElementDeclaration() {
          ElementKind elementKind = searchElement.getKind();
          if (elementKind != ElementKind.PARAMETER && elementKind != ElementKind.LOCAL_VARIABLE) {
            return ImmutableList.of();
          }
          return ImmutableList.of(new SearchMatch(
              null,
              MatchKind.VARIABLE_WRITE,
              searchElement,
              SourceRangeFactory.rangeElementName(searchElement)));
        }

        private List<SearchMatch> getAccessibleMatches(List<SearchMatch> matches) {
          // just search for name
          if (searchElement == null) {
            return matches;
          }
          LibraryElement searchElementLibrary = searchElement.getLibrary();
          // prepare filtered matches
          List<SearchMatch> filteredMatches = Lists.newArrayList();
          for (SearchMatch match : matches) {
            Element matchElement = match.getElement();
            // HtmlElement has no enclosing LibraryElement to check, so always keep these matches
            if (matchElement instanceof HtmlElement) {
              filteredMatches.add(match);
              continue;
            }
            // check enclosing LibraryElement
            LibraryElement matchLibrary = matchElement.getLibrary();
            if (isImported(searchElementLibrary, matchLibrary)) {
              filteredMatches.add(match);
            }
          }
          // done
          return filteredMatches;
        }

        /**
         * Checks if "what" is imported into "where" directly or indirectly, so there is a chance
         * that it has access to an object from "what". Otherwise we find too many "second-order"
         * positive matches.
         * <p>
         * https://code.google.com/p/dart/issues/detail?id=12268
         */
        private boolean isImported(LibraryElement what, LibraryElement where) {
          Set<LibraryElement> visibleLibraries = cachedVisibleLibraries.get(where);
          if (visibleLibraries == null) {
            LibraryElement[] visibleLibrariesArray = where.getVisibleLibraries();
            visibleLibraries = ImmutableSet.copyOf(visibleLibrariesArray);
            cachedVisibleLibraries.put(where, visibleLibraries);
          }
          return visibleLibraries.contains(what);
        }
      });
    } catch (Throwable e) {
      ExceptionHandler.handle(e, getText(), "Exception during search.");
    }
  }
}
