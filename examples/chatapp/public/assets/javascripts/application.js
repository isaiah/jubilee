// To generate a random id 
function s4() {
  return Math.floor((1 + Math.random()) * 0x10000)
    .toString(16)
    .substring(1);
};

$(function() {
  var eb = new vertx.EventBus("/eventbus");
  var updates = $("#updates");
  var uid = "Guest_" + s4();

  eb.onopen = function() {
    eb.send("login", uid, function(data){
      for(var i = 0; i < data.users.length; i++) {
        if (data.users[i] != uid) $("#receivers").append("<option>" + data.users[i] + "</option>");
      }
    });
    updates.html("<h5>Welcome to the Jubilee chat room!</h5>");
    eb.registerHandler("chat", function(data) {
      if (data.sender != uid)
        updates.append("<div class='public'><span class='sender'>" + data.sender + " said:</span>" + data.message + "</div>");
      else
        updates.append("<div class='public by_you'><span class='sender'>You said:</span>" + data.message + "</div>");
    });

    eb.registerHandler("new_user", function(data) {
      if (data != uid) {
        $("#receivers").append("<option>" + data + "</option");
        updates.append("<div class='login'>" + data + " joined the room.</div>");
      }
    });

    eb.registerHandler(uid, function(data) {
      updates.append("<div class='private'><span class='sender'>" + data.sender + " said to you:</span>" + data.message + "</div>");
    });

    eb.registerHandler("logout", function(data) {
      $('#receivers option:contains("' + data + '")').remove();
      updates.append("<div class='logout'>" + data + " left the room.</div>");
    });

    window.onbeforeunload = function() {
      eb.publish("logout", uid);
    }
  }

  var sendMessage = function() {
    var msg = $("#content").val();
    if ((receiver = $("#receivers").val()) === "all") {
      eb.publish("chat", {sender: uid, message: msg});
    } else {
      updates.append("<div class='public by_you'><span class='sender'>You said to " + receiver + ":</span>" + msg + "</div>");
      eb.send(receiver, {sender: uid, message: msg});
    }
    $("#content").val("");
  }

  $("#send").click(sendMessage);

  $(document.body).keyup(function(ev) {
    if (ev.which === 13) {
      sendMessage();
    }
  });

});
