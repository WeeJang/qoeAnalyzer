package qoeAnalyzer.actor

import akka.actor._
import com.mongodb.casbah.Imports._
import org.apache.log4j._
import qoeAnalyzer._
import qoeAnalyzer.message.{Debug, lteSignalMessage, wifiSignalMessage, Wake}
import qoeAnalyzer.util.Utils
import qoeAnalyzer.message._

import scala.collection.JavaConversions._
/*
 * @Author: Jangwee
 * @Description:
 *     根据master发过来的起始timestamp继续查询．将查询到的wifi 和　ping 数据按照时间戳进行对应组装．
 *     将组装好的消息发送给　DashWorker
 * 
 */
class SignalWorker extends Actor {
  //日志记录
  val logger = Logger.getLogger("logger4signalworker")
  //DashWorker实例
  val dashWorker = context.actorOf(Props[DashWorker], name = "dashWorker")
  //当前Actor处理Collection
  val collection = db.DBObject.signalCollection

  //wifi数据暂存区
  //timeStamp: Double,RSSI: Int,linkSpeed:Int
  val wifiAssembledZone = new scala.collection.mutable.ArrayBuffer[(Double, Double, Double)](10)
  //wifi暂存区是否需要清空标志位
  var wifiAssembleShouldBeCleaned = false
  //wifi初始化清空
  wifiAssembledZone.clear()

  //lte数据暂存区
  //timeStamp,mLteRsrp,mLteSignalStrength,mLteCqi,mLteRssnr,mLteRsrq : All are Double
  val lteAssembledZone = new scala.collection.mutable.ArrayBuffer[(Double, Double, Double, Double, Double, Double)](10)
  //lte暂存区是否需要清空标志位
  var lteAssembledShouldBeCleaned = false
  //lte初始化
  lteAssembledZone.clear()

  //flag which used to distinct the wifi(true) or lte(false)
  var isWifiSignal = true

