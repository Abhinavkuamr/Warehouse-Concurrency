import java.time.Clock;

public class EmulationClock
{
    private static EmulationClock clock;
    int time_tick_size; // eg. 50 ms
    int days; // examples 365; (maybe make it fixed)
    private int tick = 0;
    private long lastUpdateTime = System.currentTimeMillis(); //initialized once object gets created


    private EmulationClock(){}

    private EmulationClock(int time_tick_size, int days)
    {
        this.time_tick_size = time_tick_size;
        this.days = days;
    }

    public static EmulationClock getInstance(int time_tick_size, int days)
    {
        if(clock == null){
            clock = new EmulationClock(time_tick_size,days);
            return clock;
        }
        return clock;
    }


    public void updateTick()
    {
        // increase the tick after every time_tick_size
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime >= time_tick_size) {
            tick++;
            lastUpdateTime += time_tick_size;
        }
    }

    public int getTick()
    {
        System.out.println("Tick: "+tick);
        return tick;
    }

    @Override
    public String toString() {

        return "TICK: "+tick;
    }

    public void reset()
    {
        this.tick = 0;
        this.days = 0;
        this.time_tick_size = 0;
    }

    public int Complete()
    {
        //return the final tick ; 1 day = 1000 ticks
        return days * 1000;
    }








}