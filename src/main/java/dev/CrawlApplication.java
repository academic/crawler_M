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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
public class CrawlApplication {

    public static void main(String[] args) throws IOException, URISyntaxException {
        SpringApplication.run(CrawlApplication.class, args);

        List<JSONObject> articles = new ArrayList<>();


        String domain = "http://dergipark.gov.tr/api/public/oai/?verb=ListRecords";


        while (getResumptionToken(domain) != ""){
            processDomain(articles,domain);
            String resumptionToken = getResumptionToken(domain);
            domain = domain + "&resumptionToken="  + resumptionToken;
            generateResults(articles, domain);
        }


    }



    public static String getResumptionToken(String domain) {


        String resumptionToken = "";
        Document doc = new Document("");
        try {
            doc = Jsoup.connect(domain).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc.select("ListRecords").contains("resumptionToken")) {
            return resumptionToken = doc.select("ListRecords").attr("resumptionToken").toString();

        }else {

            return "";
        }
    }


    private static void processDomain(List<JSONObject> articles, String domain) throws URISyntaxException {
        Document doc = new Document("");


        try {
            doc = Jsoup.connect(domain).get();
            System.out.println("Connected to site - "+ domain);
        } catch (Exception e) {
            return System.out.println("error");
        }

        for (Element record : doc.select("record")) {


          if (!record.attr("identifier").isEmpty) {
              System.out.println(record.attr("identifier"));
          }

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
        for (Element link : doc.select("link[type=application/rss+xml]")) {



            JSONObject feed = new JSONObject();
            feed.put("categoryId", feedCategoryId);
            feed.put("description", feedDescription);
            feed.put("id", link.attr("title").toLowerCase().replaceAll("[+.^:,|»]", "").replaceAll(" ", "-").replaceAll("--", ""));

            if (feedIcon.contains("http")) {
                feed.put("image", feedIcon);
            } else if (feedIcon.length() > 0) {
                URI uri = new URI(site);
                String domain = uri.getHost();
                feed.put("image", domain + feedIcon);
            }


            try {
                HttpURLConnection.setFollowRedirects(true);
                // note : you may also need
                //        HttpURLConnection.setInstanceFollowRedirects(false)
                HttpURLConnection con =
                        (HttpURLConnection) new URL(feed.get("image").toString()).openConnection();
                con.setRequestMethod("HEAD");
                con.connect();

                if(con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    feed.put("image", "http://www.crownmountainmovers.com/wp-content/uploads/2015/10/arrow-right1.png");
                }
            }
            catch (Exception e) {
                feed.put("image", "http://www.crownmountainmovers.com/wp-content/uploads/2015/10/arrow-right1.png");
            }

            if (link.attr("title").contains("rss") || link.attr("title").contains("RSS")) {
                Elements title = doc.select("title");
                feed.put("title", title.text());
            } else if (link.attr("title").length() > 0) {
                feed.put("title", link.attr("title"));
            } else {
                feed.put("title", "");
            }


            if (link.attr("href").contains("http")) {
                feed.put("url", link.attr("href"));
            } else if (link.attr("href").length() > 0) {
                feed.put("url", site + link.attr("href"));
            }


            Iterator<?> keys = feed.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                if ((feed.get(key) == null || feed.get(key) == "") && !(key.equals("description"))) {
                    dataIncomplete = true;
                }
            }

            if (dataIncomplete) {
                sitesInCompleteData.add(feed);
            } else {
                feeds.add(feed);
            }

            System.out.println("RSS Feed: " + link.attr("title") + ", Content: " + link.attr("href"));
        }
    }
  }

    private static void generateResults(List<JSONObject> articles,  String domain) throws IOException {

        System.out.println("\n\nWriting news feeds to feeds.json\n");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonJsonParser();

        Object je = jp.parseList(feeds.toString());
        String prettyJsonString = gson.toJson(je);

        PrintWriter writer = new PrintWriter("feeds.json", "UTF-8");
        writer.print(prettyJsonString);
        writer.close();


        System.out.println("Writing incomplete data to incomplete.json");
        je = jp.parseList(sitesInCompleteData.toString());
        prettyJsonString = gson.toJson(je);


        writer = new PrintWriter("incomplete.json", "UTF-8");
        writer.print(prettyJsonString);
        writer.close();

        //Close the input stream
        br.close();

        System.out.println("Success! Processing complete.");
    }
