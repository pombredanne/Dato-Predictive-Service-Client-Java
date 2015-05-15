package com.dato.deploy;

import com.ning.http.client.*;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.json.simple.JSONObject;

/**
 * This is the Predictive Service Java Client that consumes the service that 
 * is provided by Dato Predictive Service.
 *
 */
public class PredictiveServiceClient {
    /**
     * Predictive Service Client
     */
    private String api_key;
    private String endpoint;
    private boolean should_verify_certificate;
    private int timeout;
    private AsyncHttpClient asyncClient;

    /**
     *  Construct a new PredictiveServiceClient
     *
     *  PredictiveServiceClient may be instantiated by passing
     *  endpoint/api_key/should_verify_certificate to the constructor.
     *
     * @param endpoint
     *      HTTP end point to the Dato Predictive Service.
     * @param api_key
     *      API key generated by the Dato Predictive Service.
     * @param should_verify_certificate
     *      True, if the user wants to verify SSL certificate on the Predictive
     *      Service; False, if otherwise.
     */
    public PredictiveServiceClient(String endpoint, String api_key,
                                   boolean should_verify_certificate) {
        this.endpoint = endpoint;
        this.api_key = api_key;
        this.should_verify_certificate = should_verify_certificate;

        // ssl verification
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
                .setAcceptAnyCertificate(!should_verify_certificate)
                .build();
        this.asyncClient = new AsyncHttpClient(config);

        this.timeout = 10000; // default to 10 seconds timeout
        initConnection(); // initialize a connection to Predictive Service.
    }

    /**
     * Construct a new PredictiveServiceClient from the file path to a
     * Dato Predictive Service client configuration INI file.
     *
     * @param filepath
     *      String representation of the file path to the configuration file.
     * @return
     *      PredictiveServiceClient generated with the specified configurations.
     */
    public static PredictiveServiceClient fromConfigFile(String filepath) {
        File file = new File(filepath);
        return fromConfigFile(file);
    }

    /**
     * Construct a new PredictiveServiceClient from the file  to a
     * Dato Predictive Service client configuration INI file.
     *
     * @param file
     *      File handle to the configuration file.
     * @return
     *      PredictiveServiceClient generated with the specified configurations.
     */
    public static PredictiveServiceClient fromConfigFile(File file) {
        Wini ini = null;
        try {
            // load ini file
            ini = new Wini(file);
        } catch (InvalidFileFormatException e) {
            throw new PredictiveServiceClientException("Invalid config"
                    + " file format.", e);
        } catch (IOException e) {
            throw new PredictiveServiceClientException("Error while reading"
                    + " the config file.", e);
        }

        // parse configurations
        String endpoint = ini.get("Service Info", "endpoint", String.class);
        String api_key = ini.get("Service Info", "api key", String.class);
        boolean should_verify = ini.get("Service Info", "verify certificate",
                boolean.class);

        return new PredictiveServiceClient(endpoint, api_key, should_verify);
    }
    /**
     * Query the Predictive Service for predictive object, by giving the name
     * of predictive object and a constructed JSONObject for query data.
     *
     * @param predictive_object_name
     *      String name of the predictive object to query.
     * @param request
     *      JSONObject constructed to query the
     *      predictive object.
     * @return
     *      PredictiveServiceClientResponse containing the response from the
     *      Predictive Service.
     */
    @SuppressWarnings("unchecked")
    public PredictiveServiceClientResponse query(String predictive_object_name,
                                                 JSONObject request) {
        String url = constructURL(this.endpoint)
                    + "/query/" + predictive_object_name;

        JSONObject requestJSON = new JSONObject();
        requestJSON.put("data", request);
        requestJSON.put("api_key", this.getApikey());

        return postRequest(url, requestJSON);
    }

    /**
     * Send feedback to the Predictive Service for a specific query result.
     *
     * @param request_id
     *      String representation of the UUID from a query result.
     * @param data
     *      JSONObject constructed to send any additional attributes and
     *      value pairs associated with the query result.
     * @return
     *      PredictiveServiceClientResponse containing the response from the
     *      Predictive Service.
     */
    @SuppressWarnings("unchecked")
    public PredictiveServiceClientResponse feedback(
                    String request_id,
                    JSONObject data) {
        String url = constructURL(this.endpoint) + "/feedback";

        JSONObject requestJSON = new JSONObject();
        requestJSON.put("data", data);
        requestJSON.put("id", request_id);
        requestJSON.put("api_key", this.getApikey());

        return postRequest(url, requestJSON);
    }

