/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.util.temporal

import java.util.Date

/**
 * An object that contains all needed constants.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

object Constants {

  /**
   * period delimiter.
   */
  val PERIOD_DELIM = ","

  /**
   * Simple Date Format
   */
  val SIMPLE_DATE_FORMAT = "yyyy-MM-DD'T'hh:mm:ss"

  /**
   * Min Time - "the epoch" (January 1, 1970, 00:00:00 GMT)
   */
  val MIN_TIME = new Date(0l).getDate()

  /**
   * Max Time - "NOW"
   */
  val MAX_TIME = new Date().getDate()

  /**
   * Convert Milliseconds
   */
  val MILLISECS_PER_SEC = 1000.0
  val MILLISECS_PER_MIN = 60000.0
  val MILLISECS_PER_HOUR = 3600000.0
  val MILLISECS_PER_DAY = 86400000.0
  val MILLISECS_PER_MONTH = 2629743830.0
  val MILLISECS_PER_YEAR = 31556926000.0

  /**
   * Time Distances
   */
  val MILLISECS_DISTANCE = "millisecsDistance"
  val SECS_DISTANCE = "secsDistance"
  val MIN_DISTANCE = "minDistance"
  val HOUR_DISTANCE = "hourDistance"
  val DAY_DISTANCE = "dayDistance"
  val MONTH_DISTANCE = "monthDistance"
  val YEAR_DISTANCE = "yearDistance"

  /**
   * Allen's Relations
   */
  val BEFORE = "before"
  val AFTER = "after"
  val MEETS = "meets"
  val IS_MET_BY = "isMetBy"
  val OVERLAPS = "overlaps"
  val IS_OVERLAPPED_BY = "isOverlappedBy"
  val FINISHES = "finishes"
  val IS_FINISHED_BY = "isFinishesBy"
  val CONTAINS = "contains"
  val DURRING = "durring"
  val STARTS = "starts"
  val IS_STARTED_BY = "isStartedBy"
  val EQUALS = "equals"
}