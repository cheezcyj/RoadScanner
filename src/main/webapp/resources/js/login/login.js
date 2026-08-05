(function (window, document, $) {
  "use strict";

  var contextElement = document.querySelector('meta[name="application-context"]');
  var applicationContext = contextElement ? contextElement.getAttribute("content") : "";
  var csrfTokenElement = document.querySelector('meta[name="csrf-token"]');
  var csrfParameterElement = document.querySelector('meta[name="csrf-parameter"]');
  var csrfToken = csrfTokenElement ? csrfTokenElement.getAttribute("content") : "";
  var csrfParameter = csrfParameterElement ? csrfParameterElement.getAttribute("content") : "";

  $("#doLogin").on("click", function () {
    var id = $("#id").val();
    var password = $("#pw").val();

    if (!id) {
      alert("아이디를 입력하세요");
      $("#id").focus();
      return;
    }
    if (!password) {
      alert("비밀번호를 입력하세요");
      $("#pw").focus();
      return;
    }

    var loginData = {
      id: id,
      password: password
    };
    if (csrfToken && csrfParameter) {
      loginData[csrfParameter] = csrfToken;
    }

    $.ajax({
      type: "POST",
      url: applicationContext + "/login",
      dataType: "json",
      data: loginData,
      success: function (message) {
        if (message.msgId === "1" || message.msgId === "10") {
          alert(message.msgContents);
          $("#id").focus();
          return;
        }
        if (message.msgId === "2" || message.msgId === "20") {
          alert(message.msgContents);
          $("#pw").focus();
          return;
        }
        if (message.msgId === "30") {
          window.location.href = applicationContext + "/main/preUpload";
          return;
        }
        if (message.msgId === "40") {
          alert(message.msgContents);
          $("#id").val("");
          $("#pw").val("");
        }
      },
      error: function () {
        console.error("Login request failed");
      }
    });
  });
})(window, document, window.jQuery);
