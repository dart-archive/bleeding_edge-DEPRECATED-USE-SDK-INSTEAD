// A Dart port of Kevin Roast's Asteroids game.
// http://www.kevs3d.co.uk/dev/asteroids
// Used with permission, including the sound and bitmap assets.

// This should really be multiple files but the embedder doesn't support
// parts yet. I concatenated the parts in a somewhat random order.
//
// Note that Skia seems to have issues with the render compositing modes, so
// explosions look a bit messy; they aren't transparent where they should be.
//
// Currently we use the accelerometer on the phone for direction and thrust.
// This is hard to control and should probably be changed. The game is also a
// bit janky on the phone.

library asteroids;

import 'dart:math' as Math;
import 'gl.dart';

const RAD = Math.PI / 180.0;
const PI = Math.PI;
const TWOPI = Math.PI * 2;
const ONEOPI = 1.0 / Math.PI;
const PIO2 = Math.PI / 2.0;
const PIO4 = Math.PI / 4.0;
const PIO8 = Math.PI / 8.0;
const PIO16 = Math.PI / 16.0;
const PIO32 = Math.PI / 32.0;

var _rnd = new Math.Random();
double random() => _rnd.nextDouble();
int randomInt(int min, int max) => min + _rnd.nextInt(max - min + 1);

class Key {
  static const SHIFT = 16;
  static const CTRL = 17;
  static const ESC = 27;
  static const RIGHT = 39;
  static const UP = 38;
  static const LEFT = 37;
  static const DOWN = 40;
  static const SPACE = 32;
  static const A = 65;
  static const E = 69;
  static const G = 71;
  static const L = 76;
  static const P = 80;
  static const R = 82;
  static const S = 83;
  static const Z = 90;
}

// Globals
var Debug = {
  'enabled': false,
  'invincible': false,
  'collisionRadius': false,
  'fps': true
};

var glowEffectOn = true;
const GLOWSHADOWBLUR = 8;
const SCOREDBKEY = "asteroids-score-1.1";

var _asteroidImgs = [];
var _shieldImg = new ImageElement();
var _backgroundImg = new ImageElement();
var _playerImg = new ImageElement();
var _enemyshipImg = new ImageElement();
var soundManager;

/** Asteroids color constants */
class Colors {
  static const PARTICLE = "rgb(255,125,50)";
  static const ENEMY_SHIP = "rgb(200,200,250)";
  static const ENEMY_SHIP_DARK = "rgb(150,150,200)";
  static const GREEN_LASER = "rgb(120,255,120)";
  static const GREEN_LASER_DARK = "rgb(50,255,50)";
  static const GREEN_LASERX2 = "rgb(120,255,150)";
  static const GREEN_LASERX2_DARK = "rgb(50,255,75)";
  static const PLAYER_BOMB = "rgb(155,255,155)";
  static const PLAYER_THRUST = "rgb(25,125,255)";
  static const PLAYER_SHIELD = "rgb(100,100,255)";
}

/**
 * Actor base class.
 *
 * Game actors have a position in the game world and a current vector to
 * indicate direction and speed of travel per frame. They each support the
 * onUpdate() and onRender() event methods, finally an actor has an expired()
 * method which should return true when the actor object should be removed
 * from play.
 */
class Actor {
  Vector position, velocity;

  Actor(this.position, this.velocity);

  /**
   * Actor game loop update event method. Called for each actor
   * at the start of each game loop cycle.
   */
   onUpdate(Scene scene) {}

  /**
   * Actor rendering event method. Called for each actor to
   * render for each frame.
   */
   void onRender(CanvasRenderingContext2D ctx) {}

  /**
   * Actor expiration test; return true if expired and to be removed
   * from the actor list, false if still in play.
   */
   bool expired() => false;

   get frameMultiplier => GameHandler.frameMultiplier;
   get frameStart => GameHandler.frameStart;
   get canvas_height => GameHandler.height;
   get canvas_width => GameHandler.width;
}

// Short-lived actors (like particles and munitions). These have a
// start time and lifespan, and fade out after a period.

class ShortLivedActor extends Actor {
  int lifespan;
  int start;

  ShortLivedActor(Vector position, Vector velocity,
      this.lifespan)
      : super(position, velocity),
      this.start = GameHandler.frameStart;

  bool expired() => (frameStart - start > lifespan);

  /**
   * Helper to return a value multiplied by the ratio of the remaining lifespan
   */
  double fadeValue(double val, int offset) {
    var rem = lifespan - (frameStart - start),
    result = val;
    if (rem < offset) {
      result = (val / offset) * rem;
      result = Math.max(0.0, Math.min(result, val));
    }
    return result;
  }
}

class AttractorScene extends Scene {
  AsteroidsMain game;

  AttractorScene(this.game)
      : super(false, null) {
  }

  bool start = false;
  bool imagesLoaded = false;
  double sine = 0.0;
  double mult = 0.0;
  double multIncrement = 0.0;
  List actors = null;
  const SCENE_LENGTH = 400;
  const SCENE_FADE = 75;
  List sceneRenderers = null;
  int currentSceneRenderer = 0;
  int currentSceneFrame = 0;

  bool isComplete() => start;

  void onInitScene() {
    start = false;
    mult = 512.0;
    multIncrement = 0.5;
    currentSceneRenderer = 0;
    currentSceneFrame = 0;

    // scene renderers
    // display welcome text, info text and high scores
    sceneRenderers = [
        sceneRendererWelcome,
        sceneRendererInfo,
        sceneRendererScores ];

    // randomly generate some background asteroids for attractor scene
    actors = [];
    for (var i = 0; i < 8; i++) {
      var pos = new Vector(random() * GameHandler.width.toDouble(),
                           random() * GameHandler.height.toDouble());
      var vec = new Vector(((random() * 2.0) - 1.0), ((random() * 2.0) - 1.0));
      actors.add(new Asteroid(pos, vec, randomInt(3, 4)));
    }

    game.score = 0;
    game.lives = 3;
  }

  void onRenderScene(CanvasRenderingContext2D ctx) {
    if (imagesLoaded) {
      // Draw the background asteroids.
      for (var i = 0; i < actors.length; i++) {
        var actor = actors[i];
        actor.onUpdate(this);
        game.updateActorPosition(actor);
        actor.onRender(ctx);
      }

      // Handle cycling through scenes.
      if (++currentSceneFrame == SCENE_LENGTH) { // Move to next scene.
        if (++currentSceneRenderer == sceneRenderers.length) {
          currentSceneRenderer = 0; // Wrap to first scene.
        }
        currentSceneFrame = 0;
      }

      ctx.save();

      // fade in/out
      if (currentSceneFrame < SCENE_FADE) {
        // fading in
        ctx.globalAlpha = 1 - ((SCENE_FADE - currentSceneFrame) / SCENE_FADE);
      } else if (currentSceneFrame >= SCENE_LENGTH - SCENE_FADE) {
        // fading out
        ctx.globalAlpha = ((SCENE_LENGTH - currentSceneFrame) / SCENE_FADE);
      } else {
        ctx.globalAlpha = 1.0;
      }

      sceneRenderers[currentSceneRenderer](ctx);

      ctx.restore();

      sineText(ctx, "BLASTEROIDS",
          GameHandler.width ~/ 2 - 130, GameHandler.height ~/ 2 - 64);
    } else {
      centerFillText(ctx, "Loading...",
          "18pt Courier New", GameHandler.height ~/ 2, "white");
    }
  }

  void sceneRendererWelcome(CanvasRenderingContext2D ctx) {
    ctx.fillStyle = ctx.strokeStyle = "white";
    centerFillText(ctx, "Press SPACE or click to start", "18pt Courier New",
        GameHandler.height ~/ 2);
    fillText(ctx, "based on Javascript game by Kevin Roast",
        "10pt Courier New", 16, 624);
  }

  void sceneRendererInfo(CanvasRenderingContext2D ctx) {
    ctx.fillStyle = ctx.strokeStyle = "white";
    fillText(ctx, "How to play...", "14pt Courier New", 40, 320);
    fillText(ctx, "Arrow keys or tilt to rotate, thrust, shield. "
        "SPACE or touch to fire.",
        "14pt Courier New", 40, 350);
    fillText(ctx, "Pickup the glowing power-ups to enhance your ship.",
        "14pt Courier New", 40, 370);
    fillText(ctx, "Watch out for enemy saucers!", "14pt Courier New", 40, 390);
  }

  void sceneRendererScores(CanvasRenderingContext2D ctx) {
    ctx.fillStyle = ctx.strokeStyle = "white";
    centerFillText(ctx, "High Score", "18pt Courier New", 320);
    var sscore = this.game.highscore.toString();
    // pad with zeros
    for (var i=0, j=8-sscore.length; i<j; i++) {
      sscore = "0$sscore";
    }
    centerFillText(ctx, sscore, "18pt Courier New", 350);
  }

  /** Callback from image preloader when all images are ready */
  void ready() {
    imagesLoaded = true;
  }

  /**
   * Render the a text string in a pulsing x-sine y-cos wave pattern
   * The multiplier for the sinewave is modulated over time
   */
  void sineText(CanvasRenderingContext2D ctx, String txt, int xpos, int ypos) {
    mult += multIncrement;
    if (mult > 1024.0) {
      multIncrement = -multIncrement;
    } else if (this.mult < 128.0) {
      multIncrement = -multIncrement;
    }
    var offset = sine;
    for (var i = 0; i < txt.length; i++) {
      var y = ypos + ((Math.sin(offset) * RAD) * mult).toInt();
      var x = xpos + ((Math.cos(offset++) * RAD) * (mult * 0.5)).toInt();
      fillText(ctx, txt[i], "36pt Courier New", x + i * 30, y, "white");
    }
    sine += 0.075;
  }

  bool onKeyDownHandler(int keyCode) {
    log("In onKeyDownHandler, AttractorScene");
    switch (keyCode) {
      case Key.SPACE:
        if (imagesLoaded) {
          start = true;
        }
        return true;
      case Key.ESC:
        GameHandler.togglePause();
        return true;
    }
    return false;
  }

  bool onMouseDownHandler(e) {
    if (imagesLoaded) {
      start = true;
    }
    return true;
  }
}

/**
 * An actor representing a transient effect in the game world. An effect is
 * nothing more than a special graphic that does not play any direct part in
 * the game and does not interact with any other objects. It automatically
 * expires after a set lifespan, generally the rendering of the effect is
 * based on the remaining lifespan.
 */
class EffectActor extends Actor {
  int lifespan; // in msec.
  int effectStart; // start time

  EffectActor(Vector position , Vector velocity, [this.lifespan = 0])
      : super(position, velocity) {
    effectStart = frameStart;
  }

  bool expired() => (frameStart - effectStart > lifespan);

  /**
   * Helper for an effect to return the value multiplied by the ratio of the
   * remaining lifespan of the effect.
   */
  double effectValue(double val) {
    var result = val - (val * (frameStart - effectStart)) / lifespan;
    return Math.max(0.0, Math.min(val, result));
  }
}

/** Text indicator effect actor class. */
class TextIndicator extends EffectActor {
  int fadeLength;
  int textSize;
  String msg;
  String color;

  TextIndicator(Vector position, Vector velocity, this.msg,
      [this.textSize = 12, this.color = "white",
          int fl = 500]) :
    super(position, velocity, fl), fadeLength = fl;

  const DEFAULT_FADE_LENGTH = 500;


  void onRender(CanvasRenderingContext2D ctx) {
    // Fade out alpha.
    ctx.save();
    ctx.globalAlpha = effectValue(1.0);
    fillText(ctx, msg, "${textSize}pt Courier New",
        position.x, position.y, color);
    ctx.restore();
  }
}

/** Score indicator effect actor class. */
class ScoreIndicator extends TextIndicator {
  ScoreIndicator(Vector position, Vector velocity, int score,
      [int textSize = 12, String prefix = '', String color = "white",
      int fadeLength = 500]) :
    super(position, velocity, '${prefix.length > 0 ? "$prefix " : ""}${score}',
        textSize, color, fadeLength);
}

/** Power up collectable. */
class PowerUp extends EffectActor {
  PowerUp(Vector position, Vector velocity)
  : super(position, velocity);

  const RADIUS = 8;
  int pulse = 128;
  int pulseinc = 5;

