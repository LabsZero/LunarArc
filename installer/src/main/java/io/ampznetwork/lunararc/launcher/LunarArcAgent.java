package io.ampznetwork.lunararc.launcher;

import java.lang.instrument.Instrumentation;

public class LunarArcAgent {

    public static volatile Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
    }
}
