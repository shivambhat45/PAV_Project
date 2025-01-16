import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;


import java.util.*;

public class Safety {

    public static HashMap<Integer, String> myMap = new HashMap<>();
    public static void analyzeArraySafety(
            List<ProgramPoint> programPoints,
            ArrayList<ArrayList<LatticeElement>> intervalAnalysis,
            ArrayList<ArrayList<LatticeElement>> pointerAnalysis) {

        // Map to store static array sizes (array name -> size)
        HashMap<String, Integer> staticArraySizes = new HashMap<>();
        int cnt=0;
        for (int programPointIndex = 0; programPointIndex < programPoints.size(); programPointIndex++) {
            ProgramPoint programPoint = programPoints.get(programPointIndex);
            Stmt stmt = programPoint.getStmt();

            // Step 1: Check for static array declaration
            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                if (assignStmt.getRightOp() instanceof NewArrayExpr) {
                    // Static array declaration found
                    NewArrayExpr newArrayExpr = (NewArrayExpr) assignStmt.getRightOp();
                    String arrayName = assignStmt.getLeftOp().toString();
                    int arraySize = Integer.parseInt(newArrayExpr.getSize().toString());

                    // Store the static array size
                    staticArraySizes.put(arrayName, arraySize);
                    staticArraySizes.put("new"+String.format("%02d", cnt++),arraySize);


//                    System.out.println("Static Array Declared: " + arrayName + " with size " + arraySize + " at Program Point " + programPointIndex);
                    continue;
                }
            }

            // Step 2: Check for array access (RHS or LHS contains ArrayRef)
            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;

                // Check RHS for ArrayRef
                if (assignStmt.getRightOp() instanceof ArrayRef) {
                    handleArrayAccess(
                            (ArrayRef) assignStmt.getRightOp(),
                            staticArraySizes,
                            intervalAnalysis,
                            pointerAnalysis,
                            programPointIndex);
                }

                // Check LHS for ArrayRef
                if (assignStmt.getLeftOp() instanceof ArrayRef) {
                    handleArrayAccess(
                            (ArrayRef) assignStmt.getLeftOp(),
                            staticArraySizes,
                            intervalAnalysis,
                            pointerAnalysis,
                            programPointIndex);
                }
            }
        }
    }

    private static void handleArrayAccess(
            ArrayRef arrayRef,
            HashMap<String, Integer> staticArraySizes,
            ArrayList<ArrayList<LatticeElement>> intervalAnalysis,
            ArrayList<ArrayList<LatticeElement>> pointerAnalysis,
            int programPointIndex) {

        String arrayName = arrayRef.getBase().toString();
        String indexVar=arrayRef.getIndex().toString();

        Interval indexInterval;
        if(isInteger(indexVar))
        {
            int num=Integer.parseInt(indexVar);
             indexInterval=new Interval(num,num);
        }
        else {
            // Retrieve index variable interval
             indexInterval = ValueFilter.getVariableValueAtProgramPoint(intervalAnalysis, programPointIndex, indexVar);
        }
        // Check for static or dynamic array size
        Integer staticSize = staticArraySizes.get(arrayName);
        boolean isSafe = true;

        if (staticSize != null) {
            // Static array size available
            if (indexInterval == null || indexInterval.getLowerBound() < 0 || indexInterval.getUpperBound() >= staticSize) {
                isSafe = false;
                myMap.put(programPointIndex,"Potentially Unsafe");
//                System.out.println("Potentially Unsafe: Array access " + arrayName + "[" + indexVar + "] at Program Point " + programPointIndex +
//                        " exceeds static bounds (size: " + staticSize + ")");
            }
        } else {
            // Dynamic array handling using pointer analysis
            HashSet<String> pointerValues = ValueFilter.getPointerValueAtProgramPoint(pointerAnalysis, programPointIndex, arrayName);
            if (pointerValues == null || pointerValues.isEmpty()) {
                isSafe = false;
                myMap.put(programPointIndex,"Potentially Unsafe");
//                System.out.println("Potentially Unsafe: Array " + arrayName + " is uninitialized at Program Point " + programPointIndex);
            } else {
                for (String allocationSite : pointerValues) {
                    // Handle null allocation sites
                    if (allocationSite.equals("null")) {
                        isSafe = false;
                        myMap.put(programPointIndex, "Potentially Unsafe");
//                        System.out.println("Potentially Unsafe: Null allocation site encountered for array " + arrayName +" at Program Point " + programPointIndex);
                        continue;
                    }
                    else{
                    int dynamicSize = staticArraySizes.get(allocationSite);
                    if (indexInterval == null || indexInterval.getLowerBound() < 0 || indexInterval.getUpperBound() >= dynamicSize) {
                        isSafe = false;
                        myMap.put(programPointIndex,"Potentially Unsafe");
//                        System.out.println("Potentially Unsafe: Array access " + arrayName + "[" + indexVar + "] at Program Point " +
//                                programPointIndex + " exceeds bounds for allocation site " + allocationSite);
                    }
                }
                }
            }
        }

        if (isSafe) {
            myMap.put(programPointIndex,"Safe");
//            System.out.println("Safe: Array access " + arrayName + "[" + indexVar + "] at Program Point " + programPointIndex);
        }
    }

    // Placeholder to retrieve dynamic array size from allocation site
    private static int getArraySize(String allocationSite) {
        // Example implementation: Replace with actual logic
        return 10; // Assume all arrays are of size 10 for now
    }

    public static boolean isInteger(String str) {
        return str.matches("-?\\d+");
    }


}








