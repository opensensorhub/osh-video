/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.dahua;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;


/**
 * <p>
 * Implementation of video output interface for Dahua cameras using
 * RTSP/RTP protocol
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class DahuaVideoOutput extends RTPVideoOutput<DahuaCameraDriver>
{
    volatile long lastFrameTime;
    Timer watchdog;
    
    
	protected DahuaVideoOutput(DahuaCameraDriver driver)
	{
		super(driver,
		      driver.getConfiguration().video,
		      driver.getConfiguration().rtsp);
	}
	

    @Override
    public void start() throws SensorException
    {
        super.start();
        
        DahuaCameraConfig config = parentSensor.getConfiguration();
        final long maxFramePeriod = 10000 / config.video.frameRate;
        
        // start watchdog thread to detect disconnections
        lastFrameTime = Long.MAX_VALUE;
        TimerTask checkFrameTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (lastFrameTime < System.currentTimeMillis() - maxFramePeriod)
                {
                    parentSensor.connection.reconnect();
                    cancel();
                }
            }
        };
        
        watchdog = new Timer();
        watchdog.scheduleAtFixedRate(checkFrameTask, 0L, 10000L);
    }


    @Override
    public void onFrame(long timeStamp, int seqNum, ByteBuffer frameBuf, boolean packetLost)
    {
        super.onFrame(timeStamp, seqNum, frameBuf, packetLost);
        lastFrameTime = System.currentTimeMillis();
    }


    @Override
    public void stop()
    {
        super.stop();
        watchdog.cancel();
    }
}
