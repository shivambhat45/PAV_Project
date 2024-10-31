import soot.jimple.Stmt;
public interface LatticeElement extends Cloneable{

    // joins two lattice elements
    public LatticeElement join_op(LatticeElement r);

    // returns true if a lattice element is equal to another
    public boolean equals(LatticeElement r);

    // transfer function for assignment statements
    public LatticeElement tf_assignstmt(Stmt st);

    // transfer function for conditional statements
    public LatticeElement tf_condstmt(boolean b, Stmt st,int upperBound);

    //------------------- Extra Added ----------------------
    LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken,int upperBound);

    // return new LatticeElement same as "this", but not the same object.
    LatticeElement tf_identity_fn();

    public boolean lessThan(LatticeElement r);

    public boolean propogationCondition();

    LatticeElement deepCopy();

    // ---------------Till Here ---------------------------

}
