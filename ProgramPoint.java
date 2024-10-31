import java.util.ArrayList;
import java.util.List;
import soot.jimple.Stmt;

public class ProgramPoint {
    private int id;
    private Stmt st;
    private boolean marked;
    private List<ProgramPoint> successors;

    public ProgramPoint(int id, Stmt st) {
        this.id = id;
        this.st = st;
        this.marked = false;
        this.successors = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public Stmt getStmt() {
        return st;
    }

    public void mark() {
        this.marked = true;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean value){this.marked=value;}
    public List<ProgramPoint> getSuccessors() {
        return successors;
    }

    public void setSuccessors(List<ProgramPoint> s) {this.successors = s;}

    public void addSuccessor(ProgramPoint successor) {
        this.successors.add(successor);
    }

    @Override
    public String toString() {
        return "ProgramPoint{id=" + id + ", statement='" + st + "'}";
    }
}
