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

class OutliersFacet extends Facet {
  constructor(div, config, options, facet_type) {

    super(div, config, options, facet_type);

    this._from = ("from" in this._config) ? this._config.from : null;
    this._to = ("to" in this._config) ? this._config.to : null;
    this._step = ("step" in this._config) ? this._config.step : null;

    this._selectCoef = ("selectCoef" in this._config) ? this._config.selectCoef : null;
    this._selectShowList = false;

    //this._lang = Refine.getPreference('userLang', 'en');
    this._formatter = new Intl.NumberFormat(this._lang, { useGrouping: true, maximumFractionDigits: 2 });

    this._baseNumericCount = 0;
    this._baseNonNumericCount = 0;

    this._defaultCoefCount = 0;
    this._showListCount = 0;

    this._error = false;
    this._initializedUI = false;

    this._data = null;
    this._lower_fence = 0;
    this._upper_fence = 0;
    this._enter = 0;
    this._first = true;
  };

  reset() {
    this._from = this._config.min;
    this._to = this._config.max;
    this._sliderWidget.update(
        this._config.min, 
        this._config.max, 
        this._config.step, 
        this._from,
        this._to
    );

    this._selectShowList = false;

    this._setRangeIndicators();
  };

  getUIState() {
    var json = {
        c: this.getJSON(),
        o: this._options
    };

    return json;
  };

  getJSON() {
    var o = {
        type: "outliers",
        name: this._config.name,
        expression: this._config.expression,
        columnName: this._config.columnName,
        option: this._config.option,
        coefficient: this._config.coefficient,
        compute: this._config.compute,
        slide: this._config.slide
    };

    if (this._from !== null) {
      o.from = this._from;
    }
    if (this._to !== null) {
      o.to = this._to;
    }

    return o;
  };

  hasSelection() {
    if (!this._selectCoef || !this._selectShowList) {
      return true;
    }

    return (this._from !== null && (!this._initializedUI || this._from > this._config.min)) ||
    (this._to !== null && (!this._initializedUI || this._to < this._config.max));
  };

