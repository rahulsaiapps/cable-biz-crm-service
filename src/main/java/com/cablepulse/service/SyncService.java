package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.StandardResponse_SyncResolution;
import com.cablepulse.dto.DtoClasses.SyncRequestPayload;

public interface SyncService {

    StandardResponse_SyncResolution processSyncBatch(SyncRequestPayload payload);
}
