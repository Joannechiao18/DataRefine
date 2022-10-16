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

package com.google.refine.browsing.facets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionStringComparisonRowFilter;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.VariableExpr;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.cell.MedianOperation;
import com.google.refine.operations.cell.SemanticTypeOperation;
import com.google.refine.util.PatternSyntaxExceptionParser;

public class SummaryFacet implements Facet {
    
    /*
     *  Configuration
     */
    public static class SummaryFacetConfig implements FacetConfig {  
        @JsonProperty("name")
        protected String     _name;
        @JsonProperty("columnName")
        protected String     _columnName;
        
        @Override
        public SummaryFacet apply(Project project) {
            SummaryFacet facet = new SummaryFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }
        
        @Override
        public String getJsonType() {
            return "text";
        }
    }
    SummaryFacetConfig _config = new SummaryFacetConfig();
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
//    public List<String> choices = new ArrayList<String>();
    public double _average=Double.NaN;;
    public double _median=Double.NaN;;
    public double _min=Double.NaN;;
    public double _max=Double.NaN;;
    public double _sum=Double.NaN;;
    public double _count=-1;
    
    public SummaryFacet() {
    }
    
    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }
    
    public void initializeFromConfig(SummaryFacetConfig config, Project project) {
        _config = config;
        Column column = project.columnModel.getColumnByName(_config._columnName);
        _cellIndex = column != null ? column.getCellIndex() : -1;
    }


    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
		// nothing to do
    	filteredRows.accept(project, new RowVisitor() {
            List<String> _rowIndices;
            int _cellIndex;
            public RowVisitor init(int _cellIndex) {
            	this._cellIndex = _cellIndex;
                return this;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                if (_count == -1){
                	List<Double> total = new ArrayList<Double>();
                	_sum = 0;
                    _median = 0;
                    _average = 0;
                    _min = 0;
                    _max = 0;
                	_count = project.recordModel.getRecordCount();
                	for (int r = 0; r < _count; r++) {
                    	Row _row = project.rows.get(r);
                    	Object value = _row.getCellValue(this._cellIndex);
                    	if((value  instanceof Long)||(value  instanceof Number)||(value  instanceof Integer)){
                            Double col_val = null;
                            if (value instanceof Long) {
                                col_val = ((Long) value).doubleValue();
                            }	
                            else if(value instanceof Number) {
                            	col_val = ((Number) value).doubleValue();
                            }
                            else if (value instanceof Integer) {
                            	col_val = ((Integer) value).doubleValue();
                            }
                            total.add(col_val);
                            _sum += Double.valueOf(col_val); 
                        }
                    }
                    // 表示不是數值型態的欄位'
                	System.out.print(total.size());
                    if (total.size() == 0) {
                        _sum = Double.NaN;
                        _median = Double.NaN;
                        _average = Double.NaN;
                        _min = Double.NaN;
                        _max = Double.NaN;
                        return false;
                    }
                	_median = MedianOperation.calMedian(total);
                	_average = _sum/_count;
                	_min = findMin(total);
                	_max = findMax(total);
                }
                
                return false;
            }

		
        }.init(_cellIndex));
    }
    
    public static Double findMin(List<Double> list)
    {
        if (list == null || list.size() == 0) {
        	return Double.MAX_VALUE;
        }
        List<Double> sortedlist = new ArrayList<>(list);
        Collections.sort(sortedlist);
        return sortedlist.get(0);
    }
  
    public static Double findMax(List<Double> list)
    {
        if (list == null || list.size() == 0) {
            return Double.MIN_VALUE;
        }
        List<Double> sortedlist = new ArrayList<>(list);
        Collections.sort(sortedlist);
        return sortedlist.get(sortedlist.size() - 1);
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        // nothing to do
    }

	@Override
	public RowFilter getRowFilter(Project project) {
		// TODO Auto-generated method stub
		return null;
	}

}