  _initializeUI() {
    var self = this;
    console.log("initial");

    this._div
    .empty()
    .show()
    .html(
      '<div class="facet-title" bind="headerDiv">' +	
      '<div class="grid-layout layout-tightest layout-full"><table><tr>' +	
        '<td width="1%">' +	
          '<a href="javascript:{}" title="'+$.i18n('core-facets/remove-facet')+'" class="facet-title-remove" bind="removeButton">&nbsp;</a>' +	
        '</td>' +	
        '<td width="1%">' +	
          '<a href="javascript:{}" title="'+$.i18n('core-facets/minimize-facet')+'" class="facet-title-minimize" bind="minimizeButton">&nbsp;</a>' +	
        '</td>' +	
        '<td>' +	
          '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n('core-facets/reset')+'</a>' +	
          '<a href="javascript:{}" class="facet-choice-link" bind="changeButton">'+$.i18n('core-facets/change')+'</a>' +	
          '<span bind="facetTitle"></span>' +	
        '</td>' +	
      '</tr></table></div>' +	
    '</div>' +	
    '<div class="facet-expression" bind="expressionDiv" title="'+$.i18n('core-facets/click-to-edit')+'"></div>' +	
      '<div class="facet-range-body">' +	
        '<div class="facet-range-message" bind="messageDiv">'+$.i18n('core-facets/loading')+'</div>' +	
        '<div class="facet-range-slider" bind="sliderWidgetDiv">' +	
          '<div class="facet-range-histogram" bind="histogramDiv"></div>' +	
        '</div>' +	
      '<div class="facet-range-status" bind="statusDiv"></div>' +	
      '<div class="facet-range-other-choices" bind="otherChoicesDiv"></div>' +	
      '<div class="showOutliers" id="showOutliers" bind="showOutliersButton"><button class="btn-show-outliers"><b>'+ $.i18n("core-facets/outliers") +'</b></button></div></div>'+	
      '<div id="outliersTitle" bind="outliersTitle"></div>'+	
      '<div id="outliersLowerBound" bind="outliersLowerBound"></div>'+	
      '<div id="outliersUpperBound" bind="outliersUpperBound"></div>'+	
      '<div class="container" id="delOutliers" bind="delOutliers"></div>'+
      '<table class="outliers" id="outliersTable" bind="outliersTable"></table>'   
    );
    
    this._elmts = DOM.bind(this._div);

    this._elmts.facetTitle.text(this._config.name);
    this._elmts.changeButton.attr("title",$.i18n('core-facets/current-expression')+": " + this._config.expression).click(function() {
      self._elmts.expressionDiv.slideToggle(100, function() {
        if (self._elmts.expressionDiv.css("display") != "none") {
          self._editExpression();
        }
      });
    });
    this._elmts.expressionDiv.text(this._config.expression).click(function() { 
      self._editExpression(); 
    }).hide();

    this._elmts.resetButton.click(function() {
      self.reset();
      self._updateRest();
    });
    
    this._elmts.removeButton.click(function() { self._remove(); });
    this._elmts.minimizeButton.click(function() { self._minimize(); });

    var self = this;

    var lowerbound = null;
    var upperbound = null;
    var outliers_list = null;
    var outliers_index = null;

    if(this._data.compute == 1){
      lowerbound = this._formatter.format(this._lower_fence);
      upperbound = this._formatter.format(this._upper_fence);
      outliers_list = this._data.outliers[0][0];
      outliers_index = this._data.outliers[0][3];
    }
    
    //var selectShowList = this._selectShowList;
    
    this._elmts.showOutliersButton.click(function() { 
      self._selectShowList = true;
      //if (selectShowList){ 
        if(self._selectCoef) {
          self._config.coefficient = self._selectCoef;
        }
        else {
          self._config.coefficient = 1.5;
        }

        self._config.compute = 1;
        var a = {
          //facets: self.getJSON(),
          facets: [],
          mode: "row-based"
        };
        a.facets.push(self.getJSON());

        $.post(
          "command/core/compute-facets?" + $.param({ project: theProject.id }),  
       
          //{ engine: JSON.stringify(self.getJSON(true))},
          { engine: JSON.stringify(a)},
         
          function(data) {
            if(data.code === "error") {
              var clearErr = $('#err-text').remove();
              var err = $('<div id="err-text">')
                        .text(data.message)
                        .appendTo(self._elmts.errors);
              //self._elmts.errors.css("display", "block");
              /*if (onDone) {
                onDone();
              }*/
              return;
            } 
            var facetData = data.facets;
        
            for (var i = 0; i < facetData.length; i++) {
              self.updateState(facetData[i]);
            }
          },
          "json"
        );        
      }
    );

    if(outliers_list){
      console.log("in if");
      self._elmts.outliersTitle.html('<span class="outliers-title">異常值列表</span>'+ 	
      '<span class="outliers-sub-title">* 顯示離群值 (outlier)</span>');	
      self._elmts.outliersLowerBound.html('<span class="outliers-lowerbound"><b>lower bound: </b>'+lowerbound+'</span>');	
      self._elmts.outliersUpperBound.html('<span class="outliers-upperbound"><b>upper bound: </b>'+upperbound+'</span>');	
      self._elmts.delOutliers.html('<button id="btn-del-all-outliers" class="btn_del_all_outliers"><b>'+ $.i18n("core-facets/outliers-DeleteAllOutliers") +'</b></button>');    	
      self._elmts.delOutliers.append('<button id="btn-del-ch-outliers" class="btn_del_ch_outliers"><b>'+ $.i18n("core-facets/outliers-DeleteSelectedOutliers") +'</b></button>');                 
      self._elmts.outliersTable.html('<thead><tr><th></th>'+'<th><b>index</b></th>'+'<th><b>內容</b></th></tr></thead><tbody>');
      
      for (var i = 0; i < outliers_list.length; i++) {	
        self._elmts.outliersTable.append('<tr><td><input type="checkbox" name="'+outliers_index[i]+'"></td>'+'<td >'+outliers_index[i]+'</td><td>'+outliers_list[i]+'</td></tr>');	
      }	
      self._elmts.outliersTable.append('</tbody>');	

      self._elmts.delOutliers.find('.btn_del_all_outliers').click(function(){
        var config = {
          removed: [],
        };
      
        for (var i = 0; i < outliers_list.length; i++){
          config.removed.push(outliers_index[i]);         
        }
      
        for(var j = 0; j < config.removed.length; j++){
          outliers_index.splice(outliers_index.indexOf(config.removed[j]), 1);   
          outliers_list.splice(outliers_index.indexOf(config.removed[j]), 1);
        }
      
        Refine.postCoreProcess(
          "remove-outliers", 
          null,
          config,
          { rowMetadataChanged: true }
          );
      });  
    
      self._elmts.delOutliers.find('.btn_del_ch_outliers').click(function(){
        var config = {
          removed: [],
        };
        $("#outliersTable input[type=checkbox]:checked").each(function () {
          config.removed.push($(this).attr('name'));
          var val = $(this).attr('name');
          var valNum = parseInt(val);
      
          outliers_list.splice(outliers_index.indexOf(valNum), 1);          
          outliers_index.splice(outliers_index.indexOf(valNum), 1);   
        });

        Refine.postCoreProcess(
          "remove-outliers", 
          null,
          config,
          { rowMetadataChanged: true }
          );
        });  
      }

    this._histogram = new HistogramWidget(this._elmts.histogramDiv, { binColors: [ "#bbccff", "#88aaee" ] });
    this._sliderWidget = new SliderWidget(this._elmts.sliderWidgetDiv);

    this._elmts.sliderWidgetDiv.bind("slide", function(evt, data) {
      self._config.slide = true;
      self._from = data.from;
      self._to = data.to;
      self._setRangeIndicators();
    }).bind("stop", function(evt, data) {
      self._from = data.from;
      self._to = data.to;
      self._updateRest();
    }); 
  };

