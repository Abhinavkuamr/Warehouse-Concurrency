import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//use it at last to startStocking function
class Globals {
    static Semaphore trolleys;
    static AtomicInteger trolleyID = new AtomicInteger(0); //for trolley ID , atomic operation , no disturbance from other threads
    static ConcurrentHashMap<Thread, Integer> mappings = new ConcurrentHashMap<>(); //Thread to trolley(sema) mappings

    static ForkJoinPool fjPool = new ForkJoinPool();

    static void init(int trolleyCount) {
        trolleys = new Semaphore(trolleyCount);
    }
}

interface  Trolley {
    //static int number;

    default Semaphore CreateTrolley(int number){
        Semaphore trolley = new Semaphore(number);
        return trolley;
    }

}

interface logger{
    default void TrolleyAcquireEvent(Thread t,ConcurrentHashMap mappings, int waited ){
        System.out.println("Tick="+EmulationClock.tick+" Event=acquire_trolley "+"Trolley_id="+mappings.get(t.currentThread()) + " waited="+waited+" tick(s)");
    }

    default void TrolleyReleaseEvent(Thread t,ConcurrentHashMap mappings){
        System.out.println("Tick="+EmulationClock.tick+" Event=release_trolley "+"Trolley_id="+mappings.get(t.currentThread()));
    }

    default void LoadEvent(LinkedList<BoxTypes> boxes, int waited, int stockerId) {
        System.out.println("Tick="+EmulationClock.tick+" Event=stocker_load stocker_id="+stockerId+" "+boxes+" waited="+waited+" tick(s)");
    }
    default void MoveEvent(String from, String to, int load, int trolleyId, int waited, int stockerId){
        System.out.println("Tick="+EmulationClock.tick+" Event=move stocker_id="+stockerId+" from="+from+" to="+to+" load="+load+" trolley_id="+trolleyId+" waited="+waited+" tick(s)");
    }

    default void StockBeginEvent(String section, int amount, int trolleyId, int waited, int stockerId) {
        System.out.println("Tick="+EmulationClock.tick+" Event=stock_begin stocker_id="+stockerId+" section="+section+" amount="+amount+" trolley_id="+trolleyId+" waited="+waited+" tick(s)");
    }

    default void StockEndEvent(String section, int stocked, int remainingLoad, int trolleyId, int stockerId){
        System.out.println("Tick="+EmulationClock.tick+" Event=stock_end stocker_id="+stockerId+" section="+section+" stocked="+stocked+" remaining_load="+remainingLoad+" trolley_id="+trolleyId);
    }

    default void ReturnToStagingEvent(int load, int trolleyId, int waited){
        System.out.println("Tick="+EmulationClock.tick+" Event=return_to_staging load="+load+" trolley_id="+trolleyId+" waited="+waited+" tick(s)");
    }

    default void PickStartEvent(int pickId, String section, int trolleyId) {
        System.out.println("Tick="+EmulationClock.tick+" Event=pick_start pick_id="+pickId+" section="+section+" trolley_id="+trolleyId);
    }

    default void PickDoneEvent(int pickId, String section, int picked, int trolleyId, int waitedTicks) {
        System.out.println("Tick="+EmulationClock.tick+" Event=pick_done pick_id="+pickId+" section="+section+" picked="+picked+" trolley_id="+trolleyId+" waited="+waitedTicks+" tick(s)");
    }

    default void StockerBreakStartEvent( ){
        System.out.println("Tick="+EmulationClock.tick+" Event=stocker_break_start ");
    }

    default  void StockerBreakEndEvent(){
        System.out.println("Tick="+EmulationClock.tick+" Event=stocker_break_end ");
    }
}

// TODO: 1. Take boxes from Staging area
// TODO: 2. Make Stocker follow singleton pattern too
// create that Global class forjoin -> call it from main.java where clock is runnning
public class Stocker extends RecursiveTask<Boolean> implements logger {

    static int box_limit;
    static int number  ; // user input
    StagingArea stgArea = StagingArea.getInstance();  // have entire access as a stocker to staging area

    static Boolean stockerBreakEnabled = false;
    static int stockerBreakTickValue;


