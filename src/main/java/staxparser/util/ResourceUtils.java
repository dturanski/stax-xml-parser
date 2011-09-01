package staxparser.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Common resource Utilities
 * 
 * @author David Turanski
 *
 */
public class ResourceUtils {
    /**
     * Convert contents of an input stream to a String
     * @param inputStream
     * @return the stream contents
     * @throws IOException
     */
   public static String streamToString(InputStream inputStream) throws IOException {
		Writer writer = new StringWriter();
		 byte[] b = new byte[4096];
		 for (int n; (n = inputStream.read(b)) != -1;) {     
		      writer.append(new String(b, 0, n));
		 }
	     return writer.toString();
   }
   
   /**
    * Get class resource as String (convenience wrapper around streamToString)
    * @param clazz - the class
    * @param resourcePath - classpath resource path
    * @return - the contents as a String
    * @throws IOException
    */
   public static String classPathResourceAsString(Class clazz,String resourcePath) throws IOException{
       return streamToString(clazz.getResourceAsStream(resourcePath));
   }
}
