import soot.jimple.Stmt;
public interface LatticeElement {

    public LatticeElement join_op(LatticeElement r);

    public boolean equals(LatticeElement r);

    public LatticeElement tf_assignstmt(Stmt st);

    public LatticeElement tf_condstmt(boolean b, Stmt st);

}
