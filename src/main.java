import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

public class main {
    public static void main(String[] args)
    {
        // making delivery in Clock
        EmulationClock clk = EmulationClock.getInstance(50,1);
        Stocker.StartStocking(2); // make it asynchronus so that clock doesnt stop ticking execute() or submit() not invoke
        while(clk.getTick() != clk.Complete()){
            clk.updateTick();

        }





    }
}
