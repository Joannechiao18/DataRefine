/*******************************************************************************
 * Copyright (C) 2018, Antonin Delpeuch
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.commands.browsing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.util.ParsingUtilities;

public class GetContentAccessCommand extends Command {
	final static Logger logger = LoggerFactory.getLogger("get-content-access_command");
	
	public static String DEFAULT_ACCESS_TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwQjlwY05VeTVSdnpYMkItV1hQbWxnTXhySEp2Z2JGRDI2SzczQi1tdGo4In0.eyJleHAiOjE2NjA3MTI4MDksImlhdCI6MTY2MDcwMjAwOSwianRpIjoiYjI1MzY5Y2MtM2FlZC00YzJhLTlhNzItNTVlNGViNjcyZDI5IiwiaXNzIjoiaHR0cHM6Ly9jb3JlLnJwbS5haS1wbGF0Zm9ybS9hdXRoL3JlYWxtcy9haXBsYXRmb3JtIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImU4MWI5ZDIyLWQzNDgtNDU2NS1hZWIwLTI2OTdhMDJmODcyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFpcG9ydGFsIiwic2Vzc2lvbl9zdGF0ZSI6IjQyYWMwYTZlLTg1NmItNDVhMC05ZmZlLTUzYTQwMTJiYWZhMyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1haXBsYXRmb3JtIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib2ZmbGluZV9hY2Nlc3MgcHJvZmlsZSBlbWFpbCIsInNpZCI6IjQyYWMwYTZlLTg1NmItNDVhMC05ZmZlLTUzYTQwMTJiYWZhMyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJncm91cHMiOlsiLzNjZTQzY2ZjLTNiMGMtNDZjZS04M2Y4LTRkNTE5MzZiMGJjNSIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvQ29tcGFueSBBZG1pbiIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvMTdjZmIyOTAtZjc5NC00NWE5LTliZTAtODUzOGExODNjZDg1L0xhYmVsIE1hbmFnZXIiLCIvM2NlNDNjZmMtM2IwYy00NmNlLTgzZjgtNGQ1MTkzNmIwYmM1LzM4MzA0NDFjLWFhNjItNDQ3Mi1iZWY1LWE3ZDE2ZWFlODFjZC9Qcm9qZWN0IEFkbWluIiwiLzNjZTQzY2ZjLTNiMGMtNDZjZS04M2Y4LTRkNTE5MzZiMGJjNS85MDY3MjE5MC01MjNmLTRiYzItYTNmZC01MGI2OGQyMDJjOTMvUHJvamVjdCBBZG1pbiIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvNWJlYTQzZjAtZDBlNS00NzdmLTkyYWUtOWQyNWYwZWE0YTU0L1Byb2plY3QgQWRtaW4iLCIvM2NlNDNjZmMtM2IwYy00NmNlLTgzZjgtNGQ1MTkzNmIwYmM1LzIzNjk0NmQxLWU3MDAtNGFhMC1hNTQ0LTQzOWQ1N2RiNDFjZi9Qcm9qZWN0IEFkbWluIiwiLzNjZTQzY2ZjLTNiMGMtNDZjZS04M2Y4LTRkNTE5MzZiMGJjNS83ZjY4YjdjMi1kM2VhLTQwNGQtYjlhMi1jYmZjZWJkMjBiMzUvUHJvamVjdCBBZG1pbiIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvZWVjM2FjMDQtNmIzMC00NmEzLTljMzQtZGRkNDFiZjY3ZDllL1Byb2plY3QgQWRtaW4iLCIvM2NlNDNjZmMtM2IwYy00NmNlLTgzZjgtNGQ1MTkzNmIwYmM1L2JmMTNhZGU4LTY4ZjAtNGEzMS1iNjBkLTFiNDQ1NGFlMWI1NS9Qcm9qZWN0IEFkbWluIiwiLzNjZTQzY2ZjLTNiMGMtNDZjZS04M2Y4LTRkNTE5MzZiMGJjNS85NmJmODY2OS00ZjU1LTQxNmMtYTA2MC0yZDQ2MGQzYTZkOTYvUHJvamVjdCBBZG1pbiIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvMDhjY2I5MjItMmU4OS00YmVmLThlYzYtZjkyM2Q3MTU5NDVhL1Byb2plY3QgQWRtaW4iLCIvM2NlNDNjZmMtM2IwYy00NmNlLTgzZjgtNGQ1MTkzNmIwYmM1LzAyMzI3NzI3LTFlYjctNGEyNC1iOWZlLWZhZThiMjdiNzZmOS9Qcm9qZWN0IEFkbWluIiwiLzNjZTQzY2ZjLTNiMGMtNDZjZS04M2Y4LTRkNTE5MzZiMGJjNS83MDM5NGUzZi0wZjA4LTQ5ZGQtYjU3Mi1lZTVhOTYyNTYzNGEvUHJvamVjdCBBZG1pbiIsIi8zY2U0M2NmYy0zYjBjLTQ2Y2UtODNmOC00ZDUxOTM2YjBiYzUvMThmZTAwYWUtNmZmNC00MmFjLWI4ZmQtZjY5YTc5YjFjY2ZhL1Byb2plY3QgQWRtaW4iLCIvM2NlNDNjZmMtM2IwYy00NmNlLTgzZjgtNGQ1MTkzNmIwYmM1L2E4ZmFmMjZiLTAwOWUtNDk0Ni04ZDYyLTI5ZTYyMTg5MDI3Ni9Qcm9qZWN0IEFkbWluIl0sInByZWZlcnJlZF91c2VybmFtZSI6InVzZXIwQGl0cmkub3JnLnR3IiwiZW1haWwiOiJ1c2VyMEBpdHJpLm9yZy50dyIsInVzZXJuYW1lIjoidXNlcjAifQ.Csva2Nl6GJ1ipdxWakyBnouK9v0FYAHRL2KK8lwYUIxQmUa9CmpfURna3ic447iJ3J9EO7ps0I68O0LNH-j_npwELOZQI039kqBxRP8Q-xD2arbH4obfWikIbcnPhA9Bvk2k7273PTnoceUtbOEXSRQgLwBOFQkt0J8YayLIT3KlqCt0-kcKVnnkTFekmaAqz2L_VcK-_4e0r2oWR1DamCoRPqeCNnSYHL_x_ZTZe0wHDSAQuo7sKLwRpxq5TxHxlOIaiY4MeyIBCdyjMas6C2q6tPgAg6y5W1Jhk17cfMuTkQC2JRjOCr17QIXZvGoBA8FYdItwGOfXAKAruPztDA";
	public static String DEFAULT_PROJECT_ID = "3830441c-aa62-4472-bef5-a7d16eae81cd";
	public static String DEFAULT_API_URL = "http://140.96.111.4:31234";
	public static String DEFAULT_SERVICE_DOMAIN = "http://content-access";
	static String env_default_domain = System.getenv("DEFAULT_DOMAIN");
	static String default_domain = env_default_domain != null ? env_default_domain : "core.rpm.ai-platform";
	public static String DEFAULT_STATUS_URL = "https://" + default_domain;
	

	private static Map<String, Object> genResponse(int result, Object data, String message) {
		Map<String, Object> responseJSON = new HashMap<>();
		responseJSON.put("result", result);
		if (data != "") {
			responseJSON.put("data", data);
		}
		responseJSON.put("message", message);
		return responseJSON;
	}
	
	public static class HttpsTrustManager implements X509TrustManager {
	    private static TrustManager[] trustManagers;
	    private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

	    @Override
	    public void checkClientTrusted(
	            X509Certificate[] x509Certificates, String s)
	            throws java.security.cert.CertificateException {

	    }

	    @Override
	    public void checkServerTrusted(
	            X509Certificate[] x509Certificates, String s)
	            throws java.security.cert.CertificateException {

	    }

	    public boolean isClientTrusted(X509Certificate[] chain) {
	        return true;
	    }

	    public boolean isServerTrusted(X509Certificate[] chain) {
	        return true;
	    }

	    @Override
	    public X509Certificate[] getAcceptedIssuers() {
	        return _AcceptedIssuers;
	    }

	    public static void allowAllSSL() {
	        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

	            @Override
	            public boolean verify(String arg0, SSLSession arg1) {
	                return true;
	            }

	        });

	        SSLContext context = null;
	        if (trustManagers == null) {
	            trustManagers = new TrustManager[]{new HttpsTrustManager()};
	        }

	        try {
	            context = SSLContext.getInstance("TLS");
	            context.init(null, trustManagers, new SecureRandom());
	        } catch (NoSuchAlgorithmException | KeyManagementException e) {
	            e.printStackTrace();
	        }

	        HttpsURLConnection.setDefaultSSLSocketFactory(context != null ? context.getSocketFactory() : null);
	    }
	}

	public static boolean isReachable(String targetUrl) throws IOException {
		HttpsTrustManager.allowAllSSL();
		HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(targetUrl).openConnection();
		httpUrlConnection.setRequestMethod("GET");

		try {
			int responseCode = httpUrlConnection.getResponseCode();
			return responseCode == HttpURLConnection.HTTP_OK;
		} catch (UnknownHostException noInternetConnection) {
			return false;
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Properties parameters = ParsingUtilities.parseUrlParameters(request);
		String operation = parameters.getProperty("operation");
		
		boolean reachable = isReachable(DEFAULT_SERVICE_DOMAIN);
		String api_url = reachable ? DEFAULT_SERVICE_DOMAIN : DEFAULT_API_URL;
		String env_project_id = System.getenv("PROJECT_ID");
		String project_id = env_project_id != null ? env_project_id : DEFAULT_PROJECT_ID;
		String requestURL = api_url + "/projects/" + project_id;
		String para_accessToken = parameters.getProperty("access_token");
		boolean isinvalid = (!para_accessToken.equals("") & !para_accessToken.equals("null") & para_accessToken != null);
		String accessToken = isinvalid ? "Bearer " + para_accessToken : DEFAULT_ACCESS_TOKEN;
		if ("list".equals(operation)) {
			// GET
			List<Object> resObj = doGet(requestURL, accessToken);
			String result_str = (String) resObj.get(0);
			HttpURLConnection con = (HttpURLConnection) resObj.get(1);
			
			if (con.getResponseCode() == 401 | con.getResponseCode() == 403) {
				respondJSON(response, genResponse(1, "", "Missing or invalid access_token parameter"));
			} else if (con.getResponseCode() == 200){
				JSONArray result = new JSONArray(result_str);
				String download_host = reachable ? DEFAULT_SERVICE_DOMAIN : "http://127.0.0.1:3333" ;
				String download_file_url = download_host + "/command/core/content-access?accessToken="+ accessToken +"&operation=download&file_name=" ;
				ArrayList<Map<String, String>> array = new ArrayList<>();
				for (int i = 0; i < result.length(); i++) {
					HashMap<String, String> map = new HashMap<>();
					String res_filename = result.getJSONObject(i).get("filename").toString();
					map.put("username", result.getJSONObject(i).get("username").toString());
					map.put("filename", result.getJSONObject(i).get("filename").toString());
					map.put("project", result.getJSONObject(i).get("project").toString());
					map.put("datatype", result.getJSONObject(i).get("datatype").toString());
					map.put("contenttype", result.getJSONObject(i).get("contenttype").toString());
					map.put("created_at", result.getJSONObject(i).get("created_at").toString());
					map.put("version", result.getJSONObject(i).get("version").toString());
					map.put("file_url", download_file_url + res_filename);
					array.add(map);
				}
				respondJSON(response, genResponse(0, array, ""));
			} else {
				respondJSON(response, genResponse(1, "", con.getResponseMessage()));
			}
		} else if ("upload".equals(operation)) {
			// POST
			String file_path = parameters.getProperty("file_path");
			HttpURLConnection con = uploadFile(new File(file_path), requestURL, accessToken);
			if (con.getResponseCode() == 200) {
				respondJSON(response, genResponse(0, "", "success"));
			} else {
				respondJSON(response, genResponse(1, "", con.getResponseMessage()));
			}

		} else if ("download".equals(operation)) {
			// GET
			String file_name = parameters.getProperty("file_name");
			String openrefine_data = System.getenv("ENV_OPENREFINE_DATA");
			String local_path = openrefine_data != null ? openrefine_data : "C:\\\\\\\\Users/User/Desktop";

			requestURL = requestURL + "/files/" + file_name;
			String fname = file_name.substring(0, file_name.lastIndexOf('.'));
			String file_extension = file_name.substring(file_name.lastIndexOf('.') + 1);
			HttpURLConnection con = downloadFile(requestURL, accessToken,
					local_path + "/" + fname + "." + file_extension);
			if (con.getResponseCode() == 200) {
				downloadResponse(request, response, local_path + "/" + fname + "." + file_extension,
						fname + "." + file_extension);
			} else {
				respondJSON(response, genResponse(1, "", con.getResponseMessage()));
			}
		} else {
			HttpUtilities.respond(response, "error", "No such sub command");
		}
	}

	private void downloadResponse(HttpServletRequest request, HttpServletResponse response, String file_path,
			String file_name) throws IOException {

		int BUFFER = 1024 * 10;
		byte data[] = new byte[BUFFER];
		BufferedInputStream bis = null;
		FileInputStream fis = new FileInputStream(file_path);

		bis = new BufferedInputStream(fis, BUFFER);
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/OCTET-STREAM");
		response.setHeader("Content-Disposition", "attachment; filename=" + file_name + ";");

		int read;
		ServletOutputStream out = response.getOutputStream();
		while ((read = bis.read(data)) != -1) {
			out.write(data, 0, read);
		}
		fis.close();
		bis.close();
	}

	private List<Object> doGet(String set_server_url, String access_token) throws JSONException {
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(set_server_url);
			// creating connection
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setRequestProperty("Authorization", access_token);
			urlConnection.setRequestProperty("Accept", "application/json");
			// reading response
			Scanner in;
			if (100 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 399) {
				in = new Scanner(urlConnection.getInputStream(), "UTF-8");
			} else {
				in = new Scanner(urlConnection.getErrorStream(), "UTF-8");
			}
			
			String result_str = "";
			while (in.hasNext()) {
				String r = in.nextLine();
				result_str = result_str + r;
			}
			return Arrays.asList(result_str, urlConnection);

		} catch (IOException e) {
			e.printStackTrace();
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		if (urlConnection != null) {
			urlConnection.disconnect();
		}
		return null;
	}

	private HttpURLConnection downloadFile(String api_url, String access_token, String file_path) throws IOException {
		URL url = new URL(api_url);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", access_token);
		connection.setRequestProperty("Accept", "application/json");

		InputStream in = null;
		FileOutputStream out = new FileOutputStream(file_path);

		in = connection.getInputStream();
		int read = -1;
		byte[] buffer = new byte[4096];
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
			System.out.println("[SYSTEM/INFO]: Downloading file...");
		}
		in.close();
		out.close();

		return connection;
	}

	public static HttpURLConnection uploadFile(File uploadFile, String url, String access_token) throws IOException {
		String EOL = "\r\n";
		String fileName = uploadFile.getName();
		try (FileInputStream file = new FileInputStream(uploadFile)) {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			final String boundary = UUID.randomUUID().toString();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setRequestProperty("Authorization", access_token);
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			try (OutputStream out = con.getOutputStream()) {
				out.write(("--" + boundary + EOL + "Content-Disposition: form-data; name=\"file\"; " + "filename=\""
						+ fileName + "\"" + EOL + "Content-Type: text/csv" + EOL + EOL)
								.getBytes(StandardCharsets.UTF_8));
				byte[] buffer = new byte[128];
				int size = -1;

				while (-1 != (size = file.read(buffer))) {
					out.write(buffer, 0, size);
				}
				out.write((EOL + "--" + boundary + "--" + EOL).getBytes(StandardCharsets.UTF_8));
				out.flush();
				System.err.println(con.getResponseMessage());
				return con;
			} finally {
				con.disconnect();
			}
		}
	}
	
	public static void setStatus(String data) throws Exception {
        // creating stream for writing request
        OutputStream out;
        HttpURLConnection urlConnection = null;
        try {
        	String api_path = "/rpm/api/v1/time_series/data_refine/progressing";
            String set_server_url = DEFAULT_STATUS_URL + api_path;
            URL url = new URL(set_server_url);
            System.out.println(set_server_url);
            HttpsTrustManager.allowAllSSL();
            // creating connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setDoOutput(true); // setting POST method
            
            out = urlConnection.getOutputStream();
            byte[] input = data.getBytes("utf-8");
            out.write(input, 0, input.length);	
            // reading response
            Scanner in;
            boolean isError = false;
			if (100 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 399) {
				in = new Scanner(urlConnection.getInputStream(), "UTF-8");
			} else {
				in = new Scanner(urlConnection.getErrorStream(), "UTF-8");
				isError = true;
			}
			
            String result_str = "";
            while(in.hasNext()){
            	String r = in.nextLine();
            	result_str = result_str + r ;
            }
           
            System.out.println(result_str);
            if (isError) {
            	throw new Exception(result_str);
            }

        } catch (IOException e) {
            e.printStackTrace();
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
