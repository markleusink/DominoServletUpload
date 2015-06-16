package eu.linqed;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.MIMEEntity;
import lotus.domino.MIMEHeader;
import lotus.domino.NotesException;
import lotus.domino.Stream;

import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.ibm.xsp.http.fileupload.FileItem;
import com.ibm.xsp.http.fileupload.disk.DiskFileItemFactory;
import com.ibm.xsp.http.fileupload.servlet.ServletFileUpload;

import frostillicus.xsp.servlet.AbstractXSPServlet;

public class UploadServlet extends AbstractXSPServlet {

	@Override
	protected void doService(HttpServletRequest req, HttpServletResponse res, FacesContext facesContext, ServletOutputStream out)
			throws Exception {

		try {

			System.out.println("UploadServlet start");

			//you'll need something like this if you're running the Angular app on a different domain than the Domino REST API, disabled for now
			/*
			 * if (req.getMethod().equals("OPTIONS")) {
			 * 
			 * 
			 * res.addHeader("Access-Control-Allow-Origin", "*"); res.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, POST");
			 * res.setStatus(HttpServletResponse.SC_OK); // OK return; }
			 */

			if (req.getMethod().equals("GET")) {

				System.out.println("GET IT :)");
				return;

			} else if (!req.getMethod().equals("POST")) {

				res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return;

			}

			boolean isMultipart = ServletFileUpload.isMultipartContent(req);

			if (!isMultipart) {
				throw (new Exception("that's not multipart content: we need that to continue"));
			}

			// Create a factory for disk-based file items
			DiskFileItemFactory factory = new DiskFileItemFactory();
			
			// Configure a repository (to ensure a secure temp location is used)
			ServletContext servletContext = this.getServletConfig().getServletContext();
			File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(repository);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Parse the request
			List<FileItem> items = upload.parseRequest(req);

			/*
			 * For this demo I store all uploaded files, as well as any form fields,
			 * in a single document
			 */
			
	
				Database dbCurrent = ExtLibUtil.getCurrentDatabase();
				Document docTarget = dbCurrent.createDocument();
				MIMEEntity mime = docTarget.createMIMEEntity("Files");
				 
	           

			for (FileItem item : items) {
				
				if (item.isFormField()) { //'form fields' contain the form data
					
					System.out.println("form field: " + item.getFieldName() + " : " + item.getString());
					
					docTarget.replaceItemValue(item.getFieldName(), item.getString());
					
				} else  {
					processUploadedFile(item, mime);
				}
			}

			if (docTarget != null) {
				docTarget.save();
			}
			
			res.setStatus(HttpServletResponse.SC_OK); // OK

			out.print( "{ \"success\" : true }");	//no, this is not how I would do it in production

		} catch (Exception e) {
			
			e.printStackTrace();

			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.print("{ \"success\" : false, \"message\" : \"" + e.getMessage() + "\"}");

		}

	}

	/*
	 * For the demo app I simple store all uploaded files in 1 document
	 */
	private void processUploadedFile(FileItem item, MIMEEntity mime) throws IOException, NotesException {
		
		System.out.println("Processing " + item.getName() );

		 MIMEHeader header = mime.createHeader("content-disposition");
         header.setHeaderVal("attachment;filename=\"" + item.getName() +"\"");
         
         Stream stream=ExtLibUtil.getCurrentSession().createStream();
 
         stream.write(item.get());
      
         mime.setContentFromBytes(stream, item.getContentType(), MIMEEntity.ENC_IDENTITY_BINARY); //ENC_BASE64);
         mime.decodeContent();
		
		
		System.out.println("stored " + item.getName() );

	}

}
