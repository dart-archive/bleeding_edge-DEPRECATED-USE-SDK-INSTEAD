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

package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Arranges the imports and creates the text edit.
 */
//TODO remove annotation once cleaned up
@SuppressWarnings("unused")
public class ImportRewriteAnalyzer {

  private static final class DirectiveDeclEntry {

    private String elementName;
    private String prefix;
    private IRegion sourceRange;
    private int containerNameLength;

    public DirectiveDeclEntry(
        int containerNameLength, String elementName, String prefix, IRegion sourceRange) {
      this.elementName = elementName;
      this.prefix = prefix;
      this.sourceRange = sourceRange;
      this.containerNameLength = containerNameLength;
    }

    public int compareTo(String fullName) {
      int cmp = this.elementName.compareTo(fullName);
      return cmp;
    }

    public String getElementName() {
      return this.elementName;
    }

    public String getPrefix() {
      return this.prefix;
    }

    public IRegion getSourceRange() {
      return this.sourceRange;
    }

    public String getTypeQualifiedName() {
      return this.elementName.substring(this.containerNameLength + 1);
    }

    public boolean hasPrefix() {
      return this.prefix != null;
    }

    public boolean isComment() {
      return this.elementName == null;
    }

    public boolean isNew() {
      return this.sourceRange == null;
    }
  }

  /*
   * Internal element for the directive structure: A container for directives of the same type
   */
  private final static class PackageEntry {
    private String name;
    private ArrayList<DirectiveDeclEntry> importEntries;
    private String group;

    /**
     * Comment package entry
     */
    public PackageEntry() {
      this("!", null); //$NON-NLS-1$
    }

    /**
     * @param name Name of the package entry. e.g. dart:io
     * @param group The index of the preference order entry assigned different group id's will
     *          result in spacers between the entries
     */
    public PackageEntry(String name, String group) {
      this.name = name;
      this.importEntries = new ArrayList<DirectiveDeclEntry>(5);
      this.group = group;

    }

    public void add(DirectiveDeclEntry imp) {
      this.importEntries.add(imp);
    }

    public int compareTo(String otherName) {
      int cmp = this.name.compareTo(otherName);

      return cmp;
    }

    public DirectiveDeclEntry find(String simpleName) {
      int nInports = this.importEntries.size();
      for (int i = 0; i < nInports; i++) {
        DirectiveDeclEntry curr = getImportAt(i);
        if (!curr.isComment()) {
          String currName = curr.getElementName();
          if (currName.endsWith(simpleName)) {
            int dotPos = currName.length() - simpleName.length() - 1;
            if ((dotPos == -1) || (dotPos > 0 && currName.charAt(dotPos) == '.')) {
              return curr;
            }
          }
        }
      }
      return null;
    }

    public String getGroupID() {
      return this.group;
    }

    public DirectiveDeclEntry getImportAt(int index) {
      return this.importEntries.get(index);
    }

    public String getName() {
      return this.name;
    }

    public int getNumberOfImports() {
      return this.importEntries.size();
    }

    public boolean isComment() {
      return "!".equals(this.name); //$NON-NLS-1$
    }

    public boolean isDefaultPackage() {
      return this.name.length() == 0;
    }

    public boolean isSameGroup(PackageEntry other) {
      if (this.group == null) {
        return other.getGroupID() == null;
      } else {
        return this.group.equals(other.getGroupID());
      }
    }

    public boolean remove(String fullName) {
      int nInports = this.importEntries.size();
      for (int i = 0; i < nInports; i++) {
        DirectiveDeclEntry curr = getImportAt(i);
        if (!curr.isComment() && curr.compareTo(fullName) == 0) {
          this.importEntries.remove(i);
          return true;
        }
      }
      return false;
    }

    public void setGroupID(String groupID) {
      this.group = groupID;
    }

