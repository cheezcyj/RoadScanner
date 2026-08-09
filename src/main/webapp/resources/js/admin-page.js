(function (window, document) {
  "use strict";

  var frames = document.querySelectorAll(".admin-list-frame");
  var resizeTimer = null;

  function fitFrame(frame) {
    try {
      var frameDocument = frame.contentDocument;
      if (!frameDocument || !frameDocument.documentElement || !frameDocument.body) {
        return;
      }

      frame.style.height = "1px";
      var height = Math.max(
          frameDocument.documentElement.scrollHeight,
          frameDocument.body.scrollHeight
      );
      frame.style.height = Math.max(340, Math.ceil(height)) + "px";
    } catch (ignored) {
      // Same-origin administrator frames are expected; fixed CSS height remains as fallback.
    }
  }

  function fitAllFrames() {
    Array.prototype.forEach.call(frames, fitFrame);
  }

  Array.prototype.forEach.call(frames, function (frame) {
    frame.addEventListener("load", function () {
      fitFrame(frame);
    });

    if (frame.contentDocument && frame.contentDocument.readyState === "complete") {
      fitFrame(frame);
    }
  });

  window.addEventListener("resize", function () {
    window.clearTimeout(resizeTimer);
    resizeTimer = window.setTimeout(fitAllFrames, 120);
  });
})(window, document);
