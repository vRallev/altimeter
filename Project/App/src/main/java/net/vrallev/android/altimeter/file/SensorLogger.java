package net.vrallev.android.altimeter.file;

import android.app.Fragment;
import android.hardware.SensorEvent;

import net.vrallev.android.base.util.Cat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("ConstantConditions")
public class SensorLogger {

    private final List<SensorEvent> mSensorEvents;
    private LoggingThread mThread;

    private final File mFile;

    private BufferedWriter mWriter;

    public SensorLogger(Fragment fragment) {
        mSensorEvents = new ArrayList<>();

        File dir = fragment.getActivity().getExternalFilesDir(null);
        mFile = new File(dir, fragment.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".csv");
    }

    public void startLogger() {
        if (mThread != null) {
            return;
        }

        mThread = new LoggingThread();
        mThread.start();
    }

    public void stopLogger() {
        if (mThread == null) {
            return;
        }

        mThread.stopWrite();
        mThread.interrupt();
        mThread = null;
    }

    public void logEvent(SensorEvent event) {
        synchronized (mSensorEvents) {
            mSensorEvents.add(event);
            mSensorEvents.notifyAll();
        }
    }

    private void writeEvent(SensorEvent event) throws IOException {
//        Cat.d(String.format(Locale.US, "%f;%f;%f", event.values[0], event.values[1], event.values[2]));
        mWriter.write(String.format(Locale.GERMANY, "%f;%f;%f\n", event.values[0], event.values[1], event.values[2]));
    }

    private void writeString(String text) throws IOException {
//        Cat.d(text);
        mWriter.write(text + "\n");
    }

    private class LoggingThread extends Thread {

        private boolean mRunning = true;

        @Override
        public void run() {
            try {
                innerRun();
            } catch (IOException e) {
                Cat.e(e);
                if (mWriter != null) {
                    try {
                        mWriter.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        private void innerRun() throws IOException {
            if (mFile.exists()) {
                if (!mFile.createNewFile()) {
                    throw new IOException();
                }
            }

            mWriter = new BufferedWriter(new FileWriter(mFile));

            writeString("x;y;z");

            while (mRunning) {
                synchronized (mSensorEvents) {
                    while (!mSensorEvents.isEmpty()) {
                        writeEvent(mSensorEvents.remove(0));
                    }

                    try {
                        mSensorEvents.wait();
                    } catch (InterruptedException e) {
                        // will stop
                    }
                }
            }

            mWriter.close();
            mWriter = null;

            Cat.d("Thread finished");
        }

        public void stopWrite() {
            mRunning = false;
        }
    }
}
