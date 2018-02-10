package java.com.example.shreyan.tartanhacks;

/**
 * Created by ravi on 26/09/17.
 */

public class Item {
    static int id;
    static String name;
    static String description;
    static double time;

    public Item() {
    }

    public Item (int id, String name, String description, double time) {
        Item.id = id;
        Item.name = name;
        Item.description = description;
        Item.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTime() {
        return time;
    }

    public void setPrice(double price) {
        this.time = price;
    }

}
