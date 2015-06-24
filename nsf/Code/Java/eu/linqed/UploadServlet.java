package eu.linqed;

import java.io.File;
import java.io.IOException;
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

			// We only accept POST requests
			if (!req.getMethod().equals("POST")) {
				
				res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return;

			}
			
			// Check if we're receiving the right content
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

			// Parse the incoming request
			List<FileItem> items = upload.parseRequest(req);

			// Create a target document in the current database
			Database dbCurrent = ExtLibUtil.getCurrentDatabase();
			Document docTarget = dbCurrent.createDocument();
			
			// Create a MIME entity in which we're storing all uploaded files
			MIMEEntity mime = docTarget.createMIMEEntity("Files");
			
			// Loop through all data received (files as well as form fields)

			for (FileItem item : items) {

				if (item.isFormField()) { //'form fields' contain the form data that is send along with the request

					System.out.println("form field: " + item.getFieldName() + " : " + item.getString());
					
					docTarget.replaceItemValue(item.getFieldName(), item.getString());

				} else {

					processUploadedFile(item, mime);
				}
			}

			docTarget.save();

			res.setStatus(HttpServletResponse.SC_OK);

			out.print("{ \"success\" : true }"); //no, this is not how I would do it in production

		} catch (Exception e) {

			e.printStackTrace();

			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.print("{ \"success\" : false, \"message\" : \"" + e.getMessage() + "\"}");

		}

	}

	/*
	 * Store an uploaded fild as a MIMEEntity in a MIME item
	 */
	private void processUploadedFile(FileItem item, MIMEEntity mime) throws IOException, NotesException {

		System.out.println("- processing uploaded file: " + item.getName());

		MIMEEntity child = mime.createChildEntity();

		MIMEHeader header = child.createHeader("content-disposition");
		header.setHeaderVal("attachment;filename=\"" + item.getName() + "\"");

		Stream stream = ExtLibUtil.getCurrentSession().createStream();
		stream.write(item.get());

		child.setContentFromBytes(stream, item.getContentType(), MIMEEntity.ENC_IDENTITY_BINARY); //ENC_BASE64);
		child.decodeContent();

	}

}
