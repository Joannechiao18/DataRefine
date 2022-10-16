/*

Copyright 2011, Google Inc.
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

 //開發測試使用

 function ThisComputerImportingSourceUI(controller) {
  this._controller = controller;
}

Refine.DefaultImportingController.sources.push({
  "label": $.i18n('core-index-import/this-computer'),
  "id": "upload",
  "uiClass": ThisComputerImportingSourceUI
});

ThisComputerImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-computer-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  
  $('#or-import-locate-files').text($.i18n('core-index-import/locate-files'));
  this._elmts.nextButton.html($.i18n('core-buttons/next'));

  this._elmts.nextButton.click(function(evt) {
    if (self._elmts.fileInput[0].files.length === 0) {
      window.alert($.i18n('core-index-import/warning-data-file'));
    } 
    else {
      self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/uploading-data'));
    }
  });
};

ThisComputerImportingSourceUI.prototype.focus = function() {
};

// Function to check if the URL getting entered is valid or not 
function isUrlValid(url) {
  // regex for a valid URL pattern
  // Derived from the jquery-validation repository https://github.com/jquery-validation/jquery-validation/blob/master/src/additional/url2.js
  return /^(https?|s?ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(url);
}
function FastApiImportingSourceUI(controller) {
  this._controller = controller;
}
Refine.DefaultImportingController.sources.push({
  "label": $.i18n('core-index-import/fast-api'),
  "id": "FastAPI",
  "uiClass": FastApiImportingSourceUI
});

FastApiImportingSourceUI.prototype.attachUI = function(bodyDiv) {
  var self = this;
  let token;
  try{
    var get_token_request = new XMLHttpRequest();
    get_token_request.onreadystatechange = function() {
        if (get_token_request.readyState == XMLHttpRequest.DONE) {
            token = get_token_request.getResponseHeader('x-auth-request-access-token');
        }
    }
    get_token_request.open('GET', '/oauth2/auth', false);
    get_token_request.send(null);
  }catch(e){
    token = null;
  }

  bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-fast-api-form.html"));

  this._elmts = DOM.bind(bodyDiv);
  
  $('#or-import-fast-api').text($.i18n('core-index-import/fast-api-label'));
  $('#or-import-fast-name').text($.i18n('core-index-import/fast-name'));
  $('#or-import-fast-type').text($.i18n('core-index-import/fast-type'));
  $('#or-import-fast-last-mod').text($.i18n('core-index-import/fast-last-mod'));

// 新手教學

if(localStorage.getItem('init_times') === null){
  localStorage.setItem('init_times',"0");
}
init_times = localStorage.getItem('init_times');

let infoModal=document.querySelector("#infoModal");
let close=document.querySelector("#close");

if(init_times == "0"){
  infoModal.showModal();
}

close.addEventListener("click", function(){
  infoModal.close();
  localStorage.setItem('init_times',"1");
})

var _contain=document.querySelector(".contain")   
var _a1=document.querySelector("a:nth-of-type(1)");  
var _a2=document.querySelector("a:nth-of-type(2)");  
var _spots=document.querySelectorAll(".spots span"); 
var _wrapper=document.querySelector(".wrapper");

var index=0;  
  function next_pic(){
    if(_contain.offsetLeft<=-7200){
      _contain.style.left=0;
    }else{
      _contain.style.left=(_contain.offsetLeft-900)+"px";
    }
    index++;
    if(index==9){
      index=0;
    }
    
    spots();
    
  }
  
  function prev_pic(){
    if(_contain.offsetLeft>=0){
      _contain.style.left="-7200px";
    }else{
      _contain.style.left=(_contain.offsetLeft+900)+"px";
    }
    index--;
    if(index==-1){
      index=8;
    }
    spots();
  }
  
   function spots(){
     for (var i = 0; i < _spots.length; i++) {
          if(i==index){
         _spots[i].className="effect";
       }else{
         _spots[i].className=""; 
       }
     }
   }
  
  _a1.onclick=function(){
    prev_pic();
  }
  _a2.onclick=function(){
    next_pic();
  }





  // var dismiss = DialogSystem.showBusy($.i18n('gdata-exporter/loading'));
  // $.get("command/core/content-access?operation=list",
  // {
  //   "access_token":token
  // },function(data) {
  //     dismiss();
  //     if(data.result==0){
  //       let res = data.data;
  //       for(let index in res){
  //         let template = $('#list-sample').clone(true,true).removeAttr('id').css('display','');
  //         template.find('.file-check').attr('value',res[index].filename);
  //         template.find('.flie-name').html(res[index].filename);
  //         template.find('.flie-type').html(res[index].datatype);
  //         template.find('.file-change-time').html(res[index].created_at);

  //         $('.add-flie-container').append(template);
  //       }
  //     } else if(data.result==1){
  //       alert(data.message);
  //     }
  //     else{
  //       alert("未預期情況發生");
  //     }
  //   }
  // );

  // Next Button文字
  this._elmts.nextButton.html($.i18n('core-buttons/next'));

  this._elmts.form.submit(function(evt){
    initializeImportFile(self, true);
  });
  // 初始化直接自動匯入檔案
  let file_name = localStorage.getItem("file_name");
  if (file_name != null) {
    initializeImportFile(self, false);
  }
  
};


// 初始化直接自動匯入檔案
function initializeImportFile(self, isManual) {
  var allProjectsName = [];
  var allProjectsID = [];

  $.ajax({
    type : 'GET',
    url : "command/core/get-all-project-metadata",
    dataType : 'json',
    success : function(data) {
              for(var i in data.projects){
                allProjectsName.push(data.projects[i].name);
                allProjectsID = Object.keys(data.projects);
              }
    },
    data : {},
    async : false
  });

  try{
    var get_token_request = new XMLHttpRequest();
    get_token_request.onreadystatechange = function() {
        if (get_token_request.readyState == XMLHttpRequest.DONE) {
            token = get_token_request.getResponseHeader('x-auth-request-access-token');
        }
    }
    get_token_request.open('GET', '/oauth2/auth', false);
    get_token_request.send(null);
    // evt.preventDefault();
  }catch(e){
    token = null;
  }
  
  if(isManual == true & $("input[name='file[]']:checked").length === 0){
    window.alert($.i18n('core-index-import/warning-data-file'));
  }else{
    let file_name = '';
    if (isManual == true){
      // 抓取選取到的fliename用來打download的api
      file_name = $("input[name='file[]']:checked").attr('value');
      localStorage.setItem("file_name", file_name);
    } else {
      file_name = localStorage.getItem("file_name");
    }

    if(file_name != null){  //open project已經存在相同檔案，直接跳轉到操作頁面
      drs_file_name = file_name
      for (var i = 0; i < drs_file_name.length; i++){
        if(/^[a-zA-Z0-9- ]*$/.test(drs_file_name[i]) == false){
          drs_file_name = drs_file_name.replace(drs_file_name[i], ' ');
        }
      }
      if(allProjectsName.includes(drs_file_name)){
        window.location.href = "http://127.0.0.1:3333/project?project="+allProjectsID[allProjectsName.indexOf(drs_file_name)];
      }
      else{  //open project沒有這個project，需要重新create project
        $('#download_url').attr("value","http://127.0.0.1:3333/command/core/content-access?access_token="+ token +"&operation=download&file_name=" + file_name);
        // 取得input的值用以判斷是否有給成功
        var importUrl = self._elmts.urlInput[0].value.trim();
        self._elmts.urlInput[0].value = importUrl; 
        if(!isUrlValid(importUrl)) {
          window.alert($.i18n('core-index-import/warning-web-address'));
        } else {
          // 都OK就跑startImportJob
          self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/downloading-data'));
        }
      }
    }
    
    /*$('#download_url').attr("value","http://127.0.0.1:3333/command/core/content-access?access_token="+ token +"&operation=download&file_name=" + file_name);
    // 取得input的值用以判斷是否有給成功
    var importUrl = self._elmts.urlInput[0].value.trim();
    self._elmts.urlInput[0].value = importUrl; 
    if(!isUrlValid(importUrl)) {
      window.alert($.i18n('core-index-import/warning-web-address'));
    } else {
      // 都OK就跑startImportJob
      self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/downloading-data'));
    } */
  }
}
  

