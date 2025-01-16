# Interval analysis & Pointer Analysis

## Phase 1 (Interval analysis)

**Problem Statement**: Intra-procedural Interval analysis (using Kildall’s fixed-point algorithm)
 **Input** : A compiled .class from a .java file.

**Assumptions**:
 Ignore Exceptional control flow edges.
 Ignore non-integer variables and non-integer fields of objects entirely
 Ignore all pointers, objects, object allocations.
 Handle only integer variables
 
**Output**: At each program point print the collected facts.(some sample output in target1-pub folder)

The Implementation at core makes use of Java Programming Language & Soot analysis framework.

## Phase 2 (Points-to analysis)
Phase 2 of the project implements a points-to analysis for arrays, and subsequently an index-out-of-bounds analysis for arrays, for simple integer programs (programs with int variables and 1-dim int arrays. No other pointers/objects. No procedures)

Implemented a flow-sensitive intra-procedural points-to analysis for array variables. Each array variable may point to ”Null” or one or more array allocation sites. Technically, the
array variable would point to one or more dynamically allocated arrays, which are allocated at array allocation sites.

**Final Result**: Using the results of this points-to analysis, and the Phase-I interval analysis, implemented an index-out-of-bounds analysis.


 The overall implementation consists of three sequential stages: 
(1) interval analysis, (2) pointer analysis, and (3) array safety checking, utilizing the results from Stages 1 and 2.
 
We are **reusing the generic Kildall algorithm**, which have been implemented for Project Phase 1, for conducting the pointer analysis as well. This structured approachensures that each analysis builds on the previous one, contributing to a comprehensive verification process for array safety

**Usage:**
##
The analysis can be run directly from the command line, provided the java runtime is available on the system.

First add the soot packages to the class path using the command source environ.sh

then use the following command to run the analysis. Here

<target-folder> is the directory where the java source file on which analysis is to be done resides.
<main-class> is the name of the class file within the target-folder
<target-class> is the class in the main-class file
<target-method> is the method on which the intra-procedural analysis will be done java Analysis.java <target-folder> <main-class> <target-class> <target-method>
Running tests
The expected-output directory has some of the tests that were manually verified. In order to run all the tests execute

./run-all-tests.sh This executes all the tests and compares the output file with the one in expected-output folder.

You can run a specific test on one function by executing

./run-one-test.sh <function-name>

 **References**
##
 https://noidsirius.medium.com/a-beginners-guide-to-static-program-analysis-using-soot-5aee14a878d

 https://www.sable.mcgill.ca/soot/doc/soot/Unit.html

 https://cs.au.dk/∼amoeller/mis/soot.pdf

 https://github.com/soot-oss/soot/wiki/Tutorials

 https://github.com/noidsirius/SootTutorial/tree/master/docs/1
