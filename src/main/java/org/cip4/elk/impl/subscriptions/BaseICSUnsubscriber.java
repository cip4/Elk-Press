/*
 * Created on 2005-apr-27 
 */
package org.cip4.elk.impl.subscriptions;

import org.apache.log4j.Logger;
import org.cip4.jdflib.jmf.JDFStopPersChParams;

/**
 * Implements a BaseICS level 3 class for unregistering subscriptions.
 * 
 * @author Ola Stering (olst6875@student.uu.se)
 * @version $Id: BaseICSUnsubscriber.java,v 1.3 2006/11/17 13:41:02 buckwalter Exp $
 */
public class BaseICSUnsubscriber implements Unsubscriber {

    private Logger log;

    // Assumes that every instance of a BaseICSUnsubscriber belongs to ONLY
    // one device
    private String _deviceID;

    /**
     * Creates an unsubscriber.
     * 
     * @param deviceID The DeviceID of this unsubscriber.
     */
    public BaseICSUnsubscriber(String deviceID) {
        _deviceID = deviceID;
        log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Unregisters subscriptions according to the <em>StopPersChParams</em>
     * and Base ICS Level 3.
     * 
     * It the <em>StopPersChParams/@URL</em> does not exist, is equal to '' or
     * does not contain any previous subscriptions, no unregistering will take
     * place.
     * 
     * The method takes into account the following attributes of
     * <em>StopPersChParams</em>, and will unregister the subscriptions that
     * match at <em>StopPersChParams/@URL</em>:
     * <ul>
     * <li><em>ChannelID</em>, unregisters the specified persistent channel
     * whose query's ID was "theID".</li>
     * <li><em>MessageType</em>, unregisters all subscriptions of the
     * specified MessageType. I. e. all subscriptions of type "QueueStatus"
     * </li>
     * <li><em>DeviceID</em>, unregisters all subscriptions belong to the
     * specified device. I. e. which has their original <em>Query/@DeviceID</em>
     * equal to this id. See NOTE.</li>
     * </ul>
     * 
     * If only <em>StopPersChParams/@URL</em> is given ALL subscriptions to
     * that url will be unregistered.
     * 
     * NOTE: Since all subscriptions are made for this Device, setting the
     * DeviceID will unregister ALL subscriptions or NONE of the specified id.
     * TODO XXX Is this correctly interpreted?
     * 
     * @see Unsubscriber#unsubscribe(SubscriptionContainer, JDFStopPersChParams)
     * @throws NullPointerException if subscriptions or stopParams is
     *             <code>null</code>
     */
    public int unsubscribe(SubscriptionContainer subscriptions,
            JDFStopPersChParams stopParams) {

        int returnCode = 0;

        if (stopParams == null) {
            throw new NullPointerException(
                    "JDFStopPersChParams may not be null.");
        } else if (subscriptions == null) {
            throw new NullPointerException(
                    "The Subscriptions parameter may not be null");
        }

        String urlKey = stopParams.getURL(); // this attribute is required

        if (!subscriptions.hasSubscriptionsAt(urlKey)) {
            log
                    .warn("The StopPersChParams/@URL='"
                            + urlKey
                            + "' does not contain any subscriptions. Operation aborted.");
            returnCode = 7; // Insufficient parameters
            return returnCode;
        }

        String channelID = stopParams.getChannelID();
        String messageType = stopParams.getMessageType();
        String deviceID = stopParams.getDeviceID();

        // These three attributes need to be handled according to BaseICS:
        // ChannelID, MessageType, DeviceID
        if (channelID != null && channelID.length() > 0) {

            boolean success = subscriptions.removeSubscription(urlKey,
                channelID);
            if (!success) {
                returnCode = 7;
            }

        } else if (messageType != null && !messageType.equals("")) { // Message Type
            returnCode = subscriptions.removeSubscriptionsOfType(urlKey,
                messageType);
        } else {
            // Device ID
            if (deviceID == null || deviceID.length() == 0 || _deviceID.equals(deviceID)) {
                log
                        .debug("All subscriptions from " + urlKey
                                + " to be removed");
                returnCode = subscriptions.removeSubscriptions(urlKey);
                // TODO This is not right I think.
            } else {
                log.warn("No subscription unregistered. " + deviceID
                        + " does not match device's ID " + _deviceID);
                returnCode = 6; // Invalid parameter
            }
        }
        return returnCode;

    }
}
