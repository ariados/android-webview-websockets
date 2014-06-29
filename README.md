# WebSockets support for pre-4.4 Android WebView

Before Android KitKat implemented the Chromium WebView, the older WebView did not include WebSockets
support. This library adds it via java-javascript interface.

This is based on the many phonegap-oriented websocket solutions out there.
Those rely on Cordova's excellent java to javascript serialization/communication.
This library implements its own java-javascript interface that isn't quite as good, but gets the job
done without requiring phonegap.

## Usage

Add the java interface to your webview like so:

```java
webView.addJavascriptInterface(new WebSocketFactory(webView), "WebSocketFactory");
```

"WebSocketFactory" will be the name of the interface in Javascript, and is referenced from websocket.js.

Next you need to include websocket.js on your webpage. What I do is have an empty websocket.js file
in my webapp, and use WebViewClient.shouldInterceptRequest to populate it on Android only:

```java
webView.setWebViewClient(new WebViewClient() {
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url.contains("websocket-shim.js")) {
            InputStream in = getClass().getClassLoader().getResourceAsStream("com/github/ariados/webview_websocket/websocket.js");
            if (in != null) {
                return new WebResourceResponse("text/javascript", "UTF-8", in);
            } else {
                Log.e(TAG, "could not open stream for websocket.js");
            }
        }
        return null;
    }
});
```

websocket.js will create a window.Websocket class in javascript for you if it is not present natively.
This will allow you to use Websockets in your webapp normally. For example, I include the
[Pusher](https://github.com/pusher/pusher-js) js library after my websocket.js, and it is able
to use Websockets just fine.

## Limitations

* Binary messages are not supported.
* Connections are kept open even when the containing activity is not in the foreground. This drains battery. At some point I may implement a timer to close them when the app is backgrounded for awhile.