  //ping一次理论包含次数
  val pingNum = 10
  //重置receive
  def receive = {
    case msg @ LastIndexMessage(index) => {
      logger.info(s"signalWorker receive msg :  ${msg}")
      println(s"receive ${msg}")
      //寻找时间戳大于当前进度的docs
      var docs = collection.find("userInfo.0.timeStamp" $gt index)
      //对每一条doc进行整理
      for (doc <- docs) {
        doc.getAs[org.bson.BSONObject]("userInfo").map { userInfos => //获取　userInfo 对应键值 eg : { "0" : { "timeStamp" : 1430725937314, "dataType" : "ping", "dataInfo" : "{pingInfo=PING 202.1 ...}"},"1" ...}
          {
            userInfos.toMap().values.filter { x => x.isInstanceOf[BasicDBObject] } //获取分条记录　{ "timeStamp" : 1430725937314, "dataType" : "ping", "dataInfo" : "{pingInfo=PING 202.1 ...}"}
              .foreach { userInfo =>
                val elem = userInfo.asInstanceOf[BasicDBObject] //强制类型转换为BasicDBObject
                val timeStamp = elem.getAs[Double]("timeStamp") //获取timeStamp
                val info = elem.getAs[String]("dataInfo") //获取dataInfo

                val wifiParttern = ".+RSSI=(-\\d+).+LinkSpeed=(\\d+).+".r //正则表达式匹配提取wifi对应数据：RSSI & LinkSpeed
                val pingParttern = ".+icmp_seq=(\\d+).+time=(\\d+.\\d+).+".r //正则表达式匹配提取ping对应数据： icmp_seq & time
                val lteParttern = ".+mLteRsrp=(-\\d+).+mLteSignalStrength=(-\\d+).+mLteCqi=(-\\d+).+mLteRssnr=(-?\\d+).+mLteRsrq=(-\\d+).+".r //正则表达式提取lte对应数据：mLteRsrp＆mLteSignalStrength＆mLteCqi＆mLteRssnr＆mLteRsrq

                elem.getAs[String]("dataType").map { dataType => //获取dataType
                  dataType match { //dataType 匹配

                    case "wifi" => { //匹配到"wifi"
                      isWifiSignal = true
                      val optionInfo = info.map { infoStr => // dataInfo数据:eg "{SSID=\"web.wlan.bjtu\", RSSI=-63, LinkSpeed=11}"
                        {
                          val wifiParttern(rssi, linkspeed) = infoStr //正则匹配，获取对应值
                          (rssi, linkspeed) //组装成Tuple2 (rssi, linkspeed)
                        }
                      }

                      val msg = for { //将Tuple2 封装成Tuple3 (timeStamp,rssi,linkspeed)
                        ts <- timeStamp
                        rssi <- Option(optionInfo.get._1)
                        ls <- Option(optionInfo.get._2)
                      } yield ((ts, rssi.toDouble, ls.toDouble))

                      msg.foreach { m =>
                        {
                          if (wifiAssembleShouldBeCleaned) { //检查是否应该对wif暂存区进行清理
                            wifiAssembledZone.clear() //暂存区清理
                            wifiAssembleShouldBeCleaned = false //重置标志位
                          }
                          wifiAssembledZone += ((m._1, m._2, m._3)) //将数据添加到暂存区
                        }
                      }

                    }

                    case "lte" => { //匹配到"lte"
                      isWifiSignal = false
                      val optionInfo = info.filter{ !_.equals("{}") }.map { infoStr => // dataInfo数据:eg "{mLteRsrp=-109, mLteSignalStrength=-1, mLteCqi=-1, mLteRssnr=2, mLteRsrq=-10}"
                        {
                          val lteParttern(mLteRsrp, mLteSignalStrength, mLteCqi, mLteRssnr, mLteRsrq) = infoStr //正则匹配，获取对应值
                          (mLteRsrp, mLteSignalStrength, mLteCqi, mLteRssnr, mLteRsrq) //组装成Tuple5 (mLteRsrp,mLteSignalStrength,mLteCqi,mLteRssnr,mLteRsrq)
                        }
                      }

                      if(optionInfo != None){
                        val msg = for { //将Tuple5 封装成Tuple6 (timeStamp,mLteRsrp,mLteSignalStrength,mLteCqi,mLteRssnr,mLteRsrq)
                          ts <- timeStamp
                          ltersrp <- Option(optionInfo.get._1)
                          ltess <- Option(optionInfo.get._2)
                          ltecqi <- Option(optionInfo.get._3)
                          lterssnr <- Option(optionInfo.get._4)
                          ltersrq <- Option(optionInfo.get._5)
                        } yield ((ts, ltersrp.toDouble, ltess.toDouble, ltecqi.toDouble, lterssnr.toDouble, ltersrq.toDouble))

                        msg.foreach { m =>
                        {
                          if (lteAssembledShouldBeCleaned) { //检查是否应该对暂存区进行清理
                            lteAssembledZone.clear() //暂存区清理
                            lteAssembledShouldBeCleaned = false //重置标志位
                          }
                          lteAssembledZone += ((m._1, m._2, m._3, m._4, m._5, m._6)) //将数据添加到暂存区
                        }
                        }
                      }
                    }

                    case "ping" => { //匹配到＂ping＂
                      val optionInfo = info.map { infoStr => //dataInfo数据:eg "{pingInfo=PING 202.112.146.103 (202.112.146.103) 56(84) bytes of data.\n64 bytes from 202.112.146.103: icmp_seq=1 ttl=61 time=494 ms\n
                        {
                          infoStr.split("\n").filter(_.contains("icmp_seq")).map { icmpstr => //切割，过滤
                            {
                              val pingParttern(icmp_seq, rtt) = icmpstr //正则匹配
                              (icmp_seq, rtt) //封装成Tuple2(icmp_seq, rtt)
                            }
                          }
                        }
                      }
                      //optionInfo.map{ x => x.foreach( println _)}
                      val msgArrOption = optionInfo.map { infoArr =>
                        infoArr.flatMap { infoTuple =>
                          {
                            for {
                              ts <- timeStamp
                              seq <- Option(infoTuple._1.toDouble)
                              rtt <- Option(infoTuple._2)
                            } yield ((ts - 1000.0 * (pingNum + 1 - seq), rtt.toDouble)) //将Tuple2(icmp_seq, rtt) 封装成Tuple2 (timeStamp,rtt)
                          }
                        }
                      }

                      //
                      msgArrOption.filter {
                        _.size != 0 //过滤掉没有数据的array
                      }.foreach { msgArr => //Ping Tuple2
                        {
                          if (isWifiSignal) {
                            //将　wifi 与　ping 进行对应zip
                            val wpmsgArr = if (wifiAssembledZone.size <= msgArr.size) { //wifi数据长度不大于ping长度
                              wifiAssembledZone.zip(msgArr)
                            } else { //wifi数据长度大于ping数据长度
                              wifiAssembledZone.zipAll(msgArr, wifiAssembledZone(wifiAssembledZone.size - 1), msgArr(msgArr.size - 1))
                            }
                            wpmsgArr.foreach { msg => dashWorker ! wifiSignalMessage(msg._1, msg._2) }
                            wifiAssembleShouldBeCleaned = true //重置暂存区
                          } else {
                            //将　lte 与　ping 进行对应zip
                            val lpmsgArr = if (lteAssembledZone.size <= msgArr.size) { //wifi数据长度不大于ping长度
                              lteAssembledZone.zip(msgArr)
                            } else { //wifi数据长度大于ping数据长度
                              lteAssembledZone.zipAll(msgArr, lteAssembledZone(lteAssembledZone.size - 1), msgArr(msgArr.size - 1))
                            }
                            lpmsgArr.foreach { msg => dashWorker ! lteSignalMessage(msg._1, msg._2) }
                            lteAssembledShouldBeCleaned = true
                          }
                        }
                      }
                    }
                  }
                }
              }
          }
        }
      }
    }

    //调试使用消息
    case msg @ Debug(index) => {
      println(s"signalWorker receive msg: ${msg}")
      dashWorker ! Debug(index)
    }

    //唤醒消息
    case msg @ Wake => {
      logger.info(s"signalworker recevie ${msg} from ${sender}")
      Utils.debugflag(println(s"signalworker recevie ${msg} from ${sender}"))()
      context.parent ! Wake
    }
  }
}

      