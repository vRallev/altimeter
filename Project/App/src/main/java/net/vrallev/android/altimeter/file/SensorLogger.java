package net.vrallev.android.altimeter.file;

import android.app.Fragment;
import android.hardware.SensorEvent;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
public class SensorLogger extends AbstractCsvWriter<SensorEvent> {

    public SensorLogger(Fragment fragment) {
        super(fragment.getActivity(), fragment.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".csv");
    }

    protected void writeEvent(SensorEvent event) throws IOException {
        // Cat.d(String.format(Locale.US, "%f;%f;%f", event.values[0], event.values[1], event.values[2]));
        writeLine(String.format(Locale.GERMANY, "%f;%f;%f", event.values[0], event.values[1], event.values[2]));
    }

    @Override
    protected String getHeader() {
        return "x;y;z";
    }
}