FastApiImportingSourceUI.prototype.focus = function() {
  this._elmts.urlInput.focus();
};


// function UrlImportingSourceUI(controller) {
//   this._controller = controller;
// }
// Refine.DefaultImportingController.sources.push({
//   "label": $.i18n('core-index-import/web-address'),
//   "id": "download",
//   "uiClass": UrlImportingSourceUI
// });

// UrlImportingSourceUI.prototype.attachUI = function(bodyDiv) {
//   var self = this;

//   bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-web-form.html"));

//   this._elmts = DOM.bind(bodyDiv);
  
//   $('#or-import-enterurl').text($.i18n('core-index-import/enter-url'));
//   this._elmts.addButton.html($.i18n('core-buttons/add-url'));
//   this._elmts.nextButton.html($.i18n('core-buttons/next'));

//   this._elmts.form.submit(function(evt){
//     evt.preventDefault();
//     var importUrl = self._elmts.urlInput[0].value.trim(); 
//     self._elmts.urlInput[0].value = importUrl;
//     if(!isUrlValid(importUrl)) {
//       window.alert($.i18n('core-index-import/warning-web-address'));
//     } else {
//       self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/downloading-data'));
//     }
//   });
//   this._elmts.addButton.click(function(evt) {
//     self._elmts.buttons.before(self._elmts.urlRow.clone());
//   });
// };

// UrlImportingSourceUI.prototype.focus = function() {
//   this._elmts.urlInput.focus();
// };

// function ClipboardImportingSourceUI(controller) {
//   this._controller = controller;
// }
// Refine.DefaultImportingController.sources.push({
//   "label": $.i18n('core-index-import/clipboard'),
//   "id": "clipboard",
//   "uiClass": ClipboardImportingSourceUI
// });

// ClipboardImportingSourceUI.prototype.attachUI = function(bodyDiv) {
//   var self = this;

//   bodyDiv.html(DOM.loadHTML("core", "scripts/index/default-importing-sources/import-from-clipboard-form.html"));

//   this._elmts = DOM.bind(bodyDiv);
  
//   $('#or-import-clipboard').text($.i18n('core-index-import/clipboard-label'));
//   this._elmts.nextButton.html($.i18n('core-buttons/next'));
  
//   this._elmts.nextButton.click(function(evt) {
//     if ($.trim(self._elmts.textInput[0].value).length === 0) {
//       window.alert($.i18n('core-index-import/warning-clipboard'));
//     } else {
//       self._controller.startImportJob(self._elmts.form, $.i18n('core-index-import/uploading-pasted-data'));
//     }
//   });
// };

// ClipboardImportingSourceUI.prototype.focus = function() {
//   this._elmts.textInput.focus();
// };


