import model.AccessionData;
import org.apache.commons.cli.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.List;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private static final String HELP_INFORMATION = "This tool  fetches and stores GEO metadata from DB to specified file";
    private static String fileName = "result.csv";

    private Option fileNameOption;
    private Option helpOption;
    private Options options = new Options();

    public Main() {
        buildOptions();
    }

    public static void main(String[] args) {
        new Main().serve(args);
    }

    private void serve(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(helpOption.getOpt())) {
                printHelp(HELP_INFORMATION);
                return;
            }
            if (cmd.hasOption(fileNameOption.getOpt())) {
                fileName = cmd.getOptionValue(fileNameOption.getOpt());
            }
        } catch (ParseException e) {
            printHelp(e.getMessage());
            logger.info("Not correct initial arguments");
            return;
        }

        DataFetcher dataFetcher = new DataFetcher();
        List<AccessionData> accessionData = dataFetcher.fetchData();
        CSVDataWriter.writeToCSVFile(fileName, accessionData);
    }

    private void printHelp(String message) {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println(message);
        formatter.printHelp("geo-data [options] [filename]", options);
    }

    private void buildOptions() {
        fileNameOption = Option
                .builder("f")
                .longOpt("file")
                .argName("file name")
                .hasArg()
                .desc("CSV file name")
                .build();
        helpOption = Option
                .builder("h")
                .longOpt("help")
                .desc("Display usage")
                .build();
        options.addOption(fileNameOption);
        options.addOption(helpOption);
    }
}
