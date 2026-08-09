package com.roadscanner.cmn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 공통 애플리케이션 로거입니다. */
public interface AppLogger {

    Logger LOG = LogManager.getLogger(AppLogger.class);
}
