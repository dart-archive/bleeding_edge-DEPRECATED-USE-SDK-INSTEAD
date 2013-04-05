# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'variables': {
    'analyzer_name': 'dartanalyzer',
     # When changing the jar files that we depend on please change the list
     # below.
     'dependent_jar_files': [
       '../third_party/commons-lang/3.1/commons-lang3-3.1.jar',
       '../third_party/json/r2_20080312/json.jar',
       '../third_party/guava/r13/guava-13.0.1.jar',
       '../third_party/args4j/2.0.12/args4j-2.0.12.jar'
     ],
  },
  'targets': [
    {
      'target_name': 'analyzer',
      'type': 'none',
      'variables': {
        'java_source_files': [
          '<!@(["python", "../tools/list_files.py", "\\.java$", "tools/plugins/com.google.dart.command.analyze"])',
          '<!@(["python", "../tools/list_files.py", "\\.java$", "tools/plugins/com.google.dart.engine"])',
        ],
        # The file where we write the class path to be used in the manifest.
        'class_path_file': '<(PRODUCT_DIR)/<(analyzer_name)/classpath_file',
      },
      'actions': [
        {
          'action_name': 'create_analyzer',
          'inputs': [
            '<@(java_source_files)',
            '<@(dependent_jar_files)',
            'tools/compile_analyzer.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/<(analyzer_name)/<(analyzer_name).jar',
          ],
          'action': [
            'python',
            'tools/compile_analyzer.py',
            '--output_dir', '<(PRODUCT_DIR)/<(analyzer_name)/',
            '--jar_file_name', '<(analyzer_name).jar',
            '--jar_entry_directory', 'com',
            '--dependent_jar_files', '"<@(dependent_jar_files)"',
            '--entry_point', 'com.google.dart.command.analyze.AnalyzerMain',
            '--class_path_file', 'classpath_file',
            '<@(java_source_files)',
          ],
          'message': 'Creating <(_outputs).',
        },
      ],
    },
  ]
}
