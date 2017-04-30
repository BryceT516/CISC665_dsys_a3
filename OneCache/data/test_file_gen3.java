import java.io.File;
import java.io.FileWriter;

public class test_file_gen3 {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        for(int u = 0; u < 5; u++){

            File file = new File("file500k"+ String.valueOf(u+1) +".txt");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);

            for (int length = 0; length <= 500000; length += 39) {
    			writer.write("abcdefghijkl");
    			writer.write("\n");
    			writer.write("abcdefghijkl");
    			writer.write("\n");
    			writer.write("abcdefghijkl");
    			writer.write("\n");
    		}

            writer.flush();
            writer.close();
        }

        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000f + " seconds");
    }
}