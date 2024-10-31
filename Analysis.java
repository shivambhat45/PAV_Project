// This program will plot a CFG for a method using soot
// [ExceptionalUnitGraph feature].
// Arguements : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>

// Ref:
// 1) https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
// 2) https://github.com/soot-oss/soot/wiki/Tutorials



////////////////////////////////////////////////////////////////////////////////
import java.io.File;
import java.io.FileWriter;
import java.util.*;

////////////////////////////////////////////////////////////////////////////////

import soot.*;
import soot.jimple.IfStmt;
import soot.options.Options;

import soot.jimple.Stmt;

import java.util.Map;
import java.util.stream.Collectors;

import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

////////////////////////////////////////////////////////////////////////////////



public class Analysis extends PAVBase {
    private DotGraph dot = new DotGraph("callgraph");
    private static HashMap<String, Boolean> visited = new
            HashMap<String, Boolean>();

    public Analysis() {
    }

    public static void doAnalysis(SootMethod targetMethod) {
        /*************************************************************
         * You can implement your analysis here. In general,
         you may implement your analysis in other classes, and invoke that code from here.
         ************************************************************/
    }

    public static void main(String[] args) {

        String targetDirectory = args[0];
        String mClass = args[1];
        String tClass = args[2];
        String tMethod = args[3];
        String upperBound = args[4];
        boolean methodFound = false;


        List<String> procDir = new ArrayList<String>();
        procDir.add(targetDirectory);

        // Set Soot options
        soot.G.reset();
        Options.v().set_process_dir(procDir);
        // Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg.spark", "verbose:false");

        Scene.v().loadNecessaryClasses();

        SootClass entryClass = Scene.v().getSootClassUnsafe(mClass);
        SootMethod entryMethod = entryClass.getMethodByNameUnsafe("main");
        SootClass targetClass = Scene.v().getSootClassUnsafe(tClass);
        SootMethod targetMethod = entryClass.getMethodByNameUnsafe(tMethod);
        // A SootClass is Soot's representation of a class in the target program.
        // A SootMethod is Soot's representation fo a method in the target program.
        // Pay attention to the code above, as you may need to use the methods used
        // above at other places in your code as well to find a desired  class or a desired method.

        Options.v().set_main_class(mClass);
        Scene.v().setEntryPoints(Collections.singletonList(entryMethod));

        // System.out.println (entryClass.getName());
        System.out.println("tclass: " + targetClass);
        System.out.println("tmethod: " + targetMethod);
        System.out.println("tmethodname: " + tMethod);
        Iterator mi = targetClass.getMethods().iterator();
        // targetClass.getMethods() retrieves all the methods in targetClass
        while (mi.hasNext()) {
            SootMethod sm = (SootMethod) mi.next();
            if (sm.getName().equals(tMethod)) {
                methodFound = true;
                break;
            }
            // Not sure why this loop and check is required
        }

        if (methodFound) {
            printInfo(targetMethod);
            IALatticePreProcess p = new IALatticePreProcess();
//            System.out.println(targetMethod.retrieveActiveBody());
            List<ProgramPoint> points = p.PreProcess(targetMethod.retrieveActiveBody());

            System.out.println(points);


//            System.out.println("----------------");
//            List<ProgramPoint> points = Merger.mergeProgramPoints(points2);
//            System.out.println(points);
//            System.out.println(mergedPoints.get(2).getSuccessors());
//            System.out.println("----------");
//            for(int i=0;i<mergedPoints.size();i++) {
//                System.out.println(mergedPoints.get(i).getId());
//                System.out.println(mergedPoints.get(i).getStmt());
//                System.out.println(mergedPoints.get(i).getSuccessors());
//
//                System.out.println("----------");
//            }

//          Interval.setGlobalUpperBound(Integer.valueOf(upperBound));


            IALatticeElement2 d0 = new IALatticeElement2();
            IALatticeElement2 bot = new IALatticeElement2();

            Body body = targetMethod.retrieveActiveBody();
            Set<String> variables = new HashSet<>();

            // Iterate over all locals in the body
            for (Local local : body.getLocals()) {
                d0.initializeVariable(local.getName());
                bot.initializeVariable(local.getName());
            }



            ArrayList<ArrayList<LatticeElement>> output = Kildall.run(points, d0, bot,Integer.valueOf(upperBound));
            System.out.println(output);


            //////////////////////////////////////////////////////////////////
            ///Joining with same successors

            ///////////////////////////////////////////////


            writeFinalOutput(output.get(output.size() - 1), targetDirectory, tClass + "." + tMethod, Integer.valueOf(upperBound));
            writeFullOutput(output, targetDirectory, tClass + "." + tMethod, Integer.valueOf(upperBound));

            // Draw the CFG with states information to .dot file.
            Map<Unit, LatticeElement> mp = new HashMap<>();
            for (int i = 0; i < points.size(); i++) {
                ProgramPoint p1 = points.get(i);
                mp.put(p1.getStmt(), output.get(output.size() - 1).get(i));
            }
            drawMethodDependenceGraph(targetMethod, mp, targetDirectory + File.separator + tClass + "." + tMethod);


//                   the call above prints the Soot IR (Intermediate Represenation) of the targetMethod.
            // You can use the same printInfo method appropriately
            // to view the IR of any method.

            // Note: Your analysis will work not on the source code of the target program, but on
            // the Soot IR only. Soot automatically generates the IR of every method from the compiled
            // .class files. In other words, any Soot-based analysis needs only the .class files, and does
            // not need or use the source .java files. The IR is similar to a three-address code.

            /*****************************************************************
             * XXX The function doAnalysis is the entry point for the Kildall's
             * fix-point algorithm over the LatticeElement.
             ******************************************************************/

            // Draw the CFG with states information to .dot file.
//            Map<Unit, LatticeElement> mp = new HashMap<>();
//            for (int i = 0; i < points.size(); i++) {
//                ProgramPoint p1 = points.get(i);
//                mp.put(p1.getStmt(), output.get(output.size() - 1).get(i));
//            }
//            drawMethodDependenceGraph(targetMethod, mp, targetDirectory + File.separator + tClass + "." + tMethod);
//	    // the call above generates the control-flow graph of the targetMethod.
            // Since you are going to implement a flow-insensitive analysis, you
            // may not need to know about control-flow graphs at all.

//	    doAnalysis(targetMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }
    }





    /**
     * Write full output to targetDirectory/tMethod.fulloutput.txt, the output format is as follows.
     * For each kildall's iterations, for each program point state which changed from previous iteration,
     * that program point is added to the output, as follows:
     * tMethod: inXX: var: {var_points_to}
     * the lines are lexographically sorted.
     *
     * @param output          ArrayList<ArrayList> States of program elements.
     * @param targetDirectory
     * @param tMethod
     */
    private static void writeFullOutput(ArrayList<ArrayList<LatticeElement>> output, String targetDirectory, String tMethod,int upperBound) {
        ArrayList<String> allLines = new ArrayList<>();

        for (int i = 0; i < output.size(); i++) {
            ArrayList<LatticeElement> iteration = output.get(i);
            Set<ResultTuple> data = new HashSet<>();
            for (int j = 1; j < iteration.size(); j++) {
                IALatticeElement2 e_ = (IALatticeElement2) iteration.get(j);
                if (i != 0 && e_.equals(output.get(i - 1).get(j)))
                    continue; // only output the states that changed from previous iterations.
                for (Map.Entry<String, Interval> entry : e_.getState().entrySet()) {
                    if (entry.getValue().isEmpty()) continue;

                    Interval interval = entry.getValue();

                    if(interval.getLowerBound()<0)
                    {
                        interval.setFinalLowerBound("-inf");
                    }
                    if(interval.getUpperBound()>upperBound)
                    {
                        interval.setFinalUpperBound("inf");
                    }
                    // Assuming Interval has methods to get the bounds
                    List<String> intervalStrings = new ArrayList<>();

                    intervalStrings.add(interval.otherString());

                    ResultTuple tuple = new ResultTuple(tMethod, "in" + String.format("%02d", j), entry.getKey(), intervalStrings);
                    data.add(tuple);
                }
            }

            String[] lines = fmtOutputData(data);


            allLines.addAll(Arrays.asList(lines));
            allLines.add("");
        }
        sortInValues(allLines);



        try {
            FileWriter writer = new FileWriter(targetDirectory + File.separator + tMethod + ".fulloutput.txt");
            for (String line : allLines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Can't write to the file:" + tMethod + ".fulloutput.txt");
            System.out.println("Writing output to stdout:");
            for (String line : allLines) {
                System.out.println(line);
            }
        }
    }






    /**
     * Write the final states info of all program point to targetDirectory/tMethod.output.txt
     * The output format is as mentioned.
     *
     * @param output          ArrayList of states of each program point in order of stmt.
     * @param targetDirectory
     * @param tMethod
     */
    private static void writeFinalOutput(ArrayList<LatticeElement> output, String targetDirectory, String tMethod,int upperBound) {
        Set<ResultTuple> data = new HashSet<>();
        int index = 0;
        for (LatticeElement e : output) {
            if(index==0)
            {
                index++;
                continue;
            }
            IALatticeElement2 e_ = (IALatticeElement2) e;
            for (Map.Entry<String, Interval> entry : e_.getState().entrySet()) {
                if (!entry.getValue().isEmpty()) {

                    Interval interval = entry.getValue();

                    if(interval.getLowerBound()<0)
                    {
                        interval.setFinalLowerBound("-inf");
                    }
                    if(interval.getUpperBound()>upperBound)
                    {
                        interval.setFinalUpperBound("inf");
                    }

                    // Assuming Interval has methods to get the bounds
                    List<String> intervalStrings = new ArrayList<>();
                    intervalStrings.add(interval.otherString());

                    ResultTuple tuple = new ResultTuple(tMethod, "in" + String.format("%02d", index), entry.getKey(), intervalStrings);
                    data.add(tuple);
                }
            }
            index++;
        }

        String[] lines = fmtOutputData(data);

        try {
            FileWriter writer = new FileWriter(targetDirectory + "/" + tMethod + ".output.txt");
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Can't write to the file:" + tMethod + ".output.txt");
            System.out.println("Writing output to stdout:");
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }


    private static void drawMethodDependenceGraph(SootMethod entryMethod, Map<Unit, LatticeElement> labels, String filename){
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();

            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

            DotGraph d = new DotGraph(filename);

            for (Unit unit : graph) {
                List<Unit> successors = graph.getSuccsOf(unit);
                for (Unit succ : successors) {
                    d.drawNode(labels.get(unit) + "\n" + unit.toString() + " hash: " + unit.hashCode());
                    d.drawNode(labels.get(succ) + "\n" + succ.toString() + " hash: " + succ.hashCode());
                    d.drawEdge(labels.get(unit) + "\n" + unit + " hash: " + unit.hashCode(), labels.get(succ) + "\n" + succ + " hash: " + succ.hashCode());
                }
            }
            d.plot(filename + ".dot");
        }
    }

    public static void printUnit(int lineno, Body b, Unit u){
        UnitPrinter up = new NormalUnitPrinter(b);
        u.toString(up);
        String linenostr = String.format("%02d", lineno) + ": ";
        System.out.println(linenostr + up.toString());
    }


    private static void printInfo(SootMethod entryMethod) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete())
        {
            Body body = entryMethod.retrieveActiveBody();
	    // `body' refers to the code body  of entryMethod

            int lineno = 1;
            for (Unit u : body.getUnits()) {
		// .getUnits retrieves all the Units in the code body of the method, as a list. 
		// A Unit basically represent a single statement or conditional in the Soot IR.
		// You should fully understand the structure of a Unit, the subtypes of Unit, etc., 
		// to make progress with your analysis.
		// Objects of type `Value' in Soot represent  local variables, constants and expressions.
		// Expressions may be BinopExp, InvokeExpr, and so on.
		// Boxes: References in Soot are called boxes. There are two types – Unitboxes, ValueBoxes.

                if (!(u instanceof Stmt)) {
                    continue;
                }
                Stmt s = (Stmt) u;
                printUnit(lineno, body, u);
                lineno++;
            }

        }
    }

