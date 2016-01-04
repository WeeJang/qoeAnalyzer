package qoeAnalyzer.actor

/**
 * @author jangwee
 * @description
 *
 */

import java.io.File

import akka.actor._
import org.apache.log4j._
import qoeAnalyzer._
import qoeAnalyzer.message._
import qoeAnalyzer.util.Utils

import scala.xml._

class Master extends Actor {
  //master logger
  val logger = Logger.getLogger("logger4master")
  //SingalWorker实例
  val signalWorker = context.actorOf(Props[SignalWorker], name = "signalWorker")
  //装载配置
  val currentXMLElem = XML.loadFile(new File(this.getClass.getClassLoader.getResource("current.xml").getPath))

  //重载receive
  def receive = {
    //处理计算消息
    case msg @ Calculate => {
      //获取当前处理进度
      val currentSignalTimestamp = Utils.getSignalTimeStamp(currentXMLElem).toDouble
      println("master start calculate")
      logger.info(s"master receive msg : ${msg}")
      logger.info(s"master start calculate from signalTimeStamp: ${currentSignalTimestamp}")
      //发送处理进度消息
      signalWorker ! LastIndexMessage(currentSignalTimestamp)
    }

    //处理调试信息
    case msg @ Debug(index) => {
      println(s"master receive msg: ${msg}")
      signalWorker ! msg
    }

    //停止信息
    case msg @ Stop => {

    }
    //test
    case test @ Test => {
      val str = this.getClass.getClassLoader.getResource("").getPath
      System.out.println(str)
    }

    //唤醒消息
    case msg @ Wake => {
      logger.info(s"master receive ${msg} from ${sender}")
      Utils.debugflag(println(s"master receive ${msg} from ${sender}"))()
      //驱动下一次计算
      self ! Calculate
    }
  }
}