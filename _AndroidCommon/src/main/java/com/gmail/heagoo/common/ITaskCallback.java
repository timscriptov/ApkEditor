package com.gmail.heagoo.common;

public interface ITaskCallback {

    public void setTaskStepInfo(TaskStepInfo stepInfo);

    // Progress from 0 to 1
    public void setTaskProgress(float progress);

    public void taskSucceed();

    public void taskFailed(String errMessage);

    // Call this function when not a genuine version
    public void taskWarning(String message);

    public static class TaskStepInfo {
        public int stepIndex = 0;
        public int stepTotal;
        public String stepDescription;
    }
}