    protected static String fmtOutputLine(ResultTuple tup, String prefix) {
        String line = tup.m + ": " + tup.p + ": " + tup.v + ": ";
        List<String> intervalValues = tup.pV;
        Collections.sort(intervalValues);
        for (int i = 0; i < intervalValues.size(); i++) {
            if (i != intervalValues.size() - 1) line += intervalValues.get(i) + ", ";
            else line += intervalValues.get(i);
        }
        return (prefix + line);
    }


    protected static String fmtOutputLine(ResultTuple tup) {
        return fmtOutputLine(tup, "");
    }

    public static void sortInValues(ArrayList<String> values) {
        Collections.sort(values, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer num1 = extractNumericValue(o1);
                Integer num2 = extractNumericValue(o2);
                return Integer.compare(num1, num2);
            }

            private Integer extractNumericValue(String str) {
                // Use regex to find the numeric value after "in"
                String regex = "in(\\d+)";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                java.util.regex.Matcher matcher = pattern.matcher(str);

                if (matcher.find()) {
                    // Return the integer value found
                    return Integer.parseInt(matcher.group(1));
                }
                // If "inXX" not found, return a value that ensures proper sorting
                return Integer.MAX_VALUE; // or any other default value
            }
        });
    }
    private static int alphanumericCompare(String str1, String str2) {
        String[] parts1 = str1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] parts2 = str2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            int cmp = parts1[i].compareTo(parts2[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(parts1.length, parts2.length); // Compare length if equal
    }




    protected static String[] fmtOutputData(Set<ResultTuple> data, String prefix) {
        String[] outputlines = new String[data.size()];

//        List<ResultTuple> sortedData = sortResultTuples(data);
        int i = 0;
        for (ResultTuple tup : data) {
            outputlines[i] = fmtOutputLine(tup, prefix);
            i++;
        }

        Arrays.sort(outputlines);
        return outputlines;
    }

    protected static String[] fmtOutputData(Set<ResultTuple> data) {
        return fmtOutputData(data, "");
    }

}