  void onRender(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.globalAlpha = 0.75;
    var col = "rgb(255,${pulse.toString()},0)";
    ctx.fillStyle = col;
    ctx.strokeStyle = "rgb(255,255,128)";
    ctx.beginPath();
    ctx.arc(position.x, position.y, RADIUS, 0, TWOPI, true);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
    ctx.restore();
    pulse += pulseinc;
    if (pulse > 255){
      pulse = 256 - pulseinc;
      pulseinc =- pulseinc;
    } else if (pulse < 0) {
      pulse = 0 - pulseinc;
      pulseinc =- pulseinc;
    }
  }

  get radius => RADIUS;

  void collected(AsteroidsMain game, Player player, GameScene scene) {
    // Randomly select a powerup to apply.
    var message = null;
    var n, m, enemy, pos;
    switch (randomInt(0, 9)) {
      case 0:
      case 1:
        message = "Energy Boost!";
        player.energy += player.ENERGY_INIT / 2;
        if (player.energy > player.ENERGY_INIT) {
          player.energy = player.ENERGY_INIT;
        }
        break;

      case 2:
        message = "Fire When Shielded!";
        player.fireWhenShield = true;
        break;

      case 3:
        message = "Extra Life!";
        game.lives++;
        break;

      case 4:
        message = "Slow Down Asteroids!";
        m = scene.enemies.length;
        for (n = 0; n < m; n++) {
          enemy = scene.enemies[n];
          if (enemy is Asteroid) {
            enemy.velocity.scale(0.66);
          }
        }
        break;

      case 5:
        message = "Smart Bomb!";

        var effectRad = 96;

        // Add a BIG explosion actor at the smart bomb weapon position
        // and vector.
        var boom = new Explosion(position.clone(),
            velocity.nscale(0.5), effectRad / 8);
        scene.effects.add(boom);

        // Test circle intersection with each enemy actor.
        // We check the enemy list length each iteration to catch baby asteroids
        // this is a fully fledged smart bomb after all!
        pos = position;
        for (n = 0; n < scene.enemies.length; n++) {
          enemy = scene.enemies[n];

          // Test the distance against the two radius combined.
          if (pos.distance(enemy.position) <= effectRad + enemy.radius) {
            // Intersection detected!
            enemy.hit(-1);
            scene.generatePowerUp(enemy);
            scene.destroyEnemy(enemy, velocity, true);
          }
        }
        break;

      case 6:
        message = "Twin Cannons!";
        player.primaryWeapons["main"] = new TwinCannonsWeapon(player);
        break;

      case 7:
        message = "Spray Cannons!";
        player.primaryWeapons["main"] = new VSprayCannonsWeapon(player);
        break;

      case 8:
        message = "Rear Gun!";
        player.primaryWeapons["rear"] = new RearGunWeapon(player);
        break;

      case 9:
        message = "Side Guns!";
        player.primaryWeapons["side"] = new SideGunWeapon(player);
        break;
    }

    if (message != null) {
      // Generate a effect indicator at the destroyed enemy position.
      var vec = new Vector(0.0, -1.5);
      var effect = new TextIndicator(
           new Vector(position.x, position.y - RADIUS), vec,
               message, null, null, 700);
      scene.effects.add(effect);
    }
  }
}
/**
 * This is the common base class of actors that can be hit and destroyed by
 * player bullets. It supports a hit() method which should return true when
 * the enemy object should be removed from play.
 */
class EnemyActor extends SpriteActor {
  EnemyActor(Vector position, Vector velocity, this.size)
      : super(position, velocity);

  bool alive = true;

  /** Size - values from 1-4 are valid for asteroids, 0-1 for ships. */
  int size;

  bool expired() => !alive;

  bool hit(num force) {
    alive = false;
    return true;
  }
}

/**
 * Asteroid actor class.
 */
class Asteroid extends EnemyActor {
  Asteroid(Vector position, Vector velocity, int size, [this.type])
    : super(position, velocity, size) {
    health = size;

    // Randomly select an asteroid image bitmap.
    if (type == null) {
      type = randomInt(1, 4);
    }
    animImage = _asteroidImgs[type-1];

    // Rrandomly setup animation speed and direction.
    animForward = (random() < 0.5);
    animSpeed = 0.3 + random() * 0.5;
    animLength = ANIMATION_LENGTH;
    rotation = randomInt(0, 180);
    rotationSpeed = (random() - 0.5) / 30;
  }

  const ANIMATION_LENGTH = 180;

  /** Asteroid graphic type i.e. which bitmap it is drawn from. */
  int type;

  /** Asteroid health before it's destroyed. */
  num health = 0;

  /** Retro graphics mode rotation orientation and speed. */
  int rotation = 0;
  double rotationSpeed = 0.0;

  /** Asteroid rendering method. */
  void onRender(CanvasRenderingContext2D ctx) {
    var rad = size * 8;
    ctx.save();
    // Render asteroid graphic bitmap. The bitmap is rendered slightly large
    // than the radius as the raytraced asteroid graphics do not quite touch
    // the edges of the 64x64 sprite - this improves perceived collision
    // detection.
    renderSprite(ctx, position.x - rad - 2, position.y - rad - 2, (rad * 2)+4);
    ctx.restore();
  }

  get radius => size * 8;

  bool hit(num force) {
    if (force != -1) {
      health -= force;
    } else {
      // instant kill
      health = 0;
    }
    return !(alive = (health > 0));
  }
}

/** Enemy Ship actor class. */
class EnemyShip extends EnemyActor {

  get radius => _radius;

  EnemyShip(GameScene scene, int size)
      : super(null, null, size) {
    // Small ship, alter settings slightly.
    if (size == 1) {
      BULLET_RECHARGE_MS = 1300;
      _radius = 8;
    } else {
      _radius = 16;
    }

    // Randomly setup enemy initial position and vector
    // ensure the enemy starts in the opposite quadrant to the player.
    var p, v;
    if (scene.player.position.x < canvas_width / 2) {
      // Player on left of the screen.
      if (scene.player.position.y < canvas_height / 2) {
        // Player in top left of the screen.
        position = new Vector(canvas_width-48, canvas_height-48);
      } else {
        // Player in bottom left of the screen.
        position = new Vector(canvas_width-48, 48);
      }
      velocity = new Vector(-(random() + 0.25 + size * 0.75),
          random() + 0.25 + size * 0.75);
    } else {
      // Player on right of the screen.
      if (scene.player.position.y < canvas_height / 2) {
        // Player in top right of the screen.
        position = new Vector(0, canvas_height-48);
      } else {
        // Player in bottom right of the screen.
        position = new Vector(0, 48);
      }
      velocity = new Vector(random() + 0.25 + size * 0.75,
          random() + 0.25 + size * 0.75);
    }

   // Setup SpriteActor values.
   animImage = _enemyshipImg;
   animLength = SHIP_ANIM_LENGTH;
 }

 const SHIP_ANIM_LENGTH = 90;
 int _radius;
 int BULLET_RECHARGE_MS = 1800;


  /** True if ship alive, false if ready for expiration. */
  bool alive = true;

  /** Bullet fire recharging counter. */
  int bulletRecharge = 0;

  void onUpdate(GameScene scene) {
    // change enemy direction randomly
    if (size == 0) {
      if (random() < 0.01) {
        velocity.y = -(velocity.y + (0.25 - (random()/2)));
      }
    } else {
      if (random() < 0.02) {
        velocity.y = -(velocity.y + (0.5 - random()));
      }
    }

    // regular fire a bullet at the player
    if (frameStart - bulletRecharge >
        BULLET_RECHARGE_MS && scene.player.alive) {
      // ok, update last fired time and we can now generate a bullet
      bulletRecharge = frameStart;

      // generate a vector pointed at the player
      // by calculating a vector between the player and enemy positions
      var v = scene.player.position.clone().sub(position);
      // scale resulting vector down to bullet vector size
      var scale = (size == 0 ? 3.0 : 3.5) / v.length();
      v.x *= scale;
      v.y *= scale;
      // slightly randomize the direction (big ship is less accurate also)
      v.x += (size == 0 ? (random() * 2.0 - 1.0) : (random() - 0.5));
      v.y += (size == 0 ? (random() * 2.0 - 1.0) : (random() - 0.5));
      // - could add the enemy motion vector for correct momentum
      // - but this leads to slow bullets firing back from dir of travel
      // - so pretend that enemies are clever enough to account for this...
      //v.add(this.vector);

      var bullet = new EnemyBullet(position.clone(), v);
      scene.enemyBullets.add(bullet);
      //soundManager.play('enemy_bomb');
    }
  }

  /** Enemy rendering method. */
  void onRender(CanvasRenderingContext2D ctx) {
    // render enemy graphic bitmap
    var rad = radius + 2;
    renderSprite(ctx, position.x - rad, position.y - rad, rad * 2);
  }

  /** Enemy hit by a bullet; return true if destroyed, false otherwise. */
  bool hit(num force) {
    alive = false;
    return true;
  }

  bool expired() {
    return !alive;
  }
}

class GameCompleted extends Scene {
  AsteroidsMain game;
  var player;

  GameCompleted(this.game)
      : super(false) {
    interval = new Interval("CONGRATULATIONS!", intervalRenderer);
    player = game.player;
  }

  bool isComplete() => true;

  void intervalRenderer(Interval interval, CanvasRenderingContext2D ctx) {
    if (interval.framecounter++ == 0) {
      if (game.score == game.highscore) {
        // save new high score to HTML5 local storage
        if (window.localStorage) {
          window.localStorage[SCOREDBKEY] = game.score;
        }
      }
    }
    if (interval.framecounter < 1000) {
      fillText(ctx, interval.label, "18pt Courier New",
          GameHandler.width ~/ 2 - 96, GameHandler.height ~/ 2 - 32, "white");
      fillText(ctx, "Score: ${game.score}", "14pt Courier New",
          GameHandler.width ~/ 2 - 64, GameHandler.height ~/ 2, "white");
      if (game.score == game.highscore) {
        fillText(ctx, "New High Score!", "14pt Courier New",
            GameHandler.width ~/ 2 - 64,
                GameHandler.height ~/ 2 + 24, "white");
      }
    } else {
      interval.complete = true;
    }
  }
}

/**
 * Game Handler.
 *
 * Singleton instance responsible for managing the main game loop and
 * maintaining a few global references such as the canvas and frame counters.
 */
class GameHandler {
   /**
    * The single Game.Main derived instance
    */
   static GameMain game = null;

   static bool paused = false;
   static CanvasElement canvas = null;
   static int width = 0;
   static int height = 0;
   static int frameCount = 0;

   /** Frame multiplier - i.e. against the ideal fps. */
   static double frameMultiplier = 1.0;

   /** Last frame start time in ms. */
   static int frameStart = 0;

   /** Debugging output. */
   static int maxfps = 0;

   /** Ideal FPS constant. */
   static const FPSMS = 1000 / 60;

   static Prerenderer bitmaps;

   /** Init function called once by your window.onload handler. */
   static void init(c) {
     canvas = c;
     width = canvas.width;
     height = canvas.height;
     log("Init GameMain($c,$width,$height)");
   }

   /**
    * Game start method - begins the main game loop.
    * Pass in the object that represent the game to execute.
    */
   static void start(GameMain g) {
     game = g;
     frameStart = new DateTime.now().millisecondsSinceEpoch;
     log("Doing first frame");
     game.frame();
   }

   /** Called each frame by the main game loop unless paused. */
   static void doFrame(_) {
     log("Doing next frame");
     game.frame();
   }

   static void togglePause() {
     if (paused) {
       paused = false;
       frameStart = new DateTime.now().millisecondsSinceEpoch;
       game.frame();
    } else {
      paused = true;
    }
  }

  static bool onAccelerometer(double x, double y, double z) {
    return game == null ? true : game.onAccelerometer(x, y, z);
  }
}

bool onAccelerometer(double x, double y, double z) {
  return GameHandler.onAccelerometer(x, y, z);
}

/** Game main loop class.  */
class GameMain {

