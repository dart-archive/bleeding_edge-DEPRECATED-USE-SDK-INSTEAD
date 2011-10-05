/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.core.ILocalVariable;
import com.google.dart.core.util.DartDocCommentReader;
import com.google.dart.core.util.MethodOverrideTester;
import com.google.dart.core.util.SequenceReader;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.core.model.TypeMember;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper needed to get the content of a Javadoc comment.
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartDocContentAccess {

  public static final String EXTENSION_POINT = "documentationProvider"; //$NON-NLS-1$

  protected static final String TAG_DOCUMENTATIONPROVIDER = "documentationProvider"; //$NON-NLS-1$
  protected static final String ATTR_DOCUMENTATIONPROVIDER_CLASS = "class"; //$NON-NLS-1$

  private static IDocumentationReader[] docReaders;

  /**
   * Gets a reader for an ILocalDeclaration's doc comment content from the source attachment.
   * Returns <code>null</code> if the declaration does not have a doc comment or if no source is
   * available.
   * 
   * @param declaration The declaration to get the doc of.
   * @param allowInherited For methods with no doc comment, the comment of the overridden class is
   *          returned if <code>allowInherited</code> is <code>true</code> and this is an argument.
   * @return Returns a reader for the doc comment content or <code>null</code> if the declaration
   *         does not contain a doc comment or if no source is available
   * @throws DartModelException is thrown when the declaration's doc can not be accessed
   */
  public static Reader getContentReader(ILocalVariable declaration, boolean allowInherited)
      throws DartModelException {
    List<Reader> readers = new ArrayList<Reader>(2);
    IDocumentationReader[] docReaders = getDocReaders(declaration);
    for (int i = 0; i < docReaders.length; i++) {
      Reader contentReader = docReaders[i].getContentReader(declaration, allowInherited);
      if (contentReader != null) {
        readers.add(contentReader);
      }
    }

    if (!readers.isEmpty()) {
      if (readers.size() == 1) {
        return readers.get(0);
      }
      return new SequenceReader(readers.toArray(new Reader[readers.size()]));
    }
    return null;
  }

  /**
   * Gets a reader for an IMember's Javadoc comment content from the source attachment. The content
   * does contain only the text from the comment without the Javadoc leading star characters.
   * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is
   * available.
   * 
   * @param member The member to get the Javadoc of.
   * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden
   *          class is returned if <code>allowInherited</code> is <code>true</code>.
   * @return Returns a reader for the Javadoc comment content or <code>null</code> if the member
   *         does not contain a Javadoc comment or if no source is available
   * @throws DartModelException is thrown when the elements javadoc can not be accessed
   */
  public static Reader getContentReader(TypeMember member, boolean allowInherited)
      throws DartModelException {
    List<Reader> readers = new ArrayList<Reader>(2);
    IDocumentationReader[] docReaders = getDocReaders(member);
    for (int i = 0; i < docReaders.length; i++) {
      Reader contentReader = docReaders[i].getContentReader(member, allowInherited);
      if (contentReader != null) {
        readers.add(contentReader);
      }
    }

    OpenableElement openable = member.getOpenable();
    // if (openable instanceof MetadataFile)
    // {
    // return new OAADocReader((MetadataFile)openable, member);
    // }

    Buffer buf = openable.getBuffer();
    if (buf != null) {
      try {
        // source or attachment found
        SourceRange jsDocRange = member.getDartDocRange();
        if (jsDocRange == null && member.getElementType() == DartElement.TYPE) {
          Method constructor = ((Type) member).getMethod(member.getElementName(), null);
          if (constructor.exists()) {
            jsDocRange = constructor.getDartDocRange();
          }
        }
        if (jsDocRange != null) {
          DartDocCommentReader reader = new DartDocCommentReader(buf, jsDocRange.getOffset(),
              jsDocRange.getOffset() + jsDocRange.getLength() - 1);
          if (!containsOnlyInheritDoc(reader, jsDocRange.getLength())) {
            reader.reset();
            readers.add(reader);
          } else if (allowInherited && (member.getElementType() == DartElement.METHOD)) {
            Reader hierarchyDocReader = findDocInHierarchy((Method) member);
            if (hierarchyDocReader != null) {
              readers.add(hierarchyDocReader);
            }
          }
        }
      } catch (DartModelException e) {
        // doesn't exist
      }
    }

    if (!readers.isEmpty()) {
      if (readers.size() == 1) {
        return readers.get(0);
      }
      return new SequenceReader(readers.toArray(new Reader[readers.size()]));
    }
    return null;
  }

  // TODO(devoncarew): This is not currently called.
//  /**
//   * Gets a reader for an ILocalDeclaration documentation comment content. and
//   * renders the tags in HTML. Returns <code>null</code> if the declaration does
//   * not contain a doc comment or if no source is available.
//   * 
//   * @param variable the variable declaration to get the doc of.
//   * @param allowInherited for methods with no (JSDoc) comment, the comment of
//   *          the overridden class is returned if <code>allowInherited</code> is
//   *          <code>true</code>
//   * @param useAttachedDoc if <code>true</code> JSDoc will be extracted from
//   *          attached JSDoc if there's no source
//   * @return a reader for the JSDoc comment content in HTML or <code>null</code>
//   *         if the member does not contain a JSDoc comment or if no source is
//   *         available
//   * @throws DartModelException is thrown when the elements JSDoc can not be
//   *           accessed
//   */
//  public static Reader getHTMLContentReader(ILocalVariable variable,
//      boolean allowInherited, boolean useAttachedDoc) throws DartModelException {
//    Reader contentReader = getContentReader(variable, allowInherited);
//    if (contentReader != null) {
//      IDocumentationReader[] docReaders = new IDocumentationReader[0];// getDocReaders(declaration);
//      if (docReaders.length > 0) {
//        List htmlReaders = new ArrayList(docReaders.length);
//        for (int i = 0; i < docReaders.length; i++) {
//          Reader documentation2htmlReader = docReaders[i].getDocumentation2HTMLReader(contentReader);
//          if (documentation2htmlReader != null) {
//            htmlReaders.add(documentation2htmlReader);
//          }
//        }
//        if (!htmlReaders.isEmpty()) {
//          htmlReaders.add(/* 0, */new JavaDoc2HTMLTextReader(contentReader));
//          return new SequenceReader(
//              (Reader[]) htmlReaders.toArray(new Reader[htmlReaders.size()]));
//        }
//      }
//      return new JavaDoc2HTMLTextReader(contentReader);
//    }
//
//    return null;
//  }

  /**
   * Gets a reader for an IMember's Javadoc comment content from the source attachment. and renders
   * the tags in HTML. Returns <code>null</code> if the member does not contain a Javadoc comment or
   * if no source is available.
   * 
   * @param member the member to get the Javadoc of.
   * @param allowInherited for methods with no (Javadoc) comment, the comment of the overridden
   *          class is returned if <code>allowInherited</code> is <code>true</code>
   * @param useAttachedJavadoc if <code>true</code> Javadoc will be extracted from attached Javadoc
   *          if there's no source
   * @return a reader for the Javadoc comment content in HTML or <code>null</code> if the member
   *         does not contain a Javadoc comment or if no source is available
   * @throws DartModelException is thrown when the elements Javadoc can not be accessed
   */
  public static Reader getHTMLContentReader(TypeMember member, boolean allowInherited,
      boolean useAttachedJavadoc) throws DartModelException {
    Reader contentReader = getContentReader(member, allowInherited);
    if (contentReader != null) {
      IDocumentationReader[] docReaders = getDocReaders(member);
      if (docReaders.length > 0) {
        List<Reader> htmlReaders = new ArrayList<Reader>(docReaders.length);
        for (int i = 0; i < docReaders.length; i++) {
          Reader htmlReader = docReaders[i].getDocumentation2HTMLReader(contentReader);
          if (htmlReader != null) {
            htmlReaders.add(htmlReader);
          }
        }
        /* return any and all HTML readers in sequence */
        if (!htmlReaders.isEmpty()) {
          // htmlReaders.add(/*0, */new JavaDoc2HTMLTextReader(contentReader));
          return new SequenceReader(htmlReaders.toArray(new Reader[htmlReaders.size()]));
        }
      }
      return new DartDoc2HTMLTextReader(contentReader);
    }

    // only if no source available
    if (useAttachedJavadoc && member.getOpenable().getBuffer() == null) {
//      // TODO(devoncarew): this javadoc method only applies to binary elements
//      String s = member.getAttachedJavadoc(null);
//      if (s != null)
//        return new StringReader(s);
    }
    return null;
  }

  /**
   * Checks whether the given reader only returns the inheritDoc tag.
   * 
   * @param reader the reader
   * @param length the length of the underlying content
   * @return <code>true</code> if the reader only returns the inheritDoc tag
   */
  private static boolean containsOnlyInheritDoc(Reader reader, int length) {
    char[] content = new char[length];
    try {
      reader.read(content, 0, length);
    } catch (IOException e) {
      return false;
    }
    return new String(content).trim().equals("{@inheritDoc}"); //$NON-NLS-1$

  }

  private static Reader findDocInHierarchy(Method method) throws DartModelException {
    Type type = method.getDeclaringType();
    if (type == null) {
      return null;
    }
    TypeHierarchy hierarchy = type.newSupertypeHierarchy(null);

    MethodOverrideTester tester = new MethodOverrideTester(type, hierarchy);

    Type[] superTypes = hierarchy.getAllSuperclasses(type);
    for (int i = 0; i < superTypes.length; i++) {
      Type curr = superTypes[i];
      Method overridden = tester.findOverriddenMethodInType(curr, method);
      if (overridden != null) {
        Reader reader = getContentReader(overridden, false);
        if (reader != null) {
          return reader;
        }
      }
    }
    return null;
  }

  private static IDocumentationReader[] getDocReaders(ILocalVariable declaration) {
    if (docReaders == null) {
      loadExtensions();
    }
    List<IDocumentationReader> readers = new ArrayList<IDocumentationReader>(docReaders.length);
    for (int i = 0; i < docReaders.length; i++) {
      if (docReaders[i].appliesTo(declaration)) {
        readers.add(docReaders[i]);
      }
    }
    return readers.toArray(new IDocumentationReader[readers.size()]);
  }

  private static IDocumentationReader[] getDocReaders(TypeMember member) {
    if (docReaders == null) {
      loadExtensions();
    }
    List<IDocumentationReader> readers = new ArrayList<IDocumentationReader>(docReaders.length);
    for (int i = 0; i < docReaders.length; i++) {
      if (docReaders[i].appliesTo(member)) {
        readers.add(docReaders[i]);
      }
    }
    return readers.toArray(new IDocumentationReader[readers.size()]);
  }

  private static void loadExtensions() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    ArrayList<IDocumentationReader> extList = new ArrayList<IDocumentationReader>();
    if (registry != null) {
      IExtensionPoint point = registry.getExtensionPoint(DartToolsPlugin.getPluginId(),
          EXTENSION_POINT);

      if (point != null) {
        IExtension[] extensions = point.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
          IConfigurationElement[] elements = extensions[i].getConfigurationElements();
          for (int j = 0; j < elements.length; j++) {
            try {
              IDocumentationReader docProvider = null;
              if (elements[j].getName().equals(TAG_DOCUMENTATIONPROVIDER)) {
                docProvider = (IDocumentationReader) elements[j].createExecutableExtension(ATTR_DOCUMENTATIONPROVIDER_CLASS);
              }

              extList.add(docProvider);
            } catch (CoreException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    docReaders = extList.toArray(new IDocumentationReader[extList.size()]);
  }

  private DartDocContentAccess() {
    // do not instantiate
  }

}
