# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'targets': [
    {
      'target_name': 'create_sdk_internal',
      'type': 'none',
      'dependencies': [
        'runtime/dart-runtime.gyp:dart',
        'utils/compiler/compiler.gyp:dart2js',
        'utils/pub/pub.gyp:pub',
        'utils/dartfmt/dartfmt.gyp:dartfmt',
        'editor/analyzer.gyp:analyzer',
      ],
      'actions': [
        {
          'action_name': 'create_sdk_py',
          'inputs': [
            '<!@(["python", "tools/list_files.py", "\\.dart$", "sdk/lib"])',
            '<!@(["python", "tools/list_files.py", "", '
                '"sdk/lib/_internal/lib/preambles"])',
            '<!@(["python", "tools/list_files.py", "", "sdk/bin"])',
            'tools/create_sdk.py',
            '<(PRODUCT_DIR)/<(EXECUTABLE_PREFIX)dart<(EXECUTABLE_SUFFIX)',
            '<(SHARED_INTERMEDIATE_DIR)/dart2js.dart.snapshot',
            '<(SHARED_INTERMEDIATE_DIR)/utils_wrapper.dart.snapshot',
            '<(SHARED_INTERMEDIATE_DIR)/pub.dart.snapshot',
            '<(PRODUCT_DIR)/dartanalyzer/dartanalyzer.jar',
            '<(SHARED_INTERMEDIATE_DIR)/dartfmt.dart.snapshot',
            'tools/VERSION'
          ],
          'outputs': [
            '<(PRODUCT_DIR)/dart-sdk/README',
          ],
          'action': [
            'python',
            'tools/create_sdk.py',
            '--sdk_output_dir', '<(PRODUCT_DIR)/dart-sdk',
            '--snapshot_location', '<(SHARED_INTERMEDIATE_DIR)/'
          ],
          'message': 'Creating SDK.',
        },
      ],
    },
  ],
}
