(function (window, document) {
  "use strict";

  var reducedMotion = window.matchMedia
    && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var videos = document.querySelectorAll(".js-lazy-video");

  function loadVideo(video) {
    if (video.dataset.loaded === "true") {
      return;
    }

    var sources = video.querySelectorAll("source[data-src]");
    for (var index = 0; index < sources.length; index += 1) {
      sources[index].src = sources[index].dataset.src;
      sources[index].removeAttribute("data-src");
    }

    video.dataset.loaded = "true";
    video.load();
  }

  function pauseVideo(video) {
    video.pause();
  }

  function playVideo(video) {
    if (reducedMotion || document.hidden) {
      pauseVideo(video);
      return;
    }

    loadVideo(video);
    var playAttempt = video.play();
    if (playAttempt && typeof playAttempt.catch === "function") {
      playAttempt.catch(function () {
        // Autoplay can be blocked by a browser preference; the poster remains visible.
      });
    }
  }

  if (!reducedMotion && "IntersectionObserver" in window) {
    var videoObserver = new IntersectionObserver(function (entries) {
      for (var index = 0; index < entries.length; index += 1) {
        var video = entries[index].target;
        video.dataset.inViewport = entries[index].isIntersecting ? "true" : "false";
        if (entries[index].isIntersecting) {
          playVideo(video);
        } else {
          pauseVideo(video);
        }
      }
    }, {
      rootMargin: "75% 0px",
      threshold: 0.01
    });

    for (var index = 0; index < videos.length; index += 1) {
      videoObserver.observe(videos[index]);
    }
  } else if (!reducedMotion) {
    for (var fallbackIndex = 0; fallbackIndex < videos.length; fallbackIndex += 1) {
      videos[fallbackIndex].dataset.inViewport = "true";
      playVideo(videos[fallbackIndex]);
    }
  }

  document.addEventListener("visibilitychange", function () {
    for (var index = 0; index < videos.length; index += 1) {
      if (videos[index].dataset.inViewport === "true") {
        playVideo(videos[index]);
      } else {
        pauseVideo(videos[index]);
      }
    }
  });
}(window, document));
