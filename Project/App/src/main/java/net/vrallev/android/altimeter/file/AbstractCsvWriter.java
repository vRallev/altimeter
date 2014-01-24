package net.vrallev.android.altimeter.file;

import android.content.Context;

import net.vrallev.android.base.util.Cat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ralf Wondratschek
 */
public abstract class AbstractCsvWriter<E> {

    private final File mFile;
    private final List<E> mEntries;

    private WriterThread mThread;
    private BufferedWriter mWriter;

    public AbstractCsvWriter(Context context, String fileName) {
        mEntries = new ArrayList<>();

        File dir = context.getExternalFilesDir(null);
        mFile = new File(dir, fileName);
    }

    public void startWriting() {
        if (mThread != null) {
            return;
        }

        mThread = new WriterThread();
        mThread.start();
    }

    public void stopWriting() {
        if (mThread == null) {
            return;
        }

        mThread.stopWrite();
        mThread.interrupt();
        mThread = null;
    }

    public File getFile() {
        return mFile;
    }

    public void addEntry(E event) {
        synchronized (mEntries) {
            mEntries.add(event);
            mEntries.notifyAll();
        }
    }

    protected abstract void writeEvent(E event) throws IOException;
    protected abstract String getHeader();

    protected void writeLine(String text) throws IOException {
        mWriter.write(text + "\n");
    }

    private class WriterThread extends Thread {

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
            if (!mFile.exists()) {
                if (!mFile.createNewFile()) {
                    throw new IOException();
                }
            }

            mWriter = new BufferedWriter(new FileWriter(mFile));

            writeLine(getHeader());

            while (mRunning) {
                synchronized (mEntries) {
                    while (!mEntries.isEmpty()) {
                        writeEvent(mEntries.remove(0));
                    }

                    try {
                        mEntries.wait();
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
