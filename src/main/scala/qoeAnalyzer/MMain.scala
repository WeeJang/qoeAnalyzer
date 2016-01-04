package qoeAnalyzer

/**
 * @author jangwee
 * @decscription
 *    入口函数
 */

import qoeAnalyzer.actor.Master
import qoeAnalyzer.db.DBObject
import qoeAnalyzer.message.Calculate

import scala.xml._
import akka.actor._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.log4j._
import org.apache.log4j.xml.DOMConfigurator

object MMain {

  def main(args: Array[String]): Unit = {
    //载入log4j配置文件
    DOMConfigurator.configure(this.getClass.getClassLoader.getResource("log4jconfig.xml").getPath);
    //数据库初始化
    DBObject.init()
    //Actor system
    val system = ActorSystem("qoeAnalyzerSystem")
    //实例化master
    val master = system.actorOf(Props[Master], name = "master")
    //启动计算
    master ! Calculate
  }
}

