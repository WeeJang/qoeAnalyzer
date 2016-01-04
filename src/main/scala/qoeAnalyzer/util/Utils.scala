package qoeAnalyzer.util

import scala.xml._

/**
 * @author jangwee
 * @date 2015-5-2
 * @description : 工具类
 */

object Utils {

  // Get MongoDB Uri
  def getMongoDBUri(elem: Elem) = getMongoDBInfo(elem, "uri")
  // Get MongoDB Name
  def getMongoDBName(elem: Elem) = getMongoDBInfo(elem, "name")
  // Get Dash Collection Name
  def getDashCollectionName(elem: Elem) = getCollectionInfo(elem, "dash")
  // Get Singal Collection Name
  def getSignalCollectionName(elem: Elem) = getCollectionInfo(elem, "signal")
  // Get Dash TimeStamp
  def getDashTimeStamp(elem: Elem) = getTimeStamp(elem, "dash")
  // Get Signal TimeStamp
  def getSignalTimeStamp(elem: Elem) = getTimeStamp(elem, "signal")
  //  Update Current XML
  def updateCurrentXML(uri: String , dashTimeStamp: String, signalTimeStamp: String): Unit = {
    val current =
      <current>
        <timeStamp>
          <dash>{ dashTimeStamp }</dash>
          <signal>{ signalTimeStamp }</signal>
        </timeStamp>
      </current>
    XML.save(uri, current, "UTF-8", true, null)
  }

  private def getMongoDBInfo(elem: Elem, attr: String): String = {
    val mongoDBInfo = elem \ "MongoDB" \ ("@" + attr)
    if (mongoDBInfo.length == 1) {
      mongoDBInfo(0).text
    } else throw new RuntimeException("Not found <MongoDB></MongDB> in xml")
  }

  private def getCollectionInfo(elem: Elem, collectionType: String): String = {
    val collectionInfo = elem \ "MongoDB" \ "collection"
    val resultSeq = for {
      collectionInfoNode <- collectionInfo
      if (collectionInfoNode \ "@type").text.equalsIgnoreCase(collectionType)
    } yield (collectionInfoNode \ "@name").text
    if (resultSeq.size == 1) resultSeq(0)
    else throw new RuntimeException("Not found info about " + collectionType + "in xml")
  }

  private def getTimeStamp(elem: Elem, collectionType: String): String = {
    val collectionInfo = elem \ "timeStamp" \ collectionType
    if (collectionInfo.size == 1) collectionInfo(0).text
    else throw new RuntimeException("Not found timeStamp about " + collectionType + "in xml")
  }
  
  //调试函数
  def debugflag(f: => Unit)(debugflag : String = "default") : Unit = {
    println(s"${debugflag}===============================start")
    f
    println(s"${debugflag}===============================end")
  }
}


