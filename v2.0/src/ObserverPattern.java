public class ObserverPattern {
    public static void main(String[] args) {
        Neu neu = new Neu();
        neu.display();

    }
}
class Neu{
    private float temperature;
    public  void  update(float temperature){
       this.temperature = temperature;
       display();
    }
    public void display(){
        System.out.println("观察者模式" + temperature);
    }
}
class WeatherDate{
    private float temperature;

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}
