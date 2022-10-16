/*
 * Copyright (c) 2010,2011,2015 Thomas F. Morris <tfmorris@gmail.com>
 *               2018,2019 OpenRefine contributors
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * Neither the name of Google nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.refine.extension.gdata;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.File.ContentHints;
import com.google.api.services.drive.model.File.ContentHints.Thumbnail;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.commands.browsing.GetContentAccessCommand;
import com.google.refine.commands.project.ExportRowsCommand;
import com.google.refine.exporters.CsvExporter;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class UploadCommand extends Command {
    static final Logger logger = LoggerFactory.getLogger("gdata_upload");
    
    private static final String METADATA_DESCRIPTION = "OpenRefine project dump";
    private static final String METADATA_ICON_FILE = "logo-openrefine-550.png";

    // TODO: We need a way to provide progress to the user during long uploads
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	if(!hasValidCSRFToken(request)) {
    		respondCSRFError(response);
    		return;
    	}
    	Properties params = ExportRowsCommand.getRequestParameters(request);
        String format = params.getProperty("format");
        String token = TokenCookie.getToken(request);
        if (token == null & !("gdata/content-access").equals(format)) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }

        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            
            String name = params.getProperty("name");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            try {
                writer.writeStartObject();
                
                List<Exception> exceptions = new LinkedList<Exception>();
                String url = upload(project, engine, params, token, name, exceptions);
                // The URL can be non-null even if it doesn't fail
                if (url != null && exceptions.size() == 0) {
                    writer.writeStringField("status", "ok");
                    writer.writeStringField("url", url);
                } else if (exceptions.size() == 0) {
                    writer.writeStringField("status", "error");
                    writer.writeStringField("message", "No such format");
                } else {
                    for (Exception e : exceptions) {
                        logger.warn(e.getLocalizedMessage(), e);
                    }
                    writer.writeStringField("status", "error");
                    writer.writeStringField("message", exceptions.get(0).getLocalizedMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                writer.writeStringField("status", "error");
                writer.writeStringField("message", e.getMessage());
            } finally {
                writer.writeEndObject();
                writer.flush();
                writer.close();
                w.flush();
                w.close();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }

    private String upload(
            Project project, Engine engine, Properties params,
            String token, String name, List<Exception> exceptions) throws Exception {
        String format = params.getProperty("format");
        if ("gdata/google-spreadsheet".equals(format)) {
            return uploadSpreadsheet(project, engine, params, token, name, exceptions);
        } else if (("raw/openrefine-project").equals(format)) {
            return uploadOpenRefineProject(project, token, name, exceptions);
        } else if (("gdata/content-access").equals(format)) {
            return uploadContentAcessFile(project, engine, params, name, exceptions);
        }
        return null;
    }

    private String uploadContentAcessFile(Project project, Engine engine, Properties params, String name,
			List<Exception> exceptions) throws Exception {
    	try {
    		System.out.println(name);
            String fname = name.substring(0, name.lastIndexOf('.'));
            String file_extension = name.substring(name.lastIndexOf('.') + 1);
            CsvExporter csvExport = new CsvExporter();
            String openrefine_data = System.getenv("ENV_OPENREFINE_DATA");
            String local_path = openrefine_data != null ? openrefine_data + "/" : "C:\\\\\\\\Users/User/Desktop/";
            String file_path = local_path + name;
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file_path), "utf-8");
            csvExport.export(project, params, engine, writer);
            writer.close();

            //parameters
            boolean reachable = GetContentAccessCommand.isReachable(GetContentAccessCommand.DEFAULT_SERVICE_DOMAIN);
            String api_url = reachable ? GetContentAccessCommand.DEFAULT_SERVICE_DOMAIN : GetContentAccessCommand.DEFAULT_API_URL;
            String env_project_id = System.getenv("PROJECT_ID");
            String project_id = env_project_id != null ? env_project_id : GetContentAccessCommand.DEFAULT_PROJECT_ID;
            String requestURL = api_url + "/projects/" + project_id;
            String task_id = params.getProperty("task_id").replace("\r\n", "").replace("\r", "").replace("\n", "");
            boolean is_task_id_invalid = (!task_id.equals("") & !task_id.equals("null") & task_id != null);
            if (!is_task_id_invalid) {
                throw new Exception("Missing or invalid task_id parameter");
            }

            String para_accessToken = params.getProperty("access_token");
            boolean isinvalid = (!para_accessToken.equals("") & !para_accessToken.equals("null") & para_accessToken != null);
            String accessToken = isinvalid ? "Bearer " + para_accessToken : GetContentAccessCommand.DEFAULT_ACCESS_TOKEN;
            // call upload api
            HttpURLConnection con = GetContentAccessCommand.uploadFile(new java.io.File(file_path), requestURL, accessToken);
            int resCode = con.getResponseCode();
            if (resCode == 200) {
            	String status = "success";
            	System.out.println(task_id);
            	String status_data = "{\"task_id\": \"" + task_id + "\",\"status\":\"" + status + "\",\"name_after\":\"" + fname + "\",\"subtype\":\"" + file_extension + "\"}";
            	System.out.println(status_data);
            	GetContentAccessCommand.setStatus(status_data);
                return requestURL;
            } else {
                if (resCode == 401) {
                    throw new Exception("沒有上傳權限");
                } else if (resCode == 400) {	
                    throw new Exception("檔案已存在");
                } else {
                	System.out.print(resCode);
                    throw new Exception("上傳檔案發生錯誤");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            exceptions.add(e);
        }
        return "";
    }

    protected byte[] getIconImage() throws IOException {
        InputStream is = getClass().getResourceAsStream(METADATA_ICON_FILE);
        return IOUtils.toByteArray(is);
    }

    private String uploadOpenRefineProject(Project project, String token,
            String name, List<Exception> exceptions) {
        FileOutputStream fos = null;
        
        try {
            java.io.File filePath = java.io.File.createTempFile(name, ".tgz"); 
            filePath.deleteOnExit();
            
            fos = new FileOutputStream(filePath);
            FileProjectManager.gzipTarToOutputStream(project, fos);

            Thumbnail tn = new Thumbnail();
            tn.setMimeType("image/x-icon").encodeImage(getIconImage());
            ContentHints contentHints = new ContentHints();
            contentHints.setThumbnail(tn); 

            File fileMetadata = new File();
            fileMetadata.setName(name + ".tar.gz")
                .setDescription(METADATA_DESCRIPTION)
                .setContentHints(contentHints);
            FileContent projectContent = new FileContent("application/x-gzip", filePath);
            File file = GoogleAPIExtension.getDriveService(token)
                    .files().create(fileMetadata, projectContent)
                .setFields("id")
                .execute();
            logger.info("File ID: " + file.getId());
            
            return file.getId();
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            exceptions.add(e);
        } 
        
        return null;
    }

    static private String uploadSpreadsheet(
            final Project project, final Engine engine, final Properties params,
            String token, String name, List<Exception> exceptions) {
        
        Drive driveService = GoogleAPIExtension.getDriveService(token);
        
        try {
            File body = new File();
            body.setName(name);
            // TODO: Internationalize (i18n)
            body.setDescription("Spreadsheet uploaded from OpenRefine project: " + name);
            body.setMimeType("application/vnd.google-apps.spreadsheet");

            File file = driveService.files().create(body).execute();
            String spreadsheetId =  file.getId();

            SpreadsheetSerializer serializer = new SpreadsheetSerializer(
                    GoogleAPIExtension.getSheetsService(token),
                    spreadsheetId,
                    exceptions);
            
            CustomizableTabularExporterUtilities.exportRows(
                    project, engine, params, serializer);
            
            return serializer.getUrl();
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            exceptions.add(e);
        }
        return null;
    }
}
