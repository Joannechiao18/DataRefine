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
//package com.google.refine.operations.row;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.browsing.filters.AnyRowRecordFilter;

import com.google.refine.browsing.filters.ExpressionStringComparisonRowFilter;
import com.google.refine.browsing.filters.ExpressionNumberComparisonRowFilter;
import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.ExpressionNumericValueBinner;
import com.google.refine.browsing.util.NumericBinIndex;
import com.google.refine.browsing.util.NumericBinRecordIndex;
import com.google.refine.browsing.util.NumericBinRowIndex;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ast.VariableExpr;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.cell.SemanticTypeOperation;
import com.google.refine.util.PatternSyntaxExceptionParser;

public class OutliersFacet implements Facet {

    public static final String ERR_NO_NUMERIC_VALUE_PRESENT = "No numeric value present.";
    
    /*
     *  Configuration
     */
    public static class OutliersFacetConfig implements FacetConfig {  
        @JsonProperty("name")
        protected String     _name;
        @JsonProperty("expression")
        protected String     _expression; // expression to compute numeric value(s) per row
        @JsonProperty("columnName")
        protected String     _columnName;

        @JsonProperty(FROM)
        protected double      _from = 0; // the numeric selection
        @JsonProperty(TO)
        protected double      _to = 0;

        @JsonProperty("coefficient")
        protected double     coefficient;
        @JsonProperty("option")
        protected int     option;
        @JsonProperty("compute")
        protected int     compute;
        @JsonProperty("slide")
        protected boolean     slide;
        
     // false if we're certain that all rows will match
        // and there isn't any filtering to do
        @JsonIgnore
        protected boolean isSelected() {
            return _from != 0 || _to != 0;
        }; 
        
        @Override
        public OutliersFacet apply(Project project) {
            OutliersFacet facet = new OutliersFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }
        
        @Override
        public String getJsonType() {
            return "text";
        }
    }
    OutliersFacetConfig _config = new OutliersFacetConfig();
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
    protected Evaluable  _eval;
    protected String     _errorMessage;
    public List<Object>  outliers = new ArrayList<Object>();
    protected static boolean    _enter = true;
    protected boolean    _selectNumeric = true;
    protected static boolean    _selectNonNumeric = false;
    protected static boolean    _selectBlank = false;
    protected boolean    _selectError = true;

    /*
     * Computed data, to return to the client side
     */
    //protected double    _from;
    //protected double    _to;
    protected double    _min;
    protected double    _max;
    protected double    _step;
    protected int[]     _baseBins;
    protected int[]     _bins;
    
    @JsonProperty("baseNumericCount")
    protected int       _baseNumericCount;
    @JsonProperty("baseNonNumericCount")
    protected int       _baseNonNumericCount;
    @JsonProperty("baseBlankCount")
    protected int       _baseBlankCount;
    @JsonProperty("baseErrorCount")
    protected int       _baseErrorCount;
    
    @JsonProperty("numericCount")
    protected int       _numericCount;
    @JsonProperty("nonNumericCount")
    protected int       _nonNumericCount;
    @JsonProperty("blankCount")
    protected int       _blankCount;
    @JsonProperty("errorCount")
    protected int       _errorCount;
    
