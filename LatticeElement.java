import soot.jimple.Stmt;
public interface LatticeElement extends Cloneable{

    // joins two lattice elements
    public LatticeElement join_op(LatticeElement r);

    // returns true if a lattice element is equal to another
    public boolean equals(LatticeElement r);

    // transfer function for assignment statements
    public LatticeElement tf_assignstmt(Stmt st,int upperBound);

    // transfer function for conditional statements
    public LatticeElement tf_condstmt(boolean b, Stmt st,int upperBound);

    // For Handling Different sort of Transfer Stmt within one.
    LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken,int upperBound);

    // Identity Function.
    LatticeElement tf_identity_fn();


}
