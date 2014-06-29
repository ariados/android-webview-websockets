package com.github.ariados.webview_websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.json.JSONObject;

import java.net.URI;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Add unsigned ssl support to java_websocket
 */
public abstract class MyWebSocketClient extends WebSocketClient {
    public MyWebSocketClient(URI serverURI, Draft draft, Map<String, String> headers, JSONObject options) {
        super(serverURI, draft, headers, 0);

        if (serverURI.getScheme().equals("wss")) {
            final boolean allowSelfSignedCertificates = options.optBoolean("allowSelfSignedCertificates", false);
            final boolean allowExpiredCertificates = options.optBoolean("allowExpiredCertificates", false);
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                final TrustManager[] tm;
                if (allowSelfSignedCertificates || allowExpiredCertificates) {
                    tm = new TrustManager[]{new InsecureX509TrustManager(null, allowSelfSignedCertificates, allowExpiredCertificates)};
                } else {
                    tm = null;
                }
                sslContext.init(null, tm, null);
                SSLSocketFactory factory = sslContext.getSocketFactory();
                this.setSocket(factory.createSocket());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getResourceDescriptor() {
        return "*";
    }
}
