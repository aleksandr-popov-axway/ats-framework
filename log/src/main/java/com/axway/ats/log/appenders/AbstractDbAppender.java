/*
 * Copyright 2017-2022 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axway.ats.log.appenders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.axway.ats.log.autodb.events.InsertMessageEvent;
import com.axway.ats.log.autodb.model.AbstractLoggingEvent;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.autodb.DbAppenderConfiguration;
import com.axway.ats.log.autodb.events.GetCurrentTestCaseEvent;
import com.axway.ats.log.autodb.exceptions.DbAppenederException;
import com.axway.ats.log.autodb.exceptions.InvalidAppenderConfigurationException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender is capable of arranging the database storage and storing
 * messages into it. It works on the Test Executor side.
 */
public abstract class AbstractDbAppender extends AppenderSkeleton {

    /**
     * indicates whether run/suites/testcases are running in parallel
     */
    public static boolean             parallel              = false;
    /** whether warning for possible deadlock (related to dbcp2 log severity) should be logged in console */
    private static boolean            logDeadlockWarning    = true;

    /** 
     * the channels for each test case. Concurrent because of LogAspect is writing on new Thread creation
     */
    private Map<String, DbChannel> channels = new ConcurrentHashMap<String, DbChannel>();

    protected AtsConsoleLogger     atsConsoleLogger = new AtsConsoleLogger(getClass());
    

    /**
     * Holds information about the parent of each thread:
     * <p>  child thread id : parent thread id </p>
     *
     * It is populated by the @After method in {@link com.axway.ats.log.aspect.LogAspect} AspectJ Java class
     */
    public static Map<String, String> childParentThreadsMap = new HashMap<>();

    /**
     * The configuration for this appender
     */
    protected DbAppenderConfiguration appenderConfig;

    /**
     * Constructor
     */
    public AbstractDbAppender() {

        super();

        // init the appender configuration
        // it will be populated when the setters are called
        this.appenderConfig = new DbAppenderConfiguration();

        logWarningForPossibleDealock();
    }

