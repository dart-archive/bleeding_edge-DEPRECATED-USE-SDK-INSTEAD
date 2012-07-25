# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'targets': [
    {
      # Convert the guide from XML to HTML. This makes it easier to
      # view it in Chrome from a local directory.
      'target_name': 'guide',
      'type': 'none',
      'actions': [
        {
          'action_name': 'xsltproc',
          'inputs': [
            'documentation/guide/dartguide.xml',
            'documentation/guide/styleguide.css',
            'documentation/guide/styleguide.xsl',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/guide/index.html'
          ],
          'action': [
            'xsltproc',
            '--output', '<@(_outputs)',
            'documentation/guide/dartguide.xml',
          ],
        },
        {
          'action_name': 'styleguide.css',
          'inputs': ['documentation/guide/styleguide.css'],
          'outputs': ['<(PRODUCT_DIR)/guide/styleguide.css'],
          'action': ['cp', '<@(_inputs)', '<@(_outputs)'],
        },
        {
          'action_name': 'libraries-and-namespaces.png',
          'inputs': ['documentation/guide/libraries-and-namespaces.png'],
          'outputs': ['<(PRODUCT_DIR)/guide/libraries-and-namespaces.png'],
          'action': ['cp', '<@(_inputs)', '<@(_outputs)'],
        },
      ],
    },
    {
      'target_name': 'dummy',
      'type': 'none',
    },
  ],
}
