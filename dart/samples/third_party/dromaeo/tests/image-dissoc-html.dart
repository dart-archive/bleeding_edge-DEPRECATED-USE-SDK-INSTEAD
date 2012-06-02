#library("image_dissoc");
#import("dart:html");
#import("dart:json");
#source("Common.dart");
#source("RunnerSuite.dart");

void main() {
  window.on.load.add((e) { real_main(); });
}

void real_main() {
  isBenchmark = document.query('#RESOURCE1') != null;

  var app = new DissociatedImage();
  app.setupBenchmark(() {
      new Suite(window, 'image-dissoc')
      .test('run', () { app.runBenchmarkOnce(); })
      .end();
    });
}


class Patch {
  Source source;
  int x, y, w, h;
  Patch(this.source, this.x, this.y, this.w, this.h);
}

class Source {
  ImageElement element;
  CanvasRenderingContext2D context;
  ImageData image;
  Source(this.element, this.context, this.image);
}

class Stencil {
  List<int> offsets;
  int count = 0;
  Stencil() : offsets = [];

  reset(capacity) {
    if (offsets.length < capacity) offsets = new List<int>(capacity);
    count = 0;
  }

  add2(int x, int y) {
    offsets[count] = x;
    offsets[count + 1] = y;
    count += 2;
  }
}

class DiscreteSlider {
  Element element;
  InputElement input;
  Element readout;
  List ticks;
  var initial;
  String label;
  var setter;
  DiscreteSlider(this.ticks, this.initial, this.label, this.setter) {
    element = new SpanElement();
    element.nodes.add(input = new InputElement());
    element.nodes.add(readout = new SpanElement());
    input.type = 'range';
    input.min = '0';
    input.max = '${ticks.length - 1}';
    setValue(initial);
    input.on.change.add(onChange);
    update();
  }
  setValue(value) {
    int index = ticks.indexOf(value);
    if (index == -1) index = 0;
    input.value = '$index';
    update();
  }
  onChange(e) { update(); }
  update() {
    var index = Math.parseInt(input.value);
    var value = ticks[index];
    readout.text = '$value $label';
    setter(value);
  }
}

class DissociatedImage {

  static final patchesTicks =
      const [1,10,20,50,
             100,200,300,400,500,600,700,800,900,
             1000,1200,1500,2000,3000,4000,5000,6000,8000,
             10000,12000,15000,20000,30000,40000,50000, 100000, 200000];

  InputElement fileInput;
  ButtonElement goButton;
  DivElement frameCounterElement;
  DiscreteSlider patchesInput;
  DiscreteSlider sizeInput;

  // Parameters
  int patchWidth = 32;
  int patchHeight = 32;
  int patchCount = 300;

  // Live
  CanvasElement canvas;
  CanvasRenderingContext2D ctx;
  ImageData creation;
  bool creationDirty = false;
  Stencil matchStencil;
  Stencil updateStencil;

  List<Source> sources;
  List<Patch> patches;

  Queue eventTasks;
  Queue tasks;

  int startMsEpoch = 0;

  int currentFrameStart;
  int frameCount = 0;
  bool kicked; // Outstanding animation frame?

  DissociatedImage() {
    sources = new List<Source>();
    patches = new List<Patch>();
    eventTasks = new Queue();
    tasks = new Queue();

    canvas = document.query('#CANVAS');
    fileInput = document.query('#FILE');
    goButton = document.query('#GO');

    canvas.style.border = '2px';

    fileInput.on.change.add(deferEvent(onFileChange));
    goButton.disabled = true;
    goButton.on.click.add(deferEvent(onGo));

    // Frame counter in upper right corner.
    frameCounterElement = new DivElement();
    frameCounterElement.style.position = 'absolute';
    frameCounterElement.style.top = '0px';
    frameCounterElement.style.right = '0px';
    document.body.nodes.add(frameCounterElement);

    var params = document.query('#PARAMS');
    patchesInput = new DiscreteSlider(patchesTicks, 300, 'patches', setPatchCount);
    sizeInput =
        new DiscreteSlider([128, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096],
          512, 'size',
          setCanvasSize);
    params.nodes.add(patchesInput.element);
    params.nodes.add(sizeInput.element);
  }

