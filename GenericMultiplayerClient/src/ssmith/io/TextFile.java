/*
 * TextFile.java
 *
 * Created on 26 September 2001, 08:57
 */

package ssmith.io;

import java.io.*;

public class TextFile {

	private String type = "unset";
	public static String READ = "read";
	public static String WRITE = "write";
	public static String APPEND = "append";

	private BufferedReader br;
	private BufferedWriter bw;
	//private boolean EOF;
	private String filename;
	private String next_line; 

	/** Creates new TextFile */
	public TextFile() {
	}

	/** Creates new TextFile */
	public void openFile(String filename, String Type) throws FileNotFoundException, IOException {
		this.type = Type;
		this.filename = filename;
		//        System.out.println("Filename: "+this.filename);
		if (Type.equalsIgnoreCase(READ)) {
			br = new BufferedReader(new FileReader(filename));
			//EOF = false;
			this.getNextLine();
		} else if (Type.equalsIgnoreCase(WRITE)) {
			bw = new BufferedWriter(new FileWriter(filename, false));
			//EOF = false;
			//this.getNextLine();
		} else if (Type.equalsIgnoreCase(APPEND)) {
			bw = new BufferedWriter(new FileWriter(filename, true));
			//EOF = false;
			//this.getNextLine();
		} else {
			throw new IOException("TextFile(): Invalid Type (" + Type + ")");
		}
	}


	private void getNextLine() throws IOException {
		this.next_line = br.readLine();
		
	}
	/**
	 * This returns a string, or nothing if EOF reached.
	 */
	public String readLine() throws IOException {
		String s = next_line;
		this.getNextLine();
		return s;
		/*String line;
		line = br.readLine();
		if (line != null) {
			EOF = false;
			// Check if there is any more to read
			if (br.markSupported() == true) {
				br.mark(2056*100);
				if (br.readLine() == null) {
					EOF = true;
				}
				br.reset();
			}
		}
		else {
			EOF = true;
		}
		return line;*/
	}


	public static String ReadAll(String filename, String cr, boolean error) throws IOException {
		try {
			TextFile tf = new TextFile();
			tf.openFile(filename, TextFile.READ);

			StringBuffer str = new StringBuffer();
			while (tf.isEOF() == false) {
				str.append(tf.readLine() + cr);
			}
			tf.close();
			return str.toString();
		} catch (IOException ex) {
			if (error) {
				throw ex;
			}
			return "";
		}
	}


	public boolean isEOF() {
		return next_line == null;
	}


	public void writeLine(String text) throws IOException {
		bw.write(text);
		bw.newLine();
	}


	public void write(String text) throws IOException {
		bw.write(text);
	}


	public void close() throws IOException {
		if (type.equalsIgnoreCase(READ) && br != null) {
			br.close();
		}
		else if (type.equalsIgnoreCase(WRITE) && bw != null) {
			bw.close();
		}
		else if (type.equalsIgnoreCase(APPEND) && bw != null) {
			bw.close();
		}
		//EOF = true;
		next_line = null;
	}


	public String getFilename() {
		return filename;
	}


	public static void QuickAppend(String url, String text, boolean cr, boolean error) throws FileNotFoundException, IOException {
		try {
			TextFile tf = new TextFile();
			tf.openFile(url, TextFile.APPEND);
			if (cr) {
				tf.writeLine(text);
			} else {
				tf.write(text);
			}
			tf.close();
		} catch (IOException ex) {
			if (error) {
				throw ex;
			}
		}

	}


	public static void QuickWrite(String url, String text, boolean error) throws FileNotFoundException, IOException {
		try {
			TextFile tf = new TextFile();
			tf.openFile(url, TextFile.WRITE);
			tf.writeLine(text);
			tf.close();
		} catch (IOException ex) {
			if (error) {
				throw ex;
			}
		}

	}


	public boolean delete() {
		//        System.out.println("Filename: "+filename);
		return new File(filename).delete();
	}


}