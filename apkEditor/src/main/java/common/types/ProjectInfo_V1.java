package common.types;

import java.io.Serializable;

public class ProjectInfo_V1 implements Serializable {

    // The original apk we are editing
    public String apkPath;

    public String decodeRootPath;
    public ActivityState_V1 state;
}
