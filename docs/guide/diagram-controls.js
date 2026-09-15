(function () {
  var MIN_SCALE = 0.05;
  var MAX_SCALE = 4;
  var ZOOM_STEP = 1.15;
  var WHEEL_SENSITIVITY = 0.0012;

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
  }

  function svgSize(svg) {
    var viewBox = svg.viewBox && svg.viewBox.baseVal;
    if (viewBox && viewBox.width && viewBox.height) {
      return { width: viewBox.width, height: viewBox.height };
    }

    return {
      width: svg.getBoundingClientRect().width || 800,
      height: svg.getBoundingClientRect().height || 480
    };
  }

  function applyState(canvas) {
    canvas.style.transform = 'translate(' + canvas._diagramX + 'px, ' + canvas._diagramY
      + 'px) scale(' + canvas._diagramScale + ')';
  }

  function syncViewportHeight(canvas, viewport) {
    var size = canvas._diagramSize;
    if (!size) {
      return;
    }

    viewport.style.height = Math.ceil((size.height * canvas._diagramScale) + 32) + 'px';
  }

  function setScale(canvas, viewport, nextScale, originX, originY) {
    var previousScale = canvas._diagramScale;
    var scale = clamp(nextScale, MIN_SCALE, MAX_SCALE);
    var ratio = scale / previousScale;

    canvas._diagramX = originX - ((originX - canvas._diagramX) * ratio);
    canvas._diagramY = originY - ((originY - canvas._diagramY) * ratio);
    canvas._diagramScale = scale;
    canvas._diagramMode = 'manual';
    applyState(canvas);
  }

  function wheelDelta(value, event, viewport) {
    if (event.deltaMode === 1) {
      return value * 16;
    }

    if (event.deltaMode === 2) {
      return value * viewport.clientHeight;
    }

    return value;
  }

  function pointerInViewport(event, viewport) {
    var rect = viewport.getBoundingClientRect();
    return {
      clientX: event.clientX,
      clientY: event.clientY,
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    };
  }

  function activePointerValues(activePointers) {
    return Object.keys(activePointers).map(function (key) {
      return activePointers[key];
    });
  }

  function pointerDistance(first, second) {
    var deltaX = second.x - first.x;
    var deltaY = second.y - first.y;
    return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
  }

  function pointerCenter(first, second) {
    return {
      x: (first.x + second.x) / 2,
      y: (first.y + second.y) / 2
    };
  }

  function positionDiagram(canvas, viewport, size) {
    canvas._diagramSize = size;
    syncViewportHeight(canvas, viewport);
    canvas._diagramX = Math.max(0, (viewport.clientWidth - (size.width * canvas._diagramScale)) / 2);
    canvas._diagramY = 16;
    applyState(canvas);
  }

  function fitDiagramWidth(canvas, viewport, svg) {
    var size = svgSize(svg);
    var availableWidth = Math.max(viewport.clientWidth, 1);
    var scale = Math.min(availableWidth / size.width, 1);

    canvas._diagramScale = clamp(scale, MIN_SCALE, MAX_SCALE);
    canvas._diagramMode = 'width';
    positionDiagram(canvas, viewport, size);
  }

  function fitDiagram(canvas, viewport, svg) {
    var size = svgSize(svg);
    var availableWidth = Math.max(viewport.clientWidth - 32, 1);
    var availableHeight = Math.max((window.innerHeight || viewport.clientHeight) - 160, 1);
    var scale = Math.min(
      availableWidth / size.width,
      availableHeight / size.height,
      0.98
    );

    canvas._diagramScale = clamp(scale, MIN_SCALE, MAX_SCALE);
    canvas._diagramMode = 'fit';
    canvas._diagramSize = size;
    syncViewportHeight(canvas, viewport);
    canvas._diagramX = Math.max(16, (viewport.clientWidth - (size.width * canvas._diagramScale)) / 2);
    canvas._diagramY = 16;
    applyState(canvas);
  }

  function button(label, title, action) {
    var element = document.createElement('button');
    element.type = 'button';
    element.textContent = label;
    element.title = title;
    element.setAttribute('aria-label', title);
    element.addEventListener('click', action);
    return element;
  }

  function toolbar(canvas, viewport, svg) {
    var element = document.createElement('div');
    element.className = 'diagram-toolbar';
    element.appendChild(button('-', 'Zoom out', function () {
      setScale(canvas, viewport, canvas._diagramScale / ZOOM_STEP,
        viewport.clientWidth / 2, viewport.clientHeight / 2);
    }));
    element.appendChild(button('+', 'Zoom in', function () {
      setScale(canvas, viewport, canvas._diagramScale * ZOOM_STEP,
        viewport.clientWidth / 2, viewport.clientHeight / 2);
    }));
    element.appendChild(button('W', 'Fit diagram width', function () {
      fitDiagramWidth(canvas, viewport, svg);
    }));
    element.appendChild(button('F', 'Fit entire diagram', function () {
      fitDiagram(canvas, viewport, svg);
    }));
    element.appendChild(button('1:1', 'Reset zoom', function () {
      canvas._diagramScale = 1;
      canvas._diagramMode = 'manual';
      canvas._diagramX = 16;
      canvas._diagramY = 16;
      applyState(canvas);
    }));
    return element;
  }

  function enablePanAndZoom(canvas, viewport) {
    var activePointers = {};
    var dragging = false;
    var startX = 0;
    var startY = 0;
    var originX = 0;
    var originY = 0;
    var pinch = null;

    viewport.addEventListener('wheel', function (event) {
      event.preventDefault();
      var rect = viewport.getBoundingClientRect();
      var pointerX = event.clientX - rect.left;
      var pointerY = event.clientY - rect.top;
      var deltaX = wheelDelta(event.deltaX, event, viewport);
      var deltaY = wheelDelta(event.deltaY, event, viewport);

      if (event.ctrlKey) {
        var zoomFactor = Math.exp(-deltaY * WHEEL_SENSITIVITY);
        var nextScale = canvas._diagramScale * clamp(zoomFactor, 0.85, 1.18);
        setScale(canvas, viewport, nextScale, pointerX, pointerY);
        return;
      }

      canvas._diagramX -= deltaX;
      canvas._diagramY -= deltaY;
      canvas._diagramMode = 'manual';
      applyState(canvas);
    }, { passive: false });

    function startPinch() {
      var points = activePointerValues(activePointers);
      var first = points[0];
      var second = points[1];
      var distance = pointerDistance(first, second);
      var center = pointerCenter(first, second);

      pinch = {
        centerX: center.x,
        centerY: center.y,
        distance: distance || 1,
        scale: canvas._diagramScale,
        x: canvas._diagramX,
        y: canvas._diagramY
      };
    }

    viewport.addEventListener('pointerdown', function (event) {
      activePointers[event.pointerId] = pointerInViewport(event, viewport);
      dragging = true;
      startX = event.clientX;
      startY = event.clientY;
      originX = canvas._diagramX;
      originY = canvas._diagramY;
      viewport.classList.add('is-panning');
      viewport.setPointerCapture(event.pointerId);

      if (activePointerValues(activePointers).length === 2) {
        startPinch();
      }
    });

    viewport.addEventListener('pointermove', function (event) {
      if (!activePointers[event.pointerId]) {
        return;
      }

      activePointers[event.pointerId] = pointerInViewport(event, viewport);

      var points = activePointerValues(activePointers);
      if (points.length >= 2) {
        if (!pinch) {
          startPinch();
        }

        var first = points[0];
        var second = points[1];
        var center = pointerCenter(first, second);
        var scale = clamp(
          pinch.scale * (pointerDistance(first, second) / pinch.distance),
          MIN_SCALE,
          MAX_SCALE
        );
        var ratio = scale / pinch.scale;

        canvas._diagramX = center.x - ((pinch.centerX - pinch.x) * ratio);
        canvas._diagramY = center.y - ((pinch.centerY - pinch.y) * ratio);
        canvas._diagramScale = scale;
        canvas._diagramMode = 'manual';
        applyState(canvas);
        return;
      }

      if (!dragging) {
        return;
      }

      canvas._diagramX = originX + event.clientX - startX;
      canvas._diagramY = originY + event.clientY - startY;
      applyState(canvas);
    });

    function stopPan(event) {
      delete activePointers[event.pointerId];

      if (viewport.hasPointerCapture(event.pointerId)) {
        viewport.releasePointerCapture(event.pointerId);
      }

      var points = activePointerValues(activePointers);
      if (!points.length) {
        dragging = false;
        pinch = null;
        viewport.classList.remove('is-panning');
        return;
      }

      pinch = null;
      startX = points[0].clientX;
      startY = points[0].clientY;
      originX = canvas._diagramX;
      originY = canvas._diagramY;

      if (points.length >= 2) {
        startPinch();
      }
    }

    viewport.addEventListener('pointerup', stopPan);
    viewport.addEventListener('pointercancel', stopPan);
  }

  function enhanceOne(container) {
    var svg = container.querySelector('svg');
    if (!svg || container.dataset.diagramEnhanced === 'true') {
      return;
    }

    var parent = container.parentNode;
    var frame = document.createElement('div');
    var viewport = document.createElement('div');
    frame.className = 'diagram-frame';
    viewport.className = 'diagram-viewport';
    container.classList.add('diagram-canvas');
    container.dataset.diagramEnhanced = 'true';

    var size = svgSize(svg);
    svg.setAttribute('width', String(size.width));
    svg.setAttribute('height', String(size.height));
    container._diagramScale = 1;
    container._diagramX = 16;
    container._diagramY = 16;
    applyState(container);

    parent.insertBefore(frame, container);
    viewport.appendChild(container);
    frame.appendChild(toolbar(container, viewport, svg));
    frame.appendChild(viewport);
    enablePanAndZoom(container, viewport);
    window.requestAnimationFrame(function () {
      fitDiagramWidth(container, viewport, svg);
    });
    window.addEventListener('resize', function () {
      if (container._diagramMode === 'width') {
        fitDiagramWidth(container, viewport, svg);
      }
    });
  }

  window.diagramControls = {
    enhance: function () {
      Array.prototype.forEach.call(document.querySelectorAll('.mermaid'), enhanceOne);
    }
  };
}());
