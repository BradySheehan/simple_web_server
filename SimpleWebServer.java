import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.io.PrintWriter;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;

/**
 * Program written by Brady Sheehan
 * A single-thread web server that returns files located in
 * the current user directory with the appropriate MIME types.
 * This handles HTTP/1.1 GET requests only.
 *
 * Note: Skeleton provided by Dr. Jackson.
 */

public class SimpleWebServer {

	public static String[] readRequest(BufferedReader br) {

		//read in request, validate the request, then respond with status code related to the request they sent

		String filePath = "";
		System.err.println("Started reading request");

		boolean valid = true;
		String startLine = "";
		String response = "";
		String URI = "";
		try {
			startLine = br.readLine();
			System.out.println(startLine);

			//***Process the start line***//

			String startLineParts[] = startLine.split(" ");
			System.err.println("size of startLineParts = " + startLineParts.length);
			System.out.println("part 0 = " + startLineParts[0]);
			System.out.println("part 1 = " + startLineParts[1]);
			System.out.println("part 2 = " + startLineParts[2]);
			if(!startLineParts[0].matches("GET")) {
				valid = false; //checks request method
				System.err.println("valid = false 1");
			}

			Pattern uri = Pattern.compile("^/"); //must start with "/"
			Matcher m1 = uri.matcher(startLineParts[1]);
			if(!m1.find()) { //checks the Request-URI is of the correct format
				valid = false;
				System.err.println("valid = false 2");
			}

			if(!startLineParts[2].trim().matches("HTTP/1.1")) { //checks HTTP version
				valid = false;
				System.err.println("valid = false 3");
			}

			if(valid) { //look for file name and read file
				String requestURI = startLineParts[1].replace("/", "").trim();
				System.out.println("request URI = " + requestURI);
				File file = new File(filePath+requestURI);
				System.out.println("file.exists() = " + file.exists());
				System.out.println("!file.isDirectory() = " + !file.isDirectory());
				if(file.exists() && !file.isDirectory()) { //got this check from stack overflow here:
					//http://stackoverflow.com/questions/1816673/how-do-i-check-if-a-file-exists-in-java
					URI = startLineParts[1];
					System.out.println("URI = " + URI);
				} else {
					valid = false;
					System.err.println("valid = false 4");
				}
			}

			System.err.println("finished processing the start line");

			//***Process the Header Fields***//

			System.err.println("started processing the header fields");

			String line = br.readLine();
			while(line.length()!=0) { //the header is followed by a blank line, so read until blank line
				System.out.println(line);
				line = br.readLine();
			}

			System.err.println("finished reading header fields");

			//***Process the optional message body***//

			//no message body when dealing with GET requests
			//if any of the checks failed, then here will will assign a response code of 404
			if(valid) {
				response = "202";
			} else {
				response = "404";
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] result = {response, URI};
		System.err.println("finished reading request.");
		return result;
	}

	public static void writeResponse(OutputStream out, String[] result, String date) {

		//note:
		//result[0] = response code
		//result[1] = URI, so of the form /filename.ext

		System.err.println("Starting writing response.");
		long fileLength = 0;
		File file = null;
		PrintWriter pw = new PrintWriter(out);
		//String filePath = "/Users/BradySheehan/Documents/Development/java/web_based_systems_assignments/assignment_1/BradySheehanWebServer/src/";
		String filePath = "";
		//***Write status line***//

		if(result[0].matches("202")) {
			System.err.println("respone 202");
			pw.println("HTTP/1.1 200 OK");
			file = new File(filePath + result[1].replace("/", ""));

			fileLength = file.length();
			System.out.println("file.length() = " + file.length());
			System.err.println("Finished writing status line.");

			//***Write header Fields***//

			String mimeType = URLConnection.getFileNameMap().getContentTypeFor(file.getName());

			pw.println("Date: "+ date);
			pw.println("Content-Type: " + mimeType); //need to use the URLCONnection thing from the book
			pw.println("Content-Length: " + (int)fileLength);

			System.err.println("Finished writing required header fields.");

			//***Write blank Line***//

			pw.println("");
			pw.flush(); //flush before writing the file to the output stream
			//here, quit using this
			System.err.println("Finished writing blank line and flushing.");

			//***Write optional Message body***//

			//create a new file input stream and read bytes with it (read method)
			//and then write the bytes with the output stream object directly
			//copies the file from the input stream to the outputstream object byte by byte
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				while(in.available()!=0) {
					out.write(in.read()); //reads a byte from the file input stream and writes it directly to the sockets output stream
				}

				System.err.println("Completed writing file to outputstream object ");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				pw.close();

				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			try {
				//the file was not found and we will write 404 file not found to the browser
				System.err.println("respone 404");
				pw.println("HTTP/1.1 404 File Not Found");
				pw.println("Date: "+ date);
				pw.println("Content-Type: "); //need to use the URLCONnection thing from the book
				pw.println("Content-Length: " + 0);
				pw.println("");
				pw.flush();
				String failure = "404 File Not Found";
				byte[] b = failure.getBytes(Charset.forName("UTF-8")); //convert the message to bytes so it can be written to the stream
				out.write(b);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				pw.close();
			}
		}
	}

	public static void main (String args[]) {
		ServerSocket mySocket = null;
		try {
			File file = new File("./");
			System.out.println("current file path =  " + file.getAbsolutePath());
			// Create server socket bound to port 8080
			mySocket = new ServerSocket(8080);
			// Repeat until someone kills us
			while (true) {
				// Listen for a connection
				Socket yourSocket = mySocket.accept();
				// If we reach this line, someone connected to our port!
				// Create the Date header value string
				SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz", new DateFormatSymbols(Locale.US));
				formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
				String dateTime = formatter.format(new Date());

				//read in request, validate the request, then respond with code related to the request they sent
				BufferedReader br = new BufferedReader(new InputStreamReader(yourSocket.getInputStream()));
				String response[] = readRequest(br);
				OutputStream output = yourSocket.getOutputStream();
				writeResponse(output, response, dateTime);
				yourSocket.close();// Done with this connection. Close the socket.
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally { //ensure that the socket was closed
			try {
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
	}
}