  onFileChange(e) {
    status(e);
    var reader = new FileReader();
    reader.on.load.add(deferEvent(onFileLoad));
    reader.on.error.add(deferEvent(onFileError));
    reader.readAsDataURL(e.target.files[0]);
  }

  onFileError(e) { status('Error $e'); }

  onFileLoad(e) {
    var src = e.target.result;
    var img = addPreview(src);
    img.on.load.add(deferEvent(onImageLoad(img)));
  }

  addPreview(src) {
    var img = new ImageElement();
    img.src = src;
    img.height = 100;
    img.style.border = '3px solid gold';
    img.style.margin = '3px';
    fileInput.value = '';
    document.query('#PREVIEWS').nodes.add(img);
    return img;
  }

  onImageLoad(ImageElement img) => (e) {
    CanvasElement c =
        new CanvasElement(height: img.naturalHeight, width: img.naturalWidth);
    var context = c.getContext('2d');
    context.drawImage(img, 0, 0);
    ImageData pixels = context.getImageData(0, 0, c.width, c.height);
    sources.add(new Source(img, context, pixels));

    goButton.disabled = false;

    // TODO: Remove this 'copy' to the canvas.
    ctx = canvas.getContext('2d');
    ctx.putImageData(pixels, 0, 0);
  };

  setPatchCount(value) { patchCount = value; }

  setCanvasSize(value) {
    canvas.width = value;
    canvas.height = value;
    creationDirty = true;
  }

  onGo(e) {
    status('Go!');
    tasks.clear();
    patches.clear();
    matchStencil = new Stencil();
    updateStencil = new Stencil();
    startMsEpoch = new Date.now().value;
    newTask(initCreation);
    newTask(updateCanvas);  // flush
    newTask(selectInitialPatches(0));
    newTask((){status('done initial patched');});
    newTask(selectSeed);
  }

  initCreation() {
    status('init');
    ctx = canvas.getContext('2d');
    creation = ctx.getImageData(0, 0, canvas.width, canvas.height);
    initCreationFrom(0);
  }

  initCreationFrom(int start) {
    Uint8ClampedArray a = creation.data;
    creationDirty = true;
    int end = Math.min(a.length, start + 10000);
    for (int i = start; i < end; ++i) {
      a[i] = 0x80;
    }
    if (end < a.length) continueTask(() => initCreationFrom(end));
  }

  selectInitialPatches(int i) => () {
    if (i >= patchCount) {
      status('initial patches selected');
    } else {
      i += 1;
      status('select initial patches ${i}/${patchCount}');
      Patch p = freshPatch();
      patches.add(p);
      continueTask(selectInitialPatches(i));
    }
  };

  Patch freshPatch() {
    int height = patchWidth;
    int width = patchHeight;
    int ix = randInt(sources.length);
    Source s = sources[ix];
    int x = randInt(s.image.width - width);
    int y = randInt(s.image.height - height);
    return new Patch(s, x, y, width, height);
  }

  selectSeed(){
    newTask(patchPath(0));
  }

  radius(theta) =>
      Math.min(patchWidth, patchHeight) * 0.7 * theta / (2.0 * Math.PI);

  dT(theta) => Math.min(1.0, 2.0 * Math.PI / theta);

  patchPath(theta) => () {
    var r = radius(theta);
    var xx = creation.width + patchWidth / 2;
    var yy = creation.height + patchHeight / 2;
    if (r * r > (xx * xx + yy * yy) / 4) {
      patchPathDone();
      return;
    }

    var dTheta = dT(theta);
    if (false) {
      continueTask(patchPath(theta + dTheta));
      continueTask(patchOne(theta));
    } else {
      var dTheta2 = dT(theta + dTheta);
      var dTheta3 = dT(theta + dTheta + dTheta2);
      continueTask(patchPath(theta + dTheta + dTheta2 + dTheta3));
      continueTask(patchOne(theta));
      continueTask(patchOne(theta + dTheta));
      continueTask(patchOne(theta + dTheta + dTheta2));
    }
  };

