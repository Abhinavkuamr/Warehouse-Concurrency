import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//singleton
public class StagingArea {
    private Queue<String> stagingArea = new LinkedList<>(); // Shared Memory
    public static StagingArea stgArea;

    ReentrantLock lock = new ReentrantLock();  // acquire or release locks for critical sections
    Condition lock_condition = lock.newCondition(); // to use - await() , wait() , signal(), signalAll() for lock

    private StagingArea(){}

    public static StagingArea getInstance(){
        if(stgArea == null){
            stgArea = new StagingArea();
            return stgArea;
        }
        return stgArea;
    }


    // after each tick call makeDelivery -> calculate the probability of boxes being delivered
    public void makeDelivery()
    {
        //StagingArea get boxes; use random to delivery 10 boxes after every 100 tick (average)
        //delivery happens every 100 ticks on average, -> per probability is 1/100 = 0.01
        Random rand = new Random(); // gives value between 0 and 1
        //rand.nextDouble() > 0.01 is true 99% of the time
        //That would deliver almost every tick so < 0.01 should suffice ig
        // ticks * probablity = number of delivery 1000 * 0.01 = 10 in 1 day
        if(rand.nextDouble() < 0.01){
            //make a delivery
            LinkedList<BoxTypes> temp = BoxTypes.getRandomBoxes();
            for(var i : temp){
                stagingArea.add(String.valueOf(i));
            }
            System.out.println("tick: "+EmulationClock.tick +" Delivery Made: "+temp);


        }
    }

    //Flow: If 3 threads comes , t1 locks sees size is 0 releas +  sleep, t2 locks sees 0 release + goes to sleep , t3 locks sees0, release +  goes to sleep
    // if a section gets empty , t1 t2 t3 can be awaken by notifyall and they all can try to satisfy that other condition too
    // 1 stocker at a time
    //TODO: Need changes -> this design might result in LiveLock is possible
    public LinkedList<String> getBoxes(int number)
    {
        LinkedList<String> boxes = new LinkedList<>(); // No need to protect this

        lock.lock();  // acquire the lock
        try {
            while(stagingArea.isEmpty()){
                 lock_condition.await();//release Lock + goto Sleep, if stagingArea is empty; if not empty re-acquire the lock
            }
            // critical section starts
            int totalBoxPossible = Math.min(stagingArea.size(), number);
            for(int i = 0; i< totalBoxPossible; i++){
                boxes.add(stagingArea.poll());
            }
            //critical section ends
            lock_condition.signalAll(); // make all waiting threads awake ; DONT USE signal() -> might awake some different thread that is not here => livelockfor staging area for awhile
            //signal before releasing lock otherwise illegalMonitorStateException
        } catch (Exception e) {
                System.err.println("Exception: "+e);
        } finally {
            lock.unlock(); // release the lock;

        }
        return boxes;

    }

}
