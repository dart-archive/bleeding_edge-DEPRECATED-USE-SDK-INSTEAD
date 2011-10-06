// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Dart port of the playable Pac-Man doodle for the homepage for May 21-22, 2010.
 * @author mwichary@google.com (Marcin Wichary)
 * @author khom@google.com (Kristopher Hom)
 * @author jgw@google.com (Joel Webber)
 *
 * Game logic largely based on
 * http://home.comcast.net/~jpittman2/pacman/pacmandossier.html
 */

class DOM {
  static void remove(Element el) {
    if (el) {
      el.parentNode.removeChild(el);
    }
  }

  static void append(Element el) {
    document.body.appendChild(el);
  }
}

class Routine {
  double x, y, dest, speed;
  int dir; // TODO(jgw): enum

  Routine(double x, double y, int dir, double dest, double speed):
    x(x), y(y), dir(dir), dest(dest), speed(speed) { }
}

class Movement {
  int axis, increment;

  // TODO(jgw): const
  Movement(int axis, int increment):
    axis(axis), increment(increment) { }
}

class Point {
  int x, y;

  // TODO(jgw): const
  Point(int x, int y): x(x), y(y) { }
}

class Position {
  int x, y, scatterX, scatterY;
  int dir; // TODO(jgw): enum

  // TODO(jgw): const
  Position(int x, int y, int dir, int scatterX = 0, int scatterY = 0):
    x(x), y(y), dir(dir), scatterX(scatterX), scatterY(scatterY) { }
}

class Path {
  int x, y, w, h;
  int type; // TODO(jgw): enum

  // TODO(jgw): const
  Path(int x, int y, int w, int h, int type = 0):
    x(x), y(y), w(w), h(h), type(type) { }
}

class Tile {
  int path, intersection;
  int dot;
  int type;
  int allowedDir;

  Tile(int path, int dot, int intersection)
    : path(path), dot(dot), intersection(intersection) { }
}

class Cutscene {
  Array<CutsceneActor> actors;
  Array<CutsceneSequence> sequence;

  Cutscene(Array<CutsceneActor> actors, Array<CutsceneSequence> sequence):
    actors(actors), sequence(sequence) { }
}

class CutsceneActor {
  boolean ghost;
  double x, y;
  int id;

  CutsceneActor(boolean ghost, double x, double y, int id):
    ghost(ghost), x(x), y(y), id(id) { }
}

class CutsceneSequence {
  double time;
  Array<CutsceneMove> moves;

  CutsceneSequence(double time, Array<CutsceneMove> moves):
    time(time), moves(moves) { }
}

class CutsceneMove {
  int dir;
  double speed;
  int mode;
  String elId;

  CutsceneMove(int dir, double speed, int mode=null, String elId=null):
    dir(dir), speed(speed), mode(mode), elId(elId) { }
}

class PM {
  static final CSS =
    '#pcm-c {'+
    '  width: 554px;'+
    '  border-top: 25px solid black;'+
    '  padding-bottom: 25px;'+
    '  height: 136px;'+
    '  position: relative;'+
    '  background: black;'+
    '  outline: 0;'+
    '  overflow: hidden;'+
    '  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);'+
    '}'+
    '#pcm-c * {'+
    '  position: absolute;'+
    '  overflow: hidden;'+
    '}'+
    '#pcm-p,'+
    '#pcm-cc {'+
    '  left: 45px;'+
    '  width: 464px;'+
    '  height: 136px;'+
    '  z-index: 99;'+
    '  overflow: hidden;'+
    '}'+
    '#pcm-p .pcm-d {'+
    '  width: 2px;'+
    '  height: 2px;'+
    '  margin-left: 3px;'+
    '  margin-top: 3px;'+
    '  background: #f8b090;'+
    '  z-index: 100;'+
    '}'+
    '#pcm-p .pcm-e {'+
    '  width: 8px;'+
    '  height: 8px;'+
    '  z-index: 101;'+
    '}'+
    '#pcm-sc-1 {'+
    '  left: 18px;'+
    '  top: 16px;'+
    '  width: 8px;'+
    '  height: 56px;'+
    '  position: absolute;'+
    '  overflow: hidden;'+
    '}'+
    '#pcm-sc-2 {'+
    '  left: 18px;'+
    '  top: 80px;'+
    '  width: 8px;'+
    '  height: 56px;'+
    '  position: absolute;'+
    '  overflow: hidden;'+
    '}'+
    '#pcm-le {'+
    '  position: absolute;'+
    '  left: 515px;'+
    '  top: 74px;'+
    '  height: 64px;'+
    '  width: 32px;'+
    '} '+
    '#pcm-le div {'+
    '  position: relative;'+
    '}'+
    '#pcm-sc-1-l {  '+
    '  left: -2px;'+
    '  top: 0;'+
    '  width: 48px;'+
    '  height: 8px;'+
    '}'+
    '#pcm-sc-2-l {  '+
    '  left: -2px;'+
    '  top: 64px;'+
    '  width: 48px;'+
    '  height: 8px;'+
    '}'+
    '#pcm-so {'+
    '  left: 7px;'+
    '  top: 116px;'+
    '  width: 12px;'+
    '  height: 12px;'+
    '  border: 8px solid black;'+
    '  cursor: pointer;'+
    '}'+
    '#pcm-li {'+
    '  position: absolute;'+
    '  left: 523px;'+
    '  top: 0;'+
    '  height: 80px;'+
    '  width: 16px;'+
    '}'+
    '#pcm-li .pcm-lif {'+
    '  position: relative;'+
    '  width: 16px;'+
    '  height: 12px;'+
    '  margin-bottom: 3px;'+
    '}'+
    '#pcm-p.blk .pcm-e {'+
    '  visibility: hidden;'+
    '}'+
    '#pcm-c .pcm-ac {'+
    '  width: 16px;'+
    '  height: 16px;'+
    '  margin-left: -4px;'+
    '  margin-top: -4px;'+
    '  z-index: 110;'+
    '}'+
    '#pcm-c .pcm-n {'+
    '  z-index: 111;'+
    '}'+
    '#pcm-c #pcm-stck {'+
    '  z-index: 109;'+
    '}'+
    '#pcm-c #pcm-gbug {'+
    '  width: 32px;'+
    '}'+
    '#pcm-c #pcm-bpcm {'+
    '  width: 32px;'+
    '  height: 32px;'+
    '  margin-left: -20px;'+
    '  margin-top: -20px;'+
    '}'+
    '#pcm-f,'+
    '#pcm-le div {'+
    '  width: 32px;'+
    '  height: 16px;'+
    '  z-index: 105;'+
    '}'+
    '#pcm-f {'+
    '  margin-left: -8px;'+
    '  margin-top: -4px;'+
    '}'+
    '#pcm-do {'+
    '  width: 19px;'+
    '  height: 2px;'+
    '  left: 279px;'+
    '  top: 46px;'+
    '  overflow: hidden;'+
    '  position: absolute;'+
    '  background: #ffaaa5;'+
    '}'+
    '#pcm-re {'+
    '  width: 48px;'+
    '  height: 8px;'+
    '  z-index: 120;'+
    '  left: 264px;'+
    '  top: 80px;'+
    '}'+
    '#pcm-go {'+
    '  width: 80px;'+
    '  height: 8px;'+
    '  z-index: 120;'+
    '  left: 248px;'+
    '  top: 80px;'+
    '}';

  /**
   * Constants for player characters.
   */
  static final PACMAN = 0;
  static final MS_PACMAN = 1;

  /**
   * Gameplay mode constants.
   */
  static final GAMEPLAY_GAME_IN_PROGRESS = 0;       // Regular game
  static final GAMEPLAY_GHOST_BEING_EATEN = 1;      // Pause after Pac-Man eats ghost
  static final GAMEPLAY_PLAYER_DYING_PART_1 = 2;    // Pause before Pac-Man dies
  static final GAMEPLAY_PLAYER_DYING_PART_2 = 3;    // Pac-Man death animation
  static final GAMEPLAY_READY_PART_1 = 4;           // READY! with ghosts hidden
  static final GAMEPLAY_READY_PART_2 = 5;           // READY! with ghosts visible
  static final GAMEPLAY_FAST_READY_PART_1 = 6;      // Before quick READY! (empty)
  static final GAMEPLAY_FAST_READY_PART_2 = 7;      // Quick READY! without melody
                                              // (level 2 and above)
  static final GAMEPLAY_GAMEOVER = 8;               // GAME OVER
  static final GAMEPLAY_LEVEL_COMPLETE_PART_1 = 9;  // Pause after level ends
  static final GAMEPLAY_LEVEL_COMPLETE_PART_2 = 10; // Playfield blinking
  static final GAMEPLAY_LEVEL_COMPLETE_PART_3 = 11; // Quick pause with no playfield
  static final GAMEPLAY_DOUBLE_MODE_SWITCH = 12;    // Pause before Ms. Pac-Man
  static final GAMEPLAY_CUTSCENE = 13;              // Cutscene
  static final GAMEPLAY_INFINITE_GAMEOVER = 14;     // GAME OVER at level 256

  /**
   * Player and non-player (ghost) character modes.
   */
  static final PLAYER_MODE_MOVING = 1;          // Normal Pac-Man move (no other mode)
  static final GHOST_MODE_CHASE = 1;            // Ghost chases Pac-Man
  static final GHOST_MODE_SCATTER = 2;          // Ghost just wanders around
  static final GHOST_MODE_FRIGHT = 4;           // (Blue) ghost runs away
  static final GHOST_MODE_EYES = 8;             // Just being eaten, returns to pen
  static final GHOST_MODE_IN_PEN = 16;          // In pen
  static final GHOST_MODE_EXITING_PEN = 32;     // Leaving pen for the first time
  static final GHOST_MODE_REENTERING_PEN = 64;  // Re-entering pen (after being eaten)
  static final GHOST_MODE_REEXITING_PEN = 128;  // Re-exiting pen (after being eaten)

  /**
   * Direction/movement constants.
   */
  static final DIR_NONE = 0;
  static final DIR_UP = 1;
  static final DIR_DOWN = 2;
  static final DIR_LEFT = 4;
  static final DIR_RIGHT = 8;

  /**
   * A list of all directions. The order here is important: the original game
   * considered directions when figuring out ghost movement in this particular
   * order.
   */
  static final DIRS = [DIR_UP, DIR_LEFT, DIR_DOWN, DIR_RIGHT];

  /**
   * Information about movement.
   */
  static final MOVEMENTS = {
    // TODO(jgw): const
    0: new Movement(0,  0), // DIR_NONE
    1: new Movement(0, -1), // DIR_UP
    2: new Movement(0,  1), // DIR_DOWN
    4: new Movement(1, -1), // DIR_LEFT
    8: new Movement(1,  1) // DIR_RIGHT
  };

  /**
   * Dot types. Energizer = big blinking dot.
   */
  static final DOT_TYPE_NONE = 0;
  static final DOT_TYPE_DOT = 1;
  static final DOT_TYPE_ENERGIZER = 2;

  /**
   * Speed constants. Pac-Man moves slower when eating dots. Ghosts move
   * slower in a tunnel.
   */
  static final SPEED_FULL = 0;
  static final SPEED_DOT_EATING = 1;
  static final SPEED_TUNNEL = 2;

  /**
   * Path type = regular path or a tunnel.
   */
  static final PATH_NORMAL = 0;
  static final PATH_TUNNEL = 1;

  /**
   * Sound constants.
   */
  static final SOUND_PACMAN_DEATH = 'death';
  static final SOUND_FRUIT = 'fruit';
  static final SOUND_EXTRA_LIFE = 'extra-life';
  static final SOUND_DOT_EATING_PART_1 = 'eating-dot-1';
  static final SOUND_DOT_EATING_PART_2 = 'eating-dot-2';
  static final SOUND_EATING_GHOST = 'eating-ghost';
  static final SOUND_AMBIENT_1 = 'ambient-1';
  static final SOUND_AMBIENT_2 = 'ambient-2';
  static final SOUND_AMBIENT_3 = 'ambient-3';
  static final SOUND_AMBIENT_4 = 'ambient-4';
  static final SOUND_AMBIENT_FRIGHT = 'ambient-fright';
  static final SOUND_AMBIENT_EYES = 'ambient-eyes';
  static final SOUND_START_MUSIC = 'start-music';
  static final SOUND_AMBIENT_CUTSCENE = 'cutscene';
  static final SOUND_START_MUSIC_DOUBLE = 'start-music-double';
  static final SOUND_DOT_EATING_DOUBLE = 'eating-dot-double';
  static final SOUND_PACMAN_DEATH_DOUBLE = 'death-double';

  /**
   * Sound channel constants.
   */
  static final CHANNEL_AUX = 0;
  static final CHANNEL_EATING = 1;
  static final CHANNEL_EATING_DOUBLE = 3;

  /**
   * Number of supported sound channels.
   */
  static final SOUND_CHANNEL_COUNT = 5;

  /**
   * Multi-channel count. We rotate between 2 channels for eating and
   * ambient sounds.
   */
  static final MULTI_CHANNEL_COUNT = 2;

  /**
   * Interval for eating dots sound.
   */
  static final DOT_EATING_SOUND_INTERVAL = 150;
  static final DOT_EATING_SOUND_CLEAR_TIME = DOT_EATING_SOUND_INTERVAL + 100;


  // GAMEPLAY CONFIGURATION CONSTANTS

  /**
   * A number of non-player characters (ghosts).
   */
  static final GHOST_ACTOR_COUNT = 4;

  /**
   * Extra life multiplier (awarded every 10000 points).
   */
  static final EXTRA_LIFE_SCORE = 10000;

  /**
   * The maximum number of Pac-Man lives.
   */
  static final MAX_LIVES = 5;

  /**
   * How many dots need to be eaten for the fruit to appear.
   */
  static final FRUIT_DOTS_TRIGGER_1 = 70;
  static final FRUIT_DOTS_TRIGGER_2 = 170;

  /**
   * How many dots does it take to be eaten for ambient sounds to change
   * (become faster)
   */
  static final SOUND_AMBIENT_2_DOTS = 138;
  static final SOUND_AMBIENT_3_DOTS = 207;
  static final SOUND_AMBIENT_4_DOTS = 241;

  /**
   * Scores for eating a dot, an energizer, and a ghost.
   */
  static final SCORE_DOT = 10;
  static final SCORE_ENERGIZER = 50;
  static final SCORE_GHOST = 200;

  /**
   * Values used to determine whether ghosts can leave the pen in the
   * alternate count mode (c.f. PacMan.dotEaten).
   */
  static final ALTERNATE_DOT_COUNT = [0, 7, 17, 32];

  /**
   * Width/height of one tile in pixels.
   */
  static final TILE_SIZE = 8;

  /**
   * The number of pixels available for "cornering" (if you change directions
   * before the turn, you can "corner" the turn gaining a little bit of
   * distance). Two variants depending on the direction.
   */
  static final CORNER_DELTA_MAX = 4 - 0.4;
  static final CORNER_DELTA_MIN = 4;

  /**
   * Correction to position the gameplay exactly on top of the doodle graphics.
   * (Currently 32 pixels to the left.)
   */
  static final PLAYFIELD_OFFSET_X = -32;
  static final PLAYFIELD_OFFSET_Y = 0;

  /**
   * Available paths for Pac-Man and ghosts to travel through. A path starts
   * with position (x and y), and is either horizontal (w = width) or vertical
   * (h = height).
   */
  static final PATHS = [
    // TODO(jgw): const
    new Path(5, 1, 56, 0),
    new Path(5, 1, 56, 0),
    new Path(5, 4, 5, 0),
    new Path(5, 1, 0, 4),
    new Path(9, 1, 0, 12),
    new Path(5, 12, 0, 4),
    new Path(10, 12, 0, 4),
    new Path(5, 15, 16, 0),
    new Path(5, 12, 31, 0),
    new Path(60, 1, 0, 4),
    new Path(54, 1, 0, 4),
    new Path(19, 1, 0, 12),
    new Path(19, 4, 26, 0),
    new Path(13, 5, 7, 0),
    new Path(13, 5, 0, 4),
    new Path(13, 8, 3, 0),
    new Path(56, 4, 0, 9),
    new Path(48, 4, 13, 0),
    new Path(48, 1, 0, 12),
    new Path(60, 12, 0, 4),
    new Path(44, 15, 17, 0),
    new Path(54, 12, 0, 4),
    new Path(44, 12, 17, 0),
    new Path(44, 1, 0, 15),
    new Path(41, 13, 4, 0),
    new Path(41, 13, 0, 3),
    new Path(38, 13, 0, 3),
    new Path(38, 15, 4, 0),
    new Path(35, 10, 10, 0),
    new Path(35, 1, 0, 15),
    new Path(35, 13, 4, 0),
    new Path(21, 12, 0, 4),
    new Path(24, 12, 0, 4),
    new Path(24, 15, 12, 0),
    new Path(27, 4, 0, 9),
    new Path(52, 9, 5, 0),
    new Path(56, 8, 10, 0, PATH_TUNNEL),
    new Path(1, 8, 9, 0, PATH_TUNNEL)
  ];

  /**
   * Paths that should not have dots in them. Essentially, paths around
   * the ghost pen, and the tunnel.
   */
  static final NO_DOT_PATHS = [
    // TODO(jgw): const
    new Path( 1,  8, 8, 0), // Tunnel
    new Path( 57, 8, 9, 0),
    new Path( 44, 2, 0, 10), // Around the pen
    new Path( 35, 5, 0, 7),
    new Path( 36, 4, 8, 0),
    new Path( 36, 10, 8, 0),
    new Path( 39, 15, 2, 0) // Where Pac-Man starts
  ];

  /**
   * Positions of energizers (big dots that turn ghosts blue and allow
   * Pac-Man to eat them).
   */
  static final ENERGIZERS = [
    // TODO(jgw): const
    new Point(5, 15),
    new Point(5, 3),
    new Point(15, 8),
    new Point(60, 3),
    new Point(60, 15)
  ];

  /**
   * Positions of two ends of horizontal tunnel linking both sides of the
   * playfield.
   */
  static final TUNNEL_ENDS = [
    // TODO(jgw): const
    new Point(2, 8),
    new Point(63, 8)
  ];

  /**
   * Initial position of all the actors. scatterX and scatterY are the
   * off-screen targets for scatter mode.
   */
  static final INITIAL_ACTOR_POSITIONS = [
    null,
    [ // Pac-Man mode (one player)
      new Position(39.5,   15, DIR_LEFT),
      new Position(39.5,   4,  DIR_LEFT, 57, -4),
      new Position(39.5,   7,  DIR_DOWN, 0,  -4),
      new Position(37.625, 7,  DIR_UP,   57, 20),
      new Position(41.375, 7,  DIR_UP,   0,  20)
    ],
    [ // Ms. Pac-Man mode (two players)
      new Position(40.25,  15, DIR_RIGHT),
      new Position(38.75,  15, DIR_LEFT),
      new Position(39.5,   4,  DIR_LEFT, 57, -4),
      new Position(39.5,   7,  DIR_DOWN, 0,  -4),
      new Position(37.625, 7,  DIR_UP,   57, 20),
      new Position(41.375, 7,  DIR_UP,   0,  20)
    ]
  ];

  /**
   * The position of the exit/entrance to the pen (Y, X)
   */
  static final EXIT_PEN_POS = [4 * TILE_SIZE, 39 * TILE_SIZE];

  /**
   * The position of the fruit (Y, X)
   */
  static final FRUIT_POS = [10 * TILE_SIZE, 39 * TILE_SIZE];

  /**
   * Constants for different timers.
   */
  static final TIMING_ENERGIZER = 0;               // Energizer blinking
  static final TIMING_FRIGHT_BLINK = 1;            // Ghosts blinking blue/white
  static final TIMING_GHOST_BEING_EATEN = 2;       // Pause while ghost is being eaten
  static final TIMING_PLAYER_DYING_PART_1 = 3;     // Pause before Pac-Man dies
  static final TIMING_PLAYER_DYING_PART_2 = 4;     // Pac-Man death animation
  static final TIMING_FAST_READY_PART_1 = 5;       // Blank screen before quick READY!
  static final TIMING_FAST_READY_PART_2 = 6;       // Quick READY! (level 2 and above)
  static final TIMING_READY_PART_1 = 7;            // READY! with ghosts hidden
  static final TIMING_READY_PART_2 = 8;            // READY! with ghosts visible
  static final TIMING_GAMEOVER = 9;                // GAME OVER!
  static final TIMING_LEVEL_COMPLETE_PART_1 = 10;  // Pause after level ends
  static final TIMING_LEVEL_COMPLETE_PART_2 = 11;  // Playfield blinking
  static final TIMING_LEVEL_COMPLETE_PART_3 = 12;  // Quick pause with no playfield
  static final TIMING_DOUBLE_MODE = 13;            // Switching to Ms. Pac-Man mode
  static final TIMING_FRUIT_DECAY = 14;            // Pause after eating the fruit
  static final TIMING_FRUIT_MIN = 15;              // Minimum fruit time
  static final TIMING_FRUIT_MAX = 16;              // Maximum fruit time
  static final TIMING_SCORE_LABEL = 17;            // Score label blinking

