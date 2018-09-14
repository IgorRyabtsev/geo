import model.AccessionData;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import utils.Pair;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class DataFetcher {

    private static final Logger logger = LogManager.getLogger(DataFetcher.class);

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
    private final LinkedBlockingQueue<Pair<Integer, String>> accessions;
    private final LinkedBlockingQueue<Pair<Integer, AccessionData>> accessionsData;
    private int threadCount;
    private boolean saveInputOrder;

    public DataFetcher(int threadCount, boolean saveInputOrder) {
        samplesIds = new ArrayList<>();
        accessions = new LinkedBlockingQueue<>();
        accessionsData = new LinkedBlockingQueue<>();
        this.threadCount = threadCount;
        this.saveInputOrder = saveInputOrder;
    }


    public List<AccessionData> fetchData() {

        fetchSamples();

        // set up executor service for getting GSMxxx-s
        ExecutorService executorGSMs = Executors.newFixedThreadPool(threadCount);
        Set<Callable<Void>> callables = createCallablesForFetchingGSMs();
        try {
            executorGSMs.invokeAll(callables);
        } catch (InterruptedException e) {
            logger.error("Problem with fetching GSMs", e);
            throw new RuntimeException(e);
        }
        executorGSMs.shutdownNow();

        // set up executor service for fetching metadata
        ExecutorService executorMetadata = Executors.newFixedThreadPool(threadCount);
        callables = createCallablesForFetchingMetadata();
        try {
            executorMetadata.invokeAll(callables);
        } catch (InterruptedException e) {
            logger.error("Problem with fetching metadata", e);
            throw new RuntimeException(e);
        }
        executorMetadata.shutdownNow();


        List<Pair<Integer, AccessionData>> pairsWithAccessions = new ArrayList<>(accessionsData);

        if (saveInputOrder) {
            pairsWithAccessions.sort(Comparator.comparing(Pair::getFirst));
        }
        return pairsWithAccessions
                .stream()
                .map(Pair::getSecond)
                .collect(Collectors.toList());

    }

    private Set<Callable<Void>> createCallablesForFetchingGSMs() {
        List<Pair<Integer, Integer>> indexes = new ArrayList<>();
        StringBuilder ids = new StringBuilder();
        int lastIndex = 0;
        for (int i = 0; i < samplesIds.size(); ++i) {
            if (ids.length() + ACCESIONS_URL.length() + samplesIds.get(i).length() > MAX_URL_SIZE) {
                indexes.add(new Pair<>(lastIndex, i - 1));
                lastIndex = i;
                ids = new StringBuilder(samplesIds.get(i) + ",");
                continue;
            }
            ids.append(samplesIds.get(i)).append(",");
        }
        indexes.add(new Pair<>(lastIndex, samplesIds.size() - 1));

        Set<Callable<Void>> callables = new HashSet<>();
        for (Pair<Integer, Integer> index : indexes) {
            int startIndex = index.getFirst();
            int endIndex = index.getSecond();
            callables.add(() -> {
                fetchGSMs(startIndex, endIndex);
                return null;
            });
        }
        return callables;
    }

    private Set<Callable<Void>> createCallablesForFetchingMetadata() {
        Set<Callable<Void>> callables = new HashSet<>();
        for (Pair<Integer, String> accession : accessions) {
            callables.add(() -> {
                fetchMetadata(accession.getFirst(), accession.getSecond());
                return null;
            });
        }
        return callables;
    }


    private void fetchGSMs(int firstIndex, int lastIndex) {
        StringBuilder ids = new StringBuilder();
        StringBuilder response = new StringBuilder();
        try {
            for (int i = firstIndex; i <= lastIndex; ++i) {
                ids.append(samplesIds.get(i));
                if (i != lastIndex) {
                    ids.append(",");
                }
            }
            response.append(getResponse(ACCESIONS_URL + ids));
        } catch (IOException e) {
            logger.error("Problem with fetching GSMxxx", e);
            throw new RuntimeException(e);
        }

        // extract GSMxxx-s from response
        int index = response.indexOf(SEARCH_PATTERN);
        int indexForSorting = firstIndex;
        while (index >= 0) {
            int counter = 0;
            while (Character.isDigit(response.charAt(index + SEARCH_PATTERN.length() + counter))
                    || Character.isLetter(response.charAt(index + SEARCH_PATTERN.length() + counter)))
                ++counter;
            accessions.add(new Pair<>(indexForSorting, response.substring(index + SEARCH_PATTERN.length(),
                    index + SEARCH_PATTERN.length() + counter)));
            ++indexForSorting;
            index = response.indexOf(SEARCH_PATTERN, index + 1);
        }
    }


    private void fetchMetadata(Integer indexInInputData, String accession) {
        try {
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

            // extract metadata from response
            String donorSex = ABSENT_VALUE;
            String donorAge = ABSENT_VALUE;
            NodeList characteristics = doc.getElementsByTagName(CHARACTERISTICS_TAG);
            for (int i = 0; i < characteristics.getLength(); ++i) {
                Node node = characteristics.item(i);
                switch (node.getAttributes().item(0).getTextContent().trim().toLowerCase()) {
                    case "sex":
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

            accessionsData.add(new Pair<>(indexInInputData,
                    new AccessionData(accession, organism, assemblyValue, donorSex, donorAge, downloadURL)));

        } catch (Exception e) {
            logger.error("Problem with fetching metadata", e);
            throw new RuntimeException(e);
        }
    }

    private void fetchSamples() {
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
            logger.error("Problem with fetching samples", e);
            throw new RuntimeException(e);
        }
    }

    private StringBuffer getResponse(String url) throws IOException {
        BufferedReader in = null;
        StringBuffer response;
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
            logger.error("Problem with response or connection to" + url, e);
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return response;
    }

}
