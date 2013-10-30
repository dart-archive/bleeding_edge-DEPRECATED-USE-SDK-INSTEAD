2D Solar System Simulation
==========================

A 2D solar system visualization using Canvas.

You can open the example in Dart Editor and run it by clicking
`web/solar.html`.
Or, you can try this
[live demo](http://www.dartlang.org/samples/solar/).

The `solar.dart` file contains all the Dart code used in this sample. Two
classes, `PlanetaryBody` and `SolarSystem` are defined in that file.

The `PlanetaryBody` class contains the representation of a plantetary body.
This class calculates a planetary body's  position for a given time index,
and then draws it and any child planets.

The `SolarSystem` class maintains a list of planetary bodies. It knows how to
draw the solar system background and the planets. This class uses
requestAnimationFrame to redraw the solar system at appropriate time
intervals.

Please report any [bugs or feature requests](http://dartbug.com/new).
