package dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.json.JsonJsonParser;
import org.springframework.boot.json.JsonParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
public class CrawlApplication {

    public static void main(String[] args) throws IOException, URISyntaxException {
        SpringApplication.run(CrawlApplication.class, args);

        List<JSONObject> articles = new ArrayList();


        String Link = "http://dergipark.gov.tr/api/public/oai/?verb=ListRecords";


        processLink(articles, Link);
        while (checkResumptionToken(Link)) {
            Link = updateLink(Link);
            processLink(articles, Link);
        }
        generateResults(articles, Link);


    }


    public static String updateLink(String Link) {
        String resumptionToken = getResumptionToken(Link);
        Link = Link + "&resumptionToken=" + resumptionToken;
        return Link;
    }

    public static boolean checkResumptionToken(String Link) {


        String resumptionToken = "";
        Document doc = new Document("");
        try {
            doc = Jsoup.connect(Link).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc.select("ListRecords").contains("resumptionToken")) {
            return true;
        } else {

            return false;
        }

    }

    public static String getResumptionToken(String Link) {


        String resumptionToken = "";
        Document doc = new Document("");
        try {
            doc = Jsoup.connect(Link).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc.select("ListRecords").contains("resumptionToken")) {
            return resumptionToken = doc.select("ListRecords").attr("resumptionToken").toString();

        } else {

            return "";
        }
    }


    private static void processLink(List<JSONObject> articles, String Link) throws URISyntaxException {

        Document doc = new Document("");


        try {
            doc = Jsoup.connect(Link).get();
            System.out.println("Connected to site - " + Link);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // JSON LD
        for (Element JSONLD : doc.select("script[type=application/ld+json]")) {
            JSONObject jsonObject = new JSONObject(JSONLD.data());

            Iterator<?> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (!(jsonObject.get(key) instanceof JSONObject)) {
                    System.out.println("JSON LD : " + key + ": " + jsonObject.get(key));
                }
            }

        }

        // For each link DOM element with RSS XML content
        for (Element identifier : doc.select("record")) {


            JSONObject article = new JSONObject();
            article.put("id", identifier);


        }

    }

    private static void generateResults(List<JSONObject> articles, String Link) throws IOException {

        System.out.println("\n\nWriting news feeds to feeds.json\n");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonJsonParser();

        Object je = jp.parseList(articles.toString());
        String prettyJsonString = gson.toJson(je);

        PrintWriter writer = new PrintWriter("articles.json", "UTF-8");
        writer.print(prettyJsonString);
        writer.close();


        System.out.println("Success! Processing complete.");
    }

}
