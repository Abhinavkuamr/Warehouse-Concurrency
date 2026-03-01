import java.util.LinkedList;

public class DeliveryArea {
    private LinkedList stagingArea = new LinkedList();

    public void MakeDelivery()
    {
        //StagingArea get boxes
    }

    // 1 stocker at a time
    public synchronized LinkedList GetBoxes()
    {
        return new LinkedList();
    }

}
