package qoeAnalyzer.actor

/**
 * @author jangwee
 * @decription
 *     dashWorker 根据发送过来的 signalMessage 查询对应dashDataCollection中的数据，封装成最终输出结果
 *     送给formatWorker进行格式化输出
 *
 */

import akka.actor._
import com.mongodb.casbah.Imports._
import org.apache.log4j._
import qoeAnalyzer._
import qoeAnalyzer.message.{dashJoinLteMessage, dashJoinWifiMessage, Wake}
import qoeAnalyzer.util.Utils
import qoeAnalyzer.message._

import scala.concurrent.duration._
import scala.math._

class DashWorker extends Actor {

  //日志记录
  val logger = Logger.getLogger("logger4dashworker")
  //下一级worker
  val formatWorker = context.actorOf(Props[FormatWorker], name = "formatWorker")
  //当前Actor处理Collection
  val collection = db.DBObject.dashCollection

  //可接受的时间误差
  val MATCHERR = 799

  //设置超时
  context.setReceiveTimeout(Duration(10, HOURS))

  //根据时间戳，查询对应数据
  def find(timeStamp: Double) = {

    collection.find("qosInfo.0.0" $lt timeStamp).filter { doc => //{ "_id" : ... , "userId" : "::ffff:172.31.18.131", "qoeIndicator" : -1, "qosInfo" : [ [ 1430728092039, 0, 0, 1, 0, 0 ], [ 1430728093944, 0, 46, 1, "0.0000", 0 ], [ 1430728094962, 0, 3858, 20, "12.000", 0 ], [ 1430728095963, 0.958654, 3858, 20, "11.292", 0 ],
      {
        doc.getAs[com.mongodb.BasicDBList]("qosInfo").map { eleList => //[ [ 1430728092039, 0, 0, 1, 0, 0 ], [ 1430728093944, 0, 46, 1, "0.0000", 0 ], [ 1430728094962, 0, 3858, 20, "12.000", 0 ], [ 1430728095963, 0.958654, 3858, 20, "11.292", 0 ]
          {
            eleList.apply(eleList.size - 1).asInstanceOf[com.mongodb.BasicDBList].apply(0).asInstanceOf[Double] //获取最后一个数据的时间戳
          }
        }.map { _ > timeStamp }.getOrElse(false) //判断该条数据的最后一个时间戳是否大于目标时间戳
      }
    }.map { aimdoc => //找到对应的观看记录
      {
        aimdoc.getAs[com.mongodb.BasicDBList]("qosInfo").map { aimeleList => //[ [ 1430728092039, 0, 0, 1, 0, 0 ], [ 1430728093944, 0, 46, 1, "0.0000", 0 ], [ 1430728094962, 0, 3858, 20, "12.000", 0 ], [ 1430728095963, 0.958654, 3858, 20, "11.292", 0 ]
          {
            aimeleList.filter { aimele => //过滤出与目标时间戳偏差符合要求的数据
              {
                abs(aimele.asInstanceOf[com.mongodb.BasicDBList].apply(0).asInstanceOf[Double] - timeStamp) < MATCHERR
              }
            }.map { aiminnerele => //针对符合要求的数据　[ 1430728092039, 0, 0, 1, 0, 0 ]
              {
                val formatelem = aiminnerele.asInstanceOf[com.mongodb.BasicDBList] //强制转换
                //Utils.debugflag(println(formatelem))()
                (formatelem.apply(0).asInstanceOf[Double],
                  // formatelem.apply(1).asInstanceOf[Double],
                  if (formatelem.apply(1).isInstanceOf[Double]) formatelem.apply(1).asInstanceOf[Double] else formatelem.apply(1).asInstanceOf[Int].toDouble,
                  formatelem.apply(2).asInstanceOf[Int].toDouble,
                  formatelem.apply(3).asInstanceOf[Int].toDouble,
                  //formatelem.apply(4).asInstanceOf[String].toDouble,
                  if (formatelem.apply(4).isInstanceOf[String]) formatelem.apply(4).asInstanceOf[String].toDouble else formatelem.apply(4).asInstanceOf[Int].toDouble,
                  formatelem.apply(5).asInstanceOf[Int].toDouble) // 格式化为Tuple5
              }
            }
          }
        }
      }
    }.map { re => // 寻找时间偏差最小的Tuple5
      {
        re.map(seq =>
          seq.foldLeft((0d, 0d, 0d, 0d, 0d, 0d)) { (aimer, i) =>
            {
              if (abs(i._1 - timeStamp) < abs(aimer._1 - timeStamp)) i
              else aimer
            }
          })
      }
    }
  }.filter(!_.isEmpty)

  def receive = {
    case msg @ wifiSignalMessage(wifiInfo, pingInfo) => {

      logger.info(s"dashWorker receive msg : ${msg}")
      //根据时间戳，查询对应数据
      val iter = find(wifiInfo._1)
      //根据迭代器取值
      val nmsg = if (iter.hasNext) {
        val elem = iter.next()
        for {
          wifi <- Option(wifiInfo)
          ping <- Option(pingInfo)
          dash <- elem
        } yield (dashJoinWifiMessage(wifi, ping, dash))
      } else None

      //向formatworker发送消息
      nmsg.foreach { m => formatWorker ! m }
    }

    case msg @ lteSignalMessage(lteInfo, pingInfo) => {
      logger.info(s"dashWorker receive msg : ${msg}")
      //根据时间戳，查询对应数据
      val iter = find(lteInfo._1)
      //根据迭代器取值
      val nmsg = if (iter.hasNext) {
        val elem = iter.next()
        for {
          lte <- Option(lteInfo)
          ping <- Option(pingInfo)
          dash <- elem
        } yield (dashJoinLteMessage(lte, ping, dash))
      } else None

      nmsg.foreach { m => formatWorker ! m }
    }

    //调试消息
    case msg @ Debug(index) => {
      println(s"dashWorker receive msg : ${msg}")
      formatWorker ! msg
    }

    //超时信息
    case ReceiveTimeout => {
      Utils.debugflag(println("dashworker receive message timeout , send Wake !"))()
      logger.info("dashworker receive message timeout 10s , send Wake !")
      context.parent ! Wake //向上一层递交wake消息
    }
  }
}


