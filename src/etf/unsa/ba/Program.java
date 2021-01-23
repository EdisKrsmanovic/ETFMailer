package etf.unsa.ba;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Program {
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private Map<String, Integer> duzinePredmeta = new HashMap<>();
    private Map<String, String> naziviPredmeta = new HashMap<>();
    private String cookie;

//TODO: Napraviti prijavu na zamger sa username i password-om, tj. dobaviti cookie pomoću tih informacija te zamijeniti potrebu traženja i unošenja cookie-a ručno
    public void start(String cookie) {
        this.cookie = cookie;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://zamger.etf.unsa.ba/hybrid/index.php"))
                    .setHeader("cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            Pattern regexZaPredmet = Pattern.compile("<td><a href=\"(.*)");
            Matcher m = regexZaPredmet.matcher(body);
            List<String> linkoviPredmeta = new ArrayList<>();
            while (m.find()) {
                String linkPredmeta = "https://zamger.etf.unsa.ba/hybrid/index.php" + m.group(1).split("\"")[0];
                String nazivPredmeta = m.group(1).split(">")[1].split("<")[0];
                linkoviPredmeta.add(linkPredmeta);
                this.naziviPredmeta.put(linkPredmeta, nazivPredmeta);
            }
            while (true) {
                provjeriPredmete(linkoviPredmeta);
                Thread.sleep(10000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void provjeriPredmete(List<String> linkoviPredmeta) {
        linkoviPredmeta.forEach(this::provjeriPredmet);
    }

    private void provjeriPredmet(String link) {
        try {
            Integer novaDuzinaSadrzaja = dajDuzinuSadrzajaNaLinku(link);
            Integer staraDuzinaSadrzaja = duzinePredmeta.get(link);

            if (staraDuzinaSadrzaja == null) {
                duzinePredmeta.put(link, novaDuzinaSadrzaja);
            } else if (!staraDuzinaSadrzaja.equals(novaDuzinaSadrzaja)) {
                posaljiObavijestZaPredmet(link);
            }

            Thread.sleep(5000);
        } catch (Exception ignored) {
        }
    }


    private Integer dajDuzinuSadrzajaNaLinku(String link) throws IOException, InterruptedException {
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(link))
                .setHeader("cookie", this.cookie)
                .build();
        return httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString()).body().length();
    }

    private void posaljiObavijestZaPredmet(String link) {
        String naslovEmaila = "[" + this.naziviPredmeta.get(link) + "] Zamger promijenjen";
        String porukaEmaila = "Desila se izmjena na stranici predmeta. Link: " + link;
    }
//    private void prijaviSeNaZamger(String username, String password) throws IOException, InterruptedException {
//        ////      ZAMGER LOGIN
//        HttpRequest loginRequest = HttpRequest.newBuilder()
//                .GET()
//                .uri(URI.create("https://zamger.etf.unsa.ba/"))
//                .build();
//        HttpResponse<String> response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
//
//        String authUrl = response.headers().firstValue("location").get();
//
//        loginRequest = HttpRequest.newBuilder()
//                .GET()
//                .uri(URI.create(authUrl))
//                .setHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
//                .setHeader("accept-encoding", "gzip, deflate, br")
//                .setHeader("accept-language", "en-US,en;q=0.9")
//                .setHeader("sec-fetch-dest", "document")
//                .setHeader("sec-fetch-mode", "navigate")
//                .setHeader("sec-fetch-site", "none")
//                .setHeader("sec-fetch-user", "?1")
//                .setHeader("upgrade-insecure-requests", "1")
//                .setHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")
//                .build();
//        response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
//
//        List<String> cookieHeaderi =response.headers().allValues("set-cookie");
//        String privremeniCookieHeader = cookieHeaderi.get(0).split(";")[0] + ";" + cookieHeaderi.get(1).split(";")[0];
//
//        Thread.sleep(2000);
//        String body = "loginforma=1&login=" + username + "&pass=" + password + "&credentialId=";
//        loginRequest = HttpRequest.newBuilder()
//                .POST(HttpRequest.BodyPublishers.ofString(body))
//                .uri(URI.create(authUrl))
//                .setHeader("authority", "sso.etf.unsa.ba")
//                .setHeader("method", "POST")
//                .setHeader("path", authUrl.substring(24))
//                .setHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
//                .setHeader("accept-encoding", "gzip, deflate, br")
//                .setHeader("accept-language", "en-US,en;q=0.9")
//                .setHeader("cache-control", "max-age=0")
//                .setHeader("set-fetch-dest", "document")
//                .setHeader("set-fetch-mode", "navigate")
//                .setHeader("set-fetch-site", "same-origin")
//                .setHeader("set-fetch-user", "?1")
//                .setHeader("upgrade-insecure-requests", "1")
//                .setHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")
//                .setHeader("content-type", "application/x-www-form-urlencoded")
//                .setHeader("cookie", privremeniCookieHeader)
//                .setHeader("origin", "https://sso.etf.unsa.ba")
//                .setHeader("referer", authUrl)
//                .setHeader("content-length", String.valueOf(body.length()))
//                .build();
//        response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
//        System.out.println(response);
////        this.cookie = response.headers().allValues("set-cookie").get(0).substring(0,36);
////
////        loginRequest = HttpRequest.newBuilder()
////                .POST(HttpRequest.BodyPublishers.ofString("loginforma=1&login=" + username + "&pass=" + password))
////                .setHeader("Content-Type", "application/x-www-form-urlencoded")
////               .setHeader("Cookie", this.cookie)
////                .uri(URI.create(response.headers().firstValue("location").get()))
////                .build();
////        response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
//    }
}
