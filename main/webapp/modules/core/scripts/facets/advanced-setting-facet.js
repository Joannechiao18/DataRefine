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
class AdvancedSettingFacet extends Facet {
  constructor(div, config, options) {
    super(div, config, options);
    if (!("invert" in this._config)) {
      this._config.invert = false;
    }

    this._query = config.query || null;
    this._timerID = null;


    this._initializeUI();
    this._update();
  };

  static textSearchFacetCounterForLabels = 0;

  reset() {
    this._query = null;
    this._div.find(".input-container input").each(function() { this.value = ""; });
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
        type: "advanced-setting",
        name: this._config.name,
        columnName: this._config.columnName,
        mode: this._config.mode,
        caseSensitive: this._config.caseSensitive,
        invert: this._config.invert,
        query: this._query
    };
    return o;
  };

  hasSelection() {
    return this._query !== null;
  };

  _initializeUI() {
    var self = this;
    console.log("_initializeUI");

    this._div.empty().show().html(
      '<div class="facet-title" bind="facetTitle">' + 
        '<div class="grid-layout layout-tightest layout-full"><table><tr>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/remove-facet')+'" class="facet-title-remove" bind="removeButton">&nbsp;</a>' +
          '</td>' +
          '<td width="1%">' +
            '<a href="javascript:{}" title="'+$.i18n('core-facets/minimize-facet')+'" class="facet-title-minimize" bind="minimizeButton">&nbsp;</a>' +
          '</td>' +
          '<td>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="resetButton">'+$.i18n('core-facets/reset')+'</a>' +
            '<a href="javascript:{}" class="facet-choice-link" bind="invertButton">'+$.i18n('core-facets/invert')+'</a>' +
            '<span bind="titleSpan"></span>' +
          '</td>' +
        '</tr></table></div>' +
      '</div>' +
      '<div class="facet-text-body">' +
        /*'<div class="advanced-setting-title"><span class="title-setting">資料格式</span></div>'+
        '<div class="advanced-setting-choice-List" bind="choiceList"></div>' + 
          '<div class="advanced-setting-select">'+
            '<select id="advanced-setting_selectDataType">'+
              '<option id ="text" value="text">text</option>'+
              '<option id = "number" value="number">number</option>'+
              '<option id = "date" value="date">date</option>'+
            '</select>'+
        '</div>' +
        '<div class="advanced-setting-title"><span class="title-setting">資料轉換</span></div>'+
        '<div class="advanced-setting-choice-List" bind="choiceList"></div>' + 
          '<div class="advanced-setting-select">'+
            '<select id="advanced-setting_selectDataTrans">'+
              '<option id ="auto" value="auto">auto</option>'+
              '<option id = "titlecase" value="To titlecase">To titlecase</option>'+
              '<option id = "uppercase" value="To uppercase">To uppercase</option>'+
              '<option id = "lowercase" value="To lowercase">To lowercase</option>'+
            '</select>'+
          '</div>' +
        '<div class="advanced-setting-title"><span class="title-setting">Edit cells</span></div>'+
        //'<div class="advanced-setting-choice-List" bind="choiceList"></div>' + 
        '<div class="container-2" id="move">'+
          '<button id="most-left" class="btn-advanced-most-left" bind="mostLeftButton"><b>最左</b></button>'+
          '<button id="left" class="btn-advanced-left" bind="LeftButton"><b>左</b></button>'+
          '<button id="right" class="btn-advanced-right" bind="RightButton"><b>右</b></button>'+
          '<button id="most-right" class="btn-advanced-most-right" bind="mostRightButton"><b>最右</b></button>'+     
        '</div>'+*/

        /*edit cell*/
        '<div class="advanced-setting-title"><span class="title-setting">'+$.i18n("core-facets/advanced-setting-editCells")+'</span></div>'+
        '<div class="advanced-setting-choice-List" bind="choiceList"></div>' + 
        '<div class="advanced-setting-select">'+
            '<select id="advanced-setting_selectEditCell">'+
              '<option id = "lowercase" value="Common transform">'+$.i18n("core-facets/advanced-setting-otherTransforms")+'</option>'+
              '<option id = "titlecase" value="Unescape HTML entities">'+$.i18n("core-facets/advanced-setting-unescape")+'</option>'+
              '<option id = "uppercase" value="Replace smart quotes with ASCII">'+$.i18n("core-facets/advanced-setting-replace")+'</option>'+             
            '</select>'+
        '</div>' +
        '<button id="split-cell" class="btn-advanced-split-cell" bind="splitMultiCell">'+$.i18n("core-facets/advanced-setting-splitCells")+'</button>'+
        '<button id="merge-cell" class="btn-advanced-merge-cell" bind="mergeMultiCell">'+$.i18n("core-facets/advanced-setting-mergeCells")+'</button>'+

        /*edit column*/
        '<div class="advanced-setting-title"><span class="title-setting">'+$.i18n("core-facets/advanced-setting-editCol")+'</span></div>'+
        '<div class="advanced-setting-choice-List" bind="choiceList"></div>' +
        '<div class="advanced-setting-select">'+
            '<select id="advanced-setting_selectEditCol">'+
              '<option id = "lowercase" value="Add Column">'+$.i18n("core-facets/advanced-setting-addColumn")+'</option>'+
              '<option id = "titlecase" value="Add column based on this column">'+$.i18n("core-facets/advanced-setting-addColumnThis")+'</option>'+
              '<option id = "uppercase" value="Add column by fetching URLs">'+$.i18n("core-facets/advanced-setting-addColumnURL")+'</option>'+   
              '<option id = "uppercase" value="Add column from reconciled values">'+$.i18n("core-facets/advanced-setting-addColumnRecon")+'</option>'+          
            '</select>'+
        '</div>' +
        '<button id="split-col" class="btn-advanced-split-col" bind="splitCol">'+$.i18n("core-facets/advanced-setting-splitCol")+'</button>'+
        '<button id="merge-col" class="btn-advanced-merge-col" bind="mergeCol">'+$.i18n("core-facets/advanced-setting-joinCol")+'</button>'+

        '<div class="advanced-setting-title"><span class="title-setting">'+$.i18n("core-facets/advanced-setting-transpose")+'</span></div>'+
        '<div class="advanced-setting-choice-List" bind="choiceList"></div>' + 
        '<button id="col-to-row" class="btn-advanced-col-to-row" bind="colToRowButton">'+$.i18n("core-facets/advanced-setting-colToRow")+'</button>'+
        '<button id="row-to-col" class="btn-advanced-row-to-col" bind="rowToColButton">'+$.i18n("core-facets/advanced-setting-rowToCol")+'</button>'+
      '</div>'
    );

    this._elmts = DOM.bind(this._div);
    this._elmts.titleSpan.text(this._config.name);

    this._elmts.removeButton.click(function() { self._remove(); });
    this._elmts.minimizeButton.click(function() { self._minimize(); });
    this._elmts.resetButton.click(function() { self._reset(); });
    this._elmts.invertButton.click(function() { self._invert(); });
    
    var columnName = this._config.name;
    var columnIndex = Refine.columnNameToColumnIndex(columnName);

    var column = {
      cellIndex: columnIndex,
      constraints: "{}",
      description: "",
      format: "default",
      name: columnName,
      originalName: columnName,
      title: "",
      type: ""
    }

    var doAddColumn = function() {
      
      var frame = $(
          DOM.loadHTML("core", "scripts/views/data-table/add-column-dialog.html")
          .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml()));
  
      var elmts = DOM.bind(frame);
      elmts.dialogHeader.text($.i18n('core-views/add-col-col')+" " + column.name);
      
      elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
      elmts.or_views_onErr.text($.i18n('core-views/on-error'));
      elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
      elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
      elmts.or_views_copyVal.text($.i18n('core-views/copy-val'));
      elmts.okButton.html($.i18n('core-buttons/ok'));
      elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  
      var level = DialogSystem.showDialog(frame);
      var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
  
      var o = DataTableView.sampleVisibleRows(column);
      var previewWidget = new ExpressionPreviewDialog.Widget(
        elmts, 
        column.cellIndex,
        o.rowIndices,
        o.values,
        null
      );
      
      elmts.cancelButton.click(dismiss);
      elmts.okButton.click(function() {
        var columnName = $.trim(elmts.columnNameInput[0].value);
        if (!columnName.length) {
          alert($.i18n('core-views/warning-col-name'));
          return;
        }
  
        Refine.postCoreProcess(
          "add-column", 
          {
            baseColumnName: column.name,  
            newColumnName: columnName, 
            columnInsertIndex: columnIndex + 1,
            onError: $('input[name="create-column-dialog-onerror-choice"]:checked')[0].value
          },
          { expression: previewWidget.getExpression(true) },
          { modelsChanged: true },
          {
            onDone: function(o) {
              dismiss();
            }
          }
        );
      });
    };

    var doAddColumnByFetchingURLs = function() {
      var frame = $(
          DOM.loadHTML("core", "scripts/views/data-table/add-column-by-fetching-urls-dialog.html")
          .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml())
          .replace("$HTTP_HEADERS_WIDGET$", HttpHeadersDialog.generateWidgetHtml())
          );
  
      var elmts = DOM.bind(frame);
      elmts.dialogHeader.text($.i18n('core-views/add-col-fetch')+" " + column.name);
      
      elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
      elmts.or_views_throttle.text($.i18n('core-views/throttle-delay'));
      elmts.or_views_milli.text($.i18n('core-views/milli'));
      elmts.or_views_onErr.text($.i18n('core-views/on-error'));
      elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
      elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
      elmts.or_views_cacheResponses.text($.i18n('core-views/cache-responses'));
      elmts.or_views_httpHeaders.text($.i18n('core-views/http-headers'));
      elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/show'));
      elmts.or_views_httpHeadersShowHide.click(function() {
                                                            $( ".set-httpheaders-container" ).toggle( "slow", function() {
                                                              if ($(this).is(':visible')) {
                                                                elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/hide'));
                                                              } else {
                                                                elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/show'));
                                                              }
                                                            });
                                                          });
      elmts.or_views_urlFetch.text($.i18n('core-views/url-fetch'));
      elmts.okButton.html($.i18n('core-buttons/ok'));
      elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  
      var level = DialogSystem.showDialog(frame);
      var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
  
      var o = DataTableView.sampleVisibleRows(column);
      var previewWidget = new ExpressionPreviewDialog.Widget(
        elmts, 
        column.cellIndex,
        o.rowIndices,
        o.values,
        null
      );
  
      elmts.cancelButton.click(dismiss);
      elmts.okButton.click(function() {
        var columnName = $.trim(elmts.columnNameInput[0].value);
        if (!columnName.length) {
          alert($.i18n('core-views/warning-col-name'));
          return;
        }
        
        Refine.postCoreProcess(
          "add-column-by-fetching-urls", 
          {
            baseColumnName: column.name, 
            urlExpression: previewWidget.getExpression(true), 
            newColumnName: columnName, 
            columnInsertIndex: columnIndex + 1,
            delay: elmts.throttleDelayInput[0].value,
            onError: $('input[name="dialog-onerror-choice"]:checked')[0].value,
            cacheResponses: $('input[name="dialog-cache-responses"]')[0].checked,
            httpHeaders: JSON.stringify(elmts.setHttpHeadersContainer.find("input").serializeArray())
          },
          null,
          { modelsChanged: true }
        );
        dismiss();
      });
    };

    var doAddColumnByReconciliation = function() {
      var columnIndex = Refine.columnNameToColumnIndex(column.name);
      var o = DataTableView.sampleVisibleRows(column);
      new ExtendReconciledDataPreviewDialog(
        column, 
        columnIndex, 
        o.rowIndices,
        function(extension, endpoint, identifierSpace, schemaSpace) {
          Refine.postProcess(
              "core",
              "extend-data", 
              {
                baseColumnName: column.name,
          endpoint: endpoint,
                identifierSpace: identifierSpace,
                schemaSpace: schemaSpace,
                columnInsertIndex: columnIndex + 1
              },
              {
                extension: JSON.stringify(extension)
              },
              { rowsChanged: true, modelsChanged: true }
          );
        }
      );
    };

    $('#advanced-setting_selectEditCol').on('change', function() {
      var value = $(this).val();
      if(value == "Add column based on this column"){
        doAddColumn();
      }
      else if(value == "Add column by fetching URLs"){
        doAddColumnByFetchingURLs();
      }
      else if(value == "Add column from reconciled values"){
        doAddColumnByReconciliation();
      }
    });

    $('#advanced-setting_selectEditCell').on('change', function() {
      var value = $(this).val();
      if(value == "Unescape HTML entities"){
        Refine.postCoreProcess(
          "text-transform",
          {
            columnName: columnName,
            onError: "keep-original",
            repeat: true,
            repeatCount: 10
          },
          { expression: "value.unescape('html')" },
          { cellsChanged: true },
        );
      }
      else if(value == "Replace smart quotes with ASCII"){
        Refine.postCoreProcess(
          "text-transform",
          {
            columnName: columnName,
            onError: "keep-original",
            repeat: false,
            repeatCount: ""
          },
          { expression: "value.replace(/[\u2018\u2019\u201A\u201B\u2039\u203A\u201A]/,\"\\\'\").replace(/[\u201C\u201D\u00AB\u00BB\u201E]/,\"\\\"\")" },
          { cellsChanged: true },
        );
      }
    });

    /*this._elmts.mostLeftButton.click(function() { 
      Refine.postCoreProcess(
        "move-column", 
        {
          columnName: columnName,
          index: 0
        },
        null,
        { modelsChanged: true }
      );

    });

    this._elmts.LeftButton.click(function() { 
      var newidx = Refine.columnNameToColumnIndex(columnName) + (-1);
      if (newidx >= 0) {
        Refine.postCoreProcess(
            "move-column", 
            {
              columnName: columnName,
              index: newidx
            },
            null,
            { modelsChanged: true }
        );
      }
    });

    this._elmts.RightButton.click(function() { 
      var newidx = Refine.columnNameToColumnIndex(columnName) + (1);
      if (newidx >= 0) {
        Refine.postCoreProcess(
            "move-column", 
            {
              columnName: columnName,
              index: newidx
            },
            null,
            { modelsChanged: true }
        );
      }
    });

    this._elmts.mostRightButton.click(function() { 
      Refine.postCoreProcess(
        "move-column", 
        {
          columnName: columnName,
          index: theProject.columnModel.columns.length - 1
        },
        null,
        { modelsChanged: true }
      );
    });*/

    var doTransposeColumnsIntoRows = function() {
      var dialog = $(DOM.loadHTML("core", "scripts/views/data-table/transpose-columns-into-rows.html"));
  
      var elmts = DOM.bind(dialog);
      var level = DialogSystem.showDialog(dialog);
  
      elmts.dialogHeader.html($.i18n('core-views/transp-cell'));
      elmts.or_views_fromCol.html($.i18n('core-views/from-col'));
      elmts.or_views_toCol.html($.i18n('core-views/to-col'));
      elmts.or_views_transpose.html($.i18n('core-views/transp-into'));
      elmts.or_views_twoCol.html($.i18n('core-views/two-new-col'));
      elmts.or_views_keyCol.html($.i18n('core-views/key-col'));
      elmts.or_views_containNames.html($.i18n('core-views/contain-names'));
      elmts.or_views_valCol.html($.i18n('core-views/val-col'));
      elmts.or_views_containOrig.html($.i18n('core-views/contain-val'));
      elmts.or_views_oneCol.html($.i18n('core-views/one-col'));
      elmts.or_views_prependName.html($.i18n('core-views/prepend-name'));
      elmts.or_views_followBy.html($.i18n('core-views/follow-by'));
      elmts.or_views_beforeVal.html($.i18n('core-views/before-val'));
      elmts.or_views_ignoreBlank.html($.i18n('core-views/ignore-blank'));
      elmts.or_views_fillOther.html($.i18n('core-views/fill-other'));
      elmts.okButton.html($.i18n('core-buttons/transpose'));
      elmts.cancelButton.html($.i18n('core-buttons/cancel'));
  
      var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
      };
  
      var columns = theProject.columnModel.columns;
  
      elmts.cancelButton.click(function() { dismiss(); });
      elmts.okButton.click(function() {
        var config = {
          startColumnName: elmts.fromColumnSelect[0].value,
          columnCount: elmts.toColumnSelect[0].value,
          ignoreBlankCells: elmts.ignoreBlankCellsCheckbox[0].checked,
          fillDown: elmts.fillDownCheckbox[0].checked
        };
  
        var mode = dialog.find('input[name="transpose-dialog-column-choices"]:checked')[0].value;
        if (mode == "2") {
          config.keyColumnName = $.trim(elmts.keyColumnNameInput[0].value);
          config.valueColumnName = $.trim(elmts.valueColumnNameInput[0].value);
          if (config.keyColumnName == "") {
            alert($.i18n('core-views/spec-new-name'));
            return;
          } else if (config.valueColumnName == "") {
            alert($.i18n('core-views/spec-new-val'));
            return;
          }
        } else {
          config.combinedColumnName = $.trim(elmts.combinedColumnNameInput[0].value);
          config.prependColumnName = elmts.prependColumnNameCheckbox[0].checked;
          config.separator = elmts.separatorInput[0].value;
          if (config.combinedColumnName == "") {
            alert($.i18n('core-views/spec-col-name'));
            return;
          } else if (config.prependColumnName && config.separator == "") {
            alert($.i18n('core-views/spec-separator'));
            return;
          }
        }
  
        Refine.postCoreProcess(
            "transpose-columns-into-rows",
            config,
            null,
            { modelsChanged: true },
            {
              onDone: dismiss
            }
        );
      });
  
      for (var i = 0; i < columns.length; i++) {
        var column2 = columns[i];
        var option = $('<option>').val(column2.name).text(column2.name).appendTo(elmts.fromColumnSelect);
        //if (column2.name == column.name) {
        if (column2.name == columnName) {
          option.prop("selected", "true");
        }
      }
  
      var populateToColumn = function() {
        elmts.toColumnSelect.empty();
  
        var toColumnName = elmts.fromColumnSelect[0].value;
  
        var j = 0;
        for (; j < columns.length; j++) {
          var column2 = columns[j];
          if (column2.name == toColumnName) {
            break;
          }
        }
  
        for (var k = j + 1; k < columns.length; k++) {
          var column2 = columns[k];
          $('<option>').val(k - j + 1).text(column2.name).appendTo(elmts.toColumnSelect);
        }
  
        $('<option>')
          .val("-1")
          .prop("selected", "true")
          .text("(last column)")
          .appendTo(elmts.toColumnSelect);
      };
      populateToColumn();
  
      elmts.fromColumnSelect.bind("change", populateToColumn);
    };

    var doTransposeRowsIntoColumns = function() {
      var rowCount = window.prompt($.i18n('core-views/how-many-rows'), "2");
      if (rowCount !== null) {
        try {
          rowCount = parseInt(rowCount,10);
        } catch (e) {
          // ignore
        }
  
        if (isNaN(rowCount) || rowCount < 2) {
          alert($.i18n('core-views/expect-two'));
        } else {
          var config = {
            columnName: columnName,
            rowCount: rowCount
          };
  
          Refine.postCoreProcess(
            "transpose-rows-into-columns",
            config,
            null,
            { modelsChanged: true }
          );
        }
      }
    };

    this._elmts.colToRowButton.click(function() { 
      doTransposeColumnsIntoRows();
    });

    this._elmts.rowToColButton.click(function() { 
      doTransposeRowsIntoColumns();
    });

    var columnIndex = Refine.columnNameToColumnIndex(columnName);
    var selectedColumn = null;

    var doSplitColumn = function() {
      var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-column-dialog.html"));
      var elmts = DOM.bind(frame);
      elmts.dialogHeader.text($.i18n('core-views/split-col', columnName));
      
      elmts.or_views_howSplit.text($.i18n('core-views/how-split'));
      elmts.or_views_bySep.text($.i18n('core-views/by-sep'));
      elmts.or_views_separator.text($.i18n('core-views/separator'));
      elmts.or_views_regExp.text($.i18n('core-views/reg-exp'));
      elmts.or_views_splitInto.text($.i18n('core-views/split-into'));
      elmts.or_views_colMost.text($.i18n('core-views/col-at-most'));
      elmts.or_views_fieldLen.text($.i18n('core-views/field-len'));
      elmts.or_views_listInt.text($.i18n('core-views/list-int'));
      elmts.or_views_afterSplit.text($.i18n('core-views/after-split'));
      elmts.or_views_guessType.text($.i18n('core-views/guess-cell'));
      elmts.or_views_removeCol.text($.i18n('core-views/remove-col'));
      elmts.okButton.html($.i18n('core-buttons/ok'));
      elmts.cancelButton.text($.i18n('core-buttons/cancel'));
  
      var level = DialogSystem.showDialog(frame);
      var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
      
      elmts.separatorInput.focus().select();
  
      elmts.cancelButton.click(dismiss);
      elmts.okButton.click(function() {
        var mode = $("input[name='split-by-mode']:checked")[0].value;
        var config = {
          columnName: columnName,
          mode: mode,
          guessCellType: elmts.guessCellTypeInput[0].checked,
          removeOriginalColumn: elmts.removeColumnInput[0].checked
        };
        if (mode == "separator") {
          config.separator = elmts.separatorInput[0].value;
          if (!(config.separator)) {
            alert($.i18n('core-views/specify-sep'));
            return;
          }
  
          config.regex = elmts.regexInput[0].checked;
  
          var s = elmts.maxColumnsInput[0].value;
          if (s) {
            var n = parseInt(s,10);
            if (!isNaN(n)) {
              config.maxColumns = n;
            }
          }
        } else {
          var s = "[" + elmts.lengthsTextarea[0].value + "]";
          try {
            var a = JSON.parse(s);
  
            var lengths = [];
            $.each(a, function(i,n) { 
              if (typeof n == "number") {
                lengths.push(n); 
              }
            });
  
            if (lengths.length === 0) {
              alert($.i18n('core-views/warning-no-length'));
              return;
            }
  
            config.fieldLengths = JSON.stringify(lengths);
            
          } catch (e) {
            alert($.i18n('core-views/warning-format'));
            return;
          }
        }
  
        Refine.postCoreProcess(
          "split-column", 
          config,
          null,
          { modelsChanged: true }
        );
        dismiss();
      });
    }; 

    var doJoinColumns = function() {
      var self = this;
      var dialog = $(DOM.loadHTML("core","scripts/views/data-table/column-join.html"));
      var elmts = DOM.bind(dialog);
      var level = DialogSystem.showDialog(dialog);
      // Escape strings
      function escapeString(s,dontEscape) {
        var dontEscape = dontEscape || false;
        var temp = s;
        if (dontEscape) {
          // replace "\n" with newline and "\t" with tab
          temp = temp.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
          // replace "\" with "\\"
          temp = temp.replace(/\\/g, '\\\\');
          // replace "\newline" with "\n" and "\tab" with "\t"
          temp = temp.replace(/\\\n/g, '\\n').replace(/\\\t/g, '\\t');
          // replace ' with \'
          temp = temp.replace(/'/g, "\\'");
        } 
        else {
      // escape \ and '
          temp = s.replace(/\\/g, '\\\\').replace(/'/g, "\\'") ; 
          // useless : .replace(/"/g, '\\"')
        }
        return temp;
      };
      // Close the dialog window
      var dismiss = function() {
        DialogSystem.dismissUntil(level - 1);
      };
      // Join the columns according to user input
      var transform = function() {
        // function called in a callback
        var deleteColumns = function() {
          if (deleteJoinedColumns) {
            var columnsToKeep = theProject.columnModel.columns
            .map (function (col) {return col.name;})
            .filter (function(colName) {
              // keep the selected column if it contains the result
              return (
                  (columnsToJoin.indexOf (colName) == -1) ||
                  ((writeOrCopy !="copy-to-new-column") && (colName == columnName)));
              }); 
            Refine.postCoreProcess(
                "reorder-columns",
                null,
                { "columnNames" : JSON.stringify(columnsToKeep) }, 
                { modelsChanged: true },
                { includeEngine: false }
            );
          }
        };
        // get options
        var onError = "keep-original" ;
        var repeat = false ;
        var repeatCount = "";
        var deleteJoinedColumns = elmts.delete_joined_columnsInput[0].checked;
        var writeOrCopy = $("input[name='write-or-copy']:checked")[0].value;
        var newColumnName = $.trim(elmts.new_column_nameInput[0].value);
        var manageNulls = $("input[name='manage-nulls']:checked")[0].value;
        var nullSubstitute = elmts.null_substituteInput[0].value;
        var fieldSeparator = elmts.field_separatorInput[0].value;
        var dontEscape = elmts.dont_escapeInput[0].checked;
        // fix options if they are not consistent
        if (newColumnName != "") {
          writeOrCopy ="copy-to-new-column";
          } else
          {
            writeOrCopy ="write-selected-column";
          }
        if (nullSubstitute != "") {
            manageNulls ="replace-nulls";
        }   
        // build GREL expression
        var columnsToJoin = [];
        elmts.column_join_columnPicker
          .find('.column-join-column input[type="checkbox"]:checked')
          .each(function() {
              columnsToJoin.push (this.closest ('.column-join-column').getAttribute('column'));
          });
        expression = columnsToJoin.map (function (colName) {
          if (manageNulls == "skip-nulls") {
            return "cells['"+escapeString(colName) +"'].value";
          }
        else {
            return "coalesce(cells['"+escapeString(colName)+"'].value,'"+ escapeString(nullSubstitute,dontEscape) + "')";
          }
        }).join (',');
        expression = 'join ([' + expression + '],\'' + escapeString(fieldSeparator,dontEscape) + "')";
        // apply expression to selected column or new column
        if (writeOrCopy =="copy-to-new-column") {
          Refine.postCoreProcess(
            "add-column", 
            {
            baseColumnName: columnName,  
            newColumnName: newColumnName, 
            columnInsertIndex: columnIndex + 1,
            onError: onError
            },
            { expression: expression },
            { modelsChanged: true },
            { onFinallyDone: deleteColumns}
          );
        } 
        else {
          doTextTransform(
              columnName,
              expression,
              onError,
              repeat,
              repeatCount,
              { onFinallyDone: deleteColumns});
        }
      };
      // core of doJoinColumn
      elmts.dialogHeader.text($.i18n('core-views/column-join'));
      elmts.or_views_column_join_before_column_picker.text($.i18n('core-views/column-join-before-column-picker'));
      elmts.or_views_column_join_before_options.text($.i18n('core-views/column-join-before-options'));
      elmts.or_views_column_join_replace_nulls.text($.i18n('core-views/column-join-replace-nulls'));
      elmts.or_views_column_join_replace_nulls_advice.text($.i18n('core-views/column-join-replace-nulls-advice'));
      elmts.or_views_column_join_skip_nulls.text($.i18n('core-views/column-join-skip-nulls'));
      elmts.or_views_column_join_write_selected_column.text($.i18n('core-views/column-join-write-selected-column'));
      elmts.or_views_column_join_copy_to_new_column.text($.i18n('core-views/column-join-copy-to-new-column'));
      elmts.or_views_column_join_delete_joined_columns.text($.i18n('core-views/column-join-delete-joined-columns'));
      elmts.or_views_column_join_field_separator.text($.i18n('core-views/column-join-field-separator'));
      elmts.or_views_column_join_field_separator_advice.text($.i18n('core-views/column-join-field-separator-advice'));
      elmts.or_views_column_join_dont_escape.text($.i18n('core-views/column-join-dont-escape'));
      elmts.selectAllButton.html($.i18n('core-buttons/select-all'));
      elmts.deselectAllButton.html($.i18n('core-buttons/deselect-all'));
      elmts.okButton.html($.i18n('core-buttons/ok'));
      elmts.cancelButton.html($.i18n('core-buttons/cancel'));
      /*
      * Populate column list.
      */
      for (var i = 0; i < theProject.columnModel.columns.length; i++) {
        var col = theProject.columnModel.columns[i];
        var colName = col.name;
        var div = $('<div>').
          addClass("column-join-column")
          .attr("column", colName)
          .appendTo(elmts.column_join_columnPicker);
        $('<input>').
          attr('type', 'checkbox')
          .attr("column", colName)
          .prop('checked',(i == columnIndex) ? true : false)
          .appendTo(div);
        $('<span>')
          .text(colName)
          .appendTo(div);
      }
      // Move the selected column on the top of the list
      if (columnIndex > 0) {
        selectedColumn = elmts.column_join_columnPicker
          .find('.column-join-column')
          .eq(columnIndex);
        selectedColumn.parent().prepend(selectedColumn);
      }
      // Make the list sortable
      elmts.column_join_columnPicker.sortable({});
    /*
      * Hook up event handlers.
      */
      elmts.column_join_columnPicker
        .find('.column-join-column')
        .click(function() {
          elmts.column_join_columnPicker
          .find('.column-join-column')
          .removeClass('selected');
          $(this).addClass('selected');
        });
      elmts.selectAllButton
        .click(function() {
          elmts.column_join_columnPicker
          .find('input[type="checkbox"]')
          .prop('checked',true);
        });
      elmts.deselectAllButton
        .click(function() {
          elmts.column_join_columnPicker
          .find('input[type="checkbox"]')
          .prop('checked',false);
        });
      elmts.okButton.click(function() {
        transform();
        dismiss();
      });
      elmts.cancelButton.click(function() {
        dismiss();
      });
      elmts.new_column_nameInput.change(function() {
        if (elmts.new_column_nameInput[0].value != "") {
          elmts.copy_to_new_columnInput.prop('checked',true);
        } else
          {
          elmts.write_selected_columnInput.prop('checked',true);
          }
      });
      elmts.null_substituteInput.change(function() {
          elmts.replace_nullsInput.prop('checked',true);
      });
    };

    var doSplitMultiValueCells = function() {
      var frame = $(DOM.loadHTML("core", "scripts/views/data-table/split-multi-valued-cells-dialog.html"));
      var elmts = DOM.bind(frame);
      elmts.dialogHeader.text($.i18n('core-views/split-cells'));

      elmts.or_views_howSplit.text($.i18n('core-views/how-split-cells'));
      elmts.or_views_bySep.text($.i18n('core-views/by-sep'));
      elmts.or_views_separator.text($.i18n('core-views/separator'));
      elmts.or_views_regExp.text($.i18n('core-views/reg-exp'));

      elmts.or_views_fieldLen.text($.i18n('core-views/field-len'));
      elmts.or_views_listInt.text($.i18n('core-views/list-int'));

      elmts.or_views_byCase.text($.i18n('core-views/by-case'));
      elmts.or_views_byNumber.text($.i18n('core-views/by-number'));
      elmts.or_views_revCase.text($.i18n('core-views/by-rev'));
      elmts.or_views_revNum.text($.i18n('core-views/by-rev'));
      elmts.or_views_caseExample.text($.i18n('core-views/by-case-example'));
      elmts.or_views_caseReverseExample.text($.i18n('core-views/by-case-rev-example'));
      elmts.or_views_numberExample.text($.i18n('core-views/by-number-example'));
      elmts.or_views_numberReverseExample.text($.i18n('core-views/by-number-rev-example'));

      elmts.okButton.html($.i18n('core-buttons/ok'));
      elmts.cancelButton.text($.i18n('core-buttons/cancel'));

      var level = DialogSystem.showDialog(frame);
      var dismiss = function() { DialogSystem.dismissUntil(level - 1); };
      
      var defaultValue = Refine.getPreference("ui.cell.rowSplitDefaultSeparator", ",");
      elmts.separatorInput[0].value = defaultValue;
      elmts.separatorInput.focus().select();
      
      elmts.cancelButton.click(dismiss);
      elmts.okButton.click(function() {
        var mode = $("input[name='split-by-mode']:checked")[0].value;
        var config = {
          columnName: column.name,
          keyColumnName: theProject.columnModel.keyColumnName,
          mode
        };
        if (mode === "separator") {
          config.separator = elmts.separatorInput[0].value;
          if (!(config.separator)) {
            alert($.i18n('core-views/specify-sep'));
            return;
          }

          config.regex = elmts.regexInput[0].checked;
          Refine.setPreference("ui.cell.rowSplitDefaultSeparator", config.separator);
        } else if (mode === "lengths") {
          var s = "[" + elmts.lengthsTextarea[0].value + "]";
          try {
            var a = JSON.parse(s);

            var lengths = [];
            $.each(a, function(i,n) {
              if (typeof n == "number") {
                lengths.push(n);
              }
            });

            if (lengths.length === 0) {
              alert($.i18n('core-views/warning-no-length'));
              return;
            }

            config.fieldLengths = JSON.stringify(lengths);

          } catch (e) {
            alert($.i18n('core-views/warning-format'));
            return;
          }
        } else if (mode === "cases") {
          if(elmts.reversTranistionCases[0].checked) {
            config.separator = "(?<=\\p{Upper}|[\\p{Upper}][\\s])(?=\\p{Lower})";
          } else {
            config.separator = "(?<=\\p{Lower}|[\\p{Lower}][\\s])(?=\\p{Upper})";
          }
          config.regex = true;
        } else if (mode === "number") {
          if(elmts.reversTranistionNumbers[0].checked) {
            config.separator = "(?<=\\p{L}|[\\p{L}][\\s])(?=\\p{Digit})";
          } else {
            config.separator = "(?<=\\p{Digit}|[\\p{Digit}][\\s])(?=\\p{L})";
          }
          config.regex = true;
        }

        Refine.postCoreProcess(
          "split-multi-value-cells",
          config,
          null,
          { rowsChanged: true }
        );

        dismiss();
      });
    };

    var doJoinMultiValueCells = function(separator) {
      var defaultValue = Refine.getPreference("ui.cell.rowSplitDefaultSeparator", ",");
      var separator = window.prompt($.i18n('core-views/enter-separator'), defaultValue);
      if (separator !== null) {
        Refine.postCoreProcess(
          "join-multi-value-cells",
          {
            columnName: column.name,
            keyColumnName: theProject.columnModel.keyColumnName,
            separator
          },
          null,
          { rowsChanged: true }
        );
        Refine.setPreference("ui.cell.rowSplitDefaultSeparator", separator);
      }
    };
  
    this._elmts.splitCol.click(function() { 
      doSplitColumn();
    });

    this._elmts.mergeCol.click(function() { 
      doJoinColumns();
    });

    this._elmts.splitMultiCell.click(function() { 
      doSplitMultiValueCells();
    });

    this._elmts.mergeMultiCell.click(function() { 
      doJoinMultiValueCells();
    });

  };

  updateState(data) {
    this._update();
  };

  render() {
    this._setRangeIndicators();
  };

  _reset() {
    this._query = null;
    this._config.mode = "text";
    this._config.caseSensitive = false;
    this._elmts.input.val([]);
    this._elmts.caseSensitiveCheckbox.prop("checked", false);
    this._elmts.regexCheckbox.prop("checked", false);
    this._config.invert = false;

    this._updateRest();
  };

  _invert() {
    this._config.invert = !this._config.invert;

    this._updateRest();
  };

  _update() {
    var invert = this._config.invert;
    if (invert) {
      this._elmts.facetTitle.addClass("facet-title-inverted");
      this._elmts.invertButton.addClass("facet-mode-inverted");
    } else {
      this._elmts.facetTitle.removeClass("facet-title-inverted");
      this._elmts.invertButton.removeClass("facet-mode-inverted");
    }
  };

  _scheduleUpdate() {
    if (!this._timerID) {
      var self = this;
      this._timerID = window.setTimeout(function() {
        self._timerID = null;
        self._updateRest();
      }, self._config.mode === 'regex' ? 1500 : 500);
    }
  };

  _updateRest() {
    Refine.update({ engineChanged: true });
  };

  _uniqueIdForLabels() {
    return AdvancedSettingFacet.textSearchFacetCounterForLabels++;
  };
}


AdvancedSettingFacet.reconstruct = function(div, uiState) {
  return new AdvancedSettingFacet(div, uiState.c, uiState.o);
};