  patchPathDone() {
    var now = new Date.now().value;
    status('took ${(now - startMsEpoch) / 1000}s');
    var img = new ImageElement();
    img.src = canvas.toDataURL('image/png');
    img.height = 100;
    img.style.border = '3px solid red';
    img.style.margin = '3px';
    document.query('#RESULTS').nodes = [img];
  }

  patchOne(theta) => () {
    var r = radius(theta);
    int x = (r * Math.cos(theta) + 0.5).toInt() + (creation.width >> 1) - (patchWidth >> 1);
    int y = (r * Math.sin(theta) + 0.5).toInt() + (creation.height >> 1) - (patchHeight >> 1);

    if (x + patchWidth < 0 || x >= creation.width ||
        y + patchHeight < 0 || y >= creation.height) return;

    prepareStencils(x, y);

    int bestIx = 0;
    int bestError = matchThroughStencil(patches[0], matchStencil, x, y, 2147000000);
    int initialError = bestError;

    for (int i = 1; i < patches.length; i++) {
      Patch patch = patches[i];
      int err = matchThroughStencil(patch, matchStencil, x, y, bestError);
      if (err < bestError) {
        bestIx = i;
        bestError = err;
      }
    }
    // print('initial err $initialError    best $bestError   ix $bestIx');

    Patch patch = patches[bestIx];
    patches[bestIx] = freshPatch();
    outputThroughStencil(patch, updateStencil, x, y);
  };

  prepareStencils(int x, int y) {
    matchStencil.reset(patchWidth * patchHeight * 2);
    updateStencil.reset(patchWidth * patchHeight * 2);
    Uint8ClampedArray existing = creation.data;
    for (int dx = 0; dx < patchWidth; dx++) {
      int cx = dx + x;
      if (cx < 0 || cx >= creation.width) continue;
      for (int dy = 0; dy < patchHeight; dy++) {
        int cy = dy + y;
        if (cy < 0 || cy >= creation.height) continue;
        int co = 4 * (cy * creation.width + cx);
        if (existing[co + 3] == 0x80) {
          updateStencil.add2(dx, dy);
        } else {
          matchStencil.add2(dx, dy);
        }
      }
    }
    // print('match ${matchStencil.count}  update ${updateStencil.count}');
  }

  int matchThroughStencil(Patch patch, Stencil stencil, int cx, int cy,
                          int bestError) {
    int len = stencil.count;
    List a = stencil.offsets;
    Uint8ClampedArray target = creation.data;
    Uint8ClampedArray source = patch.source.image.data;
    int px = patch.x;
    int py = patch.y;
    int pw = patch.source.image.width;
    int cw = creation.width;
    int error = 0;
    for (int i = 0; i < len; i += 2) {
      int dx = a[i];
      int dy = a[i + 1];
      int po = 4 * ((py + dy) * pw + px + dx);
      int co = 4 * ((cy + dy) * cw + cx + dx);
      int d0 = target[co + 0] - source[po + 0];
      int d1 = target[co + 1] - source[po + 1];
      int d2 = target[co + 2] - source[po + 2];
      error += d0 * d0 + d1 * d1 + d2 * d2;
      if (error > bestError) return error;
    }
    return error;
  }

