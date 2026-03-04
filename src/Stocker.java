import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//use it at last to startStocking function
class Globals {
    static Semaphore trolleys = new Semaphore(10);
    static AtomicInteger trolleyID = new AtomicInteger(0);
    static ConcurrentHashMap<Thread, Integer> mappings = new ConcurrentHashMap<>();

    static ForkJoinPool fjPool = new
            ForkJoinPool();
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
        System.out.println("Tick: "+EmulationClock.tick+" Event: acquire_trolley "+"Trolley_id: "+mappings.get(t.currentThread()) + " waited: "+waited+" tick(s)");
    }

    default void TrolleyReleaseEvent(Thread t,ConcurrentHashMap mappings){
        System.out.println("Tick: "+EmulationClock.tick+" Event: release_trolley "+"Trolley_id: "+mappings.get(t.currentThread()));
    }

    default  void LoadEvent(LinkedList<String> boxes_taken_from_stagingArea, int waited )
    {
        System.out.println("Tick: "+EmulationClock.tick+" Event: stocker_load "+boxes_taken_from_stagingArea+ " waited: "+waited+" tick(s)");

    }
}

// TODO: 1. Take boxes from Staging area
// TODO: 2. Make Stocker follow singleton pattern too
// create that Global class forjoin -> call it from main.java where clock is runnning
public class Stocker extends RecursiveTask<Boolean> implements Trolley,logger {

    static int box_limit;
    static int number  ; // user input
    StagingArea stgArea = StagingArea.getInstance();  // have entire access as a stocker to staging area




    Stocker(int box_limit, int number)
    {
        this.box_limit = box_limit;
        this.number = number;

    }



    /*
    Task 1:
    * 1. Acquire trolley semaphore -> Log event
    * 2. Get 10 boxes then fork so that next thread can take it -> log events
    * */
    @Override
    protected Boolean compute() {
        LinkedList<String> myBoxes = new LinkedList<>(); // stocker's boxes taken from staging area
        int startTick = EmulationClock.tick;


        //waited = EmulationClock.tick; // this one should be 0 tick at start
        while(stgArea.stagingArea.isEmpty() && myBoxes.isEmpty()){
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
            while(stgArea.stagingArea.isEmpty()) {
                stgArea.lock_condition.await(); // sleeps, releases lock, wakes on signalAll
            }
            int waited = EmulationClock.tick - startTick;


            Globals.trolleys.acquire();
            Globals.mappings.put(Thread.currentThread(), Globals.trolleyID.addAndGet(1));
            logger.super.TrolleyAcquireEvent(Thread.currentThread(), Globals.mappings, waited);

            //wait time to get boxes -> cuz 1 stocker at staging area at a time
            startTick = EmulationClock.tick;

            LinkedList<String> boxes_taken = stgArea.getBoxes(10 - myBoxes.size());
            myBoxes.addAll(boxes_taken);
            Thread.sleep(EmulationClock.time_tick_size);

            waited = EmulationClock.tick - startTick;
            logger.super.LoadEvent(boxes_taken, waited); // print LoadEvent
            //fork to make delivery in warehouse
            //TODO: warehouse
        } catch (InterruptedException e) {
        } finally {
            stgArea.lock.unlock();
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