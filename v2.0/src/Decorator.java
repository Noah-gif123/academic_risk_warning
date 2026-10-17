public class Decorator {
    public static void main(String[] args) {
        Espresso beverage = new Espresso();
        System.out.println("装饰着模式");
    }
}
    abstract class Beverage{
       public String description = "未知饮料";
       public abstract double cost();
    }

    class Espresso extends  Beverage{
        public Espresso(){

        }
    }


