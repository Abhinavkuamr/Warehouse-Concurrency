import java.util.LinkedList;
import java.util.concurrent.RecursiveTask;

// Singleton -> There is only 1 warehouse
public class Warehouse extends RecursiveTask<Boolean> implements  logger{
    private static Warehouse warehouse;

    //TODO: Need 2 functions -
    // 1.function to wait 10 ticks + number boxes ticks to move from StgArea to Warehouse
    // 2. warehouse to Staging area = 10 ticks + number of boxes stocker still has (if section is full he might go back)
    // 3. Going between sections = 10 ticks + number of boxes they still posses

    void wait_stageToWarehouse(Thread t, int myBoxes){
        try {
            t.sleep(EmulationClock.time_tick_size * (10 + myBoxes) );
        } catch (InterruptedException e) {

        }
    }



    private Warehouse(){}

    @Override
    protected Boolean compute() {
        return null;
    }

    ;

    private Warehouse(LinkedList<String> myBoxes){
        wait_stageToWarehouse(Thread.currentThread(), myBoxes.size());
    }


    public static Warehouse getInstance(){
        if(warehouse == null){
            warehouse = new Warehouse();
            return warehouse;
        }
        return warehouse;
    }
}
