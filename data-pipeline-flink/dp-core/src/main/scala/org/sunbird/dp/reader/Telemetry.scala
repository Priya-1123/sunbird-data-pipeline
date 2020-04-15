package org.sunbird.dp.reader

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util
import java.util.Date


@SerialVersionUID(8132821816689744470L)
class Telemetry(var map: util.Map[String, Any]) extends Serializable {

  def add(keyPath: String, value: Any): Boolean = {
    try {
      val lastParent = lastParentMap(map, keyPath)
      lastParent.addChild(value)
      return true
    } catch {
      case e: Exception =>
    }
    false
  }

  def getMap: util.Map[String, Any] = map

  def read[T](keyPath: String): Option[T] = try {
    val parentMap = lastParentMap(map, keyPath)
    Option(parentMap.readChild.getOrElse(null).asInstanceOf[T])
  } catch {
    case ex: Exception =>
      println(s"Could not read the object from $keyPath")
      Option(null.asInstanceOf[T])
  }

  def readOrDefault[T](keyPath: String, defaultValue: T): T = {
    val value = read(keyPath)
    if (null != value) {
      value.asInstanceOf[T]
    } else {
      defaultValue.asInstanceOf[T]
    }

  }

  @throws[TelemetryReaderException]
  def mustReadValue[T](keyPath: String): T = {
    //    read(keyPath).getOrElse({
    //     val eid = read("eid")
    //      throw new TelemetryReaderException(s"keyPath is not available in the $eid ")
    //    })
    //    val readValue = read(keyPath)
    //    if (null == readValue) {
    //      val eid = read("eid")
    //      throw new TelemetryReaderException(s"keyPath is not available in the $eid ")
    //    } else {
    //      readValue.getOrElse(null)
    //    }
    null.asInstanceOf[T]
  }

  private def lastParentMap(map: util.Map[String, Any], keyPath: String): ParentType = {
    try {
      var parent = map
      val keys = keyPath.split("\\.")
      val lastIndex = keys.length - 1
      if (keys.length > 1) {
        var i = 0
        while ( {
          i < lastIndex && parent != null
        }) {
          var result: util.Map[String, Any] = null
          if (parent.isInstanceOf[util.Map[_, _]]) {
            result = new ParentMap(parent, keys(i)).readChild.getOrElse(null)
          }
          else if (parent.isInstanceOf[util.List[_]]) {
            result = new ParentListOfMap(parent.asInstanceOf[util.List[util.Map[String, Any]]], keys(i)).readChild.getOrElse(null)
          } else {
            result = null
          }
          parent = result
          i += 1
        }
      }
      val lastKeyInPath = keys(lastIndex)
      if (parent.isInstanceOf[util.Map[_, _]]) {
        new ParentMap(parent, lastKeyInPath)
      }
      else if (parent.isInstanceOf[util.List[_]]) new ParentListOfMap(parent.asInstanceOf[util.List[util.Map[String, Any]]], lastKeyInPath)
      else null
    } catch {
      case ex: Exception =>
        null
    }
  }

  override def toString: String = "Telemetry{" + "map=" + map + '}'

  def id: String = this.read[String]("metadata.checksum").getOrElse(null)

  def addFieldIfAbsent[T](fieldName: String, value: T): Unit = {
    if (null != read(fieldName)) add(fieldName, value.asInstanceOf[T])
  }

  @throws[TelemetryReaderException]
  def getEts: Long = {
    val ets = this.mustReadValue[Double]("ets")
    ets.toLong
  }

  def getAtTimestamp: String = {
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val timeStamp = read("@timestamp").getOrElse(simpleDateFormat.format(new Date(System.currentTimeMillis().longValue)))
    if (timeStamp.isInstanceOf[Number]) {
      simpleDateFormat.format(new Date(timeStamp.asInstanceOf[Number].longValue))
    } else {
      timeStamp.toString
    }
  }

  def getSyncts: String = {
    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val timeStamp = read("syncts")
    if (timeStamp.isInstanceOf[Number]) {
      simpleDateFormat.format(new Date(timeStamp.asInstanceOf[Number].longValue))
    } else {
      timeStamp.toString
    }
  }
}