    public void sortIn(DirectiveDeclEntry imp) {
      String fullImportName = imp.getElementName();
      int insertPosition = -1;
      int nInports = this.importEntries.size();
      for (int i = 0; i < nInports; i++) {
        DirectiveDeclEntry curr = getImportAt(i);
        if (!curr.isComment()) {
          int cmp = curr.compareTo(fullImportName);
          if (cmp == 0) {
            return; // exists already
          } else if (cmp > 0 && insertPosition == -1) {
            insertPosition = i;
          }
        }
      }
      if (insertPosition == -1) {
        this.importEntries.add(imp);
      } else {
        this.importEntries.add(insertPosition, imp);
      }
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      if (isComment()) {
        buf.append("comment\n"); //$NON-NLS-1$
      } else {
        buf.append(this.name);
        buf.append(", groupId: ");buf.append(this.group);buf.append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
        int nImports = getNumberOfImports();
        for (int i = 0; i < nImports; i++) {
          DirectiveDeclEntry curr = getImportAt(i);
          buf.append(" "); //$NON-NLS-1$
          buf.append(curr.getTypeQualifiedName());
          if (curr.isNew()) {
            buf.append(" (new)"); //$NON-NLS-1$
          }
          buf.append("\n"); //$NON-NLS-1$
        }
      }
      return buf.toString();
    }
  }

  private static class PackageMatcher {
    private String newName;
    private String bestName;
    private int bestMatchLen;

    public PackageMatcher() {
      // initialization in 'initialize'
    }

    public void initialize(String newImportName, String bestImportName) {
      this.newName = newImportName;
      this.bestName = bestImportName;
      this.bestMatchLen = getCommonPrefixLength(bestImportName, newImportName);
    }

    public boolean isBetterMatch(String currName, boolean preferCurr) {
      boolean isBetter;
      int currMatchLen = getCommonPrefixLength(currName, this.newName);
      int matchDiff = currMatchLen - this.bestMatchLen;
      if (matchDiff == 0) {
        if (currMatchLen == this.newName.length() && currMatchLen == currName.length()
            && currMatchLen == this.bestName.length()) {
          // duplicate entry and complete match
          isBetter = preferCurr;
        } else {
          isBetter = sameMatchLenTest(currName);
        }
      } else {
        isBetter = (matchDiff > 0); // curr has longer match
      }
      if (isBetter) {
        this.bestName = currName;
        this.bestMatchLen = currMatchLen;
      }
      return isBetter;
    }

    private boolean sameMatchLenTest(String currName) {
      int matchLen = this.bestMatchLen;
      // known: bestName and currName differ from newName at position 'matchLen'
      // currName and bestName don't have to differ at position 'matchLen'

      // determine the order and return true if currName is closer to newName
      char newChar = getCharAt(this.newName, matchLen);
      char currChar = getCharAt(currName, matchLen);
      char bestChar = getCharAt(this.bestName, matchLen);

      if (newChar < currChar) {
        if (bestChar < newChar) { // b < n < c
          return (currChar - newChar) < (newChar - bestChar); // -> (c - n) < (n - b)
        } else { // n < b  && n < c
          if (currChar == bestChar) { // longer match between curr and best
            return false; // keep curr and best together, new should be before both
          } else {
            return currChar < bestChar; // -> (c < b)
          }
        }
      } else {
        if (bestChar > newChar) { // c < n < b
          return (newChar - currChar) < (bestChar - newChar); // -> (n - c) < (b - n)
        } else { // n > b  && n > c
          if (currChar == bestChar) { // longer match between curr and best
            return true; // keep curr and best together, new should be ahead of both
          } else {
            return currChar > bestChar; // -> (c > b)
          }
        }
      }
    }
  }

  /* package */static char getCharAt(String str, int index) {
    if (str.length() > index) {
      return str.charAt(index);
    }
    return 0;
  }

  /* package */static int getCommonPrefixLength(String s, String t) {
    int len = Math.min(s.length(), t.length());
    for (int i = 0; i < len; i++) {
      if (s.charAt(i) != t.charAt(i)) {
        return i;
      }
    }
    return len;
  }

  private static int getFirstTypeBeginPos(DartUnit root) {
    List<DartNode> types = root.getTopLevelNodes();
    if (!types.isEmpty()) {
      return types.get(0).getSourceInfo().getOffset();
    }
    return -1;
  }

  private final DartUnit ast;
  private final CompilationUnit cu;

  private final List<String> importsCreated;

  private final IRegion replaceRange;

  private int flags = 0;
  private List<PackageEntry> packageEntries = new ArrayList<PackageEntry>();
  private static final int F_NEEDS_LEADING_DELIM = 2;

  private static final int F_NEEDS_TRAILING_DELIM = 4;

  private String[] importOrder = new String[] {"dart", "package", "library"};