  /**
   * Different timers as per above constants.
   */
  static final TIMING = [
    .16,   // TIMING_ENERGIZER
    .23,   // TIMING_FRIGHT_BLINK
    1,     // TIMING_GHOST_BEING_EATEN
    1,     // TIMING_PLAYER_DYING_PART_1
    2.23,  // TIMING_PLAYER_DYING_PART_2
    .3,    // TIMING_FAST_READY_PART_1
    1.9,   // TIMING_FAST_READY_PART_2
    2.23,  // TIMING_READY_PART_1
    1.9,   // TIMING_READY_PART_2
    5,     // TIMING_GAMEOVER
    1.9,   // TIMING_LEVEL_COMPLETE_PART_1
    1.18,  // TIMING_LEVEL_COMPLETE_PART_2
    .3,    // TIMING_LEVEL_COMPLETE_PART_3
    .5,    // TIMING_DOUBLE_MODE
    1.9,   // TIMING_FRUIT_DECAY
    9,     // TIMING_FRUIT_MIN
    10,    // TIMING_FRUIT_MAX
    .26    // TIMING_SCORE_LABEL
  ];

  /**
   * Master speed value.
   */
  static final MASTER_SPEED = .8;

  /**
   * Speed whenever the ghosts turns into eyes after being eaten.
   */
  static final EYES_SPEED = MASTER_SPEED * 2;

  /**
   * Ghost speed inside a pen.
   */
  static final PEN_SPEED = MASTER_SPEED * .6;

  /**
   * Ghost speed when exiting the pen.
   */
  static final EXIT_PEN_SPEED = MASTER_SPEED * .4;

  /**
   * Information about the level. We have information for levels 1 (start)
   * through 21. Every level after 21 is the same. Level 255 is the last one,
   * after which we get a "kill screen."
   */
  static final LEVELS = [
    // 0
    {},

    // 1
    {
      ghostSpeed: .75,            // Regular ghost speed
      ghostTunnelSpeed: .4,       // Ghost speed in a tunnel

      playerSpeed: .8,            // Regular Pac-Man speed
      dotEatingSpeed: .71,        // Pac-Man speed when eating dots

      ghostFrightSpeed: .5,       // Ghost speed when frightened (blue)
      playerFrightSpeed: .9,      // Pac-Man speed when ghosts are frightened
      dotEatingFrightSpeed: .79,  // Pac-Man speed when eating dots in fright
                                  // mode

      elroyDotsLeftPart1: 20,     // How many dots have to remain before red
                                  // ghost (Blinky) turns into "Cruise Elroy"
      elroySpeedPart1: .8,        // Speed of "Cruise Elroy"
      elroyDotsLeftPart2: 10,     // How many dots have to remain before
                                  // "Cruise Elroy" gets even faster
      elroySpeedPart2: .85,       // ...this fast

      frightTime: 6,              // Fright mode lasts for 6 seconds
      frightBlinkCount: 5,        // ...after 6 seconds, we get 5 blinks

      fruit: 1,                   // Type of fruit (1 to 8)
      fruitScore: 100,            // Fruit score when eaten

      // Times in seconds of alternating ghost modes
      // (scatter, chase, scatter...)
      ghostModeSwitchTimes: [7, 20, 7, 20, 5, 20, 5, 1],

      penForceTime: 4,            // How many seconds of Pac-Man inactivity
                                  // (not eating dots) does it take for the
                                  // ghosts to start leaving the pen

      // How many dots need to be eaten before ghosts start leaving (for four
      // ghosts)
      penLeavingLimits: [0, 0, 30, 60],
    },

    // 2
    {
      ghostSpeed: .85, ghostTunnelSpeed: .45,
      playerSpeed: .9, dotEatingSpeed: .79,
      ghostFrightSpeed: .55, playerFrightSpeed: .95, dotEatingFrightSpeed: .83,
      elroyDotsLeftPart1: 30, elroySpeedPart1: .9,
      elroyDotsLeftPart2: 15, elroySpeedPart2: .95,
      frightTime: 5, frightBlinkCount: 5,
      fruit: 2, fruitScore: 300,
      ghostModeSwitchTimes: [7, 20, 7, 20, 5, 1033, 1 / 60, 1],
      penForceTime: 4, penLeavingLimits: [0, 0, 0, 50],

      cutsceneId: 1 // Which cutscene to play after the level
    },

    // 3
    {
      ghostSpeed: .85, ghostTunnelSpeed: .45,
      playerSpeed: .9, dotEatingSpeed: .79,
      ghostFrightSpeed: .55, playerFrightSpeed: .95, dotEatingFrightSpeed: .83,
      elroyDotsLeftPart1: 40, elroySpeedPart1: .9,
      elroyDotsLeftPart2: 20, elroySpeedPart2: .95,
      frightTime: 4, frightBlinkCount: 5,
      fruit: 3, fruitScore: 500,
      ghostModeSwitchTimes: [7, 20, 7, 20, 5, 1033, 1 / 60, 1],
      penForceTime: 4, penLeavingLimits: [0, 0, 0, 0]
    },

    // 4
    {
      ghostSpeed: .85, ghostTunnelSpeed: .45,
      playerSpeed: .9, dotEatingSpeed: .79,
      ghostFrightSpeed: .55, playerFrightSpeed: .95, dotEatingFrightSpeed: .83,
      elroyDotsLeftPart1: 40, elroySpeedPart1: .9,
      elroyDotsLeftPart2: 20, elroySpeedPart2: .95,
      frightTime: 3, frightBlinkCount: 5,
      fruit: 3, fruitScore: 500,
      ghostModeSwitchTimes: [7, 20, 7, 20, 5, 1033, 1 / 60, 1],
      penForceTime: 4, penLeavingLimits: [0, 0, 0, 0]
    },

    // 5
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 40, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 20, elroySpeedPart2: 1.05,
      frightTime: 2, frightBlinkCount: 5,
      fruit: 4, fruitScore: 700,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0],

      cutsceneId: 2
    },

    // 6
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 50, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 25, elroySpeedPart2: 1.05,
      frightTime: 5, frightBlinkCount: 5,
      fruit: 4, fruitScore: 700,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 7
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 50, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 25, elroySpeedPart2: 1.05,
      frightTime: 2, frightBlinkCount: 5,
      fruit: 5, fruitScore: 1000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 8
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 50, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 25, elroySpeedPart2: 1.05,
      frightTime: 2, frightBlinkCount: 5,
      fruit: 5, fruitScore: 1000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 9
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 60, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 30, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 6, fruitScore: 2000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0],

      cutsceneId: 3
    },

    // 10
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 60, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 30, elroySpeedPart2: 1.05,
      frightTime: 5, frightBlinkCount: 5,
      fruit: 6, fruitScore: 2000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 11
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 60, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 30, elroySpeedPart2: 1.05,
      frightTime: 2, frightBlinkCount: 5,
      fruit: 7, fruitScore: 3000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 12
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 80, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 40, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 7, fruitScore: 3000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 13
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 80, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 40, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0],

      cutsceneId: 3
    },

    // 14
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 80, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 40, elroySpeedPart2: 1.05,
      frightTime: 3, frightBlinkCount: 5,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 15
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 100, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 50, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 16
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 100, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 50, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 17
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 100, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 50, elroySpeedPart2: 1.05,
      frightTime: 0, frightBlinkCount: 0,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0],

      cutsceneId: 3
    },

    // 18
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 100, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 50, elroySpeedPart2: 1.05,
      frightTime: 1, frightBlinkCount: 3,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 19
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 120, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 60, elroySpeedPart2: 1.05,
      frightTime: 0, frightBlinkCount: 0,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 20
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: 1, dotEatingSpeed: .87,
      ghostFrightSpeed: .6, playerFrightSpeed: 1, dotEatingFrightSpeed: .87,
      elroyDotsLeftPart1: 120, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 60, elroySpeedPart2: 1.05,
      frightTime: 0, frightBlinkCount: 0,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]
    },

    // 21+
    {
      ghostSpeed: .95, ghostTunnelSpeed: .5,
      playerSpeed: .9, dotEatingSpeed: .79,
      ghostFrightSpeed: .75, playerFrightSpeed: .9, dotEatingFrightSpeed: .79,
      elroyDotsLeftPart1: 120, elroySpeedPart1: 1,
      elroyDotsLeftPart2: 60, elroySpeedPart2: 1.05,
      frightTime: 0, frightBlinkCount: 0,
      fruit: 8, fruitScore: 5000,
      ghostModeSwitchTimes: [5, 20, 5, 20, 5, 1037, 1 / 60, 1],
      penForceTime: 3, penLeavingLimits: [0, 0, 0, 0]

    }
  ];

  /**
   * Constants for different "routines." Routines are pre-programmed paths
   * ghosts follow when entering/leaving the pen.
   */
  static final ROUTINE_LEFT_PEN = 1;
  static final ROUTINE_CENTER_PEN = 2;
  static final ROUTINE_RIGHT_PEN = 3;
  static final ROUTINE_LEFT_PEN_EXIT = 4;
  static final ROUTINE_CENTER_PEN_EXIT = 5;
  static final ROUTINE_RIGHT_PEN_EXIT = 6;
  static final ROUTINE_REENTER_LEFT_PEN = 7;
  static final ROUTINE_REENTER_CENTER_PEN = 8;
  static final ROUTINE_REENTER_RIGHT_PEN = 9;
  static final ROUTINE_REEXIT_LEFT_PEN = 10;
  static final ROUTINE_REEXIT_CENTER_PEN = 11;
  static final ROUTINE_REEXIT_RIGHT_PEN = 12;

  /**
   * Different routines. Each step has a position, direction, speed, and
   * the destination value (X if left/right, Y if up/down).
   */
  static final ROUTINES = {
    // TODO(jgw): const
    1: [ // ROUTINE_LEFT_PEN
      new Routine(37.6, 7,     DIR_UP, 6.375, PEN_SPEED),
      new Routine(37.6, 6.375, DIR_DOWN, 7.625, PEN_SPEED),
      new Routine(37.6, 7.625, DIR_UP, 7, PEN_SPEED)
    ],
    2: [ // ROUTINE_CENTER_PEN
      new Routine(39.5, 7, DIR_DOWN, 7.625, PEN_SPEED),
      new Routine(39.5, 7.625, DIR_UP, 6.375, PEN_SPEED),
      new Routine(39.5, 6.375, DIR_DOWN, 7, PEN_SPEED)
    ],
    3: [ // ROUTINE_RIGHT_PEN
      new Routine(41.4, 7, DIR_UP, 6.375, PEN_SPEED),
      new Routine(41.4, 6.375, DIR_DOWN, 7.625, PEN_SPEED),
      new Routine(41.4, 7.625, DIR_UP, 7, PEN_SPEED)
    ],
    4: [ // ROUTINE_LEFT_PEN_EXIT
      new Routine(37.6, 7, DIR_RIGHT, 39.5, EXIT_PEN_SPEED),
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ],
    5: [ // ROUTINE_CENTER_PEN_EXIT
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ],
    6: [ // ROUTINE_RIGHT_PEN_EXIT
      new Routine(41.4, 7, DIR_LEFT, 39.5, EXIT_PEN_SPEED),
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ],
    7: [ // ROUTINE_REENTER_LEFT_PEN
      new Routine(39.5, 4, DIR_DOWN, 7, EYES_SPEED),
      new Routine(39.5, 7, DIR_LEFT, 37.625, EYES_SPEED)
    ],
    8: [ // ROUTINE_REENTER_CENTER_PEN
      new Routine(39.5, 4, DIR_DOWN, 7, EYES_SPEED)
    ],
    9: [ // ROUTINE_REENTER_RIGHT_PEN
      new Routine(39.5, 4, DIR_DOWN, 7, EYES_SPEED),
      new Routine(39.5, 7, DIR_RIGHT, 41.375, EYES_SPEED)
    ],
    10: [ // ROUTINE_REEXIT_LEFT_PEN
      new Routine(37.6, 7, DIR_RIGHT, 39.5, EXIT_PEN_SPEED),
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ],
    11: [ // ROUTINE_REEXIT_CENTER_PEN
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ],
    12: [ // ROUTINE_REEXIT_RIGHT_PEN
      new Routine(41.4, 7, DIR_LEFT, 39.5, EXIT_PEN_SPEED),
      new Routine(39.5, 7, DIR_UP, 4, EXIT_PEN_SPEED)
    ]
  };

  /**
   * Information about cutscenes.
   */
  static final CUTSCENES = {
    // First cutscene: Ghost chases Pac-Man, big Pac-Man chases back
    // TODO(jgw): const
    1: new Cutscene([
        new CutsceneActor(false, 64, 9, 0),
        new CutsceneActor(true, 68.2, 9, 1)
      ], [
        new CutsceneSequence(5.5, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, .78 * MASTER_SPEED * 2)
          ]
        ),
        new CutsceneSequence(.1, [
            new CutsceneMove(DIR_LEFT, 20 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, 0)
          ]
        ),
        new CutsceneSequence(9, [
            new CutsceneMove(DIR_RIGHT, .75 * MASTER_SPEED * 2, null, 'pcm-bpcm'),
            new CutsceneMove(DIR_RIGHT, .5 * MASTER_SPEED * 2, GHOST_MODE_FRIGHT)
          ]
        )
      ]),

    // Second cutscene: Ghost getting damaged
    2: new Cutscene([
        new CutsceneActor(false, 64, 9, 0),
        new CutsceneActor(true, 70.2, 9, 1),
        new CutsceneActor(true, 32, 9.5, 2)
      ], [
        new CutsceneSequence(2.70, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, .78 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_NONE, 0, null, 'pcm-stck')
          ]
        ),
        new CutsceneSequence(1, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, .1 * MASTER_SPEED),
            new CutsceneMove(DIR_NONE, 0, null, 'pcm-stck')
          ]
        ),
        new CutsceneSequence(1.3, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, 0 * MASTER_SPEED),
            new CutsceneMove(DIR_NONE, 0, null, 'pcm-stck')
          ]
        ),
        new CutsceneSequence(1, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, 0, null, 'pcm-ghfa'),
            new CutsceneMove(DIR_NONE, 0, null, 'pcm-stck')
          ]
        ),
        new CutsceneSequence(2.5, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, 0, null, 'pcm-ghfa'),
            new CutsceneMove(DIR_NONE, 0, null, 'pcm-stck')
          ]
        )
      ]),

    // Third cutscene: Fixed ghost chases Pac-Man, bug runs away
    3: new Cutscene([
        new CutsceneActor(false, 64, 9, 0),
        new CutsceneActor(true, 70.2, 9, 2)
      ], [
        new CutsceneSequence(5.3, [
            new CutsceneMove(DIR_LEFT, .75 * MASTER_SPEED * 2),
            new CutsceneMove(DIR_LEFT, .78 * MASTER_SPEED * 2, null, 'pcm-ghin')
          ]
        ),
        new CutsceneSequence(5.3, [
            new CutsceneMove(DIR_LEFT, 0),
            new CutsceneMove(DIR_RIGHT, .78 * MASTER_SPEED * 2, null, 'pcm-gbug')
          ]
        )
      ])
  };

  /**
   * Allowed framerates. The game will start with the first one (90fps) and
   * scale down if it determines the computer is too slow.
   */
  static final ALLOWED_FPS = [90, 45, 30];
  static final TARGET_FPS = ALLOWED_FPS[0];

  /**
   * Stop trying to catch up with timer if it exceeds 100ms. Otherwise,
   * if the whole browser stutters, Pac-Man/ghosts etc. would jump a long
   * distance.
   */
  static final MAX_TIME_DELTA = 100;

  /**
   * If two subsequent clock ticks are longer than 50ms, we increase the
   * slowness count.
   */
  static final TIME_SLOWNESS = 50;

  /**
   * If the slowness count exceeds 20, we drop to lower framerate.
   */
  static final MAX_SLOWNESS_COUNT = 20;

  /**
   * How many fruit icons maximum can we show to indicate the current level
   * in the lower-right corner.
   */
  static final LEVEL_CHROME_MAX = 4;

  /**
   * Key codes for all keyboard operations.
   */
  static final KEYCODE_LEFT = 37;
  static final KEYCODE_UP = 38;
  static final KEYCODE_RIGHT = 39;
  static final KEYCODE_DOWN = 40;
  static final KEYCODE_A = 65;
  static final KEYCODE_D = 68;
  static final KEYCODE_S = 83;
  static final KEYCODE_W = 87;

  /**
   * Click event sensitivity. Any click event further than 8px from Pac-Man
   * will be interpreted as directional.
   */
  static final CLICK_SENSITIVITY = 8;

  /**
   * Touch event sensitivity. Any touch event more than 15px long will
   * be interpreted as a stroke.
   */
  static final TOUCH_SENSITIVITY = 15;

  /**
   * Touch click event sensitivity. Any touch event shorter than 8px will
   * be interpreted as a click.
   */
  static final TOUCH_CLICK_SENSITIVITY = 8;

  /**
   * Minimal supported Flash version.
   */
  static final MIN_FLASH_VERSION = '9.0.0.0';

  /**
   * We allow Flash 3 seconds to load and initialize. If it doesn't in that
   * time, we start the game without sound. This is in case something goes
   * wrong and Flash is detected but doesn't load correctly.
   */
  static final FLASH_NOT_READY_TIMEOUT = 3000;
}

class Actor {
  int id;
  Element el;
  Array<int> pos, posDelta, tilePos, targetPos, scatterPos, elPos, elBackgroundPos, lastGoodTilePos;
  int dir, lastActiveDir, nextDir, requestedDir; // TODO: enum
  int routineMoveId, routineToFollow;
  int physicalSpeed, fullSpeed, tunnelSpeed;
  boolean eatenInThisFrightMode, modeChangedWhileInPen, freeToLeavePen, reverseDirectionsNext;
  boolean followingRoutine;
  boolean ghost, mode;
  int dotCount;
  int currentSpeed;
  Array<int> speedIntervals;
  boolean proceedToNextRoutineMove;
  int speed;
  int targetPlayerId;

  /**
   * Creates a Pac-Man actor object with a given actor id.
   * @param id Actor id (0 for Pac-Man, 1 for Ms. Pac-Man if present, 2, 3, etc. for ghosts).
   */
  Actor(int id) {
    this.id = id;
  }

  /**
   * Restarts/reboots the actor (usually at the beginning of new level/new
   * life).
   */
  void restart() {
    var position = PM.INITIAL_ACTOR_POSITIONS[PacMan.playerCount][id];

    // Current position, including fractions for positions within tiles.
    pos = [position.y * PM.TILE_SIZE, position.x * PM.TILE_SIZE];
    // Used for subtle changes in position when cornering.
    posDelta = [0, 0];
    // Last full/rounded position (or = currently active tile).
    tilePos = [position.y * PM.TILE_SIZE, position.x * PM.TILE_SIZE];
    // For ghosts, the tile the ghosts is going after.
    targetPos = [position.scatterY * PM.TILE_SIZE, position.scatterX * PM.TILE_SIZE];
    // For ghosts, the target tile in the scatter mode.
    scatterPos = [position.scatterY * PM.TILE_SIZE, position.scatterX * PM.TILE_SIZE];

    // Current direction.
    dir = position.dir;
    // Last direction before actor stopped.
    lastActiveDir = dir;
    // Next direction to be taken at the nearest possible intersection.
    nextDir = PM.DIR_NONE;
    // Next player direction requested by keyboard press or touch event.
    requestedDir = PM.DIR_NONE;

    physicalSpeed = 0;

    // Current speed (full, tunnel, eating dot, etc.).
    changeCurrentSpeed(PM.SPEED_FULL);

    // If the ghost was already eaten during a given fright mode, it will
    // exit the pen non-blue.
    eatenInThisFrightMode = false;
    // If the global mode was changed while the ghost was in the pen,
    // the ghost will chose a different direction upon entering.
    modeChangedWhileInPen = false;
    // Whether the ghost is free to leave pen. Does not apply to Blinky,
    // who's never really in the pen.
    freeToLeavePen = false;
    reverseDirectionsNext = false;

    updateTargetPlayerId();
  }

  /**
   * Creates a DOM element for a given actor.
   */
  void createElement() {
    el = document.createElement('div');
    el.attributes['class'] = 'pcm-ac';
    el.id = 'actor' + id;
    PacMan.prepareElement(el, 0, 0);
    PacMan.playfieldEl.appendChild(el);

    // We store the last DOM position and last DOM background position so that
    // we don't request DOM changes unless we have to.
    elPos = [0, 0];
    elBackgroundPos = [0, 0];
  }

