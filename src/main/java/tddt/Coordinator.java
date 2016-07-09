package main.java.tddt;

import javafx.scene.control.Label;
import main.java.tddt.data.Log;
import main.java.tddt.data.LogList;
import vk.core.api.*;
import vk.core.api.CompilerResult;
import vk.core.api.TestResult;
import vk.core.api.CompileError;
import vk.core.api.CompilerFactory;
import vk.core.api.JavaStringCompiler;
import vk.core.internal.*;
import vk.core.internal.InternalResult;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Collection;

public class Coordinator {
    private String classname; //namen bei einem Coordinator, der für eine Session ist, festgelegt
    private String testname;
    public int phase; //wird 1,2 oder 3 also red, green oder refactor
    //public Label zeitlabel;  //gui verbindung
    private boolean babystepsactiv = false;
    public LogList logs;
    private File logfile;
    LocalDateTime timer = LocalDateTime.now();
    private double babystepstime;

    public Coordinator(String classname,  String testname){ //Anfangskonstuktor
        this.classname = classname;
        this.testname = testname;
        phase = 1; //phase 1, also red bzw. tests schreiben
    }
    public Coordinator(String classname,  String testname, int phase){ //Konstruktor zum Laden einer bestimmten phase
        this.classname = classname;
        this.testname = testname;
        this.phase = phase;
    }
    public Coordinator(String classname, String testname, int phase, File file){//Konstruktor zum Laden eines vorhandenen Logs
        this.classname = classname;
        this.testname = testname;
        this.phase = phase;
        this.logfile = file;
    }
    /*public Coordinator(String classname,  String testname, int phase, Label label){ //Konstruktor zum Laden einer bestimmten phase und übergebenem Label
        this.classname = classname;
        this.testname = testname;
        this.phase = phase;
        this.zeitlabel = label;
    }*/
    public String compile(String classcontent, String testcontent){
        String result = ""; //Compilemessages zurückgeben
        CompilationUnit testcompile = new CompilationUnit(testname, testcontent, true);
        CompilationUnit classcompile = new CompilationUnit(classname, classcontent, false);
        JavaStringCompiler compiler = CompilerFactory.getCompiler(classcompile, testcompile);
        compiler.compileAndRunTests();
        CompilerResult compresult = compiler.getCompilerResult();
        TestResult testresult = compiler.getTestResult();
        if(compresult.hasCompileErrors()){
            Collection<CompileError> errors = compresult.getCompilerErrorsForCompilationUnit(classcompile);
            for(CompileError e : errors)result += e.getMessage() + "\n";
            Collection<CompileError> testerrors = compresult.getCompilerErrorsForCompilationUnit(testcompile);
            for(CompileError e : testerrors)result += e.getMessage() + "\n";
        }
        else {
            result += "Compiled successfully" + "\n";

            //kann es nicht kompiliert werden, so können die Tests auch nicht ausgeführt werden
            if ((testresult.getNumberOfFailedTests() > 0)) {
                Collection<TestFailure> failures = testresult.getTestFailures();
                for (TestFailure f : failures) result += f.getMessage() + "\n";
            } else { //ansonsten sind alle Tests erfolgreich
                result += "All Tests successful. " + "Number of executed tests: " + Integer.toString(testresult.getNumberOfSuccessfulTests());
            }
        }
        LocalDateTime time = LocalDateTime.now();
        //aktuellen Log hinzufügen
        try {
            logs.addLog(new Log(this.phase, time, timer, classcontent, testcontent, result)); //timer nur bei phasenwechsel wichtig
        }
        catch(JAXBException j){}
        return result;
    }

    public LocalDateTime nextPhase(String classcontent, String testcontent){
        CompilationUnit testcompile = new CompilationUnit(testname, testcontent, true);
        CompilationUnit classcompile = new CompilationUnit(classname, classcontent, false);
        JavaStringCompiler compiler = CompilerFactory.getCompiler(classcompile, testcompile);
        compiler.compileAndRunTests();
        CompilerResult compresult = compiler.getCompilerResult();
        TestResult testresult = compiler.getTestResult();
        int tempphase = this.phase;  //zum Vergleichen, ob die Phase gewechselt werden konnte
        //phase RED, also phase = 1
        if(phase == 1 && compresult.hasCompileErrors() ){//Bedingung auch erfüllt, wenn es Code gibt ,der nicht kompiliert
            phase = 2;
        }
        else if(!compresult.hasCompileErrors()) {
            //phase GREEN also phase 2
            if(phase == 1  && (testresult.getNumberOfFailedTests() > 0)){
                phase = 2; //Bedingungen für Übergang zu Phase 2 also Green gegeben
            }
            else if (phase == 2 && (testresult.getNumberOfFailedTests() == 0)) {
                phase = 3; //Alle Tests ans Laufen bekommen, die Bedingung für das Refacotring erfüllt
            }
            //Refactoring erfolgreich, man kann dann wieder zu RED wechseln
            else if (phase == 3 && (testresult.getNumberOfFailedTests() == 0)) {
                phase = 1; //zurück zu RED
            }
        }
        LocalDateTime time = LocalDateTime.now();
        if(this.phase != tempphase){//Beim Wechsel der Phase Log hinzufügen, timer speichern und neu starten für die neue Phase
            //aktuellen Log hinzufügen
            try {
                logs.addLog(new Log(this.phase, time, time, classcontent, testcontent, "test"));
                return time;
            }
            catch(JAXBException j){}
            //Funktionen noch nicht vorhanden
            //timer.stop
            //timer = new Timer
        }
        return time;
    }

    //zurück zum Zustand des letzten Logs
    public void BackToLastLog(){
        logs.deleteLast(); //aktuellen Log löschen
    }
    public Log lastLog(){
        logs.deleteLast(); //aktuellen Log löschen
        return logs.getLog(logs.size() - 1);
    }
    //logliste löschen
    public void deleteLog(){
        logs.deleteAll();
    }
    //zu letzter Phase zurück
    public Log lastPhase(){
        int tempphase = logs.getLog(logs.size()-1).getPhase();
        this.BackToLastLog();
        if(tempphase == logs.getLog(logs.size()-1).getPhase()){ //Log zurück bis es der letzte Log der letzten Phase ist
            return this.lastPhase();
        }
        else {
            this.phase = logs.getLog(logs.size()-1).getPhase();//Phase entsprechend aendern
            return logs.getLog(logs.size() - 1);
        }
    }

    public Log Babystepsover(){ //wenn die zeit in den Babysteps abgelaufen ist, dann an Anfang der Phase springen
        int tempphase = logs.getLog(logs.size()-1).getPhase();
        this.BackToLastLog();
        if(tempphase == logs.getLog(logs.size()-1).getPhase()){ //Log zurück bis es der letzte Log der letzten Phase ist
            return this.lastPhase();
        }
        else {
            //in Phase bleiben, sodass man nun wieder am Anfang der Phase ist, die man mit Babysteps gestartet hat
            return logs.getLog(logs.size() - 1);
        }
    }

    public void setBabystepsActivated(boolean activ, double timing){
        this.babystepsactiv = activ;
        this.babystepstime = timing;
    }

    //Babystepszustand holen
    public boolean getBabystepsActivated(){
        return this.babystepsactiv;
    }
    public double getBabystepsTime(){
        return babystepstime;
    }

    public LocalDateTime[][] getPhaseTimes(){
        LocalDateTime[][] times = new LocalDateTime[3][4];
        times[0][1] = LocalDateTime.now();
        return times;
    }

}
