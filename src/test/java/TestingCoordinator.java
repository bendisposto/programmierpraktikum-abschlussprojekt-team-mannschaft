package test.java;
import main.java.tddt.Coordinator;
import static org.junit.Assert.*;

import main.java.tddt.data.Log;
import main.java.tddt.gui.Controller;
import org.junit.*;

import java.time.LocalDateTime;

/**
 * Created by renato on 30.06.16.
 */
public class TestingCoordinator {
    //Klassencontentstrings zum Testen:
    //Testklassenstrings aus den Tests der Bibliothek entnommen
    String klassencontent = "public class Bar { \n"
            + " public static int square(int n) { \n"
            + "    return n * n; \n"
            + " }\n"
            + " public static int plusThree(int n) { \n"
            + "    return 3 + n; \n"
            + " }\n"
            + "}";
    String testcontent = "import static org.junit.Assert.*;\n"
            + "import org.junit.Test;\n"
            + "public class BarTest { \n"
            + "   @Test\n"
            + "   public void squareOf3_shouldReturn9() { \n "
            + "       assertEquals(9, Bar.square(3)); \n"
            + "   }\n "
            + "}";

    //Testklassenstrings aus der Compile Bibliothek
    String klassencontentfalsch = "public class Bar { \n"
            + " public static int square(int n) { \n"
            + "    return n * 12; \n"
            + " }\n"
            + " public static int plusThree(int n) { \n"
            + "    return 3 + n; \n"
            + " }\n"
            + "}";

    @Test
    public void vonphase1zu2Test(){ //testet, ob die phasen richtig gewechselt werden, hier von  ohase 1 zu 2
        Coordinator coor = new Coordinator("Bar", "BarTest");
        //für die Bedingung muss es fehlschlagende Tests geben
        LocalDateTime wer = coor.nextPhase(klassencontentfalsch, testcontent);
        assertEquals(2, coor.phase); //sollte danach dann 2 sein
    }

    @Test
    public void notcompilingInphase1(){
        Coordinator coor = new Coordinator("Bar", "BarTest");
        //für die Bedingung muss es fehlschlagende Tests geben
        String methodfortestnotdefined = "public class Bar{}";
        LocalDateTime qwe = coor.nextPhase(methodfortestnotdefined, testcontent);
        assertEquals(2, coor.phase); //sollte danach dann 2 sein
    }

    @Test
    public void vonphase2zu3Test(){ //testet ob bei erfolgreichen Tests in phase 2 zu phase 3 übergegangen wird
        Coordinator coortest = new Coordinator("Bar", "BarTest");
        LocalDateTime wer = coortest.nextPhase(klassencontentfalsch, testcontent);
        //phase sollte hiernach 2 sein
        wer = coortest.nextPhase(klassencontent, testcontent);
        assertEquals(3, coortest.phase); //phase sollte danach dann 3 sein
    }

    @Test
    public void Refactoringsuccess(){
        Coordinator coortest = new Coordinator("Bar", "BarTest");
        LocalDateTime ti = coortest.nextPhase(klassencontentfalsch, testcontent);
        //phase sollte hiernach 2 sein
        ti = coortest.nextPhase(klassencontent, testcontent);
        //phase sollte danach dann 3 sein
        //hierbei refactoring ohne aenderungen betrachtet, da dies keinen Unterschied macht, diese Phase soll ja frei gewählt werden können
        ti = coortest.nextPhase(klassencontent, testcontent);
        //phase sollte hiernach wieder 1 sein also RED für den Kreislauf
        assertEquals(1, coortest.phase);
    }

    @Test
    public void coordinatormitphase2init(){
        Coordinator coortest = new Coordinator("Bar", "BarTest", 2);
        LocalDateTime t = coortest.nextPhase(klassencontent, testcontent);
        //phase sollte danach dann 3 sein
        assertEquals(3, coortest.phase);
    }

    @Test
    public void lastphaseTestbackto1(){
        Coordinator coortest = new Coordinator("Bar", "BarTest", 2);
        Log l = coortest.lastPhase();
        assertEquals(1, coortest.phase);
    }

    @Test
    public void lastphaseTestbackto2(){
        Coordinator coortest = new Coordinator("Bar", "BarTest", 3);
        Log l = coortest.lastPhase();
        assertEquals(2, coortest.phase);
    }

    @Test
    public void lastphaseTestbackto3(){
        Coordinator coortest = new Coordinator("Bar", "BarTest", 1);
        Log l = coortest.lastPhase();
        assertEquals(3, coortest.phase);
    }
}