  public ImportRewriteAnalyzer(DartUnit ast, CompilationUnit cu, boolean restoreExistingImports,
      boolean useContextToFilterImplicitImports) {
    this.ast = ast;
    this.cu = cu;
    this.importsCreated = new ArrayList<String>();
    this.flags = 0;
    this.replaceRange = evaluateReplaceRange(ast);

    if (restoreExistingImports) {
      addExistingImports(ast);
    }

    PackageEntry[] order = new PackageEntry[importOrder.length];
    for (int i = 0; i < order.length; i++) {
      String curr = importOrder[i];
      order[i] = new PackageEntry(curr, curr);
    }

    addPreferenceOrderHolders(order);
  }

  public void addImport(DartImportDirective directive) {
    String typeContainerName = getQualifier(directive);
    DirectiveDeclEntry decl = new DirectiveDeclEntry(
        typeContainerName.length(),
        getFullName(directive),
        getPrefix(directive),
        null);
    sortIn(typeContainerName, decl);
  }

  public void addImport(String fullName) {

  }

  public String[] getCreatedImports() {
    // TODO(keertip): fill in method stub
    return null;
  }

  public TextEdit getResultingEdits(IProgressMonitor monitor) {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    MultiTextEdit resEdit = new MultiTextEdit();
    try {
      int importsStart = this.replaceRange.getOffset();
      int importsLen = this.replaceRange.getLength();

      String lineDelim = null;

      lineDelim = this.cu.findRecommendedLineSeparator();
      Buffer buffer = this.cu.getBuffer();
      System.out.println(buffer.toString());

      int currPos = importsStart;

      List<DartImportDirective> imports = getImports(ast);
      for (DartImportDirective directive : imports) {
        addImport(directive);
      }

      if ((this.flags & F_NEEDS_LEADING_DELIM) != 0) {
        // new import container
        resEdit.addChild(new InsertEdit(currPos, lineDelim));
      }

      PackageEntry lastPackage = null;

      ArrayList<String> stringsToInsert = new ArrayList<String>();

      int nPackageEntries = this.packageEntries.size();
      for (int i = 0; i < nPackageEntries; i++) {
        PackageEntry pack = this.packageEntries.get(i);
        int nImports = pack.getNumberOfImports();
        if (nImports == 0) {
          continue;
        }

        lastPackage = pack;

        for (int k = 0; k < nImports; k++) {
          DirectiveDeclEntry currDecl = pack.getImportAt(k);
          IRegion region = currDecl.getSourceRange();

          if (region == null) { // new entry

            String str = getNewImportString(
                currDecl.getElementName(),
                currDecl.getPrefix(),
                lineDelim);
            if (stringsToInsert.indexOf(str) == -1) {
              stringsToInsert.add(str);
            }

          } else {

            int offset = region.getOffset();
            //          removeAndInsertNew(buffer, currPos, offset, stringsToInsert, resEdit);
            stringsToInsert.clear();
            currPos = offset + region.getLength();
          }
        }

      }

      int end = importsStart + importsLen;
      removeAndInsertNew(buffer, currPos, end, stringsToInsert, resEdit);

      if (importsLen == 0) {
        if (!this.importsCreated.isEmpty()) { // new import container
          if ((this.flags & F_NEEDS_TRAILING_DELIM) != 0) {
            resEdit.addChild(new InsertEdit(currPos, lineDelim));
          }
        } else {
          return new MultiTextEdit(); // no changes
        }
      }

    } catch (DartModelException e) {
      DartToolsPlugin.log(e);

    } finally {
      monitor.done();
    }
    return resEdit;
  }

  public void removeImport(String substring) {
    // TODO(keertip): fill in method stub

  }

  public void setFilterImplicitImports(Object filterImplicitImports2) {
    // TODO(keertip): fill in stub

  }

