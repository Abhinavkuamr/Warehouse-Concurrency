import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;


// TODO: set minimum picker length as 5 ( since we got 5 types of sections, atleast 1 each) maybe ?
public class Picker extends RecursiveAction implements logger, Trolley{

    Warehouse warehouse = Warehouse.getInstance();

    // idea: when each picker gets initialized -> it gets a random warehousesection assigned
    BoxTypes pickerType = BoxTypes.getRandomType();
    Section warehouseSection = warehouse.sectionMap.get(this.pickerType);

    static AtomicInteger pickIdCounter = new AtomicInteger(0);
    double pickProbability; // set in constructor based on number of pickers
    static int pickAttemptyPerDay;





    // create many pickers
    public Picker(int totalPickers)
    {
        // 100 attempts per 1000 ticks = 0.1 per tick total
        // split across all pickers
        // 100 / 1000 = 0.1 / 5 = 0.02 probablity per tick
        this.pickProbability = (pickAttemptyPerDay / 1000.000 ) / totalPickers;
    }

    // just get boxes ; use compute to give logic for synchronization
    int getBoxes(Section section, BoxTypes type) throws InterruptedException {
        //this.pickerType = BoxTypes.getRandomType();
        LinkedList<BoxTypes> myBoxes = warehouse.pickBoxes(type);
        int picked = myBoxes.size();
        //destroy myBoxes; poll 1 by 1
        while(myBoxes.poll() != null);
        return picked;
    }

    // need to reuse this alot
    public Boolean isStockerWorking(){
        return warehouseSection.stockerLock.availablePermits() == 0 ;
    }



    @Override
    protected void compute() {
        while(EmulationClock.tick < EmulationClock.Complete()) {
            try {
                if(new Random().nextDouble() >= pickProbability) { // > 0.02 pick it
                    Thread.sleep(EmulationClock.time_tick_size);
                    continue;
                }

                BoxTypes type = BoxTypes.getRandomType();
                Section section = warehouse.sectionMap.get(type);
                int pickId = pickIdCounter.addAndGet(1);

                int startTick = EmulationClock.tick;
                Globals.trolleys.acquire();
                int trolleyId = Globals.trolleyID.addAndGet(1);
                Globals.mappings.put(Thread.currentThread(), trolleyId);
                int waited = EmulationClock.tick - startTick;
                logger.super.TrolleyAcquireEvent(Thread.currentThread(), Globals.mappings, waited);

                logger.super.PickStartEvent(pickId, type.toString(), trolleyId);

                startTick = EmulationClock.tick;

                section.waitingPickers.incrementAndGet();
                while(section.storage.isEmpty()) {
                    Thread.sleep(EmulationClock.time_tick_size);
                }
                section.waitingPickers.decrementAndGet();

                section.pickerPermits.acquire(1);
                int picked = 0;
                try {
                    picked = getBoxes(section, type);
                } finally {
                    section.pickerPermits.release(1);
                }

                int waitedTicks = EmulationClock.tick - startTick - picked;
                logger.super.PickDoneEvent(pickId, type.toString(), picked, trolleyId, waitedTicks);

                logger.super.TrolleyReleaseEvent(Thread.currentThread(), Globals.mappings);
                Globals.trolleys.release();

            } catch (InterruptedException e) {}
        }
    }


    // how many pickers user nneed to choose
    static void StartPicking(int picker){

        for(int i = 0 ; i<picker; i++){
            Globals.fjPool.execute(new
                    Picker(picker)); // when we create stocker we create 10 trolleys , 10 boxes max 1 can have TODO: need to seprate that

        }


    }
}