    static AtomicInteger stockerIdCounter = new AtomicInteger(0);
    int stockerId;




    Stocker(int box_limit, int number) {
        this.box_limit = box_limit;
        this.number = number;
        this.stockerId = stockerIdCounter.addAndGet(1);
    }



    /*
    Task 1:
    * 1. Acquire trolley semaphore -> Log event
    * 2. Get 10 boxes then fork so that next thread can take it -> log events
    * */
    @Override
    protected Boolean compute() {

        LinkedList<BoxTypes> myBoxes = new LinkedList<>(); // stocker's boxes taken from staging area


        while(EmulationClock.tick < EmulationClock.Complete()) {

            int startTick = EmulationClock.tick;
            int waited = 0;




        //waited = EmulationClock.tick; // this one should be 0 tick at start
        while (stgArea.stagingArea.isEmpty() && myBoxes.isEmpty()) {
            try {
                Thread.currentThread().sleep(EmulationClock.time_tick_size); //just goto sleep
            } catch (InterruptedException e) {

            }
        }


        // makeDelivery done -> notifyall() called, thread came out of above while loop
        //Lock the staging area for 1 stocker to enter
        stgArea.lock.lock();


        // t1 gets lock , t2 waits -> t1 gets 10 boxes but staging area is empty now, t2 comes checks while loop again releases lock and goes to await()
        try {
            while (stgArea.stagingArea.isEmpty()) {
                stgArea.lock_condition.await(); // sleeps, releases lock, wakes on signalAll
            }
             waited = EmulationClock.tick - startTick;


            Globals.trolleys.acquire();
            Globals.mappings.put(Thread.currentThread(), Globals.trolleyID.addAndGet(1));
            logger.super.TrolleyAcquireEvent(Thread.currentThread(), Globals.mappings, waited);

            //wait time to get boxes -> cuz 1 stocker at staging area at a time
            startTick = EmulationClock.tick;

            LinkedList<BoxTypes> boxes_taken = stgArea.getBoxes(10 - myBoxes.size()); //box limit is hardcoded
            myBoxes.addAll(boxes_taken);
            Thread.sleep(EmulationClock.time_tick_size);

            waited = EmulationClock.tick - startTick;
            logger.super.LoadEvent(boxes_taken, waited, stockerId); // print LoadEvent

        } catch (InterruptedException e) {
        } finally {
            stgArea.lock.unlock();

        }

            //fork to make delivery in warehouse
            //TODO: warehouse forking
            int trolleyId = Globals.mappings.get(Thread.currentThread());
            WarehouseTask task = new WarehouseTask(myBoxes, trolleyId, stockerId);
            task.fork();
            LinkedList<BoxTypes> leftover = task.join(); // blocks until warehouse done
            if (!leftover.isEmpty()) {
                startTick = EmulationClock.tick;
                try {
                    Thread.sleep(EmulationClock.time_tick_size * (10 + leftover.size()));
                } catch (InterruptedException e) {

                }
                waited = EmulationClock.tick - startTick;
                logger.super.MoveEvent("warehouse", "staging", leftover.size(), trolleyId, waited, stockerId);
                logger.super.ReturnToStagingEvent(leftover.size(), trolleyId, waited);
                myBoxes = leftover; // carry leftovers into next loop iteration
            }
            else {
                myBoxes = new LinkedList<>();
                Globals.trolleys.release();
                logger.super.TrolleyReleaseEvent(Thread.currentThread(), Globals.mappings);
                // trolley has been released, now stocker can go on break
                if(stockerBreakEnabled){
                    if(new Random().nextDouble() < 1.0000 / stockerBreakTickValue ){
                        //log it
                        logger.super.StockerBreakStartEvent();
                        try {
                            Thread.currentThread().sleep(150 * EmulationClock.time_tick_size);
                        } catch (InterruptedException e) {

                        }
                        finally {
                            logger.super.StockerBreakEndEvent();
                        }
                    }
                }
            }



    }

            return null;

    }

    static void StartStocking(int stockers) {

        for(int i = 0 ; i<stockers; i++){
            Globals.fjPool.execute(new
                    Stocker(10, 10)); // create 10 trolleys , 10 boxes max 1 can have

        }

    }

}