  /**
   * Changes the mode of a given actor (ghost) and reacts to the change.
   * @param mode New mode (PM.GHOST_MODE_* constants).
   */
  void changeMode(int mode) {
    var oldMode = this.mode;
    this.mode = mode;

    // Cruise Elroy speed sometimes depends on whether the last ghost is or is
    // not in the pen.
    if ((id == PacMan.playerCount + 3) &&
        (mode == PM.GHOST_MODE_IN_PEN || oldMode == PM.GHOST_MODE_IN_PEN)) {
      PacMan.updateCruiseElroySpeed();
    }

    switch (oldMode) {
      case PM.GHOST_MODE_EXITING_PEN:
        PacMan.ghostExitingPenNow = false;
        break;
      case PM.GHOST_MODE_EYES:
        if (PacMan.ghostEyesCount > 0) {
          PacMan.ghostEyesCount = PacMan.ghostEyesCount - 1; // TODO(jgw): --
        }
        if (PacMan.ghostEyesCount == 0) {
          PacMan.playAmbientSound();
        }
        break;
    }

    switch (mode) {
      case PM.GHOST_MODE_FRIGHT:
        fullSpeed = PacMan.levels['ghostFrightSpeed'] * PM.MASTER_SPEED;
        tunnelSpeed = PacMan.levels['ghostTunnelSpeed'] * PM.MASTER_SPEED;
        followingRoutine = false;
        break;
      case PM.GHOST_MODE_CHASE:
        fullSpeed = PacMan.levels['ghostSpeed'] * PM.MASTER_SPEED;
        tunnelSpeed = PacMan.levels['ghostTunnelSpeed'] * PM.MASTER_SPEED;
        followingRoutine = false;
        break;
      case PM.GHOST_MODE_SCATTER:
        targetPos = scatterPos;
        fullSpeed = PacMan.levels['ghostSpeed'] * PM.MASTER_SPEED;
        tunnelSpeed = PacMan.levels['ghostTunnelSpeed'] * PM.MASTER_SPEED;
        followingRoutine = false;
        break;
      case PM.GHOST_MODE_EYES:
        fullSpeed = PM.EYES_SPEED;
        tunnelSpeed = PM.EYES_SPEED;
        targetPos = [PM.EXIT_PEN_POS[0], PM.EXIT_PEN_POS[1]];
        followingRoutine = false;
        freeToLeavePen = false;
        break;
      case PM.GHOST_MODE_IN_PEN:
        // Randomly pick a target (Pac-Man or Ms. Pac-Man) to focus on,
        // so that it's a surprise if a ghost exits the pen.
        updateTargetPlayerId();

        followingRoutine = true;
        routineMoveId = -1;
        switch (id) {
          case PacMan.playerCount + 1:
            routineToFollow = PM.ROUTINE_CENTER_PEN;
            break;
          case PacMan.playerCount + 2:
            routineToFollow = PM.ROUTINE_LEFT_PEN;
            break;
          case PacMan.playerCount + 3:
            routineToFollow = PM.ROUTINE_RIGHT_PEN;
            break;
        }
        break;
      case PM.GHOST_MODE_EXITING_PEN:
        followingRoutine = true;
        routineMoveId = -1;
        switch (id) {
          case PacMan.playerCount + 1:
            routineToFollow = PM.ROUTINE_CENTER_PEN_EXIT;
            break;
          case PacMan.playerCount + 2:
            routineToFollow = PM.ROUTINE_LEFT_PEN_EXIT;
            break;
          case PacMan.playerCount + 3:
            routineToFollow = PM.ROUTINE_RIGHT_PEN_EXIT;
            break;
        }
        PacMan.ghostExitingPenNow = true;
        break;
      case PM.GHOST_MODE_REENTERING_PEN:
        followingRoutine = true;
        routineMoveId = -1;
        switch (id) {
          case PacMan.playerCount:
          case PacMan.playerCount + 1:
            routineToFollow = PM.ROUTINE_REENTER_CENTER_PEN;
            break;
          case PacMan.playerCount + 2:
            routineToFollow = PM.ROUTINE_REENTER_LEFT_PEN;
            break;
          case PacMan.playerCount + 3:
            routineToFollow = PM.ROUTINE_REENTER_RIGHT_PEN;
            break;
        }
        break;
      case PM.GHOST_MODE_REEXITING_PEN:
        followingRoutine = true;
        routineMoveId = -1;
        switch (id) {
          case PacMan.playerCount:
          case PacMan.playerCount + 1:
            routineToFollow = PM.ROUTINE_REEXIT_CENTER_PEN;
            break;
          case PacMan.playerCount + 2:
            routineToFollow = PM.ROUTINE_REEXIT_LEFT_PEN;
            break;
          case PacMan.playerCount + 3:
            routineToFollow = PM.ROUTINE_REEXIT_RIGHT_PEN;
            break;
        }

        break;
    }

    updatePhysicalSpeed();
  }

  /**
   * For two-player game (with Pac-Man and Ms. Pac-Man) each ghosts randomly
   * focuses on one of the players. This changes every time a ghost leaves
   * a pen to make things more interesting.
   */
  void updateTargetPlayerId() {
    if (id >= PacMan.playerCount) {
      targetPlayerId = Math.floor(PacMan.rand() * PacMan.playerCount);
    }
  }

  /**
   * Process a direction requested by player using keyboard, touch, etc.
   * @param newDir New direction.
   */
  void processRequestedDirection(int newDir) {
    // Enable sound as long as the user hasn't previously disabled it by clicking
    // the sound icon.
    if (!PacMan.userDisabledSound) {
      PacMan.pacManSound = true;
      PacMan.updateSoundIcon();
    }

    // If the new direction is the opposite of the current one, we turn
    // immediately in place...
    if (dir == PacMan.oppositeDirections[newDir]) {
      dir = newDir;

      posDelta = [0, 0];

      // If Pac-Man reverses direction in a tile when it was eating a dot,
      // we restore the speed to full on reverse.
      if (currentSpeed != PM.SPEED_TUNNEL) {
        changeCurrentSpeed(PM.SPEED_FULL);
      }
      if (dir != PM.DIR_NONE) {
        lastActiveDir = dir;
      }
      nextDir = PM.DIR_NONE;
    } else if (dir != newDir) {
      // ...otherwise, we either move the Pac-Man straight away if it's
      // stationary...
      if (dir == PM.DIR_NONE) {
        if (PacMan.playfield[pos[0]][pos[1]].allowedDir & newDir) {
          dir = newDir;
        }
      } else {
        // We want to be more forgiving with control. If the player presses
        // the arrow one or two pixels *after* the intersection, we still want
        // to allow the turn.
        var tile = PacMan.playfield[tilePos[0]][tilePos[1]];
        if (tile && (tile.allowedDir & newDir)) {
          var movement = PM.MOVEMENTS[dir];
          var newPos = [pos[0], pos[1]];
          newPos[movement.axis] -= movement.increment;

          var backtrackDist = 0;
          if (newPos[0] == tilePos[0] && newPos[1] == tilePos[1]) {
            backtrackDist = 1;
          } else {
            newPos[movement.axis] -= movement.increment;

            if (newPos[0] == tilePos[0] && newPos[1] == tilePos[1]) {
              backtrackDist = 2;
            }
          }

          if (backtrackDist) {
            dir = newDir;
            pos[0] = tilePos[0];
            pos[1] = tilePos[1];
            var movement = PM.MOVEMENTS[dir];
            pos[movement.axis] += movement.increment * backtrackDist;
            return;
          }
        }

        // ...all else failing, we store the direction for the next possible
        // intersection
        nextDir = newDir;
        posDelta = [0, 0];
      }
    }
  }

  /**
   * Calculate the next direction for a ghost.
   * @param afterReversal Did the ghost just reverse course?
   */
  void figureOutNextDirection(boolean afterReversal) {
    var tilePos = this.tilePos;

    var movement = PM.MOVEMENTS[this.dir];
    var newPos = [tilePos[0], tilePos[1]];
    newPos[movement.axis] += movement.increment * PM.TILE_SIZE;

    var tile = PacMan.playfield[newPos[0]][newPos[1]];
    // If the ghost just reversed course, and reverses back into a
    // corner, the tile ahead might be a wall. In that case, we use a current
    // tile.
    if (afterReversal && !tile.intersection) {
      tile = PacMan.playfield[tilePos[0]][tilePos[1]];
    }

    if (tile.intersection) {
      switch (mode) {
        case PM.GHOST_MODE_SCATTER:
        case PM.GHOST_MODE_CHASE:
        case PM.GHOST_MODE_EYES:
          // If only the opposite direction is allowed (dead end), we have
          // to take it. Reversing back in an intersection is otherwise not
          // allowed, hence the special case.
          if ((dir & tile.allowedDir) == 0 &&
              tile.allowedDir == PacMan.oppositeDirections[dir]) {
            nextDir = PacMan.oppositeDirections[dir];
          } else {
            // Try each direction that's available and see which gets us
            // closer (in a straight line) to the target tile.
            var bestDistance = 2147483647; // TODO: 0x7fffffff
            var bestDir = PM.DIR_NONE;

            for (var i = 0; i < PM.DIRS.length; ++i) {
              var dir = PM.DIRS[i];

              if ((tile.allowedDir & dir) &&
                  (this.dir != PacMan.oppositeDirections[dir])) {

                var movement = PM.MOVEMENTS[dir];
                var simulatedPos = [newPos[0], newPos[1]];
                simulatedPos[movement.axis] += movement.increment;
                var newDistance = PacMan.getDistance(simulatedPos, [targetPos[0], targetPos[1]]);

                if (newDistance < bestDistance) {
                  bestDistance = newDistance;
                  bestDir = dir;
                }
              }
            }
            if (bestDir) {
              nextDir = bestDir;
            }

          }
          break;

        case PM.GHOST_MODE_FRIGHT:
          // If only the opposite direction is allowed (dead end), we have
          // to take it. Reversing back in an intersection is otherwise not
          // allowed, hence the special case.
          if ((dir & tile.allowedDir) == 0 &&
              tile.allowedDir == PacMan.oppositeDirections[dir]) {
            nextDir = PacMan.oppositeDirections[dir];
          } else {
            // In scatter mode, we just take a random new direction (except
            // reverse, which is not allowed).
            var random;
            do {
              random = PM.DIRS[Math.floor(PacMan.rand() * 4)];
            } while (((random & tile.allowedDir) == 0) ||
                     (random == PacMan.oppositeDirections[dir]));
            nextDir = random;
          }
          break;
      }
    }
  }

  /**
   * Handling a new tile. Changing the speed depending if it's a tunnel or not,
   * eat a dot if it's there, etc.
   * @param tilePos Position of a new tile.
   */
  void enterNewTile(Array<int> tilePos) {
    PacMan.tilesChanged = true;

    // When modes change, ghosts reverse directions immediately. We need to
    // special case this, since there will be no time to figure out the next
    // direction the proper way this way.
    if (reverseDirectionsNext) {
      dir = PacMan.oppositeDirections[dir];
      nextDir = 0;
      reverseDirectionsNext = false;
      figureOutNextDirection(true);
    }

    // If Marcin was a good programmer, the algorithms governing Pac-Man
    // movements would be rock solid. That is not the case. Ryan found a bug
    // once that made him go through a wall. If Marcin was a good tester, he
    // would figure out how to reproduce and fix the bug. That is not the
    // case. Hence a little protection below. This checks if a new tile is
    // actually a valid path and if not, go back to the last-known good tile
    // and stop Pac-Man in its tracks. This is essentially a "treating
    // symptoms" protection, but hey, it's better than nothing, right?
    if (!ghost && !PacMan.playfield[tilePos[0]][tilePos[1]].path) {
      pos[0] = lastGoodTilePos[0];
      pos[1] = lastGoodTilePos[1];
      tilePos[0] = lastGoodTilePos[0];
      tilePos[1] = lastGoodTilePos[1];
      dir = PM.DIR_NONE;
    } else {
      lastGoodTilePos = [tilePos[0], tilePos[1]];
    }

    // You slow down in a tunnel, except if you're ghost's eyes.
    if ((PacMan.playfield[tilePos[0]][tilePos[1]].type == PM.PATH_TUNNEL) &&
       (mode != PM.GHOST_MODE_EYES)) {
      changeCurrentSpeed(PM.SPEED_TUNNEL);
    } else {
      changeCurrentSpeed(PM.SPEED_FULL);
    }

    if (!ghost && PacMan.playfield[tilePos[0]][tilePos[1]].dot) {
      PacMan.dotEaten(id, tilePos);
    }

    this.tilePos[0] = tilePos[0];
    this.tilePos[1] = tilePos[1];
  }

  /**
   * Pac-Man and Ms. Pac-Man (but not ghost) are allowed to "corner." If the
   * direction is changed in advance of intersection, a couple pixels (3 or 4)
   * close to the middle of it are taken diagonally, allowing to gain extra
   * distance when ghosts chase you.
   */
  void handleCornering() {
    var tilePos = this.tilePos;
    Array<int> minPos, maxPos;

    switch (dir) {
      case PM.DIR_UP:
        minPos = [tilePos[0], tilePos[1]];
        maxPos = [tilePos[0] + PM.CORNER_DELTA_MAX, tilePos[1]];
        break;
      case PM.DIR_DOWN:
        minPos = [tilePos[0] - PM.CORNER_DELTA_MIN, tilePos[1]];
        maxPos = [tilePos[0], tilePos[1]];
        break;
      case PM.DIR_LEFT:
        minPos = [tilePos[0], tilePos[1]];
        maxPos = [tilePos[0], tilePos[1] + PM.CORNER_DELTA_MAX];
        break;
      case PM.DIR_RIGHT:
        minPos = [tilePos[0], tilePos[1] - PM.CORNER_DELTA_MIN];
        maxPos = [tilePos[0], tilePos[1]];
        break;
    }

    if (pos[0] >= minPos[0] && pos[0] <= maxPos[0] &&
        pos[1] >= minPos[1] && pos[1] <= maxPos[1]) {
      var movement = PM.MOVEMENTS[nextDir];

      // The corner increment is stored temporarily in posDelta and added to
      // the proper position only after cornering ends.
      posDelta[movement.axis] += movement.increment;
    }
  }


  /**
   * Checks if the current tile is not a special tile that requires a special
   * action.
   */
  void checkSpecialTiles() {
    // Horizontal tunnel.
    if (pos[0] == (PM.TUNNEL_ENDS[0].y * PM.TILE_SIZE) &&
        pos[1] == (PM.TUNNEL_ENDS[0].x * PM.TILE_SIZE)) {
      pos[0] = PM.TUNNEL_ENDS[1].y * PM.TILE_SIZE;
      pos[1] = (PM.TUNNEL_ENDS[1].x - 1) * PM.TILE_SIZE;
    } else if (pos[0] == (PM.TUNNEL_ENDS[1].y * PM.TILE_SIZE) &&
               pos[1] == (PM.TUNNEL_ENDS[1].x * PM.TILE_SIZE)) {
      pos[0] = PM.TUNNEL_ENDS[0].y * PM.TILE_SIZE;
      pos[1] = (PM.TUNNEL_ENDS[0].x + 1) * PM.TILE_SIZE;
    }

    // Entering the pen if the ghosts just died.
    if (mode == PM.GHOST_MODE_EYES &&
        pos[0] == PM.EXIT_PEN_POS[0] &&
        pos[1] == PM.EXIT_PEN_POS[1]) {
      changeMode(PM.GHOST_MODE_REENTERING_PEN);
    }

    // Eating fruit if you're Pac-Man.
    if (!ghost && (pos[0] == PM.FRUIT_POS[0]) &&
        ((pos[1] == PM.FRUIT_POS[1]) ||
         (pos[1] == PM.FRUIT_POS[1] + PM.TILE_SIZE))) {
      PacMan.eatFruit(id);
    }
  }

  /**
   * Does whatever's necessary in the middle of the tile -- mainly, change
   * directions if it's an intersection.
   */
  void middleOfATile() {
    // Check whether the tile is not a special tile (fruit, tunnel, etc.)
    // that requires extra action.
    checkSpecialTiles();

    // Ghosts figure out their next move a bit in advance.
    if (ghost) {
      figureOutNextDirection(false);
    }

    var tile = PacMan.playfield[pos[0]][pos[1]];

    if (tile.intersection) {
      if (nextDir && (nextDir & tile.allowedDir)) {
        // We can turn.
        if (dir != PM.DIR_NONE) {
          lastActiveDir = dir;
        }
        dir = nextDir;
        nextDir = PM.DIR_NONE;

        // Include the position changes gathered by cornering.
        if (!ghost) {
          pos[0] += posDelta[0];
          pos[1] += posDelta[1];
          posDelta = [0, 0];
        }
      } else if ((dir & tile.allowedDir) == PM.DIR_NONE) {
        // We cannot turn. (This should never happen for a ghost.)

        if (dir != PM.DIR_NONE) {
          lastActiveDir = dir;
        }
        dir = PM.DIR_NONE;
        nextDir = PM.DIR_NONE;
        changeCurrentSpeed(PM.SPEED_FULL);
      }
    }
  }

  /**
   * React to the new position: perhaps it's the new tile, perhaps it's the
   * middle of the tile, perhaps cornering should be covered...
   */
  void checkTheNewPixel() {
    var y = pos[0] / PM.TILE_SIZE;
    var x = pos[1] / PM.TILE_SIZE;

    var tilePos = [Math.round(y) * PM.TILE_SIZE, Math.round(x) * PM.TILE_SIZE];

    if (tilePos[0] != this.tilePos[0] || tilePos[1] != this.tilePos[1]) {
      enterNewTile(tilePos);
    } else {
      var floorPos = [Math.floor(y) * PM.TILE_SIZE, Math.floor(x) * PM.TILE_SIZE];
      if (pos[1] == floorPos[1] && pos[0] == floorPos[0]) {
        middleOfATile();
      }
    }

    if (!ghost && nextDir &&
        PacMan.playfield[tilePos[0]][tilePos[1]].intersection &&
        (nextDir & PacMan.playfield[tilePos[0]][tilePos[1]].allowedDir)) {
      handleCornering();
    }
  }

  /**
   * Each ghost has a target position that determines where it's heading.
   * It could be a tile outside a screen in scatter mode (so that the ghost
   * is just wandering around, never to reach it); a Pac-Man tile or a tile
   * close to Pac-Man in chase mode; a pen if the ghost died, etc.
   */
  void updateTargetPosition() {
    // When Blinky becomes Cruise Elroy, it always follows Pac-Man, even in
    // scatter mode.
    if (id == PacMan.playerCount &&
        PacMan.dotsRemaining < PacMan.levels['elroyDotsLeftPart1'] &&
        mode == PM.GHOST_MODE_SCATTER &&
        (!PacMan.lostLifeOnThisLevel ||
         PacMan.actors[PacMan.playerCount + 3].mode != PM.GHOST_MODE_IN_PEN)) {
      var player = PacMan.actors[targetPlayerId];
      targetPos = [player.tilePos[0], player.tilePos[1]];
    } else if (ghost && mode == PM.GHOST_MODE_CHASE) {
      var player = PacMan.actors[targetPlayerId];
      switch (id) {
        // Blinky always follows the player.
        case PacMan.playerCount:
          targetPos = [player.tilePos[0], player.tilePos[1]];
          break;

        // Pinky follows a tile ahead of player.
        case PacMan.playerCount + 1:
          targetPos = [player.tilePos[0], player.tilePos[1]];
          var movement = PM.MOVEMENTS[player.dir];
          targetPos[movement.axis] += 4 * PM.TILE_SIZE * movement.increment;

          // Recreating the original Pac-Man bug, in which UP = UP+LEFT
          if (player.dir == PM.DIR_UP) {
            targetPos[1] -= 4 * PM.TILE_SIZE;
          }
          break;

        // Inky uses a convoluted scheme averaging Blinky's position and
        // an offset tile.
        case PacMan.playerCount + 2:
          var blinky = PacMan.actors[PacMan.playerCount];

          var offsetPos = [player.tilePos[0], player.tilePos[1]];
          var movement = PM.MOVEMENTS[player.dir];
          offsetPos[movement.axis] +=
              2 * PM.TILE_SIZE * movement.increment;

          // Recreating the original Pac-Man bug, in which UP = UP+LEFT
          if (player.dir == PM.DIR_UP) {
            offsetPos[1] -= 2 * PM.TILE_SIZE;
          }

          targetPos[0] = offsetPos[0] * 2 - blinky.tilePos[0];
          targetPos[1] = offsetPos[1] * 2 - blinky.tilePos[1];
          break;

        // Clyde uses Pac-Man's position if further away, scatter position
        // if close.
        case PacMan.playerCount + 3:
          var distance = PacMan.getDistance(player.tilePos, tilePos);

          if (distance > (8 * PM.TILE_SIZE)) {
            targetPos = [player.tilePos[0], player.tilePos[1]];
          } else {
            targetPos = scatterPos;
          }
          break;
      }
    }
  }

  /**
   * If the actor (ghost) is following a routine, proceed to the next move
   * in that routine.
   */
  void nextRoutineMove() {
    routineMoveId = routineMoveId + 1; // TODO(jgw): ++
    if (routineMoveId == PM.ROUTINES[routineToFollow].length) {
      if (mode == PM.GHOST_MODE_IN_PEN && freeToLeavePen && !PacMan.ghostExitingPenNow) {
        if (eatenInThisFrightMode) {
          changeMode(PM.GHOST_MODE_REEXITING_PEN);
        } else {
          changeMode(PM.GHOST_MODE_EXITING_PEN);
        }
        return;
      } else if (mode == PM.GHOST_MODE_EXITING_PEN || mode == PM.GHOST_MODE_REEXITING_PEN) {
        pos = [PM.EXIT_PEN_POS[0], PM.EXIT_PEN_POS[1] + PM.TILE_SIZE / 2];

        // The direction in which the ghost exits the pen depends on whether
        // the main ghost mode changed during the ghost's stay in the pen.
        if (modeChangedWhileInPen) {
          dir = PM.DIR_RIGHT;
        } else {
          dir = PM.DIR_LEFT;
        }
        var mode = PacMan.mainGhostMode;
        if (mode == PM.GHOST_MODE_REEXITING_PEN && mode == PM.GHOST_MODE_FRIGHT) {
          mode = PacMan.lastMainGhostMode;
        }
        changeMode(mode);
        return;
      } else if (mode == PM.GHOST_MODE_REENTERING_PEN) {
        // Blinky never stays in the pen and exits straight away.
        if ((id == PacMan.playerCount) || freeToLeavePen) {
          changeMode(PM.GHOST_MODE_REEXITING_PEN);
        } else {
          eatenInThisFrightMode = true;
          changeMode(PM.GHOST_MODE_IN_PEN);
        }
        return;
      } else {
        routineMoveId = 0;
      }
    }

    var routine = PM.ROUTINES[routineToFollow][routineMoveId];
    pos[0] = routine.y * PM.TILE_SIZE;
    pos[1] = routine.x * PM.TILE_SIZE;
    dir = routine.dir;
    physicalSpeed = 0;
    speedIntervals = PacMan.getSpeedIntervals(routine.speed);
    proceedToNextRoutineMove = false;
    updateSprite();
  }

