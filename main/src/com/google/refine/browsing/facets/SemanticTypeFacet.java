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
import com.google.refine.operations.cell.SemanticTypeOperation;
import com.google.refine.util.PatternSyntaxExceptionParser;

public class SemanticTypeFacet implements Facet {

    /*
     * Configuration
     */
    public static class SemanticTypeFacetConfig implements FacetConfig {
        @JsonProperty("name")
        protected String _name;
        @JsonProperty("columnName")
        protected String _columnName;
        @JsonProperty("lang")
        protected String _lang;

        @Override
        public SemanticTypeFacet apply(Project project) {
            SemanticTypeFacet facet = new SemanticTypeFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }

        @Override
        public String getJsonType() {
            return "text";
        }
    }

    SemanticTypeFacetConfig _config = new SemanticTypeFacetConfig();

    /*
     * Derived configuration
     */
    protected int _cellIndex;
    public List<String> choices = new ArrayList<String>();

    public SemanticTypeFacet() {
    }

    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }

    @JsonProperty("lang")
    public String getLang() {
        return _config._lang;
    }

    public void initializeFromConfig(SemanticTypeFacetConfig config, Project project) {
        _config = config;
        Column column = project.columnModel.getColumnByName(_config._columnName);
        _cellIndex = column != null ? column.getCellIndex() : -1;
        choices.clear();
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
                if (choices.size() == 0) {
                    long time1 = System.currentTimeMillis();
                    int c = project.recordModel.getRecordCount();
                    String lang = _config._lang;
                    if (lang.equals("en")) {
                        lang = "EN";
                    } else {
                        lang = "CN";
                    }
                    String data = SemanticTypeOperation.fectch_data(c, project, this._cellIndex, lang);
                    long time2 = System.currentTimeMillis();
                    System.out.println("fectch_data() spend " + (time2 - time1) / 1000 + "seconds");
                    String predict_type = SemanticTypeOperation.predict_semantic_type(data, true);
                    long time3 = System.currentTimeMillis();
                    System.out.println("predict_semantic_type() spend " + (time3 - time2) / 1000 + " seconds");
                    String[] predict_type_arr = predict_type.split(",");
                    for (int i = 0; i < predict_type_arr.length; i++) {
                        choices.add(predict_type_arr[i]);
                    }
                }

                return false;
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
        return null;
    }

}
