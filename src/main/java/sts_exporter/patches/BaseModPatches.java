package sts_exporter.patches;

import java.util.HashMap;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;

import basemod.BaseMod;

public class BaseModPatches {
    public static HashMap<String,String> keywordClasses = new HashMap<>();

    @SpirePatch(clz = BaseMod.class, method="addKeyword", paramtypez={String.class, String.class, String[].class, String.class})
    public static class AddKeyword {
        public static void Postfix(String modId, String proper, String[] names, String description) {
            // A keyword was added, figure out which mod did it.
            String caller = getCallingClassName();
            if (caller != null) {
                keywordClasses.put(names[0], caller);
            }
        }
    }

    private static String getCallingClassName() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement step : trace) {
            if (!(step.getClassName().equals("java.lang.Thread") && step.getMethodName().equals("getStackTrace")) &&
                !(step.getClassName().equals("basemod.BaseMod") && step.getMethodName().equals("addKeyword")) &&
                !(step.getClassName().startsWith("sts_exporter.patches"))) {
                return step.getClassName();
            }
        }
        return null;
    }
}