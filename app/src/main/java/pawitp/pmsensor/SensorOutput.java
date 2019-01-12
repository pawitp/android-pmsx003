package pawitp.pmsensor;

public class SensorOutput {

    public final int pm1_0;
    public final int pm2_5;
    public final int pm10;

    public SensorOutput(int pm1_0, int pm2_5, int pm10) {
        this.pm1_0 = pm1_0;
        this.pm2_5 = pm2_5;
        this.pm10 = pm10;
    }

}