  /**
   * Does another little step in the routine.
   */
  void advanceRoutine() {
    var routine = PM.ROUTINES[routineToFollow][routineMoveId];
    if (!routine) {
      return;
    }

    // Actors move with different speeds. Instead of handling fractional
    // positions, we just move them at some frames, but not the others.
    // (Actor with half speed will move every other frame.)
    if (speedIntervals[PacMan.intervalTime]) {
      var movement = PM.MOVEMENTS[dir];
      pos[movement.axis] += movement.increment;

      // Checking whether this particular routine move has ended.
      switch (dir) {
        case PM.DIR_UP:
        case PM.DIR_LEFT:
          if (pos[movement.axis] < routine.dest * PM.TILE_SIZE) {
            pos[movement.axis] = routine.dest * PM.TILE_SIZE;
            proceedToNextRoutineMove = true;
          }
          break;
        case PM.DIR_DOWN:
        case PM.DIR_RIGHT:
          if (pos[movement.axis] > routine.dest * PM.TILE_SIZE) {
            pos[movement.axis] = routine.dest * PM.TILE_SIZE;
            proceedToNextRoutineMove = true;
          }
          break;
      }

      updateSprite();
    }
  }

  /**
   * Follow the routine.
   */
  void followRoutine() {
    if (routineMoveId == -1 || proceedToNextRoutineMove) {
      nextRoutineMove();
    }

    advanceRoutine();
  }

  /**
   * Update the physical speed of the actor.
   */
  void updatePhysicalSpeed() {
    int newPhysicalSpeed;

    switch (currentSpeed) {
      case PM.SPEED_FULL:
        // We use Cruise Elroy speed for Blinky.
        if (id == PacMan.playerCount &&
            (mode == PM.GHOST_MODE_SCATTER || mode == PM.GHOST_MODE_CHASE)) {
          newPhysicalSpeed = PacMan.cruiseElroySpeed;
        } else {
          newPhysicalSpeed = fullSpeed;
        }
        break;
      case PM.SPEED_DOT_EATING:
        newPhysicalSpeed = this['dotEatingSpeed'];  // TODO(jgw): This looks like a mistake
        break;
      case PM.SPEED_TUNNEL:
        newPhysicalSpeed = tunnelSpeed;
        break;
    }

    // Recalculate speed intervals if this is a new speed.
    if (physicalSpeed != newPhysicalSpeed) {
      physicalSpeed = newPhysicalSpeed;
      speedIntervals = PacMan.getSpeedIntervals(physicalSpeed);
    }
  }

  /**
   * Change the current (logical) speed of the actor.
   * @param speed New speed.
   */
  void changeCurrentSpeed(int speed) {
    currentSpeed = speed;
    updatePhysicalSpeed();
  }

  /**
   * Advance the actor by one pixel in a given direction if necessary
   */
  void advance() {
    if (dir) {
      // Actors move with different speeds. Instead of handling fractional
      // positions, we just move them at some frames, but not the others.
      // (Actor with half speed will move every other frame.)
      if (speedIntervals[PacMan.intervalTime]) {
        var movement = PM.MOVEMENTS[dir];
        pos[movement.axis] += movement.increment;

        checkTheNewPixel();
        updateSprite();
      }
    }
  }

  /**
   * Moves the actor. This varies depending of whether the actor follows the
   * routine or is free to move.
   */
  void move() {
    // We move the actors only during regular gameplay. An exception is that
    // ghost eyes move when another ghost is being eaten.
    if (PacMan.gameplayMode == PM.GAMEPLAY_GAME_IN_PROGRESS ||
        (ghost && (PacMan.gameplayMode == PM.GAMEPLAY_GHOST_BEING_EATEN) &&
         (mode == PM.GHOST_MODE_EYES || mode == PM.GHOST_MODE_REENTERING_PEN))) {

      if (requestedDir != PM.DIR_NONE) {
        processRequestedDirection(requestedDir);
        requestedDir = PM.DIR_NONE;
      }

      if (followingRoutine) {
        followRoutine();

        // When the ghost turns into eyes, it moves faster than 1px per 90fps.
        // Instead of supporting 180fps (too slow for IE), we just
        // special-case it and move it twice per frame.
        if (mode == PM.GHOST_MODE_REENTERING_PEN) {
          followRoutine();
        }
      } else {
        advance();
        // When the ghost turns into eyes, it moves faster than 1px per 90fps.
        // Instead of supporting 180fps (too slow for IE), we just
        // special-case it and move it twice per frame.
        if (mode == PM.GHOST_MODE_EYES) {
          advance();
        }
      }
    }
  }

  /**
   * Update actor sprite position on the screen.
   */
  void updatePosition() {
    // posDelta accounts for cornering.
    var x = PacMan.getPlayfieldX(pos[1] + posDelta[1]);
    var y = PacMan.getPlayfieldY(pos[0] + posDelta[0]);

    // We only request change to the sprite when it's actually different
    // than last time.
    if ((elPos[0] != y) || (elPos[1] != x)) {
      elPos[0] = y;
      elPos[1] = x;

      el.style.left = x + 'px';
      el.style.top = y + 'px';
    }
  }

  /**
   * Get a proper position of player (Pac-Man, Ms. Pac-Man) sprite depending
   * on the current state, mode, etc.
   * @return X and Y position of the sprite.
   */
  Array<int> getPlayerSprite() {
    var x = 0;
    var y = 0;

    var dir = this.dir;
    if (dir == 0) {
      dir = lastActiveDir;
    }

    if ((PacMan.gameplayMode == PM.GAMEPLAY_GHOST_BEING_EATEN) &&
        (id == PacMan.playerEatingGhostId)) {
      // Whoever ate the ghost disappears when the ghost is being eaten (and
      // the score is being shown).
      x = 3;
      y = 0;
    } else if ((PacMan.gameplayMode == PM.GAMEPLAY_LEVEL_COMPLETE_PART_1 ||
        PacMan.gameplayMode == PM.GAMEPLAY_LEVEL_COMPLETE_PART_2) &&
        (id == PM.PACMAN)) {
      // Pac-Man becomes a circle when the level is completed.
      x = 2;
      y = 0;
    } else if (PacMan.gameplayMode == PM.GAMEPLAY_READY_PART_1 ||
               PacMan.gameplayMode == PM.GAMEPLAY_READY_PART_2 ||
               PacMan.gameplayMode == PM.GAMEPLAY_FAST_READY_PART_2) {
      // Pac-Man becomes a circle and Ms. Pac-Man looks left when the level
      // is about to start.
      if (id == PM.PACMAN) {
        x = 2;
        y = 0;
      } else {
        x = 4;
        y = 0;
      }
    } else if (PacMan.gameplayMode == PM.GAMEPLAY_PLAYER_DYING_PART_2) {
      // Handle the dying animation, which is different for Pac-Man and for
      // Ms. Pac-Man.
      if (id == PacMan.playerDyingId) {
        var time = 20 - Math.floor(PacMan.gameplayModeTime /
            PacMan.timing[PM.TIMING_PLAYER_DYING_PART_2] * 21);
        if (id == PM.PACMAN) { // Pac-Man
          var x = time - 1;
          switch (x) {
            case -1:
              x = 0;
              break;
            case 11:
              x = 10;
              break;
            case 12: case 13: case 14: case 15: case 16:
            case 17: case 18: case 19: case 20:
              x = 11;
              break;
          }
          y = 12;
        } else { // Ms. Pac-Man.
          switch (time) {
            case 0: case 1: case 2: case 6: case 10:
              x = 4; y = 3;
              break;
            case 3: case 7: case 11:
              x = 4; y = 0;
              break;
            case 4: case 8: case 12: case 13: case 14: case 15:
            case 16: case 17: case 18: case 19: case 20:
              x = 4; y = 2;
              break;
            case 5: case 9:
              x = 4; y = 1;
              break;
          }
        }
      } else {
        x = 3;
        y = 0;
      }
    } else if (el.id == 'pcm-bpcm') {
      // Big Pac-Man using during the cutscene.
      x = 14;
      y = 0;

      var cor = Math.floor(PacMan.globalTime * 0.2) % 4;
      if (cor == 3) {
        cor = 1;
      }
      y += 2 * cor;
    } else {
      switch (dir) {
        case PM.DIR_LEFT: y = 0; break;
        case PM.DIR_RIGHT: y = 1; break;
        case PM.DIR_UP: y = 2; break;
        case PM.DIR_DOWN: y = 3; break;
      }

      // This makes Pac-Man mouth open/close.
      if (PacMan.gameplayMode != PM.GAMEPLAY_PLAYER_DYING_PART_1) {
        x = Math.floor(PacMan.globalTime * 0.3) % 4;
      }

      // We don't repeat sprites in the sprite png, so here we need to
      // make some corrections for those repeating ones.
      if ((x == 3) && (dir == PM.DIR_NONE)) {
        x = 0;
      }
      if ((x == 2) && (id == PM.PACMAN)) {
        x = 0;
      }
      if (x == 3) {
        x = 2;
        if (id == PM.PACMAN) {
          y = 0;
        }
      }

      if (id == PM.MS_PACMAN) {
        x += 4;
      }
    }
    return [y, x];
  }

  /**
   * Get a proper position of a ghost sprite depending
   * on the current state, mode, etc.
   * @return X and Y position of the sprite.
   */
  Array<int> getGhostSprite() {
    var x = 0;
    var y = 0;

    if ((PacMan.gameplayMode == PM.GAMEPLAY_LEVEL_COMPLETE_PART_2) ||
        (PacMan.gameplayMode == PM.GAMEPLAY_READY_PART_1) ||
        (PacMan.gameplayMode == PM.GAMEPLAY_PLAYER_DYING_PART_2)) {
      // Ghosts disappear when game starts, level ends, or Pac-Man is dying.
      x = 3;
      y = 0;
    } else if (PacMan.gameplayMode == PM.GAMEPLAY_GHOST_BEING_EATEN &&
        id == PacMan.ghostBeingEatenId) {
      // A ghost being eaten becomes a score indicator (200, 400, 800, 1600)
      // for a brief moment.
      switch (PacMan.modeScoreMultiplier) {
        case 2: x = 0; break;
        case 4: x = 1; break;
        case 8: x = 2; break;
        case 16: x = 3; break;
      }
      y = 11;

      // Temporary class change to make sure the score digits have higher
      // z-index to be positioned above any ghost that might occupy the same
      // tile.
      el.attributes['class'] = 'pcm-ac pcm-n';
    } else if (mode == PM.GHOST_MODE_FRIGHT ||
               ((mode == PM.GHOST_MODE_IN_PEN || mode == PM.GHOST_MODE_EXITING_PEN) &&
                (PacMan.mainGhostMode == PM.GHOST_MODE_FRIGHT) && !eatenInThisFrightMode)) {
      // Ghosts turning blue.
      x = 0;
      y = 8;

      // Ghosts blink white before the end of fright mode.
      if ((PacMan.frightModeTime <
           PacMan.levels['frightTotalTime'] - PacMan.levels['frightTime']) &&
          (Math.floor(PacMan.frightModeTime /
                      PacMan.timing[PM.TIMING_FRIGHT_BLINK]) % 2) == 0) {
        x += 2;
      }
      // This makes ghost tails wiggle.
      x += Math.floor(PacMan.globalTime / 16) % 2;
    } else if (mode == PM.GHOST_MODE_EYES ||
               mode == PM.GHOST_MODE_REENTERING_PEN) {
      // Ghosts become just eyes when traveling to the pen after being eaten.
      var dir = nextDir;
      if (!dir) {
        dir = this.dir;
      }
      switch (dir) {
        case PM.DIR_LEFT: x = 2; break;
        case PM.DIR_RIGHT: x = 3; break;
        case PM.DIR_UP: x = 0; break;
        case PM.DIR_DOWN: x = 1; break;
      }
      y = 10;
    } else if (el.id == 'pcm-ghin') {
      // Injured (repaired) ghost
      x = 6;
      y = 8;
      x += Math.floor(PacMan.globalTime / 16) % 2;
    } else if (el.id == 'pcm-gbug') {
      // Injured (repaired) ghost
      x = 6;
      y = 9;
      y += Math.floor(PacMan.globalTime / 16) % 2;
    } else if (el.id == 'pcm-ghfa') {
      // Ghost falling apart
      if (PacMan.cutsceneSequenceId == 3) {
        x = 6;
      } else {
        x = 7;
      }
      y = 11;
    } else if (el.id == 'pcm-stck') {
      // Stick
      if (PacMan.cutsceneSequenceId == 1) {
        if (PacMan.cutsceneTime > 60) {
          x = 1;
        } else if (PacMan.cutsceneTime > 45) {
          x = 2;
        } else {
          x = 3;
        }
      } else if (PacMan.cutsceneSequenceId == 2) {
        x = 3;
      } else if (PacMan.cutsceneSequenceId == 3 ||
                 PacMan.cutsceneSequenceId == 4) {
        x = 4;
      } else {
        x = 0;
      }
      y = 13;
    } else {
      // We're using next direction, not current direction, so that ghost's
      // eyes will change directions a bit before its "body" will. We make
      // an exception for the tunnel, though, so ghosts don't look off screen.
      var dir = nextDir;
      if (!dir ||
        PacMan.playfield[tilePos[0]][tilePos[1]].type ==
        PM.PATH_TUNNEL) {
        dir = dir;
      }
      switch (dir) {
        case PM.DIR_LEFT: x = 4; break;
        case PM.DIR_RIGHT: x = 6; break;
        case PM.DIR_UP: x = 0; break;
        case PM.DIR_DOWN: x = 2; break;
      }

      y = 4 + id - PacMan.playerCount;

      // This makes ghost tails wiggle.
      if ((speed > 0) || (PacMan.gameplayMode != PM.GAMEPLAY_CUTSCENE)) {
        x += Math.floor(PacMan.globalTime / 16) % 2;
      }
    }

    return [y, x];
  }

  /**
   * Update a given actor's sprite (choose a proper graphic for a given
   * position/mode/state of the actor).
   */
  void updateSprite() {
    updatePosition();

    // Position within the 16px grid of the sprite png.
    var pos = [0, 0];

    // In these modes, actors disappear...
    if ((PacMan.gameplayMode == PM.GAMEPLAY_GAMEOVER) ||
        (PacMan.gameplayMode == PM.GAMEPLAY_INFINITE_GAMEOVER)) {
      // Empty square.
      pos = [0, 3];
    } else {
      if (ghost) {
        pos = getGhostSprite();
      } else {
        pos = getPlayerSprite();
      }
    }

    // We only request change to the sprite when it's actually different
    // than last time.
    if ((elBackgroundPos[0] != pos[0]) ||
        (elBackgroundPos[1] != pos[1])) {
      elBackgroundPos[0] = pos[0];
      elBackgroundPos[1] = pos[1];

      pos[0] *= 16;
      pos[1] *= 16;

      PacMan.changeElementBkPos(el, pos[1], pos[0], true);
    }
  }
}

class PacMan {
  static int randSeed;
  static boolean ready;
  static boolean useCss;
  static boolean graphicsReady;
  static boolean soundAvailable;
  static boolean soundReady;
  static boolean hasFlash;
  static String flashVersion;
  static Element flashIframe;
  static Document flashIframeDoc;
  static Element styleElement;
  static Element playfieldEl;
  static Element canvasEl;
  static Array<double> timing;
  static Array<double> speedIntervals;
  static Array<int> oppositeDirections;
  static int fpsChoice;
  static boolean canDecreaseFps;
  static int tickTimer; 
  static int fps;
  static int tickInterval, tickMultiplier;
  static boolean pacManSound;
  static double lastTime, lastTimeDelta;
  static int lastTimeSlownessCount;
  static int playerCount;
  static int gameplayMode, gameplayModeTime;
  static Array<Actor> actors;
  static int scoreDigits;
  static Array<Element> scoreLabelEl;
  static Array<Element> scoreEl;
  static Element livesEl;
  static Element levelEl;
  static Array<int> score;
  static Array<boolean> extraLifeAwarded;
  static int lives;
  static int level;
  static boolean paused;
  static int globalTime;
  static Object levels; // TODO(jgw): Need something like a record type
  static int frightModeTime;
  static int intervalTime;
  static int fruitTime;
  static int ghostModeSwitchPos;
  static int ghostModeTime;
  static boolean ghostExitingPenNow;
  static int ghostEyesCount;
  static boolean tilesChanged;
  static boolean alternatePenLeavingScheme;
  static boolean lostLifeOnThisLevel;
  static int dotsRemaining, dotsEaten;
  static Array<Array<Tile>> playfield;
  static int playfieldWidth;
  static int playfieldHeight;
  static Element doorEl;
  static Element fruitEl;
  static int cruiseElroySpeed;
  static boolean fruitShown;
  static int forcePenLeaveTime;
  static int mainGhostMode;
  static int currentPlayerSpeed;
  static int currentDotEatingSpeed;
  static Array<int> dotEatingChannel;
  static Array<int> dotEatingSoundPart;
  static Array<boolean> dotEatingNow;
  static Array<boolean> dotEatingNext;
  static boolean userDisabledSound;
  static Element soundEl;
  static int lastMainGhostMode;
  static int playerDyingId;
  static int modeScoreMultiplier;
  static int alternateDotCount;
  static int ghostBeingEatenId;
  static int playerEatingGhostId;
  static Element cutsceneCanvasEl;
  static Array<Actor> cutsceneActors;
  static int cutsceneId;
  static Cutscene cutscene;
  static int cutsceneSequenceId;
  static int cutsceneTime;
  static int touchDX, touchDY;
  static int touchStartX, touchStartY;
  static int killScreenTileX, killScreenTileY;
  static int dotTimerMs;
  static int dotTimer;
  static Element flashSoundPlayer;

  /**
   * Send a random number from 0 to 1. The reason we're not using Math.random()
   * is that you can't seed it reliably on all the browsers -- and we need
   * random numbers to be repeatable between game plays too allow for patterns
   * etc., and also a consistent kill screen that's procedurally generated.
   * @return Random number from 0 to 1.
   */
  static double rand() {
    var t32 = 2147483647; // TODO: Was 0x100000000, but that's not an int.
    var constant = 134775813;
    var x = (constant * randSeed + 1);
    return (randSeed = x % t32) / t32;
  }

  /**
   * Seeds the random generator.
   * @param seed Seed number.
   */
  static void seed(int seed) {
    randSeed = seed;
  }

  /**
   * Calculates the difference between two positions.
   * @param firstPos First position.
   * @param secondPos Second position.
   * @return Distance.
   */
  static double getDistance(Array<int> firstPos, Array<int> secondPos) {
    return Math.sqrt((secondPos[1] - firstPos[1]) *
                     (secondPos[1] - firstPos[1]) +
                     (secondPos[0] - firstPos[0]) *
                     (secondPos[0] - firstPos[0]));
  }

  /**
   * Returns a corrected X position so that 0 is in the upper-left corner
   * of the playfield.
   * @param x X position.
   * @return Corrected X position.
   */
  static int getPlayfieldX(int x) {
    return x + PM.PLAYFIELD_OFFSET_X;
  }

  /**
   * Returns a corrected Y position so that 0 is in the upper-left corner
   * of the playfield.
   * @param y Y position.
   * @return Corrected Y position.
   */
  static int getPlayfieldY(int y) {
    return y + PM.PLAYFIELD_OFFSET_Y;
  }

  /**
   * Corrects the sprite position from a 8px grid to a 10px grid.
   * @param pos Position (x or y).
   * @return Corrected position.
   */
  static int getCorrectedSpritePos(int pos) {
    return pos / 8 * 10 + 2;
  }

  /**
   * Returns a DOM element ID for a dot (pcm-dY-X).
   * @param y Y position.
   * @param x x position.
   * @return DOM element id.
   */
  static String getDotElementId(int y, int x) {
    return 'pcm-d' + y + '-' + x;
  }

  /**
   * Shows or hides a DOM element.
   * @param id DOM element id.
   * @param show Whether to show it or not.
   */
  static void showElementById(String id, boolean show) {
    var el = document.getElementById(id);

    if (el) {
      el.style.visibility = show ? 'visible' : 'hidden';
    }
  }

  /**
   * Gets an absolute page position of a given DOM element.
   * @param el DOM element.
   * @return pos Position on the page (y and x).
   */
  static Array<int> getAbsoluteElPos(Element el) {
    var pos = [0, 0];

    do {
      pos[0] += el.offsetTop;
      pos[1] += el.offsetLeft;
    } while (el = el.offsetParent);

    return pos;
  }

  /**
   * Prepares a DOM element to serve as a sprite.
   * @param el DOM element.
   * @param x Initial X position.
   * @param y Initial Y position.
   */
  static void prepareElement(Element el, int x, int y) {
    x = getCorrectedSpritePos(parseInt(x, 10));
    y = getCorrectedSpritePos(parseInt(y, 10));

    // For all the browsers coming from a good home, we are doing it via CSS.
    if (useCss) {
      el.style.backgroundImage = 'url(pacman10-hp-sprite-2.png)';
      el.style.backgroundPosition = (-x) + 'px ' + (-y) + 'px';
      el.style.backgroundRepeat = 'no-repeat';
    } else {
      // For Internet Explorer with CSS background reloading bug, we put
      // an image inside an element, and move it around.
      el.style.overflow = 'hidden';
      var style = 'display: block; position: relative; ' +
                  'left: ' + (-x) + 'px; top: ' + (-y) + 'px';
      el.innerHTML = '<img style="' + style +
                     '" src="pacman10-hp-sprite.png">';
    }
  }