  private void addExistingImports(DartUnit root) {
    List<DartImportDirective> imports = getImports(root);

    if (imports.isEmpty()) {
      return;
    }
    PackageEntry currPackage = null;

    DartImportDirective curr = imports.get(0);
    int currOffset = curr.getSourceInfo().getOffset();
    int currLength = curr.getSourceInfo().getLength();
    int currEndLine = curr.getSourceInfo().getLine();

    for (int i = 1; i < imports.size(); i++) {
      String name = getFullName(curr);
      String prefix = getPrefix(curr);
      String packName = getQualifier(curr);
      if (currPackage == null || currPackage.compareTo(packName) != 0) {
        currPackage = new PackageEntry(packName, null);
        this.packageEntries.add(currPackage);
      }

      DartImportDirective next = imports.get(i);
      int nextOffset = next.getSourceInfo().getOffset();
      int nextLength = next.getSourceInfo().getLength();
      int nextOffsetLine = next.getSourceInfo().getLine();

      currPackage.add(new DirectiveDeclEntry(packName.length(), name, prefix, new Region(
          currOffset,
          currLength)));
      currOffset = nextOffset;
      currLength = nextLength;
      curr = next;

      currEndLine = nextOffsetLine;
    }

    String name = getFullName(curr);
    String prefix = getPrefix(curr);
    String packName = getQualifier(curr);
    if (currPackage == null || currPackage.compareTo(packName) != 0) {
      currPackage = new PackageEntry(packName, null);
      this.packageEntries.add(currPackage);
    }
    int length = this.replaceRange.getOffset() + this.replaceRange.getLength()
        - curr.getSourceInfo().getOffset();
    currPackage.add(new DirectiveDeclEntry(packName.length(), name, prefix, new Region(
        curr.getSourceInfo().getOffset(),
        length)));
  }

  private void addPreferenceOrderHolders(PackageEntry[] preferenceOrder) {

    PackageEntry[] lastAssigned = new PackageEntry[preferenceOrder.length];

    // find an existing package entry that matches most
    for (int k = 0; k < this.packageEntries.size(); k++) {
      PackageEntry entry = this.packageEntries.get(k);
      String currName = entry.getName();
      int currNameLen = currName.length();
      int bestGroupIndex = -1;
      int bestGroupLen = -1;
      for (int i = 0; i < preferenceOrder.length; i++) {
        String currPrefEntry = preferenceOrder[i].getName();
        int currPrefLen = currPrefEntry.length();
        if (currName.startsWith(currPrefEntry) && currPrefLen >= bestGroupLen) {
          if (currPrefLen == currNameLen || currName.charAt(currPrefLen) == ':') {
            if (bestGroupIndex == -1 || currPrefLen > bestGroupLen) {
              bestGroupLen = currPrefLen;
              bestGroupIndex = i;
            }
          }
        }
      }
      if (bestGroupIndex != -1) {
        entry.setGroupID(preferenceOrder[bestGroupIndex].getName());
        lastAssigned[bestGroupIndex] = entry; // remember last entry
      }
    }

    // fill in not-assigned categories, keep partial order
    int currAppendIndex = 0;
    for (int i = 0; i < lastAssigned.length; i++) {
      PackageEntry entry = lastAssigned[i];
      if (entry == null) {
        PackageEntry newEntry = preferenceOrder[i];
        this.packageEntries.add(currAppendIndex, newEntry);
        currAppendIndex++;
      } else {
        currAppendIndex = this.packageEntries.indexOf(entry) + 1;
      }
    }

  }

  private IRegion evaluateReplaceRange(DartUnit root) {
    List<DartImportDirective> imports = getImports(root);
    if (!imports.isEmpty()) {
      DartImportDirective first = imports.get(0);
      DartImportDirective last = imports.get(imports.size() - 1);

      int startPos = first.getSourceInfo().getOffset();
      int endPos = last.getSourceInfo().getEnd();
      int endLine = last.getSourceInfo().getLine();
      int firstTypePos = getFirstTypeBeginPos(root);
      if (firstTypePos != -1 && firstTypePos < endPos) {
        endPos = firstTypePos;
      }

      return new Region(startPos, endPos - startPos);
    }
    return null;
  }

  private PackageEntry findBestMatch(String newName) {
    if (this.packageEntries.isEmpty()) {
      return null;
    }
    String groupId = null;
    int longestPrefix = -1;
    // find the matching group
    for (int i = 0; i < this.packageEntries.size(); i++) {
      PackageEntry curr = this.packageEntries.get(i);

      String currGroup = curr.getGroupID();
      if (currGroup != null && newName.startsWith(currGroup)) {
        int prefixLen = currGroup.length();
        if (prefixLen == newName.length()) {
          return curr; // perfect fit, use entry
        }
        if ((newName.charAt(prefixLen) == '.' || prefixLen == 0) && prefixLen > longestPrefix) {
          longestPrefix = prefixLen;
          groupId = currGroup;
        }
      }

    }
    PackageEntry bestMatch = null;
    PackageMatcher matcher = new PackageMatcher();
    matcher.initialize(newName, ""); //$NON-NLS-1$
    for (int i = 0; i < this.packageEntries.size(); i++) { // find the best match with the same group
      PackageEntry curr = this.packageEntries.get(i);
      if (!curr.isComment()) {
        if (groupId == null || groupId.equals(curr.getGroupID())) {
          boolean preferrCurr = (bestMatch == null) || (curr.getNumberOfImports()
              > bestMatch.getNumberOfImports());
          if (matcher.isBetterMatch(curr.getName(), preferrCurr)) {
            bestMatch = curr;
          }
        }
      }
    }
    return bestMatch;
  }

