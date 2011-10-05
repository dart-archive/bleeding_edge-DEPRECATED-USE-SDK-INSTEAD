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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartDocContentAccess;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartDoc2HTMLTextReader;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.IOException;
import java.io.Reader;

public class ProposalInfo {

  /**
   * Gets the reader content as a String
   */
  private static String getString(Reader reader) {
    StringBuffer buf = new StringBuffer();
    char[] buffer = new char[1024];
    int count;
    try {
      while ((count = reader.read(buffer)) != -1) {
        buf.append(buffer, 0, count);
      }
    } catch (IOException e) {
      return null;
    }
    return buf.toString();
  }

  private boolean fJavadocResolved = false;

  private String fJavadoc = null;

  protected DartElement fElement;

  public ProposalInfo(Type type) {
    fElement = type;
  }

  public ProposalInfo(TypeMember member) {
    fElement = member;
  }

  protected ProposalInfo() {
    fElement = null;
  }

  /**
   * Gets the text for this proposal info formatted as HTML, or <code>null</code> if no text is
   * available.
   * 
   * @param monitor a progress monitor
   * @return the additional info text
   */
  public final String getInfo(IProgressMonitor monitor) {
    if (!fJavadocResolved) {
      fJavadocResolved = true;
      fJavadoc = computeInfo(monitor);
    }
    return fJavadoc;
  }

  public DartElement getJavaElement() throws DartModelException {
    return fElement;
  }

  /**
   * Gets the text for this proposal info formatted as HTML, or <code>null</code> if no text is
   * available.
   * 
   * @param monitor a progress monitor
   * @return the additional info text
   */
  private String computeInfo(IProgressMonitor monitor) {
    try {
      final DartElement javaElement = getJavaElement();
      if (javaElement instanceof TypeMember) {
        TypeMember member = (TypeMember) javaElement;
        return extractJavadoc(member, monitor);
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    } catch (IOException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  /**
   * Extracts the javadoc for the given <code>IMember</code> and returns it as HTML.
   * 
   * @param member the member to get the documentation for
   * @param monitor a progress monitor
   * @return the javadoc for <code>member</code> or <code>null</code> if it is not available
   * @throws DartModelException if accessing the javadoc fails
   * @throws IOException if reading the javadoc fails
   */
  private String extractJavadoc(TypeMember member, IProgressMonitor monitor)
      throws DartModelException, IOException {
    if (member != null) {
      Reader reader = getHTMLContentReader(member, monitor);
      if (reader != null) {
        return getString(reader);
      }
    }
    return null;
  }

  private Reader getHTMLContentReader(TypeMember member, IProgressMonitor monitor)
      throws DartModelException {
    Reader contentReader = DartDocContentAccess.getHTMLContentReader(member, true, true);
    if (contentReader != null) {
      return contentReader;
    }

    contentReader = DartDocContentAccess.getContentReader(member, true);
    if (contentReader != null) {
      return new DartDoc2HTMLTextReader(contentReader);
    }

    if (member.getOpenable().getBuffer() == null) {
      // only if no source available
      // TODO(devoncarew): this method is only valid for binary elements
//      String s = member.getAttachedJavadoc(monitor);
//      if (s != null)
//        return new StringReader(s);
    }
    return null;
  }
}