  /**
   * Changes a background of a given DOM element to show a proper sprite.
   * This is done in two different fashions depending on whether we do it
   * via CSS, or by having an image inline (for IE)
   * @param el DOM element.
   * @param x X position.
   * @param y Y position.
   * @param correction Whether to correct for the new sprite grid.
   */
  static void changeElementBkPos(Element el, int x, int y, boolean correction) {
    if (correction) {
      x = getCorrectedSpritePos(x);
      y = getCorrectedSpritePos(y);
    }

    if (useCss) {
      el.style.backgroundPosition = (-x) + 'px ' + (-y) + 'px';
    } else {
      if (el.childNodes[0]) {
        el.childNodes[0].style.left = (-x) + 'px';
        el.childNodes[0].style.top = (-y) + 'px';
      }
    }
  }

  /**
   * Determining the dimensions of the playfield based on the paths.
   */
  static void determinePlayfieldDimensions() {
    playfieldWidth = 0;
    playfieldHeight = 0;

    for (var i = 0; i < PM.PATHS.length; ++i) {
      var path = PM.PATHS[i];
      if (path.w) {
        // Horizontal path
        var curWidth = path.x + path.w - 1;
        if (curWidth > playfieldWidth) {
          playfieldWidth = curWidth;
        }
      } else {
        // Vertical path
        var curHeight = path.y + path.h - 1;
        if (curHeight > playfieldHeight) {
          playfieldHeight = curHeight;
        }
      }
    }
  }

  /**
   * Prepares the playfield by creating the necessary tile array elements.
   */
  static void preparePlayfield() {
    playfield = [ ]; // TODO: []
    for (var y = 0; y <= playfieldHeight + 1; y++) {
      playfield[y * PM.TILE_SIZE] = [ ]; // TODO: []

      // We are starting at -2 to accomodate the horizontal tunnel
      for (var x = -2; x <= playfieldWidth + 1; x++) {
        playfield[y * PM.TILE_SIZE][x * PM.TILE_SIZE] = new Tile(0, PM.DOT_TYPE_NONE, 0);
      }
    }
  }

  /**
   * Converts all the playfield paths into separate tiles, figures out where
   * the intersections are, etc.
   */
  static void preparePaths() {
    for (var i = 0; i < PM.PATHS.length; ++i) {
      var path = PM.PATHS[i];
      var type = path.type;

      if (path.w) {
        var y = path.y * PM.TILE_SIZE;

        for (var x = path.x * PM.TILE_SIZE;
             x <= (path.x + path.w - 1) * PM.TILE_SIZE; x += PM.TILE_SIZE) {
          playfield[y][x].path = true;
          // Check for dots, which are initialized in preparePlayfield.
          if (playfield[y][x].dot == PM.DOT_TYPE_NONE) {
            playfield[y][x].dot = PM.DOT_TYPE_DOT;
            dotsRemaining = dotsRemaining + 1; // TODO(jgw): ++
          }
          if (!type || (x != (path.x * PM.TILE_SIZE) &&
              x != ((path.x + path.w - 1) * PM.TILE_SIZE))) {
            playfield[y][x].type = type;
          } else {
            playfield[y][x].type = PM.PATH_NORMAL;
          }
        }
        playfield[y][path.x * PM.TILE_SIZE].intersection = true;
        playfield[y][(path.x + path.w - 1) * PM.TILE_SIZE].intersection = true;
      } else {
        var x = path.x * PM.TILE_SIZE;

        for (var y = path.y * PM.TILE_SIZE;
             y <= (path.y + path.h - 1) * PM.TILE_SIZE; y += PM.TILE_SIZE) {
          if (playfield[y][x].path) {
            playfield[y][x].intersection = true;
          }
          playfield[y][x].path = true;
          if (playfield[y][x].dot == PM.DOT_TYPE_NONE) {
            playfield[y][x].dot = PM.DOT_TYPE_DOT;
            dotsRemaining = dotsRemaining + 1; // TODO(jgw): ++
          }
          if (!type || (y != (path.y * PM.TILE_SIZE) &&
              y != ((path.y + path.h - 1) * PM.TILE_SIZE))) {
            playfield[y][x].type = type;
          } else {
            playfield[y][x].type = PM.PATH_NORMAL;
          }
        }
        playfield[path.y * PM.TILE_SIZE][x].intersection = true;
        playfield[(path.y + path.h - 1) * PM.TILE_SIZE][x].intersection = true;
      }
    }

    for (var i = 0; i < PM.NO_DOT_PATHS.length; ++i) {
      if (PM.NO_DOT_PATHS[i].w) {
        for (var x = PM.NO_DOT_PATHS[i].x * PM.TILE_SIZE;
             x <= (PM.NO_DOT_PATHS[i].x + PM.NO_DOT_PATHS[i].w - 1) *
                   PM.TILE_SIZE; x += PM.TILE_SIZE) {
          playfield[PM.NO_DOT_PATHS[i].y * PM.TILE_SIZE][x].dot =
              PM.DOT_TYPE_NONE;
          dotsRemaining = dotsRemaining - 1; // TODO(jgw): --
        }
      } else {
        for (var y = PM.NO_DOT_PATHS[i].y * PM.TILE_SIZE;
             y <= (PM.NO_DOT_PATHS[i].y + PM.NO_DOT_PATHS[i].h - 1) *
                   PM.TILE_SIZE; y += PM.TILE_SIZE) {
          playfield[y][PM.NO_DOT_PATHS[i].x * PM.TILE_SIZE].dot = PM.DOT_TYPE_NONE;
          dotsRemaining = dotsRemaining - 1; // TODO(jgw): --
        }
      }
    }
  }

  /**
   * Goes through all the paths and figures out which directions are available
   * at each tile.
   */
  static void prepareAllowedDirections() {
    for (var y = 1 * PM.TILE_SIZE; y <= playfieldHeight * PM.TILE_SIZE; y += PM.TILE_SIZE) {
      for (var x = 1 * PM.TILE_SIZE; x <= playfieldWidth * PM.TILE_SIZE; x += PM.TILE_SIZE) {
        playfield[y][x].allowedDir = 0;

        if (playfield[y - PM.TILE_SIZE][x].path) {
          playfield[y][x].allowedDir += PM.DIR_UP;
        }
        if (playfield[y + PM.TILE_SIZE][x].path) {
          playfield[y][x].allowedDir += PM.DIR_DOWN;
        }
        if (playfield[y][x - PM.TILE_SIZE].path) {
          playfield[y][x].allowedDir += PM.DIR_LEFT;
        }
        if (playfield[y][x + PM.TILE_SIZE].path) {
          playfield[y][x].allowedDir += PM.DIR_RIGHT;
        }
      }
    }
  }

  /**
   * Creates DOM elements for dots.
   */
  static void createDotElements() {
    for (var y = 1 * PM.TILE_SIZE; y <= playfieldHeight * PM.TILE_SIZE; y += PM.TILE_SIZE) {
      for (var x = 1 * PM.TILE_SIZE; x <= playfieldWidth * PM.TILE_SIZE; x += PM.TILE_SIZE) {
        if (playfield[y][x].dot) {
          var el = document.createElement('div');
          el.attributes['class'] = 'pcm-d';

          el.id = getDotElementId(y, x);

          el.style.left = (x + PM.PLAYFIELD_OFFSET_X) + 'px';
          el.style.top = (y + PM.PLAYFIELD_OFFSET_Y) + 'px';

          playfieldEl.appendChild(el);
        }
      }
    }
  }

  /**
   * Changes selected dot DOM elements to energizers (big dots).
   */
  static void createEnergizerElements() {
    for (var i = 0; i < PM.ENERGIZERS.length; ++i) {
      var energizer = PM.ENERGIZERS[i];

      var id = getDotElementId(energizer.y * PM.TILE_SIZE, energizer.x * PM.TILE_SIZE);
      document.getElementById(id).attributes['class'] = 'pcm-e';
      prepareElement(document.getElementById(id), 0, 144);

      playfield[energizer.y * PM.TILE_SIZE][energizer.x * PM.TILE_SIZE].dot = PM.DOT_TYPE_ENERGIZER;
    }
  }

  /**
   * Creates a fruit DOM element. The element is always there, it's just most
   * of the time it has transparent background and nothing else.
   */
  static void createFruitElement () {
    fruitEl = document.createElement('div');
    fruitEl.id = 'pcm-f';
    fruitEl.style.left = (getPlayfieldX(PM.FRUIT_POS[1])) + 'px';
    fruitEl.style.top = (getPlayfieldY(PM.FRUIT_POS[0])) + 'px';
    prepareElement(fruitEl, -32, -16);

    playfieldEl.appendChild(fruitEl);
  }

  /**
   * Create all "edible" playfield DOM elements: dots, energizers, fruit.
   */
  static void createPlayfieldElements() {
    // Door to the pen
    doorEl = document.createElement('div');
    doorEl.id = 'pcm-do';
    doorEl.style.display = 'none';
    playfieldEl.appendChild(doorEl);

    createDotElements();
    createEnergizerElements();
    createFruitElement();
  }

  /**
   * Create objects for all the actors.
   */
  static void createActors() {
    actors = [ ]; // TODO: []
    for (var id = 0; id < playerCount + PM.GHOST_ACTOR_COUNT; id++) {
      actors[id] = new Actor(id);

      if (id < playerCount) {
        // Player
        actors[id].ghost = false;
        actors[id].mode = PM.PLAYER_MODE_MOVING;
      } else {
        // Non-player character, or... ghost
        actors[id].ghost = true;
      }
    }
  }

  /**
   * Restarts all the actors.
   */
  static void restartActors() {
    for (var id = 0; id < actors.length; ++id) {
      actors[id].restart();
    }
  }

  /**
   * Creates DOM elements for all the actors (Pac-Man, Ms. Pac-Man, ghosts).
   */
  static void createActorElements() {
    for (var id = 0; id < actors.length; ++id) {
      actors[id].createElement();
    }
  }

  /**
   * Creates the DOM element for the playfield.
   */
  static void createPlayfield() {
    playfieldEl = document.createElement('div');
    playfieldEl.id = 'pcm-p';
    canvasEl.appendChild(playfieldEl);
  }

  /**
   * Resets the playfield in preparations for the new level.
   */
  static void resetPlayfield() {
    dotsRemaining = 0;
    dotsEaten = 0;

    playfieldEl.innerHTML = '';
    prepareElement(playfieldEl, 256, 0);

    determinePlayfieldDimensions();
    preparePlayfield();
    preparePaths();
    prepareAllowedDirections();
    createPlayfieldElements();
    createActorElements();
  }

  /**
   * Reacts to a key being pressed.
   * @param keyCode Keyboard code for the key.
   * @return Whether the key was processed and the natural browser
   *           response to that key should be supressed.
   */
  static void keyPressed(int keyCode) {
    var processed = false;

    switch (keyCode) {
      case PM.KEYCODE_LEFT:
        actors[PM.PACMAN].requestedDir = PM.DIR_LEFT;
        processed = true;
        break;
      case PM.KEYCODE_UP:
        actors[PM.PACMAN].requestedDir = PM.DIR_UP;
        processed = true;
        break;
      case PM.KEYCODE_RIGHT:
        actors[PM.PACMAN].requestedDir = PM.DIR_RIGHT;
        processed = true;
        break;
      case PM.KEYCODE_DOWN:
        actors[PM.PACMAN].requestedDir = PM.DIR_DOWN;
        processed = true;
        break;
      case PM.KEYCODE_A:
        if (playerCount == 2) {
          actors[PM.MS_PACMAN].requestedDir = PM.DIR_LEFT;
          processed = true;
        }
        break;
      case PM.KEYCODE_S:
        if (playerCount == 2) {
          actors[PM.MS_PACMAN].requestedDir = PM.DIR_DOWN;
          processed = true;
        }
        break;
      case PM.KEYCODE_D:
        if (playerCount == 2) {
          actors[PM.MS_PACMAN].requestedDir = PM.DIR_RIGHT;
          processed = true;
        }
        break;
      case PM.KEYCODE_W:
        if (playerCount == 2) {
          actors[PM.MS_PACMAN].requestedDir = PM.DIR_UP;
          processed = true;
        }
        break;
    }
    return processed;
  }

  /**
   * An event handle for a key down event.
   * @param event Keyboard event.
   */
  static void handleKeyDown(Event event) {
    // TODO(jgw): IE hack
    /*
    if (!event) {
      var event = window.event;
    }
    */

    if (keyPressed(event.keyCode)) {
      if (event.preventDefault) {
        event.preventDefault();
      } else {
        event.returnValue = false;
      }
    }
  }

  /**
   * Reacts to a click on a canvas. We support navigating Pac-Man by clicking
   * on which direction it should go.
   * @param x Horizontal position on the page.
   * @param y Vertical position on the page.
   */
  static void canvasClicked(int x, int y) {
    // Normalizing the click position
    var pos = getAbsoluteElPos(canvasEl);

    x -= pos[1] - PM.PLAYFIELD_OFFSET_X;
    y -= pos[0] - PM.PLAYFIELD_OFFSET_Y;

    // Pac-Man position
    var player = actors[0];
    var playerX = getPlayfieldX(player.pos[1] + player.posDelta[1]) + PM.TILE_SIZE * 2;
    var playerY = getPlayfieldY(player.pos[0] + player.posDelta[0]) + PM.TILE_SIZE * 4;

    var dx = Math.abs(x - playerX);
    var dy = Math.abs(y - playerY);

    if ((dx > PM.CLICK_SENSITIVITY) && (dy < dx)) {
      if (x > playerX) {
        player.requestedDir = PM.DIR_RIGHT;
      } else {
        player.requestedDir = PM.DIR_LEFT;
      }
    } else if ((dy > PM.CLICK_SENSITIVITY) && (dx < dy)) {
      if (y > playerY) {
        player.requestedDir = PM.DIR_DOWN;
      } else {
        player.requestedDir = PM.DIR_UP;
      }
    }
  }

  /**
   * An event handle for a click event.
   * @param event Mouse event.
   */
  static void handleClick(Event event) {
    if (!event) {
      var event = window.event;
    }

    canvasClicked(event.clientX, event.clientY);
  }

  /**
   * Add handlers for touch events. We support swiping to move on machines
   * that have that capability.
   */
  static void registerTouch() {
    document.body.addEventListener('touchstart', function(e) { handleTouchStart(e); }, true);
    canvasEl.addEventListener('touchstart', function(e) { handleTouchStart(e); }, true);
    /* TODO(jgw): What the hell is this?
    if (document.f && document.f.q) {
      document.f.q.addEventListener('touchstart', handleTouchStart, true);
    }
    */
  }