  outputThroughStencil(Patch patch, Stencil stencil, int cx, int cy) {
    int len = stencil.count;
    var a = stencil.offsets;
    Uint8ClampedArray target = creation.data;
    Uint8ClampedArray source = patch.source.image.data;
    int px = patch.x;
    int py = patch.y;
    int pw = patch.source.image.width;
    int cw = creation.width;
    for (int i = 0; i < len; i += 2) {
      int dx = a[i];
      int dy = a[i + 1];
      int po = 4 * ((py + dy) * pw + px + dx);
      int co = 4 * ((cy + dy) * cw + cx + dx);
      target[co + 0] = source[po + 0];
      target[co + 1] = source[po + 1];
      target[co + 2] = source[po + 2];
      target[co + 3] = source[po + 3];
    }
    //creationDirty = true;
    ctx.putImageData(creation, 0, 0, cx, cy, patch.w, patch.h);
  }

  animationFrame(t) {
    kicked = false;
    if (!tasks.isEmpty()) kick();
    currentFrameStart = new Date.now().value;
    ++frameCount;
    frameCounterElement.text = '$frameCount';
    processPendingEvents();
    for (;;) {
      if (tasks.isEmpty()) break;
      nextTask();
      if (isBenchmark) continue;
      if (new Date.now().value - currentFrameStart > 10) break;
    }
    if (creationDirty) updateCanvas();
  }

  processPendingEvents() {
    while (!eventTasks.isEmpty()) {
      var thunk = eventTasks.removeFirst();
      thunk();
    }
    // TODO: thunks may change the state (to reflect changes in this frame) and
    // schedule tasks - these tasks should be FIFO.  Since they are added with
    // addFirst they will actually be LIFO.
  }

  kick() {
    if (kicked) return;
    kicked = true;
    if (!isBenchmark)
      window.requestAnimationFrame(animationFrame);
  }

  newTask(thunk()) {
    tasks.add(thunk);
    kick();
  }

 deferEvent(handler) => (e) {
    eventTasks.add(() => handler(e));
    kick();
  };

  nextTask() {
    tasks.removeFirst()();
  }

  continueTask(thunk()) {
    tasks.addFirst(thunk);
    kick();
  }

  updateCanvas() {
    ctx.putImageData(creation, 0, 0);
    creationDirty = false;
  }

  num runBenchmarkOnce() {
    setRandomSeed(1);
    var start = new Date.now().value;
    onGo(null);
    animationFrame(0);
    var end = new Date.now().value;
    return end - start;
  }

  void setupBenchmark(continueBenchmark()) {
    patchWidth = 16;
    patchHeight = 16;
    patchesInput.setValue(20);
    sizeInput.setValue(128);
    ImageElement sampleImage = document.query('#RESOURCE1');
    var img = addPreview(sampleImage.src);
    img.on.load.add((e) {
        onImageLoad(img)(null);
        continueBenchmark();
      });
  }

  void runBenchmark() {
    setupBenchmark(() {
        num t1 = runBenchmarkOnce();
        num t2 = runBenchmarkOnce();
        num t3 = runBenchmarkOnce();
        status('t1 = $t1  t2 = $t2  t3 = $t3');
      });
  }

  void run() {
    status('Hello World!'
      //'    ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)}'
      //' -- ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)}'
      //' -- ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)}'
      //' -- ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)}'
      //' -- ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)} ${randInt(1000)}'
      );
    if (isBenchmark) runBenchmark();
  }


  int m_w = 7;   /* must not be zero */
  int m_z = 1;   /* must not be zero */

  void setRandomSeed(int n) {
    m_w = n & 65535;
    if (m_w == 0) m_w = 1;
    m_z = (n >> 16) & 65535;
    if (m_z == 0) m_z = 1;
  }

  int getRandom() {
      m_z = (36969 * (m_z & 65535) + ((m_z >> 16) & 65535)) & 0x7FFFFFFF;
      m_w = (18000 * (m_w & 65535) + ((m_w >> 16) & 65535)) & 0x7FFFFFFF;
      return ((m_z * 65536) + m_w) & 0x7FFFFFFF;
  }

  int randInt(int limit) {
    return getRandom() % limit;
  }

  void status(message) {
    document.query('#status').innerHTML = '$message';
  }
}

bool isBenchmark = false;
