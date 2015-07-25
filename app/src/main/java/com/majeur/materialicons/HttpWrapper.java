package com.majeur.materialicons;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public final class HttpWrapper {

    static String makeRequest(String uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            } else {
                //Close the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString;
    }

    static void downloadFile(String urlStr, OutputStream outputStream) {
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.connect();

            InputStream urlStream = url.openStream();
            InputStream inputStream = new BufferedInputStream(urlStream, 8192);

            byte data[] = new byte[1024];
            int count;
            while ((count = inputStream.read(data)) != -1)
                // writing data to file
                outputStream.write(data, 0, count);

            outputStream.flush();
            outputStream.close();
            inputStream.close();
            urlStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