  GameMain() {
    var me = this;

    document.onKeyDown.listen((KeyboardEvent event) {
      var keyCode = event.keyCode;

      log("In document.onKeyDown($keyCode)");
      if (me.sceneIndex != -1) {
        if (me.scenes[me.sceneIndex].onKeyDownHandler(keyCode) != null) {
          // if the key is handled, prevent any further events
          if (event != null) {
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    });

    document.onKeyUp.listen((KeyboardEvent event) {
      var keyCode =  event.keyCode;
      if (me.sceneIndex != -1) {
        if (me.scenes[me.sceneIndex].onKeyUpHandler(keyCode) != null) {
          // if the key is handled, prevent any further events
          if (event != null) {
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    });

    document.onMouseDown.listen((MouseEvent event) {
      if (me.sceneIndex != -1) {
        if (me.scenes[me.sceneIndex].onMouseDownHandler(event) != null) {
          // if the event is handled, prevent any further events
          if (event != null) {
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    });

    document.onMouseUp.listen((MouseEvent event) {
      if (me.sceneIndex != -1) {
        if (me.scenes[me.sceneIndex].onMouseUpHandler(event) != null) {
          // if the event is handled, prevent any further events
          if (event != null) {
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    });

  }

  List scenes = [];
  Scene startScene = null;
  Scene endScene = null;
  Scene currentScene = null;
  int sceneIndex = -1;
  var interval = null;
  int totalFrames = 0;

  bool onAccelerometer(double x, double y, double z) {
    if (currentScene != null) {
      return currentScene.onAccelerometer(x, y, z);
    }
    return true;
  }
  /**
   * Game frame execute method - called by anim handler timeout
   */
  void frame() {
    var frameStart = new DateTime.now().millisecondsSinceEpoch;

    // Calculate scene transition and current scene.
    if (currentScene == null) {
      // Set to scene zero (game init).
      currentScene = scenes[sceneIndex = 0];
      currentScene.onInitScene();
    } else if (isGameOver()) {
      sceneIndex = -1;
      currentScene = endScene;
      currentScene.onInitScene();
    }

    if ((currentScene.interval == null ||
        currentScene.interval.complete) && currentScene.isComplete()) {
      if (++sceneIndex >= scenes.length){
        sceneIndex = 0;
      }
      currentScene = scenes[sceneIndex];
      currentScene.onInitScene();
    }

    var ctx = GameHandler.canvas.getContext('2d');

    // Rrender the game and current scene.
    ctx.save();
    if (currentScene.interval == null || currentScene.interval.complete) {
      currentScene.onBeforeRenderScene();
      onRenderGame(ctx);
      currentScene.onRenderScene(ctx);
    } else {
      onRenderGame(ctx);
      currentScene.interval.intervalRenderer(currentScene.interval, ctx);
    }
    ctx.restore();

    GameHandler.frameCount++;

    // Calculate frame total time interval and frame multiplier required
    // for smooth animation.

    // Time since last frame.
    var frameInterval = frameStart - GameHandler.frameStart;
    if (frameInterval == 0) frameInterval = 1;
    if (GameHandler.frameCount % 16 == 0) { // Update fps every 16 frames
      GameHandler.maxfps = (1000 / frameInterval).floor().toInt();
    }
    GameHandler.frameMultiplier = frameInterval.toDouble() / GameHandler.FPSMS;

    GameHandler.frameStart = frameStart;

    if (!GameHandler.paused) {
      window.requestAnimationFrame(GameHandler.doFrame);
    }
    if ((++totalFrames % 600) == 0) {
      log('${totalFrames} frames; multiplier ${GameHandler.frameMultiplier}');
    }
  }

  void onRenderGame(CanvasRenderingContext2D ctx) {}

  bool isGameOver() => false;
}

class AsteroidsMain extends GameMain {

  AsteroidsMain() : super() {
    var attractorScene = new AttractorScene(this);

    // get the images graphics loading
    var loader = new Preloader();
    loader.addImage(_playerImg, 'player.png');
    loader.addImage(_asteroidImgs[0], 'asteroid1.png');
    loader.addImage(_asteroidImgs[1], 'asteroid2.png');
    loader.addImage(_asteroidImgs[2], 'asteroid3.png');
    loader.addImage(_asteroidImgs[3], 'asteroid4.png');
    loader.addImage(_shieldImg, 'shield.png');
    loader.addImage(_enemyshipImg, 'enemyship1.png');

    // The attactor scene is displayed first and responsible for allowing the
    // player to start the game once all images have been loaded.
    loader.onLoadCallback(() {
      attractorScene.ready();
    });

    // Generate the single player actor - available across all scenes.
    player = new Player(
        new Vector(GameHandler.width / 2, GameHandler.height / 2),
        new Vector(0.0, 0.0),
        0.0);

    scenes.add(attractorScene);

    for (var i = 0; i < 12; i++){
      var level = new GameScene(this, i+1);
      scenes.add(level);
    }

    scenes.add(new GameCompleted(this));

    // Set special end scene member value to a Game Over scene.
    endScene = new GameOverScene(this);

    if (window.localStorage.containsKey(SCOREDBKEY)) {
      highscore = int.parse(window.localStorage[SCOREDBKEY]);
    }
    // Perform prerender steps - create some bitmap graphics to use later.
    GameHandler.bitmaps = new Prerenderer();
    GameHandler.bitmaps.execute();
  }

  Player player = null;
  int lives = 0;
  int score = 0;
  int highscore = 0;
  /** Background scrolling bitmap x position */
  double backgroundX = 0.0;
  /** Background starfield star list */
  List starfield = [];

  void onRenderGame(CanvasRenderingContext2D ctx) {
    // Setup canvas for a render pass and apply background
    // draw a scrolling background image.
    var w = GameHandler.width;
    var h = GameHandler.height;
    //var sourceRect = new Rect(backgroundX, 0, w, h);
    //var destRect = new Rect(0, 0, w, h);
    //ctx.drawImageToRect(_backgroundImg, destRect,
    //    sourceRect:sourceRect);
    ctx.drawImageScaledFromSource(_backgroundImg,
        backgroundX, 0, w, h, 0, 0, w, h);

    backgroundX += (GameHandler.frameMultiplier / 4.0);
    if (backgroundX >= _backgroundImg.width / 2) {
      backgroundX -= _backgroundImg.width / 2;
    }
    ctx.shadowBlur = 0;
  }

  bool isGameOver() {
    if (currentScene is GameScene) {
      var gs = currentScene as GameScene;
      return (lives == 0 && gs.effects != null && gs.effects.length == 0);
    }
    return false;
  }

  /**
   * Update an actor position using its current velocity vector.
   * Scale the vector by the frame multiplier - this is used to ensure
   * all actors move the same distance over time regardles of framerate.
   * Also handle traversing out of the coordinate space and back again.
   */
  void updateActorPosition(Actor actor) {
    actor.position.add(actor.velocity.nscale(GameHandler.frameMultiplier));
    actor.position.wrap(0, GameHandler.width - 1, 0, GameHandler.height - 1);
  }
}

class GameOverScene extends Scene {
  var game, player;

  GameOverScene(this.game) :
    super(false) {
    interval = new Interval("GAME OVER", intervalRenderer);
    player = game.player;
  }

  bool isComplete() => true;

  void intervalRenderer(Interval interval, CanvasRenderingContext2D ctx) {
    if (interval.framecounter++ == 0) {
      if (game.score == game.highscore) {
        window.localStorage[SCOREDBKEY] = game.score.toString();
      }
    }
    if (interval.framecounter < 300) {
      fillText(ctx, interval.label, "18pt Courier New",
          GameHandler.width * 0.5 - 64, GameHandler.height*0.5 - 32, "white");
      fillText(ctx, "Score: ${game.score}", "14pt Courier New",
          GameHandler.width * 0.5 - 64, GameHandler.height*0.5, "white");
      if (game.score == game.highscore) {
        fillText(ctx, "New High Score!", "14pt Courier New",
            GameHandler.width * 0.5 - 64, GameHandler.height*0.5 + 24, "white");
      }
    } else {
      interval.complete = true;
    }
  }
}

class GameScene extends Scene {
  AsteroidsMain game;
  int wave;
  var player;
  List actors = null;
  List playerBullets = null;
  List enemies = null;
  List enemyBullets = null;
  List effects = null;
  List collectables = null;
  int enemyShipCount = 0;
  int enemyShipAdded = 0;
  int scoredisplay = 0;
  bool skipLevel = false;

  Input input;

  GameScene(this.game, this.wave)
      : super(true) {
    interval = new Interval("Wave ${wave}", intervalRenderer);
    player = game.player;
    input = new Input();
  }

  void onInitScene() {
    // Generate the actors and add the actor sub-lists to the main actor list.
    actors = [];
    enemies = [];
    actors.add(enemies);
    actors.add(playerBullets = []);
    actors.add(enemyBullets = []);
    actors.add(effects = []);
    actors.add(collectables = []);

    // Reset player ready for game restart.
    resetPlayerActor(wave != 1);

    // Randomly generate some asteroids.
    var factor = 1.0 + ((wave - 1) * 0.075);
    for (var i=1, j=(4 + wave); i < j; i++) {
      enemies.add(generateAsteroid(factor));
    }

    // Reset enemy ship count and last enemy added time.
    enemyShipAdded = GameHandler.frameStart;
    enemyShipCount = 0;

    // Reset interval flag.
    interval.reset();
    skipLevel = false;
  }

  /** Restore the player to the game - reseting position etc. */
  void resetPlayerActor(bool persistPowerUps) {
    actors.add([player]);

    // Reset the player position.
    player.position.x = GameHandler.width / 2;
    player.position.y = GameHandler.height / 2;
    player.velocity.x = 0.0;
    player.velocity.y = 0.0;
    player.heading = 0.0;
    player.reset(persistPowerUps);

    // Reset keyboard input values.
    input.reset();
  }

  /** Scene before rendering event handler. */
  void onBeforeRenderScene() {
    // Handle key input.
    if (input.left) {
      // Rotate anti-clockwise.
      player.heading -= 4 * GameHandler.frameMultiplier;
    }
    if (input.right) {
      // Rotate clockwise.
      player.heading += 4 * GameHandler.frameMultiplier;
    }
    if (input.thrust) {
      player.thrust();
    }
    if (input.shield) {
      if (!player.expired()) {
        player.activateShield();
      }
    }
    if (input.fireA) {
      player.firePrimary(playerBullets);
    }
    if (input.fireB) {
      player.fireSecondary(playerBullets);
    }

    // Add an enemy every N frames (depending on wave factor).
    // Later waves can have 2 ships on screen - earlier waves have one.
    if (enemyShipCount <= (wave < 5 ? 0 : 1) &&
        GameHandler.frameStart - enemyShipAdded > (20000 - (wave * 1024))) {
      enemies.add(new EnemyShip(this, (wave < 3 ? 0 : randomInt(0, 1))));
      enemyShipCount++;
      enemyShipAdded = GameHandler.frameStart;
    }

    // Update all actors using their current vector.
    updateActors();
  }

  /** Scene rendering event handler */
  void onRenderScene(CanvasRenderingContext2D ctx) {
    renderActors(ctx);

    if (Debug['collisionRadius']) {
      renderCollisionRadius(ctx);
    }

    // Render info overlay graphics.
    renderOverlay(ctx);

    // Detect bullet collisions.
    collisionDetectBullets();

    // Detect player collision with asteroids etc.
    if (!player.expired()) {
      collisionDetectPlayer();
    } else {
      // If the player died, then respawn after a short delay and
      // ensure that they do not instantly collide with an enemy.
      if (GameHandler.frameStart - player.killedOn > 3000) {
        // Perform a test to check no ememy is close to the player.
        var tooClose = false;
        var playerPos =
            new Vector(GameHandler.width * 0.5, GameHandler.height * 0.5);
        for (var i=0, j=this.enemies.length; i<j; i++) {
          var enemy = this.enemies[i];
          if (playerPos.distance(enemy.position) < 80) {
            tooClose = true;
            break;
          }
        }
        if (tooClose == false) {
          resetPlayerActor(false);
        }
      }
    }
  }

  bool isComplete() =>
      (skipLevel || (enemies.length == 0 && effects.length == 0));

  void intervalRenderer(Interval interval, CanvasRenderingContext2D ctx) {
    if (interval.framecounter++ < 100) {
      fillText(ctx, interval.label, "18pt Courier New",
          GameHandler.width*0.5 - 48, GameHandler.height*0.5 - 8, "white");
    } else {
      interval.complete = true;
    }
  }

  bool onAccelerometer(double x, double y, double z) {
    if (input != null) {
      input.shield =(x > 2.0);
      input.thrust = (x < -1.0);
      input.left = (y < -1.5);
      input.right = (y > 1.5);
    }
    return true;
  }

  bool onMouseDownHandler(e) {
    input.fireA = input.fireB = false;
    if (e.clientX < GameHandler.width / 3) input.fireB = true;
    else if (e.clientX > 2 * GameHandler.width / 3) input.fireA = true;
    return true;
  }

  bool onMouseUpHandler(e) {
    input.fireA = input.fireB = false;
    return true;
  }

  bool onKeyDownHandler(int keyCode) {
    log("In onKeyDownHandler, GameScene");
    switch (keyCode) {
      // Note: GLUT doesn't send key up events,
      // so the emulator sends key events as down/up pairs,
      // which is not what we want. So we have some special
      // numeric key handlers here that are distinct for
      // up and down to support use with GLUT.
      case 52: // '4':
      case Key.LEFT:
        input.left = true;
        return true;
      case 54: // '6'
      case Key.RIGHT:
        input.right = true;
        return true;
      case 56: // '8'
      case Key.UP:
        input.thrust = true;
        return true;
      case 50: // '2'
      case Key.DOWN:
      case Key.SHIFT:
        input.shield = true;
        return true;
      case 48: // '0'
      case Key.SPACE:
        input.fireA = true;
        return true;
      case Key.Z:
        input.fireB = true;
        return true;

      case Key.A:
        if (Debug['enabled']) {
          // generate an asteroid
          enemies.add(generateAsteroid(1));
          return true;
          }
        break;

      case Key.G:
        if (Debug['enabled']) {
          glowEffectOn = !glowEffectOn;
          return true;
        }
        break;

      case Key.L:
        if (Debug['enabled']) {
          skipLevel = true;
          return true;
        }
        break;

      case Key.E:
        if (Debug['enabled']) {
          enemies.add(new EnemyShip(this, randomInt(0, 1)));
          return true;
        }
        break;

      case Key.ESC:
        GameHandler.togglePause();
        return true;
    }
    return false;
  }

  bool onKeyUpHandler(int keyCode) {
    switch (keyCode) {
      case 53: // '5'
        input.left = false;
        input.right = false;
        input.thrust = false;
        input.shield = false;
        input.fireA = false;
        input.fireB = false;
        return true;

      case Key.LEFT:
        input.left = false;
        return true;
      case Key.RIGHT:
        input.right = false;
        return true;
      case Key.UP:
        input.thrust = false;
        return true;
      case Key.DOWN:
      case Key.SHIFT:
        input.shield = false;
        return true;
      case Key.SPACE:
        input.fireA = false;
        return true;
      case Key.Z:
        input.fireB = false;
        return true;
    }
    return false;
  }

  /**
   * Randomly generate a new large asteroid. Ensures the asteroid is not
   * generated too close to the player position!
   */
  Asteroid generateAsteroid(num speedFactor) {
    while (true){
      // perform a test to check it is not too close to the player
      var apos = new Vector(random()*GameHandler.width,
          random()*GameHandler.height);
      if (player.position.distance(apos) > 125) {
        var vec = new Vector( ((random()*2)-1)*speedFactor,
            ((random()*2)-1)*speedFactor );
        return new Asteroid(apos, vec, 4);
      }
    }
  }

  /** Update the actors position based on current vectors and expiration. */
  void updateActors() {
    for (var i = 0, j = this.actors.length; i < j; i++) {
      var actorList = this.actors[i];

      for (var n = 0; n < actorList.length; n++) {
        var actor = actorList[n];

        // call onUpdate() event for each actor
        actor.onUpdate(this);

        // expiration test first
        if (actor.expired()) {
          actorList.removeAt(n);
        } else {
          game.updateActorPosition(actor);
        }
      }
    }
  }

  /**
   * Perform the operation needed to destory the player.
   * Mark as killed as reduce lives, explosion effect and play sound.
   */
  void destroyPlayer() {
    // Player destroyed by enemy bullet - remove from play.
    player.kill();
    game.lives--;
    var boom =
        new PlayerExplosion(player.position.clone(), player.velocity.clone());
    effects.add(boom);
    soundManager.play('big_boom');
  }

  /**
   * Detect player collisions with various actor classes
   * including Asteroids, Enemies, bullets and collectables
   */
  void collisionDetectPlayer() {
    var playerRadius = player.radius;
    var playerPos = player.position;

    // Test circle intersection with each asteroid/enemy ship.
    for (var n = 0, m = enemies.length; n < m; n++) {
      var enemy = enemies[n];

      // Calculate distance between the two circles.
      if (playerPos.distance(enemy.position) <= playerRadius + enemy.radius) {
        // Collision detected.
        if (player.isShieldActive()) {
          // Remove thrust from the player vector due to collision.
          player.velocity.scale(0.75);

          // Destroy the enemy - the player is invincible with shield up!
          enemy.hit(-1);
          destroyEnemy(enemy, player.velocity, true);
        } else if (!Debug['invincible']) {
          destroyPlayer();
        }
      }
    }

    // Test intersection with each enemy bullet.
    for (var i = 0; i < enemyBullets.length; i++) {
      var bullet = enemyBullets[i];

      // Calculate distance between the two circles.
      if (playerPos.distance(bullet.position) <= playerRadius + bullet.radius) {
        // Collision detected.
        if (player.isShieldActive()) {
          // Remove this bullet from the actor list as it has been destroyed.
          enemyBullets.removeAt(i);
        } else if (!Debug['invincible']) {
          destroyPlayer();
        }
      }
    }

    // Test intersection with each collectable.
    for (var i = 0; i < collectables.length; i++) {
      var item = collectables[i];

      // Calculate distance between the two circles.
      if (playerPos.distance(item.position) <= playerRadius + item.radius) {
        // Collision detected - remove item from play and activate it.
        collectables.removeAt(i);
        item.collected(game, player, this);

        soundManager.play('powerup');
      }
    }
  }

  /** Detect bullet collisions with asteroids and enemy actors. */
  void collisionDetectBullets() {
    var i;
    // Collision detect player bullets with asteroids and enemies.
    for (i = 0; i < playerBullets.length; i++) {
      var bullet = playerBullets[i];
      var bulletRadius = bullet.radius;
      var bulletPos = bullet.position;

      // Test circle intersection with each enemy actor.
      var n, m = enemies.length, z;
      for (n = 0; n < m; n++) {
        var enemy = enemies[n];

        // Test the distance against the two radius combined.
        if (bulletPos.distance(enemy.position) <= bulletRadius + enemy.radius){
          // intersection detected!

          // Test for area effect bomb weapon.
          var effectRad = bullet.effectRadius;
          if (effectRad == 0) {
            // Impact the enemy with the bullet.
            if (enemy.hit(bullet.power)) {
              // Destroy the enemy under the bullet.
              destroyEnemy(enemy, bullet.velocity, true);
              // Randomly release a power up.
              generatePowerUp(enemy);
            } else {
              // Add a bullet impact particle effect to show the hit.
              var effect =
                  new PlayerBulletImpact(bullet.position, bullet.velocity);
              effects.add(effect);
            }
          } else {
            // Inform enemy it has been hit by a instant kill weapon.
            enemy.hit(-1);
            generatePowerUp(enemy);

            // Add a big explosion actor at the area weapon position and vector.
            var comboCount = 1;
            var boom = new Explosion(
                           bullet.position.clone(),
                           bullet.velocity.nscale(0.5), 5);
            effects.add(boom);

            // Destroy the enemy.
            destroyEnemy(enemy, bullet.velocity, true);

            // Wipe out nearby enemies under the weapon effect radius
            // take the length of the enemy actor list here - so we don't
            // kill off -all- baby asteroids - so some elements of the original
            // survive.
            for (var x = 0, z = this.enemies.length, e; x < z; x++) {
              e = enemies[x];

              // test the distance against the two radius combined
              if (bulletPos.distance(e.position) <= effectRad + e.radius) {
                e.hit(-1);
                generatePowerUp(e);
                destroyEnemy(e, bullet.velocity, true);
                comboCount++;
              }
            }

            // Special score and indicator for "combo" detonation.
            if (comboCount > 4) {
              // Score bonus based on combo size.
              var inc = comboCount * 1000 * wave;
              game.score += inc;

              // Generate a special effect indicator at the destroyed
              // enemy position.
              var vec = new Vector(0, -3.0);
              var effect = new ScoreIndicator(
                   new Vector(enemy.position.x,
                       enemy.position.y - (enemy.size * 8)),
                   vec.add(enemy.velocity.nscale(0.5)),
                   inc, 16, 'COMBO X ${comboCount}', 'rgb(255,255,55)', 1000);
              effects.add(effect);

              // Generate a powerup to reward the player for the combo.
              generatePowerUp(enemy, true);
            }
          }

          // Remove this bullet from the actor list as it has been destroyed.
          playerBullets.removeAt(i);
          break;
        }
      }
    }

    // collision detect enemy bullets with asteroids
    for (i = 0; i < enemyBullets.length; i++) {
      var bullet = enemyBullets[i];
      var bulletRadius = bullet.radius;
      var bulletPos = bullet.position;

      // test circle intersection with each enemy actor
      var n, m = enemies.length, z;
      for (n = 0; n < m; n++) {
        var enemy = enemies[n];

        if (enemy is Asteroid) {
          if (bulletPos.distance(enemy.position) <=
              bulletRadius + enemy.radius) {
            // Impact the enemy with the bullet.
            if (enemy.hit(1)) {
               // Destroy the enemy under the bullet.
               destroyEnemy(enemy, bullet.velocity, false);
            } else {
              // Add a bullet impact particle effect to show the hit.
              var effect = new EnemyBulletImpact(bullet.position,
                  bullet.velocity);
              effects.add(effect);
            }

            // Remove this bullet from the actor list as it has been destroyed.
            enemyBullets.removeAt(i);
            break;
          }
        }
      }
    }
  }

  /** Randomly generate a power up to reward the player */
  void generatePowerUp(EnemyActor enemy, [bool force = false]) {
    if (collectables.length < 5 &&
             (force || randomInt(0, ((enemy is Asteroid) ? 25 : 1)) == 0)) {
      // Apply a small random vector in the direction of travel
      // rotate by slightly randomized enemy heading.
      var vec = enemy.velocity.clone();
      var t = new Vector(0.0, -(random() * 2));
      t.rotate(enemy.velocity.theta() * (random() * Math.PI));
      vec.add(t);

      // Add a power up to the collectables list.
      collectables.add(new PowerUp(
               new Vector(enemy.position.x, enemy.position.y - (enemy.size * 8)),
               vec));
    }
  }

  /**
   * Blow up an enemy.
   *
   * An asteroid may generate new baby asteroids and leave an explosion
   * in the wake.
   *
   * Also applies the score for the destroyed item.
   *
   * @param enemy {Game.EnemyActor} The enemy to destory and add score for
   * @param parentVector {Vector} The vector of the item that hit the enemy
   * @param player {boolean} If true, the player was the destroyer
   */
  void destroyEnemy(EnemyActor enemy, Vector parentVector, player) {
    if (enemy is Asteroid) {
      soundManager.play('asteroid_boom${randomInt(1,4)}');

      // generate baby asteroids
      generateBabyAsteroids(enemy, parentVector);

      // add an explosion at the asteriod position and vector
      var boom = new AsteroidExplosion(
               enemy.position.clone(), enemy.velocity.clone(), enemy);
      effects.add(boom);

      if (player!= null) {
        // increment score based on asteroid size
        var inc = ((5 - enemy.size) * 4) * 100 * wave;
        game.score += inc;

        // generate a score effect indicator at the destroyed enemy position
        var vec = new Vector(0, -1.5).add(enemy.velocity.nscale(0.5));
        var effect = new ScoreIndicator(
                     new Vector(enemy.position.x, enemy.position.y -
                         (enemy.size * 8)), vec, inc);
        effects.add(effect);
      }
    } else if (enemy is EnemyShip) {
      soundManager.play('asteroid_boom1');

      // add an explosion at the enemy ship position and vector
      var boom = new EnemyExplosion(enemy.position.clone(),
          enemy.velocity.clone(), enemy);
      effects.add(boom);

      if (player != null) {
        // increment score based on asteroid size
        var inc = 2000 * wave * (enemy.size + 1);
        game.score += inc;

        // generate a score effect indicator at the destroyed enemy position
        var vec = new Vector(0, -1.5).add(enemy.velocity.nscale(0.5));
        var effect = new ScoreIndicator(
                     new Vector(enemy.position.x, enemy.position.y - 16),
                     vec, inc);
        effects.add(effect);
      }

      // decrement scene ship count
      enemyShipCount--;
    }
  }

  /**
   * Generate a number of baby asteroids from a detonated parent asteroid.
   * The number and size of the generated asteroids are based on the parent
   * size. Some of the momentum of the parent vector (e.g. impacting bullet)
   * is applied to the new asteroids.
   */
  void generateBabyAsteroids(Asteroid asteroid, Vector parentVector) {
    // generate some baby asteroid(s) if bigger than the minimum size
    if (asteroid.size > 1) {
      var xc=randomInt(asteroid.size ~/ 2, asteroid.size - 1);
      for (var x=0; x < xc; x++) {
        var babySize = randomInt(1, asteroid.size - 1);

        var vec = asteroid.velocity.clone();

        // apply a small random vector in the direction of travel
        var t = new Vector(0.0, -random());

        // rotate vector by asteroid current heading - slightly randomized
        t.rotate(asteroid.velocity.theta() * (random() * Math.PI));
        vec.add(t);

        // add the scaled parent vector - to give some momentum from the impact
        vec.add(parentVector.nscale(0.2));

        // create the asteroid - slightly offset from the centre of the old one
        var baby = new Asteroid(
                 new Vector(asteroid.position.x + (random()*5)-2.5,
                     asteroid.position.y + (random()*5)-2.5),
                 vec, babySize, asteroid.type);
        enemies.add(baby);
      }
    }
  }

  /** Render each actor to the canvas. */
  void renderActors(CanvasRenderingContext2D ctx){
    for (var i = 0, j = actors.length; i < j; i++) {
      // walk each sub-list and call render on each object
      var actorList = actors[i];

      for (var n = actorList.length - 1; n >= 0; n--) {
        actorList[n].onRender(ctx);
      }
    }
  }

  /**
   * DEBUG - Render the radius of the collision detection circle around
   * each actor.
   */
  void renderCollisionRadius(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.strokeStyle = "rgb(255,0,0)";
    ctx.lineWidth = 0.5;
    ctx.shadowBlur = 0;

    for (var i = 0, j = actors.length; i < j; i++) {
      var actorList = actors[i];

      for (var n = actorList.length - 1, actor; n >= 0; n--) {
        actor = actorList[n];
        if (actor.radius) {
          ctx.beginPath();
          ctx.arc(actor.position.x, actor.position.y, actor.radius, 0,
              TWOPI, true);
          ctx.closePath();
          ctx.stroke();
        }
      }
    }
    ctx.restore();
  }

  /**
   * Render player information HUD overlay graphics.
   *
   * @param ctx {object} Canvas rendering context
   */
  void renderOverlay(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.shadowBlur = 0;

    // energy bar (100 pixels across, scaled down from player energy max)
    ctx.strokeStyle = "rgb(50,50,255)";
    ctx.strokeRect(4, 4, 101, 6);
    ctx.fillStyle = "rgb(100,100,255)";
    var energy = player.energy;
    if (energy > player.ENERGY_INIT) {
      // the shield is on for "free" briefly when he player respawns
      energy = player.ENERGY_INIT;
    }
    ctx.fillRect(5, 5, (energy / (player.ENERGY_INIT / 100)), 5);

    // lives indicator graphics
    for (var i=0; i<game.lives; i++) {
      drawScaledImage(ctx, _playerImg, 0, 0, 64,
          350+(i*20), 0, 16);

      // score display - update towards the score in increments to animate it
      var score = game.score;
      var inc = (score - scoredisplay) ~/ 10;
      scoredisplay += inc;
      if (scoredisplay > score) {
        scoredisplay = score;
      }
      var sscore = scoredisplay.ceil().toString();
      // pad with zeros
      for (var i=0, j=8-sscore.length; i<j; i++) {
        sscore = "0${sscore}";
      }
      fillText(ctx, sscore, "12pt Courier New", 120, 12, "white");

      // high score
      // TODO: add method for incrementing score so this is not done here
      if (score > game.highscore) {
        game.highscore = score;
      }
      sscore = game.highscore.toString();
      // pad with zeros
      for (var i=0, j=8-sscore.length; i<j; i++) {
        sscore = "0${sscore}";
      }
      fillText(ctx, "HI: ${sscore}", "12pt Courier New", 220, 12, "white");

      // debug output
      if (Debug['fps']) {
        fillText(ctx, "FPS: ${GameHandler.maxfps}", "12pt Courier New",
            0, GameHandler.height - 2, "lightblue");
      }
    }
    ctx.restore();
  }
}

class Interval {
  String label;
  Function intervalRenderer;
  int framecounter = 0;
  bool complete = false;

  Interval([this.label = null, this.intervalRenderer = null]);

  void reset() {
    framecounter = 0;
    complete = false;
  }
}

class Bullet extends ShortLivedActor {

  Bullet(Vector position, Vector velocity,
      [this.heading = 0.0, int lifespan = 1300])
      : super(position, velocity, lifespan) {
  }

  const BULLET_WIDTH = 2;
  const BULLET_HEIGHT = 6;
  const FADE_LENGTH = 200;

  double heading;
  int _power = 1;

  void onRender(CanvasRenderingContext2D ctx) {
    // hack to stop draw under player graphic
    if (frameStart - start > 40) {
      ctx.save();
      ctx.globalCompositeOperation = "lighter";
      ctx.globalAlpha = fadeValue(1.0, FADE_LENGTH);
      // rotate the bullet bitmap into the correct heading
      ctx.translate(position.x, position.y);
      ctx.rotate(heading * RAD);
      // TODO(gram) - figure out how to get rid of the vector art so we don't
      // need the [0] below.
      ctx.drawImage(GameHandler.bitmaps.images["bullet"],
          -(BULLET_WIDTH + GLOWSHADOWBLUR*2)*0.5,
          -(BULLET_HEIGHT + GLOWSHADOWBLUR*2)*0.5);
      ctx.restore();
    }
  }

  /** Area effect weapon radius - zero for primary bullets. */
  get effectRadius => 0;

  // approximate based on average between width and height
  get radius => 4;

  get power => _power;
}

/**
 * Player BulletX2 actor class. Used by the TwinCannons primary weapon.
 */
class BulletX2 extends Bullet {

  BulletX2(Vector position, Vector vector, double heading)
      : super(position, vector, heading, 1750) {
    _power = 2;
  }

  void onRender(CanvasRenderingContext2D ctx) {
    // hack to stop draw under player graphic
    if (frameStart - start > 40) {
      ctx.save();
      ctx.globalCompositeOperation = "lighter";
      ctx.globalAlpha = fadeValue(1.0, FADE_LENGTH);
      // rotate the bullet bitmap into the correct heading
      ctx.translate(position.x, position.y);
      ctx.rotate(heading * RAD);
      ctx.drawImage(GameHandler.bitmaps.images["bulletx2"],
           -(BULLET_WIDTH + GLOWSHADOWBLUR*4) / 2,
           -(BULLET_HEIGHT + GLOWSHADOWBLUR*2) / 2);
      ctx.restore();
    }
  }

  get radius => BULLET_HEIGHT;
}

class Bomb extends Bullet {
  Bomb(Vector position, Vector velocity)
  : super(position, velocity, 0.0, 3000);

  const BOMB_RADIUS = 4.0;
  const FADE_LENGTH = 200;
  const EFFECT_RADIUS = 45;

  void onRender(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.globalCompositeOperation = "lighter";
    ctx.globalAlpha = fadeValue(1.0, FADE_LENGTH);
    ctx.translate(position.x, position.y);
    ctx.rotate((frameStart % (360*32)) / 32);
    var scale = fadeValue(1.0, FADE_LENGTH);
    if (scale <= 0) scale = 0.01;
    ctx.scale(scale, scale);
    ctx.drawImage(GameHandler.bitmaps.images["bomb"],
               -(BOMB_RADIUS + GLOWSHADOWBLUR),
               -(BOMB_RADIUS + GLOWSHADOWBLUR));
    ctx.restore();
  }

  get effectRadius => EFFECT_RADIUS;
  get radius => fadeValue(BOMB_RADIUS, FADE_LENGTH);
}

class EnemyBullet extends Bullet {
  EnemyBullet(Vector position, Vector velocity)
      : super(position, velocity, 0.0, 2800);

  const BULLET_RADIUS = 4.0;
  const FADE_LENGTH = 200;

  void onRender(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.globalAlpha = fadeValue(1.0, FADE_LENGTH);
    ctx.globalCompositeOperation = "lighter";
    ctx.translate(position.x, position.y);
    ctx.rotate((frameStart % (360*64)) / 64);
    var scale = fadeValue(1.0, FADE_LENGTH);
    if (scale <= 0) scale = 0.01;
    ctx.scale(scale, scale);
    ctx.drawImage(GameHandler.bitmaps.images["enemybullet"],
               -(BULLET_RADIUS + GLOWSHADOWBLUR),
               -(BULLET_RADIUS + GLOWSHADOWBLUR));
    ctx.restore();
  }

  get radius => fadeValue(BULLET_RADIUS, FADE_LENGTH) + 1;
}

class Particle extends ShortLivedActor {
  int size;
  int type;
  int fadelength;
  String color;
  double rotate;
  double rotationv;

  Particle(Vector position, Vector velocity, this.size, this.type,
                  int lifespan, this.fadelength,
                  [this.color = Colors.PARTICLE])
      : super(position, velocity, lifespan) {

    // randomize rotation speed and angle for line particle
    if (type == 1) {
      rotate = random() * TWOPI;
      rotationv = (random() - 0.5) * 0.5;
    }
  }

  bool update() {
    position.add(velocity);
    return !expired();
  }

  void render(CanvasRenderingContext2D ctx) {
    ctx.globalAlpha = fadeValue(1.0, fadelength);
    switch (type) {
      case 0:  // point (prerendered image)
          ctx.translate(position.x, position.y);
          ctx.drawImage(
             GameHandler.bitmaps.images["points_${color}"][size], 0, 0);
          break;
         // TODO: prerender a glowing line to use as the particle!
       case 1:  // line
          ctx.translate(position.x, position.y);
          var s = size;
          ctx.rotate(rotate);
          this.rotate += rotationv;
          ctx.strokeStyle = color;
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.moveTo(-s, -s);
          ctx.lineTo(s, s);
          ctx.closePath();
          ctx.stroke();
          break;
       case 2:  // smudge (prerendered image)
          var offset = (size + 1) << 2;
          renderImage(ctx,
              GameHandler.bitmaps.images["smudges_${color}"][size],
              0, 0, (size + 1) << 3,
              position.x - offset, position.y - offset, (size + 1) << 3);
        break;
    }
  }
}

/**
 * Particle emitter effect actor class.
 *
 * A simple particle emitter, that does not recycle particles, but sets itself
 * as expired() once all child particles have expired.
 *
 * Requires a function known as the emitter that is called per particle
 * generated.
 */
class ParticleEmitter extends Actor {

  List<Particle> particles;

  ParticleEmitter(Vector position, Vector velocity)
  : super(position, velocity);

  Particle emitter() {}

  void init(count) {
    // generate particles based on the supplied emitter function
    particles = [];
    for (var i = 0; i < count; i++) {
      particles.add(emitter());
    }
  }

  void onRender(CanvasRenderingContext2D ctx) {
    ctx.save();
    ctx.shadowBlur = 0;
    ctx.globalCompositeOperation = "lighter";
    for (var i=0, particle; i < particles.length; i++) {
      particle = particles[i];

      // update particle and test for lifespan
      if (particle.update()) {
        ctx.save();
        particle.render(ctx);
        ctx.restore();
      } else {
        // particle no longer alive, remove from list
        particles.removeAt(i);
      }
    }
    ctx.restore();
  }

  bool expired() => (particles.length == 0);
}

class AsteroidExplosion extends ParticleEmitter {
  var asteroid;

  AsteroidExplosion(Vector position, Vector vector, this.asteroid)
      : super(position, vector) {
    init(asteroid.size*2);
  }

  Particle emitter() {
    // Randomise radial direction vector - speed and angle, then add parent
    // vector.
    var pos = position.clone();
    if (random() < 0.5) {
      var t = new Vector(0, randomInt(5, 10));
      t.rotate(random() * TWOPI).add(velocity);
      return new Particle(pos, t, (random() * 4).floor(), 0, 400, 300);
    } else {
      var t = new Vector(0, randomInt(1, 3));
      t.rotate(random() * TWOPI).add(velocity);
      return new Particle(pos, t,
          (random() * 4).floor() + asteroid.size, 2, 500, 250);
    }
  }
}

class PlayerExplosion extends ParticleEmitter {
  PlayerExplosion(Vector position, Vector vector)
      : super(position, vector) {
    init(12);
  }

  Particle emitter() {
    // Randomise radial direction vector - speed and angle, then add
    // parent vector.
    var pos = position.clone();
    if (random() < 0.5){
      var t = new Vector(0, randomInt(5, 10));
      t.rotate(random() * TWOPI).add(velocity);
      return new Particle(pos, t, (random() * 4).floor(), 0, 400, 300);
    } else {
      var t = new Vector(0, randomInt(1, 3));
      t.rotate(random() * TWOPI).add(velocity);
      return new Particle(pos, t, (random() * 4).floor() + 2, 2, 500, 250);
    }
  }
}

/** Enemy particle based explosion - Particle effect actor class. */
class EnemyExplosion extends ParticleEmitter {
  var enemy;
  EnemyExplosion(Vector position, Vector vector, this.enemy)
      : super(position, vector) {
    init(8);
  }

  Particle emitter() {
   // randomise radial direction vector - speed and angle, then
   // add parent vector.
   var pos = position.clone();
   if (random() < 0.5) {
     var t = new Vector(0, randomInt(5, 10));
     t.rotate(random() * TWOPI).add(velocity);
     return new Particle(pos, t, (random() * 4).floor(), 0,
         400, 300, Colors.ENEMY_SHIP);
   } else {
     var t = new Vector(0, randomInt(1, 3));
     t.rotate(random() * 2 * TWOPI).add(velocity);
     return new Particle(pos, t,
         (random() * 4).floor() + (enemy.size == 0 ? 2 : 0), 2,
         500, 250, Colors.ENEMY_SHIP);
   }
 }
}

class Explosion extends EffectActor {
/**
 * Basic explosion effect actor class.
 *
 * TODO: replace all instances of this with particle effects
 *  - this is still usedby the smartbomb
 */
  Explosion(Vector position, Vector vector, this.size)
      : super(position, vector, FADE_LENGTH);

  static const FADE_LENGTH = 300;

  num size = 0;

  void onRender(CanvasRenderingContext2D ctx) {
    // fade out
    var brightness = (effectValue(255.0)).floor(),
             rad = effectValue(size * 8.0),
             rgb = brightness.toString();
    ctx.save();
    ctx.globalAlpha = 0.75;
    ctx.fillStyle = "rgb(${rgb},0,0)";
    ctx.beginPath();
    ctx.arc(position.x, position.y, rad, 0, TWOPI, true);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  }
}

/**
 * Player bullet impact effect - Particle effect actor class.
 * Used when an enemy is hit by player bullet but not destroyed.
 */
class PlayerBulletImpact extends ParticleEmitter {
  PlayerBulletImpact(Vector position, Vector vector)
      : super(position, vector) {
    init(5);
  }

  Particle emitter() {
    // slightly randomise vector angle - then add parent vector
    var t = velocity.nscale(0.75 + random() * 0.5);
    t.rotate(random() * PIO4 - PIO8);
    return new Particle(position.clone(), t,
        (random() * 4).floor(), 0, 250, 150, Colors.GREEN_LASER);
  }
}

/**
 * Enemy bullet impact effect - Particle effect actor class.
 * Used when an enemy is hit by player bullet but not destroyed.
 */
class EnemyBulletImpact extends ParticleEmitter {
  EnemyBulletImpact(Vector position , Vector vector)
      : super(position, vector) {
    init(5);
  }

  Particle emitter() {
    // slightly randomise vector angle - then add parent vector
    var t = velocity.nscale(0.75 + random() * 0.5);
    t.rotate(random() * PIO4 - PIO8);
    return new Particle(position.clone(), t,
        (random() * 4).floor(), 0, 250, 150, Colors.ENEMY_SHIP);
  }
}

class Player extends SpriteActor {
  Player(Vector position, Vector vector, this.heading)
       : super(position, vector) {
     energy = ENERGY_INIT;

     // setup SpriteActor values - used for shield sprite
     animImage = _shieldImg;
     animLength = SHIELD_ANIM_LENGTH;

     // setup weapons
     primaryWeapons = {};
  }

  const MAX_PLAYER_VELOCITY = 8.0;
  const PLAYER_RADIUS = 9;
  const SHIELD_RADIUS = 14;
  const SHIELD_ANIM_LENGTH = 100;
  const SHIELD_MIN_PULSE = 20;
  const ENERGY_INIT = 400;
  const THRUST_DELAY_MS = 100;
  const BOMB_RECHARGE_MS = 800;
  const BOMB_ENERGY = 80;

  double heading = 0.0;

  /** Player energy (shield and bombs). */
  num energy = 0;

  /** Player shield active counter. */
  num shieldCounter = 0;

  bool alive = true;
  Map primaryWeapons = null;

  /** Bomb fire recharging counter. */
  num bombRecharge = 0;

  /** Engine thrust recharge counter. */
  num thrustRecharge = 0;

  /** True if the engine thrust graphics should be rendered next frame. */
  bool engineThrust = false;

  /**
   * Time that the player was killed - to cause a delay before respawning
   * the player
   */
  num killedOn = 0;

  bool fireWhenShield = false;

  /** Player rendering method
   *
   * @param ctx {object} Canvas rendering context
   */
  void onRender(CanvasRenderingContext2D ctx) {
    var headingRad = heading * RAD;

    // render engine thrust?
    if (engineThrust) {
      ctx.save();
      ctx.translate(position.x, position.y);
      ctx.rotate(headingRad);
      ctx.globalAlpha = 0.5 + random() * 0.5;
      ctx.globalCompositeOperation = "lighter";
      ctx.fillStyle = Colors.PLAYER_THRUST;
      ctx.beginPath();
      ctx.moveTo(-5, 8);
      ctx.lineTo(5, 8);
      ctx.lineTo(0, 18 + random() * 6);
      ctx.closePath();
      ctx.fill();
      ctx.restore();
      engineThrust = false;
    }

    // render player graphic
    var size = (PLAYER_RADIUS * 2) + 6;
    // normalise the player heading to 0-359 degrees
    // then locate the correct frame in the sprite strip -
    // an image for each 4 degrees of rotation
    var normAngle = heading.floor() % 360;
    if (normAngle < 0) {
       normAngle = 360 + normAngle;
    }
    ctx.save();
    drawScaledImage(ctx, _playerImg,
        0, (normAngle / 4).floor() * 64, 64,
        position.x - (size / 2), position.y - (size / 2), size);
    ctx.restore();

    // shield up? if so render a shield graphic around the ship
    if (shieldCounter > 0 && energy > 0) {
      // render shield graphic bitmap
      ctx.save();
      ctx.translate(position.x, position.y);
      ctx.rotate(headingRad);
      renderSprite(ctx, -SHIELD_RADIUS-1,
          -SHIELD_RADIUS-1, (SHIELD_RADIUS * 2) + 2);
      ctx.restore();

      shieldCounter--;
      energy -= 1.5;
    }
  }

  /** Execute player forward thrust request. */
  void thrust() {
    // now test we did not thrust too recently, based on time since last thrust
    // request - ensures same thrust at any framerate
    if (frameStart - thrustRecharge > THRUST_DELAY_MS) {
      // update last thrust time
      thrustRecharge = frameStart;

      // generate a small thrust vector
      var t = new Vector(0.0, -0.5);

      // rotate thrust vector by player current heading
      t.rotate(heading * RAD);

      // add player thrust vector to position
      velocity.add(t);

      // player can't exceed maximum velocity - scale vector down if
      // this occurs - do this rather than not adding the thrust at all
      // otherwise the player cannot turn and thrust at max velocity
      if (velocity.length() > MAX_PLAYER_VELOCITY) {
        velocity.scale(MAX_PLAYER_VELOCITY / velocity.length());
      }
    }
    // mark so that we know to render engine thrust graphics
    engineThrust = true;
  }

  /**
   * Execute player active shield request.
   * If energy remaining the shield will be briefly applied.
   */
  void activateShield() {
    // ensure shield stays up for a brief pulse between key presses!
    if (energy >= SHIELD_MIN_PULSE) {
      shieldCounter = SHIELD_MIN_PULSE;
    }
  }

  bool isShieldActive() => (shieldCounter > 0 && energy > 0);

  get radius => (isShieldActive() ? SHIELD_RADIUS : PLAYER_RADIUS);

  bool expired() => !(alive);

  void kill() {
    alive = false;
    killedOn = frameStart;
  }

   /** Fire primary weapon(s). */

  void firePrimary(List bulletList) {
    var playedSound = false;
    // attempt to fire the primary weapon(s)
    // first ensure player is alive and the shield is not up
    if (alive && (!isShieldActive() || fireWhenShield)) {
      for (var w in primaryWeapons.keys) {
        var b = primaryWeapons[w].fire();
        if (b != null) {
          for (var i=0; i<b.length; i++) {
            bulletList.add(b[i]);
          }
          if (!playedSound) {
            soundManager.play('laser');
            playedSound = true;
          }
        }
      }
    }
  }

  /**
   * Fire secondary weapon.
   * @param bulletList {Array} to add bullet to on success
   */
  void fireSecondary(List bulletList) {
    // Attempt to fire the secondary weapon and generate bomb object if
    // successful. First ensure player is alive and the shield is not up.
    if (alive && (!isShieldActive() || fireWhenShield) && energy > BOMB_ENERGY){
      // now test we did not fire too recently
      if (frameStart - bombRecharge > BOMB_RECHARGE_MS) {
        // ok, update last fired time and we can now generate a bomb
        bombRecharge = frameStart;

        // decrement energy supply
        energy -= BOMB_ENERGY;

        // generate a vector rotated to the player heading and then add the
        // current player vector to give the bomb the correct directional
        // momentum.
        var t = new Vector(0.0, -3.0);
        t.rotate(heading * RAD);
        t.add(velocity);

        bulletList.add(new Bomb(position.clone(), t));
      }
    }
  }

  void onUpdate(_) {
    // slowly recharge the shield - if not active
    if (!isShieldActive() && energy < ENERGY_INIT) {
      energy += 0.1;
    }
  }

  void reset(bool persistPowerUps) {
    // reset energy, alive status, weapons and power up flags
    alive = true;
    if (!persistPowerUps) {
      primaryWeapons = {};
      primaryWeapons["main"] = new PrimaryWeapon(this);
      fireWhenShield = false;
    }
    energy = ENERGY_INIT + SHIELD_MIN_PULSE;  // for shield as below

    // active shield briefly
    activateShield();
  }
}

/**
 * Image Preloader class. Executes the supplied callback function once all
 * registered images are loaded by the browser.
 */
class Preloader {
  Preloader() {
    images = new List();
  }

  /**
   * Image list
   *
   * @property images
   * @type Array
   */
  var images = [];

  /**
   * Callback function
   *
   * @property callback
   * @type Function
   */
  var callback = null;

  /**
   * Images loaded so far counter
   */
  var counter = 0;

  /**
   * Add an image to the list of images to wait for
   */
  void addImage(ImageElement img, String url) {
    var me = this;
    img.src = url;
    // attach closure to the image onload handler
    img.onLoad.listen((_) {
      me.counter++;
      if (me.counter == me.images.length) {
        // all images are loaded - execute callback function
        me.callback();
      }
    });
    images.add(img);
  }

  /**
   * Load the images and call the supplied function when ready
   */
  void onLoadCallback(Function fn) {
    counter = 0;
    callback = fn;
    // load the images
    //for (var i=0, j = images.length; i<j; i++) {
     // images[i].src = images[i].url;
    //}
  }
}

/**
 * Game prerenderer class.
 */
class GamePrerenderer {
  GamePrerenderer();

 /**
  * Image list. Keyed by renderer ID - returning an array also. So to get
  * the first image output by prerenderer with id "default":
  * images["default"][0]
  */
  Map images = {};
  Map _renderers = {};

  /** Add a renderer function to the list of renderers to execute. */
  addRenderer(Function fn, String id) => _renderers[id] = fn;


  /** Execute all prerender functions. */
  void execute() {
    var buffer = new CanvasElement();
    for (var id in _renderers.keys) {
      images[id] = _renderers[id](buffer);
    }
  }
}

/**
 * Asteroids prerenderer class.
 *
 * Encapsulates the early rendering of various effects used in the game. Each
 * effect is rendered once to a hidden canvas object, the image data is
 * extracted and stored in an Image object - which can then be reused later.
 * This is much faster than rendering each effect again and again at runtime.
 *
 * The downside to this is that some constants are duplicated here and in the
 * original classes - so updates to the original classes such as the weapon
 * effects must be duplicated here.
 */
class Prerenderer extends GamePrerenderer {
  Prerenderer() : super() {

    // function to generate a set of point particle images
    var fnPointRenderer = (CanvasElement buffer, String color) {
      var imgs = [];
      for (var size = 3; size <= 6; size++) {
        var width = size << 1;
        buffer.width = buffer.height = width;
        CanvasRenderingContext2D ctx = buffer.getContext('2d');
        var radgrad = ctx.createRadialGradient(size, size, size >> 1,
            size, size, size);
        radgrad.addColorStop(0, color);
        radgrad.addColorStop(1, "#000");
        ctx.fillStyle = radgrad;
        ctx.fillRect(0, 0, width, width);
        var img = new ImageElement();
        img.src = buffer.toDataUrl("image/png");
        imgs.add(img);
      }
      return imgs;
    };

    // add the various point particle image prerenderers based on above function
    // default explosion color
    addRenderer((CanvasElement buffer) {
      return fnPointRenderer(buffer, Colors.PARTICLE);
    }, "points_${Colors.PARTICLE}");

    // player bullet impact particles
    addRenderer((CanvasElement buffer) {
      return fnPointRenderer(buffer, Colors.GREEN_LASER);
    }, "points_${Colors.GREEN_LASER}");

    // enemy bullet impact particles
    addRenderer((CanvasElement buffer) {
      return fnPointRenderer(buffer, Colors.ENEMY_SHIP);
    }, "points_${Colors.ENEMY_SHIP}");

    // add the smudge explosion particle image prerenderer
    var fnSmudgeRenderer = (CanvasElement buffer, String color) {
      var imgs = [];
      for (var size = 4; size <= 32; size += 4) {
        var width = size << 1;
        buffer.width = buffer.height = width;
        CanvasRenderingContext2D ctx = buffer.getContext('2d');
        var radgrad = ctx.createRadialGradient(size, size, size >> 3,
            size, size, size);
        radgrad.addColorStop(0, color);
        radgrad.addColorStop(1, "#000");
        ctx.fillStyle = radgrad;
        ctx.fillRect(0, 0, width, width);
        var img = new ImageElement();
        img.src = buffer.toDataUrl("image/png");
        imgs.add(img);
      }
      return imgs;
    };

    addRenderer((CanvasElement buffer) {
      return fnSmudgeRenderer(buffer, Colors.PARTICLE);
    }, "smudges_${Colors.PARTICLE}");

    addRenderer((CanvasElement buffer) {
      return fnSmudgeRenderer(buffer, Colors.ENEMY_SHIP);
    }, "smudges_${Colors.ENEMY_SHIP}");

    // standard player bullet
    addRenderer((CanvasElement buffer) {
    //   NOTE: keep in sync with Asteroids.Bullet
      var BULLET_WIDTH = 2, BULLET_HEIGHT = 6;
      var imgs = [];
      buffer.width = BULLET_WIDTH + GLOWSHADOWBLUR*2;
      buffer.height = BULLET_HEIGHT + GLOWSHADOWBLUR*2;
      CanvasRenderingContext2D ctx = buffer.getContext('2d');

      var rf = (width, height) {
        ctx.beginPath();
        ctx.moveTo(0, height);
        ctx.lineTo(width, 0);
        ctx.lineTo(0, -height);
        ctx.lineTo(-width, 0);
        ctx.closePath();
      };

      ctx.shadowBlur = GLOWSHADOWBLUR;
      ctx.translate(buffer.width * 0.5, buffer.height * 0.5);
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASER_DARK;
      rf(BULLET_WIDTH-1, BULLET_HEIGHT-1);
      ctx.fill();
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASER;
      rf(BULLET_WIDTH, BULLET_HEIGHT);
      ctx.fill();
      var img = new ImageElement();
      img.src = buffer.toDataUrl("image/png");
      return img;
    }, "bullet");

    // player bullet X2
    addRenderer((CanvasElement buffer) {
      // NOTE: keep in sync with Asteroids.BulletX2
      var BULLET_WIDTH = 2, BULLET_HEIGHT = 6;
      buffer.width = BULLET_WIDTH + GLOWSHADOWBLUR*4;
      buffer.height = BULLET_HEIGHT + GLOWSHADOWBLUR*2;
      CanvasRenderingContext2D ctx = buffer.getContext('2d');

      var rf = (width, height) {
        ctx.beginPath();
        ctx.moveTo(0, height);
        ctx.lineTo(width, 0);
        ctx.lineTo(0, -height);
        ctx.lineTo(-width, 0);
        ctx.closePath();
      };

      ctx.shadowBlur = GLOWSHADOWBLUR;
      ctx.translate(buffer.width * 0.5, buffer.height * 0.5);
      ctx.save();
      ctx.translate(-4, 0);
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASERX2_DARK;
      rf(BULLET_WIDTH-1, BULLET_HEIGHT-1);
      ctx.fill();
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASERX2;
      rf(BULLET_WIDTH, BULLET_HEIGHT);
      ctx.fill();
      ctx.translate(8, 0);
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASERX2_DARK;
      rf(BULLET_WIDTH-1, BULLET_HEIGHT-1);
      ctx.fill();
      ctx.shadowColor = ctx.fillStyle = Colors.GREEN_LASERX2;
      rf(BULLET_WIDTH, BULLET_HEIGHT);
      ctx.fill();
      ctx.restore();
      var img = new ImageElement();
      img.src = buffer.toDataUrl("image/png");
      return img;
    }, "bulletx2");

    // player bomb weapon
    addRenderer((CanvasElement buffer) {
      // NOTE: keep in sync with Asteroids.Bomb
      var BOMB_RADIUS = 4;
      buffer.width = buffer.height = BOMB_RADIUS*2 + GLOWSHADOWBLUR*2;
      CanvasRenderingContext2D ctx = buffer.getContext('2d');

      var rf = () {
        ctx.beginPath();
        ctx.moveTo(BOMB_RADIUS * 2, 0);
        for (var i = 0; i < 15; i++) {
          ctx.rotate(PIO8);
          if (i % 2 == 0) {
            ctx.lineTo((BOMB_RADIUS * 2 / 0.525731) * 0.200811, 0);
          } else {
            ctx.lineTo(BOMB_RADIUS * 2, 0);
          }
        }
        ctx.closePath();
      };

      ctx.shadowBlur = GLOWSHADOWBLUR;
      ctx.shadowColor = ctx.fillStyle = Colors.PLAYER_BOMB;
      ctx.translate(buffer.width * 0.5, buffer.height * 0.5);
      rf();
      ctx.fill();

      var img = new ImageElement();
      img.src = buffer.toDataUrl("image/png");
      return img;
    }, "bomb");

  //enemy weapon
  addRenderer((CanvasElement buffer) {
    // NOTE: keep in sync with Asteroids.EnemyBullet
    var BULLET_RADIUS = 4;
    var imgs = [];
    buffer.width = buffer.height = BULLET_RADIUS*2 + GLOWSHADOWBLUR*2;
    CanvasRenderingContext2D ctx = buffer.getContext('2d');

    var rf = () {
      ctx.beginPath();
      ctx.moveTo(BULLET_RADIUS * 2, 0);
      for (var i=0; i<7; i++) {
        ctx.rotate(PIO4);
        if (i % 2 == 0) {
          ctx.lineTo((BULLET_RADIUS * 2/0.525731) * 0.200811, 0);
        } else {
          ctx.lineTo(BULLET_RADIUS * 2, 0);
        }
      }
      ctx.closePath();
    };

    ctx.shadowBlur = GLOWSHADOWBLUR;
    ctx.shadowColor = ctx.fillStyle = Colors.ENEMY_SHIP;
    ctx.translate(buffer.width * 0.5, buffer.height * 0.5);
    ctx.beginPath();
    ctx.arc(0, 0, BULLET_RADIUS-1, 0, TWOPI, true);
    ctx.closePath();
    ctx.fill();
    rf();
    ctx.fill();

    var img = new ImageElement();
    img.src = buffer.toDataUrl("image/png");
    return img;
  }, "enemybullet");
  }
}

/**
 * Game scene base class.
 */
class Scene {
  bool playable;
  Interval interval;

  Scene([this.playable = true, this.interval = null]);

  /** Return true if this scene should update the actor list. */
  bool isPlayable() => playable;

  void onInitScene() {
    if (interval != null) {
      // reset interval flag
      interval.reset();
    }
  }

  void onBeforeRenderScene() {}
  void onRenderScene(ctx) {}
  void onRenderInterval(ctx) {}
  void onMouseDownHandler(e) {}
  void onMouseUpHandler(e) {}
  void onKeyDownHandler(int keyCode) {}
  void onKeyUpHandler(int keyCode) {}
  bool isComplete() => false;

  bool onAccelerometer(double x, double y, double z) {
    return true;
  }
}

class SoundManager {
  bool _isDesktopEmulator;
  Map _sounds = {};

  SoundManager(this._isDesktopEmulator);

  void createSound(Map props) {
    if (!_isDesktopEmulator) {
      var a = new AudioElement();
      a.volume = props['volume'] / 100.0;;
      a.src = props['url'];
      _sounds[props['id']] = a;
    }
  }

  void play(String id) {
    if (!_isDesktopEmulator) {
      _sounds[id].play();
    }
  }
}

/**
 * An actor that can be rendered by a bitmap. The sprite handling code deals
 * with the increment of the current frame within the supplied bitmap sprite
 * strip image, based on animation direction, animation speed and the animation
 * length before looping. Call renderSprite() each frame.
 *
 * NOTE: by default sprites source images are 64px wide 64px by N frames high
 * and scaled to the appropriate final size. Any other size input source should
 * be set in the constructor.
 */
class SpriteActor extends Actor {
  SpriteActor(Vector position, Vector vector, [this.frameSize = 64])
      : super(position, vector);

  /** Size in pixels of the width/height of an individual frame in the image. */
  int frameSize;

  /**
   * Animation image sprite reference.
   * Sprite image sources are all currently 64px wide 64px by N frames high.
   */
  ImageElement animImage = null;

  /** Length in frames of the sprite animation. */
  int animLength = 0;

  /** Animation direction, true for forward, false for reverse. */
  bool animForward = true;

  /** Animation frame inc/dec speed. */
  double animSpeed = 1.0;

  /** Current animation frame index. */
  int animFrame = 0;

  /**
   * Render sprite graphic based on current anim image, frame and anim direction
   * Automatically updates the current anim frame.
   */
  void renderSprite(CanvasRenderingContext2D ctx, num x, num y, num s) {
    renderImage(ctx, animImage, 0, animFrame << 6, frameSize, x, y, s);

    // update animation frame index
    if (animForward) {
      animFrame += (animSpeed * frameMultiplier).toInt();
      if (animFrame >= animLength) {
        animFrame = 0;
      }
    } else {
      animFrame -= (animSpeed * frameMultiplier).toInt();
      if (animFrame < 0) {
        animFrame = animLength - 1;
      }
    }
  }
}

class Star {
  Star();

  double MAXZ = 12.0;
  double VELOCITY = 0.85;

  num x = 0;
  num y = 0;
  num z = 0;
  num prevx = 0;
  num prevy = 0;

  void init() {
    // select a random point for the initial location
    prevx = prevy = 0;
    x = (random() * GameHandler.width - (GameHandler.width * 0.5)) * MAXZ;
    y = (random() * GameHandler.height - (GameHandler.height * 0.5)) * MAXZ;
    z = MAXZ;
  }

  void render(CanvasRenderingContext2D ctx) {
    var xx = x / z;
    var yy = y / z;

    if (prevx != 0) {
      ctx.lineWidth = 1.0 / z * 5 + 1;
      ctx.beginPath();
      ctx.moveTo(prevx + (GameHandler.width * 0.5),
          prevy + (GameHandler.height * 0.5));
      ctx.lineTo(xx + (GameHandler.width * 0.5),
          yy + (GameHandler.height * 0.5));
      ctx.stroke();
    }

    prevx = xx;
    prevy = yy;
  }
}

void drawText(CanvasRenderingContext2D g,
              String txt, String font, num x, num y,
              [String color]) {
   g.save();
   if (color != null) g.strokeStyle = color;
   g.font = font;
   g.strokeText(txt, x, y);
   g.restore();
}

void centerDrawText(CanvasRenderingContext2D g, String txt, String font, num y,
                    [String color]) {
  g.save();
  if (color != null) g.strokeStyle = color;
  g.font = font;
  g.strokeText(txt, (GameHandler.width - g.measureText(txt).width) / 2, y);
  g.restore();
}

void fillText(CanvasRenderingContext2D g, String txt, String font, num x, num y,
              [String color]) {
  g.save();
  if (color != null) g.fillStyle = color;
  g.font = font;
  g.fillText(txt, x, y);
  g.restore();
}

void centerFillText(CanvasRenderingContext2D g, String txt, String font, num y,
                    [String color]) {
  g.save();
  if (color != null) g.fillStyle = color;
  g.font = font;
  g.fillText(txt, (GameHandler.width - g.measureText(txt).width) / 2, y);
  g.restore();
}

void drawScaledImage(CanvasRenderingContext2D ctx, ImageElement image,
                 num nx, num ny, num ns, num x, num y, num s) {
  ctx.drawImageToRect(image, new Rect(x, y, s, s),
      sourceRect: new Rect(nx, ny, ns, ns));
}
/**
 * This method will automatically correct for objects moving on/off
 * a cyclic canvas play area - if so it will render the appropriate stencil
 * sections of the sprite top/bottom/left/right as needed to complete the image.
 * Note that this feature can only be used if the sprite is absolutely
 * positioned and not translated/rotated into position by canvas operations.
 */
void renderImage(CanvasRenderingContext2D ctx, ImageElement image,
                 num nx, num ny, num ns, num x, num y, num s) {
  print("renderImage(_,$nx,$ny,$ns,$ns,$x,$y,$s,$s)");
  ctx.drawImageScaledFromSource(image, nx, ny, ns, ns, x, y, s, s);

  if (x < 0) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
        GameHandler.width + x, y, s, s);
  }
  if (y < 0) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
        x, GameHandler.height + y, s, s);
  }
  if (x < 0 && y < 0) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
       GameHandler.width + x, GameHandler.height + y, s, s);
  }
  if (x + s > GameHandler.width) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
       x - GameHandler.width, y, s, s);
  }
  if (y + s > GameHandler.height) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
       x, y - GameHandler.height, s, s);
  }
  if (x + s > GameHandler.width && y + s > GameHandler.height) {
    ctx.drawImageScaledFromSource(image, nx, ny, ns, ns,
       x - GameHandler.width, y - GameHandler.height, s, s);
  }
}

void renderImageRotated(CanvasRenderingContext2D ctx, ImageElement image,
                        num x, num y, num w, num h, num r) {
  var w2 = w*0.5, h2 = h*0.5;
  var rf = (tx, ty) {
    ctx.save();
    ctx.translate(tx, ty);
    ctx.rotate(r);
    ctx.drawImage(image, -w2, -h2);
    ctx.restore();
  };

  rf(x, y);

  if (x - w2 < 0) {
    rf(GameHandler.width + x, y);
  }
  if (y - h2 < 0) {
    rf(x, GameHandler.height + y);
  }
  if (x - w2 < 0 && y - h2 < 0)  {
    rf(GameHandler.width + x, GameHandler.height + y);
  }
  if (x - w2 + w > GameHandler.width) {
    rf(x - GameHandler.width, y);
  }
  if (y - h2 + h > GameHandler.height){
    rf(x, y - GameHandler.height);
  }
  if (x - w2 + w > GameHandler.width && y - h2 + h > GameHandler.height) {
    rf(x - GameHandler.width, y - GameHandler.height);
  }
}

void renderImageRotated2(CanvasRenderingContext2D ctx, ImageElement image,
                        num x, num y, num w, num h, num r) {
  print("Rendering rotated sprite ${image.src} to dest $x,$y");
  var w2 = w*0.5, h2 = h*0.5;
  var rf = (tx, ty) {
    ctx.save();
    ctx.translate(tx, ty);
    ctx.rotate(r);
    ctx.drawImage(image, -w2, -h2);
    ctx.restore();
  };

  rf(x, y);

  if (x - w2 < 0) {
    rf(GameHandler.width + x, y);
  }
  if (y - h2 < 0) {
    rf(x, GameHandler.height + y);
  }
  if (x - w2 < 0 && y - h2 < 0)  {
    rf(GameHandler.width + x, GameHandler.height + y);
  }
  if (x - w2 + w > GameHandler.width) {
    rf(x - GameHandler.width, y);
  }
  if (y - h2 + h > GameHandler.height){
    rf(x, y - GameHandler.height);
  }
  if (x - w2 + w > GameHandler.width && y - h2 + h > GameHandler.height) {
    rf(x - GameHandler.width, y - GameHandler.height);
  }
}

class Vector {
  num x, y;

  Vector(this.x, this.y);

  Vector clone() => new Vector(x, y);

  void set(Vector v) {
    x = v.x;
    y = v.y;
  }

  Vector add(Vector v) {
    x += v.x;
    y += v.y;
    return this;
  }

  Vector nadd(Vector v) => new Vector(x + v.x, y + v.y);

  Vector sub(Vector v) {
    x -= v.x;
    y -= v.y;
    return this;
  }

  Vector nsub(Vector v) => new Vector(x - v.x, y - v.y);

  double dot(Vector v) => x * v.x + y * v.y;

  double length() => Math.sqrt(x * x + y * y);

  double distance(Vector v) {
    var dx = x - v.x;
    var dy = y - v.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  double theta() => Math.atan2(y, x);

  double thetaTo(Vector vec) {
    // calc angle between the two vectors
    var v = clone().norm();
    var w = vec.clone().norm();
    return Math.sqrt(v.dot(w));
  }

  double thetaTo2(Vector vec) =>
      Math.atan2(vec.y, vec.x) - Math.atan2(y, x);

  Vector norm() {
    var len = length();
    x /= len;
    y /= len;
    return this;
  }

  Vector nnorm() {
    var len = length();
    return new Vector(x / len, y / len);
  }

  rotate(num a) {
    var ca = Math.cos(a);
    var sa = Math.sin(a);
    var newx = x*ca - y*sa;
    var newy = x*sa + y*ca;
    x = newx;
    y = newy;
    return this;
  }

  Vector nrotate(num a) {
    var ca = Math.cos(a);
    var sa = Math.sin(a);
    return new Vector(x * ca - y * sa, x * sa + y * ca);
  }

  Vector invert() {
    x = -x;
    y = -y;
    return this;
  }

  Vector ninvert() {
    return new Vector(-x, -y);
  }

  Vector scale(num s) {
    x *= s;
    y *= s;
    return this;
  }

  Vector nscale(num s) {
    return new Vector(x * s, y * s);
  }

  Vector scaleTo(num s) {
    var len = s / length();
    x *= len;
    y *= len;
    return this;
  }

  nscaleTo(num s) {
    var len = s / length();
    return new Vector(x * len, y * len);
  }

  trim(num minx, num maxx, num miny, num maxy) {
    if (x < minx) x = minx;
    else if (x > maxx) x = maxx;
    if (y < miny) y = miny;
    else if (y > maxy) y = maxy;
  }

  wrap(num minx, num maxx, num miny, num maxy) {
    if (x < minx) x = maxx;
    else if (x > maxx) x = minx;
    if (y < miny) y = maxy;
    else if (y > maxy) y = miny;
  }

  String toString() => "<$x, $y>";
}

class Weapon {
  Weapon(this.player, [this.rechargeTime = 125]);

  int rechargeTime;
  int lastFired = 0;
  Player player;

  bool canFire() =>
      (GameHandler.frameStart - lastFired) >= rechargeTime;

  List fire() {
    if (canFire()) {
      lastFired = GameHandler.frameStart;
      return doFire();
    }
  }

  Bullet makeBullet(double headingDelta, double vectorY,
                    [int lifespan = 1300]) {
    var h = player.heading - headingDelta;
    var t = new Vector(0.0, vectorY).rotate(h * RAD).add(player.velocity);
    return new Bullet(player.position.clone(), t, h, lifespan);
  }

  List doFire() => [];
}

class PrimaryWeapon extends Weapon {
  PrimaryWeapon(Player player) : super(player);

  List doFire() => [ makeBullet(0.0, -4.5) ];
}

class TwinCannonsWeapon extends Weapon {
  TwinCannonsWeapon(Player player) : super(player, 150);

  List doFire() {
    var h = player.heading;
    var t = new Vector(0.0, -4.5).rotate(h * RAD).add(player.velocity);
    return [ new BulletX2(player.position.clone(), t, h) ];
  }
}

class VSprayCannonsWeapon extends Weapon {
  VSprayCannonsWeapon(Player player) : super(player, 250);

  List doFire() =>
      [ makeBullet(-15.0, -3.75),
        makeBullet(0.0, -3.75),
        makeBullet(15.0, -3.75) ];
}

class SideGunWeapon extends Weapon {
  SideGunWeapon(Player player) : super(player, 250);

  List doFire() =>
      [ makeBullet(-90.0, -4.5, 750),
        makeBullet(+90.0, -4.5, 750)];
}

class RearGunWeapon extends Weapon {
  RearGunWeapon(Player player) : super(player, 250);

  List doFire() => [makeBullet(180.0, -4.5, 750)];
}

class Input {
  bool left, right, thrust, shield, fireA, fireB;

  Input() { reset(); }

  void reset() {
    left = right = thrust = shield = fireA = fireB = false;
  }
}

void resize(int w, int h) {}


void setup(canvasp, int w, int h, int f) {
  var canvas;
  if (canvasp == null) {
    log("Allocating canvas");
    canvas = new CanvasElement(width: w, height: h);
    document.body.nodes.add(canvas);
  } else {
    log("Using parent canvas");
    canvas = canvasp;
  }

  for (var i = 0; i < 4; i++) {
    _asteroidImgs.add(new ImageElement());
  }
  // attach to the image onload handler
  // once the background is loaded, we can boot up the game
  _backgroundImg.onLoad.listen((e) {
    // init our game with Game.Main derived instance
    log("Loaded background image ${_backgroundImg.src}");
    GameHandler.init(canvas);
    GameHandler.start(new AsteroidsMain());
  });
  _backgroundImg.src = 'bg3_1.png';
  loadSounds(f == 1);
}

void loadSounds(bool isDesktopEmulator) {
  soundManager = new SoundManager(isDesktopEmulator);
  // load game sounds
  soundManager.createSound({
    'id': 'laser',
    'url': 'laser.$sfx_extension',
    'volume': 40,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'enemy_bomb',
    'url': 'enemybomb.$sfx_extension',
    'volume': 60,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'big_boom',
    'url': 'bigboom.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'asteroid_boom1',
    'url': 'explosion1.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'asteroid_boom2',
    'url': 'explosion2.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'asteroid_boom3',
    'url': 'explosion3.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'asteroid_boom4',
    'url': 'explosion4.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
  soundManager.createSound({
    'id': 'powerup',
    'url': 'powerup.$sfx_extension',
    'volume': 50,
    'autoLoad': true,
    'multiShot': true
  });
}

