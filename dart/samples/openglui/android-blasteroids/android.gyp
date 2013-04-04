# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Note: to use this you need SoX with mp3 support installed:
#
# sudo apt-get install sox
# sudo apt-get install libsox-fmt-mp3
#
# Alternatively, manually use Audacity to export the ../web/*.mp3 files to raw
# PCM files with a .raw file extension and copy these to the assets 
# directory.

{
  'conditions': [
    ['OS=="android"',
      {
        'targets': [
          {
            'target_name': 'android_app',
            'type': 'none',
            'dependencies': [
              'copy_extension',
              'copy_main',
              '../../../runtime/dart-runtime.gyp:android_embedder',
              'copy_embedder',
              '../web/web.gyp:assets',
              'copy_images',
              'convert_sounds',
            ],
            'actions': [
              # TODO(gram) - this should have a debug and release version.
              {
                'action_name': 'build_app',
                'inputs': [
                  '<(PRODUCT_DIR)/lib.target/libandroid_embedder.so',
                  '../../../runtime/embedders/openglui/common/gl.dart',
                  '../src/blasteroids.dart',
                ],
                'outputs': [
                  'bin/NativeActivity-debug.apk',
                ],
                'action': [ 'ant', 'debug' ]
              }
            ]
          },
          {
            'target_name': 'copy_embedder',
            'type': 'none',
            'copies': [ {
              # TODO(gram) - this should vary based on architecture.
              'destination': 'libs/x86',
              'files': [
                '<(PRODUCT_DIR)/lib.target/libandroid_embedder.so'
              ],
            }],
          },
          {
            'target_name': 'copy_extension',
            'type': 'none',
            'copies': [ {
              'destination': 'assets/dart',
              'files': [
                '../../../runtime/embedders/openglui/common/gl.dart'
              ],
            }],
          },
          {
            'target_name': 'copy_main',
            'type': 'none',
            'copies': [ {
              'destination': 'assets/dart',
              'files': [
                '../src/blasteroids.dart',
              ],
            }],
          },
          {
            'target_name': 'copy_images',
            'type': 'none',
            'copies': [ {
              'destination': 'assets',
              'files': [
                '../web/asteroid1.png',
                '../web/asteroid2.png',
                '../web/asteroid3.png',
                '../web/asteroid4.png',
                '../web/bg3_1.png',
                '../web/enemyship1.png',
                '../web/player.png',
                '../web/shield.png',
              ],
            }],
          },
          {
            'target_name': 'convert_sounds',
            'type': 'none',
            'actions': [
              {
                'action_name': 'convert_bigboom',
                'inputs': [ '../web/bigboom.mp3' ],
                'outputs': [ 'assets/bigboom.raw' ],
                'action': [ 'sox', '../web/bigboom.mp3', 'assets/bigboom.raw' ]
              },
              {
                'action_name': 'convert_boom',
                'inputs': [ '../web/boom.mp3' ],
                'outputs': [ 'assets/boom.raw' ],
                'action': [ 'sox', '../web/boom.mp3', 'assets/boom.raw' ]
              },
              {
                'action_name': 'convert_enemybomb',
                'inputs': [ '../web/enemybomb.mp3' ],
                'outputs': [ 'assets/enemybomb.raw' ],
                'action': [ 'sox', '../web/enemybomb.mp3',
                    'assets/enemybomb.raw' ]
              },
              {
                'action_name': 'convert_explosion1',
                'inputs': [ '../web/explosion1.mp3' ],
                'outputs': [ 'assets/explosion1.raw' ],
                'action': [ 'sox', '../web/explosion1.mp3',
                    'assets/explosion1.raw' ]
              },
              {
                'action_name': 'convert_explosion2',
                'inputs': [ '../web/explosion2.mp3' ],
                'outputs': [ 'assets/explosion2.raw' ],
                'action': [ 'sox', '../web/explosion2.mp3',
                    'assets/explosion2.raw' ]
              },
              {
                'action_name': 'convert_explosion3',
                'inputs': [ '../web/explosion3.mp3' ],
                'outputs': [ 'assets/explosion3.raw' ],
                'action': [ 'sox', '../web/explosion3.mp3',
                    'assets/explosion3.raw' ]
              },
              {
                'action_name': 'convert_explosion4',
                'inputs': [ '../web/explosion4.mp3' ],
                'outputs': [ 'assets/explosion4.raw' ],
                'action': [ 'sox', '../web/explosion4.mp3',
                    'assets/explosion4.raw' ]
              },
              {
                'action_name': 'convert_laser',
                'inputs': [ '../web/laser.mp3' ],
                'outputs': [ 'assets/laser.raw' ],
                'action': [ 'sox', '../web/laser.mp3', 'assets/laser.raw' ]
              },
              {
                'action_name': 'convert_powerup',
                'inputs': [ '../web/powerup.mp3' ],
                'outputs': [ 'assets/powerup.raw' ],
                'action': [ 'sox', '../web/powerup.mp3', 'assets/powerup.raw' ]
              },
            ],
          }
        ]
      }
    ]
  ]
}


