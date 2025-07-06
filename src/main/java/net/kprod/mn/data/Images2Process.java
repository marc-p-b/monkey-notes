package net.kprod.mn.data;

import java.util.ArrayList;
import java.util.List;

public class Images2Process {
    private List<Image2Process> listImage2Process = new ArrayList<>();

    private int changeAfterPageNumber = 0;

    public List<Image2Process> getListImage2Process() {
        return listImage2Process;
    }

    public Images2Process setListImage2Process(List<Image2Process> listImage2Process) {
        this.listImage2Process = listImage2Process;
        return this;
    }

    public int getChangeAfterPageNumber() {
        return changeAfterPageNumber;
    }

    public Images2Process setChangeAfterPageNumber(int changeAfterPageNumber) {
        this.changeAfterPageNumber = changeAfterPageNumber;
        return this;
    }
}