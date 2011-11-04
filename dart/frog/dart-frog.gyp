# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'targets': [
    {
      'target_name': 'frog',
      'dependencies': [
        '../runtime/dart-runtime.gyp:dart_bin',
        '../third_party/v8/src/d8.gyp:d8',
      ],
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_frog',
          'inputs': [
            '<!@(["python", "scripts/list_dart_files.py"])',
            'scripts/bootstrap/frog_bootstrap_wrapper.py',
            'scripts/bootstrap/frog_wrapper.py',
            'frog.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/frog/bin/frog',
          ],
          'action': [
            'python',
            'scripts/bootstrap/frog_bootstrap_wrapper.py',
            '<(PRODUCT_DIR)/frog/bin/frog',
          ],
          'message': 'Generating frog file'
        },
      ],
    },
    {
      'target_name': 'frogsh',
      'dependencies': [
        '../runtime/dart-runtime.gyp:dart_bin',
        '../third_party/v8/src/d8.gyp:d8',
      ],
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_frogsh',
          'inputs': [
            '<!@(["python", "scripts/list_dart_files.py"])',
            'scripts/bootstrap/frogsh_bootstrap_wrapper.py',
            'frog.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/frog/bin/frogsh',
          ],
          'action': [
            'python',
            'scripts/bootstrap/frogsh_bootstrap_wrapper.py',
            '--out=<(PRODUCT_DIR)/frog/bin/frogsh', 'frog.dart',
          ],
          'message': 'Generating frogsh file'
        },
      ],
    },
  ],
}
