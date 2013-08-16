// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A JS Interop sample accessing the Google Charts API.  The sample is based on
// the Bubble Chart example here:
// https://developers.google.com/chart/interactive/docs/gallery/bubblechart

import 'dart:html';
import 'package:js/js.dart' as js;

void drawVisualization() {
  var gviz = js.context.google.visualization;

  // Create and populate the data table.
  var listData = [
    ['ID', 'Life Expectancy', 'Fertility Rate', 'Region',     'Population'],
    ['CAN',    80.66,              1.67,      'North America',  33739900],
    ['DEU',    79.84,              1.36,      'Europe',         81902307],
    ['DNK',    78.6,               1.84,      'Europe',         5523095],
    ['EGY',    72.73,              2.78,      'Middle East',    79716203],
    ['GBR',    80.05,              2,         'Europe',         61801570],
    ['IRN',    72.49,              1.7,       'Middle East',    73137148],
    ['IRQ',    68.09,              4.77,      'Middle East',    31090763],
    ['ISR',    81.55,              2.96,      'Middle East',    7485600],
    ['RUS',    68.6,               1.54,      'Europe',         141850000],
    ['USA',    78.09,              2.05,      'North America',  307007000]
  ];

  var arrayData = js.array(listData);

  var tableData = gviz.arrayToDataTable(arrayData);

  var options = js.map({
    'title': 'Correlation between life expectancy, fertility rate and population of some world countries (2010)',
    'hAxis': {'title': 'Life Expectancy'},
    'vAxis': {'title': 'Fertility Rate'},
    'bubble': {'textStyle': {'fontSize': 11}}
  });

  // Create and draw the visualization.
  var chart = new js.Proxy(gviz.BubbleChart,
      query('#visualization'));
  chart.draw(tableData, options);
}

main() {
  js.context.google.load('visualization', '1', js.map(
    {
      'packages': ['corechart'],
      'callback': new js.Callback.once(drawVisualization)
    }));
}
