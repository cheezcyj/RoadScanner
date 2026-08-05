(function (window, document) {
  "use strict";

  var form = document.getElementById("analysisUploadForm");
  var fileUploadInput = document.getElementById("fileUpload");
  var dropZone = document.getElementById("fileUploadLabel");
  var chooseFileButton = document.getElementById("chooseFileButton");
  var selectedFilePanel = document.getElementById("selectedFilePanel");
  var selectedImage = document.getElementById("selectedImage");
  var selectedFileName = document.getElementById("selectedFileName");
  var selectedFileMeta = document.getElementById("selectedFileMeta");
  var replaceFileButton = document.getElementById("replaceFileButton");
  var cancelButton = document.getElementById("cancelButton");
  var runContainer = document.getElementById("RunContainer");
  var runButton = document.getElementById("runButton");
  var runButtonLabel = runButton.querySelector(".run-button-label");
  var uploadStatus = document.getElementById("uploadStatus");
  var contextPathElement = document.getElementById("contextPath");
  var contextPath = contextPathElement ? contextPathElement.value : "";
  var allowedExtensions = ["jpg", "jpeg", "png", "bmp"];
  var maxSize = 5 * 1024 * 1024;
  var selectedFile = null;
  var previewUrl = "";

  function setStatus(message, isError) {
    uploadStatus.textContent = message || "";
    uploadStatus.classList.toggle("is-error", Boolean(isError));
  }

  function formatFileSize(bytes) {
    if (bytes < 1024 * 1024) {
      return Math.max(1, Math.round(bytes / 1024)) + "KB";
    }
    return (bytes / (1024 * 1024)).toFixed(1) + "MB";
  }

  function getExtension(fileName) {
    var separatorIndex = fileName.lastIndexOf(".");
    return separatorIndex >= 0 ? fileName.slice(separatorIndex + 1).toLowerCase() : "";
  }

  function validateFile(file) {
    if (!file) {
      return "분석할 이미지 파일을 선택해주세요.";
    }
    if (file.size === 0) {
      return "내용이 없는 파일은 업로드할 수 없습니다.";
    }
    if (file.size > maxSize) {
      return "파일 크기는 최대 5MB까지 가능합니다.";
    }
    if (allowedExtensions.indexOf(getExtension(file.name)) < 0) {
      return "JPG, JPEG, PNG 또는 BMP 이미지 파일을 선택해주세요.";
    }
    return "";
  }

  function clearPreviewUrl() {
    if (previewUrl) {
      window.URL.revokeObjectURL(previewUrl);
      previewUrl = "";
    }
  }

  function resetSelection() {
    clearPreviewUrl();
    selectedFile = null;
    fileUploadInput.value = "";
    dropZone.hidden = false;
    selectedFilePanel.hidden = true;
    runContainer.hidden = true;
    selectedImage.removeAttribute("src");
    selectedFileName.textContent = "";
    selectedFileMeta.textContent = "";
    runButton.disabled = false;
    runButtonLabel.textContent = "이미지 분석 시작";
    setStatus("", false);
  }

  function showSelectedFile(file) {
    var validationMessage = validateFile(file);
    if (validationMessage) {
      resetSelection();
      setStatus(validationMessage, true);
      return;
    }

    clearPreviewUrl();
    selectedFile = file;
    previewUrl = window.URL.createObjectURL(file);
    selectedImage.src = previewUrl;
    selectedFileName.textContent = file.name;
    selectedFileMeta.textContent = getExtension(file.name).toUpperCase() + " · " + formatFileSize(file.size);
    dropZone.hidden = true;
    selectedFilePanel.hidden = false;
    runContainer.hidden = false;
    setStatus("분석할 이미지가 준비되었습니다.", false);
  }

  function openFilePicker() {
    fileUploadInput.click();
  }

  function openFilePickerWithKeyboard(event) {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }
    event.preventDefault();
    openFilePicker();
  }

  fileUploadInput.addEventListener("change", function () {
    showSelectedFile(fileUploadInput.files[0]);
  });

  chooseFileButton.addEventListener("keydown", openFilePickerWithKeyboard);
  replaceFileButton.addEventListener("keydown", openFilePickerWithKeyboard);
  cancelButton.addEventListener("click", resetSelection);

  ["dragenter", "dragover"].forEach(function (eventName) {
    dropZone.addEventListener(eventName, function (event) {
      event.preventDefault();
      event.stopPropagation();
      dropZone.classList.add("is-dragging");
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = "copy";
      }
    });
  });

  ["dragleave", "drop"].forEach(function (eventName) {
    dropZone.addEventListener(eventName, function (event) {
      event.preventDefault();
      event.stopPropagation();
      dropZone.classList.remove("is-dragging");
    });
  });

  dropZone.addEventListener("drop", function (event) {
    var files = event.dataTransfer && event.dataTransfer.files;
    if (!files || files.length === 0) {
      return;
    }
    if (files.length > 1) {
      setStatus("이미지는 한 번에 한 개만 선택할 수 있습니다.", true);
      return;
    }

    try {
      fileUploadInput.files = files;
    } catch (ignored) {
      // selectedFile 변수로 업로드를 계속할 수 있는 브라우저 호환 처리
    }
    showSelectedFile(files[0]);
  });

  form.addEventListener("submit", function (event) {
    event.preventDefault();

    var validationMessage = validateFile(selectedFile);
    if (validationMessage) {
      setStatus(validationMessage, true);
      return;
    }

    runButton.disabled = true;
    runButtonLabel.textContent = "이미지를 분석하고 있습니다";
    setStatus("업로드가 완료될 때까지 잠시 기다려주세요.", false);

    var formData = new FormData();
    formData.append("fileUpload", selectedFile);

    window.fetch(contextPath + "/main/fileUploaded", {
      method: "POST",
      body: formData
    }).then(function (response) {
      if (!response.ok) {
        throw new Error("Upload failed");
      }
      return response.text();
    }).then(function (idx) {
      var normalizedIdx = idx.trim();
      if (!/^\d+$/.test(normalizedIdx)) {
        throw new Error("Unexpected upload response");
      }
      window.location.href = contextPath + "/main/upload?idx="
          + encodeURIComponent(normalizedIdx);
    }).catch(function () {
      runButton.disabled = false;
      runButtonLabel.textContent = "이미지 분석 시작";
      setStatus("이미지 업로드에 실패했습니다. 잠시 후 다시 시도해주세요.", true);
      console.error("File upload failed");
    });
  });

  window.addEventListener("pagehide", clearPreviewUrl);
})(window, document);
