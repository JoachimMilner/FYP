package connectionUtils;

/**
 * This code is taken from: http://stackoverflow.com/a/9201081
 * Allows for an exponential moving average to be calculated
 * @author Joachim
 *
 */
public class ExponentialMovingAverage {
    private double alpha;
    private Double oldValue;
    public ExponentialMovingAverage(double alpha) {
        this.alpha = alpha;
    }

    public double average(double value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        double newValue = oldValue + alpha * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }
}
