package com.sentinelops.model.snapshot;

/**
 * One line from df -h (filesystem, size, used, avail, use%, mount).
 */
public class DiskUsage {
    private String filesystem;
    private String size;
    private String used;
    private String avail;
    private String usePercent;
    private String mountedOn;

    public DiskUsage() {}

    public DiskUsage(String filesystem, String size, String used, String avail, String usePercent, String mountedOn) {
        this.filesystem = filesystem;
        this.size = size;
        this.used = used;
        this.avail = avail;
        this.usePercent = usePercent;
        this.mountedOn = mountedOn;
    }

    public String getFilesystem() { return filesystem; }
    public void setFilesystem(String filesystem) { this.filesystem = filesystem; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getUsed() { return used; }
    public void setUsed(String used) { this.used = used; }
    public String getAvail() { return avail; }
    public void setAvail(String avail) { this.avail = avail; }
    public String getUsePercent() { return usePercent; }
    public void setUsePercent(String usePercent) { this.usePercent = usePercent; }
    public String getMountedOn() { return mountedOn; }
    public void setMountedOn(String mountedOn) { this.mountedOn = mountedOn; }
}
