package com.github.ariados.webview_websocket;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.java_websocket.WebSocket.READYSTATE;

/**
 * Lets a WebView communicate with a org.java_websocket.WebSocketClient object.
 */
public class WebSocketInterface {
    private static final String TAG = "websocket.WebSocketInterface";
    /** An empty string for no-argument events */
    private static String BLANK_MESSAGE = "";
    // javascript method names
    private static final String EVENT_ON_OPEN = "onopen";
    private static final String EVENT_ON_MESSAGE = "onmessage";
    private static final String EVENT_ON_CLOSE = "onclose";
    private static final String EVENT_ON_ERROR = "onerror";
    private static final Map<READYSTATE, Integer> stateMap = new HashMap<READYSTATE, Integer>() {{
        put(READYSTATE.CONNECTING, 0);
        put(READYSTATE.OPEN, 1);
        put(READYSTATE.CLOSING, 2);
        put(READYSTATE.CLOSED, 3);
        put(READYSTATE.NOT_YET_CONNECTED, 3);
    }};


    private final WebView appView;
    /** The WebSocketFactory for keeping track of references in the browser */
    private final WebSocketFactory webSocketFactory;
    private URI uri;
    private Map<String, String> headers;
    private WebSocketClient wsClient;
    private FrameBuilder frameBuilder = new FramedataImpl1();
    /** The unique id for this instance (helps to bind this to javascript events) */
    private String id;

    WebSocketInterface(WebView appView, WebSocketFactory wsf, URI uri, JSONObject options, String id) {
        this.appView = appView;
        this.webSocketFactory = wsf;
        this.id = id;
        this.uri = uri;
        this.headers = jsonToMap(options);
        setRcvBufSize(options);
        this.setCookie();

        final WebSocketInterface outerclass = this;
        wsClient = new MyWebSocketClient(uri, new Draft_17(), headers, options) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                outerclass.onOpen();
            }
            @Override
            public void onMessage(String message) {
                outerclass.onMessage(message);
            }
            @Override
            public void onMessage(ByteBuffer bytes) {
                Log.e(TAG, "binary onMessage");
                //JSONArray jsonArr = Utils.byteArrayToJSONArray(bytes.array());
                //sendResult(jsonArr, "messageBinary", true);
            }
            @Override
            public void onFragment(Framedata frame) {
                try {
                    outerclass.frameBuilder.append(frame);

                    if (frame.isFin()) {
                        ByteBuffer bytes = outerclass.frameBuilder.getPayloadData();
                        if (outerclass.frameBuilder.getOpcode() == Framedata.Opcode.BINARY) {
                            this.onMessage(bytes);
                        } else {
                            this.onMessage(new String(bytes.array(), "UTF-8"));
                        }
                        outerclass.frameBuilder.getPayloadData().clear();
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "", e);
                }
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {
                outerclass.onClose();
            }
            @Override
            public void onError(Exception ex) {
                outerclass.onError(ex);
            }
        };
    }

    @JavascriptInterface
    public void connect() throws IOException {
        wsClient.connect();
    }

    @JavascriptInterface
    public void close() {
        Log.v(TAG, "close");
        wsClient.close();
    }

    @JavascriptInterface
    public void send(final String text) {
        Log.v(TAG, "send " + text);
        wsClient.send(text);
    }

    @JavascriptInterface
    public String getId() {
        return id;
    }

    @JavascriptInterface
    public int getReadyState() {
        return convertReadyStateToInt(wsClient.getReadyState());
    }

    public READYSTATE getReadyStateEnum() {
        return wsClient.getReadyState();
    }

//    private boolean gotMessage = false;
    public void onMessage(final String msg) {
        Log.v(TAG, "onMessage " + msg);
        webSocketFactory.postMessage(id, EVENT_ON_MESSAGE, msg);
//        appView.post(new Runnable() {
//            @Override
//            public void run() {
//                if (!gotMessage) {
//                    gotMessage = true;
//                    Toast.makeText(appView.getContext(), "Got a pusher message", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    public void onOpen() {
        webSocketFactory.postMessage(id, EVENT_ON_OPEN, BLANK_MESSAGE);
    }

    public void onClose() {
        webSocketFactory.postMessage(id, EVENT_ON_CLOSE, BLANK_MESSAGE);
    }

    public void onError(final Throwable t) {
        String msg = t.getMessage() + "";
        webSocketFactory.postMessage(id, EVENT_ON_ERROR, msg);
    }

    private static Map<String, String> jsonToMap(JSONObject data) {
        Iterator<String> keys = data.keys();
        Map<String, String> result = new HashMap<String, String>();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, data.opt(key) + "");
        }
        return result;
    }

    private static void setRcvBufSize(JSONObject options) {
        if (options.has("rcvBufSize")) {
            WebSocketImpl.RCVBUF = options.optInt("rcvBufSize");
        }
    }

    private void setCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(this.uri.getHost());
        if (cookie != null) {
            this.headers.put("cookie", cookie);
        }
    }

    public static int convertReadyStateToInt(READYSTATE state) {
        return stateMap.get(state);
    }
}