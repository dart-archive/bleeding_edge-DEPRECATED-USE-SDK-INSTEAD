package com.google.dart.dev.util.analysis;

import com.google.dart.engine.ast.AnnotatedNode;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.Literal;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.internal.cache.AnalysisCache;
import com.google.dart.engine.internal.cache.CachePartition;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.internal.cache.HtmlEntry;
import com.google.dart.engine.internal.cache.HtmlEntryImpl;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MemoryUsageData {
  private static class ClassData {
    public static final Comparator<MemoryUsageData.ClassData> SORT_BY_NAME = new Comparator<MemoryUsageData.ClassData>() {
      @Override
      public int compare(MemoryUsageData.ClassData firstData, MemoryUsageData.ClassData secondData) {
        return firstData.getClassObject().getName().compareTo(secondData.getClassObject().getName());
      }
    };

    public static final Comparator<MemoryUsageData.ClassData> SORT_BY_TOTAL_SIZE = new Comparator<MemoryUsageData.ClassData>() {
      @Override
      public int compare(MemoryUsageData.ClassData firstData, MemoryUsageData.ClassData secondData) {
        return secondData.getTotalSize() - firstData.getTotalSize();
      }
    };

    private Class<?> classObject;

    private int objectSize;

    private int objectCount;

    private HashMap<String, Integer> counts;

    public ClassData(Class<?> classObject) {
      this.classObject = classObject;
      objectSize = computeObjectSize();
    }

    public Class<?> getClassObject() {
      return classObject;
    }

    public int getCount(String name) {
      Integer count = counts.get(name);
      if (count == null) {
        return 0;
      }
      return count;
    }

    public HashMap<String, Integer> getCounts() {
      return counts;
    }

    public int getObjectCount() {
      return objectCount;
    }

    public int getObjectSize() {
      return objectSize;
    }

    public int getTotalSize() {
      return objectSize * objectCount;
    }

    public void incrementCount() {
      objectCount++;
    }

    public void incrementCount(String name, int delta) {
      if (counts == null) {
        counts = new HashMap<String, Integer>();
      }
      Integer count = counts.get(name);
      if (count == null) {
        count = 0;
      }
      counts.put(name, count + delta);
    }

    public void incrementCountIfNotEmpty(String countName, List<?> list) {
      incrementCountIfTrue("non-empty " + countName, list != null && list.size() > 0);
    }

    public void incrementCountIfNotNull(String countName, Object value) {
      incrementCountIfTrue(countName, value != null);
    }

    public void incrementCountIfTrue(String countName, boolean value) {
      incrementCount(countName, value ? 1 : 0);
    }

    public void incrementFieldCount(String fieldName, Object object) {
      try {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        incrementCountIfNotNull(fieldName, field.get(object));
      } catch (Exception exception) {
        // Ignored
      }
    }

    private int computeObjectSize() {
      Class<?> currentClass = classObject;
      int size = 0;
      while (currentClass != null) {
        Field[] fields = currentClass.getDeclaredFields();
        for (Field field : fields) {
          if (!Modifier.isStatic(field.getModifiers())) {
            size++;
          }
        }
        currentClass = currentClass.getSuperclass();
      }
      return size;
    }
  }

  /**
   * A table mapping the classes for which size information is being computed to the information for
   * that class.
   */
  private HashMap<Class<?>, MemoryUsageData.ClassData> classData = new HashMap<Class<?>, MemoryUsageData.ClassData>();

  /**
   * A collection of cache partitions that have been visited, used to prevent the same partition
   * from being counted multiple times.
   */
  private HashSet<CachePartition> visitedPartitions = new HashSet<CachePartition>();

  private static final int[] CLASS_SPECIFIC_ALIGNMENTS = new int[] {
      PrintStringWriter.ALIGN_LEFT, PrintStringWriter.ALIGN_RIGHT, PrintStringWriter.ALIGN_RIGHT};

  private static final int[] CLASS_USAGE_ALIGNMENTS = new int[] {
      PrintStringWriter.ALIGN_LEFT, PrintStringWriter.ALIGN_RIGHT, PrintStringWriter.ALIGN_RIGHT,
      PrintStringWriter.ALIGN_RIGHT};

  private static final int[] SUMMARY_ALIGNMENTS = new int[] {
      PrintStringWriter.ALIGN_LEFT, PrintStringWriter.ALIGN_RIGHT};

  public void addContext(InternalAnalysisContext context) {
    AnalysisCache cache = getAnalysisCache(context);
    if (cache == null) {
      return;
    }
    CachePartition[] partitions = getPartitions(cache);
    if (partitions == null) {
      return;
    }
    for (CachePartition partition : partitions) {
      if (visitedPartitions.add(partition)) {
        for (SourceEntry entry : partition.getMap().values()) {
          addCacheEntry(entry);
        }
      }
    }
  }

  public String getReport() {
    MemoryUsageData.ClassData[] entries = classData.values().toArray(
        new MemoryUsageData.ClassData[classData.size()]);
    Arrays.sort(entries, ClassData.SORT_BY_TOTAL_SIZE);
    int rowCount = entries.length;
    int totalTotalSize = 0;
    int totalObjectCount = 0;
    String[][] data = new String[rowCount + 1][];
    data[0] = new String[] {"Class Name", "Total Size", "Object Count", "Object Size"};
    for (int i = 0; i < rowCount; i++) {
      ClassData entry = entries[i];
      int totalSize = entry.getTotalSize();
      totalTotalSize += totalSize;
      int objectCount = entry.getObjectCount();
      totalObjectCount += objectCount;
      data[i + 1] = new String[] {
          entry.getClassObject().getName(), Integer.toString(totalSize),
          Integer.toString(objectCount), Integer.toString(entry.getObjectSize())};
    }

    PrintStringWriter writer = new PrintStringWriter();
    writer.println("Summary");
    writer.println();
    writer.printTable(
        new String[][] {
            {"Total memory usage:", Integer.toString(totalTotalSize)},
            {"Total object count:", Integer.toString(totalObjectCount)}}, SUMMARY_ALIGNMENTS);
    writer.println();
    writer.println("Usage by Class");
    writer.println();
    writer.printTable(data, CLASS_USAGE_ALIGNMENTS);
    writer.println();
    writer.println("Class-specific Counts");
//    Arrays.sort(entries, ClassData.SORT_BY_NAME);
    for (int i = 0; i < rowCount; i++) {
      printClassSpecificReportData(writer, entries[i]);
    }
    return writer.toString();
  }

  private void addAllObjects(Object[] objects) {
    if (objects != null) {
      for (Object object : objects) {
        if (object != null) {
          MemoryUsageData.ClassData data = getDataFor(object.getClass());
          data.incrementCount();
        }
      }
    }
  }

  private void addAst(CompilationUnit unit) {
    if (unit != null) {
      addTokens(unit.getBeginToken());
      unit.accept(new GeneralizingAstVisitor<Void>() {
        @Override
        public Void visitAnnotatedNode(AnnotatedNode node) {
          ClassData data = getDataFor(AnnotatedNode.class);
          data.incrementCount();
          data.incrementCountIfNotNull("documentationComment", node.getDocumentationComment());
          data.incrementCountIfNotEmpty("metadata", node.getMetadata());
          return super.visitAnnotatedNode(node);
        }

        @Override
        public Void visitAnnotation(Annotation node) {
          ClassData data = getDataFor(Annotation.class);
          data.incrementCountIfNotNull("element", node.getElement());
          data.incrementCountIfNotNull("elementAnnotation", node.getElementAnnotation());
          return super.visitAnnotation(node);
        }

        @Override
        public Void visitArgumentList(ArgumentList node) {
          ClassData data = getDataFor(ArgumentList.class);
          data.incrementCountIfNotEmpty("arguments", node.getArguments());
          data.incrementFieldCount("correspondingPropagatedParameters", node);
          data.incrementFieldCount("correspondingStaticParameters", node);
          return super.visitArgumentList(node);
        }

        @Override
        public Void visitAsExpression(AsExpression node) {
          ClassData data = getDataFor(AsExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitAsExpression(node);
        }

        @Override
        public Void visitAssignmentExpression(AssignmentExpression node) {
          ClassData data = getDataFor(AssignmentExpression.class);
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          if (node.getOperator().getType() != TokenType.EQ) {
            data.incrementCount("compound", 1);
          }
          return super.visitAssignmentExpression(node);
        }

        @Override
        public Void visitBinaryExpression(BinaryExpression node) {
          ClassData data = getDataFor(BinaryExpression.class);
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitBinaryExpression(node);
        }

        @Override
        public Void visitBlock(Block node) {
          ClassData data = getDataFor(Block.class);
          data.incrementCountIfNotEmpty("statements", node.getStatements());
          return super.visitBlock(node);
        }

        @Override
        public Void visitBreakStatement(BreakStatement node) {
          ClassData data = getDataFor(BreakStatement.class);
          data.incrementCountIfNotNull("label", node.getLabel());
          return super.visitBreakStatement(node);
        }

        @Override
        public Void visitCascadeExpression(CascadeExpression node) {
          ClassData data = getDataFor(CascadeExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitCascadeExpression(node);
        }

        @Override
        public Void visitComment(Comment node) {
          ClassData data = getDataFor(Comment.class);
          data.incrementCountIfNotEmpty("references", node.getReferences());
          return super.visitComment(node);
        }

        @Override
        public Void visitCompilationUnit(CompilationUnit node) {
          ClassData data = getDataFor(CompilationUnit.class);
          data.incrementCountIfNotEmpty("declarations", node.getDeclarations());
          data.incrementCountIfNotEmpty("directives", node.getDirectives());
          data.incrementCountIfNotNull("lineInfo", node.getLineInfo());
          return super.visitCompilationUnit(node);
        }

        @Override
        public Void visitConditionalExpression(ConditionalExpression node) {
          ClassData data = getDataFor(ConditionalExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitConditionalExpression(node);
        }

        @Override
        public Void visitConstructorDeclaration(ConstructorDeclaration node) {
          ClassData data = getDataFor(ConstructorDeclaration.class);
          data.incrementCountIfNotEmpty("initializers", node.getInitializers());
          return super.visitConstructorDeclaration(node);
        }

        @Override
        public Void visitContinueStatement(ContinueStatement node) {
          ClassData data = getDataFor(ContinueStatement.class);
          data.incrementCountIfNotNull("label", node.getLabel());
          return super.visitContinueStatement(node);
        }

        @Override
        public Void visitExportDirective(ExportDirective node) {
          ClassData data = getDataFor(ExportDirective.class);
          data.incrementCountIfNotEmpty("combinators", node.getCombinators());
          return super.visitExportDirective(node);
        }

        @Override
        public Void visitExpression(Expression node) {
          ClassData data = getDataFor(Expression.class);
          data.incrementCount();
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitExpression(node);
        }

        @Override
        public Void visitFormalParameterList(FormalParameterList node) {
          ClassData data = getDataFor(FormalParameterList.class);
          data.incrementCountIfNotEmpty("", node.getParameters());
          return super.visitFormalParameterList(node);
        }

        @Override
        public Void visitForStatement(ForStatement node) {
          ClassData data = getDataFor(ForStatement.class);
          data.incrementCountIfNotNull("initialization", node.getInitialization());
          data.incrementCountIfNotNull("variables", node.getVariables());
          data.incrementCountIfNotEmpty("updaters", node.getUpdaters());
          return super.visitForStatement(node);
        }

        @Override
        public Void visitFunctionExpression(FunctionExpression node) {
          ClassData data = getDataFor(FunctionExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitFunctionExpression(node);
        }

        @Override
        public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
          ClassData data = getDataFor(FunctionExpressionInvocation.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitFunctionExpressionInvocation(node);
        }

        @Override
        public Void visitImportDirective(ImportDirective node) {
          ClassData data = getDataFor(ImportDirective.class);
          data.incrementCountIfNotEmpty("combinators", node.getCombinators());
          data.incrementCountIfNotNull("deferred", node.getDeferredToken());
          data.incrementCountIfNotNull("prefix", node.getPrefix());
          return super.visitImportDirective(node);
        }

        @Override
        public Void visitIndexExpression(IndexExpression node) {
          ClassData data = getDataFor(IndexExpression.class);
          data.incrementCountIfNotNull("auxiliaryElements", node.getAuxiliaryElements());
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitIndexExpression(node);
        }

        @Override
        public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
          ClassData data = getDataFor(InstanceCreationExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitInstanceCreationExpression(node);
        }

        @Override
        public Void visitIsExpression(IsExpression node) {
          ClassData data = getDataFor(IsExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitIsExpression(node);
        }

        @Override
        public Void visitLibraryIdentifier(LibraryIdentifier node) {
          ClassData data = getDataFor(LibraryIdentifier.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitLibraryIdentifier(node);
        }

        @Override
        public Void visitListLiteral(ListLiteral node) {
          ClassData data = getDataFor(ListLiteral.class);
          data.incrementCountIfNotEmpty("elements", node.getElements());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitListLiteral(node);
        }

        @Override
        public Void visitLiteral(Literal node) {
          ClassData data = getDataFor(Literal.class);
          data.incrementCount();
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitLiteral(node);
        }

        @Override
        public Void visitMapLiteral(MapLiteral node) {
          ClassData data = getDataFor(MapLiteral.class);
          data.incrementCountIfNotEmpty("entries", node.getEntries());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitMapLiteral(node);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocation node) {
          ClassData data = getDataFor(MethodInvocation.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitMethodInvocation(node);
        }

        @Override
        public Void visitNamedExpression(NamedExpression node) {
          ClassData data = getDataFor(NamedExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitNamedExpression(node);
        }

        @Override
        public Void visitNamespaceDirective(NamespaceDirective node) {
          ClassData data = getDataFor(NamespaceDirective.class);
          data.incrementCount();
          data.incrementCountIfNotEmpty("combinators", node.getCombinators());
          return super.visitNamespaceDirective(node);
        }

        @Override
        public Void visitNode(AstNode node) {
          addObject(node);
          return super.visitNode(node);
        }

        @Override
        public Void visitNormalFormalParameter(NormalFormalParameter node) {
          ClassData data = getDataFor(NormalFormalParameter.class);
          data.incrementCount();
          data.incrementCountIfNotNull("", node.getDocumentationComment());
          data.incrementCountIfNotEmpty("", node.getMetadata());
          return super.visitNormalFormalParameter(node);
        }

        @Override
        public Void visitParenthesizedExpression(ParenthesizedExpression node) {
          ClassData data = getDataFor(ParenthesizedExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitParenthesizedExpression(node);
        }

        @Override
        public Void visitPostfixExpression(PostfixExpression node) {
          ClassData data = getDataFor(PostfixExpression.class);
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitPostfixExpression(node);
        }

        @Override
        public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
          ClassData data = getDataFor(PrefixedIdentifier.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitPrefixedIdentifier(node);
        }

        @Override
        public Void visitPrefixExpression(PrefixExpression node) {
          ClassData data = getDataFor(PrefixExpression.class);
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitPrefixExpression(node);
        }

        @Override
        public Void visitPropertyAccess(PropertyAccess node) {
          ClassData data = getDataFor(PropertyAccess.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitPropertyAccess(node);
        }

        @Override
        public Void visitRethrowExpression(RethrowExpression node) {
          ClassData data = getDataFor(RethrowExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitRethrowExpression(node);
        }

        @Override
        public Void visitReturnStatement(ReturnStatement node) {
          ClassData data = getDataFor(ReturnStatement.class);
          data.incrementCountIfNotNull("expression", node.getExpression());
          return super.visitReturnStatement(node);
        }

        @Override
        public Void visitSimpleIdentifier(SimpleIdentifier node) {
          ClassData data = getDataFor(SimpleIdentifier.class);
          data.incrementCountIfNotNull("auxiliaryElements", node.getAuxiliaryElements());
          data.incrementCountIfNotNull("propagatedElement", node.getPropagatedElement());
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticElement", node.getStaticElement());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitSimpleIdentifier(node);
        }

        @Override
        public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
          ClassData data = getDataFor(SimpleStringLiteral.class);
          data.incrementCountIfNotNull("toolkitElement", node.getToolkitElement());
          return super.visitSimpleStringLiteral(node);
        }

        @Override
        public Void visitSuperExpression(SuperExpression node) {
          ClassData data = getDataFor(SuperExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitSuperExpression(node);
        }

        @Override
        public Void visitSwitchMember(SwitchMember node) {
          ClassData data = getDataFor(SwitchMember.class);
          data.incrementCount();
          data.incrementCountIfNotEmpty("labels", node.getLabels());
          data.incrementCountIfNotEmpty("statements", node.getStatements());
          return super.visitSwitchMember(node);
        }

        @Override
        public Void visitThisExpression(ThisExpression node) {
          ClassData data = getDataFor(ThisExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitThisExpression(node);
        }

        @Override
        public Void visitThrowExpression(ThrowExpression node) {
          ClassData data = getDataFor(ThrowExpression.class);
          data.incrementCountIfNotNull("propagatedType", node.getPropagatedType());
          data.incrementCountIfNotNull("staticType", node.getStaticType());
          return super.visitThrowExpression(node);
        }

        @Override
        public Void visitTryStatement(TryStatement node) {
          ClassData data = getDataFor(TryStatement.class);
          data.incrementCountIfNotEmpty("catchClauses", node.getCatchClauses());
          return super.visitTryStatement(node);
        }

        @Override
        public Void visitTypeParameter(TypeParameter node) {
          ClassData data = getDataFor(TypeParameter.class);
          data.incrementCountIfNotNull("bound", node.getBound());
          return super.visitTypeParameter(node);
        }
      });
    }
  }

  private void addCacheEntry(SourceEntry entry) {
    // TODO(brianwilkerson) This method is out-of-date.
    addObject(entry);
    addObject(entry.getException());
    addObject(entry.getValue(SourceEntry.LINE_INFO));
    if (entry instanceof DartEntryImpl) {
      DartEntryImpl dartEntry = (DartEntryImpl) entry;
      addAllObjects(dartEntry.getValue(DartEntry.ANGULAR_ERRORS));
      addElements(dartEntry.getValue(DartEntry.ELEMENT));
      addAllObjects(dartEntry.getValue(DartEntry.PARSE_ERRORS));
      addAst(dartEntry.getValue(DartEntry.PARSED_UNIT));
      addObject(dartEntry.getValue(DartEntry.PUBLIC_NAMESPACE));
      addAllObjects(dartEntry.getValue(DartEntry.SCAN_ERRORS));
      addTokens(dartEntry.getValue(DartEntry.TOKEN_STREAM));

      Source[] containingLibraries = dartEntry.getValue(DartEntry.CONTAINING_LIBRARIES);
      for (Source librarySource : containingLibraries) {
        addAllObjects(dartEntry.getValueInLibrary(DartEntry.RESOLUTION_ERRORS, librarySource));
        addAst(dartEntry.getValueInLibrary(DartEntry.RESOLVED_UNIT, librarySource));
        addAllObjects(dartEntry.getValueInLibrary(DartEntry.VERIFICATION_ERRORS, librarySource));
        addAllObjects(dartEntry.getValueInLibrary(DartEntry.HINTS, librarySource));
      }
    } else if (entry instanceof HtmlEntryImpl) {
      HtmlEntryImpl htmlEntry = (HtmlEntryImpl) entry;
      addObject(htmlEntry.getValue(HtmlEntry.ANGULAR_APPLICATION));
      addObject(htmlEntry.getValue(HtmlEntry.ANGULAR_COMPONENT));
      addObject(htmlEntry.getValue(HtmlEntry.ANGULAR_ENTRY));
      addAllObjects(htmlEntry.getValue(HtmlEntry.ANGULAR_ERRORS));
      addObject(htmlEntry.getValue(HtmlEntry.ELEMENT));
      addAllObjects(htmlEntry.getValue(HtmlEntry.PARSE_ERRORS));
      addObject(htmlEntry.getValue(HtmlEntry.PARSED_UNIT));
      addObject(htmlEntry.getValue(HtmlEntry.RESOLVED_UNIT));
      addAllObjects(htmlEntry.getValue(HtmlEntry.RESOLUTION_ERRORS));
      addAllObjects(htmlEntry.getValue(HtmlEntry.HINTS));
      addAllObjects(htmlEntry.getValue(HtmlEntry.POLYMER_BUILD_ERRORS));
      addAllObjects(htmlEntry.getValue(HtmlEntry.POLYMER_RESOLUTION_ERRORS));
    }
  }

  private void addElements(LibraryElement element) {
    if (element != null) {
      element.accept(new GeneralizingElementVisitor<Void>() {
        @Override
        public Void visitElement(Element element) {
          addObject(element);
          return super.visitElement(element);
        }
      });
    }
  }

  private void addObject(Object object) {
    if (object != null) {
      MemoryUsageData.ClassData data = getDataFor(object.getClass());
      data.incrementCount();
    }
  }

  private void addTokens(Token token) {
    if (token != null) {
      while (token.getNext() != token) {
        addObject(token);
        token = token.getNext();
      }
    }
  }

  private AnalysisCache getAnalysisCache(InternalAnalysisContext context) {
    try {
      Field field = context.getClass().getDeclaredField("cache");
      field.setAccessible(true);
      return (AnalysisCache) field.get(context);
    } catch (Exception exception) {
      return null;
    }
  }

  private String[][] getClassSpecificReportData(ClassData classData) {
    HashMap<String, Integer> counts = classData.getCounts();
    int rowCount = counts.size();
    String[] countNames = counts.keySet().toArray(new String[rowCount]);
    Arrays.sort(countNames);
    String[][] data = new String[rowCount][];
    int objectCount = classData.getObjectCount();
    for (int i = 0; i < rowCount; i++) {
      String countName = countNames[i];
      int count = classData.getCount(countName);
      int pecentage = (int) Math.round((count * 100.0) / objectCount);
      data[i] = new String[] {countName, Integer.toString(count), Integer.toString(pecentage) + "%"};
    }
    return data;
  }

  private MemoryUsageData.ClassData getDataFor(Class<?> someClass) {
    MemoryUsageData.ClassData data = classData.get(someClass);
    if (data == null) {
      data = new ClassData(someClass);
      classData.put(someClass, data);
    }
    return data;
  }

  private CachePartition[] getPartitions(AnalysisCache cache) {
    try {
      Field field = cache.getClass().getDeclaredField("partitions");
      field.setAccessible(true);
      return (CachePartition[]) field.get(cache);
    } catch (Exception exception) {
      return null;
    }
  }

  private void printClassSpecificReportData(PrintStringWriter writer, ClassData data) {
    if (data.getCounts() != null) {
      writer.println();
      writer.print(data.getClassObject().getName());
      writer.print(" (");
      writer.print(data.getObjectCount());
      writer.println(")");
      writer.printTable(getClassSpecificReportData(data), CLASS_SPECIFIC_ALIGNMENTS);
    }
  }
}
