### Data fetcher

The program allow to fetch data from GEO db.

#### Usage

To compile it's necessary execute:   

 `mvn clean install -Dlog4j.configuration=PATH_TO_LOG4J_PROPERTIES`

Where PATH_TO_LOG4J_PROPERTIES is a path to log4j.properties file. Now it's located in src/main/resources/log4j.properties.  
In log4j.properties you can change file for logging.

To execute it's necessary to execute JAR located in target.

`java -jar geo-data.jar`

You can use -h key to look how to use app.

`java -jar geo-data.jar -h`

Output:
 >This tool  fetches and stores GEO metadata from DB to specified file    
  usage: geo-data [options] [filename]   
   -f,--file <file name>   CSV file name.  
   -h,--help               Display usage.  
   -s,--save                     Save input order. Default value is false.  
   -t,--thread <thread count>   Set up thread size, default - 1.
  

Default filename is `results.csv`

#### Example usage

The example how to start app and save metadata to res.csv file using 5 threads and with input order:  
* It's necessary to compile project:  
    `mvn clean install -Dlog4j.configuration=${PATH_TO_PROJECT}/src/main/resources/log4j.properties`  
* Execute jar file:  
    `java -jar geo-data.jar -t 5 -f res.csv -s`