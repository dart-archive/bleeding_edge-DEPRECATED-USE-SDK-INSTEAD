# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'variables': {
    # These variables are used in the creation of the .vcproj file on 
    # Windows.
    'cygwin_dir': '../third_party/cygwin',
    'msvs_cygwin_dirs': ['<(cygwin_dir)'],
  },
  'targets': [
    {
      'target_name': 'frog',
      'dependencies': [
        '../runtime/dart-runtime.gyp:dart',
        # TODO(efortuna): Currently the Windows build only runs using the 
        # dart VM, so we don't depend on d8 because of v8 build issues. Fix
        # this so that Windows can also run with node.js and d8. 
        '<!@(["python", "scripts/list_frog_dependencies.py"])'
      ],
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_frog',
          'inputs': [
            '<!@(["python", "scripts/list_frog_files.py"])',
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
            '<(PRODUCT_DIR)',
          ],
          'message': 'Generating frog file'
        },
      ],
    },
    {
      'target_name': 'frogsh',
      'dependencies': [
        '../runtime/dart-runtime.gyp:dart',
        '<!@(["python", "scripts/list_frog_dependencies.py"])',
      ],
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_frogsh',
          'inputs': [
            '<!@(["python", "scripts/list_frog_files.py"])',
            'scripts/bootstrap/frogsh_bootstrap_wrapper.py',
            'frog.py',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/frog/bin/frogsh',
          ],
          'action': [
            'python',
            'scripts/bootstrap/frogsh_bootstrap_wrapper.py',
            '<(PRODUCT_DIR)',
          ],
          'message': 'Generating frogsh file'
        },
      ],
    },
  ],
}
