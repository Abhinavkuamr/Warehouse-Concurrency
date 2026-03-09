import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.Semaphore;

// multiple objects = multiple sections
class Section {
    BoxTypes type;
    int limit;
    LinkedList<BoxTypes> storage = new LinkedList<>();
    Semaphore stockerLock = new Semaphore(1);

    Section(BoxTypes type, int limit) {
        this.type = type;
        this.limit = limit;
    }

    boolean isFull() { return storage.size() >= limit; }

    // TODO: picker method to pick boxes
}


// Singleton -> There is only 1 warehouse
class Warehouse implements logger {
    private static Warehouse warehouse;

    HashMap<BoxTypes, Section> sectionMap = new HashMap<>();

    //TODO: section limit should be given by user not hardcoded
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

    //TODO: Each section stocking of 1 box takeks 1 tick

    LinkedList<BoxTypes> stockBoxes(LinkedList<BoxTypes> myBoxes, int trolleyId) throws InterruptedException {
        HashMap<BoxTypes, LinkedList<BoxTypes>> grouped = new HashMap<>();
        for (BoxTypes box : myBoxes) {
            grouped.computeIfAbsent(box, k -> new LinkedList<>()).add(box);
        }

        LinkedList<BoxTypes> leftover = new LinkedList<>();
        String previousSection = "staging";
        int totalStocked = 0;

        // do for all keys/box types that stocker has
        for (BoxTypes type : grouped.keySet()) {
            LinkedList<BoxTypes> boxesOfType = grouped.get(type);
            int currentLoad = myBoxes.size() - leftover.size() - totalStocked;

            int startTick = EmulationClock.tick;
            // if im moving from 1 section to other , i have to wait again for 10 ticks + no. of boxes * tick size
            if (!previousSection.equals("staging")) {
                Thread.sleep(EmulationClock.time_tick_size * (10 + currentLoad));
            }
            int waited = EmulationClock.tick - startTick;

            logger.super.MoveEvent(previousSection, type.toString(), currentLoad, trolleyId, waited);

            Section section = sectionMap.get(type); // get the object of that particular section

            // if another stocker acquired already then watiting preiod starts -> calculate it
            startTick = EmulationClock.tick;
            section.stockerLock.acquire(); // either wait or go forward
            waited = EmulationClock.tick - startTick;

            logger.super.StockBeginEvent(type.toString(), boxesOfType.size(), trolleyId, waited);

            int stocked = 0;
            try {
                for (BoxTypes box : boxesOfType) {
                    if (section.isFull()) {
                        leftover.add(box); // if section is full , populate leftover for that particular thread
                        continue;
                    }
                    section.storage.add(box);
                    Thread.sleep(EmulationClock.time_tick_size); // 1 tick for each box stocking
                    stocked++;
                }
            } finally {
                section.stockerLock.release();
            }

            logger.super.StockEndEvent(type.toString(), stocked, currentLoad - stocked, trolleyId);

            previousSection = type.toString(); // change previous -> so that stocker can wait (travel time)
            totalStocked += stocked;

        }



        // leftover = boxes that couldn't fit
        // TODO: stocker takes these back to staging area
        if (!leftover.isEmpty()) {
            int startTick = EmulationClock.tick;
            Thread.sleep(EmulationClock.time_tick_size * (10 + leftover.size()));
            int waited = EmulationClock.tick - startTick;
            logger.super.MoveEvent(previousSection, "staging", leftover.size(), trolleyId, waited);
        }
        return leftover; // return leftover to master thread
    }


    // TODO: picker functions


}


class WarehouseTask extends RecursiveTask<LinkedList<BoxTypes>> implements logger {
    LinkedList<BoxTypes> boxes;
    int trolleyId;
    Warehouse warehouse = Warehouse.getInstance();

    WarehouseTask(LinkedList<BoxTypes> boxes, int trolleyId) {
        this.boxes = boxes;
        this.trolleyId = trolleyId;
    }

    @Override
    protected LinkedList<BoxTypes> compute() {
        try {
            int startTick = EmulationClock.tick;
            Thread.sleep(EmulationClock.time_tick_size * (10 + boxes.size())); // goto warehouse waiting time
            int waited = EmulationClock.tick - startTick;
            logger.super.MoveEvent("staging", "warehouse", boxes.size(), trolleyId, waited);
            return warehouse.stockBoxes(boxes, trolleyId);
        } catch (InterruptedException e) {
        }
        return new LinkedList<>();
    }
}