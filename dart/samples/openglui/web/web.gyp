# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Note: You need wget and ImageMagick installed and on your path
# for this to work. You can avoid the latter by converting bg3_1.jpg to 
# a .png file with some other tool.

{
  'targets': [
    {
      'target_name': 'assets',
      'type': 'none',
      'actions': [
        {
          'action_name': 'fetch_sounds',
          'inputs': [
          ],
          'outputs': [
            'bigboom.mp3',
            'boom.mp3',
            'enemybomb.mp3',
            'explosion1.mp3',
            'explosion2.mp3',
            'explosion3.mp3',
            'explosion4.mp3',
            'laser.mp3',
            'powerup.mp3',
          ],
          'action': [
            'wget',
            '-nc',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/bigboom.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/boom.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/enemybomb.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/explosion1.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/explosion2.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/explosion3.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/explosion4.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/laser.mp3',
            'http://www.kevs3d.co.uk/dev/asteroids/sounds/powerup.mp3',
          ]
        },
        {
          'action_name': 'fetch_sprites',
          'inputs': [
          ],
          'outputs': [
            'asteroid1.png',
            'asteroid2.png',
            'asteroid3.png',
            'asteroid4.png',
            'bg3_1.jpg',
            'enemyship1.png',
            'player.png',
            'shield.png',
          ],
          'action': [
            'wget',
            '-nc',
            'http://www.kevs3d.co.uk/dev/asteroids/images/asteroid1.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/asteroid2.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/asteroid3.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/asteroid4.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/bg3_1.jpg',
            'http://www.kevs3d.co.uk/dev/asteroids/images/enemyship1.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/player.png',
            'http://www.kevs3d.co.uk/dev/asteroids/images/shield.png',
          ]
        },
        {
          'action_name': 'convert_background',
          'inputs': [
            'bg3_1.jpg',
          ],
          'outputs': [
            'bg3_1.png',
          ],
          'action': [
            'convert',
            'bg3_1.jpg',
            'bg3_1.png'
          ]
        },
      ]
    },
  ]
}

