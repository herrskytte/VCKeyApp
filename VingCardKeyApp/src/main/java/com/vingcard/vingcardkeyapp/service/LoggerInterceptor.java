package com.vingcard.vingcardkeyapp.service;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import android.util.Log;

public class LoggerInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {

		Log.e("REST", request.getMethod() + " to " + request.getURI());
		//Log.e("REST", "Request Headers: " + request.getHeaders());
		Log.e("REST", "Request body: " + new String(body));

		ClientHttpResponse response = execution.execute(request, body);

		//Log.e("REST", "Response Headers: " + response.getHeaders());
		Log.e("REST", "Response Status: " + response.getStatusCode() + ": " + response.getStatusText());
		
		//Outputs response but kills the stream. Only for debugging errors.
		//java.util.Scanner s = new java.util.Scanner(response.getBody()).useDelimiter("\\A");
		//Log.e("REST", "Response Body: " + (s.hasNext() ? s.next() : ""));

		return response;
	}
	
}