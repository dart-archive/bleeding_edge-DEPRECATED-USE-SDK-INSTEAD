package com.google.dart.dev.util.analysis;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class MemoryUsageData {
  private static class ClassData {
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

    public void conditionallyIncrementCount(String countName, Object value) {
      if (value != null) {
        incrementCount(countName, 1);
      }
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
      MemoryUsageData.ClassData entry = entries[i];
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
    printClassSpecificReportData(writer, SimpleIdentifier.class, new String[] {
        "auxiliaryElements", "propagatedElement", "propagatedType", "staticElement", "staticType"});
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
      unit.accept(new UnifyingAstVisitor<Void>() {
        @Override
        public Void visitNode(AstNode node) {
          addObject(node);
          return super.visitNode(node);
        }

        @Override
        public Void visitSimpleIdentifier(SimpleIdentifier node) {
          ClassData data = getDataFor(SimpleIdentifier.class);
          data.conditionallyIncrementCount("auxiliaryElements", node.getAuxiliaryElements());
          data.conditionallyIncrementCount("propagatedElement", node.getPropagatedElement());
          data.conditionallyIncrementCount("propagatedType", node.getPropagatedType());
          data.conditionallyIncrementCount("staticElement", node.getStaticElement());
          data.conditionallyIncrementCount("staticType", node.getStaticType());
          return super.visitSimpleIdentifier(node);
        }
      });
    }
  }

  private void addCacheEntry(SourceEntry entry) {
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
        addAllObjects(dartEntry.getValueInLibrary(DartEntry.BUILD_ELEMENT_ERRORS, librarySource));
        addAst(dartEntry.getValueInLibrary(DartEntry.BUILT_UNIT, librarySource));
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

  private String[][] getClassSpecificReportData(Class<?> targetClass, String[] countNames) {
    int rowCount = countNames.length;
    String[][] data = new String[rowCount][];
    ClassData classData = getDataFor(targetClass);
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

  private void printClassSpecificReportData(PrintStringWriter writer, Class<?> targetClass,
      String[] countNames) {
    writer.println();
    writer.println(targetClass.getName());
    writer.printTable(getClassSpecificReportData(targetClass, countNames),
        CLASS_SPECIFIC_ALIGNMENTS);
  }
}
