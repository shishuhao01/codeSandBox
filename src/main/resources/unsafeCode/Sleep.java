//恶意占用系统资源，占用系统时间
public class Main{
    public static void main(String[] args) throws Exception{
        long OUT_TIME = 60 * 60 * 1000;
        Thread.sleep(OUT_TIME);
        System.out.println("苏醒了");
    }
}
