import java.io.*;

class test_file_gen {
	public static void main (String args[]) throws Exception {
		RandomAccessFile f = new RandomAccessFile("file75k1", "rw");
		f.setLength(75 * 1024);
	}
}