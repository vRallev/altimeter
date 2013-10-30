package net.vrallev.android.altimeter;

import net.vrallev.android.base.App;
import net.vrallev.android.base.util.Cat;

/**
 * @author Ralf Wondratschek
 */
public class AppExtension extends App {

    @Override
    public void onCreate() {
        super.onCreate();
        Cat.setDefaultInstance(true);
    }
}
