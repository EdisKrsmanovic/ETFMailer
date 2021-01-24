package etf.unsa.ba;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Program {
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private Map<String, String> sadrzajPredmeta = new HashMap<>();
    private Map<String, String> naziviPredmeta = new HashMap<>();
    private String cookie;
    private String username;
    private String password;

    public void start(String username, String password) {
        this.username = username;
        this.password = password;
        prijaviSeNaZamger(username, password);
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
            System.out.println("Provjeravam sljedeće predmete: ");
            while (m.find()) {
                String linkPredmeta = "https://zamger.etf.unsa.ba/hybrid/index.php" + m.group(1).split("\"")[0];
                String nazivPredmeta = m.group(1).split(">")[1].split("<")[0];
                linkoviPredmeta.add(linkPredmeta);
                this.naziviPredmeta.put(linkPredmeta, nazivPredmeta);
                System.out.println(nazivPredmeta);
            }
            int i = 0;
            while (true) {
                System.out.println("Provjeravam " + i + ". put!");
                provjeriPredmete(linkoviPredmeta);
                Thread.sleep(10000);
                i++;
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
            System.out.println("Provjeravam " + naziviPredmeta.get(link));
            String noviSadrzaj = dajSadrzajNaLinku(link);
            String stariSadrzaj = sadrzajPredmeta.get(link);
            if (stariSadrzaj == null) {
                sadrzajPredmeta.put(link, noviSadrzaj);
            } else {
                double ukupnoNaZadacamaNovo = dajUkupnoNaZadacama(noviSadrzaj);
                double ukupnoNaZadacamaStaro = dajUkupnoNaZadacama(stariSadrzaj);
                if (ukupnoNaZadacamaNovo == 0 && ukupnoNaZadacamaStaro != 0) { //Specijalan slucaj kada Zamger vrati pogresno ucitanu stranicu (poremecena tabela za zadace)
                    System.out.println("Pogresno ucitana stranica predmeta, ignorisem. " + link);
                } else if (stariSadrzaj.length() != noviSadrzaj.length()) {
                    sadrzajPredmeta.put(link, noviSadrzaj);
                    zapisiSadrzaj(stariSadrzaj, "[" + naziviPredmeta.get(link) + "] old " + LocalDateTime.now().toString() + ".txt");
                    zapisiSadrzaj(noviSadrzaj, "[" + naziviPredmeta.get(link) + "] new " + LocalDateTime.now().toString() + ".txt");
                    posaljiObavijestZaPredmet(link);
                }
            }

            Thread.sleep(5000);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private void zapisiSadrzaj(String sadrzaj, String nazivFajla) {
        try {
            nazivFajla = nazivFajla.replace(":", "-");
            FileWriter writer = new FileWriter(nazivFajla);
            writer.write(sadrzaj);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    private String dajSadrzajNaLinku(String link) throws IOException, InterruptedException {
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(link))
                .setHeader("cookie", this.cookie)
                .build();
        HttpResponse<String> httpResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        String body = httpResponse.body();
        if (body.contains("Nepoznat predmet")) {
            return sadrzajPredmeta.get(link);
        } else if (httpResponse.statusCode() != 200) {
            prijaviSeNaZamger(this.username, this.password);
            return sadrzajPredmeta.get(link);
        } else return body;
    }

    private double dajUkupnoNaZadacama(String sadrzaj) {
        if (!sadrzaj.contains("id=\"zadace")) {
            return 0;
        } else {
            String tempSadrzaj = sadrzaj.replace("\t", "");
            tempSadrzaj = tempSadrzaj.replace("\n", "");
            Pattern regexPatern = Pattern.compile("UKUPNO: </td><td>.*</td><td>");
            Matcher m = regexPatern.matcher(tempSadrzaj);
            m.find();
            try {
                return Double.parseDouble(m.group(0).split("<td>")[1].split("</td>")[0]);
            } catch(Exception e) {
                return 0;
            }
        }
    }

    private void posaljiObavijestZaPredmet(String link) {
        String nazivPredmeta = this.naziviPredmeta.get(link);
        String naslovEmaila = "[" + nazivPredmeta + "] Zamger promijenjen";
        String porukaEmaila = "Desila se izmjena na stranici predmeta. Link: " + link;

        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        // Get a Properties object
        Properties props = System.getProperties();
        props.setProperty("mail.smtp.host", "smtp.gmail.com");
        props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "true");
        props.put("mail.store.protocol", "pop3");
        props.put("mail.transport.protocol", "smtp");
        final String username = this.username + "@etf.unsa.ba";
        final String password = this.password;
        props.put("mail.smtp.user", username);
        props.put("mail.smtp.password", password);
        try {
            Session session = Session.getDefaultInstance(props,
                    new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            Message msg = new MimeMessage(session);

            // -- Set the FROM and TO fields --
            msg.setFrom(new InternetAddress(this.username + "@etf.unsa.ba"));

            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(this.username + "@etf.unsa.ba", false));
            msg.setSubject(naslovEmaila);
            msg.setText(porukaEmaila);
            msg.setSentDate(new Date());
            Transport.send(msg);
            System.out.println("[" + nazivPredmeta + "] Email poslan za izmjenu koja se desila na predmetu.");
        } catch (MessagingException e) {
            System.out.println("Error, cause: " + e);
        }
    }

    private void prijaviSeNaZamger(String username, String password) {
        try {
            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://zamger.etf.unsa.ba/"))
                    .build();
            HttpResponse<String> response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

            this.cookie = response.headers().firstValue("set-cookie").get();
            String ssoUrl = response.headers().firstValue("location").get();

            loginRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(ssoUrl))
                    .build();
            response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());


            List<String> cookieHeaderi = response.headers().allValues("set-cookie");
            String privremeniCookieHeader = cookieHeaderi.get(0).split(";")[0] + ";" + cookieHeaderi.get(1).split(";")[0];

            String body = response.body();
            Pattern regexPatern = Pattern.compile("action=\"(.*?)\"");
            Matcher m = regexPatern.matcher(body);
            m.find();
            String authUrl = m.group(1).replace("amp;", "");


            String postBody = "username=" + username + "&password=" + password;
            loginRequest = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(postBody))
                    .uri(URI.create(authUrl))
                    .setHeader("cookie", privremeniCookieHeader)
                    .setHeader("content-type", "application/x-www-form-urlencoded")
                    .setHeader("accept", "*/*")
                    .setHeader("accept-encoding", "gzip, deflate, br")
                    .setHeader("origin", "https://sso.etf.unsa.ba")
                    .build();
            response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

            String zamgerAuthUrl = response.headers().firstValue("location").get();
            loginRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(zamgerAuthUrl))
                    .setHeader("cookie", this.cookie)
                    .build();
            httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