    public OutliersFacet() {
    }
    protected static final String MIN = "min";
    protected static final String MAX = "max";
    protected static final String TO = "to";
    protected static final String FROM = "from";
    
    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _config._expression;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }

    @JsonProperty("error")
    @JsonInclude(Include.NON_NULL)
    public String getError() {
        if (_errorMessage != null) {
            return _errorMessage;
        } else if (!isFiniteRange()) {
            return ERR_NO_NUMERIC_VALUE_PRESENT;
        }
        return null;
    }

    @JsonProperty(MIN)
    @JsonInclude(Include.NON_NULL)
    public Double getMin() {
        if(getError() == null) {
            return _min;
        }
        return null;
    }

    @JsonProperty(MAX)
    @JsonInclude(Include.NON_NULL)
    public Double getMax() {
        if(getError() == null) {
            return _max;
        }
        return null;
    }

    @JsonIgnore
    public boolean isFiniteRange() {
        return !Double.isInfinite(_min) && !Double.isInfinite(_max);
    }
    
    @JsonProperty("coefficient")
    public double getCoefficient() {
        return _config.coefficient;
    }
    
    @JsonProperty("option")
    public int getOption() {
        return _config.option;
    }
    
    @JsonProperty("compute")
    public int getCompute() {
        return _config.compute;
    }
    
    @JsonProperty("slide")
    public boolean getSlide() {
        return _config.slide;
    }

    @JsonProperty("step")
    @JsonInclude(Include.NON_NULL)
    public Double getStep() {
        if (getError() == null) {
            return _step;
        }
        return null;
    }
    
    @JsonProperty("bins")
    @JsonInclude(Include.NON_NULL)
    public int[] getBins() {
        if (getError() == null) {
            return _bins;
        }
        return null;
    }
    
    @JsonProperty("baseBins")
    @JsonInclude(Include.NON_NULL)
    public int[] getBaseBins() {
        if (getError() == null) {
            return _baseBins;
        }
        return null;
    }
    
    @JsonProperty(FROM)
    @JsonInclude(Include.NON_NULL)
    public Double getFrom() {
        if (getError() == null) {
            return _config._from;
        }
        return null;
    }
    
    @JsonProperty(TO)
    @JsonInclude(Include.NON_NULL)
    public Double getTo() {
        if (getError() == null) {
            return _config._to;
        }
        return null;
    } 
    
    public void initializeFromConfig(OutliersFacetConfig config, Project project) {
        _config = config;

        if (_config._columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_config._columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _config._columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_config._expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }

        outliers.clear();
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
                if (outliers.size() == 0){
                	List<Object> input = new ArrayList<Object>();
                	int c = project.recordModel.getRecordCount();
                	for (int r = 0; r < c; r++) {
                    	Row _row = project.rows.get(r);
                    	Object value = _row.getCellValue(this._cellIndex);
                    	input.add(value);
                    }
                	
                    if (!input.isEmpty()){
                    	//long time1 = System.currentTimeMillis();
                    	
                    	List<Object>  sep_input = new ArrayList<Object>();
                    	sep_input = (List<Object>) checkType(input);
                    	//long time2 = System.currentTimeMillis();
                    	//System.out.println("input spend " + (time2 - time1) + "milli seconds");
                    	
                    	if (_eval != null && _errorMessage == null) {
                            RowEvaluable rowEvaluable = getRowEvaluable(project);
                            
                            Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
                            String key = "numeric-bin:row-based:" + _config._expression;
                            NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);

                            if (index == null) {
                                index = new NumericBinRowIndex(project, rowEvaluable);
                                column.setPrecompute(key, index);
                            }
                            
                            retrieveDataFromBaseBinIndex(index);
                            
                            ExpressionNumericValueBinner binner = 
                                new ExpressionNumericValueBinner(rowEvaluable, index);
                            
                            filteredRows.accept(project, binner);
                            retrieveDataFromBinner(binner);
                        }
                    	if (((List<Double>) sep_input.get(0)).size() == 0) {
                    		outliers.add("資料異常");
                            return false;
                        } 
                    	/*else {
                            outliers.add(getOutliers(sep_input, _config.coefficient, _config.option));
                        }*/
                        if(_config.compute == 1) {
                    		outliers.add(getOutliers(sep_input, _config.coefficient, _config.option));
                    	}
                        
                    } else {
                    	outliers.add("資料異常");
                    }
                }
                
                return true;
                //return false;
            }

		
        }.init(_cellIndex));
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        // nothing to do
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        // TODO Auto-generated method stub
        // return null;
    	boolean _selected = _config.isSelected()||_selectNumeric ||_selectNonNumeric||_selectBlank||_selectError;
    	
        if (_eval != null && _errorMessage == null && _config.isSelected() && _config.slide) {
            return new ExpressionNumberComparisonRowFilter(
                    getRowEvaluable(project), _selectNumeric, !_selectNonNumeric, !_selectBlank, _selectError) {

                @Override
                protected boolean checkValue(double d) {
                    return d >= _config._from && d <= _config._to;
                };
            };
        } else {
            return null;
        }
    }

	public static Object checkType (List<Object> input) {
		List<Object> tmp = new ArrayList<Object>();
		
		tmp.addAll(input);
		
		List<Object> nonNumeric_list = new ArrayList<Object>();
		List<Integer> nonNumeric_index = new ArrayList<Integer>();
		List<Integer> data_index = new ArrayList<Integer>();
		
		for (int i=0;i<input.size();i++) {
			data_index.add(i+1);
		}	
			
		for(int i = 0; i < input.size(); i++){
			if((input.get(i) instanceof Long)||(input.get(i) instanceof Number)||(input.get(i) instanceof Integer)){
				Double d = null;
				if ((input.get(i) instanceof Long))
					d = ((Long) input.get(i)).doubleValue();
            	else if((input.get(i) instanceof Number))
            		d = ((Number) input.get(i)).doubleValue();
            	else if ((input.get(i) instanceof Integer))
            		d = ((Integer) input.get(i)).doubleValue();
                tmp.set(tmp.indexOf(input.get(i)), d);
			}
			else {
            	nonNumeric_list.add(input.get(i));

            	nonNumeric_index.add(data_index.get(tmp.indexOf(input.get(i))));
            	data_index.remove(tmp.indexOf(input.get(i)));
            	tmp.remove(tmp.indexOf(input.get(i)));
			}
		}
		
		if(nonNumeric_list != null) {
			for(int i=0;i<nonNumeric_list.size();i++) {
				if(nonNumeric_list.get(i) == null)
					_selectBlank = true;
				if(nonNumeric_list.get(i) instanceof String)
					_selectNonNumeric = true;
			}
			//_selectBlank = true;
			//_selectNonNumeric = true;
		}
		
		
		 
        if (!input.isEmpty()) {
        	return Arrays.asList(tmp, data_index, nonNumeric_list, nonNumeric_index);
        }
        else {
        	return 0;  //input empty
        }
    }

    public static Object getOutliers(List<Object> sep_input, Double c, Integer option) {  	
    	List<Double> data = new ArrayList<Double>();
    	List<Integer> data_index = new ArrayList<Integer>();
    	List<Double> tmp = new ArrayList<Double>();
    	
    	data = (List<Double>) sep_input.get(0);
    	data_index = (List<Integer>) sep_input.get(1);	
    	tmp.addAll(data);
        
        /*for(Object o: (List<Object>) sep_input.get(0)) {
        	data.add(((Number) o).doubleValue());
        }*/
       
        Collections.sort(data);

        List<Double> output = new ArrayList<Double>();
        List<Integer> outliers_index = new ArrayList<Integer>();
        List<Double> left_sublist = new ArrayList<Double>();
        List<Double> right_sublist = new ArrayList<Double>();
        
    	output = (List<Double>) sep_input.get(2);  
    	outliers_index = (List<Integer>) sep_input.get(3);
        

        /*根據data長度(奇偶)判斷sublist長度*/
        if (data.size() % 2 == 0) {
            left_sublist = data.subList(0, data.size() / 2);
            right_sublist = data.subList(data.size() / 2, data.size());
        } else {
            left_sublist = data.subList(0, data.size() / 2);
            right_sublist = data.subList(data.size() / 2 + 1, data.size());
        }      

        /*計算Q1, Q3*/
        double q1 = getMedian(left_sublist);
        double q3 = getMedian(right_sublist);

        /*計算outlier lists, U/L_fence*/
        double iqr = q3 - q1;
        double lower_fence = q1 - c * iqr;
        double upper_fence = q3 + c * iqr;
        
        for (int i = 0;i <data.size(); i++) {
            if (data.get(i) < lower_fence || data.get(i) > upper_fence) {
                output.add(data.get(i));

                outliers_index.add(data_index.get(tmp.indexOf(data.get(i))));
                data_index.remove(tmp.indexOf(data.get(i)));
                tmp.remove(tmp.indexOf(data.get(i)));
            }
        }
        
        System.out.println(output.size());

        if (output.size() > 0) {
        	/*if (output.size() > data.size()*0.5)  //如果異常值數量超過原資料的一半
                return 0;*/
              if (option == 1)
                  return Arrays.asList(output, lower_fence, upper_fence, outliers_index);
              else
                  return Arrays.asList(lower_fence, upper_fence);
        }
        else
        	return 0;        
    }

    private static double getMedian(List<Double> sublist) {
        /*根據sublist長度(奇偶)判斷中位數位置*/
        if (sublist.size() % 2 == 0)
            return (sublist.get(sublist.size() / 2) + sublist.get(sublist.size() / 2 - 1)) / 2;
        else
            return sublist.get(sublist.size() / 2);
    }

    protected RowEvaluable getRowEvaluable(Project project) {
        return new ExpressionBasedRowEvaluable(_config._columnName, _cellIndex, _eval);
    }
    
    protected void retrieveDataFromBaseBinIndex(NumericBinIndex index) {

        _min = index.getMin();
        _max = index.getMax();
        _step = index.getStep();
        _baseBins = index.getBins();
        
        _baseNumericCount = index.getNumericRowCount();
        _baseNonNumericCount = index.getNonNumericRowCount();
        _baseBlankCount = index.getBlankRowCount();
        _baseErrorCount = index.getErrorRowCount();
        
        if (_config.isSelected()) {
            _config._from = Math.max(_config._from, _min);        
            _config._to = Math.min(_config._to, _max);
            
        } else {
            _config._from = _min;
            _config._to = _max;
        }
    }
    
    protected void retrieveDataFromBinner(ExpressionNumericValueBinner binner) {
        _bins = binner.bins;
        _numericCount = binner.numericCount;
        _nonNumericCount = binner.nonNumericCount;
        _blankCount = binner.blankCount;
        _errorCount = binner.errorCount;
    }
}
