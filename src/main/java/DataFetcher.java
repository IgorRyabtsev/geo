import model.AccessionData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataFetcher {

    private final static int MAX_URL_SIZE = 2083;
    private final static String SEARCH_PATTERN = "Accession: ";
    private static final String LINE_FEED = "\n";
    private static final String ORGANISM_TAG = "Organism";
    private static final String CHARACTERISTICS_TAG = "Characteristics";
    private static final String SUPPLEMENTARY_DATA_TAG = "Supplementary-Data";
    private static final String ABSENT_VALUE = "-";
    private static final String ID_LIST_TAG = "IdList";
    private static final String ID_TAG = "Id";

    private final static String SAMPLES_URL = "http://bit.ly/geo_query_example";
    private final static String ACCESIONS_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi" +
            "?db=gds&rettype=xml&id=";
    private final static String ACCESSION_METADATA_URL = "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=%s" +
            "&targ=self&form=xml&view=quick";

    private final List<String> samplesIds;
    private final List<String> accessions;
    private final List<AccessionData> accessionsData;

    public DataFetcher() {
        samplesIds = new ArrayList<>();
        accessions = new ArrayList<>();
        accessionsData = new ArrayList<>();
    }

    public List<AccessionData> fetchData() {
        getSamples();
        getGSMs();
        getMetadata();
        return accessionsData;

    }

    private void getMetadata() {
        try {
            for (String accession : accessions) {
                StringBuffer response = getResponse(String.format(ACCESSION_METADATA_URL, accession));
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(response.toString())));
                String organism = doc.getElementsByTagName(ORGANISM_TAG).item(0).getTextContent();

                String assemblySearchPattern = "assembly: ";
                String assemblyValue = ABSENT_VALUE;
                int index = response.indexOf(assemblySearchPattern);
                if (index != -1) {
                    int counter = 0;
                    while (Character.isDigit(response.charAt(index + assemblySearchPattern.length() + counter))
                            || Character.isLetter(response.charAt(index + assemblySearchPattern.length() + counter)))
                        ++counter;
                    assemblyValue = response.substring(index + assemblySearchPattern.length(),
                            index + assemblySearchPattern.length() + counter);
                }

                String donorSex = ABSENT_VALUE;
                String donorAge = ABSENT_VALUE;
                NodeList characteristics = doc.getElementsByTagName(CHARACTERISTICS_TAG);
                for (int i = 0; i < characteristics.getLength(); ++i) {
                    Node node = characteristics.item(i);
                    switch (node.getAttributes().item(0).getTextContent().trim()) {
                        case "Sex":
                            donorSex = node.getTextContent().trim();
                            break;
                        case "age":
                            donorAge = node.getTextContent().trim();
                            break;
                        default:
                            break;
                    }
                }

                String downloadURL = ABSENT_VALUE;
                NodeList supplementaryData = doc.getElementsByTagName(SUPPLEMENTARY_DATA_TAG);
                for (int i = 0; i < supplementaryData.getLength(); ++i) {
                    Node node = supplementaryData.item(i);
                    if (node.getAttributes().item(0).getTextContent().trim().equalsIgnoreCase("bed")) {
                        downloadURL = node.getTextContent().trim();
                    }
                }

                accessionsData.add(new AccessionData(accession, organism, assemblyValue, donorSex, donorAge, downloadURL));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getGSMs() {
        StringBuilder ids = new StringBuilder();
        StringBuilder response = new StringBuilder();
        try {
            for (String samplesId : samplesIds) {
                if (ids.length() + ACCESIONS_URL.length() + samplesId.length() > MAX_URL_SIZE) {
                    response.append(getResponse(ACCESIONS_URL + ids));
                    ids = new StringBuilder(samplesId + ",");
                    continue;
                }
                ids.append(samplesId).append(",");
            }
            response.append(getResponse(ACCESIONS_URL + ids));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // extract GSMxxx-s from response
        int index = response.indexOf(SEARCH_PATTERN);
        while (index >= 0) {
            int counter = 0;
            while (Character.isDigit(response.charAt(index + SEARCH_PATTERN.length() + counter))
                    || Character.isLetter(response.charAt(index + SEARCH_PATTERN.length() + counter)))
                ++counter;
            accessions.add(response.substring(index + SEARCH_PATTERN.length(),
                    index + SEARCH_PATTERN.length() + counter));
            index = response.indexOf(SEARCH_PATTERN, index + 1);
        }
    }

    private void getSamples() {
        try {
            StringBuffer response = getResponse(SAMPLES_URL);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(response.toString())));
            NodeList errNodes = doc.getElementsByTagName(ID_LIST_TAG);
            if (errNodes.getLength() > 0) {
                for (int i = 0; i < errNodes.getLength(); ++i) {
                    Element err = (Element) errNodes.item(i);
                    for (int j = 0; j < err.getElementsByTagName(ID_TAG).getLength(); ++j) {
                        samplesIds.add(err.getElementsByTagName(ID_TAG).item(j).getTextContent());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private StringBuffer getResponse(String url) throws IOException {
        BufferedReader in = null;
        StringBuffer response = null;
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // check if there is redirect
            String redirect = con.getHeaderField("Location");
            if (redirect != null) {
                con = (HttpURLConnection) new URL(redirect).openConnection();
            }
            in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append(LINE_FEED);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return response;
    }

}
