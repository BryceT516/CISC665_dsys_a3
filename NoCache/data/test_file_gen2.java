import java.io.*;

class test_file_gen2 {
	public static void main (String args[]) throws Exception {
		ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream("file75k1"));
		byte[] buf = new byte[75*1024];
		String strBuf = buf.toString();
		s.writeUTF(strBuf);
		s.flush();
		s.close();
	}
}