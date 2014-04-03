/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.scanner.Token;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Instances of the class {@code ImportDirective} represent an import directive.
 * 
 * <pre>
 * importDirective ::=
 *     {@link Annotation metadata} 'import' {@link StringLiteral libraryUri} ('as' identifier)? {@link Combinator combinator}* ';'
 *   | {@link Annotation metadata} 'import' {@link StringLiteral libraryUri} 'deferred' 'as' identifier {@link Combinator combinator}* ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ImportDirective extends NamespaceDirective {

  public static final Comparator<ImportDirective> COMPARATOR = new Comparator<ImportDirective>() {
    @Override
    public int compare(ImportDirective import1, ImportDirective import2) {
      //
      // uri
      //
      StringLiteral uri1 = import1.getUri();
      StringLiteral uri2 = import2.getUri();
      String uriStr1 = uri1.getStringValue();
      String uriStr2 = uri2.getStringValue();
      if (uriStr1 != null || uriStr2 != null) {
        if (uriStr1 == null) {
          return -1;
        } else if (uriStr2 == null) {
          return 1;
        } else {
          int compare = uriStr1.compareTo(uriStr2);
          if (compare != 0) {
            return compare;
          }
        }
      }

      //
      // as
      //
      SimpleIdentifier prefix1 = import1.getPrefix();
      SimpleIdentifier prefix2 = import2.getPrefix();
      String prefixStr1 = prefix1 != null ? prefix1.getName() : null;
      String prefixStr2 = prefix2 != null ? prefix2.getName() : null;
      if (prefixStr1 != null || prefixStr2 != null) {
        if (prefixStr1 == null) {
          return -1;
        } else if (prefixStr2 == null) {
          return 1;
        } else {
          int compare = prefixStr1.compareTo(prefixStr2);
          if (compare != 0) {
            return compare;
          }
        }
      }

      //
      // hides and shows
      //
      NodeList<Combinator> combinators1 = import1.getCombinators();
      ArrayList<String> allHides1 = new ArrayList<String>();
      ArrayList<String> allShows1 = new ArrayList<String>();
      for (Combinator combinator : combinators1) {
        if (combinator instanceof HideCombinator) {
          NodeList<SimpleIdentifier> hides = ((HideCombinator) combinator).getHiddenNames();
          for (SimpleIdentifier simpleIdentifier : hides) {
            allHides1.add(simpleIdentifier.getName());
          }
        } else {
          NodeList<SimpleIdentifier> shows = ((ShowCombinator) combinator).getShownNames();
          for (SimpleIdentifier simpleIdentifier : shows) {
            allShows1.add(simpleIdentifier.getName());
          }
        }
      }
      NodeList<Combinator> combinators2 = import2.getCombinators();
      ArrayList<String> allHides2 = new ArrayList<String>();
      ArrayList<String> allShows2 = new ArrayList<String>();
      for (Combinator combinator : combinators2) {
        if (combinator instanceof HideCombinator) {
          NodeList<SimpleIdentifier> hides = ((HideCombinator) combinator).getHiddenNames();
          for (SimpleIdentifier simpleIdentifier : hides) {
            allHides2.add(simpleIdentifier.getName());
          }
        } else {
          NodeList<SimpleIdentifier> shows = ((ShowCombinator) combinator).getShownNames();
          for (SimpleIdentifier simpleIdentifier : shows) {
            allShows2.add(simpleIdentifier.getName());
          }
        }
      }
      // test lengths of combinator lists first
      if (allHides1.size() != allHides2.size()) {
        return allHides1.size() - allHides2.size();
      }
      if (allShows1.size() != allShows2.size()) {
        return allShows1.size() - allShows2.size();
      }
      // next ensure that the lists are equivalent
      if (!allHides1.containsAll(allHides2)) {
        return -1;
      }
      if (!allShows1.containsAll(allShows2)) {
        return -1;
      }
      return 0;
    }
  };

  /**
   * The token representing the 'deferred' token, or {@code null} if the imported is not deferred.
   */
  private Token deferredToken;

  /**
   * The token representing the 'as' token, or {@code null} if the imported names are not prefixed.
   */
  private Token asToken;

  /**
   * The prefix to be used with the imported names, or {@code null} if the imported names are not
   * prefixed.
   */
  private SimpleIdentifier prefix;

  /**
   * Initialize a newly created import directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'import' keyword
   * @param libraryUri the URI of the library being imported
   * @param deferredToken the token representing the 'deferred' token
   * @param asToken the token representing the 'as' token
   * @param prefix the prefix to be used with the imported names
   * @param combinators the combinators used to control how names are imported
   * @param semicolon the semicolon terminating the directive
   */
  public ImportDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, Token deferredToken, Token asToken, SimpleIdentifier prefix,
      List<Combinator> combinators, Token semicolon) {
    super(comment, metadata, keyword, libraryUri, combinators, semicolon);
    this.deferredToken = deferredToken;
    this.asToken = asToken;
    this.prefix = becomeParentOf(prefix);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitImportDirective(this);
  }

  /**
   * Return the token representing the 'as' token, or {@code null} if the imported names are not
   * prefixed.
   * 
   * @return the token representing the 'as' token
   */
  public Token getAsToken() {
    return asToken;
  }

  /**
   * Return the token representing the 'deferred' token, or {@code null} if the imported is not
   * deferred.
   * 
   * @return the token representing the 'deferred' token
   */
  public Token getDeferredToken() {
    return deferredToken;
  }

  @Override
  public ImportElement getElement() {
    return (ImportElement) super.getElement();
  }

  /**
   * Return the prefix to be used with the imported names, or {@code null} if the imported names are
   * not prefixed.
   * 
   * @return the prefix to be used with the imported names
   */
  public SimpleIdentifier getPrefix() {
    return prefix;
  }

  @Override
  public LibraryElement getUriElement() {
    ImportElement element = getElement();
    if (element == null) {
      return null;
    }
    return element.getImportedLibrary();
  }

  /**
   * Set the token representing the 'as' token to the given token.
   * 
   * @param asToken the token representing the 'as' token
   */
  public void setAsToken(Token asToken) {
    this.asToken = asToken;
  }

  /**
   * Set the token representing the 'deferred' token to the given token.
   * 
   * @param deferredToken the token representing the 'deferred' token
   */
  public void setDeferredToken(Token deferredToken) {
    this.deferredToken = deferredToken;
  }

  /**
   * Set the prefix to be used with the imported names to the given identifier.
   * 
   * @param prefix the prefix to be used with the imported names
   */
  public void setPrefix(SimpleIdentifier prefix) {
    this.prefix = becomeParentOf(prefix);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(prefix, visitor);
    getCombinators().accept(visitor);
  }
}
