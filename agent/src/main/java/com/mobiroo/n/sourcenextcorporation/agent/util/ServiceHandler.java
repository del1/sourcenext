package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.app.Activity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Pritam on 28/06/17.
 */

public class ServiceHandler {

    public final static int GET = 1;
    public final static int POST = 2;
    public final static int PUT = 3;
    public final static int DELETE = 4;
    public final static int PATCH = 5;
    public static int STATUS_CODE = 0;

    Activity mActivity;
    StringBuilder sbResponse;

    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';

    private static final int TIMEOUT = 30000;

    public ServiceHandler() {
    }

    /**
     * function to make a http call
     * @param stringUrl
     * @param method
     * @param urlParameters
     * @param activity
     * @param isJsonRequest
     * @return
     */
    public String makeServiceCall(String stringUrl, int method, String urlParameters, Activity activity,
                                  boolean isJsonRequest) {
        this.mActivity = activity;
        HttpURLConnection httpURLConnection = null;
        try {
            if (method == GET) {
                if (urlParameters != null) {
                    stringUrl = stringUrl + "?" + urlParameters;
                }
                Log.e(Constants.TAG,"URL ==> "+stringUrl);
                URL url = new URL(stringUrl);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setAllowUserInteraction(false);
                httpURLConnection.setConnectTimeout(TIMEOUT);
                httpURLConnection.setReadTimeout(TIMEOUT);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoOutput(false);
                httpURLConnection.connect();

            } else if (method == POST) {
                URL url = new URL(stringUrl);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setAllowUserInteraction(false);
                httpURLConnection.setConnectTimeout(TIMEOUT);
                httpURLConnection.setReadTimeout(TIMEOUT);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                if (urlParameters != null) {
                    if (isJsonRequest) {
                        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    } else {
                        httpURLConnection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                    }
                    httpURLConnection.connect();
                    OutputStream os = httpURLConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(urlParameters);
                    writer.flush();
                    writer.close();
                    os.close();
                }
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            sbResponse = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sbResponse.append(line + "\n");
            }
            br.close();
            STATUS_CODE = httpURLConnection.getResponseCode();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                STATUS_CODE = httpURLConnection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        httpURLConnection.disconnect();

        if(sbResponse!=null) {
            return sbResponse.toString();
        } else {
            return null;
        }
    }

    /**
     * function to create urlencoded parameter string
     * @param parameters
     * @return
     */
    public String createQueryStringForParameters(Map<String, String> parameters) {
        StringBuilder parametersAsQueryString = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;

            for (String parameterName : parameters.keySet()) {
                if (!firstParameter) {
                    parametersAsQueryString.append(PARAMETER_DELIMITER);
                }

                try {
                    parametersAsQueryString
                            .append(parameterName)
                            .append(PARAMETER_EQUALS_CHAR)
                            .append(URLEncoder.encode(
                                    parameters.get(parameterName), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                firstParameter = false;
            }
        }
        return parametersAsQueryString.toString();
    }
}
