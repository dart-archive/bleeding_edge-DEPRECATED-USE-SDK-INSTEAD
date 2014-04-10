// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage_reader;

import 'dart:collection';
import 'package:petitparser/xml.dart';

import 'numeric_list_parser.dart' as numlist;
import 'coverage_data.dart';

/// Parse the [xml] into a coverage map.
Map<String, CoverageData> processXml(String xml) {
  var parser = new XmlParser();
  var data = parser.parse(xml);
  if (data.isSuccess) {
    XmlNode coverageData = data.value.lastChild;
    return _buildMap(coverageData.children);
  } else {
    throw new Exception("Could not process XML");
  }
}

/// Destructively merge the [src] coverage map into the [dest] coverage map.
void merge(Map<String, CoverageData> dest, Map<String, CoverageData> src) {
  for (var srcData in src.values) {
    String className = srcData.className;
    var destData = dest[className];
    if (destData == null) {
      destData = new CoverageData(className, [], []);
      dest[className] = destData;
    }
    destData.merge(srcData);
  }
}

Map<String, CoverageData> _buildMap(List<XmlNode> nodes) {
  var coverageMap = new HashMap();
  for (var node in nodes) {
    if (node is XmlElement) {
      var className = node.attributes[0].value.replaceAll('/', '.');
      var children = node.children;
      // children: [XmlText, XmlElement, XmlText, XmlElement, XmlText]
      var lines = _extractLineInfoFromXml(children[1]);
      var visited = _extractLineInfoFromXml(children[3]);
      var coverageData = new CoverageData(className, lines, visited);
      coverageMap[className] = coverageData;
    }
  }
  return coverageMap;
}

List<num> _extractLineInfoFromXml(XmlElement node) {
  var linesString = node.attributes[0].value;
  return numlist.parseNumericList(linesString);
}
