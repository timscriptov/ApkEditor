package com.gmail.heagoo.apkeditor.patch;

import java.util.ArrayList;
import java.util.List;

public class Patch {

    public static final int engineVersion = 1;
    // V1
    public int requiredEngine;
    public String author;
    public String packagename;
    public List<PatchRule> rules = new ArrayList<PatchRule>();

}
