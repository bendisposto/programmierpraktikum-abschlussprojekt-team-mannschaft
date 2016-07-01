package main.java.tddt;

import vk.core.api.*;
import vk.core.api.CompilerResult;
import vk.core.api.TestResult;
import vk.core.api.CompileError;
import vk.core.api.CompilerFactory;
import vk.core.api.JavaStringCompiler;
import vk.core.internal.*;
import vk.core.internal.InternalResult;
import java.util.Collections;
import java.util.Collection;

public class Coordinator {
    private String classname; //namen bei einem Coordinator, der für eine Session ist, festgelegt
    private String testname;
    public int phase; //wird 1,2 oder 3 also red, green oder refactor

    public Coordinator(String classname,  String testname){
        this.classname = classname;
        this.testname = testname;
        phase = 1; //phase 1, also red bzw. tests schreiben
    }
    public String compile(String classcontent, String testcontent){
        String result = ""; //Compilemessages zurückgeben
        CompilationUnit testcompile = new CompilationUnit(testname, testcontent, true);
        CompilationUnit classcompile = new CompilationUnit(classname, classcontent, false);
        JavaStringCompiler compiler = CompilerFactory.getCompiler(classcompile, testcompile);
        compiler.compileAndRunTests();
        CompilerResult compresult = compiler.getCompilerResult();
        TestResult testresult = compiler.getTestResult();
        //zumächst die Refactor fälle betrachtet
        if(phase == 3 && compresult.hasCompileErrors()){
            Collection<CompileError> errors = compresult.getCompilerErrorsForCompilationUnit(classcompile);
            for(CompileError e : errors)result += e.getMessage();
        }
        //Refactoring erfolgreich, man kann dann wieder zu RED wechseln
        if(phase == 3 && !compresult.hasCompileErrors() && (testresult.getNumberOfFailedTests() == 0)){
            result = "Refactoring successful, at least Tests are running without failures again";
            phase = 4; //in dieser Phase ist der Wechsel zu Red wieder möglich
        }
        //phase RED, also phase = 1
        if(phase == 1 && compresult.hasCompileErrors() ){//Bedingung auch erfüllt, wenn es Code gibt ,der nicht kompiliert
            Collection<CompileError> errors = compresult.getCompilerErrorsForCompilationUnit(testcompile);
            for(CompileError e : errors)result += e.getMessage();
            phase = 2;
        }
        else if(phase == 1  && (testresult.getNumberOfFailedTests() > 0)){
            Collection<TestFailure> failures = testresult.getTestFailures();
            for(TestFailure f : failures)result += f.getMessage();
            phase = 2; //Bedingungen für Übergang zu Phase 2 also Green gegeben
        }
        //phase GREEN also phase = 2
        if(phase == 2 && compresult.hasCompileErrors()){
            Collection<CompileError> errors = compresult.getCompilerErrorsForCompilationUnit(classcompile);
            for(CompileError e : errors)result += e.getMessage();
        }
        else if(phase == 2  && (testresult.getNumberOfFailedTests() > 0)) {
            Collection<TestFailure> failures = testresult.getTestFailures();
            for (TestFailure f : failures) result += f.getMessage();
        }
        else if(phase == 2 && (testresult.getNumberOfFailedTests() == 0)){
            result = "All "+Integer.toString(testresult.getNumberOfSuccessfulTests())+ "executed successfully";
            phase = 3; //Alle Tests ans Laufen bekommen, die Bedingung für das Refacotring erfüllt
        }
        return result;
    }

    public void setPhase(int i){//zum aktiven Phasenwechsel, e.g. zurück zu RED oder bei Refactor
        if(i > 0 && i < 4){  // nur phasen 1-3 zu wählen
            this.phase = i;
        }
    }

}
