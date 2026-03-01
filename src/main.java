import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

public class main {
    public static void main(String[] args)
    {
        EmulationClock clk = EmulationClock.getInstance(100,1);
        while(clk.getTick() != clk.Complete()){
            clk.updateTick();
        }





    }
}
