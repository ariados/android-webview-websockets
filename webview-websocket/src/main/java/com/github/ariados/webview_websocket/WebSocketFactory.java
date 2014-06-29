package com.github.ariados.webview_websocket;


import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The <tt>WebSocketFactory</tt> is a helper class to instantiate new
 * WebSocketInterface instances from Javascript side.
 */
public class WebSocketFactory {
    private static final String TAG = "websocket.WebSocketFactory";

    WebView appView;
    // messages waiting to be sent to the various WebSocket instances in the browser
    private List<JSONObject> messageQueue = new ArrayList<JSONObject>();
    // check-in spot for websocket objects to find stale ones no longer referenced in browser
    Map<String, WebSocketInterface> instances = new HashMap<String, WebSocketInterface>();

    public WebSocketFactory(WebView appView) {
        this.appView = appView;
    }

    @JavascriptInterface
    public WebSocketInterface getInstance(String url, String optionsJson) {
        WebSocketInterface socket = null;
        try {
            String id = getRandomUniqueId();
            socket = new WebSocketInterface(appView, this, new URI(url), new JSONObject(optionsJson), id);
            addInstance(id, socket);
            socket.connect();
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return socket;
    }


    public void addInstance(String socketId, WebSocketInterface instance) {
        instances.put(socketId, instance);
    }

    @JavascriptInterface
    public void closeUnreferenced(String id) {
        WebSocketInterface ws = instances.get(id);
        if (ws == null) {
            Log.e(TAG, "instance not found? " + id);
            return;
        }
        WebSocket.READYSTATE s = ws.getReadyStateEnum();
        boolean closed = s == WebSocket.READYSTATE.CLOSING || s== WebSocket.READYSTATE.CLOSED;
        if (!closed) {
            Log.v(TAG, "closing stale websocket due to browser not referencing it " + id);
            ws.close();
        }
    }

    /**
     * Add a message for the poller to pick up later
     * @param target the instance id of the websocket object this message should be sent to
     */
    public void postMessage(String target, String event, String msg) {
        try {
            JSONObject json = new JSONObject();
            json.put("_target", target);
            json.put("event", event);
            json.put("data", msg);
            messageQueue.add(json);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * The polling javascript fetches new messages from here, and routes
     * them to the appropriate WebSocket instances
     */
    @JavascriptInterface
    public String fetchMessages() {
        JSONArray messages = new JSONArray(messageQueue);
        messageQueue.clear();
        return messages.toString();
    }

    /**
     * Generates random unique ids for WebSocketInterface instances, used for
     * directing callbacks from the native side to the appropriate instance.
     *
     */
    private String getRandomUniqueId() {
        return "WEBSOCKET." + new Random().nextInt(100);
    }

}