    private void logWarningForPossibleDealock() {

        if (logDeadlockWarning) {
            Logger dbcp2Logger = Logger.getLogger("org.apache.commons.dbcp2");
            if (dbcp2Logger != null) {
                if (dbcp2Logger.isEnabledFor(Level.DEBUG)) {
                    atsConsoleLogger.warn("Logger '" + dbcp2Logger.getName() + "' has logging level '"
                                          + Level.DEBUG.toString() + "'. This may cause deadlock. Please raise severity to INFO or higher.");
                }
            }
            logDeadlockWarning = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions() {

        // check whether the configuration is valid first
        try {
            this.appenderConfig.validate();
        } catch (InvalidAppenderConfigurationException iace) {
            throw new DbAppenederException(iace);
        }

        // set the threshold if there is such
        if (getThreshold() != null) {
            this.appenderConfig.setLoggingThreshold(getThreshold().toInt());
        }
    }

    protected abstract String getDbChannelKey( LoggingEvent event );

    protected DbChannel getDbChannel( LoggingEvent event ) {

        String channelKey = getDbChannelKey( event );

        DbChannel channel = this.channels.get( channelKey );
        if (channel == null) {
            // check if TestNG does NOT run in parallel
            if (!parallel && ! (this instanceof PassiveDbAppender)) { // if this is not a PassiveDbAppender and we are not in parallel mode
                // see if there is at least one db channel created
                if (!this.channels.isEmpty()) {
                    // get the first channel from the map
                    return this.channels.get(this.channels.keySet().iterator().next());
                }
            } else { // new thread, spawned in thread (non-TestNG thread)
                if ((event instanceof InsertMessageEvent) || ((event instanceof AbstractLoggingEvent) == false)) {
                    // the event is only of class InsertMessageEvent
                    if (childParentThreadsMap.containsKey(channelKey)) {
                        String threadId = childParentThreadsMap.get(channelKey);
                        do {
                            channel = channels.get(threadId/*childParentThreadsMap.get(threadId)*/);
                            if (channel != null) {
                                return channel;
                            }
                            threadId = childParentThreadsMap.get(threadId);
                        } while (threadId != null);
                    }
                }
            }
            /* 
             * We've ended up here because:
             * - TestNG runs in parallel or
             * - the channels map is empty or
             * - the parent of the current thread is not associated with any of the already created DbChannel(s)
            */
            // TODO inss
            atsConsoleLogger.info("Creating new DbChannel for channel: " + channelKey); // TODOs lower severity
            channel = new DbChannel(this.appenderConfig );
            channel.initialize( atsConsoleLogger, this.layout, true );
            // check whether the configuration is valid first
            try {
                this.appenderConfig.validate();
            } catch (InvalidAppenderConfigurationException iace) {
                throw new DbAppenederException(iace);
            }
            this.channels.put( channelKey, channel );
        }

        return channel;
    }

    protected void destroyDbChannel( String channelKey ) {

        //DbChannel channel = this.channels.get( channelKey );
        this.channels.remove( channelKey );
    }

    /**
     * Destroy all DB channels
     * 
     * @param waitForQueueToProcessAllEvents whether to wait for each channel's queue logger thread to finish execution of all events
     * */
    protected void destroyAllChannels( boolean waitForQueueToProcessAllEvents ) {

        for (DbChannel channel : channels.values()) {
            if (waitForQueueToProcessAllEvents) {
                channel.waitForQueueToProcessAllEvents();
            }
        }
        channels.clear();
    }

    public abstract GetCurrentTestCaseEvent getCurrentTestCaseState( GetCurrentTestCaseEvent event );

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#close()
     */
    public void close() {

        if (!channels.isEmpty()) {
            getDbChannel(null).close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
     */
    public boolean requiresLayout() {

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.log4j.AppenderSkeleton#setLayout(org.apache.log4j.Layout)
     */
    @Override
    public void setLayout(
                           Layout layout ) {

        super.setLayout(layout);

        // remember it
        this.layout = layout;

    }

    /**
     * log4j system reads the "events" parameter from the log4j.xml and calls
     * this method
     *
     * @param maxNumberLogEvents
     */
    public void setEvents(
                           String maxNumberLogEvents ) {

        this.appenderConfig.setMaxNumberLogEvents(maxNumberLogEvents);
    }

    /**
     * @return the capacity of the logging queue
     */
    public int getMaxNumberLogEvents() {

        return this.appenderConfig.getMaxNumberLogEvents();
    }

    /**
     * @return the current size of the logging queue
     */
    public int getNumberPendingLogEvents() {

        return getDbChannel( null ).getNumberPendingLogEvents();
    }

    /**
     * @return if sending log messages in batch mode
     */
    public boolean isBatchMode() {

        return this.appenderConfig.isBatchMode();
    }

    /**
     * log4j system reads the "mode" parameter from the log4j.xml and calls this
     * method
     *
     * Expected value is "batch", everything else is skipped.
     *
     * @param mode
     */
    public void setMode(
                         String mode ) {

        this.appenderConfig.setMode(mode);
    }

    /**
     * Get the current run id
     *
     * @return the current run id
     */
    public int getRunId() {

        return getDbChannel( null ).eventProcessor.getRunId();
    }

    /**
     * Get the current suite id
     *
     * @return the current suite id
     */
    public int getSuiteId() {

        return getDbChannel( null ).eventProcessor.getSuiteId();
    }

    /**
     * Get the current run name
     *
     * @return the current run name
     */
    public String getRunName() {

        return getDbChannel( null ).eventProcessor.getRunName();
    }

    /**
     * Get the current run user note
     *
     * @return the current run user note
     */
    public String getRunUserNote() {

        return getDbChannel( null ).eventProcessor.getRunUserNote();
    }

    /**
     *
     * @return the current testcase id
     */
    public int getTestCaseId() {

        return getDbChannel( null ).eventProcessor.getTestCaseId();
    }

    /**
     * @return the last executed, regardless of the finish status (e.g passed/failed/skipped), testcase ID
     * */
    public int getLastExecutedTestCaseId() {

        return getDbChannel( null ).eventProcessor.getLastExecutedTestCaseId();
    }

    public boolean getEnableCheckpoints() {

        return this.appenderConfig.getEnableCheckpoints();
    }

    public void setEnableCheckpoints( boolean enableCheckpoints ) {

        this.appenderConfig.setEnableCheckpoints(enableCheckpoints);
    }

    public DbAppenderConfiguration getAppenderConfig() {

        return appenderConfig;
    }

    public void setAppenderConfig( DbAppenderConfiguration appenderConfig ) {

        this.appenderConfig = appenderConfig;
        threshold = Level.toLevel(appenderConfig.getLoggingThreshold());
    }

    public void calculateTimeOffset( long executorTimestamp ) {

        // FIXME make the next working
        getDbChannel( null ).calculateTimeOffset(executorTimestamp);
    }
}
