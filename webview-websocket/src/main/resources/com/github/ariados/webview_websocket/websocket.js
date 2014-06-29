// Provide websocket support for pre-KitKat Android WebView via native interface.

function hasWebSocket() {
  //if (true) return false;

  var m = /Android ([0-9]+)\.([0-9]+)/i.exec(navigator.userAgent);
  var hasConstructor = typeof WebSocket === "function";

  if (!m) { return hasConstructor; }

  var x = parseInt(m[1], 10);
  var y = parseInt(m[2], 10);

  return hasConstructor && (x > 4 || (x === 4 && y >= 4));
}

hasWebSocket() || (function() {
    if (!window.WebSocketFactory) {
      if (window.console) {
        console.log("no WebSocket in browser and WebSocketFactory for creating Android shim not found either.");
      }
      return;
    }

    // window object
    var global = window;

    var messagePoller = function() {
        var messages = JSON.parse(WebSocketFactory.fetchMessages());
        messages.forEach(function (evt) {
            var target = WebSocket.store[evt._target];
            if (!target) {
                WebSocketFactory.closeUnreferenced(evt._target);
                return;
            }
            target[evt.event].call(global, evt);
        });
    };
    setInterval(messagePoller, 250);

    // WebSocket Object. All listener methods are cleaned up!
    var WebSocket = global.WebSocket = function(url, protocols, options) {
        var socket = this;
        options || (options = {});
        options.headers || (options.headers = {});

        if (Array.isArray(protocols)) {
          protocols = protocols.join(',');
        }

        if (protocols) {
          options.headers["Sec-WebSocket-Protocol"] = protocols;
        }

        this.events = [];
        this.options = options;
        this.url = url;

        // get a new websocket object from factory (check com.strumsoft.websocket.WebSocketFactory.java)
        this.socket = WebSocketFactory.getInstance(url, JSON.stringify(options));
        // store in registry
        if(this.socket) {
            WebSocket.store[this.socket.getId()] = this;
        } else {
            throw new Error('Websocket instantiation failed! Address might be wrong.');
        }
    };

    // storage to hold websocket object for later invokation of event methods
    WebSocket.store = {};

    // instance event methods
    WebSocket.prototype.send = function(data) {
        this.socket.send(data);
    }

    WebSocket.prototype.close = function() {
        this.socket.close();
    }

    WebSocket.prototype.getReadyState = function() {
        this.socket.getReadyState();
    }
    ///////////// Must be overloaded
    WebSocket.prototype.onopen = function(){
        throw new Error('onopen not implemented.');
      };

      // alerts message pushed from server
      WebSocket.prototype.onmessage = function(msg){
        throw new Error('onmessage not implemented.');
      };

      // alerts message pushed from server
      WebSocket.prototype.onerror = function(msg){
        throw new Error('onerror not implemented.');
      };

      // alert close event
      WebSocket.prototype.onclose = function(){
          throw new Error('onclose not implemented.');
      };
})();
