package beam.utils.conversion;

import org.jfree.util.Log;
import scala.xml.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/*
 * This class accepts an int which defines how big of a byte buffer you want for your file writing.
 * The main use is for writing massive scenario output files for BEAM without destroying your RAM.
 */
public class LargeFileProcessing {
    int buffer;
    byte[] byteBuffer;

    public LargeFileProcessing(int buffer) {
        this.buffer = buffer;
        this.byteBuffer = new byte[buffer];
    }

    public void writeUsingChunks(File output, Node info, int type) throws IOException {
        //System.out.println(info.toString());
        //Here's the alternative: Turn the whole node into a String. Then turn the string into a byte array.
        // Then use a bytearraystream to put 4 * 1024 bytes into a byte array. Once you've done that,
        // use a outputsream and slap that into the file.
        byte[] bytes;
        bytes = info.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        OutputStream outputFileStream = new FileOutputStream(output);
        System.out.println("File Printing Started");
        if (type == 0){
            String docType = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<!DOCTYPE objectattributes SYSTEM \"../dtd/objectattributes_v1.dtd\">\n";
            byte[] intro = docType.getBytes();
            outputFileStream.write(intro);
        }
        else if (type == 1){
            String docType = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<!DOCTYPE objectattributes SYSTEM \"http://www.matsim.org/files/dtd/objectattributes_v1.dtd\">\n";
            byte[] intro = docType.getBytes();
            outputFileStream.write(intro);
        }
        int one_byte_data = byteArrayInputStream.read();
        byte[] data = new byte[buffer];
        int i = 0;
        while(one_byte_data != -1){
            data[i] = (byte) one_byte_data;
            one_byte_data = byteArrayInputStream.read();
            i++;
            if (one_byte_data != -1) {
                if (i == buffer) {
                    outputFileStream.write(data);
                    i = 0;
                }
            }
            else {
                Arrays.fill(data, i, buffer-1, (byte) 32);
                String trailers = new String(data, StandardCharsets.UTF_8);
                trailers = trailers.substring(0,trailers.indexOf("</objectattributes>")+19);
                outputFileStream.write(trailers.getBytes(StandardCharsets.UTF_8));
            }
        }
        //System.out.println(new String(data, StandardCharsets.UTF_8));
        outputFileStream.close();
        byteArrayInputStream.close();
    }
}