  private int findInBuffer(Buffer buffer, String str, int start, int end) {
    int pos = start;
    int len = str.length();
    if (pos + len > end || str.length() == 0) {
      return -1;
    }
    char first = str.charAt(0);
    int step = str.indexOf(first, 1);
    if (step == -1) {
      step = len;
    }
    while (pos + len <= end) {
      if (buffer.getChar(pos) == first) {
        int k = 1;

        while (k < len && buffer.getChar(pos + k) == str.charAt(k)) {
          k++;
        }
        if (k == len) {
          return pos; // found
        }
        if (k < step) {
          pos += k;
        } else {
          pos += step;
        }
      } else {
        pos++;
      }
    }
    return -1;
  }

  private String getFullName(DartImportDirective directive) {
    return directive.getLibraryUri().getValue();
  }

  private List<DartImportDirective> getImports(DartUnit root) {
    List<DartDirective> directives = root.getDirectives();
    List<DartImportDirective> imports = new ArrayList<DartImportDirective>();
    for (DartDirective directive : directives) {
      if (directive instanceof DartImportDirective) {
        imports.add((DartImportDirective) directive);
      }
    }
    return imports;
  }

  private String getNewImportString(String importName, String prefix, String lineDelim) {
    StringBuffer buf = new StringBuffer();
    buf.append("#import('"); //$NON-NLS-1$

    buf.append(importName);
    if (prefix != null) {
      buf.append("', prefix: '");
      buf.append(prefix);
    }
    buf.append("');");
    buf.append(lineDelim);

    this.importsCreated.add(importName);

    return buf.toString();
  }

  @SuppressWarnings("deprecation")
  private String getPrefix(DartImportDirective directive) {
    if (directive.getPrefix() != null) {
      return directive.getPrefix().getName();
    } else if (directive.getOldPrefix() != null) {
      return directive.getOldPrefix().toString();
    }
    return null;
  }

  private String getQualifier(DartImportDirective directive) {
    String string = directive.getLibraryUri().toString();
    if (string.indexOf("dart:") != -1) {
      return "dart";
    } else if (string.indexOf("package:") != -1) {
      return "package";
    }
    return "library";
  }

  private void removeAndInsertNew(Buffer buffer, int contentOffset, int contentEnd,
      ArrayList<String> stringsToInsert, MultiTextEdit resEdit) {
    int pos = contentOffset;
    for (int i = 0; i < stringsToInsert.size(); i++) {
      String curr = stringsToInsert.get(i);
      int idx = findInBuffer(buffer, curr, pos, contentEnd);
      if (idx != -1) {
        if (idx != pos) {
          resEdit.addChild(new DeleteEdit(pos, idx - pos));
        }
        pos = idx + curr.length();
      } else {
        resEdit.addChild(new InsertEdit(pos, curr));
      }
    }
    if (pos < contentEnd) {
      resEdit.addChild(new DeleteEdit(pos, contentEnd - pos));
    }
  }

  private void sortIn(String typeContainerName, DirectiveDeclEntry decl) {
    PackageEntry bestMatch = findBestMatch(typeContainerName);
    if (bestMatch == null) {
      PackageEntry packEntry = new PackageEntry(typeContainerName, null);
      packEntry.add(decl);
      this.packageEntries.add(0, packEntry);
    } else {
      int cmp = typeContainerName.compareTo(bestMatch.getName());
      if (cmp == 0) {
        bestMatch.sortIn(decl);
      } else {
        // create a new package entry
        String group = bestMatch.getGroupID();
        if (group != null) {
          if (!typeContainerName.startsWith(group)) {
            group = null;
          }
        }
        PackageEntry packEntry = new PackageEntry(typeContainerName, group);
        packEntry.add(decl);
        int index = this.packageEntries.indexOf(bestMatch);
        if (cmp < 0) {
          // insert ahead of best match
          this.packageEntries.add(index, packEntry);
        } else {
          // insert after best match
          this.packageEntries.add(index + 1, packEntry);
        }
      }
    }
  }

}
