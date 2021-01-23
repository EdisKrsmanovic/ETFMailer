package etf.unsa.ba;

public class Main {

    public static void main(String[] args) {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,content-length");
//        String username = null;
//        String password = null;
//
//        for(int i = 0; i < args.length; i ++) {
//            if(args[i].equals("-user")) {
//                username = args[++i];
//            } else if(args[i].equals("-pass")) {
//                password = args[++i];
//            }
//        }
//
//        if(username == null || password == null) {
//            System.out.println("Neispravno pokrenut jar, pokrenite ga kao 'java -jar imeJara.jar -user vasUsername -pass vasPassword'");
//        } else {
//            System.out.println("Provjera Zamger-a započeta. Ukoliko želite dodatnu provjeru C2 kurseva, pogledajte dokumentaciju");
//            Program program = new Program();
//            program.start(username, password);
//        }

        Program program = new Program();
        program.start("PHPSESSID=iggcd5j3cpfulgacrngno401pm");
    }
}
