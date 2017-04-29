import java.io.File;
import java.io.FileWriter;

public class test_file_gen3 {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        File file = new File("file75k1.txt");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);

        for (int length = 0; length <= 75000; length += 39) {
			writer.write("abcdefghijkl");
			writer.write("\n");
			writer.write("abcdefghijkl");
			writer.write("\n");
			writer.write("abcdefghijkl");
			writer.write("\n");
		}

        writer.flush();
        writer.close();
        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000f + " seconds");
    }
}