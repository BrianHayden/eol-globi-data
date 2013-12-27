package org.eol.globi.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

public class HttpUtil {

    public static final int FIVE_MINUTES_IN_MS = 5 * 60 * 1000;

    public static HttpClient createHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), FIVE_MINUTES_IN_MS);
        return httpClient;
    }
}