  _renderOtherChoices() {
    var self = this;
    var container = this._elmts.otherChoicesDiv.empty();
    var facet_id = this._div.attr("id");
    var choices = $('<div>').addClass("facet-range-choices");

    // ----------------- select coefficient -----------------
    var coefDiv = $('<div class="facet-range-item"></div>').appendTo(choices);        
    var coefCheck = $('<input class="outlier-coef" type="text" />').attr("id",facet_id + "-coef").appendTo(coefDiv).change(function() {
      self._selectCoef = coefCheck.val();  
      //self._updateRest();  
    });

    if(this._selectCoef) {
      coefCheck.val(this._selectCoef);
    }   
    else
      coefCheck.val(1.5);

    

    var coefLabel = $('<label>').attr("for", facet_id + "-blank").appendTo(coefDiv);    
    $('<span>').text(" 輸入異常值係數 (預設值為1.5)").addClass("facet-range-choice-label").appendTo(coefLabel);
    $('<div>').text(this._coef).addClass("facet-range-choice-count").appendTo(coefLabel);

    choices.appendTo(container);
  };

  _setRangeIndicators() {
    this._elmts.statusDiv.html('<b>'+this._formatter.format(this._from)+'</b>'+' - '+'<b>'+this._formatter.format(this._to)+'</b>');
  };

