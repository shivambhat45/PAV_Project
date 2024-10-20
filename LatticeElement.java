import soot.jimple.Stmt;
public interface LatticeElement {

    // joins two lattice elements
    public LatticeElement join_op(LatticeElement r);

    // returns true if a lattice element is equal to another
    public boolean equals(LatticeElement r);

    // transfer function for assignment statements
    public LatticeElement tf_assignstmt(Stmt st);

    // transfer function for conditional statements
    public LatticeElement tf_condstmt(boolean b, Stmt st);

}
