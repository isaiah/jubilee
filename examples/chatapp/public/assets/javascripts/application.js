$(function() {
  var eb = new vertx.EventBus("/eventbus");

  eb.onopen = function() {
                eb.registerHandler("chat", function(data) {
                  $("#updates").append(data + "<br/>");
                });
              }

  $("#send").click(function() {
    eb.publish("chat", $("#content").val());
    $("#content").val("");
  });
});
