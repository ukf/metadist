/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.]
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

/*
 * WrappedLog.java
 * 
 * IOU object for some Log data.
 * 
 * This interface is implemented, for example, by the 
 * SimpleAppenderContextImpl.WrappedStringLog class.
 */

package edu.internet2.middleware.commons.log4j;

/**
 * Wrapper to abstract the ThreadLocal log storage.
 * 
 * <p>
 * In most cases, the log data will just be a string kept in memory. However, one could imagine it would be a file on
 * disk, or an EhCache hybrid where the last 100 are kept in memory and the overflow of less recently used are written
 * to disk. So after logging is done, we return this IOU that will fetch the log data later when you want to display it.
 * 
 * @author Howard Gilbert
 */
public interface WrappedLog {

	String getLogData();

}
