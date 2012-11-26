package com.bizosys.onelineserver.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.ServiceFactory;
import com.bizosys.onelineserver.user.UserProfile;
import com.oneline.util.StringUtils;

public class FileUploadServlet extends HttpServlet {
	protected final static Logger LOG = Logger.getLogger(FileUploadServlet.class);
	private static int MAX_UPLOAD_SIZE = 10485760;
	private static String FILE_UPLOAD_ROOT_DIR = "/tmp"; 
	
	public void init(ServletConfig config) throws ServletException {
		LOG.debug("Initializing the upload servlet.");
		super.init(config);
		
		MAX_UPLOAD_SIZE = ServiceFactory.getInstance().
			getAppConfig().getInt("MAX_UPLOAD_SIZE",10485760);

		FILE_UPLOAD_ROOT_DIR = ServiceFactory.getInstance().
			getAppConfig().get("FILE_UPLOAD_ROOT_DIR","/tmp");
		if ( ! FILE_UPLOAD_ROOT_DIR.endsWith("/"))
			FILE_UPLOAD_ROOT_DIR = FILE_UPLOAD_ROOT_DIR + "/";
		File file = new File(FILE_UPLOAD_ROOT_DIR);
		if ( file.exists() ) {
			if ( ! file.isDirectory() ) {
				String msg = "Error in initializing FileUploadServlet. No directory - " + file.getAbsolutePath();
				LOG.fatal(msg);
				throw new ServletException(msg);
			}
			if ( ! file.canWrite() ) {
				String msg = "No permission on directory - " + file.getAbsolutePath();
				LOG.fatal(msg);
				throw new ServletException(msg);
			}
		} else {
			boolean status = file.mkdir();
			if ( ! status) {
				String msg = "Could not create directory - " + file.getAbsolutePath();
				LOG.fatal(msg);
				throw new ServletException(msg);
			}
		}
	}
	
	/**
	 * The post method
	 */
	public void doPost(HttpServletRequest request, 
		HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("Uploading ...First checking authorizing the user");
		OutputStream resOS = response.getOutputStream();

		Object userObject = request.getAttribute("__user");
		String userName = StringUtils.Empty;
		if ( null == userObject) {
			LOG.warn("Unknown user uploading a file");
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "UNKNOWN_USER");
			resOS.close();
			return;
		} else {
			userName = ((UserProfile) userObject).loginid;
			userName = userName.replace("..", "");
			userName = userName.replace("/", "");
		}
		
		
		//	Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		System.out.println("isMultipart :" + isMultipart);
		
		//Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();
		
		// Parse the request
		InputStream source = null;
		FileOutputStream dest = null;
		try {
			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				System.out.println("item :" + item.isFormField());
				if (item.isFormField()) continue;
			
				String fileName = item.getName();
				System.out.println("fileName = " + fileName);
				String userDirectory = FILE_UPLOAD_ROOT_DIR + userName;
				System.out.println("userDirectory = " + userDirectory);
				File userDir = new File(userDirectory);
				if ( ! userDir.exists() ) {
					System.out.println("making userDirectory = " + userDir);
					userDir.mkdirs();
				}
				
				File uploadedFile  = new File(userDirectory, fileName);
				System.out.println("uploadedFile  = " + uploadedFile.getAbsolutePath());
				
				int round = 1;
				while ( uploadedFile.exists() ) {
					uploadedFile = new File(userDirectory, fileName + "." + round);
					round++;
				}
				
				source = item.openStream();
				dest = new FileOutputStream(uploadedFile);
				byte[] buffer = new byte[1024*64];
				int length = -1;
				while (true) {
					length = source.read(buffer);
					if ( length == -1 ) break;
					dest.write(buffer, 0, length);
				}
				System.out.println("upload done.");
				resOS.write( "OK".getBytes() );
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			resOS.write( ("<error>" + ex.getMessage() + "</error>").getBytes());
		} finally {
			if ( null != source ) try { source.close(); } catch (Exception ex){}
			if ( null != dest ) try { dest.close(); } catch (Exception ex){}
			resOS.close();
		}

	} 
}
