package com.yingtu.plus.com.yingtu.plus.bean.drive;

import java.util.List;

public class DriveCheckResponse {

    private final List<DriveCheckResult> results;

    public DriveCheckResponse(List<DriveCheckResult> results) {
        this.results = results;
    }

    public List<DriveCheckResult> getResults() {
        return results;
    }
}
