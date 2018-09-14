import com.opencsv.CSVWriter;
import model.AccessionData;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVDataWriter {

    private static final Logger logger = LogManager.getLogger(CSVDataWriter.class);

    private final static String[] HEADERS = {"Accession", "Organism", "Genome assembly",
            "Donor sex", "Donor age", "File download url"};

    public static void writeToCSVFile(String fileName, List<AccessionData> accessionData) {
        File file = new File(fileName);
        try (FileWriter outputFile = new FileWriter(file);
             CSVWriter writer = new CSVWriter(outputFile)) {
            writer.writeNext(HEADERS);
            for (AccessionData accession : accessionData) {
                writer.writeNext(convertAccessionDataToFieldsValueArray(accession));
            }

        } catch (IOException e) {
            logger.error("There is problem with writing data to file", e);
            throw new RuntimeException(e);
        }
    }

    private static String[] convertAccessionDataToFieldsValueArray(AccessionData accessionData) {
        return new String[]{accessionData.getAccession(), accessionData.getOrganism(), accessionData.getGenomeAssembly(),
                accessionData.getDonorSex(), accessionData.getDonorAge(), accessionData.getDownloadURL()};
    }

}
