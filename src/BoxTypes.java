import java.util.LinkedList;
import java.util.Random;

public enum BoxTypes {
    electronics,
    medical,
    tools,
    animeCD,
    moviesCD;

    // control number of boxes needed
    public static LinkedList<BoxTypes> getRandomBoxes(int count) {
        LinkedList<BoxTypes> boxes = new LinkedList<>();
        BoxTypes[] values = BoxTypes.values(); // arrays of enums.values
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            boxes.add(values[rand.nextInt(values.length)]); // gives between 0 to values.length()
        }

        return boxes;
    }

    // No control over no. of box delivery
    public static LinkedList<BoxTypes> getRandomBoxes() {
        return getRandomBoxes(10);
    }
}