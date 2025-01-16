import soot.Body;

import java.util.List;

public interface IAPreProcess {

    public List<ProgramPoint> PreProcess(Body body);
}
