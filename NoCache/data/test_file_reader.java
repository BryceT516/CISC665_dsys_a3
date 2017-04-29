import java.io.File;
import java.io.FileWriter;
import java.io.*;
import java.util.Scanner;

public class test_file_reader {
    public static void main(String[] args) throws Exception {
	    File file = new File("filetest.txt");
	    StringBuilder fileContents = new StringBuilder((int)file.length());
	    Scanner scanner = new Scanner(file);
	    String finalFileContents="";
	    String lineSeparator = System.getProperty("line.separator");

	    try {
	        while(scanner.hasNextLine()) {
	            fileContents.append(scanner.nextLine() + lineSeparator);
	        } 
	        
	    } finally {
	        scanner.close();
	    }
	    finalFileContents = fileContents.toString();
	    System.out.println("File length = " + finalFileContents.length());
	    int sectionSize = 64000;
	    int sections = finalFileContents.length() / sectionSize + (finalFileContents.length() % sectionSize == 0 ? 0 : 1);
	    System.out.println(sections);

	    String[] fileSections = new String[sections];
	    int x;
	    for ( x = 0; x < sections -1; x++){
	    	fileSections[x] = finalFileContents.substring(x*sectionSize, (x+1)*sectionSize);
	    }
	    fileSections[x] = finalFileContents.substring(x*sectionSize);

	    for (int u=0; u<fileSections.length; u++){
	    	System.out.println(fileSections[u].length());
	    }

    }
}