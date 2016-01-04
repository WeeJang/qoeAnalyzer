package qoeAnalyzer.actor

import akka.actor._
import org.apache.log4j._
import qoeAnalyzer.util.Utils
import qoeAnalyzer.message._

class FormatWorker extends Actor {

  //logger
  val logger = Logger.getLogger("logger4formatworker")
  //outputer
  val outputer = Logger.getLogger("logger4output")

  def receive = {
    case msg @ dashJoinWifiMessage(wifiInfo, pingInfo, dashInfo) => {
      //日志记录
      logger.info(s"formatWorker receive : ${msg}")
      //格式化输出
      outputer.info(s"${wifiInfo._1}\t${wifiInfo._2}\t${wifiInfo._3}\t${pingInfo._1}\t${
        pingInfo._2}\t${dashInfo._1}\t${dashInfo._2}\t${dashInfo._3}\t${
        dashInfo._4}\t${dashInfo._5}\t${dashInfo._6}")
      //更新当前进度        
      Utils.updateCurrentXML(this.getClass.getClassLoader.getResource("current.xml").getPath, dashInfo._1.toString, wifiInfo._1.toString)
    }
    
    case msg @ dashJoinLteMessage(lteInfo, pingInfo, dashInfo) => {
      //日志记录
      logger.info(s"formatWorker receive : ${msg}")
      //格式化输出
      outputer.info(s"${lteInfo._1}\t${lteInfo._2}\t${lteInfo._3}\t${lteInfo._4}\t${lteInfo._5}\t${lteInfo._6}\t${pingInfo._1}\t${
        pingInfo._2}\t${dashInfo._1}\t${dashInfo._2}\t${dashInfo._3}\t${
        dashInfo._4}\t${dashInfo._5}\t${dashInfo._6}")
      //更新当前进度        
      Utils.updateCurrentXML(this.getClass.getClassLoader.getResource("current.xml").getPath, dashInfo._1.toString, lteInfo._1.toString)
    }
    
    
    //调试消息
    case msg @ Debug(index) => {
      println(s"formatWorker receive : ${msg}")
    }
  }
}