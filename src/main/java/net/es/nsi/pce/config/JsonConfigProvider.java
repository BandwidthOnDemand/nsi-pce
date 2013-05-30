package net.es.nsi.pce.config;


import java.io.File;

public class JsonConfigProvider {
    private String filename;

    private long timeStamp;
    private File file;

    protected boolean isFileUpdated( File file ) {
        this.file = file;
        long lastModified = file.lastModified();

        if( this.timeStamp != lastModified ) {
            this.timeStamp = lastModified;
            //Yes, file is updated
            return true;
        }
        //No, file is not updated
        return false;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
