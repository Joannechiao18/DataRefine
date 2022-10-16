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



public class AverageOperation extends EngineDependentMassCellOperation {
    
    @JsonCreator
    public AverageOperation(
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
            Float				average;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges, Mode engineMode) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                this.engineMode = engineMode;
                this.average = null;
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
                if (engineMode.equals(Mode.RecordBased) && ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))) {
                    previousCell = null;
                }
                int c = project.recordModel.getRecordCount();
        		if (this.average == null){
                    float average_val = calAverage(c, project, cellIndex);
        			this.average = average_val;
        		}
        			
                Object value = row.getCellValue(cellIndex);
                Cell cell = row.getCell(cellIndex);
                if (value != null) {
                	Cell newCell = new Cell(this.average != 0.0 ? this.average : value.toString(), null);
                    if (newCell != null) {
                    	CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    	cellChanges.add(cellChange);
                    }
                }

                return false;
            }
        }.init(column.getCellIndex(), cellChanges, engineMode);
    }
    
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private Float calAverage(int c, Project project, int cellIndex) {
 		float sum = 0;
        for (int r = 0; r < c; r++) {
        	Row _row = project.rows.get(r);
        	Object value = _row.getCellValue(cellIndex);
            if (isNumeric((String) value)) {
                sum += Double.valueOf((String) value); 
            }
        }
        float average = sum/c;
	    return average;
    }
    
}
