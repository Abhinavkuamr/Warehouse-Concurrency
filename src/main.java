import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;


public class main {
    public static void main() throws IOException {
        int stockers;
        int pickers;
        int trolleys;
        int tickSize;
        int day ;
        Boolean guiEnabled;
        Boolean priorityEnabled;



        //Credits: https://www.baeldung.com/java-properties
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
       // System.out.println(rootPath);
        String simulationPropertiesPath = rootPath + "/simulation.properties";
        Properties prop = new Properties();
        prop.load(new FileInputStream(simulationPropertiesPath));



            stockers = Integer.valueOf(prop.getProperty("stockers"));
            pickers = Integer.valueOf(prop.getProperty("pickers"));
            trolleys = Integer.valueOf(prop.getProperty("trolleys"));
            tickSize = Integer.valueOf(prop.getProperty("tickSize"));
            day = Integer.valueOf(prop.getProperty("day"));
            priorityEnabled = Boolean.valueOf(prop.getProperty("priorityEnabled")); //TODO: implementation not done
            Stocker.stockerBreakEnabled = Boolean.valueOf(prop.getProperty("stockerBreakEnabled"));
            StagingArea.deliveryProbablity = Double.valueOf(prop.getProperty("deliveryProbablity"));
            Picker.pickAttemptyPerDay = Integer.valueOf(prop.getProperty("pickAttemptyPerDay"));
        Stocker.stockerBreakTickValue = Integer.valueOf(prop.getProperty("stockerBreakTickValue"));
            Warehouse.priorityEnabled = Boolean.valueOf(prop.getProperty("priorityEnabled"));
        Warehouse.electronicsSectionCapacity = Integer.valueOf(prop.getProperty("electronicsSectionCapacity"));
        Warehouse.toolsSectionCapacity = Integer.valueOf(prop.getProperty("toolsSectionCapacity"));
        Warehouse.medsSectionCapacity = Integer.valueOf(prop.getProperty("medsSectionCapacity"));
        Warehouse.animeCdSectionCapacity = Integer.valueOf(prop.getProperty("animeCdSectionCapacity"));
        Warehouse.movieCdSectionCapacity = Integer.valueOf(prop.getProperty("movieCdSectionCapacity"));










        //TODO: Add params from user input
        // Trolley has a hardcoded limit of 10
        // making delivery in Clock
        EmulationClock clk = EmulationClock.getInstance(tickSize,day);
        Globals.init(trolleys); // from spinner value
        Stocker.StartStocking(stockers); // make it asynchronus so that clock doesnt stop ticking execute() or submit() not invoke
        Picker.StartPicking(pickers); // pickers - initially just given 5
        while(clk.getTick() != clk.Complete()){
            clk.updateTick();

        }





    }
}
