// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A solar system visualization.
 */

library solar;

import 'dart:html';
import 'dart:math';

/**
 * The entry point to the application.
 */
void main() {
  var solarSystem = new SolarSystem(query("#container"));

  solarSystem.start();
}

double fpsAverage;

/**
 * Display the animation's FPS in a div.
 */
void showFps(num fps) {
  if (fpsAverage == null) {
    fpsAverage = fps;
  }

  fpsAverage = fps * 0.05 + fpsAverage * 0.95;

  query("#notes").text = "${fpsAverage.round().toInt()} fps";
}

// TODO: remove this once dart:html Point works cross-platform
class Point {
  num x, y;

  Point(this.x, this.y);
}

/**
 * A representation of the solar system.
 *
 * This class maintains a list of planetary bodies, knows how to draw its
 * background and the planets, and requests that it be redraw at appropriate
 * intervals using the [Window.requestAnimationFrame] method.
 */
class SolarSystem {
  CanvasElement canvas;

  num _width;
  num _height;

  PlanetaryBody sun;

  num renderTime;

  SolarSystem(this.canvas) {

  }

  num get width => _width;

  num get height => _height;

  start() {
    // Measure the canvas element.
    window.requestLayoutFrame(() {
      _width = canvas.parent.clientWidth;
      _height = canvas.parent.clientHeight;

      canvas.width = _width;

      // Initialize the planets and start the simulation.
      _start();
    });
  }

  _start() {
    // Create the Sun.
    sun = new PlanetaryBody(this, "Sun", "#ff2", 14.0);

    // Add planets.
    sun.addPlanet(
        new PlanetaryBody(this, "Mercury", "orange", 0.382, 0.387, 0.241));
    sun.addPlanet(
        new PlanetaryBody(this, "Venus", "green", 0.949, 0.723, 0.615));

    var earth = new PlanetaryBody(this, "Earth", "#33f", 1.0, 1.0, 1.0);
    sun.addPlanet(earth);
    earth.addPlanet(new PlanetaryBody(this, "Moon", "gray", 0.2, 0.14, 0.075));

    sun.addPlanet(new PlanetaryBody(this, "Mars", "red", 0.532, 1.524, 1.88));

    addAsteroidBelt(sun, 150);

    final f = 0.1;
    final h = 1 / 1500.0;
    final g = 1 / 72.0;

    var jupiter = new PlanetaryBody(
        this, "Jupiter", "gray", 4.0, 5.203, 11.86);
    sun.addPlanet(jupiter);
    jupiter.addPlanet(new PlanetaryBody(
        this, "Io", "gray", 3.6 * f, 421 * h, 1.769 * g));
    jupiter.addPlanet(new PlanetaryBody(
        this, "Europa", "gray", 3.1 * f, 671 * h, 3.551 * g));
    jupiter.addPlanet(new PlanetaryBody(
        this, "Ganymede", "gray", 5.3 * f, 1070 * h, 7.154 * g));
    jupiter.addPlanet(new PlanetaryBody(
        this, "Callisto", "gray", 4.8 * f, 1882 * h, 16.689 * g));

    // Start the animation loop.
    requestRedraw();
  }

  void draw(num _) {
    num time = new Date.now().millisecondsSinceEpoch;

    if (renderTime != null) {
      showFps((1000 / (time - renderTime)).round());
    }

    renderTime = time;

    var context = canvas.context2d;

    drawBackground(context);
    drawPlanets(context);

    requestRedraw();
  }

  void drawBackground(CanvasRenderingContext2D context) {
    context.fillStyle = "white";
    context.rect(0, 0, width, height);
    context.fill();
  }

  void drawPlanets(CanvasRenderingContext2D context) {
    sun.draw(context, width / 2, height / 2);
  }

  void requestRedraw() {
    window.requestAnimationFrame(draw);
  }

  void addAsteroidBelt(PlanetaryBody body, int count) {
    Random random = new Random();

    // Asteroids are generally between 2.06 and 3.27 AUs.
    for (int i = 0; i < count; i++) {
      var radius = 2.06 + random.nextDouble() * (3.27 - 2.06);

      body.addPlanet(
          new PlanetaryBody(this, "asteroid", "#777",
              0.1 * random.nextDouble(),
              radius,
              radius * 2));
    }
  }

  num normalizeOrbitRadius(num r) {
    return r * (width / 10.0);
  }

  num normalizePlanetSize(num r) {
    return log(r + 1) * (width / 100.0);
  }
}

/**
 * A representation of a plantetary body.
 *
 * This class can calculate its position for a given time index, and draw itself
 * and any child planets.
 */
class PlanetaryBody {
  final String name;
  final String color;
  final num orbitPeriod;
  final SolarSystem solarSystem;

  num bodySize;
  num orbitRadius;
  num orbitSpeed;

  List<PlanetaryBody> planets;

  PlanetaryBody(this.solarSystem, this.name, this.color, this.bodySize,
      [this.orbitRadius = 0.0, this.orbitPeriod = 0.0]) {
    planets = [];

    bodySize = solarSystem.normalizePlanetSize(bodySize);
    orbitRadius = solarSystem.normalizeOrbitRadius(orbitRadius);
    orbitSpeed = _calculateSpeed(orbitPeriod);
  }

  void addPlanet(PlanetaryBody planet) {
    planets.add(planet);
  }

  void draw(CanvasRenderingContext2D context, num x, num y) {
    Point pos = _calculatePos(x, y);

    drawSelf(context, pos.x, pos.y);

    drawChildren(context, pos.x, pos.y);
  }

  void drawSelf(CanvasRenderingContext2D context, num x, num y) {
    context.save();

    try {
      context.lineWidth = 0.5;
      context.fillStyle = color;
      context.strokeStyle = color;

      if (bodySize >= 2.0) {
        context.shadowOffsetX = 2;
        context.shadowOffsetY = 2;
        context.shadowBlur = 2;
        context.shadowColor = "#ddd";
      }

      context.beginPath();
      context.arc(x, y, bodySize, 0, PI * 2, false);
      context.fill();
      context.closePath();
      context.stroke();

      context.shadowOffsetX = 0;
      context.shadowOffsetY = 0;
      context.shadowBlur = 0;

      context.beginPath();
      context.arc(x, y, bodySize, 0, PI * 2, false);
      context.fill();
      context.closePath();
      context.stroke();
    } finally {
      context.restore();
    }
  }

  void drawChildren(CanvasRenderingContext2D context, num x, num y) {
    for (var planet in planets) {
      planet.draw(context, x, y);
    }
  }

  num _calculateSpeed(num period) {
    if (period == 0.0) {
      return 0.0;
    } else {
      return 1 / (60.0 * 24.0 * 2 * period);
    }
  }

  Point _calculatePos(num x, num y) {
    if (orbitSpeed == 0.0) {
      return new Point(x, y);
    } else {
      num angle = solarSystem.renderTime * orbitSpeed;

      return new Point(
        orbitRadius * cos(angle) + x,
        orbitRadius * sin(angle) + y);
    }
  }

}
