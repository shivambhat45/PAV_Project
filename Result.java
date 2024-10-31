import java.util.Map;

public class Result {
    public Map<String, Interval> state;
    public boolean propagate;

    public Result(Map<String, Interval> state, boolean propagate) {
        this.state = state;
        this.propagate = propagate;
    }
}
