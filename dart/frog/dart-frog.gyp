# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'targets': [
    {
      'target_name': 'frog',
      'conditions': [
        ['OS!="win"', {
          'dependencies': [
            '../runtime/dart-runtime.gyp:dart',
            '../third_party/v8/src/d8.gyp:d8',
          ],
        }],
        ['OS=="win"', {
          'dependencies': [
            # TODO(efortuna): Currently the Windows build only runs using the
            # dart VM, so we don't depend on d8 because of v8 build issues. Fix
            # this so that Windows can also run with d8.
            '../runtime/dart-runtime.gyp:dart',
          ],
       }]],
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_frog',
          'inputs': [
            '<!@(["python", "scripts/list_frog_files.py"])',
            'scripts/bootstrap/frog_bootstrap_wrapper.py',
            'scripts/bootstrap/frog_wrapper.py',
            'frog.py',
            '<(PRODUCT_DIR)/dart',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/frog/bin/frog',
          ],
          'action': [
            'python',
            'scripts/bootstrap/frog_bootstrap_wrapper.py',
            '<(PRODUCT_DIR)',
          ],
          'message': 'Generating frog file'
        },
      ],
    },
  ],
}
