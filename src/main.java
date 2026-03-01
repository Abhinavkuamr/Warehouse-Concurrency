import java.util.Calendar;

public class main {
    public static void main(String[] args)
    {
        EmulationClock clk = EmulationClock.getInstance(50,365);
        while(clk.getTick() != clk.Complete()){
            clk.updateTick();
        }


    }
}
