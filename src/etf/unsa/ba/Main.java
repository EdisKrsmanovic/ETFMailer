package etf.unsa.ba;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String username = null;
        String password = null;

        for(int i = 0; i < args.length; i ++) {
            if(args[i].equals("-user")) {
                username = args[++i];
            } else if(args[i].equals("-pass")) {
                password = args[++i];
            }
        }

        if(username == null || password == null) {
            Scanner in = new Scanner(System.in);
            System.out.println("Unesite username: ");
            username = in.nextLine();
            System.out.println("Unesite password: ");
            password = in.nextLine();
        }

        System.out.println("Provjera Zamger-a započeta. Ukoliko želite dodatnu provjeru C2 kurseva, pogledajte dokumentaciju");

        Program program = new Program();
        program.start(username, password);
    }
}