    /*
     * Send POST request to Predictive Service given the URL and request body
     * in JSON format.
     */
    private PredictiveServiceClientResponse postRequest(String url,
                                    JSONObject requestJSON) {
        BoundRequestBuilder asyncRequest = asyncClient.preparePost(url);
        asyncRequest.setHeader("content-type", "application/json");
        asyncRequest.setBody(requestJSON.toJSONString());
        asyncRequest.setRequestTimeout(this.timeout);

        Future<Response> response = asyncRequest.execute();
        return new PredictiveServiceClientResponse(response);
    }

    /*
     * Send GET request to Predictive Service given the URL.
     */
    private PredictiveServiceClientResponse getRequest(String url) {
        Future<Response> response = asyncClient.prepareGet(url).execute();
        return new PredictiveServiceClientResponse(response);
    }

    /*
     * Initialize a connection to the Predictive Service.
     */
    private void initConnection() {
        String url = constructURL(this.endpoint);
        PredictiveServiceClientResponse response = getRequest(url);
        if (response.getStatusCode() == 200) {
            // successfully connected
            return;
        } else {
            throw new PredictiveServiceClientException(
                    "Error connecting to service: response: " +
                    response.getErrorMessage());
        }
    }

    /**
     * Set Predictive Service endpoint (Load Balancer DNS).
     *
     * @param endpoint
     *      String representation of the Predictive Service Load Balancer DNS.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Returns the Predictive Service endpoint (Load Balancer DNS).
     *
     * @return
     *      String representation of Predictive Service Load Balancer DNS.
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Set API key used to query the Predictive Service.
     *
     * @param api_key
     *      API key generated by the Predictive Service.
     */
    public void setApikey(String api_key) {
        this.api_key = api_key;
    }

    /**
     * Returns the API key used to query the Predictive Service.
     *
     * @return
     *      String representation of API key used to query Predictive Service.
     */
    public String getApikey() {
        return this.api_key;
    }

    /**
     * Set the request timeout, in milliseconds, when querying the Predictive
     * Service.
     *
     * @param milliseconds
     *      request timeout to Predictive Service in milliseconds.
     */
    public void setQueryTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }

    /**
     * Get the request timeout, in milliseconds, when querying the Predictive
     * Service.
     *
     * @return
     *      request timeout to Predictive Service in milliseconds.
     */
    public int getQueryTimeout() {
        return this.timeout;
    }

    /**
     * Set if the client wants to verify Predictive Service's certificate.
     * Note: For Predictive Service that is launched with a self-signed
     * certificate or without certificate, the client should not verify the
     * certificate.
     *
     * @param should_verify_certificate
     *      True if the client should verify the Predictive Service's
     *      certificate, otherwise False.
     */
    public void setShouldVerifyCertificate(boolean should_verify_certificate) {
        if (this.should_verify_certificate != should_verify_certificate) {
            // reset async client with new ssl verification config
            AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
                    .setAcceptAnyCertificate(!should_verify_certificate)
                    .build();
            this.asyncClient = new AsyncHttpClient(config);
            this.should_verify_certificate = should_verify_certificate;
        }
    }

    /**
     * Returns True if the client is verifying Predictive Service's certificate
     * when querying or sending feedback, otherwise returns False.
     *
     * @return
     *      True if verifying Predictive Service's certificate, otherwise False.
     */
    public boolean getShouldVerifyCertificate() {
        return this.should_verify_certificate;
    }

    /*
     * Construct a valid URL based on the given Predictive Service endpoint.
     */
    private String constructURL(String endpoint) {
        String url = endpoint;
        if (endpoint.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!endpoint.toLowerCase().startsWith("http://") &&
                !endpoint.toLowerCase().startsWith("https://")) {
            throw new PredictiveServiceClientException(
                    "Error: endpoint " + this.endpoint + " does not contain"
                    + " a protocol (http:// or https://)");
        }
        return url;
    }
}