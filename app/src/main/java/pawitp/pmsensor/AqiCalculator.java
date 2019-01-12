package pawitp.pmsensor;

public class AqiCalculator {

    private static class AqiRow {
        final double rawLower;
        final double rawUpper;
        final double aqiLower;
        final double aqiUpper;

        private AqiRow(double rawLower, double rawUpper, double aqiLower, double aqiUpper) {
            this.rawLower = rawLower;
            this.rawUpper = rawUpper;
            this.aqiLower = aqiLower;
            this.aqiUpper = aqiUpper;
        }
    }

    public static AqiRow[] PM_2_5_AQI = new AqiRow[]{
            new AqiRow(0, 12.0, 0, 50),
            new AqiRow(12.1, 35.4, 51, 100),
            new AqiRow(35.5, 55.4, 101, 150),
            new AqiRow(55.5, 150.4, 151, 200),
            new AqiRow(150.5, 250.4, 201, 300),
            new AqiRow(250.5, 350.4, 301, 400),
            new AqiRow(350.5, 500.4, 401, 500),
    };

    public static AqiRow[] PM_10_AQI = new AqiRow[]{
            new AqiRow(0, 54, 0, 50),
            new AqiRow(55, 154, 51, 100),
            new AqiRow(155, 254, 101, 150),
            new AqiRow(255, 354, 151, 200),
            new AqiRow(355, 424, 201, 300),
            new AqiRow(425, 504, 301, 400),
            new AqiRow(505, 604, 401, 500),
    };

    public static class AqiValue {
        public final int value;
        public final int color;

        private AqiValue(int value, int color) {
            this.value = value;
            this.color = color;
        }
    }

    private static AqiValue[] AQI_COLORS = new AqiValue[]{
            new AqiValue(50, R.color.aqi50),
            new AqiValue(100, R.color.aqi100),
            new AqiValue(150, R.color.aqi150),
            new AqiValue(200, R.color.aqi200),
            new AqiValue(300, R.color.aqi300),
            new AqiValue(500, R.color.aqi500),
    };

    public static AqiValue getAqi(int rawValue, AqiRow[] aqiTable) {
        int aqi = calculateAqi(rawValue, aqiTable);
        int color = getColor(aqi);
        return new AqiValue(aqi, color);
    }

    private static int calculateAqi(int rawValue, AqiRow[] aqiTable) {
        for (AqiRow row : aqiTable) {
            if (rawValue < row.rawUpper) {
                return (int) (((row.aqiUpper - row.aqiLower) / (row.rawUpper - row.rawLower)) *
                        (rawValue - row.rawLower) + row.aqiLower);
            }
        }
        return 0;
    }

    private static int getColor(int aqi) {
        for (AqiValue row : AQI_COLORS) {
            if (aqi <= row.value) return row.color;
        }
        return 0;
    }

}
