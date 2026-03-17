import javax.swing.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// multiple objects = multiple sections
class Section {
    BoxTypes type;
    int limit;
    LinkedList<BoxTypes> storage = new LinkedList<>();
    Semaphore stockerLock = new Semaphore(1);

    //for Pickers - Add multiple semaphores;
    // REad and write reentrant locks ??
    ReentrantLock storageLock = new ReentrantLock();
    Condition lock_condition = storageLock.newCondition();

    //each warehousesection can allow 10 pickers at a time
    Semaphore pickerPermits = new Semaphore(10);// TODO: controlled by user
    AtomicInteger waitingPickers = new AtomicInteger(0);





    Section(BoxTypes type, int limit) {
        this.type = type;
        this.limit = limit;
    }

    boolean isFull() { return storage.size() >= limit; }

}


// Singleton -> There is only 1 warehouse
class Warehouse implements logger {
    private static Warehouse warehouse;

    public static Boolean priorityEnabled;

    HashMap<BoxTypes, Section> sectionMap = new HashMap<>();

    //TODO: warehousesection limit should be given by user not hardcoded
    Section electronics = new Section(BoxTypes.electronics, 100);
    Section tools = new Section(BoxTypes.tools, 100);
    Section meds = new Section(BoxTypes.medical, 100);
    Section animeCd = new Section(BoxTypes.animeCD, 100);
    Section movieCd = new Section(BoxTypes.moviesCD, 100);

    private Warehouse() {
        sectionMap.put(BoxTypes.electronics, electronics);
        sectionMap.put(BoxTypes.tools, tools);
        sectionMap.put(BoxTypes.medical, meds);
        sectionMap.put(BoxTypes.animeCD, animeCd);
        sectionMap.put(BoxTypes.moviesCD, movieCd);
    }

    public static Warehouse getInstance() {
        if (warehouse == null) {
            warehouse = new Warehouse();
            return warehouse;
        }
        return warehouse;
    }

    //TODO: Each warehousesection stocking of 1 box takeks 1 tick

    LinkedList<BoxTypes> stockBoxes(LinkedList<BoxTypes> myBoxes, int trolleyId, int stockerId) throws InterruptedException {
        HashMap<BoxTypes, LinkedList<BoxTypes>> grouped = new HashMap<>();
        for (BoxTypes box : myBoxes) {
            // if not BoxType not present (for 1st attempt) create new linkedlist else just add  box
            grouped.computeIfAbsent(box, k -> new LinkedList<>()).add(box);
        }

        LinkedList<BoxTypes> leftover = new LinkedList<>();
        String previousSection = "staging";
        int totalStocked = 0;

        Iterable<BoxTypes> orderToStock;

        if (!priorityEnabled) {
            orderToStock = grouped.keySet();
        } else {
            //just sort based on available permits for picker of a, and b
            LinkedList<BoxTypes> sortedTypes = new LinkedList<>(grouped.keySet());
            sortedTypes.sort((a, b) -> {

                Section sA = sectionMap.get(a);
                Section sB = sectionMap.get(b);
                // empty sections first
                boolean aEmpty = sA.storage.isEmpty();
                boolean bEmpty = sB.storage.isEmpty();
                if (aEmpty && !bEmpty) return -1; // a goes first
                if (!aEmpty && bEmpty) return 1;  // b goes first

                return sB.waitingPickers.get() - sA.waitingPickers.get();
            });
            orderToStock = sortedTypes;
        }
        // do for all keys/box types that stocker has
        for (BoxTypes type : orderToStock) {
            LinkedList<BoxTypes> boxesOfType = grouped.get(type);
            int currentLoad = myBoxes.size() - leftover.size() - totalStocked;

            int startTick = EmulationClock.tick;
            // if im moving from 1 warehousesection to other , i have to wait again for 10 ticks + no. of boxes * tick size
            if (!previousSection.equals("staging")) {
                Thread.sleep(EmulationClock.time_tick_size * (10 + currentLoad));
            }
            int waited = EmulationClock.tick - startTick;

            logger.super.MoveEvent(previousSection, type.toString(), currentLoad, trolleyId, waited, stockerId);

            Section section = sectionMap.get(type); // get the object of that particular warehousesection

            // if another stocker acquired already then watiting preiod starts -> calculate it
            startTick = EmulationClock.tick;
            //section.stockerLock.acquire(); // either wait or go forward
            section.pickerPermits.acquire(10); // blocks until all pickers leave
            section.stockerLock.acquire();
            waited = EmulationClock.tick - startTick;

            logger.super.StockBeginEvent(type.toString(), boxesOfType.size(), trolleyId, waited, stockerId);

            int stocked = 0;
            try {
                for (BoxTypes box : boxesOfType) {
                    if (section.isFull()) {
                        leftover.add(box); // if warehousesection is full , populate leftover for that particular thread
                        continue;
                    }
                    section.storage.add(box);
                    Thread.sleep(EmulationClock.time_tick_size); // 1 tick for each box stocking
                    stocked++;
                }
            } finally {
                section.stockerLock.release();
                section.pickerPermits.release(10); // pickers coming in
            }

            logger.super.StockEndEvent(type.toString(), stocked, currentLoad - stocked, trolleyId, stockerId);

            previousSection = type.toString(); // change previous -> so that stocker can wait (travel time)
            totalStocked += stocked;

        }



        // leftover = boxes that couldn't fit
        // TODO: stocker takes these back to staging area
        if (!leftover.isEmpty()) {
            int startTick = EmulationClock.tick;
            Thread.sleep(EmulationClock.time_tick_size * (10 + leftover.size()));
            int waited = EmulationClock.tick - startTick;
            logger.super.MoveEvent(previousSection, "staging", leftover.size(), trolleyId, waited, stockerId);
        }
        return leftover; // return leftover to master thread
    }


    // TODO: picker functions
    //Assumption: Picker always comes to warehousesection wtih empty trolley
    LinkedList<BoxTypes> pickBoxes(BoxTypes type) throws InterruptedException {
        Section sectionHandler = sectionMap.get(type);
        LinkedList<BoxTypes> pickup = new LinkedList<>();

        sectionHandler.storageLock.lock();
        try {
            int toPick = Math.min(sectionHandler.storage.size(), 10);
            for(int i = 0; i < toPick; i++){
                pickup.add(sectionHandler.storage.poll());
                Thread.sleep(EmulationClock.time_tick_size); // 1 tick per box
            }
        } finally {
            sectionHandler.storageLock.unlock();
        }
        return pickup;
    }


}


class WarehouseTask extends RecursiveTask<LinkedList<BoxTypes>> implements logger {
    private final int stockerId;
    LinkedList<BoxTypes> boxes;
    int trolleyId;
    Warehouse warehouse = Warehouse.getInstance();

    WarehouseTask(LinkedList<BoxTypes> boxes, int trolleyId, int stocker_id) {
        this.boxes = boxes;
        this.trolleyId = trolleyId;
        this.stockerId = stocker_id;

    }

    @Override
    protected LinkedList<BoxTypes> compute() {
        try {
            int startTick = EmulationClock.tick;
            Thread.sleep(EmulationClock.time_tick_size * (10 + boxes.size())); // goto warehouse waiting time
            int waited = EmulationClock.tick - startTick;
            logger.super.MoveEvent("staging", "warehouse", boxes.size(), trolleyId, waited, stockerId);            return warehouse.stockBoxes(boxes, trolleyId, stockerId);
        } catch (InterruptedException e) {
        }
        return new LinkedList<>();
    }
}