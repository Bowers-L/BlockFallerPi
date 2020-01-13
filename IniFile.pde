import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Note: This code is largely third party. However, all of the comments were made by me (Logan Bowers) in order to understand what I was using.

/*
 * An ini file is a file used to hold data/configuration settings by using key value pairs.
 * ini files are organized by sections starting with [ and ending with ]. For example: [my section]
 * Key value pairs are denoted "key = value".
 * In this class, values can be accessed as a string, int, float, or double
 */

public class IniFile {

   //These patterns are used by the matcher to search for specific
   private Pattern  _section  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );  //Regex expression used to find a section header in the file
   private Pattern  _keyValue = Pattern.compile( "\\s*([^=]*)=(.*)" );  //Regex expression used to find a key value pair in the file
   
   private Map<String, Map<String, String>>  _entries  = new HashMap<String, Map<String, String>>();  //Entries described by a Section and a Key, Value pair.

   public IniFile(String path) throws IOException {  //IniFile is instantiated by passing a path name
      parse(path);  //Parse an ini file with the specified path.
   }

   public void parse(String path) throws IOException {
     
      BufferedReader br = new BufferedReader(new FileReader(path));  //Buffered Reader pushes lines through the input stream when the read() function is called.
      try {
        
       //Strings used to hold information within the file
         String line;
         String section = null;
         
         /*
         *This while loop executes for every line in the file detected by the buffered reader.
         *Each line is matched against one of the patterns declared at the top of the class, 
         *which is then used to create a mapping entry in _entries.
         */
         while((line = br.readLine()) != null ) {  //While a line exists to read
           
            Matcher m = _section.matcher(line);  //Testing the line in the file against the _section pattern
            
            if(m.matches()) {  //Line is a section
               section = m.group(1).trim();
            } else if(section != null) {  //The file has a section (in other words: this method is useless if the .ini file does not have a header)
              
               m = _keyValue.matcher( line );
               
               if( m.matches()) {  //Line is a key/value pair
                  String k   = m.group(1).trim();  //Get the key
                  String value = m.group(2).trim();  //Get the value
                  
                  //Create the key value mapping
                  Map<String, String> kv = _entries.get(section);  //Index into the map (outer layer is <section, kv>)
                  if( kv == null ) {  //Only create a new section if it has not already been created.
                     _entries.put(section, kv = new HashMap<String, String>());   
                  }
                  kv.put(k, value);  //Put the new key value pair into the mapping specified by the section
               }
            }
         }
      } finally {}
      br.close();
   }

   //Gets a string value from the specified section and key.
   public String getString(String section, String key, String defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return kv.get(key);
   }

   //Gets an int value from the specified section and key.
   public int getInt(String section, String key, int defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Integer.parseInt(kv.get( key ));
   }

   //Gets a float value from the specified section and key.
   public float getFloat(String section, String key, float defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Float.parseFloat(kv.get( key ));
   }

   //Gets a double value from the specified section and key.
   public double getDouble(String section, String key, double defaultvalue) {
      Map<String, String> kv = _entries.get(section);
      if(kv == null) {
         return defaultvalue;
      }
      return Double.parseDouble(kv.get( key ));
   }
}