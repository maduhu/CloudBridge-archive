/*
 * Copyright 2010 Cloud.com, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.stack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.bridge.util.JsonAccessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * CloudStackClient implements a simple CloudStack client object, it can be used to execute CloudStack commands 
 * with JSON response
 * 
 * @author Kelven Yang
 */
public class CloudStackClient {
    protected final static Logger logger = Logger.getLogger(CloudStackClient.class);
    
	private String _serviceUrl;
	
	private long _pollIntervalMs = 1000;			// 1 second polling interval
	private long _pollTimeoutMs = 600000;			// 10 minutes polling timeout

	public CloudStackClient(String serviceRootUrl) {
		assert(serviceRootUrl != null);
		
		if(!serviceRootUrl.endsWith("/"))
			_serviceUrl = serviceRootUrl + "/api?";
		else
			_serviceUrl = serviceRootUrl + "api?";
	}
	
	public CloudStackClient(String cloudStackServiceHost, int port, boolean bSslEnabled) {
		StringBuffer sb = new StringBuffer();
		if(!bSslEnabled) {
			sb.append("http://" + cloudStackServiceHost);
			if(port != 80)
				sb.append(":").append(port);
		} else {
			sb.append("https://" + cloudStackServiceHost);
			if(port != 443)
				sb.append(":").append(port);
		}
		
		//
		// If the CloudStack root context path has been from /client to some other name
		// use the first constructor instead
		//
		sb.append("/client/api");
		sb.append("?");
		_serviceUrl = sb.toString();
	}
	
	public CloudStackClient setPollInterval(long intervalMs) {
		_pollIntervalMs = intervalMs;
		return this;
	}
	
	public CloudStackClient setPollTimeout(long pollTimeoutMs) {
		_pollTimeoutMs = pollTimeoutMs;
		return this;
	}
	
	public <T> T call(CloudStackCommand cmd, String apiKey, String secretKey, 
		String responseName, String responseObjName, Class<T> responseClz)	throws Exception {
		
		assert(responseName != null);
		assert(responseObjName != null);
		
		JsonAccessor json = execute(cmd, apiKey, secretKey);
		if(json.tryEval(responseName + ".jobid") != null) {
			long startMs = System.currentTimeMillis();
	        while(System.currentTimeMillis() -  startMs < _pollTimeoutMs) {
				CloudStackCommand queryJobCmd = new CloudStackCommand("queryAsyncJobResult");
	        	queryJobCmd.setParam("jobId", json.getAsString(responseName + ".jobid"));
	        	
	        	JsonAccessor queryAsyncJobResponse = execute(queryJobCmd, apiKey, secretKey);

	    		if(queryAsyncJobResponse.tryEval("queryasyncjobresultresponse") != null) {
	    			int jobStatus = queryAsyncJobResponse.getAsInt("queryasyncjobresultresponse.jobstatus");
	    			switch(jobStatus) {
	    			case 2:
	    	    		throw new Exception(queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errorcode") + " " + 
    	    				queryAsyncJobResponse.getAsString("queryasyncjobresultresponse.jobresult.errortext"));
	    	    		
	    			case 0 :
	            	    try { 
	            	    	Thread.sleep( _pollIntervalMs ); 
	            	    } catch( Exception e ) {}
	            	    break;
	            	    
	    			case 1 :
	    				return (T)(new Gson()).fromJson(queryAsyncJobResponse.eval("queryasyncjobresultresponse.jobresult." + responseObjName), responseClz);
	    				
	    			default :
	    				assert(false);
	                    throw new Exception("Operation failed - invalid job status response");
	    			}
	    		} else {
	                throw new Exception("Operation failed - invalid JSON response");
	    		}
	        }
	        
            throw new Exception("Operation failed - async-job query timed out");
		} else {
			return (T)(new Gson()).fromJson(json.eval(responseName + "." + responseObjName), responseClz);
		}
	}

	// collectionType example :  new TypeToken<List<String>>() {}.getType();
	public <T> List<T> listCall(CloudStackCommand cmd, String apiKey, String secretKey, 
		String responseName, String responseObjName, Type collectionType)	throws Exception {
		
		assert(responseName != null);
		assert(responseObjName != null);
		
		JsonAccessor json = execute(cmd, apiKey, secretKey);
		return (new Gson()).fromJson(json.eval(responseName + "." + responseObjName), collectionType);
	}

	public JsonAccessor execute(CloudStackCommand cmd, String apiKey, String secretKey) throws Exception {
		JsonParser parser = new JsonParser();
		URL url = new URL(_serviceUrl + cmd.signCommand(apiKey, secretKey));
		
		if(logger.isDebugEnabled())
			logger.debug("Cloud API call + [" + url.toString() + "]");
		
        URLConnection connect = url.openConnection();
        
        int statusCode;
        statusCode = ((HttpURLConnection)connect).getResponseCode();
        if(statusCode >= 400) {
        	logger.error("Cloud API call + [" + url.toString() + "] failed with status code: " + statusCode);
        	throw new IOException("CloudStack API call HTTP response error, HTTP status: " + statusCode);
        }
        
        InputStream inputStream = connect.getInputStream(); 
		JsonElement jsonElement = parser.parse(new InputStreamReader(inputStream));
		if(jsonElement == null) {
        	logger.error("Cloud API call + [" + url.toString() + "] failed: unable to parse expected JSON response");
        	
        	throw new IOException("CloudStack API call error : invalid JSON response");
		}
		
		if(logger.isDebugEnabled())
			logger.debug("Cloud API call + [" + url.toString() + "] returned: " + jsonElement.toString());
		return new JsonAccessor(jsonElement);
	}
}