  updateState(data) {
    console.log(data);
    if(data.compute == 0){

      this._data = data;
      //this._first = false;

      this._error = false;
      this._config.step = this._data.step;
      this._baseBins = this._data.baseBins;
      this._bins = this._data.bins;
      this._config.min = data.min;
      this._config.max = data.max;

      this._from = Math.max(data.from, this._config.min);
      if ("to" in data) {
        this._to = Math.min(data.to, this._config.max);
      } else {
        this._to = data.max;
      }
      this._defaultCoefCount = this._data.defaultCoefCount;
      this._showListCount = this._data.showListCount;

      this._baseNumericCount = this._data.baseNumericCount;
      this._baseNonNumericCount = this._data.baseNonNumericCount;

      this.render();
    }   
    else{
      /*if(this._first == true){
        this._data = data;
        this._first = false;
      }*/
      this._data = data;
  
      this._error = false;
      this._config.step = this._data.step;
      this._baseBins = this._data.baseBins;
      this._bins = this._data.bins;
      this._lower_fence = this._data.outliers[0][1];
      this._upper_fence = this._data.outliers[0][2];
      this._config.min = data.min;
      this._config.max = data.max;
  
      this._from = Math.max(data.from, this._config.min);
      if ("to" in data) {
        this._to = Math.min(data.to, this._config.max);
      } else {
        this._to = data.max;
      }
      this._baseNumericCount = this._data.baseNumericCount;
      this._baseNonNumericCount = this._data.baseNonNumericCount;
  
      this._defaultCoefCount = this._data.defaultCoefCount;
      this._showListCount = this._data.showListCount;
  
      if (this._data.outliers[0] != 0)
        this.render();
      else
        alert("此範圍內的資料無異常值。");
        // this._remove();
    }

    /*if(this._first == true){
      this._data = data;
      this._first = false;
    }

    this._error = false;
    this._config.step = this._data.step;
    this._baseBins = this._data.baseBins;
    this._bins = this._data.bins;
    this._lower_fence = this._data.outliers[0][1];
    this._upper_fence = this._data.outliers[0][2];
    this._config.min = data.min;
    this._config.max = data.max;

    this._from = Math.max(data.from, this._config.min);
    if ("to" in data) {
      this._to = Math.min(data.to, this._config.max);
    } else {
      this._to = data.max;
    }
    this._baseNumericCount = this._data.baseNumericCount;
    this._baseNonNumericCount = this._data.baseNonNumericCount;

    this._defaultCoefCount = this._data.defaultCoefCount;
    this._showListCount = this._data.showListCount;

    if (this._data.outliers[0] != 0)
      this.render();
    else
      alert("資料無異常值!");*/
  };

  render() {
    if(this._enter != 0){
      this._initializedUI = false;
    }

    if (!this._initializedUI) {
      this._initializeUI();
      this._initializedUI = true;
      this._enter += 1;
    }

    if (this._error) {
      this._elmts.messageDiv.text(this._errorMessage).show();
      this._elmts.sliderWidgetDiv.hide();
      this._elmts.histogramDiv.hide();
      this._elmts.statusDiv.hide();
      this._elmts.otherChoicesDiv.hide();
      return;
    }

    this._elmts.messageDiv.hide();
      this._elmts.sliderWidgetDiv.show();
      this._elmts.histogramDiv.show();
      this._elmts.statusDiv.show();
      this._elmts.otherChoicesDiv.show();

      this._sliderWidget.update(
          this._config.min, 
          this._config.max, 
          this._config.step, 
          this._from,
          this._to
      );
      
      this._histogram.update(
          this._config.min, 
          this._config.max, 
          this._config.step, 
          [ this._baseBins, this._bins ]
      );

      this._setRangeIndicators();
      this._renderOtherChoices();
  };

  _updateRest() {
    console.log("in updateRest");
    Refine.update({ engineChanged: true });
  };

  _editExpression() {
    var self = this;
    var title = (this._config.columnName) ? 
        ($.i18n('core-facets/edit-based-col')+" " + this._config.columnName) : 
          $.i18n('core-facets/edit-facet-exp');

        var column = Refine.columnNameToColumn(this._config.columnName);
        var o = DataTableView.sampleVisibleRows(column);

        new ExpressionPreviewDialog(
            title,
            column ? column.cellIndex : -1, 
                o.rowIndices,
                o.values,
                this._config.expression, 
                function(expr) {
              if (expr != self._config.expression) {
                self._config.expression = expr;
                self._elmts.expressionDiv.text(self._config.expression);

                self.reset();
                self._from = null;
                self._to = null;
                self._updateRest();
              }
            }
        );
  };
};

OutliersFacet.reconstruct = function(div, uiState) {
  return new OutliersFacet(div, uiState.c, uiState.o);
};
