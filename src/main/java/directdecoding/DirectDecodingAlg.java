package directdecoding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DirectDecodingAlg {

    private static final String MESSAGE_DATA_FILE = "informacija.txt";

    public void execute() {
        boolean dirbameSuTekstu; //kintamasis, skirtas zinoti ar dirbsime su tekstu, ar su bitais

        List<Integer> sarasasNuskaitytuDuomenu = new ArrayList<>();

        //nuskaitome is tekstinio dokumento ir sudedame i sarasa. Funkcija grazina boolean'a, pasakanti, ar nuskaite kaip teksta, ar kaip bitus.
        // Nusiusti reikia sarasa, i kuri bus irasyti nuskaityti ir (ar paverstas i bitus tekstas)
        // Kadangi List yra objektas, jo nebereikia grazinti is funkcijos, nes visi pakeitimai funkcijoje atsispindi ir cia
        //, del to is funkcijos tiesiogiai graziname tik boolean, kuris pasakys veliau, ar dirbome su tekstu, ar su bitais
        dirbameSuTekstu = nuskaitytiIsDokumento(sarasasNuskaitytuDuomenu);

        //siunciame musu nuskaityta zinute i kodavimo algoritma, grazina uzkoduota zinute
        int[] uzkoduotaZinute = uzkoduotiZinute(sarasasNuskaitytuDuomenu);
        // System.out.println("uzkoduota zinute " + Arrays.toString(uzkoduotaZinute));

        int[] neuzkoduotaZinute = new int[sarasasNuskaitytuDuomenu.size()];
        for (int i = 0; i < sarasasNuskaitytuDuomenu.size(); i++) {
            neuzkoduotaZinute[i] = sarasasNuskaitytuDuomenu.get(i);
        }

        Scanner userInput = new Scanner(System.in);  // Create a Scanner object
        System.out.println("iveskite skaiciu nuo 0 iki 1  0.00001 tikslumu. Kuo skaicius didesnis," +
                " tuo didesne bitu iskraipymo tikimybe,  (ivedus 0 bitai nebus siunciami kanalu ir iskraipomi)");

        //padauginam tikimybe is 100000, kadangi musu atsitiktinis skaicius yra tarp 0 ir 100000
        float tikimybe = Float.parseFloat(userInput.nextLine());

        //siunciame uzkoduota zinute iskraipymui, funkcija pakeicia ta uzkoduota zinute ir is esmes grazina (be return'o)
        System.out.println("siunciame UZKODUOTA vektoriu i kanala:");
        siustIKanala(uzkoduotaZinute, tikimybe);
        apverstiBitusRanka(uzkoduotaZinute);

        System.out.println("siunciame NEUZKODUOTA vektoriu i kanala:");
        siustIKanala(neuzkoduotaZinute, tikimybe);
        apverstiBitusRanka(neuzkoduotaZinute);
        //System.out.println("uzkoduota ir sugadinta zinute " + Arrays.toString(uzkoduotaZinute));

        //Siunciame uzkoduota ir apgadinta kanale zinute i dekodavimo algoritma. Grazina dekoduota zinute.
        int[] atkoduotaZinute = atkoduotiZinute(uzkoduotaZinute);

        //jei pradzioje nuskaiteme teksta, spausdinsime ir irasysime i rezultata tik teksta, jeigu nuskaiteme skaicius - tik skaicius spausdinsime ir israsysime i rezultatu dokumenta
        if (dirbameSuTekstu) {
            String atkoduotaAtverstaIsBituZinute = isBituITeksta(atkoduotaZinute);
            System.out.println("gauta zinute is bitu SU kodavimu - " + atkoduotaAtverstaIsBituZinute);
            irasytiRezultatus(atkoduotaZinute, atkoduotaAtverstaIsBituZinute);

            String nekoduotaAtverstaIsBituZinute = isBituITeksta(neuzkoduotaZinute);
            System.out.println("gauta zinute is bitu BE kodavimo - " + nekoduotaAtverstaIsBituZinute);

        } else {
            System.out.println("musu gauta zinute SE kodavimu - " + Arrays.toString(atkoduotaZinute));
            irasytiRezultatus(atkoduotaZinute, "");

            System.out.println("musu gauta zinute BE kodavimo - " + Arrays.toString(neuzkoduotaZinute));
            irasytiRezultatus(neuzkoduotaZinute, "");
        }
    }

    private void apverstiBitusRanka(int[] uzkoduotaZinute) {
        Scanner userInput = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Jeigu norite, papildomai iveskite poziciju skaicius, kuriose norite, kad iskraipytu bitus. Poziciju yra nuo 0 iki " + uzkoduotaZinute.length
                + ". Ivedimo pavyzdys - 0 15 21. Jeigu pakeitimu daryti nenorite, parasykite 'stop' be kabuciu");
        String ivestaReiksme = userInput.nextLine();
        if (ivestaReiksme.equals("stop")) {
            System.out.println("pakeitimai nebus daromi");
            return;
        }
        //suskaidom ivestus skaicius
        String[] ivestuReiksmiuMasyvas = ivestaReiksme.trim().split("\\s+");
        int pozicija;

        for (String ivestuReiksmiuMasyva : ivestuReiksmiuMasyvas) {
            pozicija = Integer.parseInt(ivestuReiksmiuMasyva);
            uzkoduotaZinute[pozicija] = (uzkoduotaZinute[pozicija] + 1) % 2;
            System.out.println("pakeistas simbolis " + pozicija + " vietoje is " + (uzkoduotaZinute[pozicija] + 1) % 2 + " i " + uzkoduotaZinute[pozicija]);
        }
    }

    //dekodavimo funkcija
    private int[] atkoduotiZinute(int[] uzkoduotaZinute) {
        //nors realiai celiu yra tik po 6. taciau as susikuriau viena isivaizduojama, kad sekciau paskutini isejusi bita
        int[] celesVirsuj = new int[]{0, 0, 0, 0, 0, 0, 0};
        int[] celesApacioj = new int[]{0, 0, 0, 0, 0, 0, 0};
        int[] atkoduotaZinute = new int[uzkoduotaZinute.length / 2 - 6];
        int atmintiesKeitimas = 0;

        //dekodavimo algoritmas. Is esmes skirstosi i keturias dalis dalis.
        for (int i = 0; i < atkoduotaZinute.length; i++) {
            //1 dalis - sudeda reiksmes i virsutines celes
            for (int j = 0; j <= 6; j++) {
                if (j < 6) celesVirsuj[6 - j] = celesVirsuj[5 - j];
                else {
                    if (uzkoduotaZinute[i * 2] == 0) {
                        celesVirsuj[0] = 0;
                    } else {
                        celesVirsuj[0] = 1;
                    }
                }
            }
            //2 dalis - sudeda reiksmes i apatines celes.
            for (int j = 0; j <= 6; j++) {
                if (j < 6) celesApacioj[6 - j] = celesApacioj[5 - j];
                else
                    celesApacioj[0] = (celesVirsuj[0] + celesVirsuj[2] + celesVirsuj[5] + celesVirsuj[6] + uzkoduotaZinute[i * 2 + 1]) % 2;
            }

            //kadangi pirmu simboliu (nuliu) neuzrasome, patikriname, ar tai pirmi simboliai, ir jei ne, uzrasome isejusi bita
            if (i > 5) atkoduotaZinute[i - 6] = celesVirsuj[6];
            {
                //3 dalis - jeigu is apacios atejo vienetukas, pakoreguojame isejusi bita BET DAR NEZYMIME atmintiesKeitimas = 1. nes pradzioje dar turesime patikrinti ar is praeitos iteracijos jis nebuvo lygus vienetui
                if ((celesApacioj[0] + celesApacioj[1] + celesApacioj[4] + celesApacioj[6]) > 2) {
                    atkoduotaZinute[i - 6] = (1 + celesVirsuj[6]) % 2;
                }
            }
            //4 dalis - jeigu is apacios praeitoje iteracijoje isejo vienetukas, koreguojame apatiniu celiu reiksmes. Tai darome tik po to, kai i MDE pasiunteme reiksmes, nes siai iteracijai praeitos iteracijos atmintis itakos neturi daryti
            if (atmintiesKeitimas == 1) {
                celesApacioj[0] = (celesApacioj[0] + 1) % 2;
                celesApacioj[1] = (celesApacioj[1] + 1) % 2;
                celesApacioj[4] = (celesApacioj[4] + 1) % 2;
                atmintiesKeitimas = 0;
            }
            //tik dabar galime pakeisti atmintiesKeitimo bit'a
            if ((celesApacioj[0] + celesApacioj[1] + celesApacioj[4] + celesApacioj[6]) > 2) {
                atmintiesKeitimas = 1;
            }
        }

        return atkoduotaZinute;
    }

    //kodavimo funkcija
    private int[] uzkoduotiZinute(List<Integer> list) {
        int[] celes = new int[]{0, 0, 0, 0, 0, 0, 0};

        //naujausias atejes bitas, su kuriuo dabartineje iteracijoje dirbam
        int naujasBitas;
        int[] uzkoduotaZinute = new int[(list.size() * 2) + 12];

        //kodavimo algoritmas. prie list.size() pridedame 6, kadangi dar prisideda 6 nuliai, norint pabaigoje nunulinti celiu reiksmes
        for (int i = 0; i < list.size() + 6; i++) {
            //nuskaitome skaiciu ir tikrinam, ar dar nesibaige bitai. Jeigu baigesi, pradedame pildyti nuliukais
            if (i < list.size()) naujasBitas = list.get(i);
            else naujasBitas = 0;

            //perstumiame celiu reiksmes, o i pirmaja cele idedame dabartini bita
            for (int j = 0; j <= 6; j++) {
                if (j < 6) celes[6 - j] = celes[6 - j - 1];
                else celes[0] = naujasBitas;
            }

            //uzrasome isejusius bitus is virsaus ir apacios
            uzkoduotaZinute[i * 2] = naujasBitas;
            uzkoduotaZinute[i * 2 + 1] = (celes[0] + celes[2] + celes[5] + celes[6]) % 2;
        }
        //System.out.println(uzkoduotaZinute);
        return uzkoduotaZinute;
    }

    //kanalo, tai yra bitu apvertimo algoritmas
    private void siustIKanala(int[] data, float tikimybe) {
        tikimybe *= 100000;

        if (tikimybe == 0) {
            System.out.println("Zinute nebus siunciama per kanala");
            return;
        }

        int klaiduSkaicius = 0;
        List<Integer> klaiduPozicijos = new ArrayList<>();
        Random rand = new Random();
        int virsutinisRezis = 100000;

        //jeigu atsitiktinis skaicius yra didesnis uz musu tikimybes skaiciu - apverciame bita. Taip iteruojame per visus bitus
        for (int i = 0; i < data.length; i++) {

            if (rand.nextInt(virsutinisRezis) < tikimybe) {
                klaiduPozicijos.add(i);
                klaiduSkaicius++;
                data[i] = (data[i] + 1) % 2;
            }
        }

        System.out.println("ivyko " + klaiduSkaicius + " klaidu siunciant kanalu. Klaidos ivyko siose pozicijose - " + klaiduPozicijos);
    }

    //nuskaitymo is dokumento algoritmas
    private boolean nuskaitytiIsDokumento(List<Integer> list) {
        boolean dirbameSuTekstu = false;

        try {
            File myObj = new File(MESSAGE_DATA_FILE);
            Scanner myReader = new Scanner(myObj);
            //nuskaitome simbolius ir juos sudedame i sarasa.
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //jeigu pirmasis nuskaitytas simbolis nera 1 arba 0, darome isvada, kad dirbame su tekstu ir papildomai kviesime funkcija, paversiancia teksta i bitus
                if (data.charAt(0) == '1' || data.charAt(0) == '0' && !dirbameSuTekstu) {
                    for (int i = 0; i < data.length(); i++) {
                        list.add(Character.getNumericValue(data.charAt(i)));
                    }
                } else {
                    dirbameSuTekstu = true;
                    paverstiTekstaIBitus(data, list);
                }
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred. File is not found");
            e.printStackTrace();
        }
        return dirbameSuTekstu;
    }

    //teksto vertimo i bitus algoritmas
    private void paverstiTekstaIBitus(String data, List<Integer> list) {
        //System.out.println("verciame teksta i bitus - " + data);
        byte[] bytes = data.getBytes();
        for (byte b : bytes) {
            int val = b;
            for (int i = 0; i < 8; i++) {
                list.add((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }
        System.out.println("'" + data + "' to binary: " + list);
    }

    //irasome rezultatus i tekstini dokumenta.
    private void irasytiRezultatus(int[] skaitiniaiDuomenys, String tekstiniaiDuomenys) {
        try {
            FileWriter myWriter = new FileWriter("rezultatai.txt");
            //jeigu dirbome su tekstu, papildomai isspausdiname pradini teksta
            for (int skaitiniaiDuomeny : skaitiniaiDuomenys) {
                myWriter.write(skaitiniaiDuomeny + "");
            }
            //jeigu atsiunteme tuscia string'a, nieko neirasysime
            myWriter.write('\n' + tekstiniaiDuomenys);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    //algoritmas, verciantis dekoduota zinute is bitu i teksta
    private String isBituITeksta(int[] informacija) {
        StringBuilder atkoduotaZinuteString = new StringBuilder();

        //svarbu. Pradedame cikla tik kai i = 6, nes pirmos sesios reiksmes yra papildomai irasyti nuliukai
        for (int j : informacija) {
            atkoduotaZinuteString.append(j);
        }
        StringBuilder pradineZinute = new StringBuilder();
        for (int i = 0; i < atkoduotaZinuteString.length(); i += 8) {
            String temp = atkoduotaZinuteString.substring(i, i + 8);
            int num = Integer.parseInt(temp, 2);
            char letter = (char) num;
            pradineZinute.append(letter);
        }
        //System.out.print("musu gautas tekstas - " + pradineZinute);
        return pradineZinute.toString();
    }
}
