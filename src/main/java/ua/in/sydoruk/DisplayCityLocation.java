package ua.in.sydoruk;

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

class DisplayCityLocation {
    private final static Logger logger = Logger.getLogger(DisplayCityLocation.class);
    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=";
    private Set<String> setOfCities = new LinkedHashSet<>();
    private Map<String, String> mapLocationOfCities = new LinkedHashMap<>();

    private void writeCitiesFromFileToSet() {
        System.out.print("Do you want to load the default file (Y/N)? ");
        String yes = readFromInputStream();
        if ("Y".equals(yes) || "y".equals(yes)) {
            InputStream in = getClass().getClassLoader().getResourceAsStream("city.txt");
            fillSet(in);
        } else {
            System.out.println("Type file's name");
            String fileName = null;
            try {
                fileName = readFromInputStream();
                InputStream in = new FileInputStream(fileName);
                fillSet(in);
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find this file: " + fileName);
                writeCitiesFromFileToSet();
            }
        }
    }

    private void fillSet(InputStream inputStream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            br.lines().forEach(s -> {
                if (s != null && !s.isEmpty()) {
                    setOfCities.add(s);
                }
            });
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private String readFromInputStream() {
        Scanner scanner = new Scanner(System.in, "utf-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.nextLine() : "";
    }

    private String getLatLng(String cityName) {
        StringBuilder latLng = new StringBuilder();
        try (InputStream in = new URL(GEOCODE_URL + URLEncoder.encode(cityName, "UTF-8")).openConnection().getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            StringBuilder builder = new StringBuilder();
            reader.lines().forEachOrdered(builder::append);

            JSONObject json = new JSONObject(builder.toString());
            if ("OK".equals(json.getString("status"))) {
                latLng.append("lat: ");
                latLng.append(json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
                        .getJSONObject("location").getString("lat"));
                latLng.append(" lng: ");
                latLng.append(json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
                        .getJSONObject("location").getString("lng"));
            } else {
                latLng.append("Remote call to get location of the city failed: ").append(json.getString("status"));
            }
        } catch (MalformedURLException e) {
            logger.error("URL to Google Maps web service is malformed.", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding for URL failed.", e);
        } catch (IOException e) {
            logger.error("Unexpected IO failure to Google Maps web service.", e);
        } catch (JSONException e) {
            logger.error("Unexpected parsing error handling JSON from Google Maps web service", e);
        }
        return latLng.toString();
    }

    void displayCityLocation() {

        writeCitiesFromFileToSet();
        System.out.println("Wait....");
        setOfCities.stream().forEachOrdered(city -> {
            String location = getLatLng(city);
            mapLocationOfCities.put(city, location);
        });
        mapLocationOfCities.forEach((k, v) -> System.out.println(k + " " + v));

    }

}
