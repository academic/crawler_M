package dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    public final static String Link = "http://dergipark.gov.tr/api/public/oai/?verb=ListRecords&resumptionToken=";
    public static String resumptionToken = "";


    public static void main(String[] args) throws IOException, URISyntaxException {
        SpringApplication.run(CrawlApplication.class, args);

        List<JSONObject> articles = new ArrayList();


        resumptionToken = getRecursiveConnection(Link+resumptionToken);
        while (!resumptionToken.isEmpty()){
            resumptionToken = getRecursiveConnection(Link+resumptionToken);
        }


//        processLink(articles, Link);
//        generateResults(articles, Link);


    }


    private static String getRecursiveConnection(String href) throws IOException {


        Document doc;
        doc = Jsoup.connect(href).get();
        Elements elements = doc.select("identifier");
        resumptionToken = doc.select("resumptionToken").first().ownText();


        for (Element element : elements) {
            System.out.println(element.ownText());

        }


        return resumptionToken;
    }


    private static void processLink(List<JSONObject> articles, String Link) throws URISyntaxException {

        Document doc = new Document("");



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