  /**
   * Handle touch start event.
   * @param event Browser event.
   */
  static void handleTouchStart(Event event) {
    // The touch event is added to body, so after the game starts, it is not
    // possible to swipe page by accident, pinch to zoom, etc.

    touchDX = 0;
    touchDY = 0;

    // Only single touch at this moment.
    if (event.touches.length == 1) {
      touchStartX = event.touches[0].pageX;
      touchStartY = event.touches[0].pageY;

      document.body.addEventListener('touchmove', function(e) { handleTouchMove(e); }, true);
      document.body.addEventListener('touchend', function(e) { handleTouchEnd(e); }, true);
    }
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Handle touch move event.
   * @param event Browser event.
   */
  static void handleTouchMove(Event event) {
    if (event.touches.length > 1) {
      cancelTouch();
    } else {
      touchDX = event.touches[0].pageX - touchStartX;
      touchDY = event.touches[0].pageY - touchStartY;
    }
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Handle touch end event.
   * @param event Browser event.
   */
  static void handleTouchEnd(Event event) {
    // Regular tap is interpreted as a click
    if ((touchDX == 0) && (touchDY == 0)) {
      canvasClicked(touchStartX, touchStartY);
    } else {
      var dx = Math.abs(touchDX);
      var dy = Math.abs(touchDY);

      // A very short swipe is interpreted as click/tap
      if ((dx < PM.TOUCH_CLICK_SENSITIVITY) &&
          (dy < PM.TOUCH_CLICK_SENSITIVITY)) {
        canvasClicked(touchStartX, touchStartY);
      } else if ((dx > PM.TOUCH_SENSITIVITY) && (dy < (dx * 2 / 3))) {
        // Horizontal swipe
        if (touchDX > 0) {
          actors[0].requestedDir = PM.DIR_RIGHT;
        } else {
          actors[0].requestedDir = PM.DIR_LEFT;
        }
      } else if ((dy > PM.TOUCH_SENSITIVITY) && (dx < (dy * 2 / 3))) {
        // Vertical swipe
        if (touchDY > 0) {
          actors[0].requestedDir = PM.DIR_DOWN;
        } else {
          actors[0].requestedDir = PM.DIR_UP;
        }
      }
    }

    event.preventDefault();
    event.stopPropagation();
    cancelTouch();
  }

  /**
   * Finish with this touch gesture, remove handlers.
   */
  static void cancelTouch() {
    document.body.removeEventListener('touchmove', function() { handleTouchMove(); }, true);
    document.body.removeEventListener('touchend', function() { handleTouchEnd(); }, true);
    touchStartX = null;
    touchStartY = null;
  }

  /**
   * Adds necessary event listeners (keyboard, touch events).
   */
  static void addEventListeners() {
    /* TODO(jgw):
    if (window.addEventListener) {
      window.addEventListener('keydown', handleKeyDown, false);
      canvasEl.addEventListener('click', handleClick, false);
      registerTouch();
    } else {
      document.body.attachEvent('onkeydown', handleKeyDown);
      canvasEl.attachEvent('onclick', handleClick);
    }
    */
    window.addEventListener('keydown', function(e) { handleKeyDown(e); }, false);
    canvasEl.addEventListener('click', function(e) { handleClick(e); }, false);
    registerTouch();
  }

  /**
   * Starts the gameplay. Performs all the functions that are necessary to
   * be done only once (clearing scores, setting life counter, etc.)
   */
  static void startGameplay() {
    score = [0, 0];
    extraLifeAwarded = [false, false];
    lives = 3;
    level = 0;
    paused = false;
    globalTime = 0;
    newLevel(true);
  }

  /**
   * Restarts the gameplay following new level or the loss of life.
   * @param firstTime Whether this is the first game or not (first
   *          game = longer READY with music).
   */
  static void restartGameplay(firstTime) {
    // Seeding the random number generator to a constant value. This allows
    // for patterns -- ghosts always move the same way if the player does too.
    seed(0);

    // Resetting the timers.
    frightModeTime = 0;
    intervalTime = 0;
    gameplayModeTime = 0;
    fruitTime = 0;

    ghostModeSwitchPos = 0;
    ghostModeTime = levels['ghostModeSwitchTimes'][0] * PM.TARGET_FPS;

    // During the initial exiting of the pen, ghosts should come out one by
    // one. (This is not required if they do it later.)
    ghostExitingPenNow = false;

    // How many ghost eyes are floating around.
    ghostEyesCount = 0;

    // If any of the actors changed their current tile, we need to do some
    // things like check for collisions, etc. We do it only then to speed up
    // the game.
    tilesChanged = false;

    updateCruiseElroySpeed();
    hideFruit();
    resetForcePenLeaveTime();
    restartActors();
    updateActorPositions();

    switchMainGhostMode(PM.GHOST_MODE_SCATTER, true);
    // Everyone except Blinky starts in a pen.
    for (var id = playerCount + 1; id < playerCount + PM.GHOST_ACTOR_COUNT; id++) {
      actors[id].changeMode(PM.GHOST_MODE_IN_PEN);
    }

    dotEatingChannel = [0, 0];
    dotEatingSoundPart = [1, 1];
    clearDotEatingNow();

    if (firstTime) {
      changeGameplayMode(PM.GAMEPLAY_READY_PART_1);
    } else {
      changeGameplayMode(PM.GAMEPLAY_FAST_READY_PART_1);
    }
  }

  /**
   * Switch to double mode, where Ms. Pac-Man plays alongside Pac-Man.
   * This initiates a little pause before jumping in.
   */
  static void initiateDoubleMode() {
    if (playerCount != 2) {
      stopAllAudio();
      changeGameplayMode(PM.GAMEPLAY_DOUBLE_MODE_SWITCH);
    }
  }

  /**
   * Initiates a new game.
   */
  static void newGame() {
    playerCount = 1;

    createChrome();
    createPlayfield();
    createActors();

    startGameplay();
  }

  /**
   * Second part of switch to double mode.
   */
  static void switchToDoubleMode() {
    playerCount = 2;
    createChrome();
    createPlayfield();
    createActors();
    startGameplay();
  }

  /**
   * React to the user pressing Insert Coin button. This initiates double
   * mode during regular game play, or restarts the game again during game
   * over screen.
   */
  static void insertCoin() {
    if (gameplayMode == PM.GAMEPLAY_GAMEOVER ||
        gameplayMode == PM.GAMEPLAY_INFINITE_GAMEOVER) {
      newGame();
    } else {
      initiateDoubleMode();
    }
  }

  /**
   * Creates a little kill screen element.
   * @param x Horizontal position.
   * @param y Vertical position.
   * @param w Width.
   * @param h Height.
   * @param image Whether to show an image or make it black.
   */
  static void createKillScreenElement(int x, int y, int w, int h, boolean image) {
    var el = document.createElement('div');
    el.style.left = x + 'px';
    el.style.top = y + 'px';
    el.style.width = w + 'px';
    el.style.height = h + 'px';
    el.style.zIndex = 119;
    if (image) {
      el.style.background = 'url(pacman10-hp-sprite.png) -' +
          killScreenTileX + 'px -' +
          killScreenTileY + 'px no-repeat';
      killScreenTileY += 8;
    } else {
      el.style.background = 'black';
    }
    playfieldEl.appendChild(el);
  }

  /**
   * Show the "kill screen" instead of level 256. The original Pac-Man had
   * a bug that corrupted that level, making it look funny and impossible to
   * finish. We're trying to procedurally generate a similarly looking level,
   * although we're not making it playable.
   */
  static void killScreen() {
    // Makes sure it always looks the same.
    seed(0);

    canvasEl.style.visibility = '';

    // Covering the right-hand side of the playfield.
    createKillScreenElement(272, 0, 200, 80, false);
    createKillScreenElement(272 + 8, 80, 200 - 8, 136 - 80, false);

    // Creating little tiles with gibberish.
    killScreenTileX = 80;
    killScreenTileY = 0;
    for (var x = 280; x <= 472; x += 8) {
      for (var y = 0; y <= 136; y += 8) {
        if (rand() < 0.03) {
          killScreenTileX = Math.floor(rand() * 25) * 10;
          killScreenTileY = Math.floor(rand() * 2) * 10;
        }

        createKillScreenElement(x, y, 8, 8, true);
      }
    }

    changeGameplayMode(PM.GAMEPLAY_INFINITE_GAMEOVER);
  }

  /**
   * Start a new level.
   * @param firstTime Whether this is happening for the first time.
   */
  static void newLevel(boolean firstTime) {
    level = level + 1; // TODO(jgw); ++ broken here

    // Every level above 21 looks like level 21.
    if (level >= PM.LEVELS.length) {
      levels = PM.LEVELS[PM.LEVELS.length - 1];
    } else {
      levels = PM.LEVELS[level];
    }

    // Calculate proper length of fright time based on level info.
    levels['frightTime'] = Math.round(levels['frightTime'] * PM.TARGET_FPS);
    levels['frightTotalTime'] =
        levels['frightTime'] +
        timing[PM.TIMING_FRIGHT_BLINK] *
        ((levels['frightBlinkCount'] * 2) - 1);

    for (var i = 0; i < actors.length; ++i) {
      actors[i].dotCount = 0;
    }

    alternatePenLeavingScheme = false;
    lostLifeOnThisLevel = false;

    updateChrome();
    resetPlayfield();

    restartGameplay(firstTime);

    // You can never advance past level 255. Level 256 is a "kill screen"
    // and the game ends there.
    if (level == 256) {
      killScreen();
    }
  }

  /**
   * New life starts for Pac-Man. If there aren't enough lives left, game
   * over!
   */
  static void newLife() {
    // Some ghost behaviour differs when Pac-Man lost a life on a given
    // level.
    lostLifeOnThisLevel = true;

    alternatePenLeavingScheme = true;
    alternateDotCount = 0;

    lives = lives - 1; // TODO(jgw): --;
    updateChromeLives();
    if (lives == -1) {
      changeGameplayMode(PM.GAMEPLAY_GAMEOVER);
    } else {
      restartGameplay(false);
    }
  }

  /**
   * Switches the main ghost mode.
   *
   * In normal gameplay, the modes alternate between scatter (ghosts just
   * roam around) and chase (ghosts chase Pac-Man). When Pac-Man eats an
   * energizer, the main ghost mode switches to fright.
   *
   * In addition to that, ghosts have their individual modes too.
   *
   * @param newMode New main ghost mode.
   * @param initial Whether this is an initial invokation of this
   *          during level start.
   */
  static void switchMainGhostMode(int newMode, boolean initial) {
    // On further levels, fright mode doesn't happen at all. The ghosts just
    // reverse directions.
    if (newMode == PM.GHOST_MODE_FRIGHT && levels['frightTime'] == 0) {
      for (var i = 0; i < actors.length; ++i) {
        var actor = actors[i];
        if (actor.ghost) {
          actor.reverseDirectionsNext = true;
        }
      }
      return;
    }

    var oldMode = mainGhostMode;

    // Remember the previous mode that came before fright mode.
    if (newMode == PM.GHOST_MODE_FRIGHT &&
        mainGhostMode != PM.GHOST_MODE_FRIGHT) {
      lastMainGhostMode = mainGhostMode;
    }
    mainGhostMode = newMode;

    if (newMode == PM.GHOST_MODE_FRIGHT || oldMode == PM.GHOST_MODE_FRIGHT) {
      playAmbientSound();
    }

    switch (newMode) {
      case PM.GHOST_MODE_CHASE:
      case PM.GHOST_MODE_SCATTER:
        currentPlayerSpeed = levels['playerSpeed'] * PM.MASTER_SPEED;
        currentDotEatingSpeed = levels['dotEatingSpeed'] * PM.MASTER_SPEED;
        break;
      case PM.GHOST_MODE_FRIGHT:
        currentPlayerSpeed = levels['playerFrightSpeed'] * PM.MASTER_SPEED;
        currentDotEatingSpeed = levels['dotEatingFrightSpeed'] * PM.MASTER_SPEED;
        frightModeTime = levels['frightTotalTime'];
        modeScoreMultiplier = 1;
        break;
    }

    for (var i = 0; i < actors.length; ++i) {
      var actor = actors[i];
      if (actor.ghost) {
        // For each ghost, we remember whether a master mode change happened
        // while it was in pen. Its direction when exiting a pen will depend
        // on that.
        if (newMode != PM.GHOST_MODE_REENTERING_PEN && !initial) {
          actor.modeChangedWhileInPen = true;
        }

        // We need to remember whether a ghost has been eaten in a given fright
        // mode, so that it exits in its normal state, even if everyone else
        // is still blue.
        if (newMode == PM.GHOST_MODE_FRIGHT) {
          actor.eatenInThisFrightMode = false;
        }

        if ((actor.mode != PM.GHOST_MODE_EYES &&
             actor.mode != PM.GHOST_MODE_IN_PEN &&
             actor.mode != PM.GHOST_MODE_EXITING_PEN &&
             actor.mode != PM.GHOST_MODE_REEXITING_PEN &&
             actor.mode != PM.GHOST_MODE_REENTERING_PEN) || initial) {
          // Ghosts are forced to reverse direction whenever they change modes
          // (except when the fright mode ends).
          if (!initial && actor.mode != PM.GHOST_MODE_FRIGHT &&
              actor.mode != newMode) {
            actor.reverseDirectionsNext = true;
          }
          actor.changeMode(newMode);
        }
      } else {
        actor.fullSpeed = currentPlayerSpeed;
        actor['dotEatingSpeed'] = currentDotEatingSpeed;
        actor.tunnelSpeed = currentPlayerSpeed;
        actor.updatePhysicalSpeed();
      }
    }
  }

  /**
   * Check if some of the ghosts can now exit the pen.
   */
  static void figureOutPenLeaving() {
    // In the alternate pen leaving scheme (after a player died on a given
    // level) the ghosts exit after Pac-Man ate specificically numbered dots.
    if (alternatePenLeavingScheme) {
      alternateDotCount = alternateDotCount + 1; // TODO(jgw): ++

      switch (alternateDotCount) {
        case PM.ALTERNATE_DOT_COUNT[1]:
          actors[playerCount + 1].freeToLeavePen = true;
          break;
        case PM.ALTERNATE_DOT_COUNT[2]:
          actors[playerCount + 2].freeToLeavePen = true;
          break;
        case PM.ALTERNATE_DOT_COUNT[3]:
          if (actors[playerCount + 3].mode == PM.GHOST_MODE_IN_PEN) {
            alternatePenLeavingScheme = false;
          }
          break;
      }
    } else {
      // In the normal pen leaving scheme, each ghost has an individual counter
      // and leaves after it exceeds a given value. We advance the counter only
      // for the first ghost in the pen (Inky >> Pinky >> Clyde).
      if (actors[playerCount + 1].mode == PM.GHOST_MODE_IN_PEN ||
          actors[playerCount + 1].mode == PM.GHOST_MODE_EYES) {
        // TODO(jgw): ++
        actors[playerCount + 1].dotCount = actors[playerCount + 1].dotCount + 1;

        if (actors[playerCount + 1].dotCount >=
            levels['penLeavingLimits'][1]) {
          actors[playerCount + 1].freeToLeavePen = true;
        }
      } else if (actors[playerCount + 2].mode == PM.GHOST_MODE_IN_PEN ||
                 actors[playerCount + 2].mode == PM.GHOST_MODE_EYES) {
        // TODO(jgw): ++
        actors[playerCount + 2].dotCount = actors[playerCount + 2].dotCount + 1;

        if (actors[playerCount + 2].dotCount >= levels['penLeavingLimits'][2]) {
          actors[playerCount + 2].freeToLeavePen = true;
        }
      } else if (actors[playerCount + 3].mode == PM.GHOST_MODE_IN_PEN ||
                 actors[playerCount + 3].mode == PM.GHOST_MODE_EYES) {
        // TODO(jgw): ++
        actors[playerCount + 3].dotCount = actors[playerCount + 3].dotCount + 1;

        if (actors[playerCount + 3].dotCount >= levels['penLeavingLimits'][3]) {
          actors[playerCount + 3].freeToLeavePen = true;
        }
      }
    }
  }

  /**
   * Resets the force pen leave timer to its default value. The timer
   * forces the ghosts out after
   * a while when Pac-Man is not eating anything.
   */
  static void resetForcePenLeaveTime() {
    forcePenLeaveTime = levels['penForceTime'] * PM.TARGET_FPS;
  }

  /**
   * Reacts to Pac-Man or Ms. Pac-Man eating a dot
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   * @param pos Tile position.
   */
  static void dotEaten(int playerId, Array<int> pos) {
    dotsRemaining = dotsRemaining - 1; // TODO(jgw): --
    dotsEaten = dotsEaten + 1; // TODO(jgw): ++

    actors[playerId].changeCurrentSpeed(PM.SPEED_DOT_EATING);
    playDotEatingSound(playerId);

    if (playfield[pos[0]][pos[1]].dot == PM.DOT_TYPE_ENERGIZER) {
      switchMainGhostMode(PM.GHOST_MODE_FRIGHT, false);
      addToScore(PM.SCORE_ENERGIZER, playerId);
    } else {
      addToScore(PM.SCORE_DOT, playerId);
    }

    // Hide the dot on the screen and in our array.
    var el = document.getElementById(getDotElementId(pos[0], pos[1]));
    el.style.display = 'none';
    playfield[pos[0]][pos[1]].dot = PM.DOT_TYPE_NONE;

    // Cruise Elroy speed depends on how many dots have been eaten, so we
    // need to update that.
    updateCruiseElroySpeed();
    // This timer forces ghosts out of the pen if Pac-Man is not eating dots...
    // so we need to reset it now.
    resetForcePenLeaveTime();
    figureOutPenLeaving();

    // Showing the fruit at 70 and 170 dots eaten.
    if (dotsEaten == PM.FRUIT_DOTS_TRIGGER_1 ||
        dotsEaten == PM.FRUIT_DOTS_TRIGGER_2) {
      showFruit();
    }

    if (dotsRemaining == 0) {
      finishLevel();
    }

    // Ambient sound depends on the number of dots eaten
    playAmbientSound();
  }

  /**
   * Get the sprite position of a given fruit.
   * @param fruitId Fruit id (1 to 8).
   * @return X and Y position of the sprite.
   */
  static Array<int> getFruitSprite(int fruitId) {
    if (fruitId <= 4) {
      var x = 128;
    } else {
      var x = 160;
    }

    var y = 128 + 16 * ((fruitId - 1) % 4);

    return [x, y];
  }

  /**
   * Get the sprite position for a score of a given fruit (appears for a brief
   * moment after the player eats a fruit).
   * @param fruitId Fruit id (1 to 8).
   * @return X and Y position of the sprite.
   */
  static Array<int> getFruitScoreSprite(int fruitId) {
    var x = 128;
    var y = 16 * (fruitId - 1);

    return [x, y];
  }

  /**
   * Hides the fruit.
   */
  static void hideFruit() {
    fruitShown = false;

    changeElementBkPos(fruitEl, 32, 16, true);
  }

  /**
   * Shows the fruit.
   */
  static void showFruit() {
    fruitShown = true;

    var pos = getFruitSprite(levels['fruit']);
    changeElementBkPos(fruitEl, pos[0], pos[1], true);

    // Randomize fruit time between PM.TIMING_FRUIT_MIN (9 secs)
    // and PM.TIMING_FRUIT_MAX (10 secs)
    fruitTime = timing[PM.TIMING_FRUIT_MIN] +
        ((timing[PM.TIMING_FRUIT_MAX] -
          timing[PM.TIMING_FRUIT_MIN]) * rand());
  }

  /**
   * Fruit gets eaten. A sound is played, the fruit gets replaced with score
   * visual (for a brief time), and the score is increased.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void eatFruit(int playerId) {
    if (fruitShown) {
      playSound(PM.SOUND_FRUIT, PM.CHANNEL_AUX);

      fruitShown = false;
      var pos = getFruitScoreSprite(levels['fruit']);
      changeElementBkPos(fruitEl, pos[0], pos[1], true);
      fruitTime = timing[PM.TIMING_FRUIT_DECAY];

      addToScore(levels['fruitScore'], playerId);
    }
  }

  /**
   * Update target positions for all the ghosts.
   */
  static void updateActorTargetPositions() {
    for (var id = playerCount; id < playerCount + PM.GHOST_ACTOR_COUNT; id++) {
      actors[id].updateTargetPosition();
    }
  }

  /**
   * Move all the actors.
   */
  static void moveActors() {
    for (var id = 0; id < actors.length; ++id) {
      actors[id].move();
    }
  }

  /**
   * Ghost is being eaten by Pac-Man.
   * @param ghostId Ghost id.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void ghostDies(int ghostId, int playerId) {
    playSound(PM.SOUND_EATING_GHOST, PM.CHANNEL_AUX);

    addToScore(PM.SCORE_GHOST * modeScoreMultiplier, playerId);
    modeScoreMultiplier *= 2;

    ghostBeingEatenId = ghostId;
    playerEatingGhostId = playerId;
    changeGameplayMode(PM.GAMEPLAY_GHOST_BEING_EATEN);
  }

  /**
   * Ghost eats Pac-Man.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void playerDies(int playerId) {
    playerDyingId = playerId;
    changeGameplayMode(PM.GAMEPLAY_PLAYER_DYING_PART_1);
  }

  /**
   * Detect possible collisions, e.g. Pac-Man or Ms. Pac-Man occupying the same
   * tile as one of the ghosts.
   */
  static void detectCollisions() {
    tilesChanged = false;

    for (var i = playerCount; i < playerCount + PM.GHOST_ACTOR_COUNT; i++) {
      for (var j = 0; j < playerCount; j++) {
        if (actors[i].tilePos[0] == actors[j].tilePos[0] &&
            actors[i].tilePos[1] == actors[j].tilePos[1]) {

          // If the ghost is blue, Pac-Man eats the ghost...
          if (actors[i].mode == PM.GHOST_MODE_FRIGHT) {
            ghostDies(i, j);

            // return here prevents from two ghosts being eaten at the same time
            return;
          } else {
            // ...otherwise, the ghost kills Pac-Man
            if (actors[i].mode != PM.GHOST_MODE_EYES &&
                actors[i].mode != PM.GHOST_MODE_IN_PEN &&
                actors[i].mode != PM.GHOST_MODE_EXITING_PEN &&
                actors[i].mode != PM.GHOST_MODE_REEXITING_PEN &&
                actors[i].mode != PM.GHOST_MODE_REENTERING_PEN) {
              playerDies(j);
            }
          }
        }
      }
    }
  }

  /**
   * Update Cruise Elroy speed. Under some circumstances (e.g. not many dots
   * left remaining), Blinky becomes Cruise Elroy and its speed increases.
   */
  static void updateCruiseElroySpeed() {
    var newCruiseElroySpeed = levels['ghostSpeed'] * PM.MASTER_SPEED;

    if (!lostLifeOnThisLevel ||
        actors[playerCount + 3].mode != PM.GHOST_MODE_IN_PEN) {
      var levels = PacMan.levels;
      if (dotsRemaining < levels['elroyDotsLeftPart2']) {
        newCruiseElroySpeed = levels['elroySpeedPart2'] * PM.MASTER_SPEED;
      } else if (dotsRemaining < levels['elroyDotsLeftPart1']) {
        newCruiseElroySpeed = levels['elroySpeedPart1'] * PM.MASTER_SPEED;
      }
    }
    if (newCruiseElroySpeed != cruiseElroySpeed) {
      cruiseElroySpeed = newCruiseElroySpeed;
      actors[playerCount].updatePhysicalSpeed();
    }
  }

  /**
   * Gets an interval table for a given speed. Creates a new table and caches
   * it for future use.
   * @param speed Actor speed.
   * @return Array of speed intervals.
   */
  static Array<int> getSpeedIntervals(int speed) {
    if (speedIntervals[speed]) {
      return speedIntervals[speed];
    } else {
      var dist = 0;
      var lastDist = 0;

      speedIntervals[speed] = [ ]; // TODO: []
      for (var i = 0; i < PM.TARGET_FPS; i++) {
        dist += speed;
        if (Math.floor(dist) > lastDist) {
          speedIntervals[speed].add(true);
          lastDist = Math.floor(dist);
        } else {
          speedIntervals[speed].add(false);
        }
      }

      return speedIntervals[speed];
    }
  }

  /**
   * The level is completed; all the dots have been eaten. We go into the
   * short finish level animation; playfield blinks white for a couple
   * of times. Then we go into a cutscene or straight to the next level.
   */
  static void finishLevel() {
    changeGameplayMode(PM.GAMEPLAY_LEVEL_COMPLETE_PART_1);
  }

  /**
   * Change the main gameplay mode (game, cutscene, ready, game over, etc.)
   * @param mode New Mode (PM.GAMEPLAY_*).
   */
  static void changeGameplayMode(int mode) {
    gameplayMode = mode;

    if (mode != PM.GAMEPLAY_CUTSCENE) {
      for (var i = 0; i < playerCount + PM.GHOST_ACTOR_COUNT; i++) {
        actors[i].updateSprite();
      }
    }

    switch (mode) {
      case PM.GAMEPLAY_GAME_IN_PROGRESS:
        playAmbientSound();
        break;

      case PM.GAMEPLAY_PLAYER_DYING_PART_1:
        stopAllAudio();
        gameplayModeTime = timing[PM.TIMING_PLAYER_DYING_PART_1];
        break;

      case PM.GAMEPLAY_PLAYER_DYING_PART_2:
        if (playerDyingId == 0) {
          playSound(PM.SOUND_PACMAN_DEATH, PM.CHANNEL_AUX);
        } else {
          playSound(PM.SOUND_PACMAN_DEATH_DOUBLE, PM.CHANNEL_AUX);
        }
        gameplayModeTime = timing[PM.TIMING_PLAYER_DYING_PART_2];
        break;

      case PM.GAMEPLAY_FAST_READY_PART_1:
        canvasEl.style.visibility = 'hidden';
        gameplayModeTime = timing[PM.TIMING_FAST_READY_PART_1];
        break;

      case PM.GAMEPLAY_FAST_READY_PART_2:
        stopAllAudio();
        canvasEl.style.visibility = '';
        doorEl.style.display = 'block';
        var el = document.createElement('div');
        el.id = 'pcm-re';
        prepareElement(el, 160, 0);
        playfieldEl.appendChild(el);
        gameplayModeTime = timing[PM.TIMING_FAST_READY_PART_2];
        break;

      case PM.GAMEPLAY_READY_PART_1:
        doorEl.style.display = 'block';
        var el = document.createElement('div');
        el.id = 'pcm-re';
        prepareElement(el, 160, 0);
        playfieldEl.appendChild(el);
        gameplayModeTime = timing[PM.TIMING_READY_PART_1];
        stopAllAudio();
        if (playerCount == 2) {
          playSound(PM.SOUND_START_MUSIC_DOUBLE, PM.CHANNEL_AUX, true);
        } else {
          playSound(PM.SOUND_START_MUSIC, PM.CHANNEL_AUX, true);
        }
        break;

      case PM.GAMEPLAY_READY_PART_2:
        lives = lives - 1; // TODO(jgw): --;
        updateChromeLives();
        gameplayModeTime = timing[PM.TIMING_READY_PART_2];
        break;

      case PM.GAMEPLAY_GAMEOVER:
      case PM.GAMEPLAY_INFINITE_GAMEOVER: {
        var el = document.getElementById('pcm-re');
        DOM.remove(el);

        stopAllAudio();
        el = document.createElement('div');
        el.id = 'pcm-go';
        prepareElement(el, 8, 152);
        playfieldEl.appendChild(el);
        gameplayModeTime = timing[PM.TIMING_GAMEOVER];
        break;
      }

      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_1:
        stopAllAudio();
        gameplayModeTime = timing[PM.TIMING_LEVEL_COMPLETE_PART_1];
        break;

      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_2:
        doorEl.style.display = 'none';
        gameplayModeTime = timing[PM.TIMING_LEVEL_COMPLETE_PART_2];
        break;

      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_3:
        canvasEl.style.visibility = 'hidden';
        gameplayModeTime = timing[PM.TIMING_LEVEL_COMPLETE_PART_3];
        break;

      case PM.GAMEPLAY_DOUBLE_MODE_SWITCH:
        playfieldEl.style.visibility = 'hidden';
        gameplayModeTime = timing[PM.TIMING_DOUBLE_MODE];
        break;

      case PM.GAMEPLAY_GHOST_BEING_EATEN:
        gameplayModeTime = timing[PM.TIMING_GHOST_BEING_EATEN];
        break;

      case PM.GAMEPLAY_CUTSCENE:
        startCutscene();
        break;
    }
  }

  /**
   * Shows or hides the chrome in preparation for cutscenes. Scores, lives,
   * and the sound icon are hidden -- the only thing remaining is the level fruit
   * in the bottom-right corner.
   * @param show Whether to show (true) or hide (false).
   */
  static void showChrome(boolean show) {
    showElementById('pcm-sc-1-l', show);
    showElementById('pcm-sc-2-l', show);
    showElementById('pcm-sc-1', show);
    showElementById('pcm-sc-2', show);
    showElementById('pcm-li', show);
    showElementById('pcm-so', show);
  }

  /**
   * Toggles sound on or off and updates the sound icon.
   * The event is cancelled so that the click does not register as a movement.
   * @param e Window event.
   * @return false.
   */
  static boolean toggleSound(Event e) {
    e = window.event || e;
    e.cancelBubble = true;
    if (pacManSound) {
      userDisabledSound = true;
      stopAllAudio();
      pacManSound = false;
    } else {
      pacManSound = true;
      playAmbientSound();
    }
    updateSoundIcon();
    return e.returnValue = false;
  }

  /**
   * Updates the appearance of the sound icon.
   */
  static void updateSoundIcon() {
    if (soundEl) {
      if (pacManSound) {
        changeElementBkPos(soundEl, 216, 105, false);
      } else {
        changeElementBkPos(soundEl, 236, 105, false);
      }
    }
  }

  /**
   * Starts the cutscene ("coffee break"). Hides the chrome, prepares the
   * cutscene actors.
   */
  static void startCutscene() {
    playfieldEl.style.visibility = 'hidden';
    canvasEl.style.visibility = '';
    showChrome(false);

    cutsceneCanvasEl = document.createElement('div');
    cutsceneCanvasEl.id = 'pcm-cc';
    canvasEl.appendChild(cutsceneCanvasEl);

    cutscene = PM.CUTSCENES[cutsceneId];
    cutsceneSequenceId = -1;

    frightModeTime = levels['frightTotalTime'];

    cutsceneActors = [ ]; // TODO: []
    for (var i = 0; i < cutscene.actors.length; ++i) {
      var id = cutscene.actors[i].id;
      if (id > 0) {
        id += playerCount - 1;
      }

      var el = document.createElement('div');
      el.attributes['class'] = 'pcm-ac';
      el.id = 'actor' + id;
      prepareElement(el, 0, 0);

      var cutsceneActor = new Actor(id);
      cutsceneActor.el = el;
      cutsceneActor.elBackgroundPos = [0, 0];
      cutsceneActor.elPos = [0, 0];
      cutsceneActor.pos = [cutscene.actors[i].y * PM.TILE_SIZE,
                           cutscene.actors[i].x * PM.TILE_SIZE];
      cutsceneActor.posDelta = [0, 0];
      cutsceneActor.ghost = cutscene.actors[i].ghost;

      cutsceneCanvasEl.appendChild(el);

      cutsceneActors.add(cutsceneActor);
    }

    cutsceneNextSequence();

    stopAllAudio();
    playAmbientSound();
  }

  /**
   * Finishes the cutscene, moves on to the next level.
   */
  static void stopCutscene() {
    playfieldEl.style.visibility = '';
    DOM.remove(cutsceneCanvasEl);

    showChrome(true);
    newLevel(false);
  }

  /**
   * Moves on to the next sequence in a cutscene.
   */
  static void cutsceneNextSequence() {
    cutsceneSequenceId = cutsceneSequenceId + 1; // TODO(jgw): ++

    // Last sequence... the cutscene is over.
    if (cutscene.sequence.length == cutsceneSequenceId) {
      stopCutscene();
      return;
    }

    var cutsceneSequence = cutscene.sequence[cutsceneSequenceId];

    cutsceneTime = cutsceneSequence.time * PM.TARGET_FPS;

    for (var i = 0; i < cutsceneActors.length; ++i) {
      var actor = cutsceneActors[i];

      actor.dir = cutsceneSequence.moves[i].dir;
      actor.speed = cutsceneSequence.moves[i].speed;

      if (cutsceneSequence.moves[i].elId) {
        actor.el.id = cutsceneSequence.moves[i].elId;
      }
      if (cutsceneSequence.moves[i].mode) {
        actor.mode = cutsceneSequence.moves[i].mode;
      }

      actor.updateSprite();
    }
  }

  /**
   * Checks whether it's time for the next cutscene sequence.
   */
  static void checkCutscene() {
    if (cutsceneTime <= 0) {
      cutsceneNextSequence();
    }
  }

  /**
   * Updates the cutscene for every frame, moving the actors.
   */
  static void advanceCutscene() {
    for (var i = 0; i < cutsceneActors.length; ++i) {
      var actor = cutsceneActors[i];

      var movement = PM.MOVEMENTS[actor.dir];
      actor.pos[movement.axis] += movement.increment * actor.speed;

      actor.updateSprite();
    }

    cutsceneTime = cutsceneTime - 1; // TODO(jgw): --;
  }

  /**
   * Update positions of all the actors.
   */
  static void updateActorPositions() {
    for (var id = 0; id < actors.length; ++id) {
      actors[id].updatePosition();
    }
  }

  /**
   * Blink the energizers if it's expected in the current mode.
   */
  static void blinkEnergizers() {
    switch (gameplayMode) {
      case PM.GAMEPLAY_READY_PART_1:
      case PM.GAMEPLAY_READY_PART_2:
      case PM.GAMEPLAY_FAST_READY_PART_1:
      case PM.GAMEPLAY_FAST_READY_PART_2:
      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_1:
      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_2:
      case PM.GAMEPLAY_LEVEL_COMPLETE_PART_3:
      case PM.GAMEPLAY_DOUBLE_MODE_SWITCH:
        playfieldEl.classes.clear();
        break;
      case PM.GAMEPLAY_GAMEOVER:
      case PM.GAMEPLAY_INFINITE_GAMEOVER:
        playfieldEl.attributes['class'] = 'blk';
        break;
      default:
        if (globalTime % (timing[PM.TIMING_ENERGIZER] * 2) == 0) {
          playfieldEl.attributes['class'] = '';
        } else if (globalTime %
                   (timing[PM.TIMING_ENERGIZER] * 2) ==
                   timing[PM.TIMING_ENERGIZER]) {
          playfieldEl.attributes['class'] = 'blk';
        }
        break;
    }
  }

  /**
   * Blink the score label.
   */
  static void blinkScoreLabels() {
    if (gameplayMode != PM.GAMEPLAY_CUTSCENE) {
      var visibility = '';
      if (globalTime % (timing[PM.TIMING_SCORE_LABEL] * 2) == 0) {
        visibility = 'visible';
      } else if (globalTime %
                 (timing[PM.TIMING_SCORE_LABEL] * 2) ==
                 timing[PM.TIMING_SCORE_LABEL]) {
        visibility = 'hidden';
      }

      if (visibility) {
        for (var i = 0; i < playerCount; i++) {
          scoreLabelEl[i].style.visibility = visibility;
        }
      }
    }
  }

  /**
   * Finish the fright mode -- restore the previous mode.
   */
  static void finishFrightMode() {
    switchMainGhostMode(lastMainGhostMode, false);
  }

  /**
   * Handle the main gameplay mode timer (various modes, like READY or
   * GAME OVER are timed and need to end).
   */
  static void handleGameplayModeTimer() {
    if (gameplayModeTime) {
      gameplayModeTime = gameplayModeTime - 1; // TODO(jgw): --;

      switch (gameplayMode) {
        case PM.GAMEPLAY_PLAYER_DYING_PART_1:
        case PM.GAMEPLAY_PLAYER_DYING_PART_2:
          for (var i = 0; i < playerCount + PM.GHOST_ACTOR_COUNT;
               i++) {
            actors[i].updateSprite();
          }
          break;
        case PM.GAMEPLAY_LEVEL_COMPLETE_PART_2:
          // The playfield blinks blue and white.
          if ((Math.floor(gameplayModeTime / (timing[PM.TIMING_LEVEL_COMPLETE_PART_2] / 8)) % 2) == 0) {
            changeElementBkPos(playfieldEl, 322, 2, false);
          } else {
            changeElementBkPos(playfieldEl, 322, 138, false);
          }
      }

      if (gameplayModeTime <= 0) {
        gameplayModeTime = 0;

        switch (gameplayMode) {
          case PM.GAMEPLAY_GHOST_BEING_EATEN:
            changeGameplayMode(PM.GAMEPLAY_GAME_IN_PROGRESS);
            ghostEyesCount = ghostEyesCount + 1; // TODO(jgw): ++
            playAmbientSound();
            actors[ghostBeingEatenId].el.attributes['class'] = 'pcm-ac';
            actors[ghostBeingEatenId].changeMode(PM.GHOST_MODE_EYES);

            // If all the ghosts have been eaten etc., we finish the fright
            // mode.
            var fright = false;
            for (var i = playerCount; i < playerCount + PM.GHOST_ACTOR_COUNT; i++) {
              if ((actors[i].mode == PM.GHOST_MODE_FRIGHT) ||
                  (((actors[i].mode == PM.GHOST_MODE_IN_PEN) ||
                   (actors[i].mode == PM.GHOST_MODE_REEXITING_PEN)) &&
                   !actors[i].eatenInThisFrightMode)) {
                fright = true;
                break;
              }
            }
            if (!fright) {
              finishFrightMode();
            }
            break;
          case PM.GAMEPLAY_PLAYER_DYING_PART_1:
            changeGameplayMode(PM.GAMEPLAY_PLAYER_DYING_PART_2);
            break;
          case PM.GAMEPLAY_PLAYER_DYING_PART_2:
            newLife();
            break;
          case PM.GAMEPLAY_READY_PART_1:
            changeGameplayMode(PM.GAMEPLAY_READY_PART_2);
            break;
          case PM.GAMEPLAY_FAST_READY_PART_1:
            changeGameplayMode(PM.GAMEPLAY_FAST_READY_PART_2);
            break;
          case PM.GAMEPLAY_FAST_READY_PART_2:
          case PM.GAMEPLAY_READY_PART_2:
            var el = document.getElementById('pcm-re');
            DOM.remove(el);
            changeGameplayMode(PM.GAMEPLAY_GAME_IN_PROGRESS);
            break;
          case PM.GAMEPLAY_GAMEOVER:
            var el = document.getElementById('pcm-go');
            DOM.remove(el);
            // Go to search results following Game Over
            pacManQuery();
            break;
          case PM.GAMEPLAY_LEVEL_COMPLETE_PART_1:
            changeGameplayMode(PM.GAMEPLAY_LEVEL_COMPLETE_PART_2);
            break;
          case PM.GAMEPLAY_LEVEL_COMPLETE_PART_2:
            changeGameplayMode(PM.GAMEPLAY_LEVEL_COMPLETE_PART_3);
            break;
          case PM.GAMEPLAY_LEVEL_COMPLETE_PART_3:
            if (levels['cutsceneId']) {
              cutsceneId = levels['cutsceneId'];
              changeGameplayMode(PM.GAMEPLAY_CUTSCENE);
            } else {
              canvasEl.style.visibility = '';
              newLevel(false);
            }
            break;
          case PM.GAMEPLAY_DOUBLE_MODE_SWITCH:
            playfieldEl.style.visibility = '';
            canvasEl.style.visibility = '';
            switchToDoubleMode();
            break;
        }
      }
    }
  }

  /**
   * Decrements the fruit timer. If the timer runs out, this means we have to
   * hide the fruit.
   */
  static void handleFruitTimer() {
    if (fruitTime) {
      fruitTime = fruitTime - 1; // TODO(jgw): --;

      if (fruitTime <= 0) {
        hideFruit();
      }
    }
  }

  /**
   * Decrements various timers related to ghosts: fright mode countdown
   * (ghosts stopping being blue/frightened); switching between scatter and
   * chase mode in normal operation.
   */
  static void handleGhostModeTimer() {
    if (frightModeTime) {
      frightModeTime = frightModeTime - 1; // TODO(jgw): --;

      if (frightModeTime <= 0) {
        frightModeTime = 0;
        finishFrightMode();
      }
    } else {
      if (ghostModeTime > 0) {
        ghostModeTime = ghostModeTime - 1; // TODO(jgw): --;

        if (ghostModeTime <= 0) {
          ghostModeTime = 0;
          ghostModeSwitchPos = ghostModeSwitchPos + 1; // TODO(jgw): ++

          if (levels['ghostModeSwitchTimes'][ghostModeSwitchPos]) {
            ghostModeTime = levels['ghostModeSwitchTimes'][ghostModeSwitchPos] * PM.TARGET_FPS;

            switch (mainGhostMode) {
              case PM.GHOST_MODE_SCATTER:
                switchMainGhostMode(PM.GHOST_MODE_CHASE, false);
                break;
              case PM.GHOST_MODE_CHASE:
                switchMainGhostMode(PM.GHOST_MODE_SCATTER, false);
                break;
            }
          }
        }
      }
    }
  }

  /**
   * If the force pen leave timer goes down (and it does so because of
   * Pac-Man's inactivity = not eating dots), the first available ghost can
   * leave the pen.
   */
  static void handleForcePenLeaveTimer() {
    if (forcePenLeaveTime) {
      forcePenLeaveTime = forcePenLeaveTime - 1; // TODO(jgw): --;

      if (forcePenLeaveTime <= 0) {
        for (var i = 1; i <= 3; i++) {
          if (actors[playerCount + i].mode == PM.GHOST_MODE_IN_PEN) {
            actors[playerCount + i].freeToLeavePen = true;
            break;
          }
        }

        resetForcePenLeaveTime();
      }
    }
  }

  /**
   * Handle various game-related timers.
   */
  static void handleTimers() {
    if (gameplayMode == PM.GAMEPLAY_GAME_IN_PROGRESS) {
      handleForcePenLeaveTimer();
      handleFruitTimer();
      handleGhostModeTimer();
    }

    handleGameplayModeTimer();
  }

  /**
   * Handle a tick (heartbeat) of the game -- this is called every 90 frames
   * per second, or less frequently if the framerate has been adjusted.
   */
  static void tick() {
    var time = new Date.now().value;
    lastTimeDelta += (time - lastTime) - tickInterval;

    // We don't want to catch up too much because the actors will jump far
    // away
    if (lastTimeDelta > PM.MAX_TIME_DELTA) {
      lastTimeDelta = PM.MAX_TIME_DELTA;
    }

    // If the delta between ticks is more than 50ms more than 20 times, we
    // fall back to a lower framerate
    if (canDecreaseFps && lastTimeDelta > PM.TIME_SLOWNESS) {
      lastTimeSlownessCount = lastTimeSlownessCount + 1; // TODO(jgw): ++

      if (lastTimeSlownessCount == PM.MAX_SLOWNESS_COUNT) {
        decreaseFps();
      }
    }

    // Subtract extra clicks from the delta, allowing little changes to
    // accumulate.
    var extraTicks = 0;
    if (lastTimeDelta > tickInterval) {
      extraTicks = Math.floor(lastTimeDelta / tickInterval);
      lastTimeDelta -= tickInterval * extraTicks;
    }
    lastTime = time;

    // Cutscenes get a little different treatment...
    if (gameplayMode == PM.GAMEPLAY_CUTSCENE) {
      for (var i = 0; i < tickMultiplier + extraTicks; i++) {
        advanceCutscene();
        intervalTime = (intervalTime + 1) % PM.TARGET_FPS;
        globalTime = globalTime + 1; // TODO(jgw): ++
      }
      checkCutscene();
      blinkScoreLabels();
    } else {
      // ...then all the other modes
      for (var i = 0; i < tickMultiplier + extraTicks; i++) {
        moveActors();
        if (gameplayMode == PM.GAMEPLAY_GAME_IN_PROGRESS) {
          if (tilesChanged) {
            detectCollisions();
            updateActorTargetPositions();
          }
        }

        globalTime = globalTime + 1; // TODO(jgw): ++
        intervalTime = (intervalTime + 1) % PM.TARGET_FPS;

        blinkEnergizers();
        blinkScoreLabels();
        handleTimers();
      }
    }
  }

  /**
   * Award an extra life to a player.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void extraLife(int playerId) {
    playSound(PM.SOUND_EXTRA_LIFE, PM.CHANNEL_AUX);

    extraLifeAwarded[playerId] = true;
    lives = lives + 1; // TODO(jgw): ++
    if (lives > PM.MAX_LIVES) {
      lives = PM.MAX_LIVES;
    }

    updateChromeLives();
  }

  /**
   * Add score to player's counter.
   * @param score Score addition.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void addToScore(int score, int playerId) {
    score[playerId] += score;

    // Award an extra life if we crossed a 10,000 points boundary.
    if (!extraLifeAwarded[playerId] &&
        score[playerId] > PM.EXTRA_LIFE_SCORE) {
      extraLife(playerId);
    }

    updateChromeScore(playerId);
  }

  /**
   * Update all the chrome surrounding the playfield (level, lives, score).
   */
  static void updateChrome() {
    updateChromeLevel();
    updateChromeLives();
    for (var id = 0; id < playerCount; id++) {
      updateChromeScore(id);
    }
  }

  /**
   * Updates score counter.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void updateChromeScore(int playerId) {
    var scoreString = score[playerId].toString();

    if (scoreString.length > scoreDigits) {
      scoreString = scoreString.substr(scoreString.length - scoreDigits, scoreDigits);
    }

    for (var j = 0; j < scoreDigits; j++) {
      var el = document.getElementById('pcm-sc-' + (playerId + 1) + '-' + j);
      var digit = scoreString.substr(j, 1);
      if (!digit) {
        changeElementBkPos(el, 48, 0, true);
      } else {
        changeElementBkPos(el, 8 + 8 * parseInt(digit, 10), 144, true);
      }
    }
  }

  /**
   * Updates the display of lives (a column of Pac-Man icons in the upper-right
   * corners).
   */
  static void updateChromeLives() {
    livesEl.innerHTML = '';
    for (var i = 0; i < lives; i++) {
      var el = document.createElement('div');
      el.attributes['class'] = 'pcm-lif';
      prepareElement(el, 64, 129);
      livesEl.appendChild(el);
    }
  }

  /**
   * Update level indicator.
   */
  static void updateChromeLevel() {
    levelEl.innerHTML = '';

    // We're only showing at most four fruit icons
    var count = level;
    if (count > PM.LEVEL_CHROME_MAX) {
      count = PM.LEVEL_CHROME_MAX;
    }
    for (var i = level; i >= Math.max(level - PM.LEVEL_CHROME_MAX + 1, 1); i--) {
      if (i >= PM.LEVELS.length) {
        var fruit = PM.LEVELS[PM.LEVELS.length - 1]['fruit'];
      } else {
        var fruit = PM.LEVELS[i]['fruit'];
      }

      var el = document.createElement('div');
      var pos = getFruitSprite(fruit);
      prepareElement(el, pos[0], pos[1]);

      levelEl.appendChild(el);
    }

    // Bottom-aligning the icons.
    levelEl.style.marginTop = ((4 - Math.min(level, PM.LEVEL_CHROME_MAX)) * 16) + 'px';
  }

  /**
   * Create chrome surrounding the playfield -- score displays, level indicator,
   * lives indicator.
   */
  static void createChrome() {
    // Resetting the canvas to start from scratch.
    canvasEl.innerHTML = '';

    if (playerCount == 1) {
      scoreDigits = 10;
    } else {
      scoreDigits = 5;
    }

    scoreLabelEl = [ ]; // TODO: []

    // Create the score label (1 UP).
    scoreLabelEl[0] = document.createElement('div');
    scoreLabelEl[0].id = 'pcm-sc-1-l';
    prepareElement(scoreLabelEl[0], 160, 56);
    canvasEl.appendChild(scoreLabelEl[0]);

    // Create the score counter
    scoreEl = [ ]; // TODO: []
    scoreEl[0] = document.createElement('div');
    scoreEl[0].id = 'pcm-sc-1';
    for (var j = 0; j < scoreDigits; j++) {
      var el = document.createElement('div');
      el.id = 'pcm-sc-1-' + j;
      el.style.top = (j * 8) + 'px';
      el.style.left = 0;
      el.style.position = 'absolute';
      el.style.width = '8px';
      el.style.height = '8px';
      prepareElement(el, 48, 0);
      scoreEl[0].appendChild(el);
    }
    canvasEl.appendChild(scoreEl[0]);

    // Create the lives element (showing little Pac-Man icons for each life)
    livesEl = document.createElement('div');
    livesEl.id = 'pcm-li';
    canvasEl.appendChild(livesEl);

    // Create the level element (showing fruit depending on the current level)
    levelEl = document.createElement('div');
    levelEl.id = 'pcm-le';
    canvasEl.appendChild(levelEl);

    // Extra elements in Ms. Pac-Man mode (2 UP and second score counter)
    if (playerCount == 2) {
      scoreLabelEl[1] = document.createElement('div');
      scoreLabelEl[1].id = 'pcm-sc-2-l';
      prepareElement(scoreLabelEl[1], 160, 64);
      canvasEl.appendChild(scoreLabelEl[1]);

      scoreEl[1] = document.createElement('div');
      scoreEl[1].id = 'pcm-sc-2';
      for (var j = 0; j < scoreDigits; j++) {
        var el = document.createElement('div');
        el.id = 'pcm-sc-2-' + j;
        el.style.top = (j * 8) + 'px';
        el.style.left = 0;
        el.style.position = 'absolute';
        el.style.width = '8px';
        el.style.height = '8px';
        prepareElement(el, 48, 0);
        scoreEl[1].appendChild(el);
      }
      canvasEl.appendChild(scoreEl[1]);
    }

    if (soundAvailable) {
      soundEl = document.createElement('div');
      soundEl.id = 'pcm-so';
      prepareElement(soundEl, -32, -16);
      canvasEl.appendChild(soundEl);
      soundEl.onclick = toggleSound;
      updateSoundIcon();
    }
  }

  /**
   * Clear dot eating indicators. Those are used so that dot eating sounds
   * are smooth.
   */
  static void clearDotEatingNow() {
    dotEatingNow = [false, false];
    dotEatingNext = [false, false];
  }

  /**
   * Play a sound.
   * @param soundId Sound id.
   * @param channel Sound channel.
   * @param opt_dontStop Whether to stop that channel or not.
   */
  static void playSound(String soundId, int channel, boolean opt_dontStop) {
    if (!soundAvailable || !pacManSound || paused) {
      return;
    }

    if (!opt_dontStop) {
      stopSoundChannel(channel);
    }

    try {
      flashSoundPlayer.playTrack(soundId, channel);
    } catch (e) {
      soundAvailable = false;
    }
  }

  /**
   * Stop the sound on a given channel.
   * @param channel Sound channel.
   */
  static void stopSoundChannel(int channel) {
    if (!soundAvailable) {
      return;
    }

    try {
      flashSoundPlayer.stopChannel(channel);
    } catch (e) {
      soundAvailable = false;
    }
  }

  /**
   * Stop all sounds.
   */
  static void stopAllAudio() {
    if (!soundAvailable) {
      return;
    }

    try {
      flashSoundPlayer.stopAmbientTrack();
    } catch (e) {
      soundAvailable = false;
    }

    for (var i = 0; i < PM.SOUND_CHANNEL_COUNT; i++) {
      stopSoundChannel(i);
    }
  }

  /**
   * Play the dot eating sound. We need to alternate between two sounds for
   * Pac-Man.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void playDotEatingSound(int playerId) {
    if (!soundAvailable || !pacManSound) {
      return;
    }

    if (gameplayMode == PM.GAMEPLAY_GAME_IN_PROGRESS) {
      if (dotEatingNow[playerId]) {
        dotEatingNext[playerId] = true;
      } else {
        if (playerId == PM.PACMAN) {
          String soundId;

          if (dotEatingSoundPart[playerId] == 1) {
            soundId = PM.SOUND_DOT_EATING_PART_1;
          } else {
            soundId = PM.SOUND_DOT_EATING_PART_2;
          }

          playSound(soundId, PM.CHANNEL_EATING + dotEatingChannel[playerId], true);

          dotTimer = window.setInterval(repeatDotEatingSoundPacMan, PM.DOT_EATING_SOUND_INTERVAL);
        } else {
          playSound(PM.SOUND_DOT_EATING_DOUBLE,
              PM.CHANNEL_EATING_DOUBLE + dotEatingChannel[playerId], true);

          dotTimerMs = window.setInterval(
              repeatDotEatingSoundMsPacMan,
              PM.DOT_EATING_SOUND_INTERVAL);
        }

        dotEatingChannel[playerId] =
            (dotEatingChannel[playerId] + 1) % PM.MULTI_CHANNEL_COUNT;

        dotEatingSoundPart[playerId] =
            3 - dotEatingSoundPart[playerId];
      }
    }
  }

  /**
   * Repeat dot-eating sound if we still need to play it.
   * @param playerId Player id (0 = Pac-Man, 1 = Ms. Pac-Man).
   */
  static void repeatDotEatingSound(int playerId) {
    dotEatingNow[playerId] = false;

    if (dotEatingNext[playerId]) {
      dotEatingNext[playerId] = false;
      playDotEatingSound(playerId);
    }
  }

  /**
   * Repeat dot-eating sound for Pac-Man.
   */
  static void repeatDotEatingSoundPacMan() {
    repeatDotEatingSound(PM.PACMAN);
  }

  /**
   * Repeat dot-eating sound for Ms. Pac-Man.
   */
  static void repeatDotEatingSoundMsPacMan() {
    repeatDotEatingSound(PM.MS_PACMAN);
  }

  /**
   * Play an ambient sound. There's always an ambient, repeating sound in
   * the game that depends on the game mode.
   */
  static void playAmbientSound() {
    if (!soundAvailable || !pacManSound) {
      return;
    }

    var soundId = 0;
    if (gameplayMode == PM.GAMEPLAY_GAME_IN_PROGRESS ||
        gameplayMode == PM.GAMEPLAY_GHOST_BEING_EATEN) {
      if (ghostEyesCount) {
        soundId = PM.SOUND_AMBIENT_EYES;
      } else if (mainGhostMode == PM.GHOST_MODE_FRIGHT) {
        soundId = PM.SOUND_AMBIENT_FRIGHT;
      } else {
        if (dotsEaten > PM.SOUND_AMBIENT_4_DOTS) {
          soundId = PM.SOUND_AMBIENT_4;
        } else if (dotsEaten > PM.SOUND_AMBIENT_3_DOTS) {
          soundId = PM.SOUND_AMBIENT_3;
        } else if (dotsEaten > PM.SOUND_AMBIENT_2_DOTS) {
          soundId = PM.SOUND_AMBIENT_2;
        } else {
          soundId = PM.SOUND_AMBIENT_1;
        }
      }
    } else if (gameplayMode == PM.GAMEPLAY_CUTSCENE) {
      soundId = PM.SOUND_AMBIENT_CUTSCENE;
    }

    if (soundId) {
      try {
        flashSoundPlayer.playAmbientTrack(soundId);
      } catch (e) {
        soundAvailable = false;
      }
    }
  }

  /**
   * (Re)initialize the main tick timer. The tick timer ticks 90 times per
   * seconds and keeps the game alive.
   */
  static void initializeTickTimer() {
    window.clearInterval(tickTimer);

    // We're starting with 90fps, but the game can decrease it to 45fps or
    // even 30fps if necessary.
    fps = PM.ALLOWED_FPS[fpsChoice];
    tickInterval = 1000 / fps;

    // This is 1 for 90fps, 2 for 45fps and 3 for 30fps. It means how often
    // the game logic will be updated compared to screen update.
    tickMultiplier = PM.TARGET_FPS / fps;

    // Translate all the timer values from seconds to ticks.
    timing = [ ]; // TODO: []
    for (var i = 0; i < PM.TIMING.length; ++i) {
      // We're shortening the READY! screen if there's no sound as we don't
      // have to accomodate the music.
      double timing;
      if (!pacManSound &&
          (i == PM.TIMING_READY_PART_1 || i == PM.TIMING_READY_PART_2)) {
        timing = 1;
      } else {
        timing = PM.TIMING[i];
      }

      PacMan.timing[i] = Math.round(timing * PM.TARGET_FPS);
    }

    lastTime = new Date.now().value;
    lastTimeDelta = 0;
    lastTimeSlownessCount = 0;

    tickTimer = window.setInterval(function() { tick(); }, tickInterval);
  }

  /**
   * If the game seems to slow (choppy), we decrease the framerate from 90
   * to 45, or from 45 to 30 seconds.
   */
  static void decreaseFps() {
    if (fpsChoice < PM.ALLOWED_FPS.length - 1) {
      fpsChoice = fpsChoice + 1; // TODO(jgw): ++
      initializeTickTimer();

      if (fpsChoice == PM.ALLOWED_FPS.length - 1) {
        canDecreaseFps = false;
      }
    }
  }

  /**
   * Add the necessary CSS rules.
   */
  static void addCss() {
    styleElement = document.createElement('style');
    styleElement.type = 'text/css';
    if (styleElement.styleSheet) {
      styleElement.styleSheet.cssText = PM.CSS;
    } else {
      styleElement.appendChild(document.createTextNode(PM.CSS));
    }
    document.getElementsByTagName('head')[0].appendChild(styleElement);
  }

  /**
   * Creates the main canvas element
   */
  static void createCanvasElement() {
    canvasEl = document.createElement('div');
    canvasEl.id = 'pcm-c';
    // Fixes the annoying border when focusing on IE.
    canvasEl.hideFocus = true;

    document.getElementById('logo').appendChild(canvasEl);

    // Focusing on the element so that up/right arrow do not invoke things
    // in the search box or elsewhere.
    canvasEl.tabIndex = 0;
    canvasEl.focus();
  }

  /**
   * Start the process once everything's loaded.
   */
  static void everythingIsReady() {
    if (ready) {
      return;
    }
    ready = true;

    // Remove the loading message
    var el = document.getElementById('logo-l');
    DOM.remove(el);
    // Remove the doodle (not removed if autoplay)
    document.getElementById('logo').style.background = 'black';

    addCss();

    createCanvasElement();

    speedIntervals = [ ]; // TODO: []

    oppositeDirections = [ ]; // TODO: []
    oppositeDirections[PM.DIR_UP] = PM.DIR_DOWN;
    oppositeDirections[PM.DIR_DOWN] = PM.DIR_UP;
    oppositeDirections[PM.DIR_LEFT] = PM.DIR_RIGHT;
    oppositeDirections[PM.DIR_RIGHT] = PM.DIR_LEFT;

    addEventListeners();
    fpsChoice = 0;
    canDecreaseFps = true;

    initializeTickTimer();

    newGame();
  }

  /**
   * Checks whether both sounds and graphics have been loaded. If so,
   * proceed, but with little delay to allow Flash to find its bearings.
   */
  static void checkIfEverythingIsReady() {
    if (soundReady || graphicsReady) {
      updateLoadingProgress(.67);
    }

    if (soundReady && graphicsReady) {
      updateLoadingProgress(1);
      everythingIsReady();
    }
  }

  /**
   * Preloads an image so that we know it's in the cache when we start the
   * game
   * @param url URL of the image.
   */
  static void preloadImage(String url) {
    var img = new Image();
    var isMSIE = false; // google.browser.engine.IE;

    if (!isMSIE) {
      img.onload = function() { imageLoaded(); };
    }
    img.src = url;

    // IE doesn't call onload when the image is in the cache. In that case,
    // we assume image loaded.
    if (isMSIE) {
      imageLoaded();
    }
  }

  /**
   * Gets called when the image is loaded.
   */
  static void imageLoaded() {
    graphicsReady = true;
    checkIfEverythingIsReady();
  }

  /**
   * Preload all the necessary graphics (actually, just one image).
   */
  static void prepareGraphics() {
    graphicsReady = false;
    preloadImage('pacman10-hp-sprite.png');
  }

  /**
   * Trims white spaces to the left and right of a string.
   * Function lifted from Closure.
   * @param str The string to trim.
   * @return A trimmed copy of {@code str}.
   */
  static String trimString(String str) {
    // Since IE doesn't include non-breaking-space (0xa0) in their \s character
    // class (as required by section 7.2 of the ECMAScript spec), we explicitly
    // include it in the regexp to enforce consistent cross-browser behavior.
    return str.replace(new RegExp('^[\\s\\xa0]+|[\\s\\xa0]+$', 'g'), '');
  }

  /**
   * Compares elements of a version number.
   * Function lifted from Closure.
   *
   * @param left An element from a version number.
   * @param right An element from a version number.
   *
   * @return  1 if {@code left} is higher.
   *          0 if arguments are equal.
   *         -1 if {@code right} is higher.
   */
  static int compareElements_(int left, int right) {
    if (left < right) {
      return -1;
    } else if (left > right) {
      return 1;
    }
    return 0;
  }

  /**
   * Compares two version numbers.
   * Function lifted from Closure.
   *
   * @param version1 Version of first item.
   * @param version2 Version of second item.
   *
   * @return  1 if {@code version1} is higher.
   *          0 if arguments are equal.
   *         -1 if {@code version2} is higher.
   */
  static int compareVersions(int version1, int version2) {
    var order = 0;
    // Trim leading and trailing whitespace and split the versions into
    // subversions.
    var v1Subs = trimString(String(version1)).split('.');
    var v2Subs = trimString(String(version2)).split('.');
    var subCount = Math.max(v1Subs.length, v2Subs.length);

    // Iterate over the subversions, as long as they appear to be equivalent.
    for (var subIdx = 0; order == 0 && subIdx < subCount; subIdx++) {
      var v1Sub = v1Subs[subIdx] || '';
      var v2Sub = v2Subs[subIdx] || '';

      // Split the subversions into pairs of numbers and qualifiers (like 'b').
      // Two different RegExp objects are needed because they are both using
      // the 'g' flag.
      var v1CompParser = new RegExp('(\\d*)(\\D*)', 'g');
      var v2CompParser = new RegExp('(\\d*)(\\D*)', 'g');
      do {
        var v1Comp = v1CompParser.exec(v1Sub) || ['', '', ''];
        var v2Comp = v2CompParser.exec(v2Sub) || ['', '', ''];
        // Break if there are no more matches.
        if (v1Comp[0].length == 0 && v2Comp[0].length == 0) {
          break;
        }

        // Parse the numeric part of the subversion. A missing number is
        // equivalent to 0.
        var v1CompNum = v1Comp[1].length == 0 ? 0 : parseInt(v1Comp[1], 10);
        var v2CompNum = v2Comp[1].length == 0 ? 0 : parseInt(v2Comp[1], 10);

        // Compare the subversion components. The number has the highest
        // precedence. Next, if the numbers are equal, a subversion without any
        // qualifier is always higher than a subversion with any qualifier.
        // Next, the qualifiers are compared as strings.
        order = compareElements_(v1CompNum, v2CompNum) ||
            compareElements_(v1Comp[2].length == 0,
                v2Comp[2].length == 0) ||
            compareElements_(v1Comp[2], v2Comp[2]);
      // Stop as soon as an inequality is discovered.
      } while (order == 0);
    }

    return order;
  }

  /**
   * Gets/normalizes the Flash version.
   * Function lifted from google3/javascript/closure/useragent/flash.js
   * @param desc Description from MIME or plugin.
   * @return Three-segment version (e.g. 10.1.2).
   */
  static String getFlashVersion(String desc) {
    var matches = desc.match(new RegExp('[\\d]+', 'g'));
    matches.length = 3; // To standardize IE vs. FF
    return matches.join('.');
  }

  /**
   * Detects the presence and version of Flash.
   * Function lifted from google3/javascript/closure/useragent/flash.js
   */
  static void detectFlash() {
    hasFlash = true;
    flashVersion = PM.MIN_FLASH_VERSION;

    /* TODO(jgw): Shimming all the navigator stuff just isn't worth the trouble for now.
    var hasFlash = false;
    var flashVersion = '';

    if (navigator.plugins && navigator.plugins.length) {
      var plugin = navigator.plugins['Shockwave Flash'];
      if (plugin) {
        hasFlash = true;
        if (plugin.description) {
          flashVersion = getFlashVersion(plugin.description);
        }
      }

      if (navigator.plugins['Shockwave Flash 2.0']) {
        hasFlash = true;
        flashVersion = '2.0.0.11';
      }
    } else if (navigator.mimeTypes && navigator.mimeTypes.length) {
      var mimeType = navigator.mimeTypes['application/x-shockwave-flash'];
      hasFlash = mimeType && mimeType.enabledPlugin;
      if (hasFlash) {
        var description = mimeType.enabledPlugin.description;
        flashVersion = getFlashVersion(description);
      }
    } else {
      try {
        // Try 7 first, since we know we can use GetVariable with it
        var ax = new ActiveXObject('ShockwaveFlash.ShockwaveFlash.7');
        hasFlash = true;
        flashVersion = getFlashVersion(ax.GetVariable('$version'));
      } catch (e) {
        // Try 6 next, some versions are known to crash with GetVariable calls
        try {
          var ax = new ActiveXObject('ShockwaveFlash.ShockwaveFlash.6');
          hasFlash = true;
          flashVersion = '6.0.21'; // First public version of Flash 6
        } catch (e2) {
          try {
            // Try the default activeX
            var ax = new ActiveXObject('ShockwaveFlash.ShockwaveFlash');
            hasFlash = true;
            flashVersion = getFlashVersion(ax.GetVariable('$version'));
          } catch (e3) {
            // No flash
          }
        }
      }
    }

    hasFlash = hasFlash;
    flashVersion = flashVersion;
    */
  }

  /**
   * Test whether the version of installed Flash is the same or higher than
   * required.
   * @param version Version of Flash required (e.g. 10.0.0.0).
   * @return True if compatible, false if not.
   */
  static boolean isFlashVersion(String version) {
    // TODO
    return true;
//    return compareVersions(flashVersion, version) >= 0;
  }

  /**
   * Create a Flash controller to host sound, if Flash is available.
   * Originally, we tried to do HTML5 audio, but it proved unreliable even
   * to use in browsers that nominally support it.
   */
  static void prepareSound() {
    soundAvailable = false;
    soundReady = false;

    detectFlash();

    // Don't even try if no Flash.
    if (!hasFlash || !isFlashVersion(PM.MIN_FLASH_VERSION)) {
      soundReady = true;
      checkIfEverythingIsReady();
      return;
    }

    // For some reason, creating an <object> in DOM failed in IE. We need
    // to put it in separate iframe to use document.write. The frame also
    // needs to be a certain size for IE to use it.
    flashIframe = document.createElement('iframe');
    flashIframe.name = 'pm-sound';
    flashIframe.style.position = 'absolute';
    flashIframe.style.top = '-150px';
    flashIframe.style.border = 0;
    flashIframe.style.width = '100px';
    flashIframe.style.height = '100px';
    DOM.append(flashIframe);

    flashIframeDoc = flashIframe.contentDocument;
    if (!flashIframeDoc) {
      flashIframeDoc = flashIframe.contentWindow.document;
    }

    flashIframeDoc.open();
    flashIframeDoc.write(
      '<html><head></head><body>' +
      '<object classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000" ' +
      'codebase="http://download.macromedia.com/pub/shockwave/' +
      'cabs/flash/swflash.cab#version=9,0,0,0" width="0" height="0" ' +
      'id="pacman-sound-player" type="application/x-shockwave-flash"> ' +
      '<param name="movie" value="pacman10-hp-sound.swf"> ' +
      '<param name="allowScriptAccess" value="always"> ' +
      '<object id="pacman-sound-player-2"  ' +
      'type="application/x-shockwave-flash" ' +
      'data="pacman10-hp-sound.swf" ' +
      'width="0" height="0"><param name="allowScriptAccess" value="always"> ' +
      '</object></object></body></html>');
    flashIframeDoc.close();

    window.setTimeout(function() { flashNotReady(); }, PM.FLASH_NOT_READY_TIMEOUT);
  }

  /**
   * Flash is not ready or available in time. We proceed as if Flash wasn't
   * present.
   */
  static void flashNotReady() {
    if (!ready) {
      soundAvailable = false;
      soundReady = true;
      checkIfEverythingIsReady();
    }
  }

  /**
   * Flash is ready.
   * @param el DOM element.
   */
  static void flashReady(Element el) {
    flashSoundPlayer = el;
    soundAvailable = true;
    soundReady = true;
    checkIfEverythingIsReady();
  }

  /**
   * Callback function whenever the Flash sound controller loads and reports
   * ready. We try to find the controller, and then proceed with game loading.
   */
  static void flashLoaded() {
    if (flashIframeDoc) {
      var el = flashIframeDoc.getElementById('pacman-sound-player');

      if (el && el.playTrack) {
        flashReady(el);
        return;
      } else {
        var el = flashIframeDoc.getElementById('pacman-sound-player-2');

        if (el && el.playTrack) {
          flashReady(el);
          return;
        }
      }
    }

    // In this case, Flash loads, but we can't find the controller. We fall
    // back to no sound.
    flashNotReady();
  }

  /**
   * Clean up the game during JESR transistions.
   */
  static void destroy() {
    stopAllAudio();
    window.clearInterval(tickTimer);
    window.clearInterval(dotTimer);
    window.clearInterval(dotTimerMs);
    DOM.remove(styleElement);
    DOM.remove(flashIframe);
    DOM.remove(canvasEl);
  }

  /**
   * Export external function calls.
   */
  static void exportFunctionCalls() {
    /* TODO(jgw): Decide how to do exports.
    google.pacman = {};

    // This function is called when pressing the button on the homepage
    google.pacman.insertCoin = insertCoin;

    // This function is called from Flash sound controller
    google.pacman.flashLoaded = flashLoaded;

    // Called in google.dstr in JESR.
    google.pacman.destroy = destroy;
    */
  }

  /**
   * Update the loading progress bar shown before the game loads.
   * @param progress Number from 0.0 to 1.0.
   */
  static void updateLoadingProgress(double progress) {
    var val = Math.round(progress * 200);
    document.getElementById('logo-b').style.width = val + 'px';
  }

  static void pacManQuery() {
    alert('TODO: something interesting after game over');
  }

  /**
   * Start the loading process.
   */
  static void main() {
    ready = false;

    // Remove the alt text from the doodle so it doesn't appear when the user
    // hovers the mouse over the game.
    document.getElementById('logo').title = '';

    updateLoadingProgress(.33);
    exportFunctionCalls();

    // In case of old Internet Explorers we give up on using CSS for spriting
    // because of the bugs that make IE reload the same image over and over
    // again.
    if (navigator.userAgent.indexOf('MSIE 5.') != -1 ||
        navigator.userAgent.indexOf('MSIE 6.') != -1 ||
        navigator.userAgent.indexOf('MSIE 7.') != -1) {
      useCss = false;
    } else {
      useCss = true;
    }

    prepareGraphics();
    prepareSound();
  }
}
