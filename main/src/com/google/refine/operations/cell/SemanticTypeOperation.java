/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.operations.cell;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.security.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;



public class SemanticTypeOperation extends EngineDependentMassCellOperation {
    
    @JsonCreator
    public SemanticTypeOperation(
            @JsonProperty("engineConfig")
            EngineConfig engineConfig,
            @JsonProperty("columnName")
            String columnName
        ) {
        super(engineConfig, columnName, true);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Blank down cells in column " + _columnName;
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Blank down " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        Mode engineMode = createEngine(project).getMode();
        
        return new RowVisitor() {
            int                 cellIndex;
            int 			    keyCellIndex;
            List<CellChange>    cellChanges;
            Cell                previousCell;
            Mode                engineMode;
            String				semantic_type_result;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges, Mode engineMode) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                this.engineMode = engineMode;
                this.semantic_type_result = "";
                return this;
            }

            @Override
            public void start(Project project) {
            	keyCellIndex = project.columnModel.columns.get(
                		project.columnModel.getKeyColumnIndex()).getCellIndex();
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
//                if (engineMode.equals(Mode.RecordBased) && ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))) {
//                    previousCell = null;
//                }
//                int c = project.recordModel.getRecordCount();
//                if (semantic_type_result == ""){
//                    long time1 = System.currentTimeMillis();
//                    String data = fectch_data(c, project, cellIndex, "EN");
//                    long time2 = System.currentTimeMillis();
//                    System.out.println("fectch_data() spend " + (time2-time1)/1000 + " seconds");
//                    String predict_type = predict_semantic_type(data, false);
//                    System.out.println(predict_type);
//                    long time3 = System.currentTimeMillis();
//                    System.out.println("predict_semantic_type() spend " + (time3-time2)/1000 + " seconds");
//                    this.semantic_type_result = predict_type;
//                }
//                Object value = row.getCellValue(cellIndex);
//                Cell cell = row.getCell(cellIndex);
//                if (value != null) {
//                	Cell newCell = new Cell(md5(value.toString())+"_"+semantic_type_result, null);
//                    if (newCell != null) {
//                    	CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
//                    	cellChanges.add(cellChange);
//                    }
//                }
//
                return false;
            }
        }.init(column.getCellIndex(), cellChanges, engineMode);
    }
    
    private String md5(String md5) {
    	try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
              sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
           }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        	return md5;
        }
    }
    
    public static String fectch_data(int c, Project project, int cellIndex, String language) {
    	// prepare api data
    	ArrayList<String> header = new ArrayList<String>();
 		header.add("predict_column");
 		List<List<Object>> table=new ArrayList<>();
 		
        for (int r = 0; r < c; r++) {
        	Row _row = project.rows.get(r);
        	Object value = _row.getCellValue(cellIndex);
        	ArrayList<Object> sub_table = new ArrayList<>();
    		sub_table.add(value);
    		table.add(r, sub_table);
        }
        
        JSONObject js = new JSONObject();
	    try {
			js.put("header", header);
			js.put("table", table);
			js.put("language", language);
			return js.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	    return "";
    }
    
	private static boolean isReachable(String targetUrl) throws IOException {
		HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(targetUrl).openConnection();
		httpUrlConnection.setRequestMethod("GET");

		try {
			int responseCode = httpUrlConnection.getResponseCode();
			return responseCode == HttpURLConnection.HTTP_OK;
		} catch (Exception noInternetConnection) {
			return false;
		} 
	}
    
    public static String predict_semantic_type(String data, Boolean isAll) {
        // creating stream for writing request
        OutputStream out;
        HttpURLConnection urlConnection = null;
        try {
            String target_host_name = "http://127.0.0.1:5000/api/doc";
            boolean reachable = isReachable(target_host_name);
            String host_name = reachable ? "http://127.0.0.1:5000" : "http://140.96.111.94:32290";
            String set_server_url = host_name + "/api/semantic_type/predict_semantic_type";
            URL url = new URL(set_server_url);
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
            Scanner in = new Scanner(urlConnection.getInputStream());
            String result_str = "";
            while(in.hasNext()){
            	String r = in.nextLine();
            	result_str = result_str + r ;
            }
            JSONObject jsonObject = new JSONObject(result_str);
            JSONArray result = (JSONArray) jsonObject.get("result");
            if (!isAll) {
	        	JSONArray res = (JSONArray)result.get(0);
	            return res.getString(0);
            } else {
            	JSONArray array = (JSONArray)result.get(0);
            	List<String> list = new ArrayList<String>();
            	for (int i=0; i < array.length(); i++) {
            		list.add(array.get(i).toString());
            	}
            	return String.join(",", list);
            }
	            
        } catch (IOException e) {
            e.printStackTrace();
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return "";

    }
}
