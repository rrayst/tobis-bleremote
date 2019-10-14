package de.tobiaspolley.bleremote.jobs;

public abstract class Job {
    boolean wantsResponse;

    public Job(boolean wantsResponse) {
        this.wantsResponse = wantsResponse;
    }

    public boolean isWantsResponse() {
        return wantsResponse;
    }

    public boolean canReplaceOtherJob() {
        return false;
    }

    public boolean canReplaceOtherJob(Job job) {
        return false;
    }
}
